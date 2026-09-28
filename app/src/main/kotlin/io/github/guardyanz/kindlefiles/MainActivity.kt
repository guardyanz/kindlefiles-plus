// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.guardyanz.kindlefiles.core.Document
import io.github.guardyanz.kindlefiles.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KindleTheme {
                val model: FilesViewModel = viewModel()
                val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    result.data?.takeIf { result.resultCode == RESULT_OK }?.let { data ->
                        data.data?.let { model.connect(it, data.flags) }
                    }
                }
                val destinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val pending = model.pendingTransfer
                    model.pendingTransfer = null
                    if (result.resultCode == RESULT_OK) {
                        val data = result.data
                        val uri = data?.data
                        if (pending != null && uri != null) {
                            model.connect(uri, data.flags, open = false) { model.transfer(pending.first, it, pending.second) }
                        } else model.fail(getString(R.string.lost_picker))
                    }
                }
                val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == RESULT_OK) {
                        result.data?.let { data ->
                            val uris = data.clipData?.let { clip -> (0 until clip.itemCount).map { clip.getItemAt(it).uri } }
                                ?: listOfNotNull(data.data)
                            model.pickedFiles(uris)
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    intent.getStringExtra("folder")?.let(model::openSavedFolder)
                }
                FileApp(model,
                    connect = { safely(model) { folderPicker.launch(treeIntent()) } },
                    pickFiles = { safely(model) { filePicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }) } },
                    transfer = { files, move ->
                        model.pendingTransfer = files to move
                        safely(model) { destinationPicker.launch(treeIntent()) }
                    },
                    external = { document, share -> openExternal(document, share, model) },
                )
            }
        }
    }

    private fun treeIntent() = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
    }
    private fun safely(model: FilesViewModel, action: () -> Unit) {
        try { action() } catch (_: ActivityNotFoundException) { model.fail(getString(R.string.no_picker)) }
        catch (error: SecurityException) { model.fail(error.message ?: getString(R.string.access_error)) }
    }
    private fun openExternal(file: Document, share: Boolean, model: FilesViewModel) {
        val uri = Uri.parse(file.id)
        val intent = if (share) Intent(Intent.ACTION_SEND).apply {
            type = file.mime; putExtra(Intent.EXTRA_STREAM, uri)
        } else Intent(Intent.ACTION_VIEW).setDataAndType(uri, file.mime)
        intent.clipData = ClipData.newRawUri(file.name, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(Intent.createChooser(intent, getString(if (share) R.string.share else R.string.open))) }
        catch (_: ActivityNotFoundException) { model.fail(getString(R.string.no_reader)) }
        catch (error: SecurityException) { model.fail(error.message ?: getString(R.string.access_error)) }
    }
}
