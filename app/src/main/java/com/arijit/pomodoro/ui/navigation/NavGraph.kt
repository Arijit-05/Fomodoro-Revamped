package com.arijit.pomodoro.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arijit.pomodoro.ui.aod.AodScreen
import com.arijit.pomodoro.ui.settings.SettingsScreen
import com.arijit.pomodoro.ui.settings.SettingsViewModel
import com.arijit.pomodoro.ui.timer.MainScreen
import com.arijit.pomodoro.ui.timer.TimerViewModel

@Composable
fun FomodoroNavGraph(
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val timerViewModel: TimerViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val timerUiState by timerViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "timer",
        modifier = modifier.fillMaxSize(),
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { slideOutHorizontally { -it } + fadeOut() },
        popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
    ) {
        composable("timer") {
            if (timerUiState.preferences.ambientAodMode) {
                AodScreen(
                    snapshot = timerUiState.snapshot,
                    onExitAod = {
                        settingsViewModel.setAmbientAodMode(false)
                    }
                )
            } else {
                MainScreen(
                    viewModel = timerViewModel,
                    onNavigateToSettings = {
                        android.util.Log.d("NavGraph", "onNavigateToSettings invoked!")
                        navController.navigate("settings")
                    },
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
            }
        }

        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

