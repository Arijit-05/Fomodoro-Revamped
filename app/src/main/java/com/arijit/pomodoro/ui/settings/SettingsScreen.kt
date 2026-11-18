package com.arijit.pomodoro.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arijit.pomodoro.core.datastore.HapticLevel
import com.arijit.pomodoro.core.datastore.ThemeMode
import com.arijit.pomodoro.system.BatteryOptimizationHelper
import com.arijit.pomodoro.ui.theme.SessionColorPresets


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences = uiState.preferences
    val isTimerActive = uiState.isTimerRunningOrPaused

    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // SAF Picker Launcher for Custom Ambient Audio
    val ambientAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setAmbientConfig(
                style = "custom",
                uri = uri.toString(),
                volume = preferences.ambientVolume,
                pauseDuringBreak = preferences.pauseAmbientDuringBreak
            )
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mid-phase notification badge (#53)
            AnimatedVisibility(visible = isTimerActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Timer is currently active. Any duration adjustments apply to the next session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 1. TIMER SECTION
            SettingsSectionHeader(title = "Timer Durations")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Focus Duration Slider
                    DurationSliderRow(
                        title = "Focus Duration",
                        value = preferences.focusDurationMinutes,
                        range = 1f..120f,
                        unit = "min",
                        onValueChange = { newMinutes ->
                            viewModel.updateTimerDurations(
                                focusMinutes = newMinutes,
                                shortBreakMinutes = preferences.shortBreakDurationMinutes,
                                longBreakMinutes = preferences.longBreakDurationMinutes,
                                sessionsPerCycle = preferences.sessionsPerCycle,
                                autoStartBreaks = preferences.autoStartBreaks,
                                autoStartFocus = preferences.autoStartFocus,
                                snoozeMinutes = preferences.snoozeDurationMinutes
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Short Break Duration Slider (#43: up to 60m)
                    DurationSliderRow(
                        title = "Short Break",
                        value = preferences.shortBreakDurationMinutes,
                        range = 1f..60f,
                        unit = "min",
                        onValueChange = { newMinutes ->
                            viewModel.updateTimerDurations(
                                focusMinutes = preferences.focusDurationMinutes,
                                shortBreakMinutes = newMinutes,
                                longBreakMinutes = preferences.longBreakDurationMinutes,
                                sessionsPerCycle = preferences.sessionsPerCycle,
                                autoStartBreaks = preferences.autoStartBreaks,
                                autoStartFocus = preferences.autoStartFocus,
                                snoozeMinutes = preferences.snoozeDurationMinutes
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Long Break Duration Slider
                    DurationSliderRow(
                        title = "Long Break",
                        value = preferences.longBreakDurationMinutes,
                        range = 1f..90f,
                        unit = "min",
                        onValueChange = { newMinutes ->
                            viewModel.updateTimerDurations(
                                focusMinutes = preferences.focusDurationMinutes,
                                shortBreakMinutes = preferences.shortBreakDurationMinutes,
                                longBreakMinutes = newMinutes,
                                sessionsPerCycle = preferences.sessionsPerCycle,
                                autoStartBreaks = preferences.autoStartBreaks,
                                autoStartFocus = preferences.autoStartFocus,
                                snoozeMinutes = preferences.snoozeDurationMinutes
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sessions per cycle
                    DurationSliderRow(
                        title = "Sessions before Long Break",
                        value = preferences.sessionsPerCycle,
                        range = 1f..12f,
                        unit = "sessions",
                        onValueChange = { newSessions ->
                            viewModel.updateTimerDurations(
                                focusMinutes = preferences.focusDurationMinutes,
                                shortBreakMinutes = preferences.shortBreakDurationMinutes,
                                longBreakMinutes = preferences.longBreakDurationMinutes,
                                sessionsPerCycle = newSessions,
                                autoStartBreaks = preferences.autoStartBreaks,
                                autoStartFocus = preferences.autoStartFocus,
                                snoozeMinutes = preferences.snoozeDurationMinutes
                            )
                        }
                    )
                }
            }

            // Timer Automation Toggles
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Auto-start Breaks") },
                        supportingContent = { Text("Automatically begin break after focus session") },
                        trailingContent = {
                            Switch(
                                checked = preferences.autoStartBreaks,
                                onCheckedChange = { checked ->
                                    viewModel.updateTimerDurations(
                                        focusMinutes = preferences.focusDurationMinutes,
                                        shortBreakMinutes = preferences.shortBreakDurationMinutes,
                                        longBreakMinutes = preferences.longBreakDurationMinutes,
                                        sessionsPerCycle = preferences.sessionsPerCycle,
                                        autoStartBreaks = checked,
                                        autoStartFocus = preferences.autoStartFocus,
                                        snoozeMinutes = preferences.snoozeDurationMinutes
                                    )
                                }
                            )
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Auto-start Focus") },
                        supportingContent = { Text("Automatically begin focus after break finishes") },
                        trailingContent = {
                            Switch(
                                checked = preferences.autoStartFocus,
                                onCheckedChange = { checked ->
                                    viewModel.updateTimerDurations(
                                        focusMinutes = preferences.focusDurationMinutes,
                                        shortBreakMinutes = preferences.shortBreakDurationMinutes,
                                        longBreakMinutes = preferences.longBreakDurationMinutes,
                                        sessionsPerCycle = preferences.sessionsPerCycle,
                                        autoStartBreaks = preferences.autoStartBreaks,
                                        autoStartFocus = checked,
                                        snoozeMinutes = preferences.snoozeDurationMinutes
                                    )
                                }
                            )
                        }
                    )
                }
            }

            // 2. APPEARANCE SECTION
            SettingsSectionHeader(title = "Appearance & Display")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    // Theme Mode Selector
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("App Theme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = when (preferences.themeMode) {
                                ThemeMode.SYSTEM -> "Follow System Default"
                                ThemeMode.LIGHT -> "Light Theme"
                                ThemeMode.DARK -> "Dark Theme"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeButton(
                                label = "System",
                                isSelected = preferences.themeMode == ThemeMode.SYSTEM,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setThemeMode(ThemeMode.SYSTEM)
                            }
                            ThemeModeButton(
                                label = "Light",
                                isSelected = preferences.themeMode == ThemeMode.LIGHT,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setThemeMode(ThemeMode.LIGHT)
                            }
                            ThemeModeButton(
                                label = "Dark",
                                isSelected = preferences.themeMode == ThemeMode.DARK,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setThemeMode(ThemeMode.DARK)
                            }
                        }
                    }

                    // Dynamic Color
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ListItem(
                            headlineContent = { Text("Dynamic Color") },
                            supportingContent = { Text("Override with Material You system accent color") },
                            trailingContent = {
                                Switch(
                                    checked = preferences.dynamicColor,
                                    onCheckedChange = viewModel::setDynamicColor
                                )
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    // Session Background Colors (User selectable 8 palettes)
                    Text(
                        text = "Session Background Colors",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )

                    SessionColorSelector(
                        title = "Focus Timer Color",
                        selectedId = preferences.focusThemeColor,
                        onSelectColor = viewModel::setFocusThemeColor
                    )

                    SessionColorSelector(
                        title = "Short Break Color",
                        selectedId = preferences.shortBreakThemeColor,
                        onSelectColor = viewModel::setShortBreakThemeColor
                    )

                    SessionColorSelector(
                        title = "Long Break Color",
                        selectedId = preferences.longBreakThemeColor,
                        onSelectColor = viewModel::setLongBreakThemeColor
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    // AMOLED Black
                    ListItem(
                        headlineContent = { Text("AMOLED Pure Black") },
                        supportingContent = { Text("Pure #000000 background in dark mode (off: uses deep session color)") },
                        trailingContent = {
                            Switch(
                                checked = preferences.amoledBlack,
                                onCheckedChange = viewModel::setAmoledBlack
                            )
                        }
                    )


                    // Keep Screen Awake (#19)
                    ListItem(
                        headlineContent = { Text("Keep Screen Awake") },
                        supportingContent = { Text("Keeps screen on during active focus sessions") },
                        trailingContent = {
                            Switch(
                                checked = preferences.keepScreenOn,
                                onCheckedChange = viewModel::setKeepScreenOn
                            )
                        }
                    )

                    // Ultra Focus Mode (#51)
                    ListItem(
                        headlineContent = { Text("Ultra Focus Mode") },
                        supportingContent = { Text("Landscape fullscreen distraction-free timer with press-and-hold escape") },
                        trailingContent = {
                            Switch(
                                checked = preferences.ultraFocusMode,
                                onCheckedChange = viewModel::setUltraFocusMode
                            )
                        }
                    )

                    // Always-On-Display (AOD) / Burn-in mode (#20)
                    ListItem(
                        headlineContent = { Text("Ambient AOD Mode") },
                        supportingContent = { Text("Dim OLED black screen with 1-min pixel shift to prevent burn-in") },
                        trailingContent = {
                            Switch(
                                checked = preferences.ambientAodMode,
                                onCheckedChange = viewModel::setAmbientAodMode
                            )
                        }
                    )
                }
            }

            // 3. SOUNDS & AMBIENCE SECTION (#16, #36, #42)
            SettingsSectionHeader(title = "Sounds & Music")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ambient Audio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Ambient sound picker
                    val ambientOptions = listOf("none", "brown_noise", "rain", "ocean", "fireplace", "forest", "cafe", "custom")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound Track:")
                        DropdownSelector(
                            selected = preferences.ambientSoundStyle,
                            options = ambientOptions,
                            onSelected = { style ->
                                if (style == "custom") {
                                    ambientAudioPickerLauncher.launch(arrayOf("audio/*"))
                                } else {
                                    viewModel.setAmbientConfig(
                                        style = style,
                                        uri = null,
                                        volume = preferences.ambientVolume,
                                        pauseDuringBreak = preferences.pauseAmbientDuringBreak
                                    )
                                }
                            }
                        )
                    }

                    if (preferences.ambientSoundStyle != "none") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Volume: ${(preferences.ambientVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = preferences.ambientVolume,
                            onValueChange = { vol ->
                                viewModel.setAmbientConfig(
                                    style = preferences.ambientSoundStyle,
                                    uri = preferences.ambientSoundUri,
                                    volume = vol,
                                    pauseDuringBreak = preferences.pauseAmbientDuringBreak
                                )
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pause during breaks", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = preferences.pauseAmbientDuringBreak,
                                onCheckedChange = { pause ->
                                    viewModel.setAmbientConfig(
                                        style = preferences.ambientSoundStyle,
                                        uri = preferences.ambientSoundUri,
                                        volume = preferences.ambientVolume,
                                        pauseDuringBreak = pause
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ticking Sound", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val tickingOptions = listOf("none", "classic", "mechanical", "soft")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ticking Style:")
                        DropdownSelector(
                            selected = preferences.tickingSoundStyle,
                            options = tickingOptions,
                            onSelected = { style ->
                                viewModel.setTickingConfig(
                                    style = style,
                                    volume = preferences.tickingVolume,
                                    onlyDuringFocus = preferences.tickOnlyDuringFocus
                                )
                            }
                        )
                    }

                    if (preferences.tickingSoundStyle != "none") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Volume: ${(preferences.tickingVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = preferences.tickingVolume,
                            onValueChange = { vol ->
                                viewModel.setTickingConfig(
                                    style = preferences.tickingSoundStyle,
                                    volume = vol,
                                    onlyDuringFocus = preferences.tickOnlyDuringFocus
                                )
                            }
                        )
                    }
                }
            }

            // 4. FEEDBACK & FLASH (#49)
            SettingsSectionHeader(title = "Haptics & Flash")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Haptic Feedback", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Intensity: ${preferences.hapticLevel.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val hapticOptions = listOf(
                            Triple(HapticLevel.OFF, "Off", "No vibration or haptic feedback"),
                            Triple(HapticLevel.SUBTLE, "Subtle", "Gentle tactile taps on buttons"),
                            Triple(HapticLevel.STANDARD, "Standard", "Balanced vibration for controls and alerts"),
                            Triple(HapticLevel.STRONG, "Strong", "Pronounced vibration alerts")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            hapticOptions.forEach { (level, label, desc) ->
                                val isSelected = preferences.hapticLevel == level
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setHapticLevel(level) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.setHapticLevel(level) }
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }


                    ListItem(
                        headlineContent = { Text("Test Haptics") },
                        trailingContent = {
                            FilledTonalButton(onClick = viewModel::testHaptics) {
                                Text("Test")
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Torch Flash Notification") },
                        supportingContent = { Text("Blink torch on phase completion with screen fallback") },
                        trailingContent = {
                            Switch(
                                checked = preferences.flashNotification,
                                onCheckedChange = viewModel::setFlashNotification
                            )
                        }
                    )
                }
            }

            // 5. NOTIFICATIONS & SYSTEM
            SettingsSectionHeader(title = "Notifications & Background")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    if (Build.VERSION.SDK_INT >= 36) {
                        ListItem(
                            headlineContent = { Text("Live Updates (Android 16)") },
                            supportingContent = { Text("Promoted ongoing status bar chip and progress styles") },
                            trailingContent = {
                                Switch(
                                    checked = preferences.liveUpdatesEnabled,
                                    onCheckedChange = viewModel::setLiveUpdatesEnabled
                                )
                            }
                        )
                    }

                    ListItem(
                        headlineContent = { Text("Battery & Background Guidance") },
                        supportingContent = { Text("Configure OEM autostart & battery optimizations") },
                        trailingContent = {
                            FilledTonalButton(
                                onClick = {
                                    val intent = BatteryOptimizationHelper.createOemAutostartIntent(context)
                                    if (intent != null) context.startActivity(intent)
                                }
                            ) {
                                Text("Open")
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Don't Kill My App Guide") },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    val uri = BatteryOptimizationHelper.getDontKillMyAppUri()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }
                            ) {
                                Text("dontkillmyapp.com")
                            }
                        }
                    )
                }
            }

            // 6. ABOUT
            SettingsSectionHeader(title = "About Fomodoro")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fomodoro Revamped v2.0", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Built with Jetpack Compose, Material 3, and Media3 ExoPlayer. 100% CC0 Royalty-Free synthesized audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun DurationSliderRow(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textInput by remember(value) { mutableStateOf(value.toString()) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { isEditing = !isEditing }) {
                Text("$value $unit", fontWeight = FontWeight.Bold)
            }
        }

        if (isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        textInput = input
                        input.toIntOrNull()?.let { num ->
                            val clamped = num.coerceIn(range.start.toInt(), range.endInclusive.toInt())
                            onValueChange(clamped)
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )
                TextButton(onClick = { isEditing = false }) {
                    Text("Done")
                }
            }
        } else {
            Slider(
                value = value.toFloat(),
                onValueChange = { floatVal ->
                    onValueChange(floatVal.toInt())
                },
                valueRange = range,
                steps = (range.endInclusive - range.start).toInt() - 1
            )
        }
    }
}

@Composable
fun ThemeModeButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    ) {
        Text(label, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.replace('_', ' ').replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .width(160.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SessionColorSelector(
    title: String,
    selectedId: String,
    onSelectColor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPreset = SessionColorPresets.getPreset(selectedId)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = currentPreset.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SessionColorPresets.presets) { preset ->
                val isSelected = preset.id == selectedId
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(preset.primaryColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(preset.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

