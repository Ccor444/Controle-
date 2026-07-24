package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TvDao {
    @Query("SELECT * FROM saved_tvs ORDER BY lastConnected DESC")
    fun getAllSavedTvs(): Flow<List<TvEntity>>

    @Query("SELECT * FROM saved_tvs WHERE id = :id LIMIT 1")
    suspend fun getTvById(id: String): TvEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tv: TvEntity)

    @Query("DELETE FROM saved_tvs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM saved_tvs")
    suspend fun deleteAll()
}
