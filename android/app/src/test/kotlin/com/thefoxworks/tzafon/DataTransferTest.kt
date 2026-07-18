package com.thefoxworks.tzafon

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thefoxworks.tzafon.data.transfer.ConflictPolicy
import com.thefoxworks.tzafon.data.transfer.DataImporter
import com.thefoxworks.tzafon.data.transfer.DataTransferRepository
import com.thefoxworks.tzafon.data.transfer.EXPORT_FORMAT
import com.thefoxworks.tzafon.data.transfer.EXPORT_FORMAT_VERSION
import com.thefoxworks.tzafon.data.transfer.ExportCollections
import com.thefoxworks.tzafon.data.transfer.ExportCounts
import com.thefoxworks.tzafon.data.transfer.ExportDocument
import com.thefoxworks.tzafon.data.transfer.ExportManifest
import com.thefoxworks.tzafon.data.transfer.ImportStats
import com.thefoxworks.tzafon.domain.model.ArchivedOutcome
import com.thefoxworks.tzafon.domain.model.Contribution
import com.thefoxworks.tzafon.domain.model.ContributionVia
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalStep
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.LogSource
import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.ReviewKind
import com.thefoxworks.tzafon.domain.model.ReviewStatus
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DM-EXPORT-1 / FR-DATA-1 / FR-DATA-2 — the export document round-trips,
 * validates safely, and the two conflict policies behave as specified.
 *
 * The Room DAOs are exercised via instrumented tests; here we test the pure
 * pieces — serialization, validation, and the conflict-policy dispatch — with
 * a fake [DataTransferRepository] that mirrors the Room contract.
 */
class DataTransferTest {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // ── a representative spread across all eight collections ──
    private fun fixture(): ExportDocument {
        val rule = Rule(
            pattern = Pattern.WEEKDAY,
            interval = 1,
            unit = IntervalUnit.WEEK,
            weekdays = listOf(1, 3, 5),
            weekInterval = 1,
            monthMode = MonthMode.DATE,
            monthDate = 1,
            monthWeekPos = 1,
            monthWeekday = 0,
            monthInterval = 1,
        )
        val series = Series(
            id = "s1",
            title = "Clean desk",
            description = "",
            rule = rule,
            ruleAnchor = "2026-07-06",
            startDate = "2026-07-06",
            endDate = null,
            dueMode = DueMode.NONE,
            dueRule = null,
            dueSingular = null,
            exceptions = emptyList(),
            generatedThrough = "2026-08-05",
            frozen = false,
            cue = Cue(CueType.AT_TIME, "8:30", "08:30"),
            themeId = "t1",
            habitId = null,
            goalIds = listOf("g1"),
            createdAt = 1000L,
        )
        val standalone = Task(
            id = "task1",
            title = "עברית", // RTL sanity
            description = "note",
            toDoDate = "2026-07-18",
            state = TaskState.OPEN,
            goalIds = listOf("g1", "g2"),
            createdAt = 100L,
        )
        val occurrence = Task(
            id = "s1__2026-07-06",
            title = "Clean desk",
            toDoDate = "2026-07-06",
            state = TaskState.DONE,
            seriesId = "s1",
            occurrenceDate = "2026-07-06",
            createdAt = 1001L,
            stateChangedAt = 1002L,
            completedAt = 1002L,
        )
        val habit = Habit(
            id = "h1",
            name = "Push-ups",
            kind = HabitKind.QUANTITATIVE,
            target = 30.0,
            unit = "reps",
            targetDays = 5,
            cue = Cue(CueType.AFTER_ROUTINE, "After coffee"),
            primaryThemeId = "t1",
            themeIds = listOf("t1"),
            goalId = null,
            startedAt = 500L,
            createdAt = 500L,
        )
        val log = HabitLog(
            habitId = "h1", date = "2026-07-15",
            done = true, amount = 20.0,
            source = LogSource.DIRECT,
        )
        val goal = Goal(
            id = "g1", title = "Marathon", description = "",
            type = GoalType.STEPPED,
            steps = listOf(GoalStep("Sign up", true), GoalStep("Train", false)),
            primaryThemeId = "t1", themeIds = listOf("t1"),
            lastActivityAt = 2000L, createdAt = 200L,
        )
        val theme = Theme(
            id = "t1", name = "Better health", why = "For long life",
            windowStart = "2026-06-01", windowEnd = "2026-09-01",
            state = ThemeState.ACTIVE, accentSlot = 1, createdAt = 50L,
        )
        val archivedTheme = Theme(
            id = "t0", name = "Old direction", why = "For a stretch",
            windowStart = "2026-01-01", windowEnd = "2026-06-01",
            state = ThemeState.ARCHIVED, archivedOutcome = ArchivedOutcome.EVOLVED,
            renewedToThemeId = "t1", accentSlot = 0, createdAt = 10L, archivedAt = 6000L,
        )
        val review = Review(
            id = "2026-07-06", kind = ReviewKind.WEEKLY,
            periodStart = "2026-07-06", periodEnd = "2026-07-12",
            status = ReviewStatus.DONE,
            reflectNote = "went well", snapshot = "3/5 aligned",
            completedAt = 5000L,
        )
        val contribution = Contribution(
            id = "c1", taskId = "task1", goalId = "g1",
            amount = 1.0, via = ContributionVia.DIRECT, createdAt = 3000L,
        )

        val tasks = listOf(standalone, occurrence)
        val serieses = listOf(series)
        val habits = listOf(habit)
        val logs = listOf(log)
        val goals = listOf(goal)
        val themes = listOf(theme, archivedTheme)
        val reviews = listOf(review)
        val contributions = listOf(contribution)

        return ExportDocument(
            manifest = ExportManifest(
                format = EXPORT_FORMAT,
                formatVersion = EXPORT_FORMAT_VERSION,
                appVersion = "2.8.0",
                appVersionCode = 9,
                exportedAt = 1_752_790_000_000L,
                exportedAtIso = "2026-07-18T00:26:40+03:00",
                counts = ExportCounts(
                    tasks = tasks.size, series = serieses.size,
                    habits = habits.size, habitLogs = logs.size,
                    goals = goals.size, themes = themes.size,
                    reviews = reviews.size, contributions = contributions.size,
                ),
            ),
            collections = ExportCollections(
                tasks = tasks, series = serieses,
                habits = habits, habitLogs = logs,
                goals = goals, themes = themes,
                reviews = reviews, contributions = contributions,
            ),
        )
    }

    // ── DM-EXPORT-1: round-trip + byte-identical re-serialize ──

    @Test
    fun `round-trip preserves every collection deeply`() {
        val doc = fixture()
        val json = gson.toJson(doc)
        val back = gson.fromJson(json, ExportDocument::class.java)
        assertEquals(doc, back)
    }

    @Test
    fun `re-serialize is byte-identical`() {
        val doc = fixture()
        val a = gson.toJson(doc)
        val b = gson.toJson(gson.fromJson(a, ExportDocument::class.java))
        assertEquals(a, b)
    }

    @Test
    fun `the file is human-readable JSON, not CSV or control chars`() {
        val json = gson.toJson(fixture())
        // goalIds must be an array, not CSV; steps must be array-of-object.
        assertTrue("goalIds should be JSON array", json.contains("\"goalIds\": ["))
        assertTrue("steps should be array of objects", json.contains("\"steps\": ["))
        assertTrue("recurrence Rule should be a nested object", json.contains("\"rule\": {"))
        // No ␞ (U+241E) / ␟ (U+241F) control chars anywhere in the file.
        assertFalse(json.contains('␞'))
        assertFalse(json.contains('␟'))
        // Enums serialized by name.
        assertTrue(json.contains("\"OPEN\""))
        assertTrue(json.contains("\"QUANTITATIVE\""))
        // Long timestamp field written as integer, not scientific double.
        assertTrue(json.contains("\"createdAt\": 200"))
    }

    // ── FR-DATA-2.3/2.6 validation ──

    @Test
    fun `rejects a non-JSON blob`() {
        val transfer = FakeTransfer()
        val importer = DataImporter(transfer)
        val result = importer.parse("not json at all")
        assertTrue(result is DataImporter.ParseResult.Rejected.Malformed)
    }

    @Test
    fun `rejects JSON that is missing manifest`() {
        val transfer = FakeTransfer()
        val importer = DataImporter(transfer)
        val result = importer.parse("""{"collections":{}}""")
        assertTrue(result is DataImporter.ParseResult.Rejected.Malformed)
    }

    @Test
    fun `rejects a foreign format discriminator`() {
        val transfer = FakeTransfer()
        val importer = DataImporter(transfer)
        val bad = """{"manifest":{"format":"not.tzafon","formatVersion":1,"appVersion":"x","appVersionCode":1,"exportedAt":0,"exportedAtIso":"","counts":{"tasks":0,"series":0,"habits":0,"habitLogs":0,"goals":0,"themes":0,"reviews":0,"contributions":0}},"collections":{"tasks":[],"series":[],"habits":[],"habitLogs":[],"goals":[],"themes":[],"reviews":[],"contributions":[]}}"""
        val result = importer.parse(bad)
        assertTrue(result is DataImporter.ParseResult.Rejected.WrongFormat)
    }

    @Test
    fun `rejects a newer formatVersion`() {
        val transfer = FakeTransfer()
        val importer = DataImporter(transfer, supportedFormatVersion = 1)
        val newer = """{"manifest":{"format":"tzafon.export","formatVersion":2,"appVersion":"x","appVersionCode":1,"exportedAt":0,"exportedAtIso":"","counts":{"tasks":0,"series":0,"habits":0,"habitLogs":0,"goals":0,"themes":0,"reviews":0,"contributions":0}},"collections":{"tasks":[],"series":[],"habits":[],"habitLogs":[],"goals":[],"themes":[],"reviews":[],"contributions":[]}}"""
        val result = importer.parse(newer)
        assertTrue(result is DataImporter.ParseResult.Rejected.NewerVersion)
    }

    // ── FR-DATA-2.4 count mismatch is a warning, not a hard-block ──

    @Test
    fun `count mismatch surfaces but does not reject`() {
        val transfer = FakeTransfer()
        val importer = DataImporter(transfer)
        val doc = fixture()
        // manifest says 2 tasks, but drop one from the array
        val truncated = doc.copy(
            collections = doc.collections.copy(tasks = doc.collections.tasks.drop(1)),
        )
        val json = gson.toJson(truncated)
        val result = importer.parse(json)
        assertTrue(result is DataImporter.ParseResult.Ok)
        assertTrue((result as DataImporter.ParseResult.Ok).countMismatch)
    }

    // ── FR-DATA-2.7 / FR-DATA-2.8 conflict policies ──

    @Test
    fun `backup wins overwrites existing rows`() {
        val transfer = FakeTransfer()
        transfer.tasks["task1"] = Task(id = "task1", title = "local edit").also {}
        val importer = DataImporter(transfer)
        val stats = runBlocking { importer.apply(fixture(), ConflictPolicy.BACKUP_WINS) }
        assertEquals("task1 was overwritten", "עברית", transfer.tasks["task1"]?.title)
        assertTrue("some rows were replaced", stats.replaced >= 1)
    }

    @Test
    fun `current wins skips existing rows`() {
        val transfer = FakeTransfer()
        transfer.tasks["task1"] = Task(id = "task1", title = "local edit")
        val importer = DataImporter(transfer)
        val stats = runBlocking { importer.apply(fixture(), ConflictPolicy.CURRENT_WINS) }
        assertEquals("task1 kept its local value", "local edit", transfer.tasks["task1"]?.title)
        assertTrue("some rows were skipped", stats.skipped >= 1)
        // the occurrence (only in backup) still lands
        assertNotNull(transfer.tasks["s1__2026-07-06"])
    }

    @Test
    fun `isEmpty is honored - clean restore reports no conflict`() {
        val transfer = FakeTransfer()
        assertTrue(runBlocking { transfer.isEmpty() })
        val importer = DataImporter(transfer)
        assertTrue(runBlocking { importer.isStoreEmpty() })
    }

    // ── FR-DATA-2.10/2.11 idempotency: re-importing the same file is a no-op ──

    @Test
    fun `re-import under current-wins is a no-op`() {
        val transfer = FakeTransfer()
        val importer = DataImporter(transfer)
        runBlocking { importer.apply(fixture(), ConflictPolicy.BACKUP_WINS) }
        val second = runBlocking { importer.apply(fixture(), ConflictPolicy.CURRENT_WINS) }
        assertEquals(0, second.inserted)
    }

    @Test
    fun `re-import under backup-wins overwrites with identical values`() {
        val transfer = FakeTransfer()
        val importer = DataImporter(transfer)
        val before = fixture()
        runBlocking { importer.apply(before, ConflictPolicy.BACKUP_WINS) }
        val snapshotAfterFirst = transfer.snapshotTasks()
        runBlocking { importer.apply(before, ConflictPolicy.BACKUP_WINS) }
        assertEquals(snapshotAfterFirst, transfer.snapshotTasks())
    }

    // ── FR-DATA-2.10 series+occurrence are kept related ──

    @Test
    fun `a materialized occurrence keeps its seriesId after round-trip`() {
        val json = gson.toJson(fixture())
        val back = gson.fromJson(json, ExportDocument::class.java)
        val occ = back.collections.tasks.first { it.id == "s1__2026-07-06" }
        assertEquals("s1", occ.seriesId)
        assertNotNull(back.collections.series.firstOrNull { it.id == "s1" })
    }

    // ── FR-DATA-2.7 atomicity: an injected failure rolls back the whole apply ──

    @Test
    fun `injected mid-apply failure rolls back the whole import`() {
        val transfer = FakeTransfer(failOn = "themes")
        val importer = DataImporter(transfer)
        val before = transfer.snapshotAll()
        try {
            runBlocking { importer.apply(fixture(), ConflictPolicy.BACKUP_WINS) }
        } catch (_: RuntimeException) {
            /* expected */
        }
        // the fake rolls back on failure, mirroring Room's transaction contract
        assertEquals(before, transfer.snapshotAll())
    }

    // ─────────────────────────────────────────────────────
    // A test-double for DataTransferRepository. Mirrors the Room contract
    // (all-or-nothing apply; upsert on BACKUP_WINS; insert-if-absent on
    // CURRENT_WINS) without needing an Android context.
    // ─────────────────────────────────────────────────────
    private class FakeTransfer(
        /** Optional collection name to throw on, testing atomicity rollback. */
        private val failOn: String? = null,
    ) : DataTransferRepository {
        val tasks = LinkedHashMap<String, Task>()
        val series = LinkedHashMap<String, Series>()
        val habits = LinkedHashMap<String, Habit>()
        val habitLogs = LinkedHashMap<Pair<String, String>, HabitLog>()
        val goals = LinkedHashMap<String, Goal>()
        val themes = LinkedHashMap<String, Theme>()
        val reviews = LinkedHashMap<String, Review>()
        val contributions = LinkedHashMap<String, Contribution>()

        override suspend fun isEmpty(): Boolean =
            tasks.isEmpty() && series.isEmpty() && habits.isEmpty() &&
                habitLogs.isEmpty() && goals.isEmpty() && themes.isEmpty() &&
                reviews.isEmpty() && contributions.isEmpty()

        override suspend fun importAll(
            document: ExportDocument,
            policy: ConflictPolicy,
        ): ImportStats {
            val snap = snapshotAll()
            var inserted = 0; var replaced = 0; var skipped = 0
            fun <T, K> writeSet(items: List<T>, dest: MutableMap<K, T>, key: (T) -> K, name: String) {
                for (row in items) {
                    val k = key(row)
                    val existed = dest.containsKey(k)
                    if (existed && policy == ConflictPolicy.CURRENT_WINS) { skipped++; continue }
                    dest[k] = row; if (existed) replaced++ else inserted++
                }
                if (failOn == name) throw RuntimeException("injected on $name")
            }
            try {
                writeSet(document.collections.tasks, tasks, { it.id }, "tasks")
                writeSet(document.collections.series, series, { it.id }, "series")
                writeSet(document.collections.habits, habits, { it.id }, "habits")
                writeSet(document.collections.habitLogs, habitLogs, { it.habitId to it.date }, "habitLogs")
                writeSet(document.collections.goals, goals, { it.id }, "goals")
                writeSet(document.collections.themes, themes, { it.id }, "themes")
                writeSet(document.collections.reviews, reviews, { it.id }, "reviews")
                writeSet(document.collections.contributions, contributions, { it.id }, "contributions")
            } catch (e: Throwable) {
                restore(snap)
                throw e
            }
            return ImportStats(inserted = inserted, replaced = replaced, skipped = skipped)
        }

        fun snapshotTasks(): Map<String, Task> = LinkedHashMap(tasks)

        data class Snap(
            val tasks: Map<String, Task>, val series: Map<String, Series>,
            val habits: Map<String, Habit>, val logs: Map<Pair<String, String>, HabitLog>,
            val goals: Map<String, Goal>, val themes: Map<String, Theme>,
            val reviews: Map<String, Review>, val contributions: Map<String, Contribution>,
        )

        fun snapshotAll() = Snap(
            LinkedHashMap(tasks), LinkedHashMap(series),
            LinkedHashMap(habits), LinkedHashMap(habitLogs),
            LinkedHashMap(goals), LinkedHashMap(themes),
            LinkedHashMap(reviews), LinkedHashMap(contributions),
        )

        private fun restore(s: Snap) {
            tasks.clear(); tasks.putAll(s.tasks)
            series.clear(); series.putAll(s.series)
            habits.clear(); habits.putAll(s.habits)
            habitLogs.clear(); habitLogs.putAll(s.logs)
            goals.clear(); goals.putAll(s.goals)
            themes.clear(); themes.putAll(s.themes)
            reviews.clear(); reviews.putAll(s.reviews)
            contributions.clear(); contributions.putAll(s.contributions)
        }

    }
}
