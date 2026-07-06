package com.thefoxworks.tzafon.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.thefoxworks.tzafon.TzafonApp
import java.util.concurrent.TimeUnit

/**
 * FR-NOTIF-2 / TECH-3 — the periodic check: once a day (and after boot,
 * app start, or a data change) recompute today's cue reminders and set
 * their alarms.
 */
class TopUpWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TzafonApp ?: return Result.failure()
        NotifyScheduler.topUp(applicationContext, app.container)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "notify-topup"
        private const val ONCE = "notify-topup-now"

        /** Daily cadence, kept unique across process restarts. */
        fun ensureScheduled(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TopUpWorker>(24, TimeUnit.HOURS).build(),
            )
        }

        /** Immediate refresh — boot, app start, cue/task edits. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONCE,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<TopUpWorker>().build(),
            )
        }
    }
}
