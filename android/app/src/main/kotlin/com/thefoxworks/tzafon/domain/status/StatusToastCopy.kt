package com.thefoxworks.tzafon.domain.status

import com.thefoxworks.tzafon.domain.model.TaskState

/** An amount-measuring goal a Done just moved, and where it stands now. */
data class GoalMove(val title: String, val total: Double, val unit: String?)

/** The toast's one line, and whether it offers Undo. */
data class StatusToast(val text: String, val undoable: Boolean)

/**
 * The status toast's copy (v2.13.0): one line, segments joined by " · ",
 * no "+1" (DEC-2 / DM-NOT). A Done that ticked a habit or moved a goal says
 * so in place of the title, using *ticked* and *moved*. Whole-series changes
 * name the series and offer no Undo.
 */
object StatusToastCopy {

    fun of(
        title: String,
        target: TaskState,
        series: SeriesChange? = null,
        habit: String? = null,
        goals: List<GoalMove> = emptyList(),
    ): StatusToast {
        if (series != null) {
            val head = when (series) {
                SeriesChange.FROZEN -> "Series frozen"
                SeriesChange.CLOSED -> "Series closed"
                SeriesChange.THAWED -> "Series thawed"
                SeriesChange.REOPENED -> "Series reopened"
            }
            return StatusToast("$head · $title", undoable = false)
        }
        val text = when (target) {
            TaskState.DONE -> {
                val links = listOfNotNull(habit?.let { "ticked $it" }) +
                    goals.map { "moved ${it.title} to ${compact(it.total)}${it.unit?.let { u -> " $u" } ?: ""}" }
                "Done · " + (links.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: title)
            }
            TaskState.OPEN -> "Marked open · $title"
            TaskState.CLOSED -> "Closed · $title"
            TaskState.FROZEN -> "Frozen · $title"
            TaskState.BACKLOG -> "Moved to Backlog · $title"
        }
        return StatusToast(text, undoable = true)
    }

    /** 500 → "500", 62 500 → "62.5K", 1 200 000 → "1.2M": one decimal at most. */
    fun compact(v: Double): String {
        val (scaled, suffix) = when {
            kotlin.math.abs(v) >= 1_000_000 -> v / 1_000_000 to "M"
            kotlin.math.abs(v) >= 1_000 -> v / 1_000 to "K"
            else -> v to ""
        }
        val tenths = kotlin.math.round(scaled * 10).toLong()
        val whole = tenths / 10
        val frac = kotlin.math.abs(tenths % 10)
        return (if (frac == 0L) "$whole" else "$whole.$frac") + suffix
    }
}
