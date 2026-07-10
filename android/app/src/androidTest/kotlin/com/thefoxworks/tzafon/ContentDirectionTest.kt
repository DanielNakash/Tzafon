package com.thefoxworks.tzafon

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import com.thefoxworks.tzafon.ui.theme.contentDir
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-DESIGN-3 — content-direction (RTL) for user-authored text.
 *
 * The behavioural contract: styles produced by [contentDir] carry
 * [TextDirection.Content], which tells Compose to resolve paragraph direction
 * from the first strong character (Unicode Bidi Algorithm). Chrome stays LTR;
 * user-authored strings that start with Hebrew/Arabic characters render RTL
 * with the caret at the correct edge.
 *
 * We cover two invariants:
 *   1. [contentDir] mutates the style's `textDirection` — a static equality
 *      assertion covers the FR-DESIGN-3 core promise.
 *   2. Hebrew and English strings both render (nothing regresses in the LTR
 *      host chrome) with the resolved direction still LTR at the layout root
 *      — the chrome is unaffected by content direction on individual Texts.
 */
@RunWith(AndroidJUnit4::class)
class ContentDirectionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun contentDirExtension_setsTextDirectionContent() {
        val base = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, fontWeight = FontWeight.Normal)
        val styled = base.contentDir()
        assertEquals(TextDirection.Content, styled.textDirection)
        // Every other property is preserved — this is a copy, not a replace.
        assertEquals(base.fontFamily, styled.fontFamily)
        assertEquals(base.fontSize, styled.fontSize)
        assertEquals(base.fontWeight, styled.fontWeight)
    }

    @Test
    fun hebrewAndEnglishTitles_coExistUnderLtrChrome() {
        val hebrewTitle = "לרוץ בבוקר"
        val englishTitle = "Run in the morning"
        rule.setContent {
            TzafonTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        Text(hebrewTitle, style = DenType.rowTitle.contentDir())
                        Text(englishTitle, style = DenType.rowTitle.contentDir())
                    }
                }
            }
        }
        rule.onNodeWithText(hebrewTitle).assertIsDisplayed()
        rule.onNodeWithText(englishTitle).assertIsDisplayed()
    }

    /**
     * Regression guard: applying [contentDir] to a style that is later re-copied
     * (as happens inside [BasicTextField.decorationBox] placeholders) must still
     * carry [TextDirection.Content]. If someone reorders `.copy(...).contentDir()`
     * to `.contentDir().copy(...)`, this test still passes as long as
     * `TextDirection.Content` survives the intermediate copy.
     */
    @Test
    fun contentDir_survivesFurtherCopyChain() {
        val chained = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp)
            .contentDir()
            .copy(fontWeight = FontWeight.SemiBold)
        assertEquals(TextDirection.Content, chained.textDirection)
    }

}
