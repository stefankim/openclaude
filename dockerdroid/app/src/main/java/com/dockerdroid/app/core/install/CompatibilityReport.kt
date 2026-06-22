package com.dockerdroid.app.core.install

/** Severity of a single compatibility finding. */
enum class CheckStatus { PASS, WARN, FAIL }

/**
 * One probed kernel/userspace capability.
 *
 * @property remedy optional hint shown to the user when [status] is not [CheckStatus.PASS].
 */
data class CompatibilityCheck(
    val key: String,
    val label: String,
    val status: CheckStatus,
    val detail: String,
    val remedy: String? = null,
)

/**
 * Aggregated result of [CompatibilityChecker].
 *
 * Drives the "compatibility report" screen and decides whether the engine should
 * run native Docker or fall back to the rootless containerd/Podman path.
 */
data class CompatibilityReport(
    val checks: List<CompatibilityCheck>,
    val recommendedRuntime: Runtime,
) {
    val hasBlockingFailure: Boolean get() = checks.any { it.status == CheckStatus.FAIL }
    val warnings: List<CompatibilityCheck> get() = checks.filter { it.status == CheckStatus.WARN }

    enum class Runtime {
        /** Full Docker Engine (dockerd + containerd + runc). */
        DOCKER,

        /**
         * Daemonless fallback used when Docker-specific kernel features are missing
         * (e.g. no overlayfs, restricted cgroup v2 delegation, or hardened kernels).
         */
        PODMAN,

        /** Device cannot run containers at all. */
        UNSUPPORTED,
    }
}
