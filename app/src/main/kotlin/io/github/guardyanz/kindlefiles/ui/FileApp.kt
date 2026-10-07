// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
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
    val motion = LocalMotionEnabled.current
    GlassBackdrop { Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) SidePanel(state, model::page, connect, Modifier.width(268.dp).fillMaxHeight().padding(Space.md))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    BrandHeader { about = true }
                    AnimatedContent(state.page, modifier = Modifier.weight(1f).fillMaxWidth(), label = "destination",
                        transitionSpec = {
                            (fadeIn(if (motion) tween(220) else snap()) + slideInVertically(if (motion) tween(220) else snap()) { if (motion) 16 else 0 })
                                .togetherWith(fadeOut(if (motion) tween(100) else snap()))
                        }) { page -> when (page) {
                        Page.PLACES -> Places(state, connect, pickFiles, { model.openFolder(it, true) }, model::forget)
                        Page.FAVORITES -> Favorites(state, { model.openFolder(it, true) }, model::favorite)
                        Page.TOOLS -> Tools(state, model::scan, { model.page(Page.FILES) })
                        Page.FILES -> Browser(state, model, actions,
                            newFolder = { nameRequest = NameRequest(R.string.new_folder) { model.newFolder(it) } },
                            zip = { val files = state.selection; nameRequest = NameRequest(R.string.archive_name, "Archive.zip") { model.zip(files, it) } },
                        )
                    } }
                    if (!wide) NavigationTabs(state.page, model::page)
                }
            }
        }
    } }
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
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) {
            Icon(Icons.Outlined.FolderCopy, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(10.dp).size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
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
private fun Destination(target: Page, label: Int, icon: ImageVector, selected: Boolean, wide: Boolean,
                        change: (Page) -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val motion = LocalMotionEnabled.current
    val background by animateColorAsState(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = if (motion) tween(180) else snap(), label = "navigation color")
    val scale by animateFloatAsState(if (selected && motion) 1.08f else 1f,
        animationSpec = if (motion) spring(dampingRatio = .65f, stiffness = 600f) else snap(), label = "navigation icon")
    val ink = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier.pressFeedback(source).selectable(selected, interactionSource = source, indication = ripple(), role = Role.Tab,
        onClick = { change(target) }), shape = RoundedCornerShape(24.dp), color = background, contentColor = ink) {
        if (wide) Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale })
            Text(stringResource(label), style = MaterialTheme.typography.titleSmall, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        } else Column(Modifier.heightIn(min = 72.dp).padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale })
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, maxLines = 3, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun NavigationTabs(page: Page, change: (Page) -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(32.dp)) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            destinations.forEach { (target, label, icon) -> Destination(target, label, icon, page == target, false, change, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SidePanel(state: BrowserState, change: (Page) -> Unit, connect: () -> Unit, modifier: Modifier) {
    GlassPanel(modifier, shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Spacer(Modifier.height(Space.lg))
            Text(stringResource(R.string.local_badge), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(Space.md))
            destinations.forEach { (target, label, icon) -> Destination(target, label, icon, state.page == target, true, change) }
            Spacer(Modifier.weight(1f))
            ExpressiveButton(onClick = connect, tonal = true, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(Space.sm)); Text(stringResource(R.string.connect))
            }
            Text(stringResource(R.string.offline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun FolderArtwork() {
    Box(Modifier.size(208.dp, 184.dp), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.size(152.dp, 112.dp).graphicsLayer { rotationZ = -14f; translationX = -14.dp.toPx(); translationY = -10.dp.toPx() },
            tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .75f), shape = RoundedCornerShape(24.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FolderOpen, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        GlassPanel(Modifier.size(158.dp, 116.dp).graphicsLayer { rotationZ = 10f; translationX = 16.dp.toPx(); translationY = 16.dp.toPx() },
            tint = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .88f), shape = RoundedCornerShape(24.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FolderOpen, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        }
    }
}

@Composable
private fun Places(state: BrowserState, connect: () -> Unit, pick: () -> Unit, open: (Document) -> Unit, forget: (Document) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.md)) {
        item {
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomEnd = 36.dp, bottomStart = 12.dp)) {
                BoxWithConstraints {
                    val art = maxWidth >= 620.dp
                    Row(Modifier.fillMaxWidth().padding(Space.lg), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                            Text(stringResource(R.string.local_badge), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.welcome), style = MaterialTheme.typography.displaySmall)
                            Text(stringResource(R.string.welcome_hint), modifier = Modifier.widthIn(max = 420.dp),
                                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ExpressiveButton(onClick = connect) {
                                Icon(Icons.Outlined.CreateNewFolder, null); Spacer(Modifier.width(Space.sm)); Text(stringResource(R.string.connect))
                            }
                        }
                        if (art) FolderArtwork()
                    }
                }
            }
        }
        item { Text(stringResource(R.string.connect_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        if (state.roots.isNotEmpty()) item { SectionTitle(stringResource(R.string.places)) }
        items(state.roots, key = { it.id }) { root ->
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                ListItem(headlineContent = { Text(root.name) }, supportingContent = { Text(stringResource(R.string.browse)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent), leadingContent = { FolderBadge() },
                    trailingContent = { IconButton(onClick = { forget(root) }) { Icon(Icons.Outlined.LinkOff, stringResource(R.string.forget)) } },
                    modifier = Modifier.clickable { open(root) })
            }
        }
        item {
            val source = remember { MutableInteractionSource() }
            GlassPanel(Modifier.fillMaxWidth().pressFeedback(source).clickable(interactionSource = source, indication = ripple(), onClick = pick)) {
                Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    FolderBadge(Icons.Outlined.Download)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Text(stringResource(R.string.import_files), fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.import_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
private fun Favorites(state: BrowserState, open: (Document) -> Unit, remove: (Document) -> Unit) {
    if (state.favorites.isEmpty()) EmptyState(Icons.Outlined.StarOutline, R.string.no_favorites, R.string.no_favorites_hint)
    else LazyColumn(contentPadding = PaddingValues(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        item { SectionTitle(stringResource(R.string.favorites)) }
        items(state.favorites, key = { it.id }) { folder ->
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                ListItem(headlineContent = { Text(folder.name) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { FolderBadge() }, modifier = Modifier.clickable { open(folder) },
                    trailingContent = { IconButton(onClick = { remove(folder) }) { Icon(Icons.Outlined.Star, stringResource(R.string.favorite_remove)) } })
            }
        }
    }
}

@Composable
private fun Tools(state: BrowserState, scan: () -> Unit, browse: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 640.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Text(stringResource(R.string.tools_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.tools_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToolCard(Icons.Outlined.ContentCopy, R.string.duplicates, R.string.duplicates_hint) {
                Text(state.folder?.name ?: stringResource(R.string.choose_first), style = MaterialTheme.typography.bodySmall)
                ExpressiveButton(onClick = scan, enabled = state.folder != null, tonal = true) { Text(stringResource(R.string.scan)) }
            }
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                ToolCard(Icons.Outlined.FolderZip, R.string.zip, R.string.zip_hint, Modifier.weight(1f)) {
                    TextButton(onClick = browse) { Text(stringResource(R.string.browse)) }
                }
                ToolCard(Icons.Outlined.Widgets, R.string.widget_title, R.string.widget_hint, Modifier.weight(1f)) {}
            } else {
                ToolCard(Icons.Outlined.FolderZip, R.string.zip, R.string.zip_hint) { TextButton(onClick = browse) { Text(stringResource(R.string.browse)) } }
                ToolCard(Icons.Outlined.Widgets, R.string.widget_title, R.string.widget_hint) {}
            }
        }
    }
}

@Composable
private fun ToolCard(icon: ImageVector, title: Int, description: Int, modifier: Modifier = Modifier,
                     action: @Composable ColumnScope.() -> Unit) {
    GlassPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FolderBadge(icon)
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(description), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 640.dp))
            action()
        }
    }
}

@Composable
internal fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = Space.sm)) }

@Composable
internal fun EmptyState(icon: ImageVector, title: Int, description: Int) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlassPanel(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            Column(Modifier.padding(Space.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
                FolderBadge(icon)
                Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(stringResource(description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun FolderBadge(icon: ImageVector = Icons.Outlined.Folder) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(12.dp).size(24.dp))
    }
}

@Composable
internal fun fileDescription(file: Document): String {
    if (file.directory) return stringResource(R.string.folder)
    val size = Formatter.formatFileSize(LocalContext.current, file.size)
    val date = if (file.modified > 0) DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(file.modified)) else stringResource(R.string.unknown_date)
    return "$size · $date"
}
