package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppraisalDao {
    @Query("SELECT * FROM saved_appraisals ORDER BY timestamp DESC")
    fun getAllAppraisals(): Flow<List<SavedAppraisal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppraisal(appraisal: SavedAppraisal): Long

    @Query("DELETE FROM saved_appraisals WHERE id = :id")
    suspend fun deleteAppraisal(id: Long)

    @Query("DELETE FROM saved_appraisals")
    suspend fun clearAll()
}
