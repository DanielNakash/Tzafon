package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.Theme

/**
 * v2.13.0 — the Alignment pane's read-only theme line. [names] are the
 * themes to show in bold, and [source] says where they come from ("the goal",
 * "the goals and the habit", …); with no names the line is the faint
 * "No theme yet — it comes from a goal".
 */
data class InheritedTheme(val names: List<String>, val source: String?) {

    /** The whole line as one string (for TalkBack, and as the plain-text form). */
    val sentence: String get() =
        if (names.isEmpty()) NONE
        else "${if (names.size == 1) "Theme" else "Themes"} ${names.joinToString(" · ")} — inherited from $source"

    companion object {
        const val NONE = "No theme yet — it comes from a goal"

        /**
         * The primary themes of the goals [draft] moves (in link order), then
         * of the habit it ticks, each named once. Only the links that bring a
         * theme count as its source; links to anything deleted are skipped.
         * `TaskDraft.themeId` is ignored: no screen sets it, and it's carried
         * through untouched.
         */
        fun of(draft: TaskDraft, goals: List<Goal>, habits: List<Habit>, themes: List<Theme>): InheritedTheme {
            val byId = themes.associateBy { it.id }
            val goalThemes = draft.goalIds.mapNotNull { id -> goals.firstOrNull { it.id == id }?.primaryThemeId?.let(byId::get) }
            val habitTheme = draft.habitId?.let { id -> habits.firstOrNull { it.id == id } }?.primaryThemeId?.let(byId::get)
            val names = (goalThemes + listOfNotNull(habitTheme)).map { it.name }.distinct()
            val source = listOfNotNull(
                when (goalThemes.size) { 0 -> null; 1 -> "the goal"; else -> "the goals" },
                habitTheme?.let { "the habit" },
            ).joinToString(" and ").takeIf { it.isNotEmpty() }
            return InheritedTheme(names, source)
        }
    }
}
