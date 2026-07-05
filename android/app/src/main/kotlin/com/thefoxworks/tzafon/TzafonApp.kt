package com.thefoxworks.tzafon

import android.app.Application
import com.thefoxworks.tzafon.data.db.TzafonDatabase
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.model.TaskRepository

/**
 * Manual DI (PLAN §3): one container on the Application. The repository
 * bindings here are the single swap-point for the deferred Firebase sync
 * layer (M9b) — nothing else in the app knows what backs a repository.
 */
class AppContainer(app: Application) {
    private val db by lazy { TzafonDatabase.build(app) }

    val taskRepository: TaskRepository by lazy { RoomTaskRepository(db.taskDao(), db.seriesDao()) }
    val settings by lazy { SettingsStore(app) }
}

class TzafonApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
