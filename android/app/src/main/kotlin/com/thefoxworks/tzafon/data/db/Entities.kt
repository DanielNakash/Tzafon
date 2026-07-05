package com.thefoxworks.tzafon.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule

/**
 * Room rows deliberately mirror the v1.1.0 Firestore document shapes
 * (users/{uid}/tasks + series) so the deferred sync layer can map 1:1.
 * List-ish columns use CSV of safe tokens (ISO dates, ids) — no JSON dep.
 */

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,               // UUID or "{seriesId}__{occurrenceDate}"
    val title: String,
    val description: String = "",
    val toDoDate: String? = null,
    val dueDate: String? = null,
    val state: String = "OPEN",
    val seriesId: String? = null,
    val occurrenceDate: String? = null,
    val overridden: Boolean = false,
    val cueType: String? = null,
    val cueLabel: String? = null,
    val cueTime: String? = null,
    val themeId: String? = null,
    val habitId: String? = null,
    val goalIds: String = "",                 // CSV
    val commitment: String? = null,
    val focusDate: String? = null,
    val focusWeekStart: String? = null,
    val sortOrder: Long = 0,
    val createdAt: Long = 0,
    val stateChangedAt: Long = 0,
    val completedAt: Long? = null,
)

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    // rule
    val pattern: String,
    @ColumnInfo(name = "intervalN") val interval: Int = 1,
    val unit: String = "WEEK",
    val weekdays: String = "",                // CSV of 0..6
    val weekInterval: Int = 1,
    val monthMode: String = "DATE",
    val monthDate: Int = 1,
    val monthWeekPos: Int = 1,
    val monthWeekday: Int = 0,
    val monthInterval: Int = 1,
    // series meta
    val ruleAnchor: String,
    val startDate: String,
    val endDate: String? = null,
    val dueMode: String = "NONE",
    val dueSingular: String? = null,
    // due rule (nullable clone of the rule fields)
    val duePattern: String? = null,
    val dueInterval: Int? = null,
    val dueUnit: String? = null,
    val dueWeekdays: String? = null,
    val dueWeekInterval: Int? = null,
    val dueMonthMode: String? = null,
    val dueMonthDate: Int? = null,
    val dueMonthWeekPos: Int? = null,
    val dueMonthWeekday: Int? = null,
    val dueMonthInterval: Int? = null,
    val exceptions: String = "",              // CSV of ISO dates
    val generatedThrough: String? = null,
    val frozen: Boolean = false,
    // template alignment for new occurrences
    val cueType: String? = null,
    val cueLabel: String? = null,
    val cueTime: String? = null,
    val themeId: String? = null,
    val habitId: String? = null,
    val goalIds: String = "",                 // CSV
    val createdAt: Long = 0,
)

// ── mapping ───────────────────────────────────────────────────

private fun csv(list: List<String>) = list.joinToString(",")
private fun unCsv(s: String?) = s?.takeIf { it.isNotBlank() }?.split(",") ?: emptyList()
private fun csvInts(list: List<Int>) = list.joinToString(",")
private fun unCsvInts(s: String?) = unCsv(s).map { it.toInt() }

private fun cueOf(type: String?, label: String?, time: String?): Cue? =
    if (type != null && label != null) Cue(CueType.valueOf(type), label, time) else null

fun TaskEntity.toDomain() = Task(
    id = id, title = title, description = description,
    toDoDate = toDoDate, dueDate = dueDate,
    state = TaskState.valueOf(state),
    seriesId = seriesId, occurrenceDate = occurrenceDate, overridden = overridden,
    cue = cueOf(cueType, cueLabel, cueTime),
    themeId = themeId, habitId = habitId, goalIds = unCsv(goalIds),
    commitment = commitment, focusDate = focusDate, focusWeekStart = focusWeekStart,
    sortOrder = sortOrder, createdAt = createdAt, stateChangedAt = stateChangedAt,
    completedAt = completedAt,
)

fun Task.toEntity() = TaskEntity(
    id = id, title = title, description = description,
    toDoDate = toDoDate, dueDate = dueDate,
    state = state.name,
    seriesId = seriesId, occurrenceDate = occurrenceDate, overridden = overridden,
    cueType = cue?.type?.name, cueLabel = cue?.label, cueTime = cue?.time,
    themeId = themeId, habitId = habitId, goalIds = csv(goalIds),
    commitment = commitment, focusDate = focusDate, focusWeekStart = focusWeekStart,
    sortOrder = sortOrder, createdAt = createdAt, stateChangedAt = stateChangedAt,
    completedAt = completedAt,
)

fun SeriesEntity.rule() = Rule(
    pattern = Pattern.valueOf(pattern),
    interval = interval,
    unit = IntervalUnit.valueOf(unit),
    weekdays = unCsvInts(weekdays),
    weekInterval = weekInterval,
    monthMode = MonthMode.valueOf(monthMode),
    monthDate = monthDate,
    monthWeekPos = monthWeekPos,
    monthWeekday = monthWeekday,
    monthInterval = monthInterval,
)

fun SeriesEntity.dueRuleOrNull(): Rule? = duePattern?.let {
    Rule(
        pattern = Pattern.valueOf(it),
        interval = dueInterval ?: 1,
        unit = IntervalUnit.valueOf(dueUnit ?: "WEEK"),
        weekdays = unCsvInts(dueWeekdays),
        weekInterval = dueWeekInterval ?: 1,
        monthMode = MonthMode.valueOf(dueMonthMode ?: "DATE"),
        monthDate = dueMonthDate ?: 1,
        monthWeekPos = dueMonthWeekPos ?: 1,
        monthWeekday = dueMonthWeekday ?: 0,
        monthInterval = dueMonthInterval ?: 1,
    )
}

fun SeriesEntity.toDomain() = Series(
    id = id, title = title, description = description,
    rule = rule(),
    ruleAnchor = ruleAnchor, startDate = startDate, endDate = endDate,
    dueMode = DueMode.valueOf(dueMode),
    dueRule = dueRuleOrNull(),
    dueSingular = dueSingular,
    exceptions = unCsv(exceptions),
    generatedThrough = generatedThrough,
    frozen = frozen,
    cue = cueOf(cueType, cueLabel, cueTime),
    themeId = themeId, habitId = habitId, goalIds = unCsv(goalIds),
    createdAt = createdAt,
)

fun Series.toEntity() = SeriesEntity(
    id = id, title = title, description = description,
    pattern = rule.pattern.name,
    interval = rule.interval,
    unit = rule.unit.name,
    weekdays = csvInts(rule.weekdays),
    weekInterval = rule.weekInterval,
    monthMode = rule.monthMode.name,
    monthDate = rule.monthDate,
    monthWeekPos = rule.monthWeekPos,
    monthWeekday = rule.monthWeekday,
    monthInterval = rule.monthInterval,
    ruleAnchor = ruleAnchor, startDate = startDate, endDate = endDate,
    dueMode = dueMode.name,
    dueSingular = dueSingular,
    duePattern = dueRule?.pattern?.name,
    dueInterval = dueRule?.interval,
    dueUnit = dueRule?.unit?.name,
    dueWeekdays = dueRule?.weekdays?.let { csvInts(it) },
    dueWeekInterval = dueRule?.weekInterval,
    dueMonthMode = dueRule?.monthMode?.name,
    dueMonthDate = dueRule?.monthDate,
    dueMonthWeekPos = dueRule?.monthWeekPos,
    dueMonthWeekday = dueRule?.monthWeekday,
    dueMonthInterval = dueRule?.monthInterval,
    exceptions = csv(exceptions),
    generatedThrough = generatedThrough,
    frozen = frozen,
    cueType = cue?.type?.name, cueLabel = cue?.label, cueTime = cue?.time,
    themeId = themeId, habitId = habitId, goalIds = csv(goalIds),
    createdAt = createdAt,
)
