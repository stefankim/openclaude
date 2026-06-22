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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.ui.viewmodel.ImagesViewModel

@Composable
fun ImagesScreen(viewModel: ImagesViewModel) {
    val state by viewModel.state.collectAsState()
    var reference by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("image:tag") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                onClick = { if (reference.isNotBlank()) viewModel.pull(reference.trim()) },
                enabled = !state.pulling,
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("Pull") }
        }
        if (state.pulling) {
            Text(state.pullStatus, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }

        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.images, key = { it.id }) { image ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(image.repoTags?.firstOrNull() ?: image.id.removePrefix("sha256:").take(12))
                        Text("${image.size / (1024 * 1024)} MB", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.remove(image.id) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}
