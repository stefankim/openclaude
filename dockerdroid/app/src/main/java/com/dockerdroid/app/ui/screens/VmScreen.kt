package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.vm.VmState
import com.dockerdroid.app.ui.viewmodel.VmViewModel

/**
 * Boots a real Docker daemon inside an on-device QEMU VM (no root). Software
 * emulation is slow but works on any device; the daemon is reached over loopback.
 */
@Composable
fun VmScreen(viewModel: VmViewModel, onRunning: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val log by viewModel.log.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Local Docker VM", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Runs a real Docker Engine in a Linux VM on this device — no root required. " +
                "Note: without hardware virtualization this is slow; best for light testing.",
            style = MaterialTheme.typography.bodySmall,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                val label = when (val s = state) {
                    VmState.Stopped -> "Stopped"
                    is VmState.Provisioning -> "Provisioning: ${s.message}"
                    VmState.Booting -> "Booting the guest…"
                    VmState.Running -> "Running"
                    is VmState.Error -> "Error: ${s.message}"
                }
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }

        when (state) {
            is VmState.Provisioning, VmState.Booting -> CircularProgressIndicator()
            VmState.Running -> {
                Button(onClick = onRunning, modifier = Modifier.fillMaxWidth()) { Text("Open dashboard") }
                OutlinedButton(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) { Text("Stop VM") }
            }
            else -> Button(onClick = viewModel::start, modifier = Modifier.fillMaxWidth()) { Text("Start VM") }
        }

        if (log.isNotEmpty()) {
            Text("Console", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(log) { line ->
                    Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
