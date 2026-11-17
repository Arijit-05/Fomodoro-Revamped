package com.arijit.pomodoro.ui.timer

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff


import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.arijit.pomodoro.R
import com.arijit.pomodoro.core.model.Phase

import com.arijit.pomodoro.system.WakeLockManager
import com.arijit.pomodoro.ui.components.AmbientSoundSheet
import com.arijit.pomodoro.ui.components.StackedTimerText
import com.arijit.pomodoro.ui.components.UltraFocusEscapeButton
import com.arijit.pomodoro.ui.theme.LocalFomodoroPalette
import com.arijit.pomodoro.ui.theme.Montserrat

@OptIn(ExperimentalMaterial3Api::class)
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

    val palette = LocalFomodoroPalette.current
    val animatedBgColor by animateColorAsState(
        targetValue = palette.background,
        animationSpec = tween(durationMillis = 350),
        label = "ScreenBgAnimation"
    )

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showAmbientSheet by remember { mutableStateOf(false) }

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
            .background(animatedBgColor)
    ) {
        // Notification permission banner if denied
        if (!hasNotificationPermission) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 60.dp)
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
                            text = "Notifications disabled. Enable for background alerts.",
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
            LandscapeTimerLayout(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        } else {
            PortraitTimerLayout(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 56.dp, bottom = 16.dp)
            )
        }

        // Top Bar: Waveform / Ambient Button (Left) and Settings Button (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ambient sound button: Lottie animation when playing, static waveform when off
            val isMusicActuallyPlaying = snapshot.isRunning &&
                preferences.ambientSoundStyle != "none" &&
                (!snapshot.phase.isBreak || !preferences.pauseAmbientDuringBreak)
            val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.music_anim))
            val lottieProgress by animateLottieCompositionAsState(
                composition = lottieComposition,
                isPlaying = isMusicActuallyPlaying,
                iterations = LottieConstants.IterateForever
            )

            IconButton(
                onClick = { showAmbientSheet = true },
                modifier = Modifier.size(48.dp)
            ) {
                if (isMusicActuallyPlaying) {
                    LottieAnimation(
                        composition = lottieComposition,
                        progress = { lottieProgress },
                        modifier = Modifier.size(34.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.music_white),
                        contentDescription = "Ambient Sound",
                        tint = palette.topBarWaveColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }


            // If Ultra Focus is active, show escape progress button
            if (preferences.ultraFocusMode) {
                UltraFocusEscapeButton(
                    onExit = viewModel::onExitUltraFocus,
                    hapticManager = viewModel.hapticManager
                )
            }

            // Settings button: ALWAYS accessible (#53)
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings",
                    tint = palette.topBarGearColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Ambient Sound Modal Bottom Sheet
    if (showAmbientSheet) {
        AmbientSoundSheet(
            currentStyle = preferences.ambientSoundStyle,
            currentVolume = preferences.ambientVolume,
            onSelectStyle = { style ->
                viewModel.setAmbientStyle(style)
            },
            onVolumeChange = { vol ->
                viewModel.setAmbientVolume(vol)
            },
            onDismiss = { showAmbientSheet = false }
        )
    }
}

@Composable
fun PortraitTimerLayout(
    uiState: TimerUiState,
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot
    val palette = LocalFomodoroPalette.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Center Content Block: Pill + Stacked Digits + Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Top Phase Pill
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = palette.pillBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val iconRes = if (snapshot.phase == Phase.FOCUS) {
                        R.drawable.brain
                    } else {
                        R.drawable.coffee
                    }
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = palette.pillContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (snapshot.phase) {
                            Phase.FOCUS -> "Focus"
                            Phase.SHORT_BREAK -> "Short Break"
                            Phase.LONG_BREAK -> "Long Break"
                        },
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = palette.pillContentColor
                    )
                }
            }

            // Stacked 2x2 Digits (Minutes top, Seconds bottom tightly packed)
            StackedTimerText(
                remainingMillis = snapshot.remainingMillis,
                color = palette.digitColor,
                fontSize = 135.sp
            )

            // Break prompt if active during break (#48) - cycles calmly every 3s without shifting
            if (snapshot.phase.isBreak) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = uiState.currentBreakPrompt,
                        animationSpec = tween(400),
                        label = "BreakPromptTransition",
                        modifier = Modifier.fillMaxWidth()
                    ) { prompt ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!prompt.isNullOrBlank()) {
                                Text(
                                    text = prompt,
                                    fontFamily = Montserrat,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = palette.digitColor.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }


            Spacer(modifier = Modifier.height(8.dp))

            // Controls Block
            TimerControls(
                snapshot = snapshot,
                viewModel = viewModel
            )

        }

        // Bottom Session Indicator: "1/3"
        Text(
            text = "${snapshot.cycleState.currentSession}/${snapshot.cycleState.totalSessions}",
            fontFamily = Montserrat,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            color = palette.sessionTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun LandscapeTimerLayout(
    uiState: TimerUiState,
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot
    val palette = LocalFomodoroPalette.current

    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Left Column: Stacked Digits & Pill (compact for landscape height)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = palette.pillBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconRes = if (snapshot.phase == Phase.FOCUS) R.drawable.brain else R.drawable.coffee
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = palette.pillContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (snapshot.phase) {
                            Phase.FOCUS -> "Focus"
                            Phase.SHORT_BREAK -> "Short Break"
                            Phase.LONG_BREAK -> "Long Break"
                        },
                        fontFamily = Montserrat,
                        fontSize = 14.sp,
                        color = palette.pillContentColor
                    )
                }
            }

            StackedTimerText(
                remainingMillis = snapshot.remainingMillis,
                color = palette.digitColor,
                fontSize = 72.sp
            )
        }

        // Right Column: Controls + Session info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimerControls(
                snapshot = snapshot,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${snapshot.cycleState.currentSession}/${snapshot.cycleState.totalSessions}",
                fontFamily = Montserrat,
                fontSize = 15.sp,
                color = palette.sessionTextColor
            )
        }
    }
}

@Composable
fun TimerControls(
    snapshot: com.arijit.pomodoro.core.model.TimerSnapshot,
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalFomodoroPalette.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
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
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBackground)
                ) {
                    Text("Start Next Phase", color = palette.buttonContentColor)
                }
            }
        }

        if (!snapshot.isPhaseEnded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Restart / Reset Button: on the LEFT (visible when timer is Running or Paused)
                if (snapshot.isRunning || snapshot.isPaused) {
                    IconButton(
                        onClick = viewModel::onReset,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.reset_white),
                            contentDescription = "Restart Timer",
                            tint = palette.controlIconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Main Squircle Play / Pause Button in the center
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 76.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.buttonBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (snapshot.isRunning) {
                                    viewModel.onPause()
                                } else if (snapshot.isPaused) {
                                    viewModel.onResume()
                                } else {
                                    viewModel.onStart()
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconRes = if (snapshot.isRunning) {
                        R.drawable.pause_white
                    } else {
                        R.drawable.play_white
                    }
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = if (snapshot.isRunning) "Pause" else "Start Timer",
                        tint = palette.buttonContentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Next Button: on the RIGHT (ONLY when timer is Running or Paused, NEVER when Idle!)
                if (snapshot.isRunning || snapshot.isPaused) {
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = viewModel::onSkip,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_next),
                            contentDescription = "Next Session",
                            tint = palette.controlIconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }


            }

            // "Back to timer" text button on break screens
            if (snapshot.phase.isBreak) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Back to timer",
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = palette.actionTextColor,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = viewModel::onSkip
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}
