package com.paletteboard.ui.main

import android.content.Intent

sealed interface MainUiEvent {
    data class LaunchIntent(val intent: Intent) : MainUiEvent
}
