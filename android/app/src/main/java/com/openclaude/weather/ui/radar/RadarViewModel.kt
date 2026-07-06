package com.openclaude.weather.ui.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openclaude.weather.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RadarFrameUi(val time: Long, val tileUrlTemplate: String, val isForecast: Boolean)

data class RadarUiState(
    val loading: Boolean = true,
    val frames: List<RadarFrameUi> = emptyList(),
    val error: String? = null
)

data class PrecipForecastState(
    val loading: Boolean = false,
    val grid: WeatherRepository.PrecipGrid? = null,
    val error: String? = null
)

class RadarViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val _state = MutableStateFlow(RadarUiState())
    val state: StateFlow<RadarUiState> = _state.asStateFlow()

    private val _forecast = MutableStateFlow(PrecipForecastState())
    val forecast: StateFlow<PrecipForecastState> = _forecast.asStateFlow()

    private var lastForecastKey: String? = null

    init { load() }

    /** Loads the native precipitation forecast grid centred on the given point (cached). */
    fun loadForecast(lat: Double, lon: Double, fine: Boolean = false) {
        val key = "%.2f,%.2f,%b".format(lat, lon, fine)
        if (key == lastForecastKey && _forecast.value.grid != null) return
        lastForecastKey = key
        // Keep showing the previous grid while the finer/coarser one loads.
        _forecast.value = _forecast.value.copy(loading = _forecast.value.grid == null, error = null)
        viewModelScope.launch {
            runCatching { repository.precipForecast(lat, lon, fine) }
                .onSuccess { _forecast.value = PrecipForecastState(grid = it) }
                .onFailure {
                    lastForecastKey = null
                    if (_forecast.value.grid == null) {
                        _forecast.value = PrecipForecastState(error = it.message ?: "Forecast unavailable")
                    }
                }
        }
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.radarFrames() }
                .onSuccess { data ->
                    val frames = data.frames.mapIndexed { index, f ->
                        // RainViewer tile template: host + path + /256/{z}/{x}/{y}/2/1_1.png
                        RadarFrameUi(
                            time = f.time,
                            tileUrlTemplate = "${data.host}${f.path}/256/%d/%d/%d/2/1_1.png",
                            isForecast = index >= data.nowcastFrom
                        )
                    }
                    _state.value = RadarUiState(loading = false, frames = frames)
                }
                .onFailure {
                    _state.value = RadarUiState(loading = false, error = it.message ?: "Radar unavailable")
                }
        }
    }

    class Factory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RadarViewModel(repository) as T
    }
}
