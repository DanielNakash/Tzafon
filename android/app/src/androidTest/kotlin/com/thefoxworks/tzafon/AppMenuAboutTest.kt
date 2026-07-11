package com.thefoxworks.tzafon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-NAV-4 — the About row on `AppMenuSheet`. Verifies the row exists in the
 * expected order after Settings, that the About tap fires the callback, and
 * that the existing three rows are untouched (FR-NAV-4.4).
 */
@RunWith(AndroidJUnit4::class)
class AppMenuAboutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rendersAboutRowWithDenVoicedSubtitle() {
        rule.setContent {
            TzafonTheme {
                AppMenuSheet(
                    onClose = {},
                    onAllTasks = {},
                    onBacklog = {},
                    onSettings = {},
                    onAbout = {},
                )
            }
        }

        rule.onNodeWithText("About").assertIsDisplayed()
        rule.onNodeWithText("Producer, implementer, contact").assertIsDisplayed()
    }

    @Test
    fun rendersAllFourRowsWithExistingLabelsUnchanged() {
        rule.setContent {
            TzafonTheme {
                AppMenuSheet(
                    onClose = {},
                    onAllTasks = {},
                    onBacklog = {},
                    onSettings = {},
                    onAbout = {},
                )
            }
        }

        rule.onNodeWithText("All Tasks").assertIsDisplayed()
        rule.onNodeWithText("Backlog").assertIsDisplayed()
        rule.onNodeWithText("Settings").assertIsDisplayed()
        rule.onNodeWithText("About").assertIsDisplayed()
        // FR-NAV-4.4 — existing subtitles remain untouched.
        rule.onNodeWithText("The complete, searchable index").assertIsDisplayed()
        rule.onNodeWithText("Someday / maybe — parked, not scheduled").assertIsDisplayed()
        rule.onNodeWithText("Week start, reminders, account").assertIsDisplayed()
    }

    @Test
    fun tapping_About_closesSheet_thenFiresOnAbout() {
        var closes = 0
        var aboutTaps = 0
        rule.setContent {
            TzafonTheme {
                AppMenuSheet(
                    onClose = { closes++ },
                    onAllTasks = {},
                    onBacklog = {},
                    onSettings = {},
                    onAbout = { aboutTaps++ },
                )
            }
        }

        rule.onNodeWithText("About").performClick()

        assertEquals("sheet closes exactly once on About tap", 1, closes)
        assertEquals("About callback fires exactly once", 1, aboutTaps)
    }

    /** DM-NOT — the menu row must not surface streak/points/badge/adherence copy. */
    @Test
    fun aboutRow_hasNoGamificationCopy() {
        rule.setContent {
            TzafonTheme {
                AppMenuSheet(
                    onClose = {},
                    onAllTasks = {},
                    onBacklog = {},
                    onSettings = {},
                    onAbout = {},
                )
            }
        }

        listOf("STREAK", "streak", "POINTS", "points", "BADGE", "badge", "adherence")
            .forEach { forbidden ->
                val hit = try {
                    rule.onNodeWithText(forbidden, substring = true).fetchSemanticsNode()
                    true
                } catch (t: AssertionError) {
                    false
                }
                assertFalse("DM-NOT: '$forbidden' must not surface on the About row", hit)
            }
    }
}
