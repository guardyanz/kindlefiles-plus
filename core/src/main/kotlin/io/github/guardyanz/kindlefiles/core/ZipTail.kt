// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.core

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** Retains only the bounded ZIP footer so a truncated archive cannot be reported as complete. */
internal class ZipTail(input: InputStream, private val maximum: Long) : FilterInputStream(input) {
    private val tail = ByteArray(65_557) // End record (22 bytes) plus the largest ZIP comment.
    private var count = 0L

    private fun record(value: Byte) {
        if (count >= maximum) throw IOException("Compressed archive exceeds its size limit.")
        tail[(count % tail.size).toInt()] = value
        count++
    }
    override fun read(): Int = `in`.read().also { if (it >= 0) record(it.toByte()) }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = `in`.read(buffer, offset, length).also { read ->
        if (read > 0) {
            if (count > maximum - read) throw IOException("Compressed archive exceeds its size limit.")
            val keep = minOf(read, tail.size)
            val start = ((count + read - keep) % tail.size).toInt()
            val first = minOf(keep, tail.size - start)
            System.arraycopy(buffer, offset + read - keep, tail, start, first)
            if (first < keep) System.arraycopy(buffer, offset + read - keep + first, tail, 0, keep - first)
            count += read
        }
    }

    fun validate(entries: Int) {
        val length = minOf(count, tail.size.toLong()).toInt()
        require(length >= 22) { "Incomplete ZIP archive." }
        val bytes = ByteArray(length) { index -> tail[((count - length + index) % tail.size).toInt()] }
        fun u16(offset: Int): Int = (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)
        fun u32(offset: Int): Long = u16(offset).toLong() or (u16(offset + 2).toLong() shl 16)
        val end = (length - 22 downTo 0).firstOrNull { index ->
            u32(index) == 0x06054b50L && index + 22 + u16(index + 20) == length
        } ?: throw IOException("Incomplete ZIP archive: end record missing.")
        require(u16(end + 4) == 0 && u16(end + 6) == 0) { "Split ZIP archives are not supported." }
        require(u16(end + 8) == entries && u16(end + 10) == entries) { "ZIP entry count does not match its directory; ZIP64 is not supported." }
        val centralSize = u32(end + 12)
        val centralOffset = u32(end + 16)
        require(centralOffset + centralSize <= count - length + end) { "Invalid ZIP directory." }
    }
}
