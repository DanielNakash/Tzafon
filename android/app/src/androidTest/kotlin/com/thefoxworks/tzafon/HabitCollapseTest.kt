package com.thefoxworks.tzafon

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.ui.habits.HabitCard
import com.thefoxworks.tzafon.ui.habits.HabitCardState
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-HAB-5 — Habits view collapse-by-default behaviour. Covers the four
 * acceptance criteria that can be exercised at the composable level:
 *   1. renders collapsed (name + log + expand affordance visible; expanded
 *      details hidden);
 *   2. log-from-collapsed fires onLog;
 *   3. tapping the header expands and reveals full FR-HAB-1 contents; tapping
 *      again collapses;
 *   4. remounting the composable resets to collapsed (FR-HAB-5.2 —
 *      expand state is not persisted across navigation).
 */
@RunWith(AndroidJUnit4::class)
class HabitCollapseTest {

    @get:Rule
    val rule = createComposeRule()

    private val habit = Habit(
        id = "h1",
        name = "Read",
        kind = HabitKind.FREQUENCY,
        target = 3.0,
        cue = Cue(CueType.AFTER_ROUTINE, "After the first coffee"),
    )

    private val card = HabitCardState(
        habit = habit,
        week = HabitMath.WeekSnapshot(
            doneDays = 1,
            amountSum = 0.0,
            amounts = List(7) { 0.0 },
            doneFlags = List(7) { false },
        ),
        arc = "First week",
        freshStart = false,
        grid = List(5) { List(7) { 0 } },
        loggedToday = false,
        todayAmount = null,
        hasHistory = false,
        logs = emptyList(),
    )

    @Test
    fun rendersCollapsed_showsOnlyName_logControl_expandAffordance() {
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card, today = com.thefoxworks.tzafon.domain.dates.Dates.todayIso(), servesGoal = null, onLog = {}, onLogDate = {}, onEdit = {}, onTapStripDay = {})
            }
        }

        rule.onNodeWithText("Read").assertIsDisplayed()
        // FR-HAB-9 — the collapsed log pill now shows the "Mark"/"Un-mark" token.
        rule.onNodeWithText("Mark").assertIsDisplayed()
        rule.onNodeWithContentDescription("Expand habit Read").assertIsDisplayed()
        // FR-HAB-5.3 / FR-HAB-1 details must NOT surface until expanded.
        rule.onNodeWithText("THIS WEEK").assertDoesNotExist()
        rule.onNodeWithText("LAST 5 WEEKS").assertDoesNotExist()
        rule.onNodeWithText("3× / week").assertDoesNotExist()
    }

    @Test
    fun logFromCollapsed_firesOnLog_withoutExpanding() {
        var logs = 0
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card, today = com.thefoxworks.tzafon.domain.dates.Dates.todayIso(), servesGoal = null, onLog = { logs++ }, onLogDate = {}, onEdit = {}, onTapStripDay = {})
            }
        }

        rule.onNodeWithText("Mark").performClick()
        assertEquals("collapsed log tap fires exactly once", 1, logs)
        // Card must stay collapsed after a log tap.
        rule.onNodeWithText("THIS WEEK").assertDoesNotExist()
    }

    @Test
    fun expandThenCollapse_revealsFullContentsAndHidesAgain() {
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card, today = com.thefoxworks.tzafon.domain.dates.Dates.todayIso(), servesGoal = null, onLog = {}, onLogDate = {}, onEdit = {}, onTapStripDay = {})
            }
        }

        rule.onNodeWithContentDescription("Expand habit Read").performClick()
        // FR-HAB-1 contents come back, with cue immediately below the header.
        rule.onNodeWithText("THIS WEEK").assertIsDisplayed()
        rule.onNodeWithText("3× / week").assertIsDisplayed()
        rule.onNodeWithText("LAST 5 WEEKS").assertIsDisplayed()

        rule.onNodeWithContentDescription("Collapse habit Read").performClick()
        rule.onNodeWithText("THIS WEEK").assertDoesNotExist()
        rule.onNodeWithText("LAST 5 WEEKS").assertDoesNotExist()
    }

    /**
     * FR-HAB-5.2 — expand state is NOT persisted. Re-entering the Habits view
     * must present every card collapsed. We simulate re-entry by swapping the
     * `mount` key on a remember { mutableStateOf } composition so the card is
     * torn down and recreated with fresh internal state.
     */
    @Test
    fun rebuildingComposable_resetsToCollapsed() {
        var mountKey by mutableStateOf(0)
        rule.setContent {
            TzafonTheme {
                Column {
                    // The key drives a fresh composition — analogous to leaving
                    // and returning to the Habits tab.
                    androidx.compose.runtime.key(mountKey) {
                        HabitCard(card = card, today = com.thefoxworks.tzafon.domain.dates.Dates.todayIso(), servesGoal = null, onLog = {}, onLogDate = {}, onEdit = {}, onTapStripDay = {})
                    }
                }
            }
        }

        // Expand, confirm.
        rule.onNodeWithContentDescription("Expand habit Read").performClick()
        rule.onNodeWithText("THIS WEEK").assertIsDisplayed()

        // Force remount (equivalent to nav-tab-away-and-back).
        rule.runOnIdle { mountKey = 1 }
        rule.waitForIdle()

        // Post-remount: collapsed again.
        rule.onNodeWithText("THIS WEEK").assertDoesNotExist()
        rule.onNodeWithContentDescription("Expand habit Read").assertIsDisplayed()
    }

    @Test
    fun noStreakOrAdherenceSurface_inEitherState() {
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card, today = com.thefoxworks.tzafon.domain.dates.Dates.todayIso(), servesGoal = null, onLog = {}, onLogDate = {}, onEdit = {}, onTapStripDay = {})
            }
        }

        // Collapsed: DM-NOT audit.
        assertNoGamification()

        // Expand, re-audit.
        rule.onNodeWithContentDescription("Expand habit Read").performClick()
        assertNoGamification()
    }

    private fun assertNoGamification() {
        // A short blacklist of the exact strings DM-NOT bans. Substring match
        // via onAllNodesWithText would over-fire on the "Running N weeks" arc
        // (a duration, not a streak); the safe check is that no node exposes
        // any of these literals.
        listOf("STREAK", "Streak", "streak", "%", "POINTS", "Points", "BADGE", "Badge", "ADHERENCE")
            .forEach { forbidden ->
                val hit = try {
                    rule.onNodeWithText(forbidden, substring = true).fetchSemanticsNode()
                    true
                } catch (t: AssertionError) {
                    false
                }
                assertFalse("DM-NOT: '$forbidden' must not surface anywhere on a habit card", hit)
            }
        assertTrue(true)
    }
}
