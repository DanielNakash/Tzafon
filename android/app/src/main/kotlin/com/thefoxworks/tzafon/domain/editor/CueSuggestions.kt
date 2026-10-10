package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.Task

/**
 * v2.13.0 — the suggestion chips under the Alignment pane's inline cue.
 * Tapping one fills the field: a label, or under At a time a time.
 */
object CueSuggestions {

    /** The chip row's length; the row scrolls sideways within it. */
    const val LIMIT = 6

    /** Below this many of your own, the starter list tops the row up. */
    const val TOP_UP_BELOW = 3

    /** Starters per type, the first three from the deck's Alignment pane. */
    val STARTERS: Map<CueType, List<String>> = mapOf(
        CueType.AFTER_ROUTINE to listOf("After coffee", "After the school run", "After dinner"),
        CueType.AT_TIME to listOf("07:00", "12:30", "18:00"),
        CueType.AT_PLACE to listOf("At home", "At my desk", "At the gym"),
    )

    /**
     * Every cue in use, once per thing that carries it: each habit, each
     * one-off task and each series (its occurrences share one cue, so a
     * daily task doesn't outweigh everything else).
     */
    fun uses(tasks: List<Task>, habits: List<Habit>): List<Cue> =
        tasks.distinctBy { it.seriesId ?: it.id }.mapNotNull { it.cue } + habits.mapNotNull { it.cue }

    /**
     * The chips for [type] from the cues in [used]: your own labels of that
     * type (under At a time, your own `HH:mm` times), most-used first, ties in
     * the order first met, matched trimmed and ignoring case with the first
     * spelling kept. Fewer than [TOP_UP_BELOW] of your own are followed by the
     * starters you don't already have.
     */
    fun chips(type: CueType, used: List<Cue>): List<String> {
        val own = used.filter { it.type == type }
            .mapNotNull { cue ->
                if (type == CueType.AT_TIME) cue.time?.trim()?.takeIf(CueTime::isValid)
                else cue.label.trim().takeIf { it.isNotEmpty() }
            }
            .groupBy { it.lowercase() }
            .values
            .sortedByDescending { it.size }   // stable: ties keep first-met order
            .map { it.first() }
        val topUp = if (own.size < TOP_UP_BELOW) {
            val have = own.map { it.lowercase() }.toSet()
            STARTERS.getValue(type).filter { it.lowercase() !in have }
        } else emptyList()
        return (own + topUp).take(LIMIT)
    }
}
