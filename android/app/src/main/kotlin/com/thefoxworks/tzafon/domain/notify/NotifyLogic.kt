package com.thefoxworks.tzafon.domain.notify

import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState

/**
 * FR-NOTIF-1 — reminders fire on the **cue**, never on mere existence.
 * Pure computation: given the day's state, which reminders should exist?
 * Scheduling (AlarmManager/WorkManager) lives in the data layer; PRIN-9
 * means nothing here ever nags — one reminder per cue per day, and a
 * habit already logged today stays silent.
 */
object NotifyLogic {

    /** One local notification to schedule. `key` is stable per cue+date. */
    data class Reminder(
        val key: String,
        val title: String,
        /** the cue made visible — "After the first coffee · 8:30" */
        val line: String,
        val date: String,   // ISO yyyy-MM-dd
        val time: String,   // HH:mm
    )

    /** HH:mm → minutes since midnight, or null when absent/malformed. */
    fun minutesOf(time: String?): Int? {
        if (time == null) return null
        val m = Regex("^(\\d{1,2}):(\\d{2})$").find(time.trim()) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (h > 23 || min > 59) return null
        return h * 60 + min
    }

    private fun cueLine(cue: Cue): String {
        val label = cue.label.trim()
        val time = cue.time?.trim()
        return when {
            label.isEmpty() -> time ?: ""
            time == null || label == time -> label
            else -> "$label · $time"
        }
    }

    /**
     * The reminders for `date`. AT_TIME cues always carry a time; an
     * AFTER_ROUTINE cue joins only when its optional time is set
     * (FR-NOTIF-1); AT_PLACE has no clock and stays silent (FR-NOTIF-3).
     *
     * - a task reminds on the day it's planned or due (never after — a
     *   slipped task is Planning's gentle business, not the lockscreen's)
     * - a habit reminds daily at its cue time, unless today's log exists
     * - `afterMinutes` drops times already past when scheduling intraday
     */
    fun remindersFor(
        date: String,
        tasks: List<Task>,
        habits: List<Habit>,
        logs: List<HabitLog>,
        afterMinutes: Int = -1,
    ): List<Reminder> {
        val out = ArrayList<Reminder>()

        for (t in tasks) {
            if (t.state != TaskState.OPEN) continue
            if (t.toDoDate != date && t.dueDate != date) continue
            val cue = t.cue ?: continue
            val mins = minutesOf(cue.time) ?: continue
            if (mins <= afterMinutes) continue
            out += Reminder(
                key = "task-${t.id}-$date",
                title = t.title,
                line = cueLine(cue),
                date = date,
                time = cue.time!!.trim(),
            )
        }

        val loggedToday = logs.filter { it.date == date && it.done }.map { it.habitId }.toSet()
        for (h in habits) {
            val cue = h.cue ?: continue
            val mins = minutesOf(cue.time) ?: continue
            if (mins <= afterMinutes) continue
            if (h.id in loggedToday) continue
            out += Reminder(
                key = "habit-${h.id}-$date",
                title = h.name,
                line = cueLine(cue),
                date = date,
                time = cue.time!!.trim(),
            )
        }

        return out.sortedBy { minutesOf(it.time) }
    }
}
