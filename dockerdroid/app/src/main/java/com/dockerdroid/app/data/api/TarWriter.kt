package com.dockerdroid.app.data.api

import java.io.ByteArrayOutputStream

/**
 * Minimal USTAR archive writer — just enough to package a Docker build context of a
 * few in-memory files (e.g. a single Dockerfile) without pulling in a tar library.
 */
object TarWriter {

    fun archive(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        for ((name, data) in entries) {
            out.write(header(name, data.size))
            out.write(data)
            // Pad file data to a 512-byte block.
            val pad = (512 - data.size % 512) % 512
            if (pad > 0) out.write(ByteArray(pad))
        }
        // Two zero blocks terminate the archive.
        out.write(ByteArray(1024))
        return out.toByteArray()
    }

    private fun header(name: String, size: Int): ByteArray {
        val h = ByteArray(512)
        writeString(h, 0, name.take(100))       // name
        writeOctal(h, 100, 7, 420)              // mode 0644
        writeOctal(h, 108, 7, 0)                // uid
        writeOctal(h, 116, 7, 0)                // gid
        writeOctal(h, 124, 11, size)            // size
        writeOctal(h, 136, 11, 0)               // mtime
        h[156] = '0'.code.toByte()              // typeflag: regular file
        writeString(h, 257, "ustar")            // magic
        h[263] = '0'.code.toByte(); h[264] = '0'.code.toByte() // version "00"
        // Checksum: spaces during computation, then octal.
        for (i in 148 until 156) h[i] = ' '.code.toByte()
        var checksum = 0
        for (b in h) checksum += b.toInt() and 0xff
        writeOctal(h, 148, 6, checksum)
        h[154] = 0
        h[155] = ' '.code.toByte()
        return h
    }

    private fun writeString(buf: ByteArray, offset: Int, s: String) {
        val bytes = s.toByteArray(Charsets.US_ASCII)
        System.arraycopy(bytes, 0, buf, offset, bytes.size)
    }

    private fun writeOctal(buf: ByteArray, offset: Int, len: Int, value: Int) {
        // Field is `len` octal digits followed by a NUL.
        val octal = value.toString(8).padStart(len, '0').takeLast(len)
        writeString(buf, offset, octal)
        buf[offset + len] = 0
    }
}
