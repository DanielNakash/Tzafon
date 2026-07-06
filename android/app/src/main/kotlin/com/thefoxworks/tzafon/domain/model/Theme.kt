package com.thefoxworks.tzafon.domain.model

import kotlinx.coroutines.flow.Flow

/** DM-THEME-2 — three states; upcoming never counts toward the cap. */
enum class ThemeState { ACTIVE, UPCOMING, ARCHIVED }

/** DM-THEME — archive outcomes feed Journey's "directions over time". */
enum class ArchivedOutcome { RENEWED, EVOLVED, RETIRED }

data class Theme(
    val id: String,
    /** approach-framed — the editor nudges "more of…" phrasing (PRIN-3) */
    val name: String,
    /** mandatory — a theme without a why "fluffifies" (DM-THEME-1) */
    val why: String,
    val windowStart: String,           // ISO; default today
    val windowEnd: String,             // ISO; default one quarter out (DEC-5)
    val state: ThemeState = ThemeState.UPCOMING,
    val archivedOutcome: ArchivedOutcome? = null,
    val renewedToThemeId: String? = null,
    /** cycles the three harmonized Den accents */
    val accentSlot: Int = 0,
    val createdAt: Long = 0,
    val archivedAt: Long? = null,
)

interface ThemeRepository {
    fun observeThemes(): Flow<List<Theme>>
    suspend fun getTheme(id: String): Theme?
    suspend fun upsert(theme: Theme)
    suspend fun delete(id: String)
}
