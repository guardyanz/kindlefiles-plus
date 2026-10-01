package io.github.guardyanz.kindlefiles.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.*
import java.nio.file.Files
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileEngineTest {
    @get:Rule val temp = TemporaryFolder()
    private fun store() = DiskStore(temp.newFolder())

    @Test fun `move verifies contents and deletes source only after verification`() {
        val store = store()
        val source = store.file("book.epub", "a book")
        val target = store.createDirectory(store.root.path, "usb")
        val copy = FileEngine(store).transfer(source, target.id, true)
        assertEquals("a book", store.input(copy.id).reader().readText())
        assertFalse(File(source.id).exists())
    }
    @Test fun `folder copy and move preserve nested contents and empty folders`() {
        val store = store()
        val books = store.createDirectory(store.root.path, "books")
        val series = store.createDirectory(books.id, "series")
        store.createDirectory(books.id, "empty")
        File(series.id, "part 1.epub").writeText("volume one")
        val usb = store.createDirectory(store.root.path, "usb")
        val copied = FileEngine(store).transfer(books, usb.id, false)
        assertEquals("volume one", File(copied.id, "series/part 1.epub").readText())
        assertTrue(File(copied.id, "empty").isDirectory)
        assertTrue(File(books.id).exists())
        val sd = store.createDirectory(store.root.path, "sd")
        val moved = FileEngine(store).transfer(books, sd.id, true)
        assertEquals("volume one", File(moved.id, "series/part 1.epub").readText())
        assertFalse(File(books.id).exists())
    }
    @Test fun `folder transfer cannot target its own descendant`() {
        val store = store()
        val source = store.createDirectory(store.root.path, "books")
        val nested = store.createDirectory(source.id, "inside")
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).transfer(source, nested.id, true) }
        assertTrue(store.children(nested.id).isEmpty())
        val alias = File(source.id, ".").path
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).transfer(source, alias, false) }
    }
    @Test fun `folder copy rollback preserves source when a nested write corrupts`() {
        val store = store()
        val source = store.createDirectory(store.root.path, "books")
        File(source.id, "story.txt").writeText("read me")
        val usb = store.createDirectory(store.root.path, "usb")
        store.corruptWrites = true
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).transfer(source, usb.id, true) }
        assertTrue(File(source.id, "story.txt").exists())
        assertTrue(store.children(usb.id).isEmpty())
    }
    @Test fun `folder zip includes nested and empty folders`() {
        val store = store()
        val source = store.createDirectory(store.root.path, "books")
        val series = store.createDirectory(source.id, "series")
        store.createDirectory(source.id, "empty")
        File(series.id, "part.txt").writeText("hello")
        val archive = FileEngine(store).zip(listOf(source), store.root.path, "folder.zip")
        val expanded = FileEngine(store).unzip(archive, store.root.path, "expanded")
        assertEquals("hello", File(expanded.id, "books/series/part.txt").readText())
        assertTrue(File(expanded.id, "books/empty").isDirectory)
    }
    @Test fun `corrupted destination keeps source and removes failed copy`() {
        val store = store()
        val source = store.file("book.pdf", "the source")
        val target = store.createDirectory(store.root.path, "usb")
        store.corruptWrites = true
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).transfer(source, target.id, true) }
        assertEquals("the source", store.input(source.id).reader().readText())
        assertTrue(store.children(target.id).isEmpty())
    }
    @Test fun `failed source deletion preserves verified target`() {
        val store = store()
        val source = store.file("book.pdf", "book")
        val target = store.createDirectory(store.root.path, "usb")
        store.refuseDelete = source.id
        val failure = assertThrows(IOException::class.java) { FileEngine(store).transfer(source, target.id, true) }
        assertTrue(failure.message.orEmpty().contains("Both copies"))
        assertTrue(File(source.id).exists())
        assertEquals("book", store.input(store.children(target.id).single().id).reader().readText())
    }
    @Test fun `cancellation during copy rolls back partial output`() {
        val store = store()
        val source = store.file("big.txt", "a".repeat(200_000))
        val target = store.createDirectory(store.root.path, "usb")
        var checkpoints = 0
        val engine = FileEngine(store, { if (++checkpoints == 3) throw CancellationException() })
        assertThrows(CancellationException::class.java) { engine.transfer(source, target.id, true) }
        assertTrue(File(source.id).exists())
        assertTrue(store.children(target.id).isEmpty())
    }
    @Test fun `collision is refused without overwriting existing bytes`() {
        val store = store()
        val source = store.file("a.txt", "source")
        val target = store.createDirectory(store.root.path, "usb")
        File(target.id, "A.txt").writeText("existing")
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).transfer(source, target.id, false) }
        assertEquals("existing", File(target.id, "A.txt").readText())
    }
    @Test fun `zip and unzip round trip with unicode and empty files`() {
        val store = store()
        val source = listOf(store.file("Buku pagi.txt", "selamat pagi"), store.file("空.txt", ""))
        val zip = FileEngine(store).zip(source, store.root.path, "pack.zip")
        val output = FileEngine(store).unzip(zip, store.root.path, "unpacked")
        assertEquals("selamat pagi", File(output.id, "Buku pagi.txt").readText())
        assertTrue(File(output.id, "空.txt").exists())
    }
    @Test fun `nested zip expands safely and supports explicit directory after children`() {
        val store = store()
        val archive = store.archive(listOf("books/story.txt" to "story", "books/" to ""))
        val output = FileEngine(store).unzip(archive, store.root.path, "out")
        assertEquals("story", File(output.id, "books/story.txt").readText())
    }
    @Test fun `zip slip absolute paths backslashes drive paths and deep paths are rejected`() {
        val paths = listOf("../evil", "/evil", "a/../../evil", "a\\evil", "C:/evil", "a//evil", "./evil", "a/".repeat(33) + "evil")
        paths.forEach { path ->
            val store = store()
            val archive = store.archive(listOf(path to "bad"))
            assertThrows(path, IllegalArgumentException::class.java) { FileEngine(store).unzip(archive, store.root.path, "out") }
            assertFalse(File(store.root, "out").exists())
        }
    }
    @Test fun `duplicate paths differing in case are rejected and rolled back`() {
        val store = store()
        val archive = store.archive(listOf("a.txt" to "one", "A.txt" to "two"))
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).unzip(archive, store.root.path, "out") }
        assertFalse(File(store.root, "out").exists())
    }
    @Test fun `expanded bytes are bounded even when compressed size is tiny`() {
        val store = store()
        val archive = store.archive(listOf("large.txt" to "a".repeat(100_000)))
        assertThrows(IOException::class.java) { FileEngine(store).unzip(archive, store.root.path, "out", ArchiveLimits(bytes = 50)) }
        assertFalse(File(store.root, "out").exists())
    }
    @Test fun `entry count is bounded`() {
        val store = store()
        val archive = store.archive(listOf("a.txt" to "a", "b.txt" to "b"))
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).unzip(archive, store.root.path, "out", ArchiveLimits(entries = 1)) }
        assertFalse(File(store.root, "out").exists())
    }
    @Test fun `non zip input does not create a misleading empty result`() {
        val store = store()
        val file = store.file("fake.zip", "not a zip file")
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).unzip(file, store.root.path, "out") }
        assertFalse(File(store.root, "out").exists())
    }
    @Test fun `duplicates use content not names and exclude hidden directories by default`() {
        val store = store()
        store.file("first.txt", "same")
        store.file("second.txt", "same")
        store.file("different.txt", "else")
        File(store.root, ".hidden").mkdir()
        File(store.root, ".hidden/third.txt").writeText("same")
        val engine = FileEngine(store)
        assertEquals(2, engine.duplicates(store.root.path).single().files.size)
        assertEquals(3, engine.duplicates(store.root.path, true).single().files.size)
    }
    @Test fun `hash known vector and bounded directory traversal`() {
        val store = store()
        val file = store.file("a.txt", "abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", FileEngine(store).hash(file.id))
        store.file("b.txt", "two")
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).walk(store.root.path, maximum = 1) }
    }
    @Test fun `partial cleanup failure is surfaced with the output name`() {
        val store = store()
        val source = store.file("a.txt", "a")
        val target = store.createDirectory(store.root.path, "usb")
        store.corruptWrites = true
        store.refuseDelete = File(target.id, "a.txt").path
        val error = assertThrows(IOException::class.java) { FileEngine(store).transfer(source, target.id, false) }
        assertTrue(error.message.orEmpty().contains("partial output 'a.txt'"))
    }
    @Test fun `source changed while copying is never deleted`() {
        val store = store()
        val source = store.file("book.txt", "original")
        val target = store.createDirectory(store.root.path, "usb")
        store.afterWrite = { File(source.id).writeText("edited") }
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).transfer(source, target.id, true) }
        assertEquals("edited", File(source.id).readText())
        assertTrue(store.children(target.id).isEmpty())
    }
    @Test fun `truncated ZIP footer rolls back otherwise readable entries`() {
        val store = store()
        val archive = store.archive(listOf("a.txt" to "hello"))
        val file = File(archive.id)
        file.writeBytes(file.readBytes().dropLast(22).toByteArray())
        assertThrows(IOException::class.java) { FileEngine(store).unzip(archive, store.root.path, "out") }
        assertFalse(File(store.root, "out").exists())
    }
    @Test fun `empty ZIP requires a complete end record`() {
        val store = store()
        val archive = store.archive(emptyList())
        assertTrue(FileEngine(store).unzip(archive, store.root.path, "valid").directory)
        File(archive.id).writeBytes(byteArrayOf(0x50, 0x4b, 0x05, 0x06))
        assertThrows(IllegalArgumentException::class.java) { FileEngine(store).unzip(archive, store.root.path, "invalid") }
        assertFalse(File(store.root, "invalid").exists())
    }
}

private class DiskStore(val root: File) : DocumentStore {
    var corruptWrites = false
    var refuseDelete: String? = null
    var afterWrite: (() -> Unit)? = null
    override fun identity(id: String): String = File(id).canonicalPath
    fun file(name: String, contents: String): Document = File(root, name).also { it.writeText(contents) }.let { stat(it.path) }
    fun archive(entries: List<Pair<String, String>>): Document {
        val file = File(root, "archive.zip")
        ZipOutputStream(file.outputStream()).use { zip -> entries.forEach { (name, text) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
        } }
        return stat(file.path)
    }
    override fun stat(id: String) = File(id).let { Document(id, it.name, it.isDirectory, size = it.length(), writable = true) }
    override fun children(parent: String) = File(parent).listFiles().orEmpty().map { stat(it.path) }
    override fun createDirectory(parent: String, name: String): Document {
        val file = File(parent, SafeNames.component(name)); check(file.mkdir()); return stat(file.path)
    }
    override fun createFile(parent: String, name: String, mime: String): Document {
        val file = File(parent, SafeNames.component(name)); check(file.createNewFile()); return stat(file.path)
    }
    override fun input(id: String): InputStream = File(id).inputStream()
    override fun output(id: String): OutputStream = if (corruptWrites) object : OutputStream() {
        override fun write(b: Int) {} // Simulate a provider that accepts writes but persists different bytes.
    } else object : FilterOutputStream(File(id).outputStream()) {
        override fun close() { super.close(); afterWrite?.invoke() }
    }
    override fun delete(id: String) {
        if (id == refuseDelete) throw IOException("Read only")
        check(File(id).deleteRecursively())
    }
    override fun rename(id: String, name: String): Document {
        val target = File(File(id).parentFile, name)
        Files.move(File(id).toPath(), target.toPath())
        return stat(target.path)
    }
}
