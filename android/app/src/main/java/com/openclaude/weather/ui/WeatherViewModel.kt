package com.openclaude.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openclaude.weather.data.local.LocationStore
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.data.remote.GeoResult
import com.openclaude.weather.data.repository.WeatherRepository
import com.openclaude.weather.domain.AirQuality
import com.openclaude.weather.domain.Forecast
import com.openclaude.weather.domain.WeatherAlert
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

data class CompareState(
    val loading: Boolean = false,
    val rows: List<WeatherRepository.ModelDaily> = emptyList()
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

    val settings: StateFlow<LocationStore.Settings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, LocationStore.Settings())

    private val _air = MutableStateFlow<AirQuality?>(null)
    val air: StateFlow<AirQuality?> = _air.asStateFlow()

    private val _alerts = MutableStateFlow<List<WeatherAlert>>(emptyList())
    val alerts: StateFlow<List<WeatherAlert>> = _alerts.asStateFlow()

    private val _previews = MutableStateFlow<Map<String, WeatherRepository.PlacePreview>>(emptyMap())
    val previews: StateFlow<Map<String, WeatherRepository.PlacePreview>> = _previews.asStateFlow()

    private val _compare = MutableStateFlow(CompareState())
    val compare: StateFlow<CompareState> = _compare.asStateFlow()

    private var lastLoadedKey: String? = null
    private var lastPreviewKey: String? = null

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
        // Refresh saved-place previews when the list changes.
        viewModelScope.launch {
            locations.collect { list ->
                val key = list.joinToString(";") { it.id }
                if (list.isNotEmpty() && key != lastPreviewKey) {
                    lastPreviewKey = key
                    runCatching { repository.placePreviews(list) }
                        .onSuccess { _previews.value = it }
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
        // Show cached data instantly (works offline), then refresh from the network.
        val cached = if (!force) repository.cachedForecast(location, model) else null
        _forecast.value = if (cached != null) ForecastState.Success(cached) else ForecastState.Loading

        runCatching { repository.forecast(location, force, model) }
            .onSuccess { _forecast.value = ForecastState.Success(it) }
            .onFailure {
                if (cached == null) {
                    _forecast.value = ForecastState.Error(it.message ?: "Could not load weather")
                } // else: keep showing the cached forecast (its timestamp shows staleness)
            }

        launch {
            runCatching { repository.airQuality(location) }
                .onSuccess { _air.value = it }
                .onFailure { _air.value = null }
        }
        launch {
            runCatching { repository.alerts(location) }
                .onSuccess { _alerts.value = it }
                .onFailure { _alerts.value = emptyList() }
        }
    }

    // ---- Settings ----

    fun setTempUnit(v: String) = viewModelScope.launch { store.setTempUnit(v) }
    fun setWindUnit(v: String) = viewModelScope.launch { store.setWindUnit(v) }
    fun setAnimIntensity(v: String) = viewModelScope.launch { store.setAnimIntensity(v) }
    fun setNotifMorning(v: Boolean) = viewModelScope.launch { store.setNotifMorning(v) }
    fun setNotifRain(v: Boolean) = viewModelScope.launch { store.setNotifRain(v) }
    fun setNotifAlerts(v: Boolean) = viewModelScope.launch { store.setNotifAlerts(v) }

    fun loadCompare() = viewModelScope.launch {
        val loc = selected.value ?: return@launch
        _compare.value = CompareState(loading = true)
        val rows = repository.compareModels(
            loc, listOf("best_match", "ecmwf_ifs025", "icon_seamless", "gfs_seamless")
        )
        _compare.value = CompareState(rows = rows)
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
        addLocation(loc, onResult)
        clearSearch()
    }

    /** Adds a location resolved from GPS (Places "use my location"). */
    fun addManual(
        name: String,
        region: String?,
        country: String?,
        latitude: Double,
        longitude: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        addLocation(
            SavedLocation(
                id = SavedLocation.idFor(latitude, longitude),
                name = name, region = region, country = country,
                latitude = latitude, longitude = longitude, timezone = null
            ),
            onResult
        )
    }

    private fun addLocation(loc: SavedLocation, onResult: (Boolean, String) -> Unit) {
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
                }
            }
        }
    }

    fun remove(id: String) = viewModelScope.launch { store.remove(id) }

    /** Moves a saved place one position up or down. */
    fun move(id: String, up: Boolean) = viewModelScope.launch {
        val list = locations.value.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j < 0 || j >= list.size) return@launch
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        store.reorder(list)
    }

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
