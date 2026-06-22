package com.dockerdroid.app.compose

/** Minimal in-memory representation of a docker-compose file. */
data class ComposeFile(
    val version: String?,
    val services: Map<String, ComposeService>,
    val volumes: List<String> = emptyList(),
    val networks: List<String> = emptyList(),
)

data class ComposeService(
    val name: String,
    val image: String?,
    val command: List<String> = emptyList(),
    val environment: List<String> = emptyList(),
    val ports: List<PortMapping> = emptyList(),
    val volumes: List<String> = emptyList(),
    val restart: String? = null,
    val dependsOn: List<String> = emptyList(),
)

/** A `host:container[/proto]` port mapping. */
data class PortMapping(val host: String, val container: String, val protocol: String = "tcp") {
    val containerKey: String get() = "$container/$protocol"
}
