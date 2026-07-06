package com.thefoxworks.tzafon.domain.model

import kotlinx.coroutines.flow.Flow

/** DM-GOAL-1 — the three goal shapes. */
enum class GoalType { STEPPED, ACCUMULATIVE, GENERIC }

enum class GoalState { ONGOING, COMPLETED, FROZEN }

data class GoalStep(val label: String, val done: Boolean = false)

data class Goal(
    val id: String,
    val title: String,
    val description: String = "",
    val type: GoalType = GoalType.GENERIC,
    /** Stepped — DM-GOAL-2 endows the first step "Defined this goal ✓" done */
    val steps: List<GoalStep> = emptyList(),
    /** Accumulative */
    val targetQty: Double = 0.0,
    val unit: String? = null,
    val currentQty: Double = 0.0,      // honest non-zero start allowed
    val state: GoalState = GoalState.ONGOING,
    val deadline: String? = null,
    val commitment: String? = null,    // DM-GOAL-1 commitment device
    /** shown as "serves: <theme>" — the display parent (DM-REL-1) */
    val primaryThemeId: String? = null,
    /** the full serve set (DM-GOAL-6) — primary + shared ("+N") */
    val themeIds: List<String> = emptyList(),
    val lastActivityAt: Long = 0,      // DM-GOAL-5 stale nudge
    val completedAt: Long? = null,
    val createdAt: Long = 0,
) {
    /** Endowed progress percent (PRIN-8) — never an empty bar. */
    fun pct(): Int = when (type) {
        GoalType.STEPPED ->
            if (steps.isEmpty()) 0 else (steps.count { it.done } * 100 / steps.size)
        GoalType.ACCUMULATIVE ->
            if (targetQty <= 0) 0 else ((currentQty / targetQty) * 100).toInt().coerceIn(0, 100)
        GoalType.GENERIC -> 40 // directional stand-in, per the design
    }
}

/**
 * DM-ATTR-1 / NFR-DATA-2 — the provenance ledger. Every automatic numeric
 * advance writes a row; reversal subtracts exactly these and deletes them.
 */
enum class ContributionVia { DIRECT, HABIT }

data class Contribution(
    val id: String,
    val taskId: String,
    val goalId: String,
    val amount: Double,
    val via: ContributionVia,
    val createdAt: Long = 0,
)

interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>
    fun observeContributions(): Flow<List<Contribution>>

    suspend fun getGoal(id: String): Goal?
    suspend fun upsert(goal: Goal)
    suspend fun delete(id: String)

    /** Progress the second way (DM-GOAL-3): direct edit on the goal. */
    suspend fun advanceDirect(goalId: String, newCurrent: Double)

    /** Apply a task's planned contributions (writes ledger rows + bumps). */
    suspend fun applyForTask(taskId: String, contributions: List<Contribution>)

    /** DM-ATTR-1(4) — exact reversal of everything this task contributed. */
    suspend fun reverseForTask(taskId: String)
}
