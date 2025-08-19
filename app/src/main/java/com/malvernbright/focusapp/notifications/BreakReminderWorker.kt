package com.malvernbright.focusapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BreakReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        NotificationHelper.notifySessionEnd(
            applicationContext,
            title = "Break reminder",
            message = "Time to take a short break"
        )
        Result.success()
    }
}

