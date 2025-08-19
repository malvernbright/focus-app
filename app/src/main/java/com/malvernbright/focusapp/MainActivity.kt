package com.malvernbright.focusapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.malvernbright.focusapp.ui.ProjectsScreen
import com.malvernbright.focusapp.ui.TasksScreen
import com.malvernbright.focusapp.ui.TimerScreen
import com.malvernbright.focusapp.ui.theme.FocusAppTheme

class MainActivity : ComponentActivity() {
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore result; user can change in settings later */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        setContent {
            FocusAppTheme {
                AppScaffold()
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun AppScaffold() {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { /* placeholder */ },
                    label = { Text("Timer") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { /* placeholder */ },
                    label = { Text("Tasks") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { /* placeholder */ },
                    label = { Text("Projects") }
                )
            }
        }
    ) { innerPadding ->
        when (tab) {
            0 -> Box(Modifier.padding(innerPadding)) { TimerScreen() }
            1 -> Box(Modifier.padding(innerPadding)) { TasksScreen() }
            else -> Box(Modifier.padding(innerPadding)) { ProjectsScreen() }
        }
    }
}