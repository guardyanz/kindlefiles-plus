// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.core

import java.io.InputStream
import java.io.OutputStream

data class Document(
    val id: String,
    val name: String,
    val directory: Boolean,
    val mime: String = "application/octet-stream",
    val size: Long = 0,
    val modified: Long = 0,
    val writable: Boolean = false,
)

/** Provider-neutral boundary. Implementations must not overwrite an existing child. */
interface DocumentStore {
    /** Stable key for the underlying document, even when two tree grants use different URI forms. */
    fun identity(id: String): String = id
    fun stat(id: String): Document
    fun children(parent: String): List<Document>
    fun createDirectory(parent: String, name: String): Document
    fun createFile(parent: String, name: String, mime: String): Document
    fun input(id: String): InputStream
    fun output(id: String): OutputStream
    fun delete(id: String)
    fun rename(id: String, name: String): Document
}

/** Rejects ambiguous names across Android, ZIP, and portable Windows storage. */
object SafeNames {
    fun component(name: String): String {
        require(name.isNotBlank() && name != "." && name != "..") { "Enter a valid name." }
        require(name.length <= 240 && name.none { it < ' ' || it in "/\\:*?\"<>|" }) {
            "Names cannot contain separators, control characters, or reserved punctuation."
        }
        require(name == name.trim() && !name.endsWith('.')) { "Names cannot end with a dot or spaces." }
        return name
    }

    fun archivePath(path: String): List<String> {
        require(path.isNotEmpty() && !path.startsWith('/') && !path.contains('\\')) { "Unsafe ZIP path." }
        val parts = path.removeSuffix("/").split('/')
        require(parts.size <= 32) { "ZIP nesting exceeds 32 levels." }
        return parts.map(::component)
    }
}
