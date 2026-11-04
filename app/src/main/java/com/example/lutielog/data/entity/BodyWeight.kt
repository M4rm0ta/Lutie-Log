package com.example.lutielog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_weight")
data class BodyWeight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val bodyFatPct: Double = 0.0, // 体脂率 %，不填为 0
    val note: String = "",
    val measuredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
