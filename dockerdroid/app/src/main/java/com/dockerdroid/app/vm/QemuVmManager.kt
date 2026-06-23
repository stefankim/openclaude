package com.dockerdroid.app.vm

import android.content.Context
import com.dockerdroid.app.data.api.DockerApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runs a real Docker daemon on the phone **without root** by booting a guest Linux
 * kernel in QEMU (user-mode, software-emulated — no `/dev/kvm` needed). The guest's
 * dockerd is exposed only on `127.0.0.1:${VmImages.DOCKER_PORT}` via QEMU's port
 * forward, and the app talks to it with [DockerApiClient.tcp].
 *
 * QEMU itself is a native executable; to satisfy Android's W^X / SELinux rules it must
 * ship inside the APK as a jniLib (`nativeLibraryDir`), not be downloaded. When that
 * payload is absent the manager fails fast with a clear message — see docs/VM.md and
 * scripts/build-vm-assets.sh for the build pipeline that produces it.
 */
class QemuVmManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val provisioner: VmProvisioner = VmProvisioner(context),
) {
    private val _state = MutableStateFlow<VmState>(VmState.Stopped)
    val state: StateFlow<VmState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    @Volatile private var process: Process? = null

    /** The QEMU binary, shipped as a jniLib so it lives in an executable directory. */
    private fun qemuBinary(): File =
        File(context.applicationInfo.nativeLibraryDir, "libqemu-system-aarch64.so")

    suspend fun start() {
        if (_state.value.isActive) return

        val qemu = qemuBinary()
        if (!qemu.exists()) {
            _state.value = VmState.Error(
                "QEMU engine not bundled in this build. The VM backend is wired up, but " +
                    "the native qemu-system-aarch64 jniLib must be added (see docs/VM.md).",
            )
            return
        }

        _state.value = VmState.Provisioning("Checking guest image")
        val provisioned = provisioner.ensure { msg -> _state.value = VmState.Provisioning(msg) }
        if (provisioned.isFailure) {
            _state.value = VmState.Error("Guest image unavailable: ${provisioned.exceptionOrNull()?.message}")
            return
        }

        _state.value = VmState.Booting
        val started = runCatching { launchQemu(qemu) }
        if (started.isFailure) {
            _state.value = VmState.Error("Failed to start QEMU: ${started.exceptionOrNull()?.message}")
            return
        }
        awaitDockerReady()
    }

    private suspend fun launchQemu(qemu: File) = withContext(Dispatchers.IO) {
        val kernel = provisioner.file("vmlinuz")
        val rootfs = provisioner.file("rootfs.img")
        val args = listOf(
            qemu.absolutePath,
            "-machine", "virt",
            "-cpu", "max",
            "-smp", VmImages.GUEST_CPUS.toString(),
            "-m", VmImages.GUEST_MEM_MB.toString(),
            "-kernel", kernel.absolutePath,
            "-drive", "file=${rootfs.absolutePath},if=virtio,format=raw",
            "-append", "console=ttyAMA0 root=/dev/vda rw quiet",
            "-netdev", "user,id=n0,hostfwd=tcp:127.0.0.1:${VmImages.DOCKER_PORT}-:${VmImages.DOCKER_PORT}",
            "-device", "virtio-net-pci,netdev=n0",
            "-nographic",
        )
        val proc = ProcessBuilder(args)
            .directory(provisioner.dir())
            .redirectErrorStream(true)
            .start()
        process = proc

        // Drain the serial console into the log buffer for the VM screen.
        scope.launch(Dispatchers.IO) {
            proc.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> _log.value = (_log.value + line).takeLast(MAX_LOG_LINES) }
            }
        }
    }

    private suspend fun awaitDockerReady() {
        val probe = DockerApiClient.tcp("127.0.0.1", VmImages.DOCKER_PORT)
        repeat(BOOT_WAIT_TRIES) {
            if (process?.isAlive != true) {
                _state.value = VmState.Error("QEMU exited during boot. See the VM log.")
                return
            }
            if (probe.ping()) {
                _state.value = VmState.Running
                return
            }
            delay(2_000)
        }
        _state.value = VmState.Error("Timed out waiting for the guest Docker daemon.")
    }

    fun stop() {
        process?.destroy()
        process = null
        _state.value = VmState.Stopped
    }

    private companion object {
        const val BOOT_WAIT_TRIES = 60 // ~2 min; software emulation boots slowly
        const val MAX_LOG_LINES = 500
    }
}
