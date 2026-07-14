package com.thefoxworks.tzafon

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.ui.about.ABOUT_CONTACT_MAILTO
import com.thefoxworks.tzafon.ui.about.AboutScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-ABOUT-1 — About screen. Exercises the acceptance criteria in v2.3.0 §5.3
 * that live at the composable level: three body-text rows, version from
 * BuildConfig, FoxLogo presence, mailto intent target, graceful no-handler.
 */
@RunWith(AndroidJUnit4::class)
class AboutScreenTest {

    @get:Rule
    val rule = createComposeRule()

    /** FR-ABOUT-1.2 — three body-text rows in order. */
    @Test
    fun rendersThreeBodyRows() {
        rule.setContent {
            TzafonTheme {
                AboutScreen(onClose = {}, versionName = "2.3.0", launchEmail = {})
            }
        }

        rule.onNodeWithText("Produced by The Fox Works").assertIsDisplayed()
        rule.onNodeWithText("Implemented by Claude").assertIsDisplayed()
        rule.onNodeWithText("Version 2.3.0").assertIsDisplayed()
    }

    /** FR-ABOUT-1.4 — version row reads BuildConfig.VERSION_NAME at render time. */
    @Test
    fun versionRowReadsBuildConfig() {
        rule.setContent {
            TzafonTheme {
                AboutScreen(onClose = {}, launchEmail = {})
            }
        }

        rule.onNodeWithText("Version ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
    }

    /** FR-ABOUT-1.7 — FoxLogo is present as the identity mark. */
    @Test
    fun rendersFoxLogo() {
        rule.setContent {
            TzafonTheme {
                AboutScreen(onClose = {}, versionName = "2.3.0", launchEmail = {})
            }
        }

        rule.onNodeWithContentDescription("The Fox Works").assertIsDisplayed()
    }

    /**
     * FR-ABOUT-1.1 — a dismiss control lives in the header. Since v2.4.0 M1
     * (nav-affordance consistency) this is the "Close" (X) affordance rendered
     * in the header's `right` slot. It's an icon-only pressable, so its "Close"
     * label rides the OnClick action (TalkBack action label) rather than a
     * `contentDescription`; select it by that label.
     */
    @Test
    fun rendersCloseControl() {
        var closed = 0
        rule.setContent {
            TzafonTheme {
                AboutScreen(onClose = { closed++ }, versionName = "2.3.0", launchEmail = {})
            }
        }

        rule.onNode(hasOnClickLabel("Close")).assertIsDisplayed()
        rule.onNode(hasOnClickLabel("Close")).performClick()
        assertEquals("close tap dismisses via onClose", 1, closed)
    }

    /** Matches an icon-only pressable whose "Close"-style label rides the
     *  OnClick action (see [Modifier.pressable]'s `onClickLabel`). */
    private fun hasOnClickLabel(label: String) =
        SemanticsMatcher("onClickLabel == '$label'") { node ->
            node.config.getOrNull(SemanticsActions.OnClick)?.label == label
        }

    /** FR-ABOUT-1.5 — Contact Us dispatches ACTION_SENDTO with the exact mailto URI. */
    @Test
    fun contactUs_launchesMailtoIntent() {
        var captured: Intent? = null
        rule.setContent {
            TzafonTheme {
                AboutScreen(
                    onClose = {},
                    versionName = "2.3.0",
                    launchEmail = { intent -> captured = intent },
                )
            }
        }

        rule.onNodeWithText("Contact Us").performClick()

        assertNotNull("Contact Us tap launches an intent", captured)
        assertEquals("ACTION_SENDTO is used, not ACTION_VIEW/SEND", Intent.ACTION_SENDTO, captured!!.action)
        assertEquals("mailto URI is the verbatim recipient", ABOUT_CONTACT_MAILTO, captured!!.data.toString())
        // FR-ABOUT-1.5 — no prefilled subject or body in v2.3.0.
        assertFalse("no subject extra in v2.3.0", captured!!.hasExtra(Intent.EXTRA_SUBJECT))
        assertFalse("no body extra in v2.3.0", captured!!.hasExtra(Intent.EXTRA_TEXT))
    }

    /** FR-ABOUT-1.5 — a graceful inline hint replaces a crash when no email app is available. */
    @Test
    fun contactUs_noEmailHandler_showsInlineHint() {
        rule.setContent {
            TzafonTheme {
                AboutScreen(
                    onClose = {},
                    versionName = "2.3.0",
                    launchEmail = { throw ActivityNotFoundException("no email app") },
                )
            }
        }

        // Before the tap, the hint is absent.
        var pre = try {
            rule.onNodeWithText("No email app is available").fetchSemanticsNode()
            true
        } catch (t: AssertionError) {
            false
        }
        assertFalse("hint is absent before tap", pre)

        rule.onNodeWithText("Contact Us").performClick()
        rule.onNodeWithText("No email app is available").assertIsDisplayed()
    }

    /** DM-NOT audit — About surfaces no gamification copy. */
    @Test
    fun hasNoGamificationSurface() {
        rule.setContent {
            TzafonTheme {
                AboutScreen(onClose = {}, versionName = "2.3.0", launchEmail = {})
            }
        }

        listOf("STREAK", "Streak", "streak", "POINTS", "Points", "BADGE", "Badge", "ADHERENCE", "adherence")
            .forEach { forbidden ->
                val hit = try {
                    rule.onNodeWithText(forbidden, substring = true).fetchSemanticsNode()
                    true
                } catch (t: AssertionError) {
                    false
                }
                assertFalse("DM-NOT: '$forbidden' must not surface on About", hit)
            }
        assertTrue(true) // reached without a hit
    }
}
