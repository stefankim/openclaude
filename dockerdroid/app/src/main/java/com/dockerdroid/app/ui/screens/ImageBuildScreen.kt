package com.dockerdroid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.ui.viewmodel.ImageBuildViewModel

@Composable
fun ImageBuildScreen(viewModel: ImageBuildViewModel) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Build image", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = state.tag,
            onValueChange = viewModel::setTag,
            label = { Text("Tag") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.dockerfile,
            onValueChange = viewModel::setDockerfile,
            label = { Text("Dockerfile") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
        if (state.building) {
            CircularProgressIndicator()
        } else {
            Button(onClick = viewModel::build, modifier = Modifier.fillMaxWidth()) { Text("Build") }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(state.output) { line ->
                Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
