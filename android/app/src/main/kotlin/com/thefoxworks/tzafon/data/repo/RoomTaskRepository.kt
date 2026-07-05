package com.thefoxworks.tzafon.data.repo

import com.thefoxworks.tzafon.data.db.SeriesDao
import com.thefoxworks.tzafon.data.db.TaskDao
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Room-backed TaskRepository. The save/delete/generation semantics are a
 * faithful port of the v1.1.0 reference (src/services/series.js): occurrences
 * are real rows with deterministic ids "{seriesId}__{occurrenceDate}", so
 * generation is idempotent; deleting one occurrence records an exception so
 * the slot never regenerates.
 */
class RoomTaskRepository(
    private val taskDao: TaskDao,
    private val seriesDao: SeriesDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeSeries(): Flow<List<Series>> =
        seriesDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTask(id: String): Task? = taskDao.get(id)?.toDomain()

    override suspend fun getSeries(id: String): Series? = seriesDao.get(id)?.toDomain()

    // ── generation ────────────────────────────────────────────

    private fun occurrenceId(seriesId: String, date: String) = "${seriesId}__$date"

    /** Materialize missing occurrences for one series up to the horizon. */
    private suspend fun generate(series: Series, existingDates: Set<String>, today: String, horizon: String) {
        if (series.frozen) return // FR-REC-5: a frozen series stops generating
        val plan = Recurrence.planOccurrences(series.toSpec(), horizon, existingDates, today)
        if (plan.create.isNotEmpty()) {
            val rows = plan.create.map { c ->
                Task(
                    id = occurrenceId(series.id, c.occurrenceDate),
                    title = series.title,
                    description = series.description,
                    toDoDate = c.toDo,
                    dueDate = c.due,
                    state = TaskState.OPEN,
                    seriesId = series.id,
                    occurrenceDate = c.occurrenceDate,
                    overridden = false,
                    cue = series.cue,
                    themeId = series.themeId,
                    habitId = series.habitId,
                    goalIds = series.goalIds,
                    createdAt = now(),
                    stateChangedAt = now(),
                ).toEntity()
            }
            taskDao.upsertAll(rows)
        }
        if (series.generatedThrough != plan.generatedThrough) {
            seriesDao.upsert(series.copy(generatedThrough = plan.generatedThrough).toEntity())
        }
    }

    override suspend fun topUp(today: String, horizonDays: Long) {
        val horizon = Dates.addDays(today, maxOf(horizonDays, Recurrence.HORIZON_DAYS))
        val allTasks = taskDao.getAll()
        val datesBySeries = allTasks.filter { it.seriesId != null }
            .groupBy({ it.seriesId!! }, { it.occurrenceDate ?: "" })
            .mapValues { (_, v) -> v.toSet() }
        for (row in seriesDao.getAll()) {
            val series = row.toDomain()
            generate(series, datesBySeries[series.id] ?: emptySet(), today, horizon)
        }
    }

    // ── saving (v1.1.0 saveTask semantics) ────────────────────

    override suspend fun saveDraft(draft: TaskDraft, scope: EditScope, today: String) {
        val existing = draft.id?.let { taskDao.get(it)?.toDomain() }
        val rec = draft.recurrence

        // ── non-recurring result ──
        if (rec == null) {
            if (existing?.seriesId != null && scope == EditScope.ALL) {
                // recurrence turned off for the whole series → collapse to standalone
                deleteSeriesAndOccurrences(existing.seriesId)
            }
            val keepId = existing != null && !(existing.seriesId != null && scope == EditScope.ALL)
            val base = if (keepId) existing!! else Task(id = UUID.randomUUID().toString(), title = "", createdAt = now(), stateChangedAt = now())
            taskDao.upsert(
                base.copy(
                    title = draft.title.trim(),
                    description = draft.description,
                    toDoDate = draft.toDoDate,
                    dueDate = draft.dueDate,
                    seriesId = null,
                    occurrenceDate = null,
                    overridden = false,
                    cue = draft.cue,
                    themeId = draft.themeId,
                    habitId = draft.habitId,
                    goalIds = draft.goalIds,
                    commitment = draft.commitment,
                ).toEntity()
            )
            return
        }

        // ── recurring result ──
        // new recurring task, or a standalone being converted into one
        if (existing?.seriesId == null) {
            val anchor = draft.toDoDate ?: today
            val series = Series(
                id = UUID.randomUUID().toString(),
                title = draft.title.trim(),
                description = draft.description,
                rule = rec.rule,
                ruleAnchor = anchor,
                startDate = anchor,
                endDate = rec.endDate,
                dueMode = rec.dueMode,
                dueRule = if (rec.dueMode == DueMode.RECURRING) rec.dueRule else null,
                dueSingular = if (rec.dueMode == DueMode.SINGULAR) draft.dueDate else null,
                exceptions = emptyList(),
                generatedThrough = anchor,
                cue = draft.cue,
                themeId = draft.themeId,
                habitId = draft.habitId,
                goalIds = draft.goalIds,
                createdAt = now(),
            )
            seriesDao.upsert(series.toEntity())
            if (existing != null) taskDao.delete(existing.id) // drop the old standalone row
            generate(series, emptySet(), today, Dates.addDays(today, Recurrence.HORIZON_DAYS))
            return
        }

        // editing an existing recurring occurrence
        val series = seriesDao.get(existing.seriesId)?.toDomain() ?: return

        if (scope == EditScope.ONE) {
            // reschedule / edit this occurrence only — series untouched
            taskDao.upsert(
                existing.copy(
                    title = draft.title.trim(),
                    description = draft.description,
                    toDoDate = draft.toDoDate,
                    dueDate = draft.dueDate,
                    overridden = true,
                    cue = draft.cue,
                    themeId = draft.themeId,
                    habitId = draft.habitId,
                    goalIds = draft.goalIds,
                    commitment = draft.commitment,
                ).toEntity()
            )
            return
        }

        val chosen = existing.occurrenceDate ?: existing.toDoDate ?: today
        val anchor = if (scope == EditScope.FORWARD) chosen else series.startDate
        val updated = series.copy(
            title = draft.title.trim(),
            description = draft.description,
            rule = rec.rule,
            ruleAnchor = anchor,
            startDate = series.startDate, // ALL keeps original start; FORWARD anchors at chosen
            endDate = rec.endDate,
            dueMode = rec.dueMode,
            dueRule = if (rec.dueMode == DueMode.RECURRING) rec.dueRule else null,
            dueSingular = if (rec.dueMode == DueMode.SINGULAR) draft.dueDate else null,
            generatedThrough = anchor,
            cue = draft.cue,
            themeId = draft.themeId,
            habitId = draft.habitId,
            goalIds = draft.goalIds,
        )
        seriesDao.upsert(updated.toEntity())

        // remove auto-generated, still-open occurrences the new rule replaces;
        // keep overridden/settled ones but refresh their text
        val occ = taskDao.occurrencesOf(series.id).map { it.toDomain() }
        val settled = { t: Task -> t.state != TaskState.OPEN }
        for (o in occ) {
            val inRange = scope == EditScope.ALL || (o.occurrenceDate ?: "") >= chosen
            if (inRange && !o.overridden && !settled(o)) {
                taskDao.delete(o.id)
            } else if (inRange) {
                taskDao.upsert(o.copy(title = draft.title.trim(), description = draft.description).toEntity())
            }
        }

        val remaining = occ.filter { o ->
            (if (scope == EditScope.ALL) false else (o.occurrenceDate ?: "") < chosen) || o.overridden || settled(o)
        }
        generate(
            updated,
            remaining.mapNotNull { it.occurrenceDate }.toSet(),
            today,
            Dates.addDays(today, Recurrence.HORIZON_DAYS),
        )
    }

    // ── deleting ──────────────────────────────────────────────

    private suspend fun deleteSeriesAndOccurrences(seriesId: String) {
        val occ = taskDao.occurrencesOf(seriesId)
        taskDao.deleteAll(occ.map { it.id })
        seriesDao.delete(seriesId)
    }

    override suspend fun deleteTask(id: String, scope: EditScope) {
        val existing = taskDao.get(id)?.toDomain() ?: return
        val seriesId = existing.seriesId
        if (seriesId == null) {
            taskDao.delete(id)
            return
        }
        if (scope == EditScope.ALL) {
            deleteSeriesAndOccurrences(seriesId)
            return
        }
        // delete just this occurrence; record an exception so it never regenerates
        taskDao.delete(id)
        val series = seriesDao.get(seriesId)?.toDomain() ?: return
        existing.occurrenceDate?.let { date ->
            if (date !in series.exceptions) {
                seriesDao.upsert(series.copy(exceptions = series.exceptions + date).toEntity())
            }
        }
    }

    // ── done toggle (M0 parity; the full machine lands in M1) ─

    override suspend fun toggleDone(id: String) {
        val t = taskDao.get(id)?.toDomain() ?: return
        val next = if (t.state == TaskState.DONE) TaskState.OPEN else TaskState.DONE
        taskDao.upsert(
            t.copy(
                state = next,
                stateChangedAt = now(),
                completedAt = if (next == TaskState.DONE) now() else null,
            ).toEntity()
        )
    }
}
