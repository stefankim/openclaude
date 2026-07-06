package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.ui.viewmodel.RemoteViewModel

/**
 * Connect the app to a remote Docker daemon over SSH. No root or on-device daemon is
 * involved — the phone is purely a client to (e.g.) Docker Desktop on a Mac.
 */
@Composable
fun RemoteConnectScreen(viewModel: RemoteViewModel, onConnected: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.connected) {
        if (state.connected) onConnected()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Connect to a remote host", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Point DockerDroid at a Docker Engine reachable over SSH (e.g. Docker " +
                "Desktop on your Mac). Enable Remote Login on the host first.",
            style = MaterialTheme.typography.bodySmall,
        )

        // One-tap reconnect to previously saved hosts.
        if (viewModel.savedHosts.isNotEmpty()) {
            Text("Saved hosts", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.savedHosts.forEach { host ->
                    AssistChip(
                        onClick = { viewModel.connectSaved(host) },
                        label = { Text(host.label) },
                    )
                }
            }
        }

        // Hosts discovered on the LAN via mDNS.
        val discovered by viewModel.discovered.collectAsState()
        if (discovered.isNotEmpty()) {
            Text("Found on your network", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                discovered.forEach { h ->
                    AssistChip(onClick = { viewModel.useDiscovered(h) }, label = { Text(h.name) })
                }
            }
        }

        // Pairing code: paste a dockerdroid:// URI (e.g. from a QR the host printed).
        var pairing by remember { mutableStateOf("") }
        OutlinedTextField(
            value = pairing,
            onValueChange = { pairing = it },
            label = { Text("Paste pairing code (dockerdroid://…)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { viewModel.applyPairingUri(pairing) }) { Text("Apply pairing code") }

        OutlinedTextField(
            value = state.host,
            onValueChange = { v -> viewModel.update { it.copy(host = v) } },
            label = { Text("Host (IP or hostname)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.port,
            onValueChange = { v -> viewModel.update { it.copy(port = v) } },
            label = { Text("SSH port") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = { v -> viewModel.update { it.copy(username = v) } },
            label = { Text("SSH username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.dialCommand,
            onValueChange = { v -> viewModel.update { it.copy(dialCommand = v) } },
            label = { Text("Docker dial command (advanced)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text("If \"docker: command not found\", use an absolute path, e.g. /opt/homebrew/bin/docker system dial-stdio")
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.authMethod == RemoteViewModel.AuthMethod.PASSWORD,
                onClick = { viewModel.update { it.copy(authMethod = RemoteViewModel.AuthMethod.PASSWORD) } },
                label = { Text("Password") },
            )
            FilterChip(
                selected = state.authMethod == RemoteViewModel.AuthMethod.KEY,
                onClick = { viewModel.update { it.copy(authMethod = RemoteViewModel.AuthMethod.KEY) } },
                label = { Text("Private key") },
            )
        }

        when (state.authMethod) {
            RemoteViewModel.AuthMethod.PASSWORD -> OutlinedTextField(
                value = state.secret,
                onValueChange = { v -> viewModel.update { it.copy(secret = v) } },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            RemoteViewModel.AuthMethod.KEY -> {
                OutlinedTextField(
                    value = state.secret,
                    onValueChange = { v -> viewModel.update { it.copy(secret = v) } },
                    label = { Text("Private key (PEM)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.passphrase,
                    onValueChange = { v -> viewModel.update { it.copy(passphrase = v) } },
                    label = { Text("Key passphrase (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.connecting) {
            CircularProgressIndicator()
        } else {
            Button(onClick = viewModel::connect, modifier = Modifier.fillMaxWidth()) {
                Text("Connect")
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
