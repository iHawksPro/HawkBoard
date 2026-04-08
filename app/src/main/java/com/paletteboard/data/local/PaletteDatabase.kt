package com.paletteboard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ThemeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PaletteDatabase : RoomDatabase() {
    abstract fun themeDao(): ThemeDao
}
