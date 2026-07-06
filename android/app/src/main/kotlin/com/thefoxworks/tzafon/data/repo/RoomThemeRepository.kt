package com.thefoxworks.tzafon.data.repo

import com.thefoxworks.tzafon.data.db.ThemeDao
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed ThemeRepository (DM-THEME). */
class RoomThemeRepository(
    private val dao: ThemeDao,
) : ThemeRepository {

    override fun observeThemes(): Flow<List<Theme>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTheme(id: String): Theme? = dao.get(id)?.toDomain()

    override suspend fun upsert(theme: Theme) = dao.upsert(theme.toEntity())

    override suspend fun delete(id: String) = dao.delete(id)
}
