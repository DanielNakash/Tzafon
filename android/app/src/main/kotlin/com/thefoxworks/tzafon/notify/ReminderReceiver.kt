package com.thefoxworks.tzafon.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thefoxworks.tzafon.TzafonApp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.TaskState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires at the cue's moment. Re-checks state before posting so a task
 * completed after scheduling (or a habit logged meanwhile) stays silent —
 * cancel-by-recheck instead of alarm bookkeeping.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val line = intent.getStringExtra(EXTRA_LINE) ?: ""
        val app = context.applicationContext as? TzafonApp ?: return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (stillWanted(app, key)) {
                    Notify.postReminder(context, key, title, line)
                }
            } finally {
                result.finish()
            }
        }
    }

    private suspend fun stillWanted(app: TzafonApp, key: String): Boolean {
        val c = app.container
        if (!c.settings.remindersEnabled.first()) return false
        val today = Dates.todayIso()
        return when {
            key.startsWith("task-") -> {
                val id = key.removePrefix("task-").removeSuffix("-$today")
                c.taskRepository.getTask(id)?.state == TaskState.OPEN
            }
            key.startsWith("habit-") -> {
                val id = key.removePrefix("habit-").removeSuffix("-$today")
                if (c.habitRepository.getHabit(id) == null) return false
                c.habitRepository.observeLogs().first()
                    .none { it.habitId == id && it.date == today && it.done }
            }
            else -> false
        }
    }

    companion object {
        const val EXTRA_KEY = "key"
        const val EXTRA_TITLE = "title"
        const val EXTRA_LINE = "line"
    }
}
