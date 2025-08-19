package com.malvernbright.focusapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malvernbright.focusapp.data.TaskEntity
import java.util.Locale
import java.text.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(
    vm: TimerViewModel = viewModel(),
    tasksVm: TasksViewModel = viewModel(),
    projectsVm: ProjectsViewModel = viewModel(),
) {
    val isRunning by vm.isRunning.collectAsState()
    val remaining by vm.remainingMillis.collectAsState()
    val total by vm.totalMillis.collectAsState()
    val type by vm.type.collectAsState()
    val tasks by tasksVm.tasks.collectAsState()
    val projects by projectsVm.projects.collectAsState()

    var minutesText by remember { mutableStateOf("25") }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var breakEnabled by remember { mutableStateOf(false) }
    var breakIntervalText by remember { mutableStateOf("60") }

    val selectedMinutes: Int = when {
        selectedTaskId != null -> tasks.firstOrNull { it.id == selectedTaskId }?.expectedMinutes ?: 0
        selectedProjectId != null -> projects.firstOrNull { it.id == selectedProjectId }?.expectedMinutes ?: 0
        else -> minutesText.toIntOrNull() ?: 0
    }
    val showZeroWarning = selectedMinutes <= 0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = type == "WORK", onClick = { vm.setType(true) })
            Text("Work")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = type == "BREAK", onClick = { vm.setType(false) })
            Text("Break")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = minutesText,
            onValueChange = { if (it.all { ch -> ch.isDigit() }) minutesText = it },
            label = { Text("Minutes") },
            singleLine = true,
            enabled = selectedTaskId == null && selectedProjectId == null,
            readOnly = selectedTaskId != null || selectedProjectId != null,
            modifier = Modifier.width(200.dp)
        )
        if (selectedTaskId != null || selectedProjectId != null) {
            TextButton(onClick = {
                selectedTaskId = null
                selectedProjectId = null
            }) { Text("Clear selection") }
        }
        if (showZeroWarning) {
            Text("Minutes must be greater than 0", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))

        // Task selection (optional)
        var expandTasks by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { expandTasks = !expandTasks }) { Text(selectedTaskId?.let { id ->
            tasks.firstOrNull { it.id == id }?.title ?: "Select task (optional)"
        } ?: "Select task (optional)") }
        if (expandTasks) {
            Surface(Modifier.fillMaxWidth().padding(8.dp)) {
                LazyColumn {
                    items(tasks.filter { !it.isCompleted }) { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedTaskId = task.id
                                selectedProjectId = null
                                minutesText = task.expectedMinutes.toString()
                                expandTasks = false
                            }.padding(8.dp)
                        ) {
                            Column { Text(task.title); if (!task.description.isNullOrBlank()) Text(task.description!!, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        // Project selection (optional)
        var expandProjects by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { expandProjects = !expandProjects }) { Text(selectedProjectId?.let { id ->
            projects.firstOrNull { it.id == id }?.name ?: "Select project (optional)"
        } ?: "Select project (optional)") }
        if (expandProjects) {
            Surface(Modifier.fillMaxWidth().padding(8.dp)) {
                LazyColumn {
                    items(projects.filter { !it.isCompleted }) { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedProjectId = p.id
                                selectedTaskId = null
                                minutesText = p.expectedMinutes.toString()
                                expandProjects = false
                            }.padding(8.dp)
                        ) { Column { Text(p.name); if (!p.description.isNullOrBlank()) Text(p.description!!, style = MaterialTheme.typography.bodySmall) } }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Break reminders")
            Switch(checked = breakEnabled, onCheckedChange = {
                breakEnabled = it
                val interval = breakIntervalText.toIntOrNull() ?: 60
                if (it) vm.enableBreakReminders(interval) else vm.disableBreakReminders()
            })
            OutlinedTextField(
                value = breakIntervalText,
                onValueChange = { if (it.all { ch -> ch.isDigit() }) breakIntervalText = it },
                label = { Text("Interval (min)") },
                singleLine = true,
                modifier = Modifier.width(150.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        val progress = if (total > 0) 1f - (remaining.toFloat() / total.toFloat()) else 0f
        val animProgress by animateFloatAsState(targetValue = progress, label = "progress")
        LinearProgressIndicator(progress = { animProgress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatMillis(remaining),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (selectedMinutes <= 0) return@Button
                vm.start(selectedMinutes, selectedTaskId)
            }) { Text("Start") }
            OutlinedButton(onClick = { if (isRunning) vm.pause() else vm.resume() }) { Text(if (isRunning) "Pause" else "Resume") }
            TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
        }
    }
}

@Composable
fun TasksScreen(vm: TasksViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing: TaskEntity? by remember { mutableStateOf(null) }
    val appContext = LocalContext.current.applicationContext

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tasks")
            Button(onClick = { editing = null; showDialog = true }) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))
        val (active, completed) = remember(tasks) { tasks.partition { !it.isCompleted } }
        Text("Active", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
            items(active, key = { it.id }) { task ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.isCompleted, onCheckedChange = { vm.toggleCompleted(task) })
                    Column(Modifier.weight(1f).clickable { editing = task; showDialog = true }) {
                        Text(task.title)
                        if (!task.description.isNullOrBlank()) Text(task.description!!, style = MaterialTheme.typography.bodySmall)
                        Text("Expected: ${task.expectedMinutes} min, Actual: ${task.actualMinutes} min", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.deleteTask(task) }) { Text("Delete") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (completed.isNotEmpty()) {
            Text("Completed", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(completed, key = { it.id }) { task ->
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(task.title)
                        if (!task.description.isNullOrBlank()) Text(task.description!!, style = MaterialTheme.typography.bodySmall)
                        val dt = task.completedAt?.let { DateFormat.getDateTimeInstance().format(java.util.Date(it)) } ?: ""
                        if (dt.isNotEmpty()) Text("Completed: $dt", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showDialog) {
        TaskDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { title, minutes, pid, alarm, description ->
                if (editing == null) {
                    vm.addTask(title, minutes, pid, alarm, description)
                } else {
                    vm.viewModelScope.launch {
                        val repo = com.malvernbright.focusapp.data.Repository.get(appContext)
                        val t = editing!!
                        repo.upsertTask(
                            id = t.id,
                            title = title,
                            expectedMinutes = minutes,
                            projectId = pid,
                            alarmOnCompletion = alarm,
                            actualMinutes = t.actualMinutes,
                            isCompleted = t.isCompleted,
                            description = description,
                            completedAt = t.completedAt
                        )
                    }
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun TaskDialog(
    initial: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Long?, Boolean, String?) -> Unit
) {
    var title by remember { mutableStateOf(TextFieldValue(initial?.title ?: "")) }
    var minutes by remember { mutableStateOf(initial?.expectedMinutes?.toString() ?: "25") }
    var projectId: Long? by remember { mutableStateOf(initial?.projectId) }
    var alarm by remember { mutableStateOf(initial?.alarmOnCompletion ?: true) }
    var description by remember { mutableStateOf(TextFieldValue(initial?.description ?: "")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = minutes, onValueChange = { if (it.all { ch -> ch.isDigit() }) minutes = it }, label = { Text("Expected minutes") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = alarm, onCheckedChange = { alarm = it })
                    Text("Alarm on completion")
                }
                // Project selection could be added here; keeping simple for now
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(title.text.trim(), minutes.toIntOrNull() ?: 0, projectId, alarm, description.text.trim().ifEmpty { null })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProjectsScreen(vm: ProjectsViewModel = viewModel()) {
    val projects by vm.projects.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Projects")
            Button(onClick = { showDialog = true }) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))
        val (active, completed) = remember(projects) { projects.partition { !it.isCompleted } }
        Text("Active", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
            items(active, key = { it.id }) { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = p.isCompleted, onCheckedChange = { vm.toggleCompleted(p) })
                    Column(Modifier.weight(1f)) {
                        Text(p.name)
                        if (!p.description.isNullOrBlank()) Text(p.description!!, style = MaterialTheme.typography.bodySmall)
                        Text("Expected: ${p.expectedMinutes} min", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (completed.isNotEmpty()) {
            Text("Completed", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(completed, key = { it.id }) { p ->
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(p.name)
                        if (!p.description.isNullOrBlank()) Text(p.description!!, style = MaterialTheme.typography.bodySmall)
                        val dt = p.completedAt?.let { DateFormat.getDateTimeInstance().format(java.util.Date(it)) } ?: ""
                        if (dt.isNotEmpty()) Text("Completed: $dt", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = minutes, onValueChange = { if (it.all { ch -> ch.isDigit() }) minutes = it }, label = { Text("Expected minutes") })
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.addProject(name.trim(), minutes.toIntOrNull() ?: 0, description.trim().ifEmpty { null }); name = ""; minutes = "0"; description = ""; showDialog = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
