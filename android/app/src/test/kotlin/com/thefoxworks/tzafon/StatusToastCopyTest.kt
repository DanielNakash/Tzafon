package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.GoalMove
import com.thefoxworks.tzafon.domain.status.SeriesChange
import com.thefoxworks.tzafon.domain.status.StatusToast
import com.thefoxworks.tzafon.domain.status.StatusToastCopy
import com.thefoxworks.tzafon.domain.status.TapAction
import com.thefoxworks.tzafon.domain.status.TapRule
import org.junit.Assert.assertEquals
import org.junit.Test

/** v2.13.0 — the one tap rule and the status toast's copy. */
class StatusToastCopyTest {

    private val title = "Write 500 words"

    @Test
    fun tap_togglesOpenAndDone_andSettledStatusesOpenTheSheet() {
        assertEquals(TapAction.SetStatus(TaskState.DONE), TapRule.forTap(TaskState.OPEN, needsAmount = false))
        assertEquals(TapAction.AskAmount, TapRule.forTap(TaskState.OPEN, needsAmount = true))
        assertEquals(TapAction.SetStatus(TaskState.OPEN), TapRule.forTap(TaskState.DONE, needsAmount = true))
        for (s in listOf(TaskState.CLOSED, TaskState.FROZEN, TaskState.BACKLOG)) {
            assertEquals(TapAction.OpenSheet, TapRule.forTap(s, needsAmount = false))
        }
    }

    @Test
    fun singleTaskChanges_nameTheTitle_andOfferUndo() {
        val cases = mapOf(
            TaskState.DONE to "Done · $title",
            TaskState.OPEN to "Marked open · $title",
            TaskState.CLOSED to "Closed · $title",
            TaskState.FROZEN to "Frozen · $title",
            TaskState.BACKLOG to "Moved to Backlog · $title",
        )
        cases.forEach { (target, text) ->
            assertEquals(StatusToast(text, undoable = true), StatusToastCopy.of(title, target))
        }
    }

    @Test
    fun done_saysWhatItTickedAndMoved_inPlaceOfTheTitle() {
        assertEquals(
            "Done · ticked Write in the morning · moved First draft to 62.5K words",
            StatusToastCopy.of(
                title, TaskState.DONE,
                habit = "Write in the morning",
                goals = listOf(GoalMove("First draft", 62_500.0, "words")),
            ).text,
        )
        assertEquals("Done · ticked Stretch", StatusToastCopy.of(title, TaskState.DONE, habit = "Stretch").text)
        assertEquals(
            "Done · moved Savings to 1.2K",
            StatusToastCopy.of(title, TaskState.DONE, goals = listOf(GoalMove("Savings", 1_200.0, null))).text,
        )
    }

    @Test
    fun wholeSeriesChanges_nameTheSeries_withoutUndo() {
        val cases = mapOf(
            SeriesChange.FROZEN to "Series frozen · Run",
            SeriesChange.CLOSED to "Series closed · Run",
            SeriesChange.THAWED to "Series thawed · Run",
            SeriesChange.REOPENED to "Series reopened · Run",
        )
        cases.forEach { (change, text) ->
            assertEquals(StatusToast(text, undoable = false), StatusToastCopy.of("Run", TaskState.OPEN, series = change))
        }
    }

    @Test
    fun compact_keepsOneDecimalAtMost() {
        assertEquals("500", StatusToastCopy.compact(500.0))
        assertEquals("12.5", StatusToastCopy.compact(12.5))
        assertEquals("62.5K", StatusToastCopy.compact(62_500.0))
        assertEquals("80K", StatusToastCopy.compact(80_000.0))
        assertEquals("1.2M", StatusToastCopy.compact(1_200_000.0))
    }
}
