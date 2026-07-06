package com.openclaude.weather.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openclaude.weather.R
import com.openclaude.weather.domain.WeatherScene
import com.openclaude.weather.ui.ForecastState
import com.openclaude.weather.ui.WeatherViewModel
import com.openclaude.weather.ui.daily.ExtendedScreen
import com.openclaude.weather.ui.daily.WeekScreen
import com.openclaude.weather.ui.locations.LocationsScreen
import com.openclaude.weather.ui.radar.RadarScreen
import com.openclaude.weather.ui.radar.RadarViewModel
import com.openclaude.weather.ui.settings.SettingsScreen
import com.openclaude.weather.ui.theme.sceneGradient
import com.openclaude.weather.ui.today.TodayScreen
import com.openclaude.weather.util.Units
import kotlinx.coroutines.launch

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, Icons.Filled.WbSunny),
    WEEK(R.string.tab_week, Icons.Filled.CalendarViewWeek),
    EXTENDED(R.string.tab_extended, Icons.Filled.CalendarMonth),
    RADAR(R.string.tab_radar, Icons.Filled.Radar),
    LOCATIONS(R.string.tab_places, Icons.Filled.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AppScaffold(
    weatherVm: WeatherViewModel,
    radarVm: RadarViewModel
) {
    val locations by weatherVm.locations.collectAsState()
    val selected by weatherVm.selected.collectAsState()
    val forecastState by weatherVm.forecast.collectAsState()
    val search by weatherVm.search.collectAsState()
    val model by weatherVm.model.collectAsState()
    val settings by weatherVm.settings.collectAsState()
    val alerts by weatherVm.alerts.collectAsState()
    val air by weatherVm.air.collectAsState()
    val previews by weatherVm.previews.collectAsState()
    val compare by weatherVm.compare.collectAsState()
    val radarState by radarVm.state.collectAsState()
    val radarForecast by radarVm.forecast.collectAsState()

    var tab by remember { mutableStateOf(Tab.TODAY) }
    var showSettings by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Keep the process-wide display units in sync with persisted settings.
    LaunchedEffect(settings.tempUnit, settings.windUnit) {
        Units.tempUnit = settings.tempUnit
        Units.windUnit = settings.windUnit
    }
    val animIntensity = when (settings.animIntensity) {
        "low" -> 0.5f
        "high" -> 1.5f
        else -> 1f
    }

    // Background scene follows current weather; sensible default before data loads.
    val (scene, isDay) = when (val s = forecastState) {
        is ForecastState.Success -> s.forecast.current.condition.scene to s.forecast.current.isDay
        else -> WeatherScene.CLEAR_DAY to true
    }

    val refreshing = forecastState is ForecastState.Loading
    val pullState = rememberPullRefreshState(refreshing = refreshing, onRefresh = { weatherVm.refresh() })

    Box(modifier = Modifier.fillMaxSize().background(sceneGradient(scene, isDay))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (showSettings) stringResource(R.string.settings)
                            else selected?.displayName ?: stringResource(R.string.app_name),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        if (!showSettings && tab != Tab.LOCATIONS && tab != Tab.RADAR) {
                            IconButton(onClick = { weatherVm.refresh() }) {
                                Icon(Icons.Filled.Refresh, stringResource(R.string.refresh), tint = Color.White)
                            }
                        }
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Filled.Settings, stringResource(R.string.settings), tint = Color.White)
                        }
                    }
                )
            },
            bottomBar = {
                if (!showSettings) {
                    NavigationBar(containerColor = Color(0x33000000)) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = stringResource(t.labelRes)) },
                                label = { Text(stringResource(t.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = Color.White.copy(alpha = 0.22f),
                                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showSettings) {
                    SettingsScreen(
                        settings = settings,
                        model = model,
                        compare = compare,
                        onBack = { showSettings = false },
                        onSetTempUnit = weatherVm::setTempUnit,
                        onSetWindUnit = weatherVm::setWindUnit,
                        onSetModel = weatherVm::setModel,
                        onSetAnimIntensity = weatherVm::setAnimIntensity,
                        onSetNotifMorning = weatherVm::setNotifMorning,
                        onSetNotifRain = weatherVm::setNotifRain,
                        onSetNotifAlerts = weatherVm::setNotifAlerts,
                        onLoadCompare = weatherVm::loadCompare
                    )
                    return@Box
                }
                when (tab) {
                    Tab.LOCATIONS -> LocationsScreen(
                        locations = locations,
                        selectedId = selected?.id,
                        search = search,
                        previews = previews,
                        onQueryChange = weatherVm::onQueryChange,
                        onAdd = { result ->
                            weatherVm.addFromSearch(result) { _, msg ->
                                scope.launch { snackbar.showSnackbar(msg) }
                            }
                        },
                        onAddManual = { name, region, country, lat, lon ->
                            weatherVm.addManual(name, region, country, lat, lon) { _, msg ->
                                scope.launch { snackbar.showSnackbar(msg) }
                            }
                        },
                        onSelect = { weatherVm.select(it); tab = Tab.TODAY },
                        onRemove = weatherVm::remove,
                        onMove = weatherVm::move,
                        onMessage = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                    )

                    Tab.RADAR -> RadarScreen(
                        state = radarState,
                        forecast = radarForecast,
                        location = selected,
                        onRetry = { radarVm.load() },
                        onLoadForecast = { lat, lon, fine -> radarVm.loadForecast(lat, lon, fine) }
                    )

                    else -> {
                        // Swipe-down refreshes the forecast tabs.
                        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullState)) {
                            if (locations.isEmpty()) {
                                EmptyPrompt { tab = Tab.LOCATIONS }
                            } else {
                                when (val s = forecastState) {
                                    is ForecastState.Success -> when (tab) {
                                        Tab.TODAY -> selected?.let {
                                            TodayScreen(it, s.forecast, alerts, air, animIntensity)
                                        }
                                        Tab.WEEK -> WeekScreen(s.forecast)
                                        Tab.EXTENDED -> ExtendedScreen(s.forecast)
                                        else -> Unit
                                    }
                                    is ForecastState.Error -> ErrorView(s.message) { weatherVm.refresh() }
                                    else -> CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = Color.White
                                    )
                                }
                            }
                            PullRefreshIndicator(
                                refreshing = refreshing,
                                state = pullState,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPrompt(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.padding(8.dp))
        Text(stringResource(R.string.add_location_title), color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.add_location_body),
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 8.dp)
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.LocationOn, stringResource(R.string.add), tint = Color(0xFF9FD0FF))
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = Color.White, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, stringResource(R.string.retry), tint = Color.White)
        }
    }
}
