package com.malvernbright.focusapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class Repository private constructor(private val db: AppDatabase) {
    val projects: Flow<List<ProjectEntity>> = db.projectDao().observeProjects()
    val tasks: Flow<List<TaskEntity>> = db.taskDao().observeTasks()

    suspend fun upsertProject(
        name: String,
        expectedMinutes: Int = 0,
        description: String? = null,
        isCompleted: Boolean = false,
        completedAt: Long? = null,
        id: Long? = null
    ): Long {
        val entity = ProjectEntity(
            id = id ?: 0,
            name = name,
            expectedMinutes = expectedMinutes,
            description = description,
            isCompleted = isCompleted,
            completedAt = completedAt
        )
        return db.projectDao().upsert(entity)
    }

    suspend fun deleteProject(project: ProjectEntity) = db.projectDao().delete(project)

    suspend fun upsertTask(
        title: String,
        expectedMinutes: Int,
        projectId: Long?,
        alarmOnCompletion: Boolean,
        id: Long? = null,
        actualMinutes: Int = 0,
        isCompleted: Boolean = false,
        description: String? = null,
        completedAt: Long? = null,
    ): Long {
        val entity = TaskEntity(
            id = id ?: 0,
            title = title,
            projectId = projectId,
            expectedMinutes = expectedMinutes,
            actualMinutes = actualMinutes,
            isCompleted = isCompleted,
            alarmOnCompletion = alarmOnCompletion,
            description = description,
            completedAt = completedAt
        )
        return db.taskDao().upsert(entity)
    }

    suspend fun deleteTask(task: TaskEntity) = db.taskDao().delete(task)

    suspend fun getTask(id: Long): TaskEntity? = db.taskDao().getById(id)

    suspend fun logSession(
        taskId: Long?,
        type: String,
        startTime: Long,
        endTime: Long,
        expectedMinutes: Int,
        actualMinutes: Int
    ): Long {
        val id = db.sessionLogDao().insert(
            SessionLogEntity(
                taskId = taskId,
                type = type,
                startTime = startTime,
                endTime = endTime,
                expectedMinutes = expectedMinutes,
                actualMinutes = actualMinutes
            )
        )
        if (taskId != null) {
            val total = (db.sessionLogDao().getTotalMinutesForTask(taskId) ?: 0)
            val task = db.taskDao().getById(taskId)
            if (task != null) {
                db.taskDao().upsert(task.copy(actualMinutes = total))
            }
        }
        return id
    }

    companion object {
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Repository(AppDatabase.get(context)).also { INSTANCE = it }
        }
    }
}
