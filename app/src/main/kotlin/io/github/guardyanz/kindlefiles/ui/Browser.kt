// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.guardyanz.kindlefiles.R
import io.github.guardyanz.kindlefiles.core.Document

internal data class FileActions(
    val open: (Document) -> Unit,
    val external: (Document, Boolean) -> Unit,
    val transfer: (List<Document>, Boolean) -> Unit,
    val rename: (Document) -> Unit,
    val delete: (List<Document>) -> Unit,
    val details: (Document) -> Unit,
    val unzip: (Document) -> Unit,
)

@Composable
internal fun Browser(state: BrowserState, model: FilesViewModel, actions: FileActions, newFolder: () -> Unit, zip: () -> Unit) {
    var options by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { model.back() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.up)) }
            Column(Modifier.weight(1f).padding(Space.sm)) {
                Text(state.folder?.name ?: stringResource(R.string.import_files), style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (state.path.size > 1) state.path.dropLast(1).joinToString(" / ") { it.name } else stringResource(R.string.item_count, state.entries.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            state.folder?.let { folder ->
                val starred = state.favorites.any { it.id == folder.id }
                IconButton(onClick = { model.favorite(folder) }) { Icon(if (starred) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                    stringResource(if (starred) R.string.favorite_remove else R.string.favorite_add), tint = MaterialTheme.colorScheme.primary) }
            }
            Box {
                IconButton(onClick = { options = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.tools)) }
                DropdownMenu(expanded = options, onDismissRequest = { options = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.refresh)) }, onClick = { options = false; model.refresh() }, enabled = state.folder != null)
                    DropdownMenuItem(text = { Text(stringResource(R.string.new_folder)) }, onClick = { options = false; newFolder() }, enabled = state.folder != null)
                    DropdownMenuItem(text = { Text(stringResource(R.string.hidden)) }, trailingIcon = { Checkbox(state.hidden, null) }, onClick = { model.hidden(!state.hidden) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.all_files)) }, onClick = { options = false; model.selectAll() })
                    HorizontalDivider()
                    listOf(Sort.NAME to R.string.sort_name, Sort.DATE to R.string.sort_date, Sort.SIZE to R.string.sort_size).forEach { (sort, label) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, trailingIcon = { if (state.sort == sort) Icon(Icons.Outlined.Check, null) },
                            onClick = { model.sort(sort); options = false })
                    }
                }
            }
        }
        OutlinedTextField(value = state.query, onValueChange = model::query, singleLine = true,
            label = { Text(stringResource(R.string.search)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm))
        if (state.selected.isNotEmpty()) SelectionBar(state, model::clearSelection, actions, zip)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        val files = state.visible
        when {
            files.isEmpty() && !state.loading -> EmptyState(Icons.Outlined.FolderOpen,
                if (state.query.isEmpty()) R.string.empty_folder else R.string.no_results,
                if (state.query.isEmpty()) R.string.empty_folder_hint else R.string.no_results_hint)
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.sm)) {
                items(files, key = { it.id }) { file ->
                    FileRow(file, file.id in state.selected, { model.select(file.id) }, actions,
                        canRename = state.folder != null, selectedMode = state.selected.isNotEmpty())
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(state: BrowserState, clear: () -> Unit, actions: FileActions, zip: () -> Unit) {
    val selection = state.selection
    val filesOnly = selection.isNotEmpty() && selection.none { it.directory }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).horizontalScroll(rememberScrollState()).padding(horizontal = Space.sm),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = clear) { Icon(Icons.Outlined.Close, stringResource(R.string.clear_selection)) }
        Text(stringResource(R.string.files_selected, selection.size), style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { actions.transfer(selection, false) }, enabled = filesOnly) { Text(stringResource(R.string.copy)) }
        TextButton(onClick = { actions.transfer(selection, true) }, enabled = filesOnly) { Text(stringResource(R.string.move)) }
        TextButton(onClick = zip, enabled = filesOnly && state.folder != null) { Text(stringResource(R.string.zip)) }
        TextButton(onClick = { actions.delete(selection) }) { Text(stringResource(R.string.delete)) }
    }
}

@Composable
private fun FileRow(file: Document, selected: Boolean, select: () -> Unit, actions: FileActions, canRename: Boolean, selectedMode: Boolean) {
    var menu by remember { mutableStateOf(false) }
    val description = stringResource(R.string.select, file.name)
    ListItem(
        headlineContent = { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(fileDescription(file), style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            if (file.directory) FolderBadge() else Icon(when {
                file.mime.startsWith("image/") -> Icons.Outlined.Image
                file.mime.startsWith("video/") -> Icons.Outlined.Movie
                file.name.endsWith(".zip", true) -> Icons.Outlined.FolderZip
                file.name.endsWith(".epub", true) || file.mime == "application/pdf" -> Icons.AutoMirrored.Outlined.MenuBook
                else -> Icons.AutoMirrored.Outlined.InsertDriveFile
            }, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(selected, { select() }, modifier = Modifier.semantics { contentDescription = description })
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more, file.name)) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        fun perform(action: () -> Unit) { menu = false; action() }
                        if (!file.directory) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.open)) }, onClick = { perform { actions.external(file, false) } })
                            DropdownMenuItem(text = { Text(stringResource(R.string.share)) }, onClick = { perform { actions.external(file, true) } })
                            DropdownMenuItem(text = { Text(stringResource(R.string.copy)) }, onClick = { perform { actions.transfer(listOf(file), false) } })
                            DropdownMenuItem(text = { Text(stringResource(R.string.move)) }, onClick = { perform { actions.transfer(listOf(file), true) } })
                            if (file.name.endsWith(".zip", true) && canRename) DropdownMenuItem(text = { Text(stringResource(R.string.unzip)) }, onClick = { perform { actions.unzip(file) } })
                        }
                        if (canRename) DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { perform { actions.rename(file) } })
                        DropdownMenuItem(text = { Text(stringResource(R.string.details)) }, onClick = { perform { actions.details(file) } })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }, onClick = { perform { actions.delete(listOf(file)) } })
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable { if (selectedMode) select() else actions.open(file) },
    )
}
