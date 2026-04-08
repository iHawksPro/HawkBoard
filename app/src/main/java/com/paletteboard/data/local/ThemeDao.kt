package com.paletteboard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ThemeEntity?

    @Query("SELECT COUNT(*) FROM themes")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(theme: ThemeEntity)

    @Upsert
    suspend fun upsertAll(themes: List<ThemeEntity>)

    @Query("DELETE FROM themes WHERE id = :id")
    suspend fun deleteById(id: String)
}
