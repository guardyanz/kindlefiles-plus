// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.core

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ArchiveLimits(val entries: Int = 10_000, val bytes: Long = 2L * 1024 * 1024 * 1024)
data class DuplicateGroup(val sha256: String, val files: List<Document>)

/** Streaming operations. Run off the UI thread; checkpoint supports cooperative cancellation. */
class FileEngine(
    private val store: DocumentStore,
    private val checkpoint: () -> Unit = {},
    private val progress: (String) -> Unit = {},
) {
    private fun sha256() = MessageDigest.getInstance("SHA-256")
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun pump(input: InputStream, output: OutputStream?, maximum: Long = Long.MAX_VALUE): Pair<Long, String> {
        val digest = sha256()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            checkpoint()
            val read = input.read(buffer)
            if (read == -1) break
            if (read == 0) continue
            if (total > maximum - read) throw IOException("Expanded archive exceeds its size limit.")
            output?.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            total += read
        }
        return total to digest.digest().hex()
    }

    fun hash(id: String): String = store.input(id).use { pump(it, null).second }

    private fun vacant(parent: String, name: String) {
        SafeNames.component(name)
        require(store.children(parent).none { it.name.equals(name, ignoreCase = true) }) {
            "An item named '$name' already exists. Rename it or choose another folder."
        }
    }

    private inline fun <T> rollback(document: Document, block: () -> T): T {
        try { return block() } catch (failure: Exception) {
            try { store.delete(document.id) } catch (cleanup: Exception) {
                throw IOException("Operation failed; partial output '${document.name}' could not be removed. Inspect the destination.", failure)
                    .also { it.addSuppressed(cleanup) }
            }
            throw failure
        }
    }

    /** Move deletes only after destination read-back AND a fresh source hash match. */
    fun transfer(source: Document, destination: String, move: Boolean): Document {
        if (source.directory) return transferDirectory(source, destination, move)
        vacant(destination, source.name)
        checkpoint()
        val created = store.createFile(destination, source.name, source.mime)
        rollback(created) {
            progress(source.name)
            val copied = store.input(source.id).use { input -> store.output(created.id).use { pump(input, it) } }
            require(hash(created.id) == copied.second) { "Copy verification failed; source retained." }
            if (move) require(hash(source.id) == copied.second) { "Source changed during transfer; source retained." }
            checkpoint()
        }
        // Do not roll back the verified destination if the provider rejects source deletion.
        if (move) {
            try { store.delete(source.id) } catch (failure: Exception) {
                throw IOException("Copy verified, but '${source.name}' could not be removed from the source. Both copies were kept.", failure)
            }
        }
        return created
    }

    /** Copy a bounded tree first; for a move, verify its complete source snapshot before deletion. */
    private fun transferDirectory(source: Document, destination: String, move: Boolean): Document {
        val snapshot = snapshot(source)
        val targetIdentity = store.identity(destination)
        require(targetIdentity != store.identity(source.id) && snapshot.none { store.identity(it.document.id) == targetIdentity }) {
            "Choose a destination outside the source folder."
        }
        vacant(destination, source.name)
        checkpoint()
        val root = store.createDirectory(destination, source.name)
        rollback(root) {
            val folders = hashMapOf("" to root.id)
            val copiedFiles = HashMap<String, String>()
            snapshot.forEach { item ->
                checkpoint()
                val parentPath = item.path.substringBeforeLast('/', "")
                val parent = requireNotNull(folders[parentPath])
                if (item.document.directory) {
                    folders[item.path] = store.createDirectory(parent, item.document.name).id
                } else {
                    progress(item.path)
                    val created = store.createFile(parent, item.document.name, item.document.mime)
                    val copied = store.input(item.document.id).use { input ->
                        store.output(created.id).use { pump(input, it).second }
                    }
                    require(hash(created.id) == copied) { "Copy verification failed; source retained." }
                    if (move) require(hash(item.document.id) == copied) { "Source changed during transfer; source retained." }
                    copiedFiles[item.path] = created.id
                }
            }
            if (move) {
                val current = snapshot(source)
                require(current.map { it.path to it.document.directory } == snapshot.map { it.path to it.document.directory }) {
                    "Source folder changed during transfer; source retained."
                }
                snapshot.filterNot { it.document.directory }.forEach { item ->
                    require(hash(item.document.id) == hash(requireNotNull(copiedFiles[item.path]))) {
                        "Source changed during transfer; source retained."
                    }
                }
            }
            checkpoint()
        }
        if (move) {
            try { store.delete(source.id) } catch (failure: Exception) {
                throw IOException("Copy verified, but '${source.name}' could not be removed from the source. Both copies were kept.", failure)
            }
        }
        return root
    }

    private data class TreeItem(val path: String, val document: Document)

    private fun snapshot(source: Document): List<TreeItem> {
        val items = ArrayList<TreeItem>()
        val visited = hashSetOf(store.identity(source.id))
        fun visit(folder: Document, path: String, depth: Int) {
            require(depth <= 32) { "Folder nesting exceeds 32 levels." }
            val names = HashSet<String>()
            store.children(folder.id).forEach { child ->
                checkpoint()
                require(items.size < 10_000) { "Folder exceeds 10,000 items." }
                SafeNames.component(child.name)
                require(names.add(child.name.lowercase(java.util.Locale.ROOT))) { "Folder has names that collide on portable storage." }
                require(visited.add(store.identity(child.id))) { "Storage provider returned a repeated folder or file." }
                val childPath = if (path.isEmpty()) child.name else "$path/${child.name}"
                items += TreeItem(childPath, child)
                if (child.directory) visit(child, childPath, depth + 1)
            }
        }
        visit(source, "", 0)
        return items
    }

    fun zip(sources: List<Document>, destination: String, name: String, limits: ArchiveLimits = ArchiveLimits()): Document {
        require(sources.isNotEmpty()) { "Select at least one file." }
        vacant(destination, name)
        val seen = HashSet<String>()
        sources.forEach { require(seen.add(SafeNames.component(it.name).lowercase(java.util.Locale.ROOT))) { "Duplicate archive filename." } }
        val entries = sources.flatMap { source ->
            if (source.directory) listOf(TreeItem("${source.name}/", source)) + snapshot(source).map {
                TreeItem("${source.name}/${it.path}${if (it.document.directory) "/" else ""}", it.document)
            } else listOf(TreeItem(source.name, source))
        }
        require(entries.size <= limits.entries) { "Too many archive entries." }
        entries.forEach { SafeNames.archivePath(it.path) }
        val output = store.createFile(destination, name, "application/zip")
        return rollback(output) {
            store.output(output.id).use { raw ->
                ZipOutputStream(raw.buffered()).use { zip ->
                    var bytes = 0L
                    entries.forEach { (path, document) ->
                        checkpoint()
                        progress(path)
                        zip.putNextEntry(ZipEntry(path))
                        if (!document.directory) store.input(document.id).use { bytes += pump(it, zip, limits.bytes - bytes).first }
                        zip.closeEntry()
                    }
                }
            }
            output
        }
    }

    /** Expands only into a new directory, validates every path, bounds actual streamed bytes. */
    fun unzip(source: Document, destination: String, folderName: String, limits: ArchiveLimits = ArchiveLimits()): Document {
        vacant(destination, folderName)
        val root = store.createDirectory(destination, folderName)
        return rollback(root) {
            val directories = hashMapOf("" to root)
            val seen = HashSet<String>()
            var bytes = 0L
            var count = 0
            store.input(source.id).buffered().use { raw ->
                // ZipInputStream accepts random bytes as an empty ZIP. Reject those explicitly.
                raw.mark(4)
                val signature = ByteArray(4)
                val length = raw.read(signature)
                raw.reset()
                require(length == 4 && signature[0] == 0x50.toByte() && signature[1] == 0x4b.toByte() &&
                    ((signature[2] == 3.toByte() && signature[3] == 4.toByte()) ||
                     (signature[2] == 5.toByte() && signature[3] == 6.toByte()))) { "Not a supported ZIP archive." }
                val tracked = ZipTail(raw, limits.bytes + limits.entries.toLong() * 8192 + 65_557)
                ZipInputStream(tracked).use { zip ->
                    while (true) {
                        checkpoint()
                        val entry = zip.nextEntry ?: break
                        require(++count <= limits.entries) { "ZIP contains too many entries." }
                        val parts = SafeNames.archivePath(entry.name)
                        val key = parts.joinToString("/").lowercase(java.util.Locale.ROOT)
                        require(seen.add(key)) { "Duplicate ZIP path." }
                        var parent = root
                        var path = ""
                        val folderParts = if (entry.isDirectory) parts else parts.dropLast(1)
                        folderParts.forEach { part ->
                            path += "/" + part.lowercase(java.util.Locale.ROOT)
                            parent = directories.getOrPut(path) { store.createDirectory(parent.id, part) }
                        }
                        progress(entry.name)
                        if (!entry.isDirectory) {
                            val created = store.createFile(parent.id, parts.last(), "application/octet-stream")
                            store.output(created.id).use { bytes += pump(zip, it, limits.bytes - bytes).first }
                        } else {
                            // Directory entries can carry data too; count it against the same cap.
                            bytes += pump(zip, null, limits.bytes - bytes).first
                        }
                        zip.closeEntry()
                    }
                    // ZipInputStream stops at the central directory. Drain and validate its footer.
                    val buffer = ByteArray(64 * 1024)
                    while (tracked.read(buffer) >= 0) checkpoint()
                    tracked.validate(count)
                }
            }
            root
        }
    }

    /** Bounded recursive walk. Hidden directories are skipped unless requested. */
    fun walk(root: String, showHidden: Boolean = false, maximum: Int = 10_000): List<Document> {
        val result = ArrayList<Document>()
        val pending = ArrayDeque<Pair<String, Int>>()
        val visited = HashSet<String>()
        pending.add(root to 0)
        while (pending.isNotEmpty()) {
            checkpoint()
            val (id, depth) = pending.removeFirst()
            if (!visited.add(id)) continue
            require(depth <= 32) { "Folder nesting exceeds 32 levels." }
            store.children(id).forEach { node ->
                checkpoint()
                if (!showHidden && node.name.startsWith('.')) return@forEach
                require(result.size < maximum) { "Scan exceeds $maximum items. Choose a smaller folder." }
                result.add(node)
                if (node.directory) pending.add(node.id to depth + 1)
            }
        }
        return result.distinctBy { it.id }
    }

    fun duplicates(root: String, showHidden: Boolean = false): List<DuplicateGroup> {
        val files = walk(root, showHidden).filterNot { it.directory }
        return files.groupBy { it.size }.values.filter { it.size > 1 }.flatMap { candidates ->
            candidates.groupBy { file -> checkpoint(); progress(file.name); hash(file.id) }
                .filterValues { it.size > 1 }.map { (hash, group) -> DuplicateGroup(hash, group) }
        }
    }
}
