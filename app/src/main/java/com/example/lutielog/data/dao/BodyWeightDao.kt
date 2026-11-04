package com.example.lutielog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lutielog.data.entity.BodyWeight
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BodyWeight): Long

    @Query("SELECT * FROM body_weight ORDER BY measuredAt DESC")
    fun getAll(): Flow<List<BodyWeight>>

    @Query("SELECT * FROM body_weight ORDER BY measuredAt DESC")
    suspend fun getAllOnce(): List<BodyWeight>

    @Query("DELETE FROM body_weight WHERE id = :id")
    suspend fun delete(id: Long)
}
