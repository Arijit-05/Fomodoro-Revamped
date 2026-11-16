package com.arijit.pomodoro.ui.timer

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerStatus
import com.arijit.pomodoro.system.WakeLockManager
import com.arijit.pomodoro.ui.components.CircularTimerRing
import com.arijit.pomodoro.ui.components.SessionDots
import com.arijit.pomodoro.ui.components.TabularTimerText
import com.arijit.pomodoro.ui.components.UltraFocusEscapeButton

@Composable
fun MainScreen(
    viewModel: TimerViewModel,
    onNavigateToSettings: () -> Unit,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snapshot = uiState.snapshot
    val preferences = uiState.preferences

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Lifecycle-aware Keep Screen On (tied to Running focus state, #19)
    DisposableEffect(snapshot.isRunning, snapshot.phase, preferences.keepScreenOn) {
        val activity = context as? Activity
        if (activity != null) {
            val shouldKeepOn = preferences.keepScreenOn && snapshot.isRunning && snapshot.phase == Phase.FOCUS
            WakeLockManager.setScreenKeepOn(activity, shouldKeepOn)
        }
        onDispose {
            (context as? Activity)?.let { WakeLockManager.setScreenKeepOn(it, false) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Notification permission banner if denied
        if (!hasNotificationPermission) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notifications are disabled. Enable to receive background alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = onRequestNotificationPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Enable", fontSize = 12.sp)
                    }
                }
            }
        }

        // Main Timer Body
        if (isLandscape) {
            LandscapeLayout(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
            )
        } else {
            PortraitLayout(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 64.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            )
        }

        // Top Bar: Ultra Focus escape button (left) and Settings button (right) - composed LAST so it receives touches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (preferences.ultraFocusMode) {
                UltraFocusEscapeButton(
                    onExit = viewModel::onExitUltraFocus,
                    hapticManager = viewModel.hapticManager
                )
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Settings button: ALWAYS enabled (#53)
            IconButton(
                onClick = {
                    android.util.Log.d("MainScreen", "Settings button tapped!")
                    onNavigateToSettings()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun PortraitLayout(
    uiState: TimerUiState,
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Phase and session indicators
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (snapshot.phase) {
                    Phase.FOCUS -> "Focus"
                    Phase.SHORT_BREAK -> "Short Break"
                    Phase.LONG_BREAK -> "Long Break"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            SessionDots(
                cycleState = snapshot.cycleState,
                activeColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Session ${snapshot.cycleState.currentSession} of ${snapshot.cycleState.totalSessions}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Circular Timer Ring with Tabular Countdown Digits
        CircularTimerRing(
            progress = snapshot.progress,
            phase = snapshot.phase,
            size = 280.dp,
            strokeWidth = 14.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TabularTimerText(
                    remainingMillis = snapshot.remainingMillis,
                    textStyle = MaterialTheme.typography.displayLarge
                )

                // Optional break prompt during breaks (#48)
                if (snapshot.phase.isBreak && !uiState.currentBreakPrompt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.currentBreakPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Bottom Controls
        BottomControls(
            snapshot = snapshot,
            viewModel = viewModel
        )
    }
}

@Composable
fun LandscapeLayout(
    uiState: TimerUiState,
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Left Column: Timer Ring
        CircularTimerRing(
            progress = snapshot.progress,
            phase = snapshot.phase,
            size = 220.dp,
            strokeWidth = 10.dp
        ) {
            TabularTimerText(
                remainingMillis = snapshot.remainingMillis,
                textStyle = MaterialTheme.typography.displayMedium
            )
        }

        // Right Column: Info & Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when (snapshot.phase) {
                    Phase.FOCUS -> "Focus"
                    Phase.SHORT_BREAK -> "Short Break"
                    Phase.LONG_BREAK -> "Long Break"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            SessionDots(cycleState = snapshot.cycleState)

            BottomControls(
                snapshot = snapshot,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun BottomControls(
    snapshot: com.arijit.pomodoro.core.model.TimerSnapshot,
    viewModel: TimerViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Phase-ended action bar: Quick snooze (+1, +3, +5 min) and Start Next
        AnimatedVisibility(
            visible = snapshot.isPhaseEnded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { viewModel.onSnooze(1) }) { Text("+1m") }
                    FilledTonalButton(onClick = { viewModel.onSnooze(3) }) { Text("+3m") }
                    FilledTonalButton(onClick = { viewModel.onSnooze(5) }) { Text("+5m") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::onStartNextPhase,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Start Next Phase")
                }
            }
        }

        // Standard Timer Controls
        if (!snapshot.isPhaseEnded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Reset Button
                IconButton(
                    onClick = viewModel::onReset,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Main Play / Pause Button
                Button(
                    onClick = {
                        if (snapshot.isRunning) viewModel.onPause() else viewModel.onStart()
                    },
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (snapshot.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (snapshot.isRunning) "Pause" else "Start",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Skip Button
                IconButton(
                    onClick = viewModel::onSkip,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

