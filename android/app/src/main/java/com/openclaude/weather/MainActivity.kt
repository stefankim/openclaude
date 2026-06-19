package com.openclaude.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openclaude.weather.ui.WeatherViewModel
import com.openclaude.weather.ui.navigation.AppScaffold
import com.openclaude.weather.ui.radar.RadarViewModel
import com.openclaude.weather.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as WeatherApp).container

        setContent {
            WeatherTheme {
                val weatherVm: WeatherViewModel = viewModel(
                    factory = WeatherViewModel.Factory(container.repository, container.locationStore)
                )
                val radarVm: RadarViewModel = viewModel(
                    factory = RadarViewModel.Factory(container.repository)
                )
                AppScaffold(weatherVm = weatherVm, radarVm = radarVm)
            }
        }
    }
}
