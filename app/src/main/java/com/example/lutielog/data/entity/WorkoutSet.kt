package com.example.lutielog.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String = "",
    val setIndex: Int = 1,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationSec: Int = 0, // 持续时间（秒）：支撑类直接是秒，有氧由分钟换算
    val recordType: String = "weight", // "weight" 或 "time"（大类，用于统计口径）
    val setType: String = "normal", // v7：normal 正式 | warmup 热身 | drop 递减 | restpause 休息暂停
    // v5：有氧/体能多指标（选填，不用时为 0）
    val distanceM: Double = 0.0,       // 距离（米）：跑步机/单车 km、农夫行走 m
    val speed: Double = 0.0,          // 速度 km/h
    val incline: Double = 0.0,        // 坡度 %
    val resistanceLevel: Int = 0,     // 阻力档位
    val calories: Int = 0,            // 卡路里 kcal
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
