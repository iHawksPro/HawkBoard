package com.paletteboard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.paletteboard.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.paletteSettingsDataStore by preferencesDataStore(name = "palette_settings")

class SettingsRepository(
    private val context: Context,
    private val json: Json,
) {
    private val settingsKey = stringPreferencesKey("user_settings_json")

    val settings: Flow<UserSettings> = context.paletteSettingsDataStore.data.map { preferences ->
        preferences[settingsKey]?.let { encoded ->
            json.decodeFromString<UserSettings>(encoded)
        } ?: UserSettings()
    }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        val current = settings.first()
        context.paletteSettingsDataStore.edit { preferences ->
            preferences[settingsKey] = json.encodeToString(UserSettings.serializer(), transform(current))
        }
    }
}
