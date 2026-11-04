package com.example.lutielog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_session")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val durationSec: Int = 0,
    val totalVolume: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
