package com.dockerdroid.app

import com.dockerdroid.app.core.install.CheckStatus
import com.dockerdroid.app.core.install.CompatibilityChecker
import com.dockerdroid.app.core.install.CompatibilityReport
import com.dockerdroid.app.core.shell.CommandResult
import com.dockerdroid.app.core.shell.RootShellManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CompatibilityCheckerTest {

    private fun shellReturning(map: Map<String, String>): RootShellManager {
        val shell = mockk<RootShellManager>()
        coEvery { shell.isRootAvailable() } returns true
        coEvery { shell.exec(any(), any()) } answers {
            val cmd = firstArg<String>()
            val out = map.entries.firstOrNull { cmd.contains(it.key) }?.value ?: ""
            CommandResult(0, out.lines(), emptyList())
        }
        return shell
    }

    @Test
    fun `recommends docker on a capable kernel`() = runTest {
        val shell = shellReturning(
            mapOf(
                "uname -m" to "aarch64",
                "df -m" to "8192",
                "cgroup.controllers" to "v2",
                "/proc/self/ns" to "pid net mnt uts ipc cgroup",
                "/proc/filesystems" to "nodev overlay",
                "which iptables" to "/system/bin/iptables",
                "getenforce" to "Permissive",
            ),
        )

        val report = CompatibilityChecker(shell).run()

        assertEquals(CompatibilityReport.Runtime.DOCKER, report.recommendedRuntime)
        assertEquals(CheckStatus.PASS, report.checks.first { it.key == "overlayfs" }.status)
    }

    @Test
    fun `falls back to podman when overlayfs and netfilter are missing`() = runTest {
        val shell = shellReturning(
            mapOf(
                "uname -m" to "aarch64",
                "df -m" to "8192",
                "cgroup.controllers" to "v2",
                "/proc/self/ns" to "pid net mnt uts ipc",
                "/proc/filesystems" to "nodev tmpfs", // no overlay
                "which iptables" to "", // no netfilter
                "getenforce" to "Permissive",
            ),
        )

        val report = CompatibilityChecker(shell).run()

        assertEquals(CompatibilityReport.Runtime.PODMAN, report.recommendedRuntime)
    }

    @Test
    fun `marks device unsupported without namespaces`() = runTest {
        val shell = shellReturning(
            mapOf(
                "uname -m" to "aarch64",
                "df -m" to "8192",
                "cgroup.controllers" to "v2",
                "/proc/self/ns" to "pid", // missing net/mnt/uts/ipc
                "/proc/filesystems" to "overlay",
                "which iptables" to "/system/bin/iptables",
                "getenforce" to "Permissive",
            ),
        )

        val report = CompatibilityChecker(shell).run()

        assertEquals(CompatibilityReport.Runtime.UNSUPPORTED, report.recommendedRuntime)
        assertEquals(true, report.hasBlockingFailure)
    }
}
