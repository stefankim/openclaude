package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.ui.viewmodel.ComposeViewModel

/**
 * Two tabs: predefined templates (one-click deploy) and the user's imported stacks.
 * Importing a `.yml` from storage is wired by the host via [onImport].
 */
@Composable
fun ComposeScreen(viewModel: ComposeViewModel, onImport: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val projects by viewModel.projects.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Templates") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("My stacks") })
        }
        when (tab) {
            0 -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.templates, key = { it.id }) { template ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(template.title, style = MaterialTheme.typography.titleMedium)
                            Text(template.description, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { viewModel.deployTemplate(template) }) { Text("Deploy") }
                        }
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { TextButton(onClick = onImport) { Text("Import docker-compose.yml") } }
                items(projects, key = { it.id }) { project ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            Text(project.status.name, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.deploy(project.id) }) { Text("Deploy") }
                                TextButton(onClick = { viewModel.stop(project.id) }) { Text("Stop") }
                                TextButton(onClick = { viewModel.delete(project.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
