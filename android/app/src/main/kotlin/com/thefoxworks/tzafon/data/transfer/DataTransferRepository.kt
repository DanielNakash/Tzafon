package com.thefoxworks.tzafon.data.transfer

import androidx.room.withTransaction
import com.thefoxworks.tzafon.data.db.GoalDao
import com.thefoxworks.tzafon.data.db.HabitDao
import com.thefoxworks.tzafon.data.db.ReviewDao
import com.thefoxworks.tzafon.data.db.SeriesDao
import com.thefoxworks.tzafon.data.db.TaskDao
import com.thefoxworks.tzafon.data.db.ThemeDao
import com.thefoxworks.tzafon.data.db.TzafonDatabase
import com.thefoxworks.tzafon.data.db.toEntity
import kotlinx.coroutines.flow.first

/**
 * FR-DATA-2.7 / FR-DATA-2.8 — the transactional apply seam for import.
 *
 * All writes happen inside a single Room transaction. On any failure the
 * transaction rolls back and the store is left byte-identical to what it
 * was before the import. The two conflict policies map to DAO behavior:
 *   - Backup wins    → `upsert` (REPLACE) on every row (the DAOs already
 *                       use `OnConflictStrategy.REPLACE`).
 *   - Current wins   → insert only rows whose primary key is not present.
 */
enum class ConflictPolicy { BACKUP_WINS, CURRENT_WINS }

data class ImportStats(
    /** Rows in the file that were newly inserted. */
    val inserted: Int,
    /** Rows in the file that overwrote an existing row (BACKUP_WINS). */
    val replaced: Int,
    /** Rows in the file that were skipped because a key already existed (CURRENT_WINS). */
    val skipped: Int,
)

interface DataTransferRepository {
    /** True iff every collection has zero rows (`FR-DATA-2.8`). */
    suspend fun isEmpty(): Boolean

    /** Apply the snapshot atomically under the chosen policy. */
    suspend fun importAll(document: ExportDocument, policy: ConflictPolicy): ImportStats
}

class RoomDataTransferRepository(
    private val db: TzafonDatabase,
    private val taskDao: TaskDao,
    private val seriesDao: SeriesDao,
    private val habitDao: HabitDao,
    private val goalDao: GoalDao,
    private val themeDao: ThemeDao,
    private val reviewDao: ReviewDao,
) : DataTransferRepository {

    override suspend fun isEmpty(): Boolean {
        if (taskDao.getAll().isNotEmpty()) return false
        if (seriesDao.getAll().isNotEmpty()) return false
        if (habitDao.observeAll().first().isNotEmpty()) return false
        if (habitDao.observeLogs().first().isNotEmpty()) return false
        if (goalDao.observeAll().first().isNotEmpty()) return false
        if (goalDao.observeContributions().first().isNotEmpty()) return false
        if (themeDao.observeAll().first().isNotEmpty()) return false
        if (reviewDao.observeAll().first().isNotEmpty()) return false
        return true
    }

    override suspend fun importAll(document: ExportDocument, policy: ConflictPolicy): ImportStats {
        var inserted = 0
        var replaced = 0
        var skipped = 0

        db.withTransaction {
            // Snapshot existing primary keys inside the tx so we can decide
            // insert / replace / skip without repeatedly probing the DAOs.
            val existingTaskIds = taskDao.getAll().mapTo(HashSet()) { it.id }
            val existingSeriesIds = seriesDao.getAll().mapTo(HashSet()) { it.id }
            val existingHabitIds = habitDao.observeAll().first().mapTo(HashSet()) { it.id }
            val existingLogKeys = habitDao.observeLogs().first().mapTo(HashSet()) { it.habitId to it.date }
            val existingGoalIds = goalDao.observeAll().first().mapTo(HashSet()) { it.id }
            val existingThemeIds = themeDao.observeAll().first().mapTo(HashSet()) { it.id }
            val existingReviewIds = reviewDao.observeAll().first().mapTo(HashSet()) { it.id }
            val existingContribIds = goalDao.observeContributions().first().mapTo(HashSet()) { it.id }

            for (t in document.collections.tasks) {
                val existed = t.id in existingTaskIds
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                taskDao.upsert(t.toEntity()); if (existed) replaced++ else inserted++
            }
            for (s in document.collections.series) {
                val existed = s.id in existingSeriesIds
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                seriesDao.upsert(s.toEntity()); if (existed) replaced++ else inserted++
            }
            for (h in document.collections.habits) {
                val existed = h.id in existingHabitIds
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                habitDao.upsert(h.toEntity()); if (existed) replaced++ else inserted++
            }
            for (l in document.collections.habitLogs) {
                val existed = (l.habitId to l.date) in existingLogKeys
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                habitDao.upsertLog(l.toEntity()); if (existed) replaced++ else inserted++
            }
            for (g in document.collections.goals) {
                val existed = g.id in existingGoalIds
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                goalDao.upsert(g.toEntity()); if (existed) replaced++ else inserted++
            }
            for (th in document.collections.themes) {
                val existed = th.id in existingThemeIds
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                themeDao.upsert(th.toEntity()); if (existed) replaced++ else inserted++
            }
            for (r in document.collections.reviews) {
                val existed = r.id in existingReviewIds
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                reviewDao.upsert(r.toEntity()); if (existed) replaced++ else inserted++
            }
            for (c in document.collections.contributions) {
                val existed = c.id in existingContribIds
                if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                goalDao.insertContribution(c.toEntity()); if (existed) replaced++ else inserted++
            }
        }

        return ImportStats(inserted = inserted, replaced = replaced, skipped = skipped)
    }
}
