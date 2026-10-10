package com.thefoxworks.tzafon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.ui.components.CalendarPicker
import com.thefoxworks.tzafon.ui.components.LocalWeekStart
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * v2.13.0 — every calendar picker follows the week-start setting (FR-SET-1.1).
 * October 2026 opens the picker; its 1st is a Thursday, so it sits in column
 * 3 Monday-first, 4 Sunday-first and 5 Saturday-first.
 */
@RunWith(AndroidJUnit4::class)
class CalendarWeekStartTest {

    @get:Rule
    val rule = createComposeRule()

    private val headers = listOf("S", "M", "T", "W", "F")

    private fun show(weekStart: String?) = rule.setContent {
        TzafonTheme {
            if (weekStart == null) Picker()
            else CompositionLocalProvider(LocalWeekStart provides weekStart) { Picker() }
        }
    }

    @Composable
    private fun Picker() = CalendarPicker(value = "2026-10-15", today = "2026-10-10", onPick = {})

    private fun bounds(text: String): List<Rect> =
        rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }

    /** Header letters by column, read off the screen. */
    private fun headerRow(): List<String> {
        val cells = headers.flatMap { h -> bounds(h).map { h to it } }.sortedBy { it.second.left }
        assertEquals(7, cells.size)
        return cells.map { it.first }
    }

    private fun column(text: String): Int {
        val header = headers.flatMap { bounds(it) }
        val left = header.minOf { it.left }
        val width = header.first().width
        val day = rule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        return ((day.center.x - left) / width - 0.5f).roundToInt()
    }

    @Test
    fun mondayStart_headerStartsWithM_andThe1stFallsOnThursday() {
        show("MONDAY")
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), headerRow())
        assertEquals(3, column("1"))
        assertEquals(0, column("5"))
    }

    @Test
    fun saturdayStart_headerStartsWithS_M_inThirdColumn() {
        show("SATURDAY")
        assertEquals(listOf("S", "S", "M", "T", "W", "T", "F"), headerRow())
        assertEquals(5, column("1"))
        assertEquals(0, column("3"))
    }

    @Test
    fun noSetting_staysSundayFirst() {
        show(null)
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), headerRow())
        assertEquals(4, column("1"))
    }
}
