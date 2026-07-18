package com.thefoxworks.tzafon.data.transfer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.ReviewRepository
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.ThemeRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * DM-EXPORT-1 / FR-DATA-1 — the export half of the data-transfer feature.
 *
 * The exporter snapshots every repository's list surface (`Flow.first()`),
 * assembles the `ExportDocument` in the canonical `DM-EXPORT-1.1` shape with
 * deterministic ordering (`DM-EXPORT-1.6`), and pretty-prints it to a JSON
 * string. Nothing is written to disk here — the caller (the UI wired through
 * SAF) writes the bytes to the user-chosen URI (`FR-DATA-1.3`).
 */
class DataExporter(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val themeRepository: ThemeRepository,
    private val reviewRepository: ReviewRepository,
    private val appVersion: String,
    private val appVersionCode: Int,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    /** Assemble the domain-typed export snapshot. */
    suspend fun snapshot(): ExportDocument {
        val tasks = taskRepository.observeTasks().first()
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
        val series = taskRepository.observeSeries().first()
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
        val habits = habitRepository.observeHabits().first()
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
        val habitLogs = habitRepository.observeLogs().first()
            .sortedWith(compareBy({ it.habitId }, { it.date }))
        val goals = goalRepository.observeGoals().first()
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
        val themes = themeRepository.observeThemes().first()
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
        val reviews = reviewRepository.observeReviews().first()
            .sortedBy { it.periodStart }
        val contributions = goalRepository.observeContributions().first()
            .sortedWith(compareBy({ it.createdAt }, { it.id }))

        val exportedAt = now()
        return ExportDocument(
            manifest = ExportManifest(
                format = EXPORT_FORMAT,
                formatVersion = EXPORT_FORMAT_VERSION,
                appVersion = appVersion,
                appVersionCode = appVersionCode,
                exportedAt = exportedAt,
                exportedAtIso = ISO_OFFSET.format(Instant.ofEpochMilli(exportedAt).atZone(zone)),
                counts = ExportCounts(
                    tasks = tasks.size,
                    series = series.size,
                    habits = habits.size,
                    habitLogs = habitLogs.size,
                    goals = goals.size,
                    themes = themes.size,
                    reviews = reviews.size,
                    contributions = contributions.size,
                ),
            ),
            collections = ExportCollections(
                tasks = tasks,
                series = series,
                habits = habits,
                habitLogs = habitLogs,
                goals = goals,
                themes = themes,
                reviews = reviews,
                contributions = contributions,
            ),
        )
    }

    /** Snapshot + pretty-print, in one call. Bytes are UTF-8. */
    suspend fun toJson(): String = pretty.toJson(snapshot())

    companion object {
        /** DM-EXPORT-1.6 — pretty-printed, deterministic. */
        val pretty: Gson = GsonBuilder().setPrettyPrinting().create()

        /** Suggested SAF filename (`FR-DATA-1.3`). */
        fun suggestedFilename(today: String): String = "tzafon-backup-$today.json"

        private val ISO_OFFSET: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
