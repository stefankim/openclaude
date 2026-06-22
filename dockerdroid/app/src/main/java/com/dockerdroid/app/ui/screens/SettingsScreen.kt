package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.core.install.BinarySource
import com.dockerdroid.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val startOnBoot by viewModel.startOnBoot.collectAsState()
    val autoRestart by viewModel.autoRestart.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SettingToggle(
            title = "Start Docker on boot",
            subtitle = "Launch the engine automatically after the device restarts.",
            checked = startOnBoot,
            onChange = viewModel::setStartOnBoot,
        )
        HorizontalDivider()
        SettingToggle(
            title = "Auto-restart on crash",
            subtitle = "Supervise dockerd and relaunch it if it stops unexpectedly.",
            checked = autoRestart,
            onChange = viewModel::setAutoRestart,
        )
        HorizontalDivider()
        Text(
            "Docker Engine ${BinarySource.DOCKER_VERSION} · DockerDroid 0.1.0",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
