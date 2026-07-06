package com.thefoxworks.tzafon.domain.review

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.ReviewKind
import com.thefoxworks.tzafon.domain.model.ReviewStatus

/**
 * DM-REVIEW-1/4 — the plan-and-review loop's clockwork, pure and tested:
 * weekly fires on the chosen week start; the monthly *rides* the first
 * weekly slot on/after the 1st (never two reviews in a week); the invite
 * stays open through the next day, and partial progress resumes all week.
 */
object ReviewLogic {

    /** The review slot (period start) containing [today]. */
    fun slotFor(today: String, weekStart: String): String =
        HabitMath.weekStartFor(today, weekStart)

    fun periodEnd(slot: String): String = Dates.addDays(slot, 6)

    /**
     * DM-REVIEW-1 — a slot is the monthly upgrade when it is the first
     * week start on/after the 1st of its month (day 1..7 by construction).
     */
    fun kindFor(slot: String): ReviewKind =
        if (Dates.parse(slot).dayOfMonth <= 7) ReviewKind.MONTHLY else ReviewKind.WEEKLY

    /**
     * DM-REVIEW-4 — should the invitation surface today?
     *  - untouched (none/PENDING): on the week-start day and the next day
     *  - PARTIAL: any day of the period (resumable, progress preserved)
     *  - DONE / SKIPPED: never again for this slot
     */
    fun shouldSurface(existing: Review?, today: String, weekStart: String): Boolean {
        val slot = slotFor(today, weekStart)
        if (existing != null && existing.id != slot) return true // stale row from another period
        return when (existing?.status) {
            null, ReviewStatus.PENDING -> Dates.dayDiff(today, slot) <= 1
            ReviewStatus.PARTIAL -> true
            ReviewStatus.DONE, ReviewStatus.SKIPPED -> false
        }
    }

    /** The invite copy: weekly is light, monthly is the deeper look. */
    fun inviteKicker(kind: ReviewKind, slot: String): String {
        val day = Dates.WD_FULL[Dates.dayOfWeek(slot)].uppercase()
        return if (kind == ReviewKind.MONTHLY) "$day · THE MONTH, GENTLY" else "$day · A GENTLE LOOK BACK"
    }

    // ── the mirror numbers (DM-REVIEW-2 Reflect) ──────────────

    data class HabitRate(val name: String, val done: Int, val target: Int, val pct: Int, val quantLine: String?)
    data class GoalDelta(val title: String, val delta: String)

    data class Snapshot(
        val doneCount: Int,
        val alignedCount: Int,
        val habitRates: List<HabitRate>,
        val goalDeltas: List<GoalDelta>,
    ) {
        /** compact persisted form (DM-REVIEW-5) */
        fun encode(): String = buildString {
            append("$doneCount|$alignedCount")
            habitRates.forEach { append("\n H ${it.name} · ${it.done} of ${it.target}") }
            goalDeltas.forEach { append("\n G ${it.title} · ${it.delta}") }
        }
    }
}
