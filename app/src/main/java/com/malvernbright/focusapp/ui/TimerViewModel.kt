package com.malvernbright.focusapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.malvernbright.focusapp.notifications.BreakReminderWorker
import com.malvernbright.focusapp.notifications.SessionEndWorker
import com.malvernbright.focusapp.notifications.SessionEndWorker.Companion.TYPE_BREAK
import com.malvernbright.focusapp.notifications.SessionEndWorker.Companion.TYPE_WORK
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private val _type = MutableStateFlow(TYPE_WORK)
    val type: StateFlow<String> = _type.asStateFlow()

    private val _totalMillis = MutableStateFlow(0L)
    val totalMillis: StateFlow<Long> = _totalMillis.asStateFlow()

    private var tickerJob: Job? = null
    private var endTime: Long = 0L
    private var startTime: Long = 0L
    private var expectedMinutes: Int = 0
    private var currentTaskId: Long? = null

    private val workManager by lazy { WorkManager.getInstance(getApplication()) }

    fun setType(isWork: Boolean) { _type.value = if (isWork) TYPE_WORK else TYPE_BREAK }

    fun start(minutes: Int, taskId: Long? = null) {
        if (minutes <= 0) return
        cancelScheduledWork()
        expectedMinutes = minutes
        currentTaskId = taskId
        startTime = System.currentTimeMillis()
        endTime = startTime + minutes * 60_000L
        _totalMillis.value = minutes * 60_000L
        scheduleWorker()
        startTicker()
        _isRunning.value = true
    }

    fun pause() {
        if (!_isRunning.value) return
        tickerJob?.cancel()
        val now = System.currentTimeMillis()
        _remainingMillis.value = (endTime - now).coerceAtLeast(0)
        cancelScheduledWork()
        _isRunning.value = false
    }

    fun resume() {
        if (_isRunning.value || _remainingMillis.value <= 0L) return
        val remaining = _remainingMillis.value
        startTime = System.currentTimeMillis()
        endTime = startTime + remaining
        // totalMillis stays as original target
        scheduleWorker(delayMillis = remaining)
        startTicker()
        _isRunning.value = true
    }

    fun cancel() {
        tickerJob?.cancel()
        cancelScheduledWork()
        _isRunning.value = false
        _remainingMillis.value = 0
        _totalMillis.value = 0
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val rem = (endTime - now).coerceAtLeast(0)
                _remainingMillis.value = rem
                if (rem <= 0) {
                    _isRunning.value = false
                    break
                }
                delay(1000)
            }
        }
    }

    private fun scheduleWorker(delayMillis: Long? = null) {
        val delay = delayMillis ?: (endTime - System.currentTimeMillis()).coerceAtLeast(0)
        val data = SessionEndWorker.buildInput(
            type = _type.value,
            taskId = currentTaskId,
            expectedMinutes = expectedMinutes,
            startTime = startTime
        )
        val req = OneTimeWorkRequestBuilder<SessionEndWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, req)
    }

    private fun cancelScheduledWork() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun enableBreakReminders(intervalMinutes: Int) {
        val interval = intervalMinutes.coerceAtLeast(15)
        val req = PeriodicWorkRequestBuilder<BreakReminderWorker>(interval.toLong(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            BREAK_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun disableBreakReminders() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(BREAK_WORK_NAME)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "session_end_work"
        private const val BREAK_WORK_NAME = "break_reminders"
    }
}
