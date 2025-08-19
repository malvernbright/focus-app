package com.malvernbright.focusapp.ui

import android.app.Application
import android.content.Context
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
    private val prefs by lazy { getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    init {
        restoreIfAny()
    }

    fun setType(isWork: Boolean) { _type.value = if (isWork) TYPE_WORK else TYPE_BREAK }

    fun start(minutes: Int, taskId: Long? = null) {
        if (minutes <= 0) return
        cancelScheduledWork()
        expectedMinutes = minutes
        currentTaskId = taskId
        startTime = System.currentTimeMillis()
        endTime = startTime + minutes * 60_000L
        _totalMillis.value = minutes * 60_000L
        persistState(running = true, pausedRemaining = null)
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
        persistState(running = false, pausedRemaining = _remainingMillis.value)
    }

    fun resume() {
        if (_isRunning.value || _remainingMillis.value <= 0L) return
        val remaining = _remainingMillis.value
        startTime = System.currentTimeMillis()
        endTime = startTime + remaining
        // totalMillis stays as original target
        persistState(running = true, pausedRemaining = null)
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
        clearState()
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
                    clearState()
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

    private fun persistState(running: Boolean, pausedRemaining: Long?) {
        prefs.edit()
            .putBoolean(KEY_RUNNING, running)
            .putLong(KEY_START, startTime)
            .putLong(KEY_END, endTime)
            .putInt(KEY_EXPECTED, expectedMinutes)
            .putString(KEY_TYPE, _type.value)
            .putLong(KEY_TASK_ID, currentTaskId ?: -1L)
            .apply()
        if (!running) {
            prefs.edit().putLong(KEY_PAUSED_REMAINING, pausedRemaining ?: 0L).apply()
        } else {
            prefs.edit().remove(KEY_PAUSED_REMAINING).apply()
        }
    }

    private fun clearState() {
        prefs.edit().clear().apply()
    }

    private fun restoreIfAny() {
        val running = prefs.getBoolean(KEY_RUNNING, false)
        val savedType = prefs.getString(KEY_TYPE, TYPE_WORK) ?: TYPE_WORK
        val savedExpected = prefs.getInt(KEY_EXPECTED, 0)
        val savedStart = prefs.getLong(KEY_START, 0L)
        val savedEnd = prefs.getLong(KEY_END, 0L)
        val savedTaskId = prefs.getLong(KEY_TASK_ID, -1L).takeIf { it > 0 }
        val pausedRemaining = prefs.getLong(KEY_PAUSED_REMAINING, 0L)

        if (savedExpected > 0) {
            _type.value = savedType
            expectedMinutes = savedExpected
            currentTaskId = savedTaskId
            _totalMillis.value = savedExpected * 60_000L
        }

        if (running) {
            startTime = savedStart
            endTime = savedEnd
            val rem = (endTime - System.currentTimeMillis()).coerceAtLeast(0)
            _remainingMillis.value = rem
            if (rem > 0) {
                // Ensure worker is scheduled; REPLACE if already exists
                scheduleWorker(rem)
                startTicker()
                _isRunning.value = true
            } else {
                clearState()
            }
        } else if (pausedRemaining > 0L) {
            _remainingMillis.value = pausedRemaining
            _isRunning.value = false
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "session_end_work"
        private const val BREAK_WORK_NAME = "break_reminders"

        private const val PREFS_NAME = "timer_state"
        private const val KEY_RUNNING = "running"
        private const val KEY_START = "start"
        private const val KEY_END = "end"
        private const val KEY_EXPECTED = "expected"
        private const val KEY_TYPE = "type"
        private const val KEY_TASK_ID = "taskId"
        private const val KEY_PAUSED_REMAINING = "pausedRemaining"
    }
}
