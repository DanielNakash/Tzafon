package com.thefoxworks.tzafon

import android.app.Application
import com.thefoxworks.tzafon.data.db.TzafonDatabase
import com.thefoxworks.tzafon.data.repo.RoomGoalRepository
import com.thefoxworks.tzafon.data.repo.RoomHabitRepository
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Manual DI (PLAN §3): one container on the Application. The repository
 * bindings here are the single swap-point for the deferred Firebase sync
 * layer (M9b) — nothing else in the app knows what backs a repository.
 */
class AppContainer(app: Application) {
    private val db by lazy { TzafonDatabase.build(app) }

    val habitRepository: HabitRepository by lazy { RoomHabitRepository(db.habitDao()) }
    val goalRepository: GoalRepository by lazy { RoomGoalRepository(db.goalDao()) }
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

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
