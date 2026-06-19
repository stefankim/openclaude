package com.openclaude.weather.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openclaude.weather.domain.WeatherScene
import com.openclaude.weather.ui.ForecastState
import com.openclaude.weather.ui.WeatherViewModel
import com.openclaude.weather.ui.radar.RadarViewModel
import com.openclaude.weather.ui.daily.ExtendedScreen
import com.openclaude.weather.ui.daily.WeekScreen
import com.openclaude.weather.ui.locations.LocationsScreen
import com.openclaude.weather.ui.radar.RadarScreen
import com.openclaude.weather.ui.theme.sceneGradient
import com.openclaude.weather.ui.today.TodayScreen
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.WbSunny),
    WEEK("Week", Icons.Filled.CalendarViewWeek),
    EXTENDED("Extended", Icons.Filled.CalendarMonth),
    RADAR("Radar", Icons.Filled.Radar),
    LOCATIONS("Places", Icons.Filled.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    weatherVm: WeatherViewModel,
    radarVm: RadarViewModel
) {
    val locations by weatherVm.locations.collectAsState()
    val selected by weatherVm.selected.collectAsState()
    val forecastState by weatherVm.forecast.collectAsState()
    val search by weatherVm.search.collectAsState()
    val radarState by radarVm.state.collectAsState()

    var tab by remember { mutableStateOf(Tab.TODAY) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Background scene follows current weather; sensible default before data loads.
    val (scene, isDay) = when (val s = forecastState) {
        is ForecastState.Success -> s.forecast.current.condition.scene to s.forecast.current.isDay
        else -> WeatherScene.CLEAR_DAY to true
    }

    Box(modifier = Modifier.fillMaxSize().background(sceneGradient(scene, isDay))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            selected?.displayName ?: "Weather & Radar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        if (tab != Tab.LOCATIONS && tab != Tab.RADAR) {
                            IconButton(onClick = { weatherVm.refresh() }) {
                                Icon(Icons.Filled.Refresh, "Refresh", tint = Color.White)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0x33000000)) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
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
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.LOCATIONS -> LocationsScreen(
                        locations = locations,
                        selectedId = selected?.id,
                        search = search,
                        onQueryChange = weatherVm::onQueryChange,
                        onAdd = { result ->
                            weatherVm.addFromSearch(result) { _, msg ->
                                scope.launch { snackbar.showSnackbar(msg) }
                            }
                        },
                        onSelect = { weatherVm.select(it); tab = Tab.TODAY },
                        onRemove = weatherVm::remove
                    )

                    Tab.RADAR -> RadarScreen(
                        state = radarState,
                        location = selected,
                        onRetry = { radarVm.load() }
                    )

                    else -> {
                        if (locations.isEmpty()) {
                            EmptyPrompt { tab = Tab.LOCATIONS }
                        } else {
                            when (val s = forecastState) {
                                is ForecastState.Success -> when (tab) {
                                    Tab.TODAY -> selected?.let { TodayScreen(it, s.forecast) }
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
        Text("Add a location to get started", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "Tap the Places tab and search any city in Slovakia or worldwide.",
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 8.dp)
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.LocationOn, "Add", tint = Color(0xFF9FD0FF))
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
            Icon(Icons.Filled.Refresh, "Retry", tint = Color.White)
        }
    }
}
