package com.malvernbright.focusapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malvernbright.focusapp.data.ProjectEntity
import com.malvernbright.focusapp.data.Repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)

    val projects: StateFlow<List<ProjectEntity>> = repo.projects
        .map { it.sortedBy { p -> p.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProject(name: String, expectedMinutes: Int, description: String?) {
        viewModelScope.launch {
            repo.upsertProject(name, expectedMinutes, description)
        }
    }

    fun toggleCompleted(project: ProjectEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.upsertProject(
                id = project.id,
                name = project.name,
                expectedMinutes = project.expectedMinutes,
                description = project.description,
                isCompleted = !project.isCompleted,
                completedAt = if (!project.isCompleted) now else null
            )
        }
    }
}
