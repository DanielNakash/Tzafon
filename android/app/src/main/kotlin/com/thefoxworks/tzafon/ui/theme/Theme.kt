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
 * Material 3 re-themed to the Den look (FR-DESIGN-2) — components inherit
 * Den colors/type/shape and are never left default-Material.
 */
private val DenColorScheme = lightColorScheme(
    primary = Den.rust,
    onPrimary = Den.cream,
    primaryContainer = Den.rust.a(0.14f),
    onPrimaryContainer = Den.rustDeep,
    secondary = Den.amber,
    onSecondary = Den.ink,
    background = Den.bg,
    onBackground = Den.ink,
    surface = Den.surface,
    onSurface = Den.ink,
    surfaceVariant = Den.surfaceAlt,
    onSurfaceVariant = Den.muted,
    outline = Den.line,
    outlineVariant = Den.line2,
    error = Den.due,
    onError = Den.cream,
    surfaceContainer = Den.card,
    surfaceContainerHigh = Den.card,
    surfaceContainerHighest = Den.card,
    surfaceContainerLow = Den.surface,
    inverseSurface = Den.ink,
    inverseOnSurface = Den.surface,
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
fun TzafonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DenColorScheme,
        typography = DenTypography,
        shapes = DenShapes,
        content = content,
    )
}
