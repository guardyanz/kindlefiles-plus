// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Space { val xs = 4.dp; val sm = 8.dp; val md = 16.dp; val lg = 24.dp; val xl = 32.dp; val touch = 48.dp }
private val Paper = lightColorScheme(
    primary = Color(0xFF205C48), onPrimary = Color.White, primaryContainer = Color(0xFFD8EBDD),
    onPrimaryContainer = Color(0xFF153D30), secondary = Color(0xFF725526), secondaryContainer = Color(0xFFF7E4BE),
    onSecondaryContainer = Color(0xFF4B3615), surface = Color(0xFFF8F7F2), onSurface = Color(0xFF242C27),
    surfaceVariant = Color(0xFFEAECE3), onSurfaceVariant = Color(0xFF566057),
    surfaceContainer = Color(0xFFF0F1E9), surfaceContainerLow = Color(0xFFF2F3EC),
    surfaceContainerHighest = Color(0xFFE5E8DE), outline = Color(0xFF737B71), outlineVariant = Color(0xFFD5DACE),
)
private val Ink = darkColorScheme(
    primary = Color(0xFFA5D7BA), onPrimary = Color(0xFF12382B), primaryContainer = Color(0xFF28513E),
    onPrimaryContainer = Color(0xFFC9EED7), secondary = Color(0xFFEABD66),
    secondaryContainer = Color(0xFF564423), onSecondaryContainer = Color(0xFFF7E4BE),
    surface = Color(0xFF141B17), onSurface = Color(0xFFE1E7DE), surfaceVariant = Color(0xFF303A32),
    onSurfaceVariant = Color(0xFFC0CABE), surfaceContainer = Color(0xFF1E2821),
    surfaceContainerLow = Color(0xFF19221C), surfaceContainerHighest = Color(0xFF303B32),
)

@Composable
fun KindleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Ink else Paper, typography = Typography(), content = content)
}
