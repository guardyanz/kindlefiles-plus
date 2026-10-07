// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Static, soft light fields. No blur buffers or animation loop on Fire tablets. */
@Composable
internal fun GlassBackdrop(content: @Composable BoxScope.() -> Unit) {
    val dark = LocalGlassDark.current
    Box(Modifier.fillMaxSize().drawWithCache {
        val base = Brush.linearGradient(if (dark) listOf(Color(0xFF101B25), Color(0xFF263946), Color(0xFF302A24))
            else listOf(Color(0xFFB5C8D6), Color(0xFFE8EBE7), Color(0xFFE5CFB5)), end = Offset(size.width, size.height))
        val sky = Brush.radialGradient(listOf(Color(0xFF83AFC7).copy(alpha = if (dark) .14f else .32f), Color.Transparent),
            center = Offset(size.width * .78f, size.height * .05f), radius = size.maxDimension * .55f)
        val glow = Brush.radialGradient(listOf(Color(0xFFF4EEB8).copy(alpha = if (dark) .09f else .42f), Color.Transparent),
            center = Offset(size.width * .35f, size.height * .9f), radius = size.maxDimension * .5f)
        onDrawBehind { drawRect(base); drawRect(sky); drawRect(glow) }
    }, content = content)
}

/** Tinted frosted glass with a specular rim; text never receives a blur effect. */
@Composable
internal fun GlassPanel(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(28.dp),
                        tint: Color = Color.Unspecified, content: @Composable () -> Unit) {
    val dark = LocalGlassDark.current
    val scheme = MaterialTheme.colorScheme
    val fill = if (tint != Color.Unspecified) tint else if (dark) scheme.surface.copy(alpha = .91f) else Color.White.copy(alpha = .83f)
    val rim = Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .24f else .85f),
        scheme.outlineVariant.copy(alpha = .5f), Color.White.copy(alpha = if (dark) .06f else .45f)))
    Surface(modifier, shape = shape, color = Color.Transparent, contentColor = scheme.onSurface,
        shadowElevation = 2.dp, border = BorderStroke(1.dp, rim)) {
        Box(Modifier.background(Brush.verticalGradient(listOf(fill, fill.copy(alpha = if (dark) .96f else .93f))))) { content() }
    }
}

@Composable
internal fun Modifier.pressFeedback(source: MutableInteractionSource, amount: Float = .975f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val scale by animateFloatAsState(if (pressed && motion) amount else 1f,
        animationSpec = if (motion) spring(dampingRatio = .7f, stiffness = 700f) else snap(), label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
internal fun ExpressiveButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                              tonal: Boolean = false, content: @Composable RowScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val corner by animateDpAsState(if (pressed) 18.dp else 28.dp,
        animationSpec = if (motion) spring(dampingRatio = .75f, stiffness = 700f) else snap(), label = "button shape")
    Button(onClick, modifier.heightIn(min = 52.dp).pressFeedback(source), enabled, shape = RoundedCornerShape(corner),
        interactionSource = source, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        colors = if (tonal) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer) else ButtonDefaults.buttonColors(), content = content)
}
