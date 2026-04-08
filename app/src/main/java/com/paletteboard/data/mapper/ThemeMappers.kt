package com.paletteboard.data.mapper

import com.paletteboard.data.local.ThemeEntity
import com.paletteboard.domain.model.Theme
import kotlinx.serialization.json.Json

fun ThemeEntity.toDomain(json: Json): Theme = json.decodeFromString<Theme>(serializedTheme)

fun Theme.toEntity(json: Json): ThemeEntity = ThemeEntity(
    id = id,
    name = name,
    serializedTheme = json.encodeToString(Theme.serializer(), this),
    isPreset = isPreset,
    updatedAt = updatedAt,
)
