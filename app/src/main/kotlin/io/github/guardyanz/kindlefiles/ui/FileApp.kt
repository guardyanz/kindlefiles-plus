// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.guardyanz.kindlefiles.R
import io.github.guardyanz.kindlefiles.core.Document
import java.text.DateFormat
import java.util.Date

private data class NameRequest(val title: Int, val initial: String = "", val action: (String) -> Unit)

@Composable
fun FileApp(model: FilesViewModel, connect: () -> Unit, pickFiles: () -> Unit,
            transfer: (List<Document>, Boolean) -> Unit, external: (Document, Boolean) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var nameRequest by remember { mutableStateOf<NameRequest?>(null) }
    var deleteRequest by remember { mutableStateOf<List<Document>?>(null) }
    var detail by remember { mutableStateOf<Document?>(null) }
    var preview by remember { mutableStateOf<Document?>(null) }
    var about by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); model.dismissMessage() } }
    BackHandler(state.page != Page.PLACES || state.selected.isNotEmpty() || state.busy) { model.back() }
    val actions = FileActions(
        open = { if (it.directory) model.openFolder(it) else if (it.mime.startsWith("image/") || it.mime.startsWith("video/")) preview = it else external(it, false) },
        external = external, transfer = transfer,
        rename = { file -> nameRequest = NameRequest(R.string.rename, file.name) { model.rename(file, it) } },
        delete = { deleteRequest = it }, details = { detail = it },
        unzip = { file -> nameRequest = NameRequest(R.string.unzip, file.name.substringBeforeLast('.') + "-extracted") { model.unzip(file, it) } },
    )
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) SidePanel(state, model::page, connect, Modifier.width(248.dp).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    BrandHeader { about = true }
                    if (!wide) NavigationTabs(state.page, model::page)
                    when (state.page) {
                        Page.PLACES -> Places(state, connect, pickFiles, { model.openFolder(it, true) }, model::forget)
                        Page.FAVORITES -> Favorites(state, { model.openFolder(it, true) }, model::favorite)
                        Page.TOOLS -> Tools(state, model::scan, { model.page(Page.FILES) })
                        Page.FILES -> Browser(state, model, actions,
                            newFolder = { nameRequest = NameRequest(R.string.new_folder) { model.newFolder(it) } },
                            zip = { val files = state.selection; nameRequest = NameRequest(R.string.archive_name, "Archive.zip") { model.zip(files, it) } },
                        )
                    }
                }
            }
        }
    }
    nameRequest?.let { request -> NameDialog(request.title, request.initial, { nameRequest = null }) { value ->
        request.action(value); nameRequest = null
    } }
    deleteRequest?.let { files ->
        AlertDialog(onDismissRequest = { deleteRequest = null }, title = { Text(stringResource(R.string.delete_title, files.size)) },
            text = { Text(stringResource(R.string.delete_hint, files.take(8).joinToString("\n") { it.name })) },
            confirmButton = { TextButton(onClick = { model.delete(files); deleteRequest = null }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteRequest = null }) { Text(stringResource(R.string.cancel)) } })
    }
    detail?.let { DetailsDialog(it) { detail = null } }
    preview?.let { PreviewDialog(it, { preview = null }, { external(it, false) }) }
    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text(stringResource(R.string.about)) },
        text = { Text(stringResource(R.string.about_body), modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { about = false }) { Text(stringResource(R.string.close)) } })
    state.error?.let { error ->
        AlertDialog(onDismissRequest = model::dismissError, title = { Text(stringResource(R.string.error_title)) }, text = { Text(error) },
            confirmButton = { TextButton(onClick = { model.dismissError(); model.refresh() }) { Text(stringResource(R.string.retry)) } },
            dismissButton = { TextButton(onClick = model::dismissError) { Text(stringResource(R.string.close)) } })
    }
    state.duplicates?.let { groups -> DuplicateDialog(groups, model::dismissDuplicates, { actions.open(it) }) { file ->
        model.dismissDuplicates(); deleteRequest = listOf(file)
    } }
    if (state.busy) AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.working)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(state.progress, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.keep_open), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = model::cancel) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun BrandHeader(about: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.FolderCopy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(Space.sm))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        IconButton(onClick = about) { Icon(Icons.Outlined.Info, stringResource(R.string.about)) }
    }
}

private val destinations = listOf(
    Triple(Page.PLACES, R.string.places, Icons.Outlined.FolderOpen),
    Triple(Page.FILES, R.string.browse, Icons.AutoMirrored.Outlined.List),
    Triple(Page.FAVORITES, R.string.favorites, Icons.Outlined.StarOutline),
    Triple(Page.TOOLS, R.string.tools, Icons.Outlined.Build),
)

@Composable
private fun NavigationTabs(page: Page, change: (Page) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        destinations.forEach { (target, label, _) ->
            FilterChip(selected = page == target, onClick = { change(target) }, label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = Space.touch))
        }
    }
}

@Composable
private fun SidePanel(state: BrowserState, change: (Page) -> Unit, connect: () -> Unit, modifier: Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Spacer(Modifier.height(Space.xl))
        Text(stringResource(R.string.local_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Space.lg))
        destinations.forEach { (target, label, icon) -> NavigationDrawerItem(
            label = { Text(stringResource(label)) }, selected = state.page == target,
            onClick = { change(target) }, icon = { Icon(icon, null) }) }
        Spacer(Modifier.weight(1f))
        FilledTonalButton(onClick = connect, modifier = Modifier.fillMaxWidth().heightIn(min = Space.touch)) {
            Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(Space.sm)); Text(stringResource(R.string.connect))
        }
        Text(stringResource(R.string.offline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Space.sm))
    }
}

@Composable
private fun Places(state: BrowserState, connect: () -> Unit, pick: () -> Unit, open: (Document) -> Unit, forget: (Document) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    Text(stringResource(R.string.local_badge), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.welcome_hint), modifier = Modifier.widthIn(max = 480.dp), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = connect, modifier = Modifier.heightIn(min = Space.touch)) {
                        Icon(Icons.Outlined.CreateNewFolder, null); Spacer(Modifier.width(Space.sm)); Text(stringResource(R.string.connect))
                    }
                }
            }
        }
        item { Text(stringResource(R.string.connect_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        if (state.roots.isNotEmpty()) item { SectionTitle(stringResource(R.string.places)) }
        items(state.roots, key = { it.id }) { root ->
            ListItem(headlineContent = { Text(root.name) }, supportingContent = { Text(stringResource(R.string.browse)) },
                leadingContent = { FolderBadge() }, trailingContent = { IconButton(onClick = { forget(root) }) { Icon(Icons.Outlined.LinkOff, stringResource(R.string.forget)) } },
                modifier = Modifier.clickable { open(root) })
        }
        item { OutlinedCard(onClick = pick, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Icon(Icons.Outlined.Download, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(stringResource(R.string.import_files), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.import_hint), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
            }
        } }
    }
}

@Composable
private fun Favorites(state: BrowserState, open: (Document) -> Unit, remove: (Document) -> Unit) {
    if (state.favorites.isEmpty()) EmptyState(Icons.Outlined.StarOutline, R.string.no_favorites, R.string.no_favorites_hint)
    else LazyColumn(contentPadding = PaddingValues(Space.lg)) {
        item { SectionTitle(stringResource(R.string.favorites)) }
        items(state.favorites, key = { it.id }) { folder -> ListItem(headlineContent = { Text(folder.name) },
            leadingContent = { FolderBadge() }, modifier = Modifier.clickable { open(folder) },
            trailingContent = { IconButton(onClick = { remove(folder) }) { Icon(Icons.Outlined.Star, stringResource(R.string.favorite_remove)) } }) }
    }
}

@Composable
private fun Tools(state: BrowserState, scan: () -> Unit, browse: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Text(stringResource(R.string.tools_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.tools_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ToolCard(Icons.Outlined.ContentCopy, R.string.duplicates, R.string.duplicates_hint) {
            Text(state.folder?.name ?: stringResource(R.string.choose_first), style = MaterialTheme.typography.bodySmall)
            Button(onClick = scan, enabled = state.folder != null, modifier = Modifier.heightIn(min = Space.touch)) { Text(stringResource(R.string.scan)) }
        }
        ToolCard(Icons.Outlined.FolderZip, R.string.zip, R.string.zip_hint) {
            TextButton(onClick = browse) { Text(stringResource(R.string.browse)) }
        }
        ToolCard(Icons.Outlined.Widgets, R.string.widget_title, R.string.widget_hint) {}
    }
}

@Composable
private fun ToolCard(icon: ImageVector, title: Int, description: Int, action: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            action()
        }
    }
}

@Composable
internal fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = Space.sm)) }

@Composable
internal fun EmptyState(icon: ImageVector, title: Int, description: Int) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(Space.md))
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Space.sm))
        Text(stringResource(description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun FolderBadge() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
        Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(12.dp))
    }
}

@Composable
internal fun fileDescription(file: Document): String {
    if (file.directory) return stringResource(R.string.folder)
    val size = Formatter.formatFileSize(LocalContext.current, file.size)
    val date = if (file.modified > 0) DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(file.modified)) else stringResource(R.string.unknown_date)
    return "$size · $date"
}
