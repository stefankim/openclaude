package com.dockerdroid.app.vm

/**
 * A downloadable guest-VM asset, pinned to a verified SHA-256 (same model as the
 * native Docker installer's [com.dockerdroid.app.core.install.BinarySource]).
 */
data class VmAsset(
    val name: String,
    val url: String,
    val sha256: String,
    val fileName: String,
    val gzipped: Boolean = false,
)

object VmImages {
    /** Port the guest dockerd listens on; QEMU forwards 127.0.0.1:PORT → guest:PORT. */
    const val DOCKER_PORT = 2375

    /** Memory / CPUs handed to the guest. Conservative for phone RAM. */
    const val GUEST_MEM_MB = 2048
    const val GUEST_CPUS = 2

    /**
     * Guest = a minimal Linux kernel + a rootfs with Docker preinstalled, configured
     * to start `dockerd -H tcp://0.0.0.0:${DOCKER_PORT}` on boot. The rootfs is built
     * by `scripts/build-vm-assets.sh`; hashes are pinned per release by CI.
     */
    fun defaultArm64(): List<VmAsset> = listOf(
        VmAsset(
            name = "kernel",
            url = "https://github.com/stefankim/openclaude/releases/download/dockerdroid-vm-assets/vmlinuz-aarch64",
            sha256 = "REPLACED_AT_RELEASE",
            fileName = "vmlinuz",
        ),
        VmAsset(
            name = "rootfs",
            url = "https://github.com/stefankim/openclaude/releases/download/dockerdroid-vm-assets/docker-rootfs-arm64.img.gz",
            sha256 = "REPLACED_AT_RELEASE",
            fileName = "rootfs.img",
            gzipped = true,
        ),
    )
}
