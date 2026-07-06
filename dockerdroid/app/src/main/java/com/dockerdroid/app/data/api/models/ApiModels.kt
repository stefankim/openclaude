package com.dockerdroid.app.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Subset of the Docker Engine API responses DockerDroid consumes. */

/** `GET /containers/{id}/json` — inspect. */
@JsonClass(generateAdapter = true)
data class ApiContainerInspect(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String = "",
    @Json(name = "Config") val config: InspectConfig? = null,
    @Json(name = "State") val state: InspectState? = null,
    @Json(name = "Mounts") val mounts: List<InspectMount> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class InspectConfig(
    @Json(name = "Image") val image: String = "",
    @Json(name = "Env") val env: List<String> = emptyList(),
    @Json(name = "Cmd") val cmd: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class InspectState(
    @Json(name = "Status") val status: String = "",
    @Json(name = "Running") val running: Boolean = false,
    @Json(name = "StartedAt") val startedAt: String = "",
    @Json(name = "ExitCode") val exitCode: Int = 0,
)

@JsonClass(generateAdapter = true)
data class InspectMount(
    @Json(name = "Type") val type: String = "",
    @Json(name = "Source") val source: String = "",
    @Json(name = "Destination") val destination: String = "",
)

/** Request body for `POST /containers/{id}/exec`. */
@JsonClass(generateAdapter = true)
data class ExecCreateRequest(
    @Json(name = "AttachStdout") val attachStdout: Boolean = true,
    @Json(name = "AttachStderr") val attachStderr: Boolean = true,
    @Json(name = "Tty") val tty: Boolean = false,
    @Json(name = "Cmd") val cmd: List<String>,
)

@JsonClass(generateAdapter = true)
data class ExecCreateResponse(@Json(name = "Id") val id: String)

@JsonClass(generateAdapter = true)
data class CreateVolumeRequest(@Json(name = "Name") val name: String)

@JsonClass(generateAdapter = true)
data class CreateNetworkRequest(
    @Json(name = "Name") val name: String,
    @Json(name = "Driver") val driver: String = "bridge",
)

@JsonClass(generateAdapter = true)
data class ApiImage(
    @Json(name = "Id") val id: String,
    @Json(name = "RepoTags") val repoTags: List<String>? = null,
    @Json(name = "Size") val size: Long = 0,
    @Json(name = "Created") val created: Long = 0,
)

@JsonClass(generateAdapter = true)
data class ApiContainer(
    @Json(name = "Id") val id: String,
    @Json(name = "Names") val names: List<String> = emptyList(),
    @Json(name = "Image") val image: String = "",
    @Json(name = "State") val state: String = "",
    @Json(name = "Status") val status: String = "",
    @Json(name = "Ports") val ports: List<ApiPort> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ApiPort(
    @Json(name = "PrivatePort") val privatePort: Int = 0,
    @Json(name = "PublicPort") val publicPort: Int? = null,
    @Json(name = "Type") val type: String = "tcp",
)

@JsonClass(generateAdapter = true)
data class ApiVolume(
    @Json(name = "Name") val name: String,
    @Json(name = "Driver") val driver: String = "local",
    @Json(name = "Mountpoint") val mountpoint: String = "",
)

@JsonClass(generateAdapter = true)
data class ApiVolumeList(@Json(name = "Volumes") val volumes: List<ApiVolume>? = null)

@JsonClass(generateAdapter = true)
data class ApiNetwork(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String,
    @Json(name = "Driver") val driver: String = "",
    @Json(name = "Scope") val scope: String = "",
)

/** Request body for `POST /containers/create`. */
@JsonClass(generateAdapter = true)
data class CreateContainerRequest(
    @Json(name = "Image") val image: String,
    @Json(name = "Cmd") val cmd: List<String>? = null,
    @Json(name = "Env") val env: List<String>? = null,
    @Json(name = "ExposedPorts") val exposedPorts: Map<String, Any>? = null,
    @Json(name = "HostConfig") val hostConfig: HostConfig? = null,
)

@JsonClass(generateAdapter = true)
data class HostConfig(
    @Json(name = "PortBindings") val portBindings: Map<String, List<PortBinding>>? = null,
    @Json(name = "Binds") val binds: List<String>? = null,
    @Json(name = "RestartPolicy") val restartPolicy: RestartPolicy? = null,
    @Json(name = "NetworkMode") val networkMode: String? = null,
)

@JsonClass(generateAdapter = true)
data class PortBinding(@Json(name = "HostPort") val hostPort: String)

@JsonClass(generateAdapter = true)
data class RestartPolicy(@Json(name = "Name") val name: String = "unless-stopped")

@JsonClass(generateAdapter = true)
data class CreateContainerResponse(@Json(name = "Id") val id: String)

/** Live stats sample from `GET /containers/{id}/stats?stream=false`. */
@JsonClass(generateAdapter = true)
data class ApiStats(
    @Json(name = "cpu_stats") val cpu: CpuStats? = null,
    @Json(name = "precpu_stats") val preCpu: CpuStats? = null,
    @Json(name = "memory_stats") val memory: MemoryStats? = null,
    @Json(name = "networks") val networks: Map<String, NetStats>? = null,
)

@JsonClass(generateAdapter = true)
data class CpuStats(
    @Json(name = "cpu_usage") val usage: CpuUsage? = null,
    @Json(name = "system_cpu_usage") val systemUsage: Long = 0,
    @Json(name = "online_cpus") val onlineCpus: Int = 1,
)

@JsonClass(generateAdapter = true)
data class CpuUsage(@Json(name = "total_usage") val total: Long = 0)

@JsonClass(generateAdapter = true)
data class MemoryStats(
    @Json(name = "usage") val usage: Long = 0,
    @Json(name = "limit") val limit: Long = 0,
)

@JsonClass(generateAdapter = true)
data class NetStats(
    @Json(name = "rx_bytes") val rxBytes: Long = 0,
    @Json(name = "tx_bytes") val txBytes: Long = 0,
)
