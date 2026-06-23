package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.core.install.CheckStatus
import com.dockerdroid.app.core.install.CompatibilityCheck
import com.dockerdroid.app.core.install.InstallProgress
import com.dockerdroid.app.ui.viewmodel.InstallViewModel

@Composable
fun WelcomeScreen(onContinue: () -> Unit, onConnectRemote: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("DockerDroid", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.size(8.dp))
        Text(
            "Run a full Docker Engine on your rooted Android device — or connect to a " +
                "remote Docker host over SSH (no root required).",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.size(32.dp))
        Button(onClick = onContinue) { Text("Set up on this device (root)") }
        Spacer(Modifier.size(12.dp))
        OutlinedButton(onClick = onConnectRemote) { Text("Connect to a remote host (SSH)") }
    }
}

/**
 * Combined root-detection + compatibility-report + one-tap-install flow. Kept on a
 * single scroll surface so the user sees the full picture before committing.
 */
@Composable
fun InstallScreen(viewModel: InstallViewModel, onInstalled: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Environment check", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(16.dp))

        when {
            state.checking -> CircularProgressIndicator()
            state.report == null -> Button(onClick = viewModel::runCompatibilityCheck) {
                Text("Run compatibility check")
            }
            else -> {
                val report = state.report!!
                report.checks.forEach { CheckRow(it) }
                Spacer(Modifier.size(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Recommended runtime: ${report.recommendedRuntime}",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.size(16.dp))

                when {
                    state.installed -> Button(onClick = onInstalled) { Text("Open dashboard") }
                    state.installing -> InstallProgressView(state.progress)
                    else -> Button(
                        onClick = viewModel::install,
                        enabled = !report.hasBlockingFailure,
                    ) { Text("Install with one tap") }
                }
            }
        }

        state.error?.let {
            Spacer(Modifier.size(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CheckRow(check: CompatibilityCheck) {
    val (icon, tint) = when (check.status) {
        CheckStatus.PASS -> Icons.Filled.CheckCircle to Color(0xFF2E7D32)
        CheckStatus.WARN -> Icons.Filled.Warning to Color(0xFFF9A825)
        CheckStatus.FAIL -> Icons.Filled.Error to MaterialTheme.colorScheme.error
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = check.status.name, tint = tint)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(check.label, style = MaterialTheme.typography.titleSmall)
            Text(check.detail, style = MaterialTheme.typography.bodySmall)
            check.remedy?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = tint) }
        }
    }
}

@Composable
private fun InstallProgressView(progress: InstallProgress?) {
    Column {
        when (progress) {
            is InstallProgress.Step -> {
                LinearProgressIndicator(
                    progress = { progress.index.toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                Text("${progress.index}/${progress.total} · ${progress.message}", fontFamily = FontFamily.Monospace)
            }
            else -> CircularProgressIndicator()
        }
    }
}
