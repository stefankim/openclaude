package com.openclaude.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.data.remote.GeoResult
import com.openclaude.weather.data.repository.WeatherRepository
import com.openclaude.weather.domain.Forecast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ForecastState {
    data object Idle : ForecastState
    data object Loading : ForecastState
    data class Success(val forecast: Forecast) : ForecastState
    data class Error(val message: String) : ForecastState
}

data class SearchState(
    val query: String = "",
    val results: List<GeoResult> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val store: LocationStore
) : ViewModel() {

    val locations: StateFlow<List<SavedLocation>> =
        store.locations.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selected: StateFlow<SavedLocation?> =
        combine(store.locations, store.selectedId) { list, id ->
            list.firstOrNull { it.id == id } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _forecast = MutableStateFlow<ForecastState>(ForecastState.Idle)
    val forecast: StateFlow<ForecastState> = _forecast.asStateFlow()

    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search.asStateFlow()

    val model: StateFlow<String> =
        store.model.stateIn(viewModelScope, SharingStarted.Eagerly, "best_match")

    private var lastLoadedKey: String? = null

    init {
        // Auto-load forecast whenever the selected location or model changes.
        viewModelScope.launch {
            combine(selected, store.model) { loc, m -> loc to m }.collect { (loc, m) ->
                val key = loc?.let { "${it.id}|$m" }
                if (loc != null && key != lastLoadedKey) {
                    lastLoadedKey = key
                    load(loc, force = false, model = m)
                }
            }
        }
    }

    fun select(id: String) = viewModelScope.launch { store.select(id) }

    fun setModel(model: String) = viewModelScope.launch { store.setModel(model) }

    fun refresh() = viewModelScope.launch {
        selected.value?.let { load(it, force = true, model = model.value) }
    }

    private fun load(location: SavedLocation, force: Boolean, model: String) = viewModelScope.launch {
        _forecast.value = ForecastState.Loading
        runCatching { repository.forecast(location, force, model) }
            .onSuccess { _forecast.value = ForecastState.Success(it) }
            .onFailure {
                _forecast.value = ForecastState.Error(it.message ?: "Could not load weather")
            }
    }

    // ---- Location management ----

    fun onQueryChange(q: String) {
        _search.value = _search.value.copy(query = q)
        if (q.length < 2) {
            _search.value = _search.value.copy(results = emptyList(), message = null)
            return
        }
        viewModelScope.launch {
            _search.value = _search.value.copy(loading = true, message = null)
            runCatching { repository.search(q) }
                .onSuccess { results ->
                    _search.value = _search.value.copy(
                        loading = false,
                        results = results,
                        message = if (results.isEmpty()) "No places found" else null
                    )
                }
                .onFailure {
                    _search.value = _search.value.copy(loading = false, message = "Search failed")
                }
        }
    }

    fun addFromSearch(result: GeoResult, onResult: (Boolean, String) -> Unit) {
        val loc = SavedLocation(
            id = SavedLocation.idFor(result.latitude, result.longitude),
            name = result.name,
            region = result.admin1,
            country = result.country,
            latitude = result.latitude,
            longitude = result.longitude,
            timezone = result.timezone
        )
        viewModelScope.launch {
            val current = store.current()
            when {
                current.any { it.id == loc.id } ->
                    onResult(false, "${loc.name} is already in your list")
                current.size >= LocationStore.MAX_LOCATIONS ->
                    onResult(false, "You can save up to ${LocationStore.MAX_LOCATIONS} locations")
                else -> {
                    val ok = store.add(loc)
                    onResult(ok, if (ok) "Added ${loc.name}" else "Could not add location")
                    _search.value = SearchState()
                }
            }
        }
    }

    fun remove(id: String) = viewModelScope.launch { store.remove(id) }

    fun clearSearch() {
        _search.value = SearchState()
    }

    class Factory(
        private val repository: WeatherRepository,
        private val store: LocationStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeatherViewModel(repository, store) as T
    }
}
