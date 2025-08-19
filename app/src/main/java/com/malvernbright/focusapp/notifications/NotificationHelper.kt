package com.malvernbright.focusapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.malvernbright.focusapp.R

object NotificationHelper {
    const val CHANNEL_SESSION = "session_notifications"
    const val CHANNEL_TASK = "task_notifications"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val session = NotificationChannel(
                CHANNEL_SESSION,
                "Focus sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts at the end of work/break sessions"
                enableVibration(true)
            }
            val task = NotificationChannel(
                CHANNEL_TASK,
                "Task alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when tasks complete"
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(session)
            nm.createNotificationChannel(task)
        }
    }

    private fun canNotify(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun notifySessionEnd(context: Context, title: String, message: String, id: Int = 1001) {
        if (!canNotify(context)) return
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_SESSION)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // ignore
        }
    }

    fun notifyTaskComplete(context: Context, title: String, message: String, id: Int = 2001) {
        if (!canNotify(context)) return
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_TASK)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // ignore
        }
    }
}
