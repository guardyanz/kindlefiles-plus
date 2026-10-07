// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.ui

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Space { val xs = 4.dp; val sm = 8.dp; val md = 16.dp; val lg = 24.dp; val xl = 32.dp; val touch = 48.dp }
internal val LocalMotionEnabled = staticCompositionLocalOf { true }
internal val LocalGlassDark = staticCompositionLocalOf { false }

private val Paper = lightColorScheme(
    primary = Color(0xFF294E63), onPrimary = Color.White, primaryContainer = Color(0xFFD5E6EE),
    onPrimaryContainer = Color(0xFF163444), secondary = Color(0xFF5A541D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0EFAE), onSecondaryContainer = Color(0xFF393711),
    tertiary = Color(0xFF765741), tertiaryContainer = Color(0xFFF0DCCA), onTertiaryContainer = Color(0xFF433021),
    background = Color(0xFFE8EEF1), onBackground = Color(0xFF182831),
    surface = Color(0xFFF6F8F9), onSurface = Color(0xFF182831), surfaceVariant = Color(0xFFDEE7EB),
    onSurfaceVariant = Color(0xFF475963), surfaceContainer = Color(0xFFECF1F3),
    surfaceContainerLow = Color(0xFFF1F5F6), surfaceContainerHighest = Color(0xFFDCE6EB),
    outline = Color(0xFF6D7F89), outlineVariant = Color(0xFFBDCDD5),
)
private val Ink = darkColorScheme(
    primary = Color(0xFFAAD1E4), onPrimary = Color(0xFF103344), primaryContainer = Color(0xFF294B5E),
    onPrimaryContainer = Color(0xFFD3EBF5), secondary = Color(0xFFE5E89C), onSecondary = Color(0xFF30330A),
    secondaryContainer = Color(0xFF4A4C25), onSecondaryContainer = Color(0xFFF3F4BF),
    tertiary = Color(0xFFE4BFA0), tertiaryContainer = Color(0xFF604632), onTertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFF101A21), onBackground = Color(0xFFE4EEF3),
    surface = Color(0xFF16232C), onSurface = Color(0xFFE4EEF3), surfaceVariant = Color(0xFF31434E),
    onSurfaceVariant = Color(0xFFB8CAD5), surfaceContainer = Color(0xFF21333E),
    surfaceContainerLow = Color(0xFF1B2B35), surfaceContainerHighest = Color(0xFF334854),
    outline = Color(0xFF879EAB), outlineVariant = Color(0xFF4F6673),
)
private val Type = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KindleTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    var motion by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { motion = ValueAnimator.areAnimatorsEnabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalMotionEnabled provides motion, LocalGlassDark provides dark) {
        MaterialExpressiveTheme(
            colorScheme = if (dark) Ink else Paper, motionScheme = MotionScheme.expressive(), typography = Type,
            shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp)),
            content = content,
        )
    }
}
