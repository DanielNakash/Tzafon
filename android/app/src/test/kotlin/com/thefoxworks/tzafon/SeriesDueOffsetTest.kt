package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.SeriesEntity
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.data.sync.FirestoreCodec
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.domain.recurrence.Rule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v2.13.0 — the per-occurrence deadline offset (ADR 0001) survives the
 * series' trip through Room and the Firestore mirror, and a document written
 * before the field existed still reads.
 */
class SeriesDueOffsetTest {

    private val mondays = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(1))

    private fun offsetSeries() = Series(
        id = "s1",
        title = "Water the plants",
        rule = mondays,
        ruleAnchor = "2026-06-01",
        startDate = "2026-06-01",
        dueMode = DueMode.OFFSET,
        dueOffsetDays = 3,
    )

    @Test
    fun `an offset series round-trips through its Room row and the Firestore codec`() {
        val series = offsetSeries()
        val map = FirestoreCodec.toMap(series.toEntity())
        val back = FirestoreCodec.fromMap(map, SeriesEntity::class.java).toDomain()
        assertEquals(series, back)
    }

    @Test
    fun `a Firestore document from before v2_13 reads with no offset`() {
        val map = FirestoreCodec.toMap(offsetSeries().copy(dueMode = DueMode.NONE).toEntity()) - "dueOffsetDays"
        val back = FirestoreCodec.fromMap(map, SeriesEntity::class.java).toDomain()
        assertEquals(DueMode.NONE, back.dueMode)
        assertNull(back.dueOffsetDays)
    }

    @Test
    fun `the engine sees the series' offset`() {
        val plan = Recurrence.planOccurrences(offsetSeries().toSpec(), horizon = "2026-06-14")
        assertEquals(listOf("2026-06-04", "2026-06-11"), plan.create.map { it.due })
    }

    // ── the editor's save path keeps the offset ──

    private fun dueByOccurrence(taskDao: FakeTaskDao, seriesId: String): Map<String, String?> =
        taskDao.rows.value.values
            .filter { it.seriesId == seriesId }
            .associate { it.occurrenceDate!! to it.dueDate }
            .toSortedMap()

    @Test
    fun `a new repeating task saved with an offset gets per-occurrence deadlines`() = runBlocking {
        val taskDao = FakeTaskDao()
        val seriesDao = FakeSeriesDao()
        val repo = RoomTaskRepository(taskDao, seriesDao)
        val draft = TaskDraft(
            id = null,
            title = "Water the plants",
            toDoDate = "2026-06-01",
            recurrence = RecurrenceDraft(rule = mondays, dueMode = DueMode.OFFSET, dueOffsetDays = 3),
        )
        repo.saveDraft(draft, EditScope.ALL, "2026-06-01")

        val series = seriesDao.rows.value.values.single().toDomain()
        assertEquals(DueMode.OFFSET, series.dueMode)
        assertEquals(3, series.dueOffsetDays)
        val dues = dueByOccurrence(taskDao, series.id)
        assertEquals("2026-06-04", dues["2026-06-01"])
        assertEquals("2026-06-11", dues["2026-06-08"])
    }

    @Test
    fun `editing a whole offset series to a new offset re-dates its deadlines`() = runBlocking {
        val taskDao = FakeTaskDao()
        val seriesDao = FakeSeriesDao()
        seriesDao.upsert(offsetSeries().copy(generatedThrough = "2026-06-01").toEntity())
        val repo = RoomTaskRepository(taskDao, seriesDao)
        repo.topUp("2026-06-01", 60)

        val existing = taskDao.get("s1__2026-06-08")!!.toDomain()
        val draft = TaskDraft(
            id = existing.id,
            title = existing.title,
            toDoDate = "2026-06-08",
            recurrence = RecurrenceDraft(rule = mondays, dueMode = DueMode.OFFSET, dueOffsetDays = 1),
        )
        repo.saveDraft(draft, EditScope.ALL, "2026-06-01")

        assertEquals(1, seriesDao.get("s1")?.toDomain()?.dueOffsetDays)
        val dues = dueByOccurrence(taskDao, "s1")
        assertEquals("2026-06-09", dues["2026-06-08"])
        assertEquals("2026-06-16", dues["2026-06-15"])
    }
}
