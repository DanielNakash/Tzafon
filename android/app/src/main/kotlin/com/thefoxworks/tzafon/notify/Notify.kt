package com.thefoxworks.tzafon.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thefoxworks.tzafon.MainActivity
import com.thefoxworks.tzafon.R

/**
 * FR-NOTIF-2 — one quiet channel for cue reminders. Never engagement
 * bait (PRIN-9): a reminder is the cue the user wrote, nothing else.
 */
object Notify {
    const val CHANNEL_CUES = "cues"

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CUES,
                "Cue reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Fires on the triggers you set — \"when X, I will do Y\"."
            },
        )
    }

    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun postReminder(context: Context, key: String, title: String, line: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_CUES)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(line)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(key.hashCode(), notif)
    }
}
