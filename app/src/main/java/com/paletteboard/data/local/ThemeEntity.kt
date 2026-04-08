package com.paletteboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val serializedTheme: String,
    val isPreset: Boolean,
    val updatedAt: Long,
)
