package com.dockerdroid.app.core.install

import com.dockerdroid.app.core.shell.RootShellManager

/**
 * Probes the kernel and userspace for the features Docker needs.
 *
 * The single hardest problem for "Docker on Android" is kernel variance: vendors
 * ship kernels with cgroups, namespaces, overlayfs, or netfilter compiled out.
 * This checker turns those unknowns into an explicit report and chooses a runtime
 * ([CompatibilityReport.Runtime]) rather than letting `dockerd` fail cryptically.
 */
class CompatibilityChecker(private val shell: RootShellManager) {

    suspend fun run(): CompatibilityReport {
        val checks = buildList {
            add(checkRoot())
            add(checkArchitecture())
            add(checkStorage())
            add(checkCgroups())
            add(checkNamespaces())
            add(checkOverlayfs())
            add(checkNetfilter())
            add(checkSeLinux())
        }
        return CompatibilityReport(checks, decideRuntime(checks))
    }

    private suspend fun checkRoot(): CompatibilityCheck {
        val ok = shell.isRootAvailable()
        return CompatibilityCheck(
            key = "root",
            label = "Root access",
            status = if (ok) CheckStatus.PASS else CheckStatus.FAIL,
            detail = if (ok) "Root shell granted" else "No root shell (Magisk required)",
            remedy = "Install Magisk and grant DockerDroid root access.".takeIf { !ok },
        )
    }

    private suspend fun checkArchitecture(): CompatibilityCheck {
        val arch = shell.exec("uname -m").out.trim()
        val ok = arch in setOf("aarch64", "arm64")
        return CompatibilityCheck(
            key = "arch",
            label = "CPU architecture",
            status = if (ok) CheckStatus.PASS else CheckStatus.FAIL,
            detail = arch.ifBlank { "unknown" },
            remedy = "DockerDroid binaries target arm64-v8a only.".takeIf { !ok },
        )
    }

    private suspend fun checkStorage(): CompatibilityCheck {
        // Available MiB on the data partition.
        val freeMib = shell.exec("df -m /data | awk 'NR==2 {print \$4}'").out.trim().toLongOrNull() ?: 0
        val ok = freeMib >= MIN_FREE_MIB
        return CompatibilityCheck(
            key = "storage",
            label = "Free storage",
            status = if (ok) CheckStatus.PASS else CheckStatus.WARN,
            detail = "${freeMib} MiB free on /data",
            remedy = "At least ${MIN_FREE_MIB} MiB recommended for images.".takeIf { !ok },
        )
    }

    private suspend fun checkCgroups(): CompatibilityCheck {
        val v2 = shell.exec("test -f /sys/fs/cgroup/cgroup.controllers && echo v2").out.contains("v2")
        val v1 = shell.exec("test -d /sys/fs/cgroup/memory && echo v1").out.contains("v1")
        return when {
            v2 -> pass("cgroups", "Control groups", "cgroup v2 (unified)")
            v1 -> CompatibilityCheck("cgroups", "Control groups", CheckStatus.WARN, "cgroup v1 only", "Resource limits may be partial.")
            else -> fail("cgroups", "Control groups", "No cgroup hierarchy", "Kernel must enable CONFIG_CGROUPS.")
        }
    }

    private suspend fun checkNamespaces(): CompatibilityCheck {
        val present = shell.exec("ls /proc/self/ns").out
        val required = listOf("pid", "net", "mnt", "uts", "ipc")
        val missing = required.filter { !present.contains(it) }
        return if (missing.isEmpty()) {
            pass("namespaces", "Namespaces", "pid/net/mnt/uts/ipc available")
        } else {
            fail("namespaces", "Namespaces", "Missing: ${missing.joinToString()}", "Kernel lacks CONFIG_NAMESPACES.")
        }
    }

    private suspend fun checkOverlayfs(): CompatibilityCheck {
        // Read the kernel's registered filesystems directly; overlayfs is listed here
        // when CONFIG_OVERLAY_FS is built in or loaded.
        val supported = shell.exec("cat /proc/filesystems").out.contains("overlay")
        return if (supported) {
            pass("overlayfs", "Storage driver", "overlayfs available")
        } else {
            // Not fatal — we can fall back to the vfs storage driver (slow but works).
            CompatibilityCheck("overlayfs", "Storage driver", CheckStatus.WARN, "overlayfs missing", "Falling back to vfs (slower, more disk).")
        }
    }

    private suspend fun checkNetfilter(): CompatibilityCheck {
        val iptables = shell.exec("which iptables nft 2>/dev/null").out.isNotBlank()
        return if (iptables) {
            pass("netfilter", "Networking", "iptables/nftables present")
        } else {
            CompatibilityCheck("netfilter", "Networking", CheckStatus.WARN, "No iptables/nft", "Bridge networking disabled; use host networking.")
        }
    }

    private suspend fun checkSeLinux(): CompatibilityCheck {
        val mode = shell.exec("getenforce").out.trim()
        return if (mode.equals("Enforcing", ignoreCase = true)) {
            CompatibilityCheck("selinux", "SELinux", CheckStatus.WARN, "Enforcing", "Per-container SELinux labels are not applied.")
        } else {
            pass("selinux", "SELinux", mode.ifBlank { "Permissive" })
        }
    }

    private fun decideRuntime(checks: List<CompatibilityCheck>): CompatibilityReport.Runtime {
        fun status(key: String) = checks.first { it.key == key }.status
        if (status("root") == CheckStatus.FAIL ||
            status("arch") == CheckStatus.FAIL ||
            status("namespaces") == CheckStatus.FAIL
        ) {
            return CompatibilityReport.Runtime.UNSUPPORTED
        }
        // Hardened/locked-down kernels that fail overlayfs *and* netfilter run far
        // more reliably under rootless Podman, so recommend the fallback.
        val overlayMissing = status("overlayfs") != CheckStatus.PASS
        val netMissing = status("netfilter") != CheckStatus.PASS
        return if (overlayMissing && netMissing) {
            CompatibilityReport.Runtime.PODMAN
        } else {
            CompatibilityReport.Runtime.DOCKER
        }
    }

    private fun pass(key: String, label: String, detail: String) =
        CompatibilityCheck(key, label, CheckStatus.PASS, detail)

    private fun fail(key: String, label: String, detail: String, remedy: String) =
        CompatibilityCheck(key, label, CheckStatus.FAIL, detail, remedy)

    companion object {
        private const val MIN_FREE_MIB = 2_048L
    }
}
