package com.dockerdroid.app.vm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Downloads and verifies the guest kernel + rootfs into app storage. These are data
 * files (QEMU reads them; they are never executed), so they can live in [Context.getFilesDir]
 * unlike the QEMU binary itself, which must ship as a jniLib.
 */
class VmProvisioner(private val context: Context) {

    private val client = OkHttpClient()

    fun dir(): File = File(context.filesDir, "vm").apply { mkdirs() }
    fun file(name: String): File = File(dir(), name)

    fun isProvisioned(assets: List<VmAsset> = VmImages.defaultArm64()): Boolean =
        assets.all { file(it.fileName).exists() }

    /**
     * Download any missing assets. The downloaded bytes are SHA-256-verified *as
     * received* (matching the checksum of the published asset), and only then is a
     * gzipped asset decompressed to its final file.
     */
    suspend fun ensure(
        assets: List<VmAsset> = VmImages.defaultArm64(),
        onProgress: (String) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            for (asset in assets) {
                val target = file(asset.fileName)
                if (target.exists()) continue
                onProgress("Downloading ${asset.name}…")
                val raw = File(target.parentFile, "${asset.fileName}.download")
                downloadRaw(asset.url, asset.name, raw)
                verify(raw, asset.sha256, asset.name)
                if (asset.gzipped) {
                    onProgress("Extracting ${asset.name}…")
                    GZIPInputStream(raw.inputStream()).use { g -> target.outputStream().use { g.copyTo(it) } }
                    raw.delete()
                } else {
                    check(raw.renameTo(target)) { "Could not finalize ${asset.fileName}" }
                }
            }
        }
    }

    private fun downloadRaw(url: String, name: String, dest: File) {
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            check(resp.isSuccessful) { "Download of $name failed: HTTP ${resp.code}" }
            val body = resp.body ?: error("Empty response for $name")
            body.byteStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
        }
    }

    private fun verify(file: File, expected: String, name: String) {
        if (expected == "REPLACED_AT_RELEASE") return // hash pinned at release time
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val read = stream.read(buf)
                if (read <= 0) break
                digest.update(buf, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual.equals(expected, ignoreCase = true)) {
            "Checksum mismatch for $name (expected $expected, got $actual)"
        }
    }
}
