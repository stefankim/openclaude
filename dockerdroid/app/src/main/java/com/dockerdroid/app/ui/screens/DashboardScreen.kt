package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.core.service.DaemonState
import com.dockerdroid.app.data.remote.Connection
import com.dockerdroid.app.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenNetworks: () -> Unit = {},
    onOpenVolumes: () -> Unit = {},
) {
    val daemon by viewModel.daemonState.collectAsState()
    val counts by viewModel.counts.collectAsState()
    val connection by viewModel.connection.collectAsState()

    LaunchedEffect(daemon, connection) {
        // Refresh whenever we have a reachable daemon: any non-local connection, or a
        // running on-device daemon.
        if (connection !is Connection.Local || daemon is DaemonState.Running) viewModel.refresh()
    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        when (val c = connection) {
            is Connection.Remote -> RemoteCard(c.host.label)
            Connection.Vm -> RemoteCard("Local Docker VM")
            Connection.Local -> DaemonCard(daemon, onStart = viewModel::startDaemon, onStop = viewModel::stopDaemon)
        }
        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Containers", "${counts.running}/${counts.containers}", Modifier.weight(1f))
            StatCard("Images", counts.images.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenNetworks) { Text("Networks") }
            OutlinedButton(onClick = onOpenVolumes) { Text("Volumes") }
        }
    }
}

@Composable
private fun DaemonCard(state: DaemonState, onStart: () -> Unit, onStop: () -> Unit) {
    val label = when (state) {
        is DaemonState.Running -> "Running (pid ${state.pid})"
        DaemonState.Starting -> "Starting…"
        DaemonState.Stopped -> "Stopped"
        is DaemonState.Error -> "Error: ${state.message}"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Docker Engine", style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(12.dp))
            if (state.isActive) {
                OutlinedButton(onClick = onStop) { Text("Stop") }
            } else {
                Button(onClick = onStart) { Text("Start") }
            }
        }
    }
}

@Composable
private fun RemoteCard(label: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Remote Docker host", style = MaterialTheme.typography.titleMedium)
            Text("Connected to $label", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
