package com.dockerdroid.app.data.remote

/**
 * Connection details for a remote Docker Engine reached over SSH.
 *
 * The phone never runs a daemon in this mode; it tunnels to the host's existing
 * Docker socket using `docker system dial-stdio` (the same mechanism the Docker CLI
 * uses for `DOCKER_HOST=ssh://…`), so nothing is exposed on the network.
 */
data class RemoteHost(
    val label: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    /**
     * Command run on the remote that proxies stdin/stdout to the Docker socket.
     * The default works with Docker Desktop / Engine in the user's PATH; override
     * with an absolute path (e.g. `/usr/local/bin/docker system dial-stdio`) when
     * a non-interactive SSH session has a minimal PATH.
     */
    val dialCommand: String = "docker system dial-stdio",
)

/** How DockerDroid authenticates to the SSH host. */
sealed interface SshAuth {
    data class Password(val password: String) : SshAuth
    data class PrivateKey(val pem: String, val passphrase: String?) : SshAuth
}
