package com.thefoxworks.tzafon.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 re-themed to the Den look (FR-DESIGN-2) — components inherit the
 * active palette's colors/type/shape and are never left default-Material.
 * FR-DESIGN-4: the scheme is derived from the selected [Palette] at render time.
 */
private fun colorSchemeFor(p: Palette) = lightColorScheme(
    primary = p.rust,
    onPrimary = p.cream,
    primaryContainer = p.rust.a(0.14f),
    onPrimaryContainer = p.rustDeep,
    secondary = p.amber,
    onSecondary = p.ink,
    background = p.bg,
    onBackground = p.ink,
    surface = p.surface,
    onSurface = p.ink,
    surfaceVariant = p.surfaceAlt,
    onSurfaceVariant = p.muted,
    outline = p.line,
    outlineVariant = p.line2,
    error = p.due,
    onError = p.cream,
    surfaceContainer = p.card,
    surfaceContainerHigh = p.card,
    surfaceContainerHighest = p.card,
    surfaceContainerLow = p.surface,
    inverseSurface = p.ink,
    inverseOnSurface = p.surface,
)

private val DenTypography = Typography(
    displayLarge = DenType.h1,
    headlineLarge = TextStyle(fontFamily = DenType.serif, fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineMedium = DenType.h1Compact,
    titleLarge = TextStyle(fontFamily = DenType.serif, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = DenType.serif, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = DenType.body, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp),
    labelLarge = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    labelMedium = DenType.chip,
    labelSmall = DenType.sectionLabel,
)

private val DenShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(13.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun TzafonTheme(palette: Palette = LocalPalette.current, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorSchemeFor(palette),
        typography = DenTypography,
        shapes = DenShapes,
        content = content,
    )
}
