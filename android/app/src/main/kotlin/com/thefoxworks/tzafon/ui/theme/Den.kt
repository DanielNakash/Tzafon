package com.thefoxworks.tzafon.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * FR-DESIGN-4 — the colour system is a [Palette] data class resolved through the
 * [LocalPalette] composition local, so every `Tz.colors.xxx` access renders in
 * the user-selected palette. Five palettes ship; **Den** (the Fox Works identity,
 * `FR-DESIGN-1`) is the default and the aesthetic anchor (`FR-DESIGN-4.1`).
 *
 * Only *colour* tokens flow through here — type, spacing, radius and shadow are
 * unchanged (`FR-DESIGN-4.7`). No dark mode (`FR-DESIGN-4.8`); all five are light.
 *
 * The 21 tokens (`FR-DESIGN-4.2`): 15 base + 3 v2 state (`frozen`/`backlog`/
 * `closed`) + 3 theme-accent slots (`tHealth`/`tWrite`/`tLearn`), plus the
 * computed [themeAccents] list that feeds `Theme.accentSlot`.
 */
data class Palette(
    // ── base ──────────────────────────────────────────────
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val rust: Color,
    val rustDeep: Color,
    val amber: Color,
    val due: Color,
    val green: Color,
    val line: Color,
    val line2: Color,
    val cream: Color,
    // ── v2 state tokens (task state machine) ──────────────
    val frozen: Color,
    val backlog: Color,
    val closed: Color,
    // ── theme accents (3 active themes, harmonized) ───────
    val tHealth: Color,
    val tWrite: Color,
    val tLearn: Color,
) {
    /** The three theme accent slots, cycled by Theme.accentSlot. */
    val themeAccents: List<Color> get() = listOf(tHealth, tWrite, tLearn)
}

/**
 * Den — the Fox Works "Den" palette (warm kraft). Exact tokens from the design
 * source (task-manager/project/tz/tz-core.jsx TZ), carried verbatim from the
 * v2.5.0 `object Den` so the Den setting is a zero-pixel-diff (`FR-DESIGN-4.1`).
 *
 * Contrast (WCAG 2.1, computed design-time): the ink pairs are the body text and
 * clear AA with wide margin; `muted` is secondary text (≥14sp) at the shipped
 * v2.5.0 value (large-text AA); `faint` is decorative micro-labels only.
 *   ink/bg 13.7 · ink/surface 15.6 · ink/card 16.8 · muted/bg 4.2 · faint/bg 2.3
 */
val denPalette = Palette(
    bg = Color(0xFFF1E5CF),
    surface = Color(0xFFFBF4E6),
    surfaceAlt = Color(0xFFEFE3CB),
    card = Color(0xFFFFFDF7),
    ink = Color(0xFF241A12),
    muted = Color(0xFF7C6A52),
    faint = Color(0xFFA8967C),
    rust = Color(0xFFA6421E),
    rustDeep = Color(0xFF8A3416),
    amber = Color(0xFFD9913A),
    due = Color(0xFFB23A2E),
    green = Color(0xFF5E7A4B),
    line = Color(0x24241A12),      // ink @ 0.14
    line2 = Color(0x14241A12),     // ink @ 0.08
    cream = Color(0xFFFBF4E6),
    frozen = Color(0xFF5E7488),    // cool slate — suspended
    backlog = Color(0xFF7E7086),   // quiet mauve — parked / someday
    closed = Color(0xFF9C8C74),    // faded taupe — skipped
    tHealth = Color(0xFF5E7A4B),
    tWrite = Color(0xFF8C4A63),
    tLearn = Color(0xFF3F6E7D),
)

/**
 * Blue — cool paper, strong-blue primary. Den's warm ink tilts barely-blue to
 * keep the translated identity (`FR-DESIGN-4.3`); completion green stays green,
 * `due` stays red — both distinct from the blue primary.
 *
 * Contrast: ink/bg 14.3 · ink/surface 15.8 · ink/card 16.7 · muted/bg 5.6 · faint/bg 2.5
 */
val bluePalette = Palette(
    bg = Color(0xFFE6EBF3),
    surface = Color(0xFFF3F6FB),
    surfaceAlt = Color(0xFFDBE2EE),
    card = Color(0xFFFBFCFE),
    ink = Color(0xFF141C28),
    muted = Color(0xFF4F5D70),
    faint = Color(0xFF8996A8),
    rust = Color(0xFF1F5FA6),
    rustDeep = Color(0xFF174A82),
    amber = Color(0xFFC88A2E),
    due = Color(0xFFC23B33),
    green = Color(0xFF4F7A4A),
    line = Color(0x24141C28),
    line2 = Color(0x14141C28),
    cream = Color(0xFFEEF3FB),
    frozen = Color(0xFF5E7488),
    backlog = Color(0xFF7E7086),
    closed = Color(0xFF9C8C74),
    tHealth = Color(0xFF4F7A4A),
    tWrite = Color(0xFF8C4A63),
    tLearn = Color(0xFF2E7C86),
)

/**
 * Green — sage paper, forest primary. Completion `green` is a brighter, cooler
 * success-green kept distinct from the forest primary (`FR-DESIGN-4.3`); the
 * `tHealth` slot shifts teal-green to avoid colliding with the primary.
 *
 * Contrast: ink/bg 13.2 · ink/surface 14.7 · ink/card 15.7 · muted/bg 5.3 · faint/bg 2.3
 */
val greenPalette = Palette(
    bg = Color(0xFFE4EBDD),
    surface = Color(0xFFF1F6EC),
    surfaceAlt = Color(0xFFDBE4D2),
    card = Color(0xFFFBFDF8),
    ink = Color(0xFF1A2412),
    muted = Color(0xFF566246),
    faint = Color(0xFF93A088),
    rust = Color(0xFF37753A),
    rustDeep = Color(0xFF285C2B),
    amber = Color(0xFFCE9433),
    due = Color(0xFFBE4030),
    green = Color(0xFF6BA24E),
    line = Color(0x241A2412),
    line2 = Color(0x141A2412),
    cream = Color(0xFFEFF6E9),
    frozen = Color(0xFF5E7488),
    backlog = Color(0xFF7E7086),
    closed = Color(0xFF9C8C74),
    tHealth = Color(0xFF2E8B7A),
    tWrite = Color(0xFF8C4A63),
    tLearn = Color(0xFF3F6E7D),
)

/**
 * Magenta — blush paper, warm-magenta primary. `due` shifts orange-red so it
 * never collides with the magenta primary (`FR-DESIGN-4.3`); `backlog` shifts to
 * a cooler grey-mauve so the someday state stays distinct (`FR-DESIGN-4.10`); the
 * `tWrite` slot moves to plum to clear the primary.
 *
 * Contrast: ink/bg 14.0 · ink/surface 15.5 · ink/card 16.7 · muted/bg 5.0 · faint/bg 2.4
 */
val magentaPalette = Palette(
    bg = Color(0xFFF1E4EC),
    surface = Color(0xFFFAF0F5),
    surfaceAlt = Color(0xFFE9DAE2),
    card = Color(0xFFFFFAFC),
    ink = Color(0xFF281521),
    muted = Color(0xFF7A5766),
    faint = Color(0xFFAE8C9C),
    rust = Color(0xFFB23A78),
    rustDeep = Color(0xFF8E2C60),
    amber = Color(0xFFD9913A),
    due = Color(0xFFC24A2E),
    green = Color(0xFF5E7A4B),
    line = Color(0x24281521),
    line2 = Color(0x14281521),
    cream = Color(0xFFFBF0F6),
    frozen = Color(0xFF5E7488),
    backlog = Color(0xFF6E6A80),   // FR-DESIGN-4.10 — cooler grey-mauve vs magenta
    closed = Color(0xFF9C8C74),
    tHealth = Color(0xFF5E7A4B),
    tWrite = Color(0xFF6E4A8C),
    tLearn = Color(0xFF3F6E7D),
)

/**
 * Teal — mint paper, mid-teal primary. The `tLearn` slot shifts to blue so the
 * three theme accents stay distinct from the teal primary (`FR-DESIGN-4.3`).
 *
 * Contrast: ink/bg 13.4 · ink/surface 14.9 · ink/card 15.9 · muted/bg 5.3 · faint/bg 2.2
 */
val tealPalette = Palette(
    bg = Color(0xFFDFEBE9),
    surface = Color(0xFFEEF6F4),
    surfaceAlt = Color(0xFFD3E2DF),
    card = Color(0xFFF9FDFC),
    ink = Color(0xFF122320),
    muted = Color(0xFF4C6360),
    faint = Color(0xFF88A29D),
    rust = Color(0xFF1E877E),
    rustDeep = Color(0xFF15665F),
    amber = Color(0xFFCE8A2E),
    due = Color(0xFFC13B36),
    green = Color(0xFF5E7A4B),
    line = Color(0x24122320),
    line2 = Color(0x14122320),
    cream = Color(0xFFEAF6F3),
    frozen = Color(0xFF5E7488),
    backlog = Color(0xFF7E7086),
    closed = Color(0xFF9C8C74),
    tHealth = Color(0xFF5E7A4B),
    tWrite = Color(0xFF8C4A63),
    tLearn = Color(0xFF3A6E8A),
)

/** DM-PREF-1 — resolve a persisted palette name; unknown / null → Den. */
fun paletteFor(name: String?): Palette = when (name) {
    "blue" -> bluePalette
    "green" -> greenPalette
    "magenta" -> magentaPalette
    "teal" -> tealPalette
    else -> denPalette
}

/** The five shipping palettes, in picker order (Den first). */
val allPalettes: List<Pair<String, Palette>> = listOf(
    "den" to denPalette,
    "blue" to bluePalette,
    "green" to greenPalette,
    "magenta" to magentaPalette,
    "teal" to tealPalette,
)

/** Display name for a palette key (Settings row + PaletteSheet). */
fun paletteLabel(name: String): String = when (name) {
    "blue" -> "Blue"
    "green" -> "Green"
    "magenta" -> "Magenta"
    "teal" -> "Teal"
    else -> "Den"
}

/**
 * FR-DESIGN-4 — the active palette. Provided at the composition root from
 * `SettingsStore.palette`; defaults to Den before the store's first emission.
 */
val LocalPalette = staticCompositionLocalOf { denPalette }

/** `Tz.colors.xxx` — the app-wide colour accessor, resolved at render time. */
object Tz {
    val colors: Palette
        @Composable @ReadOnlyComposable get() = LocalPalette.current
}

/**
 * Thin compatibility shim for non-composable / test access to the default
 * palette (`FR-DESIGN-4.1`). Production composables read [Tz].colors instead.
 */
object Den {
    val default: Palette get() = denPalette
}

/** hex color at alpha — the design's tzA() helper. */
fun Color.a(alpha: Float) = copy(alpha = alpha)
