package com.thefoxworks.tzafon.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.R

/**
 * FR-DESIGN-3 — apply on TextStyle used to render user-authored strings
 * (task titles, habit names, cue labels, etc.). Directs Compose to resolve
 * paragraph direction from the first strong character per the Unicode
 * Bidirectional Algorithm, so Hebrew/Arabic content renders RTL with the
 * caret at the correct edge while chrome remains LTR.
 */
fun TextStyle.contentDir(): TextStyle = copy(textDirection = TextDirection.Content)

/**
 * Den type stack (FR-DESIGN-1): Newsreader (serif — titles), Hanken Grotesk
 * (body), Spline Sans Mono (kickers / labels / meta). Bundled TTFs.
 */
object DenType {
    val serif = FontFamily(
        Font(R.font.newsreader_regular, FontWeight.Normal),
        Font(R.font.newsreader_medium, FontWeight.Medium),
        Font(R.font.newsreader_semibold, FontWeight.SemiBold),
        Font(R.font.newsreader_bold, FontWeight.Bold),
        Font(R.font.newsreader_regular_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.newsreader_medium_italic, FontWeight.Medium, FontStyle.Italic),
    )

    val body = FontFamily(
        Font(R.font.hanken_regular, FontWeight.Normal),
        Font(R.font.hanken_medium, FontWeight.Medium),
        Font(R.font.hanken_semibold, FontWeight.SemiBold),
        Font(R.font.hanken_bold, FontWeight.Bold),
    )

    val mono = FontFamily(
        Font(R.font.spline_mono_medium, FontWeight.Medium),
        Font(R.font.spline_mono_semibold, FontWeight.SemiBold),
        Font(R.font.spline_mono_bold, FontWeight.Bold),
    )

    // ── recurring styles from the design ──────────────────
    /** mono 11 / ls 2.5 / caps — "DON'T PANIC" kickers */
    val kicker = TextStyle(fontFamily = mono, fontSize = 11.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.SemiBold)

    /** mono 10.5 / ls 1.4 / caps — section labels */
    val sectionLabel = TextStyle(fontFamily = mono, fontSize = 10.5.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)

    /** mono 11 / ls 1.4 / caps — group headers */
    val groupLabel = TextStyle(fontFamily = mono, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)

    /** serif 37/600 — big screen titles (compact: 28) */
    val h1 = TextStyle(fontFamily = serif, fontSize = 37.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
    val h1Compact = TextStyle(fontFamily = serif, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)

    /** body 16 — task row title */
    val rowTitle = TextStyle(fontFamily = body, fontSize = 16.sp, fontWeight = FontWeight.Normal)

    /** mono 11.5 — header meta line */
    val meta = TextStyle(fontFamily = mono, fontSize = 11.5.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium)

    /** mono 11 — chips */
    val chip = TextStyle(fontFamily = mono, fontSize = 11.sp, letterSpacing = 0.2.sp, fontWeight = FontWeight.Medium)
}
