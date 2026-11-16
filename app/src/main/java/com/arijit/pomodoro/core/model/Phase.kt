package com.arijit.pomodoro.core.model

enum class Phase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK;

    val isBreak: Boolean get() = this != FOCUS
    val isFocus: Boolean get() = this == FOCUS
}

