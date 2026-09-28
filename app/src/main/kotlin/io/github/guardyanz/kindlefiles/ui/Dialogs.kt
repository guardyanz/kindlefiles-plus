// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.guardyanz.kindlefiles.R
import io.github.guardyanz.kindlefiles.core.Document
import io.github.guardyanz.kindlefiles.core.DuplicateGroup
import io.github.guardyanz.kindlefiles.core.SafeNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun NameDialog(title: Int, initial: String, dismiss: () -> Unit, confirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val valid = runCatching { SafeNames.component(name) }.isSuccess
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(title)) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.name)) },
            isError = name.isNotEmpty() && !valid, supportingText = { if (name.isNotEmpty() && !valid) Text(stringResource(R.string.invalid_name)) }) },
        confirmButton = { TextButton(onClick = { confirm(name) }, enabled = valid) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun DetailsDialog(file: Document, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.details)) }, text = {
        SelectionContainer {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium)
                Text(fileDescription(file)); Text(file.mime); Text(file.id, style = MaterialTheme.typography.bodySmall)
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } })
}

@Composable
internal fun DuplicateDialog(groups: List<DuplicateGroup>, dismiss: () -> Unit, open: (Document) -> Unit, delete: (Document) -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.scan_results, groups.size)) }, text = {
        if (groups.isEmpty()) Text(stringResource(R.string.scan_empty)) else LazyColumn(Modifier.heightIn(max = 480.dp)) {
            groups.forEachIndexed { index, group ->
                item { SectionTitle(stringResource(R.string.scan_group, index + 1, group.files.size)) }
                item { Text(group.sha256, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = Space.sm)) }
                items(group.files, key = { "${group.sha256}:${it.id}" }) { file ->
                    Column {
                        TextButton(onClick = { open(file) }) { Text(file.name) }
                        Text(Uri.decode(file.id.substringAfterLast("/document/")), style = MaterialTheme.typography.bodySmall)
                        Row {
                            Text(fileDescription(file), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { delete(file) }) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.delete)) }
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } })
}

@Composable
internal fun PreviewDialog(file: Document, dismiss: () -> Unit, external: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(file.name, maxLines = 2) }, text = {
        if (file.mime.startsWith("image/")) ImagePreview(file) else VideoPreview(file)
    }, confirmButton = { TextButton(onClick = external) { Text(stringResource(R.string.open)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } })
}

@Composable
private fun ImagePreview(file: Document) {
    val resolver = LocalContext.current.contentResolver
    var failed by remember(file.id) { mutableStateOf(false) }
    val bitmap by produceState<android.graphics.Bitmap?>(null, file.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(file.id)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                require(options.outWidth > 0 && options.outHeight > 0)
                var sample = 1
                while (options.outWidth / sample > 1600 || options.outHeight / sample > 1600) sample *= 2
                options.inSampleSize = sample; options.inJustDecodeBounds = false
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }.getOrNull()
        }
        failed = value == null
    }
    when {
        bitmap != null -> Image(requireNotNull(bitmap).asImageBitmap(), file.name, modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp))
        failed -> Text(stringResource(R.string.preview_failed))
        else -> CircularProgressIndicator()
    }
}

@Composable
private fun VideoPreview(file: Document) {
    val owner = LocalLifecycleOwner.current
    var video by remember { mutableStateOf<VideoView?>(null) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) video?.pause() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); video?.stopPlayback() }
    }
    if (failed) Text(stringResource(R.string.preview_failed)) else AndroidView(
        factory = { context -> VideoView(context).apply {
            video = this
            setMediaController(MediaController(context).also { it.setAnchorView(this) })
            setOnErrorListener { _, _, _ -> failed = true; true }
            setOnPreparedListener { start() }
            setVideoURI(Uri.parse(file.id))
        } }, modifier = Modifier.fillMaxWidth().height(260.dp),
    )
}
