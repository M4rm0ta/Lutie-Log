package com.example.lutielog.data

import com.example.lutielog.data.entity.WorkoutSet

/** 录入字段的输入类型 */
enum class MetricInputType { INT, DECIMAL }

/** 单个可录入指标的展示规约 */
data class MetricField(
    val key: String,
    val label: String,
    val unit: String,
    val type: MetricInputType
)

/**
 * 动作记录指标规约：Exercise.metrics 存一组有序 key，录入界面据此动态生成输入框。
 * 底层统一落到 WorkoutSet 的固定数值列。
 */
object MetricSpec {
    val fields: Map<String, MetricField> = mapOf(
        "weight" to MetricField("weight", "重量", "kg", MetricInputType.DECIMAL),
        "reps" to MetricField("reps", "次数", "次", MetricInputType.INT),
        "dur_sec" to MetricField("dur_sec", "时长", "秒", MetricInputType.INT),
        "dur_min" to MetricField("dur_min", "时长", "分钟", MetricInputType.DECIMAL),
        "dist_km" to MetricField("dist_km", "距离", "km", MetricInputType.DECIMAL),
        "dist_m" to MetricField("dist_m", "距离", "米", MetricInputType.INT),
        "speed" to MetricField("speed", "速度", "km/h", MetricInputType.DECIMAL),
        "incline" to MetricField("incline", "坡度", "%", MetricInputType.DECIMAL),
        "resistance" to MetricField("resistance", "阻力", "档", MetricInputType.INT),
        "calories" to MetricField("calories", "卡路里", "kcal", MetricInputType.INT)
    )

    fun keys(metrics: String): List<String> =
        metrics.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun field(key: String): MetricField? = fields[key]

    /** 去掉数值多余的小数尾零：60.0 -> "60"，8.5 -> "8.5" */
    fun trimNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
}

/** 一条组/单条记录的通用文字摘要，训练页与历史页共用 */
object SetSummary {
    private fun formatDuration(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return when {
            m > 0 && s > 0 -> "${m}分${s}秒"
            m > 0 -> "${m}分"
            else -> "${s}秒"
        }
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) "${MetricSpec.trimNum(meters / 1000)}km" else "${meters.toInt()}m"

    fun of(set: WorkoutSet): String {
        val parts = mutableListOf<String>()
        // 力量：重量×次数 用紧凑写法
        if (set.weightKg > 0 && set.reps > 0) {
            parts.add("${MetricSpec.trimNum(set.weightKg)}kg×${set.reps}")
        } else {
            if (set.weightKg > 0) parts.add("${MetricSpec.trimNum(set.weightKg)}kg")
            if (set.reps > 0) parts.add("${set.reps}次")
        }
        if (set.durationSec > 0) parts.add(formatDuration(set.durationSec))
        if (set.distanceM > 0) parts.add(formatDistance(set.distanceM))
        if (set.speed > 0) parts.add("${MetricSpec.trimNum(set.speed)}km/h")
        if (set.incline > 0) parts.add("坡度${MetricSpec.trimNum(set.incline)}%")
        if (set.resistanceLevel > 0) parts.add("${set.resistanceLevel}档")
        if (set.calories > 0) parts.add("${set.calories}kcal")
        return if (parts.isEmpty()) "未填写" else parts.joinToString(" · ")
    }
}
