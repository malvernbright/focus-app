package com.malvernbright.focusapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity): Long

    @Delete
    suspend fun delete(project: ProjectEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Delete
    suspend fun delete(task: TaskEntity)
}

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_logs WHERE taskId = :taskId ORDER BY startTime DESC")
    fun observeLogsForTask(taskId: Long): Flow<List<SessionLogEntity>>

    @Insert
    suspend fun insert(log: SessionLogEntity): Long

    @Query("SELECT SUM(actualMinutes) FROM session_logs WHERE taskId = :taskId")
    suspend fun getTotalMinutesForTask(taskId: Long): Int?
}

