# Fomodoro Revamped 🍅

[![Target SDK 36](https://img.shields.io/badge/targetSdk-36%20(Android%2016)-brightgreen.svg)](https://developer.android.com)
[![Min SDK 26](https://img.shields.io/badge/minSdk-26%20(Android%208.0)-blue.svg)](https://developer.android.com)
[![Kotlin 2.0](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose M3](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer%201.5.1-orange.svg)](https://developer.android.com/media/media3)
[![License: CC0](https://img.shields.io/badge/Audio%20License-CC0%20Royalty--Free-lightgrey.svg)](https://creativecommons.org/publicdomain/zero/1.0/)

**Fomodoro Revamped** (`com.arijit.pomodoro`) is a modern, high-precision Pomodoro timer for Android, built from the ground up to achieve 100% background reliability, drift-free timekeeping across process death and Doze mode, Android 16 Live Updates, and an expressive Material 3 user interface. This is a remake of the original Fomodoro app: https://www.github.com/Arijit-05/Fomodoro

---

## Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/4879c4e9-63d6-4ccc-9760-2fc143a2519b" alt="Fomodoro main timer screen" width="240" />
  <img src="https://github.com/user-attachments/assets/11901bd1-f207-4c83-89b1-bd6e46048b4e" alt="Fomodoro screen 4" width="240" />
  <img src="https://github.com/user-attachments/assets/a726d49f-ebbb-4700-a728-444d229b00cd" alt="Fomodoro screen 5" width="240" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/9b9f7474-d3e9-4d7b-a001-fffc8d8ea405" alt="Fomodoro screen 6" width="240" />
  <img src="https://github.com/user-attachments/assets/e06a60dc-35ad-4d0a-95fd-2cecf24c9fb8" alt="Fomodoro screen 7" width="240" />
  <img src="https://github.com/user-attachments/assets/ce7ddf4f-a258-4134-b16f-bd3a1595bf29" alt="Fomodoro screen 8" width="240" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/f371d37e-8dac-44e0-b445-09e3e69238e6" alt="Fomodoro screen 9" width="240" />
  <img src="https://github.com/user-attachments/assets/e4e53a26-9d96-413a-8e10-a9ceeec995a2" alt="Fomodoro screen 10" width="240" />
  <img src="https://github.com/user-attachments/assets/dd3a1284-64f4-4843-b035-7ac16fb00286" alt="Fomodoro screen 11" width="240" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/0739a738-e52c-4586-a473-62430bf9e956" alt="Fomodoro screen 12" width="240" />
  <img src="https://github.com/user-attachments/assets/2a875e46-8342-45d5-9cc4-65e43e009506" alt="Fomodoro screen 13" width="240" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/aa85bbd3-bbe7-402d-b06a-794c4adcd231" alt="Fomodoro compact screenshot 2" width="360" />
  <img src="https://github.com/user-attachments/assets/a77d2701-31af-49fd-b7c8-01716cb85a34" alt="Fomodoro compact screenshot 3" width="360" />
</p>

---

## 1. Architecture & Tech Stack

Fomodoro Revamped is engineered following modern Android architecture best practices with **Unidirectional Data Flow (UDF)** and strict separation of concerns:

- **Language & Runtime**: Kotlin 2.0.21, Coroutines, Flow.
- **UI Framework**: Jetpack Compose 2024.11.00 BOM, Material 3, dynamic theming, pure AMOLED black mode.
- **Dependency Injection**: Dagger Hilt 2.53.
- **Data Persistence**: Jetpack DataStore (Preferences) for reactive configuration and crash recovery; Room 2.6.1 for session history.
- **Audio Engine**: Jetpack Media3 (ExoPlayer 1.5.1) for background ambient noise and ticking with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` handling.
- **System Integration**: TargetSdk 36 (Android 16), Edge-to-Edge (`enableEdgeToEdge`), Android 16 Live Updates (`Notification.ProgressStyle`, `POST_PROMOTED_NOTIFICATIONS`, status chip countdowns).
- **Code Quality**: Zero deprecated APIs, clean lint, 100% unit test success.

---

## 2. Background Execution & Reliability

Fomodoro Revamped employs a multi-tiered background execution strategy that survives aggressive OEM battery killers:

1. **Ongoing Foreground Service (`TimerForegroundService`)**:
   - Declared with dual foreground service types: `mediaPlayback` (for ambient loops and ticking) and `specialUse` (with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification in `AndroidManifest.xml`).
   - Runs with `START_STICKY` and overrides `onTaskRemoved` to ensure swiping the app from Android Recents does not cancel active sessions.
   - Hosts the ongoing countdown notification driven by the system notification chronometer (`setUsesChronometer(true)`, `setChronometerCountDown(true)`), eliminating the need for periodic 1-second service wakeups.
2. **Kernel-Level Exact Alarms (`AlarmScheduler`)**:
   - Whenever a session starts or resumes, an exact alarm is scheduled via `AlarmManager.setExactAndAllowWhileIdle()` (or `setAlarmClock()`) targeted at `targetEndTimeElapsedRealtime`.
   - Uses `android.permission.USE_EXACT_ALARM`.
   - `AlarmReceiver` holds a 5-second `PARTIAL_WAKE_LOCK` upon wakeup, triggering the crash-proof alert runner and auto-starting subsequent phases even if the foreground service process was terminated by the OS under extreme memory pressure.
3. **Crash-Proof End-of-Phase Flow (`PhaseAlertManager`)**:
   - Every side effect (sound alert, haptic pattern, torch blinker, heads-up notification, Room DB record insertion) runs inside its own isolated `kotlin.runCatching { }` block. A hardware failure (e.g. torch in use or audio focus denied) cannot crash the app or prevent subsequent alerts from firing.
4. **OEM Guidance Deep Links (`BatteryOptimizationHelper`)**:
   - Includes tailored deep links in Settings to system battery optimization and auto-start settings across Xiaomi (MIUI/HyperOS), Samsung (One UI), Oppo/Realme/OnePlus (ColorOS/OxygenOS), Vivo (FuntouchOS), and Huawei (EMUI), alongside a direct link to [dontkillmyapp.com](https://dontkillmyapp.com).

---

## 3. Android 16 Live Updates Progressive Enhancement

On Android 16+ (API 36 / 37), Fomodoro Revamped elevates ongoing timer notifications into platform-level Live Updates:

- **Permission**: `android.permission.POST_PROMOTED_NOTIFICATIONS`.
- **Status Bar Chip**: Displays a live countdown on the status bar using `setShortCriticalText("Focus")` or `setShortCriticalText("Break")`.
- **Segmented Progress (`Notification.ProgressStyle`)**: Renders native progress segments corresponding to each focus session and break within the cycle, accompanied by boundary points and real-time elapsed phase progress.
- **Swipe-to-Dismiss Resilience**: Implements `ACTION_LIVE_UPDATE_DISMISSED` so user-dismissed chips do not nag the user until the next phase change.
- **Graceful Fallback**: Older devices (Android 8.0 - 15) automatically receive the standard low-importance chronometer notification without reflection or runtime errors.

---

## 4. Audio Assets & CC0 License

All audio bundled in `app/src/main/res/raw/` is **100% CC0 Royalty-Free** synthesized procedural audio, authored directly for Fomodoro Revamped. No third-party copyrighted samples are used.

| Resource Name | Type | Description | License |
| :--- | :--- | :--- | :--- |
| `ambient_brown_noise.wav` | Ambient Loop | Deep low-pass filtered brown noise for focus | CC0 Public Domain |
| `ambient_rain.wav` | Ambient Loop | Soothing binaural rainfall texture | CC0 Public Domain |
| `ambient_ocean.wav` | Ambient Loop | Rhythmic rolling ocean waves | CC0 Public Domain |
| `ambient_forest.wav` | Ambient Loop | Gentle woodland canopy atmosphere | CC0 Public Domain |
| `ambient_fireplace.wav` | Ambient Loop | Warm hearth crackle and ambient rumble | CC0 Public Domain |
| `ambient_cafe.wav` | Ambient Loop | Subtle diffused coffee-shop ambient murmur | CC0 Public Domain |
| `tick_classic.mp3` | Ticking Beat | Authentic classic analog clock tick | CC0 Public Domain |
| `tick_mechanical.mp3` | Ticking Beat | Crisp analog clock pendulum escapement | CC0 Public Domain |
| `tick_soft.mp3` | Ticking Beat | Subdued acoustic gentle tick | CC0 Public Domain |
| `chime_focus_end.wav` | Completion Chime | Dual-tone ascending bell (523 Hz -> 659 Hz) | CC0 Public Domain |
| `chime_break_end.wav` | Completion Chime | Triple-tone descending melodic chime | CC0 Public Domain |

Users may also import their own ambient audio loops and ringtones using the Storage Access Framework (SAF) file picker with persisted read URI permissions.

---

## 5. Complete Issue-to-Fix Matrix

(From the original Fomodoro issues)

| Issue | Category | Original Root Cause (Files & Lines) | Revamped Fix |
| :--- | :--- | :--- | :--- |
| **#37, #34, #22** | Crash | `CountDownTimer.onFinish()` called fragment transaction `.commit()` after `onSaveInstanceState` when backgrounded; `requireContext()` threw `IllegalStateException` on detached fragments; unhandled torch and MediaPlayer exceptions. | All side effects (sound, haptics, torch, notifications, DB) run in isolated `kotlin.runCatching { }` blocks in `PhaseAlertManager`. Zero fragment transaction dependencies. |
| **#53** | Bug | Settings button disabled during timer run (`btnSettings.isEnabled = false`); modifying settings corrupted running phase. | Settings is ALWAYS accessible via top bar. Active sessions show an informative "Applies to next session" badge. Settings update `pendingSettings` in `TimerStateMachine`, preserving active session integrity. |
| **#51** | Feature | Ultra Focus locked the device into fullscreen landscape with no exit mechanism. | Added three guaranteed escape paths: (1) press-and-hold exit button with live progress ring and ramping haptics; (2) "Exit Ultra Focus" action in the ongoing notification; (3) automatic exit upon session completion. |
| **#19** | Bug | `keepScreenAwake` held a `WakeLock` in `SettingsActivity.onDestroy()`, never updating the running timer activity. | Clean lifecycle-aware `FLAG_KEEP_SCREEN_ON` in `MainScreen` tied strictly to active `FOCUS` sessions and released on pause/break/dispose. |
| **#16** | Bug | Background noise instantiated `MediaPlayer` in `MainActivity` and stopped in `onDestroy()`. | Audio migrated to Jetpack Media3 (`ExoPlayer`) hosted inside `TimerForegroundService` with AudioFocus handling, gapless looping, and screen-off execution. |
| **#21** | UI Polish | Digits used proportional fonts without tabular figures, causing the clock to horizontally jump every second. | Countdown text uses font feature `"tnum"` and fixed-width tabular digit components (`TabularTimerText`). |
| **#43** | Feature | Short break was hardcoded to 5 minutes with rigid pickers. | Short breaks configurable up to 60 minutes; Focus up to 120 minutes. Supported via both smooth slider and exact numeric text inputs. |
| **#50** | Feature | No way to delay phase transition when not ready. | One-tap snooze (+1m, +3m, +5m) available directly on the phase-ended screen and as a notification action. |
| **#36** | Feature | No ticking audio option. | Media3 ExoPlayer ticking engine with selectable styles (`mechanical`, `digital`, `soft`), volume control, and focus-only toggle. |
| **#11** | Feature | Hardcoded system notification beeps. | Dual sound pickers: system ringtone picker (`RingtoneManager`) and custom audio file picker (`SAF`) with persisted read permissions. |
| **#42** | Feature | Monotonous single background noise. | 6 bundled CC0 ambient loops (Rain, Forest, Ocean, Fireplace, Brown Noise, Cafe) plus custom SAF audio import. |
| **#49** | Feature | No physical visual alerts for hearing-impaired users. | Flash notification blinking device torch via `CameraManager.setTorchMode` with screen flash fallback. |
| **#48** | Feature | Break screen was empty and repetitive. | Dynamic cycling/randomized break prompts ("Stretch your arms", "Drink water", "Deep breaths") on screen and notification. |
| **#20** | Feature | Screen-on mode drained battery and caused OLED burn-in. | AMOLED AOD Mode (`AodScreen`) with pure black background, 1Hz low-power redraw, and 60-second content pixel shifting. |
| **#46** | Platform | Incompatible with Android 14/15/16 background restrictions. | TargetSdk 36 compliance: dual foreground service types, edge-to-edge support, predictive back, Android 16 Live Updates. |
| **#47** | Polish | Hardcoded English strings throughout UI. | Zero hardcoded strings. All user-facing strings centralized in `strings.xml`. Locales config in `res/xml/locales_config.xml` and complete `TRANSLATING.md`. |
| **#24** | Polish | Outdated launcher icons. | Modern adaptive launcher icon with monochrome layer (`ic_launcher_monochrome.xml`) and themed status bar notification icon. |
