package com.thefoxworks.tzafon.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.thefoxworks.tzafon.AppContainer
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.notify.NotifyLogic
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * FR-NOTIF-2 — AlarmManager delivery: exact when the user has allowed
 * exact alarms, the inexact-while-idle fallback otherwise. The daily
 * top-up (TopUpWorker) and the on-change refresh both land here.
 */
object NotifyScheduler {

    /** Recompute today's reminders and (re)set their alarms. */
    suspend fun topUp(context: Context, container: AppContainer) {
        if (!container.settings.remindersEnabled.first()) return
        if (!Notify.canPost(context)) return

        val today = Dates.todayIso()
        val now = LocalTime.now()
        val reminders = NotifyLogic.remindersFor(
            date = today,
            tasks = container.taskRepository.observeTasks().first(),
            habits = container.habitRepository.observeHabits().first(),
            logs = container.habitRepository.observeLogs().first(),
            afterMinutes = now.hour * 60 + now.minute,
        )
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        reminders.forEach { schedule(context, alarms, it) }
    }

    private fun schedule(context: Context, alarms: AlarmManager, r: NotifyLogic.Reminder) {
        val mins = NotifyLogic.minutesOf(r.time) ?: return
        val atMs = LocalDate.parse(r.date)
            .atTime(mins / 60, mins % 60)
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val pi = PendingIntent.getBroadcast(
            context,
            r.key.hashCode(),
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_KEY, r.key)
                putExtra(ReminderReceiver.EXTRA_TITLE, r.title)
                putExtra(ReminderReceiver.EXTRA_LINE, r.line)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // exact when permitted, gentle fallback otherwise (FR-NOTIF-2)
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }
}
