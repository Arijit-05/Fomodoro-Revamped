package com.arijit.pomodoro

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.arijit.pomodoro.ui.navigation.FomodoroNavGraph
import com.arijit.pomodoro.ui.theme.FomodoroTheme
import com.arijit.pomodoro.ui.timer.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val timerViewModel: TimerViewModel by viewModels()
    private var hasNotificationPermission by mutableStateOf(true)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        setContent {
            val uiState by timerViewModel.uiState.collectAsState()
            val preferences = uiState.preferences
            val snapshot = uiState.snapshot

            // Ultra Focus orientation handling (#51)
            if (preferences.ultraFocusMode) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            val currentPreset = when (snapshot.phase) {
                com.arijit.pomodoro.core.model.Phase.FOCUS -> preferences.focusThemeColor
                com.arijit.pomodoro.core.model.Phase.SHORT_BREAK -> preferences.shortBreakThemeColor
                com.arijit.pomodoro.core.model.Phase.LONG_BREAK -> preferences.longBreakThemeColor
            }

            FomodoroTheme(
                phase = snapshot.phase,
                themeMode = preferences.themeMode,
                presetId = currentPreset,
                dynamicColor = preferences.dynamicColor,
                amoledBlack = preferences.amoledBlack
            ) {

                Surface(modifier = Modifier.fillMaxSize()) {
                    FomodoroNavGraph(
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
