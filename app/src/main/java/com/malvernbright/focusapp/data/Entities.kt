package com.malvernbright.focusapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val expectedMinutes: Int = 0,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val projectId: Long? = null,
    val expectedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val alarmOnCompletion: Boolean = false,
    val description: String? = null,
    val completedAt: Long? = null
)

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val type: String, // WORK or BREAK
    val startTime: Long,
    val endTime: Long,
    val expectedMinutes: Int,
    val actualMinutes: Int
)
