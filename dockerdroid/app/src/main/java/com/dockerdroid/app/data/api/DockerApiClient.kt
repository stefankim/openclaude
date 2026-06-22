package com.dockerdroid.app.data.api

import com.dockerdroid.app.core.install.InstallManager.Paths
import com.dockerdroid.app.data.api.models.ApiContainer
import com.dockerdroid.app.data.api.models.ApiImage
import com.dockerdroid.app.data.api.models.ApiNetwork
import com.dockerdroid.app.data.api.models.ApiStats
import com.dockerdroid.app.data.api.models.ApiVolume
import com.dockerdroid.app.data.api.models.ApiVolumeList
import com.dockerdroid.app.data.api.models.CreateContainerRequest
import com.dockerdroid.app.data.api.models.CreateContainerResponse
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
 * operations the UI needs. All calls go over the unix socket via [UnixSocketFactory].
 *
 * The base host (`http://localhost`) is a placeholder — routing is handled by the
 * socket factory, not DNS.
 */
class DockerApiClient(
    socketPath: String = Paths.SOCKET,
    private val apiVersion: String = "v1.45",
) {
    private val client: OkHttpClient = UnixSocketFactory.clientFor(socketPath)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val base = "http://localhost/$apiVersion"
    private val jsonMedia = "application/json".toMediaType()

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

    // ---- Volumes ----------------------------------------------------------

    suspend fun listVolumes(): List<ApiVolume> =
        fromJson(get("/volumes"), ApiVolumeList::class.java).volumes ?: emptyList()

    suspend fun removeVolume(name: String): Boolean = delete("/volumes/$name")

    // ---- Networks ---------------------------------------------------------

    suspend fun listNetworks(): List<ApiNetwork> =
        getList("/networks", ApiNetwork::class.java)

    suspend fun removeNetwork(id: String): Boolean = delete("/networks/$id")

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url("http://localhost/_ping").build()).execute()
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
