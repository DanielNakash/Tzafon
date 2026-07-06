package com.thefoxworks.tzafon.domain.themes

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeState

/**
 * DM-THEME / FR-DIR-6 — the one hard cap in the app (≤3 active, enforced at
 * activation) and the window-arrival rules, pure and tested.
 */
object ThemeLogic {

    const val ACTIVE_CAP = 3 // DM-THEME-3 — the only hard cap anywhere

    /** Default window: one quarter (13 weeks), user-editable (DEC-5). */
    fun defaultWindowEnd(start: String): String = Dates.addDays(start, 91)

    fun activeCount(themes: List<Theme>): Int =
        themes.count { it.state == ThemeState.ACTIVE }

    /** DM-THEME-3 — may this theme become active right now? */
    fun canActivate(themes: List<Theme>, themeId: String): Boolean =
        themes.count { it.state == ThemeState.ACTIVE && it.id != themeId } < ACTIVE_CAP

    /**
     * FR-DIR-2 — window position as orientation, never pressure:
     * "QUARTER · WEEK 7" (no %, no countdown).
     */
    fun windowLabel(theme: Theme, today: String): String {
        val totalDays = Dates.dayDiff(theme.windowEnd, theme.windowStart).coerceAtLeast(1)
        val span = when {
            totalDays in 84..98 -> "QUARTER"
            totalDays % 7 < 4 -> "${(totalDays + 3) / 7} WEEKS"
            else -> "${totalDays / 7} WEEKS"
        }
        val week = (Dates.dayDiff(today, theme.windowStart) / 7 + 1).coerceAtLeast(1)
        return "$span · WEEK $week"
    }

    /** A window that has run its course — a renew moment, never a failure. */
    fun windowEnded(theme: Theme, today: String): Boolean =
        theme.state == ThemeState.ACTIVE && theme.windowEnd < today

    /** What to do when an upcoming theme's start date arrives (FR-DIR-6). */
    sealed interface Arrival {
        /** a slot is free — activate quietly */
        data object Activate : Arrival

        /** 3 active but one's window has ended — archive it, activate this */
        data class Swap(val endedThemeId: String) : Arrival

        /** 3 active, none ended — ask the user to retire or park one */
        data object Prompt : Arrival
    }

    fun onWindowArrival(themes: List<Theme>, arriving: Theme, today: String): Arrival {
        val active = themes.filter { it.state == ThemeState.ACTIVE }
        if (active.size < ACTIVE_CAP) return Arrival.Activate
        val ended = active.firstOrNull { it.windowEnd < today }
        return if (ended != null) Arrival.Swap(ended.id) else Arrival.Prompt
    }

    /** Upcoming themes whose start date has arrived. */
    fun arrivedUpcoming(themes: List<Theme>, today: String): List<Theme> =
        themes.filter { it.state == ThemeState.UPCOMING && it.windowStart <= today }

    /** The next free accent slot, cycling the three Den theme accents. */
    fun nextAccentSlot(themes: List<Theme>): Int {
        val used = themes.filter { it.state != ThemeState.ARCHIVED }.map { it.accentSlot % 3 }
        return (0..2).firstOrNull { it !in used } ?: (themes.size % 3)
    }
}
