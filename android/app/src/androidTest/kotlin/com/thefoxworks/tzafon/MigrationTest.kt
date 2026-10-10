package com.thefoxworks.tzafon

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thefoxworks.tzafon.data.db.TzafonDatabase
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NFR-DATA-1 — a database written by an earlier version migrates without
 * losing rows. The old database is built from Room's exported schema
 * (`app/schemas/…/<version>.json`, packaged as androidTest assets), then opened
 * through Room with the app's own [TzafonDatabase.MIGRATIONS]; Room validates
 * the migrated tables against the current entities on open.
 *
 * This is what `room-testing`'s MigrationTestHelper does, by hand: that
 * artifact needs a newer kotlinx-serialization than the app's runtime
 * classpath pins (via Firebase), and AGP holds the test classpath to the app's.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun deleteDb() {
        context.deleteDatabase(dbName)
    }

    /** Create [dbName] exactly as schema [version] left it. */
    private fun createDatabase(version: Int): SQLiteDatabase {
        val json = InstrumentationRegistry.getInstrumentation().context.assets
            .open("${TzafonDatabase::class.java.name}/$version.json")
            .bufferedReader().use { it.readText() }
        val schema = JSONObject(json).getJSONObject("database")
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName), null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = version
        return db
    }

    private fun openWithMigrations(): TzafonDatabase =
        Room.databaseBuilder(context, TzafonDatabase::class.java, dbName)
            .addMigrations(*TzafonDatabase.MIGRATIONS)
            .build()

    @Test
    fun migrate5To6_keepsAnOldDeadlineRuleSeries_andAddsAnEmptyOffset() = runBlocking {
        createDatabase(5).use { db ->
            db.execSQL(
                """INSERT INTO series (id, title, description, pattern, intervalN, unit, weekdays,
                    weekInterval, monthMode, monthDate, monthWeekPos, monthWeekday, monthInterval,
                    ruleAnchor, startDate, dueMode, duePattern, dueInterval, dueUnit, dueWeekdays,
                    exceptions, frozen, goalIds, createdAt)
                   VALUES ('s1', 'Clean desk', '', 'WEEKDAY', 1, 'WEEK', '1',
                    1, 'DATE', 1, 1, 0, 1,
                    '2026-06-01', '2026-06-01', 'RECURRING', 'WEEKDAY', 1, 'WEEK', '5',
                    '', 0, '', 0)""",
            )
        }

        val room = openWithMigrations()
        try {
            val old = room.seriesDao().get("s1")!!.toDomain()
            assertEquals("Clean desk", old.title)
            assertEquals(DueMode.RECURRING, old.dueMode)
            assertEquals(listOf(5), old.dueRule?.weekdays)
            assertNull(old.dueOffsetDays)

            room.seriesDao().upsert(old.copy(dueMode = DueMode.OFFSET, dueRule = null, dueOffsetDays = 3).toEntity())
            val switched = room.seriesDao().get("s1")!!.toDomain()
            assertEquals(DueMode.OFFSET, switched.dueMode)
            assertEquals(3, switched.dueOffsetDays)
        } finally {
            room.close()
        }
    }
}
