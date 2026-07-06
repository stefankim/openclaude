package com.dockerdroid.app.data.api

import com.dockerdroid.app.core.NotificationHelper
import com.dockerdroid.app.data.remote.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A single decoded Docker event (best-effort field extraction). */
data class DockerEvent(val type: String, val action: String, val name: String)

/**
 * Subscribes to the daemon's event stream and re-broadcasts container lifecycle
 * events, and raises notifications for crashes/OOM. ViewModels observe [events] to
 * refresh instantly instead of polling. Reconnects if the stream drops.
 */
class DockerEventsMonitor(
    private val connectionManager: ConnectionManager,
    private val notifications: NotificationHelper,
    private val scope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<DockerEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<DockerEvent> = _events

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                runCatching {
                    connectionManager.client.events().collect { line ->
                        parse(line)?.let { ev ->
                            _events.emit(ev)
                            when (ev.action) {
                                "die", "oom" -> notifications.notify("Container ${ev.action}", ev.name)
                            }
                        }
                    }
                }
                // Stream ended or failed (e.g. connection switch); back off and retry.
                delay(3_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun parse(json: String): DockerEvent? {
        val type = field(json, "Type") ?: return null
        if (type != "container") return null
        val action = field(json, "Action") ?: field(json, "status") ?: return null
        val name = field(json, "name") ?: field(json, "id")?.take(12) ?: ""
        return DockerEvent(type, action.substringBefore(":"), name)
    }

    /** Grab a top-level string field without a full JSON parse (events are flat-ish). */
    private fun field(json: String, key: String): String? =
        Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
}
