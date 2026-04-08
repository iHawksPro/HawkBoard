package com.paletteboard.data.repository

import com.paletteboard.data.local.ThemeDao
import com.paletteboard.data.mapper.toDomain
import com.paletteboard.data.mapper.toEntity
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ThemeExportFormat
import com.paletteboard.engine.theme.DefaultThemes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface ThemeRepository {
    fun observeThemes(): Flow<List<Theme>>
    suspend fun getTheme(id: String): Theme?
    suspend fun saveTheme(theme: Theme)
    suspend fun deleteTheme(id: String)
    suspend fun seedDefaultsIfEmpty()
    fun exportTheme(theme: Theme): String
    fun importTheme(raw: String): ThemeExportFormat
}

class RoomThemeRepository(
    private val themeDao: ThemeDao,
    private val json: Json,
) : ThemeRepository {
    override fun observeThemes(): Flow<List<Theme>> = themeDao.observeAll().map { entities ->
        entities.map { it.toDomain(json) }
    }

    override suspend fun getTheme(id: String): Theme? = themeDao.findById(id)?.toDomain(json)

    override suspend fun saveTheme(theme: Theme) {
        themeDao.upsert(theme.toEntity(json))
    }

    override suspend fun deleteTheme(id: String) {
        themeDao.deleteById(id)
    }

    override suspend fun seedDefaultsIfEmpty() {
        themeDao.upsertAll(DefaultThemes.defaults.map { it.toEntity(json) })
    }

    override fun exportTheme(theme: Theme): String = json.encodeToString(
        ThemeExportFormat.serializer(),
        ThemeExportFormat(theme = theme),
    )

    override fun importTheme(raw: String): ThemeExportFormat = json.decodeFromString(raw)
}
