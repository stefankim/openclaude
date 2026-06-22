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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dockerdroid.app.core.shell.RootShellManager
import com.dockerdroid.app.core.shell.ShellLine
import kotlinx.coroutines.launch

/**
 * Lightweight terminal. Streams each command's stdout/stderr from [RootShellManager]
 * into a scrolling buffer. A full VT100 emulator (Termux's `terminal-view`) can be
 * dropped in here later; this keeps the dependency surface minimal for v1.
 */
@Composable
fun TerminalScreen(shell: RootShellManager) {
    val lines = remember { mutableStateListOf("DockerDroid shell — root @ /data/local/docker") }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(lines) { line ->
                Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("command") },
            )
            Button(
                modifier = Modifier.padding(start = 8.dp),
                onClick = {
                    val cmd = input.trim()
                    if (cmd.isEmpty()) return@Button
                    lines += "$ $cmd"
                    input = ""
                    scope.launch {
                        shell.stream(cmd).collect { l ->
                            lines += when (l) {
                                is ShellLine.Stdout -> l.text
                                is ShellLine.Stderr -> "! ${l.text}"
                            }
                        }
                    }
                },
            ) { Text("Run") }
        }
    }
}
