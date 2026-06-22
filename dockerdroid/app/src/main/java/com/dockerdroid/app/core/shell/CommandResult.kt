package com.dockerdroid.app.core.shell

/**
 * Outcome of a finished root command.
 *
 * @property code process exit code; [SHELL_DIED] when the root shell itself was lost.
 * @property stdout captured standard-output lines (only populated for buffered calls).
 * @property stderr captured standard-error lines.
 */
data class CommandResult(
    val code: Int,
    val stdout: List<String>,
    val stderr: List<String>,
) {
    val isSuccess: Boolean get() = code == 0
    val out: String get() = stdout.joinToString("\n")
    val err: String get() = stderr.joinToString("\n")

    companion object {
        const val SHELL_DIED = -1
        const val TIMED_OUT = -2
    }
}

/** A streamed line of output, tagged by which stream it came from. */
sealed interface ShellLine {
    val text: String

    data class Stdout(override val text: String) : ShellLine
    data class Stderr(override val text: String) : ShellLine
}
