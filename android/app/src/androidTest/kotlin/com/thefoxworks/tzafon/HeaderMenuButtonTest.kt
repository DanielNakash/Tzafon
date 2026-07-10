package com.thefoxworks.tzafon

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.ui.components.HeaderMenuButton
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-NAV-3 — the hamburger swap for the header menu trigger on Today,
 * Planning and All Tasks. `HeaderMenuButton` is the single composable those
 * three screens now embed in the `RustHeader.right` slot, so verifying the
 * button in isolation covers the swap without spinning up a full screen
 * with its ViewModel.
 *
 * Acceptance criteria exercised at the composable level:
 *   1. content description "Menu" (FR-NAV-3.2 — replaces "The Fox Works");
 *   2. tap fires the caller's callback exactly once (FR-NAV-3.3 — same sheet
 *      opens; sheet contents are unchanged and tested in nav tests).
 */
@RunWith(AndroidJUnit4::class)
class HeaderMenuButtonTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rendersWithMenuContentDescription() {
        rule.setContent {
            TzafonTheme {
                HeaderMenuButton(onClick = {})
            }
        }

        rule.onNodeWithContentDescription("Menu").assertIsDisplayed()
        rule.onNodeWithContentDescription("Menu").assertHasClickAction()
    }

    @Test
    fun tapFiresCallback() {
        var taps = 0
        rule.setContent {
            TzafonTheme {
                HeaderMenuButton(onClick = { taps++ })
            }
        }

        rule.onNodeWithContentDescription("Menu").performClick()
        assertEquals("menu tap fires exactly once", 1, taps)
    }

    /**
     * FR-NAV-3.2 — no leftover "The Fox Works" identity string on the menu
     * trigger. The roundel is preserved on identity surfaces (Welcome,
     * Settings) but must not survive on the menu affordance.
     */
    @Test
    fun doesNotAnnounceFoxWorksBranding() {
        rule.setContent {
            TzafonTheme {
                HeaderMenuButton(onClick = {})
            }
        }

        // Neither the historical roundel description nor a brand name should
        // reach TalkBack from the menu affordance.
        val hits = listOf("The Fox Works", "Fox Works", "Foxworks", "Logo")
            .map { description ->
                description to try {
                    rule.onNodeWithContentDescription(description).fetchSemanticsNode()
                    true
                } catch (t: AssertionError) {
                    false
                }
            }
        hits.forEach { (description, hit) ->
            assert(!hit) { "FR-NAV-3.2: '$description' must not surface on the menu button" }
        }
    }
}
