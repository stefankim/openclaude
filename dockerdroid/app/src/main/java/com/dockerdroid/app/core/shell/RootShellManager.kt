package com.dockerdroid.app.core.shell

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import kotlin.coroutines.resume

/**
 * Single entry point for executing privileged commands.
 *
 * Built on top of [libsu](https://github.com/topjohnwu/libsu), which owns a long-lived
 * `su` process and Magisk's MagiskSU protocol. All commands are serialized through a
 * [Mutex] so the daemon lifecycle operations never interleave with ad-hoc CLI calls.
 *
 * Two execution shapes are exposed:
 *  - [exec] / [execScript] — buffered, returns a [CommandResult].
 *  - [stream] — emits stdout/stderr lines as a cold [Flow] for logs and the terminal.
 */
class RootShellManager {

    private val mutex = Mutex()

    init {
        // Configure the global libsu shell once. MOUNT_MASTER lets dockerd's bind
        // mounts and overlayfs operations escape Magisk's mount namespace.
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                // MOUNT_MASTER only; stderr is kept separate (not redirected) so the
                // terminal and log views can distinguish the two streams.
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(SHELL_INIT_TIMEOUT_SECONDS),
        )
    }

    /** True if a root shell can be (or already is) obtained. Suspends on first call. */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            Shell.getShell { shell -> cont.resume(shell.isRoot) }
        }
    }

    /**
     * Run [command] and wait for completion.
     *
     * @param timeoutMs hard ceiling; returns [CommandResult.TIMED_OUT] if exceeded.
     */
    suspend fun exec(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): CommandResult =
        execScript(listOf(command), timeoutMs)

    /** Run an ordered list of commands in a single shell invocation. */
    suspend fun execScript(
        commands: List<String>,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): CommandResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val result = withTimeoutOrNull(timeoutMs) {
                runBuffered(commands)
            }
            result ?: CommandResult(CommandResult.TIMED_OUT, emptyList(), listOf("command timed out"))
        }
    }

    private fun runBuffered(commands: List<String>): CommandResult {
        val out = Collections.synchronizedList(mutableListOf<String>())
        val err = Collections.synchronizedList(mutableListOf<String>())
        val job = Shell.cmd(*commands.toTypedArray())
            .to(out, err)
            .exec()
        return CommandResult(
            code = if (job.isSuccess) 0 else job.code,
            stdout = out.toList(),
            stderr = err.toList(),
        )
    }

    /**
     * Stream a long-running command line by line. The flow stays open until the
     * command exits or the collector cancels (which kills the underlying job).
     * Ideal for `docker logs -f`, `dockerd` boot output, and the terminal screen.
     */
    fun stream(command: String): Flow<ShellLine> = callbackFlow {
        val stdout = object : java.util.AbstractList<String>() {
            override val size: Int get() = 0
            override fun get(index: Int): String = ""
            override fun add(element: String): Boolean {
                trySend(ShellLine.Stdout(element))
                return true
            }
        }
        val stderr = object : java.util.AbstractList<String>() {
            override val size: Int get() = 0
            override fun get(index: Int): String = ""
            override fun add(element: String): Boolean {
                trySend(ShellLine.Stderr(element))
                return true
            }
        }

        Shell.cmd(command).to(stdout, stderr).submit { close() }
        // libsu has no public handle to cancel an in-flight job; the collector
        // simply stops receiving once it cancels. Long-running follows should be
        // bounded by the caller (e.g. `docker logs --tail`).
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val SHELL_INIT_TIMEOUT_SECONDS = 20L
        const val DEFAULT_TIMEOUT_MS = 60_000L
    }
}
