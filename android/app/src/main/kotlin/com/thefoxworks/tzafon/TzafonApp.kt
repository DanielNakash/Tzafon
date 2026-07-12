package com.thefoxworks.tzafon

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.thefoxworks.tzafon.data.audio.ChimePlayer
import com.thefoxworks.tzafon.data.auth.FirebaseAuthRepository
import com.thefoxworks.tzafon.data.db.ContributionEntity
import com.thefoxworks.tzafon.data.db.GoalEntity
import com.thefoxworks.tzafon.data.db.HabitEntity
import com.thefoxworks.tzafon.data.db.HabitLogEntity
import com.thefoxworks.tzafon.data.db.ReviewEntity
import com.thefoxworks.tzafon.data.db.SeriesEntity
import com.thefoxworks.tzafon.data.db.TaskEntity
import com.thefoxworks.tzafon.data.db.ThemeEntity
import com.thefoxworks.tzafon.data.db.TzafonDatabase
import com.thefoxworks.tzafon.data.repo.RoomGoalRepository
import com.thefoxworks.tzafon.data.repo.RoomHabitRepository
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.data.repo.RoomThemeRepository
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.data.sync.FirestoreSync
import com.thefoxworks.tzafon.data.sync.SyncSpec
import com.thefoxworks.tzafon.domain.model.AuthRepository
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.ThemeRepository
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.notify.Notify
import com.thefoxworks.tzafon.notify.TopUpWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Manual DI (PLAN §3): one container on the Application. The repository
 * bindings here are the single swap-point for the deferred Firebase sync
 * layer (M9b) — nothing else in the app knows what backs a repository.
 */
class AppContainer(app: Application) {
    private val db by lazy { TzafonDatabase.build(app) }

    val habitRepository: HabitRepository by lazy { RoomHabitRepository(db.habitDao()) }
    val goalRepository: GoalRepository by lazy { RoomGoalRepository(db.goalDao()) }
    val themeRepository: ThemeRepository by lazy { RoomThemeRepository(db.themeDao()) }
    val reviewRepository: com.thefoxworks.tzafon.domain.model.ReviewRepository by lazy {
        com.thefoxworks.tzafon.data.repo.RoomReviewRepository(db.reviewDao())
    }
    val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(db.taskDao(), db.seriesDao(), habitRepository, goalRepository)
    }
    val settings by lazy { SettingsStore(app) }

    /** FR-AUDIO-1 — application-scoped chime player; single SoundPool. */
    val chimePlayer: ChimePlayer by lazy { ChimePlayer(app) }

    // ── M9b: Firebase auth + offline-first Firestore sync ──
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(app) }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * The sync mirror writes raw rows straight through the DAOs (bypassing the
     * repositories' business logic on purpose — a mirror must not re-run
     * attribution/state effects). Room stays the UI's source of truth.
     */
    val syncManager by lazy {
        val taskDao = db.taskDao(); val seriesDao = db.seriesDao()
        val habitDao = db.habitDao(); val goalDao = db.goalDao()
        val themeDao = db.themeDao(); val reviewDao = db.reviewDao()
        FirestoreSync(
            firestore,
            listOf(
                SyncSpec("tasks", TaskEntity::class.java, { taskDao.observeAll() }, { it.id },
                    { taskDao.upsert(it) }, { taskDao.delete(it) }),
                SyncSpec("series", SeriesEntity::class.java, { seriesDao.observeAll() }, { it.id },
                    { seriesDao.upsert(it) }, { seriesDao.delete(it) }),
                SyncSpec("habits", HabitEntity::class.java, { habitDao.observeAll() }, { it.id },
                    { habitDao.upsert(it) }, { habitDao.delete(it) }),
                SyncSpec("habitLogs", HabitLogEntity::class.java, { habitDao.observeLogs() },
                    { "${it.habitId}::${it.date}" }, { habitDao.upsertLog(it) },
                    { id -> id.split("::", limit = 2).let { habitDao.deleteLog(it[0], it[1]) } }),
                SyncSpec("goals", GoalEntity::class.java, { goalDao.observeAll() }, { it.id },
                    { goalDao.upsert(it) }, { goalDao.delete(it) }),
                SyncSpec("contributions", ContributionEntity::class.java, { goalDao.observeContributions() },
                    { it.id }, { goalDao.insertContribution(it) }, { goalDao.deleteContribution(it) }),
                SyncSpec("themes", ThemeEntity::class.java, { themeDao.observeAll() }, { it.id },
                    { themeDao.upsert(it) }, { themeDao.delete(it) }),
                SyncSpec("reviews", ReviewEntity::class.java, { reviewDao.observeAll() }, { it.id },
                    { reviewDao.upsert(it) }, { reviewDao.delete(it) }),
            ),
        )
    }

    /**
     * FR-REC-3 / NFR-PERF-2 — the session's effective generation horizon in
     * days. Planning raises it when the user looks further ahead; All Tasks
     * reads it for the horizon marker. Deliberately not persisted.
     */
    val sessionHorizonDays = MutableStateFlow(Recurrence.HORIZON_DAYS)
}

class TzafonApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // FR-NOTIF-2 — channel + the daily top-up + a start-of-process pass
        Notify.ensureChannel(this)
        TopUpWorker.ensureScheduled(this)
        TopUpWorker.runNow(this)

        // cue/task/habit edits re-set today's alarms without waiting a day
        appScope.launch {
            combine(
                container.taskRepository.observeTasks(),
                container.habitRepository.observeHabits(),
                container.habitRepository.observeLogs(),
                container.settings.remindersEnabled,
            ) { _, _, _, _ -> Unit }
                .debounce(2000)
                .collect { TopUpWorker.runNow(this@TzafonApp) }
        }

        // M9b: sync runs only while signed in (offline-first — local works either way)
        appScope.launch {
            container.authRepository.authState.collect { user ->
                if (user != null) container.syncManager.start(user.uid)
                else container.syncManager.stop()
            }
        }
    }
}
