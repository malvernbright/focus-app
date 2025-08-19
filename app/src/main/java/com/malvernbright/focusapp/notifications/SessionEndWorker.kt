package com.malvernbright.focusapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.malvernbright.focusapp.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionEndWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val type = inputData.getString(KEY_TYPE) ?: return@withContext Result.failure()
        val rawTaskId = inputData.getLong(KEY_TASK_ID, -1L)
        val taskId = rawTaskId.takeIf { it > 0 }
        val expectedMinutes = inputData.getInt(KEY_EXPECTED_MINUTES, 0)
        val startTime = inputData.getLong(KEY_START_TIME, System.currentTimeMillis())
        val title = inputData.getString(KEY_TITLE)?.takeIf { it.isNotBlank() } ?: if (type == TYPE_WORK) "Work session complete" else "Break complete"
        val message = inputData.getString(KEY_MESSAGE)?.takeIf { it.isNotBlank() } ?: "Time's up!"

        NotificationHelper.notifySessionEnd(applicationContext, title, message)

        // Log session
        val endTime = System.currentTimeMillis()
        val actualMinutes = ((endTime - startTime) / 60000L).toInt().coerceAtLeast(expectedMinutes)
        val repo = Repository.get(applicationContext)
        repo.logSession(
            taskId = taskId,
            type = type,
            startTime = startTime,
            endTime = endTime,
            expectedMinutes = expectedMinutes,
            actualMinutes = actualMinutes
        )

        // If a work session is tied to a task, check completion and notify
        if (type == TYPE_WORK && taskId != null) {
            val task = repo.getTask(taskId)
            if (task != null) {
                if (!task.isCompleted && task.actualMinutes >= task.expectedMinutes) {
                    val now = System.currentTimeMillis()
                    repo.upsertTask(
                        id = task.id,
                        title = task.title,
                        expectedMinutes = task.expectedMinutes,
                        projectId = task.projectId,
                        alarmOnCompletion = task.alarmOnCompletion,
                        actualMinutes = task.actualMinutes,
                        isCompleted = true,
                        description = task.description,
                        completedAt = now
                    )
                    if (task.alarmOnCompletion) {
                        NotificationHelper.notifyTaskComplete(
                            applicationContext,
                            title = "Task complete",
                            message = "${task.title} finished"
                        )
                    }
                }
            }
        }
        Result.success()
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_TASK_ID = "taskId"
        const val KEY_EXPECTED_MINUTES = "expectedMinutes"
        const val KEY_START_TIME = "startTime"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"

        const val TYPE_WORK = "WORK"
        const val TYPE_BREAK = "BREAK"

        fun buildInput(
            type: String,
            taskId: Long?,
            expectedMinutes: Int,
            startTime: Long,
            title: String? = null,
            message: String? = null
        ) = workDataOf(
            KEY_TYPE to type,
            KEY_TASK_ID to (taskId ?: -1L),
            KEY_EXPECTED_MINUTES to expectedMinutes,
            KEY_START_TIME to startTime,
            KEY_TITLE to (title ?: ""),
            KEY_MESSAGE to (message ?: "")
        )
    }
}
