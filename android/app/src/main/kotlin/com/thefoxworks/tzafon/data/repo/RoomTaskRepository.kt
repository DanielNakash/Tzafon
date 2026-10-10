package com.thefoxworks.tzafon.data.repo

import com.thefoxworks.tzafon.data.db.SeriesDao
import com.thefoxworks.tzafon.data.db.TaskDao
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.domain.attribution.Attribution
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.editor.ChangeField
import com.thefoxworks.tzafon.domain.editor.DraftChanges
import com.thefoxworks.tzafon.domain.editor.DraftDiff
import com.thefoxworks.tzafon.domain.editor.ScopeSheetCopy
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Contribution
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.domain.status.StatusSnapshot
import com.thefoxworks.tzafon.domain.status.StatusUndo
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
    /** the DM-ATTR-2 habit ledger (null only in narrow tests) */
    private val habits: HabitRepository? = null,
    /** the DM-ATTR-1 goal ledger (null only in narrow tests) */
    private val goals: GoalRepository? = null,
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
        // what Save changes, measured against the task as stored
        val changes = existing?.let { t ->
            DraftDiff.of(TaskDraft.of(t, t.seriesId?.let { seriesDao.get(it)?.toDomain() }), draft)
        }
        if (existing?.seriesId != null) {
            require(changes!!.allows(scope)) { "$scope can't take these changes to a repeating task" }
        }
        // nothing but a status (or nothing at all) changed: no field is written
        val savedId = if (changes != null && !changes.asksScope) existing.id
        else saveFields(draft, scope, today, existing, changes)
        // v2.13.0 — a single-task status waits in the draft and lands last, on
        // this occurrence only, through the same path the lists use
        if (existing != null && savedId != null && draft.state != existing.state) {
            setState(savedId, draft.state, today, draft.habitAmount)
        }
    }

    /**
     * The field edits of [saveDraft]. Returns the id this task or occurrence
     * has after the save, or null when it has none to take a status.
     */
    private suspend fun saveFields(
        draft: TaskDraft,
        scope: EditScope,
        today: String,
        existing: Task?,
        changes: DraftChanges?,
    ): String? {
        val rec = draft.recurrence

        // ── non-recurring result ──
        if (rec == null) {
            if (existing?.seriesId != null) {
                // repeat turned off: the series ends the day before this one,
                // which carries the edits on as a one-off (keeping its id, so
                // what it ticked and moved stays linked)
                endSeriesBefore(existing)
            }
            // FR-BACKLOG-5: honor draft.state when creating a new task, so quick-adds
            // from Backlog land as BACKLOG (not OPEN). Existing tasks keep their state
            // — setState() is the sanctioned way to move between states.
            val base = existing ?: Task(
                id = UUID.randomUUID().toString(),
                title = "",
                state = draft.state,
                createdAt = now(),
                stateChangedAt = now(),
            )
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
            return base.id
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
                dueOffsetDays = if (rec.dueMode == DueMode.OFFSET) rec.dueOffsetDays else null,
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
            return occurrenceId(series.id, anchor)
        }

        // editing an existing recurring occurrence
        val series = seriesDao.get(existing.seriesId)?.toDomain() ?: return null

        if (scope == EditScope.ONE) {
            // reschedule / edit this occurrence only — series untouched
            editThisOne(existing, draft)
            return existing.id
        }

        // FR-REC-6 — the original occurrence date bounds the "delete from here"
        // prune. A moved to-do date re-counts the rhythm only under Every N
        // days, where it becomes the new rule anchor; on weekly and monthly
        // rules only this occurrence moves (v2.13.0), and a new rule counts
        // from this occurrence's slot.
        val originalOccurrenceDate = existing.occurrenceDate ?: existing.toDoDate ?: today
        val fields = changes!!.changes.map { it.field }.toSet()
        val recounts = ChangeField.TODO_DATE in fields && ScopeSheetCopy.recountsOnMove(rec.rule)
        val newAnchor = when {
            recounts -> draft.toDoDate ?: originalOccurrenceDate
            ChangeField.REPEAT in fields -> originalOccurrenceDate
            else -> series.ruleAnchor ?: originalOccurrenceDate
        }
        // this occurrence's own dates changed without re-counting: it keeps
        // them as an edited occurrence rather than being regenerated
        val keepsOwnDates = !recounts &&
            changes.changes.any { it.field in setOf(ChangeField.TODO_DATE, ChangeField.DEADLINE) && !it.repeatCard }
        val updated = series.copy(
            title = draft.title.trim(),
            description = draft.description,
            rule = rec.rule,
            ruleAnchor = newAnchor,
            // FR-REC-6.3 — ALL rewrites startDate to the new anchor (its
            // pre-anchor open non-settled rows are cleared by the loop below).
            // FORWARD keeps the historical startDate so past occurrences
            // remain rule-consistent with when they were generated.
            startDate = if (scope == EditScope.ALL) newAnchor else series.startDate,
            endDate = rec.endDate,
            dueMode = rec.dueMode,
            dueRule = if (rec.dueMode == DueMode.RECURRING) rec.dueRule else null,
            dueSingular = if (rec.dueMode == DueMode.SINGULAR) draft.dueDate else null,
            dueOffsetDays = if (rec.dueMode == DueMode.OFFSET) rec.dueOffsetDays else null,
            generatedThrough = newAnchor,
            cue = draft.cue,
            themeId = draft.themeId,
            habitId = draft.habitId,
            goalIds = draft.goalIds,
        )
        seriesDao.upsert(updated.toEntity())
        if (keepsOwnDates) editThisOne(existing, draft)

        // remove auto-generated, still-open occurrences the new rule replaces;
        // keep overridden/settled ones but refresh their text and links —
        // DM-HABIT-8: an ALL-scope link change applies to past occurrences too.
        // FR-REC-6.2 — prune cutoff stays at the ORIGINAL occurrence date so a
        // backward date-shift still clears the edited slot.
        val occ = taskDao.occurrencesOf(series.id).map { it.toDomain() }
        val settled = { t: Task -> t.state != TaskState.OPEN }
        for (o in occ) {
            val inRange = scope == EditScope.ALL || (o.occurrenceDate ?: "") >= originalOccurrenceDate
            if (inRange && !o.overridden && !settled(o)) {
                taskDao.delete(o.id)
            } else if (inRange) {
                taskDao.upsert(
                    o.copy(
                        title = draft.title.trim(),
                        description = draft.description,
                        cue = draft.cue,
                        themeId = draft.themeId,
                        habitId = draft.habitId,
                        goalIds = draft.goalIds,
                        commitment = draft.commitment,
                    ).toEntity(),
                )
            }
        }

        val remaining = occ.filter { o ->
            (if (scope == EditScope.ALL) false else (o.occurrenceDate ?: "") < originalOccurrenceDate) || o.overridden || settled(o)
        }
        generate(
            updated,
            remaining.mapNotNull { it.occurrenceDate }.toSet(),
            today,
            Dates.addDays(today, Recurrence.HORIZON_DAYS),
        )
        // a re-count regenerates this occurrence at its new date
        return if (recounts) occurrenceId(series.id, newAnchor) else existing.id
    }

    /** This occurrence takes the draft as it stands, dates included, as an edited occurrence. */
    private suspend fun editThisOne(occurrence: Task, draft: TaskDraft) {
        taskDao.upsert(
            occurrence.copy(
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
    }

    override suspend fun countEarlierOccurrences(taskId: String, notDoneOnly: Boolean): Int {
        val t = taskDao.get(taskId)?.toDomain() ?: return 0
        val seriesId = t.seriesId ?: return 0
        val date = t.occurrenceDate ?: return 0
        return taskDao.occurrencesOf(seriesId).map { it.toDomain() }.count { o ->
            (o.occurrenceDate ?: "") < date && !(notDoneOnly && o.state == TaskState.DONE)
        }
    }

    /**
     * End [occurrence]'s series the day before its slot: the series stops
     * there, and this occurrence and every later one that isn't Done go.
     * Earlier occurrences are untouched. The caller decides what becomes of
     * [occurrence] itself; it is deleted here only if it isn't Done.
     */
    private suspend fun endSeriesBefore(occurrence: Task) {
        val seriesId = occurrence.seriesId ?: return
        val date = occurrence.occurrenceDate ?: return
        seriesDao.get(seriesId)?.toDomain()?.let { s ->
            seriesDao.upsert(s.copy(endDate = Dates.addDays(date, -1)).toEntity())
        }
        val gone = taskDao.occurrencesOf(seriesId).map { it.toDomain() }
            .filter { (it.occurrenceDate ?: "") >= date && it.state != TaskState.DONE }
        taskDao.deleteAll(gone.map { it.id })
    }

    // ── deleting ──────────────────────────────────────────────

    /**
     * The whole series goes with every occurrence not Done. Done ones stay as
     * one-off tasks, so what they ticked and moved stays where it is.
     */
    private suspend fun deleteSeriesKeepingDone(seriesId: String) {
        val (done, rest) = taskDao.occurrencesOf(seriesId).map { it.toDomain() }
            .partition { it.state == TaskState.DONE }
        taskDao.deleteAll(rest.map { it.id })
        taskDao.upsertAll(done.map { it.copy(seriesId = null, occurrenceDate = null, overridden = false).toEntity() })
        seriesDao.delete(seriesId)
    }

    override suspend fun deleteTask(id: String, scope: EditScope) {
        val existing = taskDao.get(id)?.toDomain() ?: return
        val seriesId = existing.seriesId
        if (seriesId == null) {
            taskDao.delete(id)
            return
        }
        when (scope) {
            EditScope.ALL -> { deleteSeriesKeepingDone(seriesId); return }
            EditScope.FORWARD -> { endSeriesBefore(existing); return }
            EditScope.ONE -> Unit
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

    // ── action layer (M2) ─────────────────────────────────────

    override suspend fun setSortOrders(orders: Map<String, Long>) {
        taskDao.setSortOrders(orders)
    }

    override suspend fun setFocusDate(id: String, date: String?) {
        val t = taskDao.get(id)?.toDomain() ?: return
        taskDao.upsert(t.copy(focusDate = date).toEntity())
    }

    override suspend fun setWeekPriorities(ids: List<String>, weekStart: String) {
        // clear old marks for this week, then stamp the chosen set
        for (row in taskDao.getAll()) {
            val t = row.toDomain()
            val marked = t.focusWeekStart == weekStart
            val shouldMark = t.id in ids
            if (marked && !shouldMark) {
                taskDao.upsert(t.copy(focusWeekStart = null).toEntity())
            } else if (!marked && shouldMark) {
                taskDao.upsert(t.copy(focusWeekStart = weekStart).toEntity())
            }
        }
    }

    override suspend fun reschedule(id: String, newToDoDate: String?) {
        val t = taskDao.get(id)?.toDomain() ?: return
        taskDao.upsert(
            t.copy(
                toDoDate = newToDoDate,
                // a rescheduled occurrence detaches from its slot (v1.1.0 one-off edit)
                overridden = t.overridden || t.seriesId != null,
            ).toEntity()
        )
    }

    // ── state machine (DM-TASK-1/2/3, FR-REC-5) ───────────────

    override suspend fun toggleDone(id: String, habitAmount: Double?) {
        val t = taskDao.get(id)?.toDomain() ?: return
        val next = if (t.state == TaskState.DONE) TaskState.OPEN else TaskState.DONE
        setState(id, next, Dates.todayIso(), habitAmount)
    }

    override suspend fun setState(
        id: String,
        target: TaskState,
        today: String,
        habitAmount: Double?,
        scope: StatusScope,
    ) {
        val t = taskDao.get(id)?.toDomain() ?: return
        val series = t.seriesId?.let { seriesDao.get(it)?.toDomain() }
        val fx = StateMachine.transition(t.state, target, series?.let { SeriesStatus.of(it, today) }, scope)
        if (t.state != target) flip(t, fx, today, habitAmount)
        if (series != null) applySeriesEffects(series, t, fx, today)
    }

    override suspend fun statusSnapshot(id: String): StatusSnapshot? {
        val t = getTask(id) ?: return null
        return StatusSnapshot(
            task = t,
            habitLogs = habits?.logsForTask(id).orEmpty(),
            contributions = goals?.contributionsFor(id).orEmpty(),
        )
    }

    override suspend fun restoreStatus(before: StatusSnapshot, after: Task): Boolean {
        val id = after.id
        if (!StatusUndo.canUndo(after, getTask(id))) return false
        if (StatusUndo.touchesLedger(before.task.state, after.state)) {
            // drop what the change wrote, then put the old rows back as they were
            habits?.reverseForTask(id)
            goals?.reverseForTask(id)
            before.habitLogs.forEach { habits?.logForTask(it.habitId, it.date, id, it.amount) }
            goals?.applyForTask(id, before.contributions)
        }
        taskDao.upsert(before.task.toEntity())
        return true
    }

    /** The occurrence's own change: contribution up-flow or reversal, then the row. */
    private suspend fun flip(t: Task, fx: StateMachine.Effects, today: String, habitAmount: Double?) {
        // DM-ATTR — the up-flow, counted once: habit ledger first (ATTR-2),
        // then goal attribution (ATTR-1: gather → dedupe → apply once)
        if (fx.applyContribution) {
            val habit = t.habitId?.let { habits?.getHabit(it) }
            if (t.habitId != null) {
                habits?.logForTask(t.habitId, HabitMath.logDateFor(t, today), t.id, habitAmount)
            }
            if (goals != null) {
                val reachableIds = (t.goalIds + listOfNotNull(habit?.goalId)).distinct()
                val goalsById = reachableIds
                    .mapNotNull { goals.getGoal(it) }
                    .associateBy { it.id }
                val plan = Attribution.planOnDone(t, habit, goalsById, habitAmount)
                val rows = plan.mapNotNull { p ->
                    p.amount?.takeIf { it != 0.0 }?.let { a ->
                        Contribution(
                            id = UUID.randomUUID().toString(),
                            taskId = t.id, goalId = p.goalId,
                            amount = a, via = p.via, createdAt = now(),
                        )
                    }
                }
                goals.applyForTask(t.id, rows)
                // directional touches still feed identity (bump activity)
                plan.filter { it.amount == null }.forEach { p ->
                    goals.getGoal(p.goalId)?.let { g ->
                        goals.upsert(g.copy(lastActivityAt = now()))
                    }
                }
            }
        }
        if (fx.reverseContribution) {
            habits?.reverseForTask(t.id)
            goals?.reverseForTask(t.id) // subtracts exactly the ledger rows
        }

        taskDao.upsert(
            t.copy(
                state = fx.newState,
                stateChangedAt = now(),
                completedAt = when {
                    fx.setCompleted -> now()
                    fx.clearCompleted -> null
                    else -> t.completedAt
                },
                toDoDate = if (fx.clearToDoDate) null else t.toDoDate,
            ).toEntity()
        )

    }

    /**
     * Fill a thawed or reopened series in again from [today]. Slots before
     * today that are gone (dropped while it was frozen or closed) are recorded
     * as exceptions, so neither this nor a later topUp brings them back.
     */
    private suspend fun resumeFromToday(series: Series, today: String) {
        val existing = taskDao.occurrencesOf(series.id).mapNotNull { it.occurrenceDate }.toSet()
        val gone = Recurrence.planOccurrences(series.toSpec(), Dates.addDays(today, -1), existing)
            .create.map { it.occurrenceDate }
        val resumed = series.copy(exceptions = (series.exceptions + gone).distinct())
        seriesDao.upsert(resumed.toEntity())
        generate(resumed, existing, today, Dates.addDays(today, Recurrence.HORIZON_DAYS))
    }

    /** An occurrence's date as the user sees it: its to-do date, else its slot. */
    private fun Task.day(): String? = toDoDate ?: occurrenceDate

    /** v2.13.0 — what a status change does to the occurrence's whole series. */
    private suspend fun applySeriesEffects(series: Series, chosen: Task, fx: StateMachine.Effects, today: String) {
        if (fx.freezeSeries) {
            // FR-REC-5: stop generating; the chosen occurrence and everything
            // before it stay, later occurrences still Open go — edited or not
            seriesDao.upsert(series.copy(frozen = true).toEntity())
            val cutoff = chosen.day() ?: today
            val prune = taskDao.occurrencesOf(series.id).map { it.toDomain() }.filter { o ->
                o.id != chosen.id && (o.day() ?: "") > cutoff && o.state == TaskState.OPEN
            }
            taskDao.deleteAll(prune.map { it.id })
        }

        if (fx.closeSeries) {
            // the series ends today (and is never both frozen and closed):
            // what isn't Done settles as Closed up to today and goes after it,
            // so a reopen can bring those later dates back
            seriesDao.upsert(series.copy(endDate = today, frozen = false).toEntity())
            val notDone = taskDao.occurrencesOf(series.id).map { it.toDomain() }
                .filter { it.state != TaskState.DONE }
            val (upToToday, later) = notDone.partition { (it.day() ?: today) <= today }
            taskDao.upsertAll(
                upToToday.filter { it.state != TaskState.CLOSED }
                    .map { it.copy(state = TaskState.CLOSED, stateChangedAt = now()).toEntity() },
            )
            taskDao.deleteAll(later.map { it.id })
        }

        if (fx.thawSeries) {
            // DM-TASK-2: the series thaws and fills in again from today
            resumeFromToday(series.copy(frozen = false), today)
        }

        if (fx.reopenSeries) {
            // a closed series reopens: it ends never again (the old end date
            // isn't remembered) and fills in from today; the past stays as is
            resumeFromToday(series.copy(endDate = null), today)
        }
    }
}
