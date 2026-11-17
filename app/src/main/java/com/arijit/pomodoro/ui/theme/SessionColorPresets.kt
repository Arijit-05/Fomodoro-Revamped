package com.arijit.pomodoro.ui.theme

import androidx.compose.ui.graphics.Color

data class SessionColorPreset(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val lightBg: Color,
    val lightText: Color,
    val lightButtonBg: Color,
    val lightButtonContent: Color,
    val lightControlIcon: Color,
    val darkBg: Color,
    val darkText: Color,
    val darkButtonBg: Color,
    val darkButtonContent: Color,
    val darkControlIcon: Color
)

object SessionColorPresets {
    val presets = listOf(
        SessionColorPreset(
            id = "coral_red",
            name = "Coral Red",
            primaryColor = Color(0xFFFF7C7C),
            lightBg = Color(0xFFFFF2F2),
            lightText = Color(0xFF471515),
            lightButtonBg = Color(0xFFFF7C7C),
            lightButtonContent = Color(0xFF471515),
            lightControlIcon = Color(0xFF471515),
            darkBg = Color(0xFF241616),
            darkText = Color(0xFFFFF2F2),
            darkButtonBg = Color(0xFFFF7C7C),
            darkButtonContent = Color(0xFF471515),
            darkControlIcon = Color(0xFFFF7C7C)
        ),
        SessionColorPreset(
            id = "peach_orange",
            name = "Peach Orange",
            primaryColor = Color(0xFFFF9E6C),
            lightBg = Color(0xFFFFF6EE),
            lightText = Color(0xFF4A250E),
            lightButtonBg = Color(0xFFFF9E6C),
            lightButtonContent = Color(0xFF4A250E),
            lightControlIcon = Color(0xFF4A250E),
            darkBg = Color(0xFF281910),
            darkText = Color(0xFFFFEDE0),
            darkButtonBg = Color(0xFFFF9E6C),
            darkButtonContent = Color(0xFF4A250E),
            darkControlIcon = Color(0xFFFF9E6C)
        ),
        SessionColorPreset(
            id = "amber_gold",
            name = "Amber Gold",
            primaryColor = Color(0xFFFFD15C),
            lightBg = Color(0xFFFFFDF0),
            lightText = Color(0xFF4A380A),
            lightButtonBg = Color(0xFFFFD15C),
            lightButtonContent = Color(0xFF4A380A),
            lightControlIcon = Color(0xFF4A380A),
            darkBg = Color(0xFF261F0E),
            darkText = Color(0xFFFFF4D4),
            darkButtonBg = Color(0xFFFFD15C),
            darkButtonContent = Color(0xFF4A380A),
            darkControlIcon = Color(0xFFFFD15C)
        ),
        SessionColorPreset(
            id = "mint_green",
            name = "Mint Green",
            primaryColor = Color(0xFF8CE8A1),
            lightBg = Color(0xFFF2FFF5),
            lightText = Color(0xFF14401D),
            lightButtonBg = Color(0xFF8CE8A1),
            lightButtonContent = Color(0xFF14401D),
            lightControlIcon = Color(0xFF14401D),
            darkBg = Color(0xFF14401D),
            darkText = Color(0xFFF2FFF5),
            darkButtonBg = Color(0xFF8CE8A1),
            darkButtonContent = Color(0xFF14401D),
            darkControlIcon = Color(0xFF8CE8A1)
        ),
        SessionColorPreset(
            id = "teal_sage",
            name = "Teal Sage",
            primaryColor = Color(0xFF6EDBC4),
            lightBg = Color(0xFFF0FAF8),
            lightText = Color(0xFF0F3830),
            lightButtonBg = Color(0xFF6EDBC4),
            lightButtonContent = Color(0xFF0F3830),
            lightControlIcon = Color(0xFF0F3830),
            darkBg = Color(0xFF10332D),
            darkText = Color(0xFFE2F7F3),
            darkButtonBg = Color(0xFF6EDBC4),
            darkButtonContent = Color(0xFF0F3830),
            darkControlIcon = Color(0xFF6EDBC4)
        ),
        SessionColorPreset(
            id = "sky_blue",
            name = "Sky Blue",
            primaryColor = Color(0xFF8BCAFF),
            lightBg = Color(0xFFD9EEFF),
            lightText = Color(0xFF153047),
            lightButtonBg = Color(0xFF8BCAFF),
            lightButtonContent = Color(0xFF153047),
            lightControlIcon = Color(0xFF153047),
            darkBg = Color(0xFF153047),
            darkText = Color(0xFFD9EEFF),
            darkButtonBg = Color(0xFF8BCAFF),
            darkButtonContent = Color(0xFF153047),
            darkControlIcon = Color(0xFF8BCAFF)
        ),
        SessionColorPreset(
            id = "deep_indigo",
            name = "Deep Indigo",
            primaryColor = Color(0xFFA3B3FF),
            lightBg = Color(0xFFEEF2FF),
            lightText = Color(0xFF1B1F4A),
            lightButtonBg = Color(0xFFA3B3FF),
            lightButtonContent = Color(0xFF1B1F4A),
            lightControlIcon = Color(0xFF1B1F4A),
            darkBg = Color(0xFF1B1D38),
            darkText = Color(0xFFE3E8FF),
            darkButtonBg = Color(0xFFA3B3FF),
            darkButtonContent = Color(0xFF1B1F4A),
            darkControlIcon = Color(0xFFA3B3FF)
        ),
        SessionColorPreset(
            id = "lavender_purple",
            name = "Lavender Purple",
            primaryColor = Color(0xFFD4B0F8),
            lightBg = Color(0xFFFAF5FF),
            lightText = Color(0xFF381A4E),
            lightButtonBg = Color(0xFFD4B0F8),
            lightButtonContent = Color(0xFF381A4E),
            lightControlIcon = Color(0xFF381A4E),
            darkBg = Color(0xFF281836),
            darkText = Color(0xFFF5EBFF),
            darkButtonBg = Color(0xFFD4B0F8),
            darkButtonContent = Color(0xFF381A4E),
            darkControlIcon = Color(0xFFD4B0F8)
        )
    )

    fun getPreset(id: String): SessionColorPreset {
        return presets.firstOrNull { it.id == id } ?: presets[0]
    }
}

