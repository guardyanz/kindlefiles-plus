// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.guardyanz.kindlefiles.R
import io.github.guardyanz.kindlefiles.core.*
import io.github.guardyanz.kindlefiles.data.Bookmarks
import io.github.guardyanz.kindlefiles.data.SafStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Page { PLACES, FILES, FAVORITES, TOOLS }
enum class Sort { NAME, DATE, SIZE }
data class BrowserState(
    val page: Page = Page.PLACES,
    val roots: List<Document> = emptyList(),
    val favorites: List<Document> = emptyList(),
    val path: List<Document> = emptyList(),
    val entries: List<Document> = emptyList(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val hidden: Boolean = false,
    val sort: Sort = Sort.NAME,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val message: String? = null,
    val duplicates: List<DuplicateGroup>? = null,
) {
    val folder get() = path.lastOrNull()
    val visible get() = entries.filter { (hidden || !it.name.startsWith('.')) && it.name.contains(query, true) }
        .sortedWith(compareBy<Document> { !it.directory }.then(
            when (sort) {
                Sort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                Sort.DATE -> compareByDescending { it.modified }
                Sort.SIZE -> compareByDescending { it.size }
            }
        ))
    val selection get() = entries.filter { it.id in selected }
}

class FilesViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    val store = SafStore(application)
    private val bookmarks = Bookmarks(application)
    private val mutable = MutableStateFlow(BrowserState(roots = bookmarks.read("roots"), favorites = bookmarks.read("favorites")))
    val state = mutable.asStateFlow()
    private var readJob: Job? = null
    private var operation: Job? = null
    var pendingTransfer: Pair<List<Document>, Boolean>? = null
    private fun text(id: Int, vararg args: Any) = getApplication<Application>().getString(id, *args)

    init {
        saved.get<ArrayList<String>>("path")?.takeIf { it.isNotEmpty() }?.let { ids ->
            readJob = viewModelScope.launch {
                try {
                    val path = withContext(Dispatchers.IO) { ids.map(store::stat) }
                    mutable.update { it.copy(path = path, page = Page.FILES) }
                    refresh()
                } catch (_: Exception) { fail(text(R.string.access_error)) }
            }
        }
    }

    fun page(page: Page) { if (!mutable.value.busy) mutable.update { it.copy(page = page, selected = emptySet()) } }
    fun query(value: String) { mutable.update { it.copy(query = value) } }
    fun hidden(value: Boolean) { mutable.update { it.copy(hidden = value) } }
    fun sort(value: Sort) { mutable.update { it.copy(sort = value) } }
    fun select(id: String) { mutable.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) } }
    fun selectAll() { mutable.update { it.copy(selected = it.visible.map(Document::id).toSet()) } }
    fun clearSelection() { mutable.update { it.copy(selected = emptySet()) } }
    fun dismissError() { mutable.update { it.copy(error = null) } }
    fun dismissMessage() { mutable.update { it.copy(message = null) } }
    fun dismissDuplicates() { mutable.update { it.copy(duplicates = null) } }
    fun fail(message: String) { mutable.update { it.copy(error = message, loading = false) } }
    fun cancel() { operation?.cancel() }

    fun openSavedFolder(id: String) {
        readJob?.cancel()
        readJob = viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.IO) { store.stat(id) }
                require(document.directory)
                openFolder(document, true)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { fail(text(R.string.access_error)) }
        }
    }

    fun openFolder(document: Document, root: Boolean = false) {
        if (mutable.value.busy) return
        val path = if (root) listOf(document) else mutable.value.path + document
        saved["path"] = ArrayList(path.map(Document::id))
        mutable.update { it.copy(path = path, page = Page.FILES, query = "", selected = emptySet(), entries = emptyList()) }
        refresh()
    }

    fun back(): Boolean {
        val state = mutable.value
        if (state.busy) return true
        if (state.selected.isNotEmpty()) { clearSelection(); return true }
        if (state.page != Page.FILES) {
            if (state.page != Page.PLACES) { page(Page.PLACES); return true }
            return false
        }
        if (state.path.size > 1) {
            val path = state.path.dropLast(1)
            saved["path"] = ArrayList(path.map(Document::id))
            mutable.update { it.copy(path = path, entries = emptyList(), query = "") }
            refresh()
        } else { page(Page.PLACES) }
        return true
    }

    fun refresh() {
        val folder = mutable.value.folder ?: return
        readJob?.cancel()
        readJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true) }
            try {
                val children = withContext(Dispatchers.IO) { store.stat(folder.id); store.children(folder.id) }
                mutable.update { it.copy(entries = children, selected = emptySet(), loading = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { fail(error.message ?: text(R.string.access_error)) }
        }
    }

    fun connect(tree: Uri, flags: Int, open: Boolean = true, after: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    val grants = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    require(grants and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) { "Read access was not granted." }
                    getApplication<Application>().contentResolver.takePersistableUriPermission(tree, grants)
                    store.stat(SafStore.treeDocument(tree))
                }
                val roots = (mutable.value.roots.filterNot { it.id == document.id } + document)
                bookmarks.write("roots", roots)
                mutable.update { it.copy(roots = roots) }
                if (open) openFolder(document, true)
                after?.invoke(document.id)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { fail(error.message ?: text(R.string.access_error)) }
        }
    }

    fun pickedFiles(uris: List<Uri>) {
        readJob?.cancel()
        readJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true) }
            try {
                val entries = withContext(Dispatchers.IO) { uris.distinct().map { store.stat(it.toString()) } }
                saved["path"] = arrayListOf<String>()
                mutable.update { it.copy(page = Page.FILES, path = emptyList(), entries = entries,
                    selected = entries.map(Document::id).toSet(), query = "", loading = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { fail(error.message ?: text(R.string.access_error)) }
        }
    }

    fun favorite(document: Document) {
        val existing = mutable.value.favorites
        val next = if (existing.any { it.id == document.id }) existing.filterNot { it.id == document.id } else existing + document
        bookmarks.write("favorites", next)
        mutable.update { it.copy(favorites = next) }
    }

    fun forget(document: Document) {
        // Bookmark removal only: do not revoke a tree grant still used by favorites or widgets.
        val roots = mutable.value.roots.filterNot { it.id == document.id }
        bookmarks.write("roots", roots)
        mutable.update { it.copy(roots = roots) }
    }

    private fun runOperation(action: suspend (FileEngine) -> String) {
        if (mutable.value.busy) return
        readJob?.cancel()
        mutable.update { it.copy(busy = true, loading = false, progress = "", error = null) }
        operation = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val scopeContext = currentCoroutineContext()
                    val engine = FileEngine(store, { scopeContext.ensureActive() }, { name -> mutable.update { it.copy(progress = name) } })
                    action(engine)
                }
                mutable.update { it.copy(message = result, selected = emptySet()) }
            } catch (cancel: CancellationException) {
                mutable.update { it.copy(message = text(R.string.cancelled)) }
            } catch (error: Exception) { fail(error.message ?: text(R.string.error_title)) }
            finally {
                mutable.update { it.copy(busy = false) }
                refresh()
            }
        }
    }

    fun transfer(files: List<Document>, destination: String, move: Boolean) = runOperation { engine ->
        var completed = 0
        try {
            files.forEach { currentCoroutineContext().ensureActive(); engine.transfer(it, destination, move); completed++ }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { throw java.io.IOException(text(R.string.batch_failed, completed, error.message ?: ""), error) }
        if (mutable.value.folder == null) mutable.update { it.copy(entries = if (move) it.entries - files.toSet() else it.entries) }
        text(if (move) R.string.moved else R.string.copied, completed)
    }

    fun newFolder(name: String) {
        val parent = mutable.value.folder ?: return
        runOperation { store.createDirectory(parent.id, name); text(R.string.completed) }
    }
    fun rename(document: Document, name: String) {
        val parent = mutable.value.folder ?: return
        runOperation {
            require(store.children(parent.id).none { it.id != document.id && it.name.equals(name, true) }) { "That name already exists." }
            val renamed = store.rename(document.id, name)
            val favorites = mutable.value.favorites.map { if (it.id == document.id) renamed else it }
            bookmarks.write("favorites", favorites)
            mutable.update { it.copy(favorites = favorites) }
            text(R.string.completed)
        }
    }
    fun delete(files: List<Document>) = runOperation {
        var completed = 0
        try {
            files.forEach { file ->
                currentCoroutineContext().ensureActive()
                store.delete(file.id)
                completed++
                mutable.update { state -> state.copy(entries = state.entries.filterNot { it.id == file.id }, duplicates = null,
                    favorites = state.favorites.filterNot { it.id == file.id }) }
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { throw java.io.IOException(text(R.string.batch_failed, completed, error.message ?: ""), error) }
        finally { bookmarks.write("favorites", mutable.value.favorites) }
        text(R.string.deleted, completed)
    }
    fun zip(files: List<Document>, name: String) {
        val parent = mutable.value.folder ?: return
        runOperation { it.zip(files, parent.id, if (name.endsWith(".zip", true)) name else "$name.zip"); text(R.string.completed) }
    }
    fun unzip(file: Document, name: String) {
        val parent = mutable.value.folder ?: return
        runOperation { it.unzip(file, parent.id, name); text(R.string.completed) }
    }
    fun scan() {
        val parent = mutable.value.folder ?: return
        val hidden = mutable.value.hidden
        runOperation { engine ->
            val groups = engine.duplicates(parent.id, hidden)
            mutable.update { it.copy(duplicates = groups) }
            text(R.string.scan_results, groups.size)
        }
    }
}
