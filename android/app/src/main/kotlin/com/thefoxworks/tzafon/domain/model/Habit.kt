package com.thefoxworks.tzafon.domain.model

import kotlinx.coroutines.flow.Flow

/** DM-HABIT-1 — success is hitting a rate; quantitative habits log amounts. */
enum class HabitKind { FREQUENCY, QUANTITATIVE }

data class Habit(
    val id: String,
    val name: String,
    val kind: HabitKind = HabitKind.FREQUENCY,
    /** FREQUENCY: times per week · QUANTITATIVE: amount per day */
    val target: Double = 3.0,
    val unit: String? = null,          // quantitative only ("words", "km")
    /** QUANTITATIVE: aimed days per week (the design's "4 of 5 days") */
    val targetDays: Int? = null,
    val cue: Cue? = null,              // DM-CUE — a habit without one is just a tracker
    val primaryThemeId: String? = null,
    /** the full serve set (DM-HABIT-6) — primary + shared */
    val themeIds: List<String> = emptyList(),
    val goalId: String? = null,        // DM-HABIT-6 / D2 — may serve one Goal
    val startedAt: Long = 0,           // long arc anchor ("Running 9 weeks")
    val createdAt: Long = 0,
)

/**
 * DM-HABIT-1/5 — one row per habit-date. TASK-sourced rows carry the task id
 * so un-doing reverses exactly that contribution (DM-ATTR-2 ledger).
 */
enum class LogSource { TASK, DIRECT }

data class HabitLog(
    val habitId: String,
    val date: String,                  // ISO yyyy-MM-dd
    val done: Boolean = true,
    val amount: Double? = null,        // quantitative
    val source: LogSource = LogSource.DIRECT,
    val sourceTaskId: String? = null,
)

/** The persistence contract for habits (same seam pattern as tasks). */
interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeLogs(): Flow<List<HabitLog>>

    suspend fun getHabit(id: String): Habit?
    suspend fun upsert(habit: Habit)

    /** DM-HABIT-7 — destructive: wipes the habit AND its whole history. */
    suspend fun delete(id: String)

    /** Direct log/edit on a date (DM-HABIT-5 second path). */
    suspend fun logDirect(habitId: String, date: String, done: Boolean, amount: Double?)

    /** Task-driven ledger writes (DM-ATTR-2): apply on Done, reverse exactly. */
    suspend fun logForTask(habitId: String, date: String, taskId: String, amount: Double?)
    suspend fun reverseForTask(taskId: String)
}
