package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.CueSuggestions
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v2.13.0 — the chips under the inline cue: your own past cues of the
 * selected type, most-used first, topped up from a starter list.
 */
class CueSuggestionsTest {

    private fun routine(label: String) = Cue(CueType.AFTER_ROUTINE, label)
    private fun task(id: String, cue: Cue?, seriesId: String? = null) = Task(id = id, title = id, cue = cue, seriesId = seriesId)
    private fun habit(id: String, cue: Cue?) = Habit(id = id, name = id, cue = cue)

    @Test
    fun `your own labels come most-used first, counted across tasks and habits`() {
        val used = CueSuggestions.uses(
            tasks = listOf(
                task("a", routine("After dinner")),
                task("b", routine("After coffee")),
                task("c", routine("After the run")),
                task("d", routine("After coffee")),
            ),
            habits = listOf(habit("h", routine("After the run")), habit("i", routine("After coffee"))),
        )
        assertEquals(
            listOf("After coffee", "After the run", "After dinner"),
            CueSuggestions.chips(CueType.AFTER_ROUTINE, used),
        )
    }

    @Test
    fun `labels are trimmed and matched ignoring case, keeping the first spelling`() {
        val used = listOf(routine("After coffee "), routine("after Coffee"), routine("After dinner"), routine("After lunch"))
        assertEquals(
            listOf("After coffee", "After dinner", "After lunch"),
            CueSuggestions.chips(CueType.AFTER_ROUTINE, used),
        )
    }

    @Test
    fun `a repeating task counts once, not once per occurrence`() {
        val used = CueSuggestions.uses(
            tasks = listOf(
                task("s__1", routine("After the school run"), seriesId = "s"),
                task("s__2", routine("After the school run"), seriesId = "s"),
                task("s__3", routine("After the school run"), seriesId = "s"),
                task("x", routine("After dinner")),
                task("y", routine("After dinner")),
                task("z", routine("After lunch")),
            ),
            habits = emptyList(),
        )
        assertEquals(
            listOf("After dinner", "After the school run", "After lunch"),
            CueSuggestions.chips(CueType.AFTER_ROUTINE, used),
        )
    }

    @Test
    fun `only the selected type's cues are suggested`() {
        val used = listOf(
            routine("After coffee"), routine("After dinner"), routine("After lunch"),
            Cue(CueType.AT_PLACE, "At the studio"), Cue(CueType.AT_PLACE, "At home"), Cue(CueType.AT_PLACE, "At the gym"),
        )
        assertEquals(listOf("At the studio", "At home", "At the gym"), CueSuggestions.chips(CueType.AT_PLACE, used))
    }

    @Test
    fun `below three of your own the starter list tops them up`() {
        assertEquals(
            listOf("After coffee", "After the school run", "After dinner"),
            CueSuggestions.chips(CueType.AFTER_ROUTINE, emptyList()),
        )
        assertEquals(
            listOf("After the run", "After coffee", "After the school run", "After dinner"),
            CueSuggestions.chips(CueType.AFTER_ROUTINE, listOf(routine("After the run"))),
        )
    }

    @Test
    fun `a starter you already use isn't repeated, and your spelling wins`() {
        assertEquals(
            listOf("after dinner", "After coffee", "After the school run"),
            CueSuggestions.chips(CueType.AFTER_ROUTINE, listOf(routine("after dinner"))),
        )
    }

    @Test
    fun `every type has a starter list`() {
        CueType.entries.forEach { assertEquals(3, CueSuggestions.chips(it, emptyList()).size) }
    }

    @Test
    fun `under at a time the chips are your past times, not labels`() {
        val used = listOf(
            Cue(CueType.AT_TIME, "Lunch", "13:00"),
            Cue(CueType.AT_TIME, "", "08:30"),
            Cue(CueType.AT_TIME, "Standup", "08:30"),
            Cue(CueType.AT_TIME, "Old", "8:30"),          // not HH:mm — never offered
            Cue(CueType.AT_TIME, "", "21:15"),
            Cue(CueType.AFTER_ROUTINE, "After coffee", "07:00"),
        )
        assertEquals(listOf("08:30", "13:00", "21:15"), CueSuggestions.chips(CueType.AT_TIME, used))
    }

    @Test
    fun `the chip row stops at six`() {
        val used = (1..9).map { routine("After step $it") }
        assertEquals(6, CueSuggestions.chips(CueType.AFTER_ROUTINE, used).size)
    }
}
