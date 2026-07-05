package com.thefoxworks.tzafon.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The Fox Works "Den" palette — exact tokens from the design source
 * (task-manager/project/tz/tz-core.jsx TZ) (FR-DESIGN-1).
 */
object Den {
    // ── Den base (warm kraft) ─────────────────────────────
    val bg = Color(0xFFF1E5CF)
    val surface = Color(0xFFFBF4E6)
    val surfaceAlt = Color(0xFFEFE3CB)
    val card = Color(0xFFFFFDF7)
    val ink = Color(0xFF241A12)
    val muted = Color(0xFF7C6A52)
    val faint = Color(0xFFA8967C)
    val rust = Color(0xFFA6421E)
    val rustDeep = Color(0xFF8A3416)
    val amber = Color(0xFFD9913A)
    val due = Color(0xFFB23A2E)
    val green = Color(0xFF5E7A4B)
    val line = Color(0x24241A12)      // rgba(36,26,18,0.14)
    val line2 = Color(0x14241A12)     // rgba(36,26,18,0.08)
    val cream = Color(0xFFFBF4E6)

    // ── v2 state tokens (task state machine) ──────────────
    val frozen = Color(0xFF5E7488)    // cool slate — suspended
    val backlog = Color(0xFF7E7086)   // quiet mauve — parked / someday
    val closed = Color(0xFF9C8C74)    // faded taupe — skipped

    // ── theme accents (3 active themes, harmonized) ───────
    val tHealth = Color(0xFF5E7A4B)
    val tWrite = Color(0xFF8C4A63)
    val tLearn = Color(0xFF3F6E7D)

    /** The three theme accent slots, cycled by Theme.accentSlot. */
    val themeAccents = listOf(tHealth, tWrite, tLearn)
}

/** hex color at alpha — the design's tzA() helper. */
fun Color.a(alpha: Float) = copy(alpha = alpha)
