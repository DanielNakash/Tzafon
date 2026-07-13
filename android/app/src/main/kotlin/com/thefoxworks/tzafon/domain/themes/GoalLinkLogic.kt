package com.thefoxworks.tzafon.domain.themes

/**
 * FR-DIR-8.6 — pure promotion / selection rules for the goal ↔ theme link
 * editor. Extracted so the deterministic-promotion behaviour can be tested
 * without spinning up a Compose harness.
 *
 * The model is: primary is a single-select radio slot; extraServes is an
 * ordered list (first added → first promoted).
 */
object GoalLinkLogic {

    data class LinkSelection(val primaryId: String?, val extraServes: List<String>) {
        /** DM-GOAL-6 — full theme set (primary + serves-also). */
        fun themeIds(): List<String> =
            ((primaryId?.let(::listOf) ?: emptyList()) + extraServes).distinct()
    }

    /** Split a goal's stored (primaryThemeId, themeIds) into editor state. */
    fun fromGoal(primaryThemeId: String?, themeIds: List<String>): LinkSelection =
        LinkSelection(
            primaryId = primaryThemeId,
            extraServes = themeIds.filter { it != primaryThemeId }.distinct(),
        )

    /**
     * FR-DIR-8.6 — tap a theme in the primary selector.
     *  - If it's the current primary: deselect and promote the topmost
     *    extraServes entry (or null when none remain).
     *  - Otherwise: swap primary. If the tapped theme was in extraServes,
     *    remove it from there (a theme can't be both primary and shared).
     *    The previously-primary theme is not auto-moved to extraServes —
     *    the user picked a *different* primary, not a secondary; the old
     *    one is dropped unless the user re-adds it via serves-also.
     */
    fun tapPrimary(current: LinkSelection, tappedThemeId: String): LinkSelection {
        if (current.primaryId == tappedThemeId) {
            val promoted = current.extraServes.firstOrNull()
            return LinkSelection(
                primaryId = promoted,
                extraServes = if (current.extraServes.isEmpty()) emptyList()
                              else current.extraServes.drop(1),
            )
        }
        return LinkSelection(
            primaryId = tappedThemeId,
            extraServes = current.extraServes.filter { it != tappedThemeId },
        )
    }

    /**
     * The explicit "No theme (orphan)" option — sets primary to null
     * without firing the promotion rule. extraServes is preserved so an
     * orphan-serves-two-themes state stays representable.
     */
    fun tapOrphan(current: LinkSelection): LinkSelection =
        LinkSelection(primaryId = null, extraServes = current.extraServes)

    /**
     * Toggle a theme in the serves-also multi-select. Primary is never
     * touched (its chip is the primary selector's, not this one).
     */
    fun toggleExtra(current: LinkSelection, tappedThemeId: String): LinkSelection {
        if (tappedThemeId == current.primaryId) return current
        val next = if (tappedThemeId in current.extraServes) {
            current.extraServes.filter { it != tappedThemeId }
        } else {
            current.extraServes + tappedThemeId
        }
        return current.copy(extraServes = next)
    }
}
