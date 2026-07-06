package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.ui.viewmodel.NetworksViewModel
import com.dockerdroid.app.ui.viewmodel.VolumesViewModel

@Composable
fun NetworksScreen(viewModel: NetworksViewModel) {
    val items by viewModel.items.collectAsState()
    var name by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CreateRow("network name", name, { name = it }) { viewModel.create(name); name = "" }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { n ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(n.name, style = MaterialTheme.typography.titleMedium)
                        Text("${n.driver} · ${n.scope}", style = MaterialTheme.typography.bodySmall)
                        if (n.name !in setOf("bridge", "host", "none")) {
                            TextButton(onClick = { viewModel.remove(n.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolumesScreen(viewModel: VolumesViewModel) {
    val items by viewModel.items.collectAsState()
    var name by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CreateRow("volume name", name, { name = it }) { viewModel.create(name); name = "" }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.name }) { v ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(v.name, style = MaterialTheme.typography.titleMedium)
                        Text(v.mountpoint, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.remove(v.name) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRow(label: String, value: String, onChange: (String) -> Unit, onCreate: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onCreate, modifier = Modifier.padding(start = 8.dp)) { Text("Create") }
    }
}
