package com.thefoxworks.tzafon

import android.app.Application
import com.thefoxworks.tzafon.data.db.TzafonDatabase
import com.thefoxworks.tzafon.data.repo.RoomGoalRepository
import com.thefoxworks.tzafon.data.repo.RoomHabitRepository
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.data.repo.RoomThemeRepository
import com.thefoxworks.tzafon.data.settings.SettingsStore
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
    }
}
