package com.malvernbright.focusapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malvernbright.focusapp.data.Repository
import com.malvernbright.focusapp.data.TaskEntity
import com.malvernbright.focusapp.notifications.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)

    val tasks: StateFlow<List<TaskEntity>> = repo.tasks
        .map { it.sortedBy { t -> t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTask(title: String, minutes: Int, projectId: Long?, alarm: Boolean, description: String?) {
        viewModelScope.launch {
            repo.upsertTask(title = title, expectedMinutes = minutes, projectId = projectId, alarmOnCompletion = alarm, description = description)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repo.deleteTask(task) }
    }

    fun toggleCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = task.copy(isCompleted = !task.isCompleted, completedAt = if (!task.isCompleted) now else null)
            repo.upsertTask(
                id = updated.id,
                title = updated.title,
                expectedMinutes = updated.expectedMinutes,
                projectId = updated.projectId,
                alarmOnCompletion = updated.alarmOnCompletion,
                actualMinutes = updated.actualMinutes,
                isCompleted = updated.isCompleted,
                description = updated.description,
                completedAt = updated.completedAt
            )
            if (updated.isCompleted && updated.alarmOnCompletion) {
                NotificationHelper.notifyTaskComplete(
                    getApplication(),
                    title = "Task complete",
                    message = "${updated.title} finished"
                )
            }
        }
    }
}
