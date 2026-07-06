package com.dockerdroid.app.data.api

import com.dockerdroid.app.core.install.InstallManager.Paths
import com.dockerdroid.app.data.api.models.ApiContainer
import com.dockerdroid.app.data.api.models.ApiContainerInspect
import com.dockerdroid.app.data.api.models.ApiImage
import com.dockerdroid.app.data.api.models.ApiNetwork
import com.dockerdroid.app.data.api.models.ApiStats
import com.dockerdroid.app.data.api.models.ApiVolume
import com.dockerdroid.app.data.api.models.ApiVolumeList
import com.dockerdroid.app.data.api.models.CreateContainerRequest
import com.dockerdroid.app.data.api.models.CreateContainerResponse
import com.dockerdroid.app.data.api.models.CreateNetworkRequest
import com.dockerdroid.app.data.api.models.CreateVolumeRequest
import com.dockerdroid.app.data.api.models.ExecCreateRequest
import com.dockerdroid.app.data.api.models.ExecCreateResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Thin, coroutine-friendly client for the Docker Engine REST API, scoped to the
 * operations the UI needs.
 *
 * The transport is supplied as an [OkHttpClient]: [local] routes over the on-device
 * unix socket via [UnixSocketFactory], while [remote] is given a client that tunnels
 * to a remote daemon (e.g. over SSH). The base host (`http://localhost`) is a
 * placeholder — routing is handled by the client's socket factory, not DNS.
 */
class DockerApiClient private constructor(
    private val client: OkHttpClient,
    private val authority: String = "localhost",
    private val apiVersion: String = "v1.45",
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val base = "http://$authority/$apiVersion"
    private val jsonMedia = "application/json".toMediaType()

    companion object {
        /** Client for the daemon running on this device (unix socket). */
        fun local(socketPath: String = Paths.SOCKET, apiVersion: String = "v1.45") =
            DockerApiClient(UnixSocketFactory.clientFor(socketPath), "localhost", apiVersion)

        /** Client for a remote daemon, given a transport-configured [OkHttpClient]. */
        fun remote(client: OkHttpClient, apiVersion: String = "v1.45") =
            DockerApiClient(client, "localhost", apiVersion)

        /**
         * Client for a daemon reachable over plain TCP — used for the on-device VM,
         * whose guest dockerd is port-forwarded to `127.0.0.1:<port>` by QEMU.
         */
        fun tcp(host: String, port: Int, apiVersion: String = "v1.45") =
            DockerApiClient(OkHttpClient(), "$host:$port", apiVersion)
    }

    // ---- Images -----------------------------------------------------------

    suspend fun listImages(): List<ApiImage> =
        getList("/images/json", ApiImage::class.java)

    /** Pull an image, streaming the JSON progress events line by line. */
    fun pullImage(reference: String): Flow<String> = streamPost("/images/create?fromImage=$reference")

    suspend fun removeImage(id: String, force: Boolean = false): Boolean =
        delete("/images/$id?force=$force")

    // ---- Containers -------------------------------------------------------

    suspend fun listContainers(all: Boolean = true): List<ApiContainer> =
        getList("/containers/json?all=$all", ApiContainer::class.java)

    suspend fun createContainer(name: String?, body: CreateContainerRequest): String {
        val path = "/containers/create" + (name?.let { "?name=$it" } ?: "")
        val resp = post(path, toJson(body, CreateContainerRequest::class.java))
        return fromJson(resp, CreateContainerResponse::class.java).id
    }

    suspend fun startContainer(id: String): Boolean = postEmpty("/containers/$id/start")
    suspend fun stopContainer(id: String): Boolean = postEmpty("/containers/$id/stop")
    suspend fun restartContainer(id: String): Boolean = postEmpty("/containers/$id/restart")
    suspend fun removeContainer(id: String, force: Boolean = true): Boolean =
        delete("/containers/$id?force=$force&v=true")

    /** Follow container logs. Multiplexed stream is de-framed to plain text lines. */
    fun containerLogs(id: String, follow: Boolean = true): Flow<String> =
        streamGet("/containers/$id/logs?stdout=true&stderr=true&follow=$follow&tail=200")

    suspend fun stats(id: String): ApiStats =
        fromJson(get("/containers/$id/stats?stream=false"), ApiStats::class.java)

    suspend fun inspectContainer(id: String): ApiContainerInspect =
        fromJson(get("/containers/$id/json"), ApiContainerInspect::class.java)

    /**
     * Run a one-shot command in a running container (`docker exec`). Returns the
     * combined stdout/stderr; multiplexed frame headers are stripped best-effort.
     */
    suspend fun exec(id: String, cmd: List<String>): String {
        val created = fromJson(
            post("/containers/$id/exec", toJson(ExecCreateRequest(cmd = cmd), ExecCreateRequest::class.java)),
            ExecCreateResponse::class.java,
        )
        val body = """{"Detach":false,"Tty":false}"""
        val raw = post("/exec/${created.id}/start", body)
        // Strip Docker's 8-byte stream multiplexing headers if present.
        return raw.replace(Regex("[\\x00-\\x08\\x0e-\\x1f]"), "")
    }

    // ---- Events -----------------------------------------------------------

    /** Stream daemon events (container start/stop/die, etc.) as raw JSON lines. */
    fun events(): Flow<String> = streamGet("/events")

    // ---- Build ------------------------------------------------------------

    /** Build an image from a tar'd context, streaming build output line by line. */
    fun buildImage(contextTar: ByteArray, tag: String): Flow<String> = flow {
        val req = Request.Builder()
            .url("$base/build?t=$tag")
            .post(contextTar.toRequestBody("application/x-tar".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val source = resp.body?.source() ?: return@use
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isNotEmpty()) emit(line)
            }
        }
    }.flowOn(Dispatchers.IO)

    // ---- Volumes ----------------------------------------------------------

    suspend fun listVolumes(): List<ApiVolume> =
        fromJson(get("/volumes"), ApiVolumeList::class.java).volumes ?: emptyList()

    suspend fun createVolume(name: String): Boolean =
        runCatching { post("/volumes/create", toJson(CreateVolumeRequest(name), CreateVolumeRequest::class.java)) }
            .isSuccess

    suspend fun removeVolume(name: String): Boolean = delete("/volumes/$name")

    // ---- Networks ---------------------------------------------------------

    suspend fun listNetworks(): List<ApiNetwork> =
        getList("/networks", ApiNetwork::class.java)

    suspend fun createNetwork(name: String): Boolean =
        runCatching { post("/networks/create", toJson(CreateNetworkRequest(name), CreateNetworkRequest::class.java)) }
            .isSuccess

    suspend fun removeNetwork(id: String): Boolean = delete("/networks/$id")

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url("http://$authority/_ping").build()).execute()
                .use { it.isSuccessful }
        }.getOrDefault(false)
    }

    // ---- HTTP plumbing ----------------------------------------------------

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(base + path).build()).execute().use { resp ->
            check(resp.isSuccessful) { "GET $path -> ${resp.code}" }
            resp.body?.string().orEmpty()
        }
    }

    private suspend fun <T> getList(path: String, clazz: Class<T>): List<T> {
        val type = Types.newParameterizedType(List::class.java, clazz)
        return moshi.adapter<List<T>>(type).fromJson(get(path)) ?: emptyList()
    }

    private suspend fun post(path: String, body: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(base + path).post(body.toRequestBody(jsonMedia)).build()
        client.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "POST $path -> ${resp.code}: ${resp.body?.string()}" }
            resp.body?.string().orEmpty()
        }
    }

    private suspend fun postEmpty(path: String): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(base + path).post(ByteArray(0).toRequestBody()).build()
        client.newCall(req).execute().use { it.isSuccessful || it.code == 304 }
    }

    private suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(base + path).delete().build()).execute()
            .use { it.isSuccessful }
    }

    private fun streamGet(path: String): Flow<String> = stream(Request.Builder().url(base + path).build())

    private fun streamPost(path: String): Flow<String> =
        stream(Request.Builder().url(base + path).post(ByteArray(0).toRequestBody()).build())

    private fun stream(request: Request): Flow<String> = flow {
        client.newCall(request).execute().use { resp ->
            val source = resp.body?.source() ?: return@use
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isNotEmpty()) emit(line)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun <T> toJson(value: T, clazz: Class<T>): String = moshi.adapter(clazz).toJson(value)
    private fun <T> fromJson(json: String, clazz: Class<T>): T =
        requireNotNull(moshi.adapter(clazz).fromJson(json)) { "Unparseable response" }
}
