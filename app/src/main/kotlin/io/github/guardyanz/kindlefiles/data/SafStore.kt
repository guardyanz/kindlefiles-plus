// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import io.github.guardyanz.kindlefiles.core.Document
import io.github.guardyanz.kindlefiles.core.DocumentStore
import io.github.guardyanz.kindlefiles.core.SafeNames
import java.io.IOException

class SafStore(context: Context) : DocumentStore {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private fun file(id: String) = requireNotNull(DocumentFile.fromSingleUri(app, Uri.parse(id)))

    override fun identity(id: String): String {
        val uri = Uri.parse(id)
        return "${uri.authority}:${DocumentsContract.getDocumentId(uri)}"
    }

    override fun stat(id: String): Document {
        val file = file(id)
        if (!file.exists() || !file.canRead()) throw IOException("Folder or file unavailable. Reconnect the folder or attach the drive.")
        return Document(id, file.name ?: "Untitled", file.isDirectory, file.type ?: "application/octet-stream",
            file.length(), file.lastModified(), file.canWrite())
    }

    override fun children(parent: String): List<Document> {
        val uri = Uri.parse(parent)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_FLAGS,
        )
        val cursor = resolver.query(childrenUri, columns, null, null, null)
            ?: throw IOException("The storage provider could not list this folder.")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val mime = it.getString(2) ?: "application/octet-stream"
                    val flags = it.getInt(5)
                    add(Document(DocumentsContract.buildDocumentUriUsingTree(uri, it.getString(0)).toString(),
                        it.getString(1) ?: "Untitled", mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        mime, it.getLong(3), it.getLong(4), flags and (
                            DocumentsContract.Document.FLAG_SUPPORTS_WRITE or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
                        ) != 0))
                }
            }
        }
    }

    private fun create(parent: String, name: String, mime: String): Document {
        SafeNames.component(name)
        require(children(parent).none { it.name.equals(name, ignoreCase = true) }) { "That name already exists." }
        val uri = DocumentsContract.createDocument(resolver, Uri.parse(parent), mime, name)
            ?: throw IOException("The storage provider could not create '$name'.")
        try {
            val created = stat(uri.toString())
            require(created.name == name) { "The storage provider changed '$name' to '${created.name}'. No output was kept." }
            return created
        }
        catch (failure: Exception) {
            try {
                check(DocumentsContract.deleteDocument(resolver, uri))
            } catch (cleanup: Exception) {
                throw IOException("Could not inspect or remove new output '$name'. Check the destination.", failure)
                    .also { it.addSuppressed(cleanup) }
            }
            throw failure
        }
    }

    override fun createDirectory(parent: String, name: String) = create(parent, name, DocumentsContract.Document.MIME_TYPE_DIR)
    override fun createFile(parent: String, name: String, mime: String): Document {
        val inferred = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
        return create(parent, name, if (mime == "application/octet-stream") inferred ?: mime else mime)
    }
    override fun input(id: String) = resolver.openInputStream(Uri.parse(id)) ?: throw IOException("Cannot read this file.")
    override fun output(id: String) = resolver.openOutputStream(Uri.parse(id), "wt") ?: throw IOException("Cannot write this file.")
    override fun delete(id: String) {
        if (!DocumentsContract.deleteDocument(resolver, Uri.parse(id))) throw IOException("The provider refused to delete this item.")
    }
    override fun rename(id: String, name: String): Document {
        SafeNames.component(name)
        val uri = DocumentsContract.renameDocument(resolver, Uri.parse(id), name) ?: throw IOException("Rename failed.")
        return stat(uri.toString())
    }

    companion object {
        fun treeDocument(tree: Uri): String = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)).toString()
    }
}
