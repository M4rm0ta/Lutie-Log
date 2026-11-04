package com.example.lutielog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val type: String = "compound",
    val unit: String = "kg",
    val recordType: String = "weight", // "weight" = 重量+次数, "time" = 时间（统计大类）
    // v5：记录模式与动态字段
    val trackMode: String = "sets",    // "sets" 按组记录 | "single" 单条记录（有氧，不问组数）
    val metrics: String = "weight,reps", // 有序录入字段 key，逗号分隔，见 MetricSpec
    val restSec: Int = 90,   // v7：该动作单独的组间歇时长（秒）
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
