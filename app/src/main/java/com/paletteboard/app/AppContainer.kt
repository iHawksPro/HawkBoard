package com.paletteboard.app

import android.content.Context
import androidx.room.Room
import com.paletteboard.data.local.PaletteDatabase
import com.paletteboard.data.repository.RoomThemeRepository
import com.paletteboard.data.repository.SettingsRepository
import com.paletteboard.engine.emoji.EmojiCatalog
import com.paletteboard.engine.suggestion.SuggestionEngine
import com.paletteboard.engine.theme.ThemeManager
import com.paletteboard.ime.controller.KeyboardController
import com.paletteboard.ime.feedback.TypingFeedbackController
import com.paletteboard.updater.AppUpdateManager
import com.paletteboard.util.JsonConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val json = JsonConfig.default
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: PaletteDatabase = Room.databaseBuilder(
        applicationContext,
        PaletteDatabase::class.java,
        "paletteboard.db",
    ).fallbackToDestructiveMigration().build()

    val settingsRepository = SettingsRepository(applicationContext, json)
    val themeRepository = RoomThemeRepository(database.themeDao(), json)
    val emojiCatalog = EmojiCatalog(applicationContext)
    val suggestionEngine = SuggestionEngine(applicationContext)
    val themeManager = ThemeManager(themeRepository, settingsRepository)
    val typingFeedbackController = TypingFeedbackController(applicationContext)
    val appUpdateManager = AppUpdateManager(applicationContext, json)
    val keyboardController = KeyboardController(
        emojiCatalog = emojiCatalog,
        suggestionEngine = suggestionEngine,
    )

    init {
        applicationScope.launch {
            themeRepository.seedDefaultsIfEmpty()
        }
    }
}
