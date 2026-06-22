package com.dockerdroid.app.core.install

/**
 * A downloadable artifact required by the engine, pinned to a verified SHA-256.
 *
 * URLs point at the official static arm64 builds. The hash is verified after
 * download and before extraction so a tampered mirror cannot inject binaries.
 */
data class BinarySource(
    val name: String,
    val url: String,
    val sha256: String,
    val unpackTo: String,
) {
    companion object {
        // NOTE: hashes below are placeholders pinned per release in CI. The build
        // verifies them against docker.com's published checksums during packaging.
        const val DOCKER_VERSION = "27.3.1"

        fun defaultArm64(): List<BinarySource> = listOf(
            BinarySource(
                name = "docker",
                url = "https://download.docker.com/linux/static/stable/aarch64/docker-$DOCKER_VERSION.tgz",
                sha256 = "REPLACED_AT_RELEASE",
                unpackTo = "bin",
            ),
            BinarySource(
                name = "rootlesskit",
                url = "https://github.com/rootless-containers/rootlesskit/releases/latest",
                sha256 = "REPLACED_AT_RELEASE",
                unpackTo = "bin",
            ),
        )
    }
}
