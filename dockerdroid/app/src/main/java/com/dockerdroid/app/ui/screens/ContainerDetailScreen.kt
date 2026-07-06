package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.ui.viewmodel.ContainerDetailViewModel

@Composable
fun ContainerDetailScreen(viewModel: ContainerDetailViewModel, containerId: String) {
    LaunchedEffect(containerId) { viewModel.start(containerId) }
    val inspect by viewModel.inspect.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val stat by viewModel.stat.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Text(
            inspect?.name?.trimStart('/') ?: containerId.take(12),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        TabRow(selectedTabIndex = tab) {
            listOf("Info", "Logs", "Stats", "Exec").forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> InfoTab(viewModel)
            1 -> LogsTab(logs)
            2 -> StatsTab(stat)
            else -> ExecTab(viewModel)
        }
    }
}

@Composable
private fun ExecTab(viewModel: ContainerDetailViewModel) {
    val output by viewModel.execOutput.collectAsState()
    var cmd by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(output) { line ->
                Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = cmd,
                onValueChange = { cmd = it },
                label = { Text("command") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { viewModel.exec(cmd); cmd = "" },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("Run") }
        }
    }
}

@Composable
private fun InfoTab(viewModel: ContainerDetailViewModel) {
    val inspect by viewModel.inspect.collectAsState()
    val i = inspect
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        if (i == null) {
            item { Text("Loading…") }
            return@LazyColumn
        }
        item { KeyVal("Image", i.config?.image ?: "—") }
        item { KeyVal("Status", i.state?.status ?: "—") }
        item { KeyVal("Command", i.config?.cmd?.joinToString(" ") ?: "—") }
        item { Text("Environment", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp)) }
        items(i.config?.env ?: emptyList()) { e ->
            Text(e, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
        item { Text("Mounts", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp)) }
        items(i.mounts) { m ->
            Text("${m.source} → ${m.destination} (${m.type})", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LogsTab(logs: List<String>) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(logs) { line ->
            Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatsTab(stat: ContainerDetailViewModel.Stat) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("CPU: %.1f%%".format(stat.cpuPercent), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { (stat.cpuPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        val memFrac = if (stat.memLimitMb > 0) stat.memUsedMb.toDouble() / stat.memLimitMb else 0.0
        Text("Memory: ${stat.memUsedMb} / ${stat.memLimitMb} MB", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { memFrac.coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text("Network: ↓ ${stat.rxKb} KB · ↑ ${stat.txKb} KB", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun KeyVal(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$key: ", style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}
