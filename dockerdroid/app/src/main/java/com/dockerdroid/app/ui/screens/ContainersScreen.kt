package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.data.api.models.ApiContainer
import com.dockerdroid.app.ui.viewmodel.ContainersViewModel

/**
 * @param onOpen navigate to a container's detail screen
 * @param onOpenPort open http://<host>:<port> for a published port
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContainersScreen(
    viewModel: ContainersViewModel,
    onOpen: (String) -> Unit = {},
    onOpenPort: (Int) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.containers.isEmpty()) {
            item { Text(state.error ?: "No containers yet.") }
        }
        items(state.containers, key = { it.id }) { c ->
            ContainerCard(
                container = c,
                onOpen = { onOpen(c.id) },
                onOpenPort = onOpenPort,
                onStart = { viewModel.start(c.id) },
                onStop = { viewModel.stop(c.id) },
                onRestart = { viewModel.restart(c.id) },
                onRemove = { viewModel.remove(c.id) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContainerCard(
    container: ApiContainer,
    onOpen: () -> Unit,
    onOpenPort: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onRemove: () -> Unit,
) {
    val running = container.state == "running"
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp)) {
            Text(container.names.firstOrNull()?.trimStart('/') ?: container.id.take(12),
                style = MaterialTheme.typography.titleMedium)
            Text(container.image, style = MaterialTheme.typography.bodySmall)
            Text(container.status, style = MaterialTheme.typography.bodySmall)

            // Published ports → tap to open in the browser.
            val published = container.ports.mapNotNull { it.publicPort }.distinct()
            if (published.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    published.forEach { port ->
                        AssistChip(onClick = { onOpenPort(port) }, label = { Text(":$port ↗") })
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (running) TextButton(onClick = onStop) { Text("Stop") }
                else TextButton(onClick = onStart) { Text("Start") }
                TextButton(onClick = onRestart) { Text("Restart") }
                TextButton(onClick = onRemove) { Text("Delete") }
            }
        }
    }
}
