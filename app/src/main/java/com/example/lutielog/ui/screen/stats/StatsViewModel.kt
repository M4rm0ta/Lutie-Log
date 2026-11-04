package com.example.lutielog.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lutielog.data.entity.Exercise
import com.example.lutielog.data.entity.WorkoutSession
import com.example.lutielog.data.entity.WorkoutSet
import com.example.lutielog.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DayVolume(
    val dayLabel: String,   // 如 "周三" 或 "8/24"
    val volume: Double,
    val sessionCount: Int
)

data class MuscleStat(
    val muscleGroup: String,
    val setCount: Int,
    val ratio: Float
)

data class PersonalRecord(
    val exerciseName: String,
    val maxWeight: Double,
    val reps: Int
)

// 估算 1RM（Epley 公式）
data class Estimated1RM(
    val exerciseName: String,
    val estimatedOneRm: Double,
    val weightKg: Double,
    val reps: Int
)

// 年度热力图：某天是否训练及训练量
data class HeatmapDay(
    val dayStart: Long,
    val trained: Boolean,
    val volume: Double
)

data class HeatmapWeek(
    val weekStart: Long,
    val days: List<HeatmapDay?> // 周一到周日，未来日期为 null
)

// 肌肉分析：部位训练量与最近训练时间
data class MuscleAnalysis(
    val muscleGroup: String,
    val volume: Double,
    val setCount: Int,
    val lastTrainedAt: Long?
)

data class StatsUiState(
    val totalSessions: Int = 0,
    val totalDurationMin: Int = 0,
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val weekSessions: Int = 0,
    val weekVolume: List<DayVolume> = emptyList(),
    val muscleDistribution: List<MuscleStat> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val estimatedOneRms: List<Estimated1RM> = emptyList(),
    val heatmapWeeks: List<HeatmapWeek> = emptyList(),
    val muscleAnalysis: List<MuscleAnalysis> = emptyList(),
    val isEmpty: Boolean = true
)

class StatsViewModel(repository: WorkoutRepository) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.allSessions,
        repository.allSets,
        repository.allExercises
    ) { sessions, sets, exercises ->
        buildStats(sessions, sets, exercises)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )

    private fun buildStats(
        sessions: List<WorkoutSession>,
        sets: List<WorkoutSet>,
        exercises: List<Exercise>
    ): StatsUiState {
        val setsBySession = sets.groupBy { it.sessionId }
        // 有效会话：已正常结束，或虽未点结束但已记录训练组（容错遗留/进行中的会话）
        val valid = sessions.filter {
            it.endTime != null || (setsBySession[it.id]?.isNotEmpty() == true)
        }
        val validIds = valid.map { it.id }.toSet()

        // 时长：优先用会话记录，缺失时用「最后一组时间 - 开始时间」估算
        fun durationSecOf(s: WorkoutSession): Int =
            if (s.durationSec > 0) s.durationSec
            else (((setsBySession[s.id]?.maxOfOrNull { set -> set.createdAt } ?: s.startTime) - s.startTime)
                .coerceAtLeast(0L) / 1000).toInt()

        // 计入训练量的组：属于有效会话且排除热身组
        val countedSets = sets.filter { it.sessionId in validIds && it.setType != "warmup" }

        val totalSessions = valid.size
        val totalDurationMin = valid.sumOf { durationSecOf(it) } / 60
        val totalVolume = countedSets.sumOf { it.weightKg * it.reps }
        val totalSets = sets.count { it.sessionId in validIds }

        // 日历工具
        val dayMillis = 24L * 60 * 60 * 1000
        fun startOfDay(ts: Long): Long {
            val c = Calendar.getInstance().apply {
                timeInMillis = ts
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return c.timeInMillis
        }
        val todayStart = startOfDay(System.currentTimeMillis())

        // 每天的训练量（排除热身），用于周柱状与热力图
        val sessionStartTime = sessions.associate { it.id to it.startTime }
        val volumeByDay = HashMap<Long, Double>()
        countedSets.forEach { set ->
            val start = sessionStartTime[set.sessionId] ?: return@forEach
            val d = startOfDay(start)
            volumeByDay[d] = (volumeByDay[d] ?: 0.0) + set.weightKg * set.reps
        }

        // 近 7 天
        val weekStart = todayStart - 6 * dayMillis
        val weekDays = (0..6).map { offset ->
            val dayStart = weekStart + offset * dayMillis
            val labelCal = Calendar.getInstance().apply { timeInMillis = dayStart }
            val label = when (offset) {
                6 -> "今天"
                5 -> "昨天"
                else -> {
                    val weekDay = labelCal.get(Calendar.DAY_OF_WEEK)
                    listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[weekDay - 1]
                }
            }
            val count = valid.count { startOfDay(it.startTime) == dayStart }
            DayVolume(label, volumeByDay[dayStart] ?: 0.0, count)
        }
        val weekSessions = weekDays.sumOf { it.sessionCount }

        // 部位分布（按组数，含热身，反映部位使用频率）
        val exerciseMap = exercises.associateBy { it.id }
        val groupCount = HashMap<String, Int>()
        sets.forEach { set ->
            if (set.sessionId !in validIds) return@forEach
            val group = exerciseMap[set.exerciseId]?.muscleGroup ?: return@forEach
            groupCount[group] = (groupCount[group] ?: 0) + 1
        }
        val muscleOrder = listOf("胸", "背", "腿", "肩", "手臂", "核心", "有氧")
        val maxSets = (groupCount.values.maxOrNull() ?: 0).coerceAtLeast(1)
        val muscleDistribution = muscleOrder
            .filter { groupCount.containsKey(it) }
            .map { MuscleStat(it, groupCount[it] ?: 0, (groupCount[it] ?: 0).toFloat() / maxSets) }

        // 个人纪录：每个重量型动作的最大重量
        val personalRecords = sets
            .filter { it.recordType == "weight" && it.weightKg > 0 && it.sessionId in validIds }
            .groupBy { it.exerciseId }
            .mapNotNull { (_, groupSets) ->
                val best = groupSets.maxByOrNull { it.weightKg } ?: return@mapNotNull null
                PersonalRecord(best.exerciseName, best.weightKg, best.reps)
            }
            .sortedByDescending { it.maxWeight }
            .take(20)

        // 估算 1RM：仅 1~12 次的正式组，Epley 公式 w*(1+r/30)
        val estimatedOneRms = sets
            .filter {
                it.recordType == "weight" && it.weightKg > 0 &&
                        it.reps in 1..12 && it.setType != "warmup" && it.sessionId in validIds
            }
            .groupBy { it.exerciseId }
            .mapNotNull { (_, groupSets) ->
                val best = groupSets.maxByOrNull { epley(it.weightKg, it.reps) }
                    ?: return@mapNotNull null
                Estimated1RM(
                    best.exerciseName,
                    epley(best.weightKg, best.reps),
                    best.weightKg,
                    best.reps
                )
            }
            .sortedByDescending { it.estimatedOneRm }
            .take(20)

        // 年度热力图：本周一往前 52 周，共 53 周
        val todayCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        val daysSinceMonday = (todayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 周一=0 … 周日=6
        val thisMonday = todayStart - daysSinceMonday * dayMillis
        val firstMonday = thisMonday - 52 * 7 * dayMillis
        val heatmapWeeks = (0..52).map { weekOffset ->
            val monday = firstMonday + weekOffset * 7 * dayMillis
            val days = (0..6).map { dayOffset ->
                val d = monday + dayOffset * dayMillis
                if (d > todayStart) null
                else HeatmapDay(d, (volumeByDay[d] ?: 0.0) > 0, volumeByDay[d] ?: 0.0)
            }
            HeatmapWeek(monday, days)
        }

        // 肌肉分析：每个部位的训练量（排除热身）、组数、最近训练时间
        val analysisMap = LinkedHashMap<String, MuscleAnalysis>()
        sets.forEach { set ->
            if (set.sessionId !in validIds) return@forEach
            val group = exerciseMap[set.exerciseId]?.muscleGroup ?: return@forEach
            if (group == "有氧") return@forEach
            val prev = analysisMap[group]
            val addVol = if (set.setType != "warmup") set.weightKg * set.reps else 0.0
            val newLast = prev?.lastTrainedAt?.let { maxOf(it, set.createdAt) } ?: set.createdAt
            analysisMap[group] = MuscleAnalysis(
                muscleGroup = group,
                volume = (prev?.volume ?: 0.0) + addVol,
                setCount = (prev?.setCount ?: 0) + 1,
                lastTrainedAt = newLast
            )
        }
        val muscleAnalysis = muscleOrder.filter { analysisMap.containsKey(it) }.map { analysisMap[it]!! }

        return StatsUiState(
            totalSessions = totalSessions,
            totalDurationMin = totalDurationMin,
            totalVolume = totalVolume,
            totalSets = totalSets,
            weekSessions = weekSessions,
            weekVolume = weekDays,
            muscleDistribution = muscleDistribution,
            personalRecords = personalRecords,
            estimatedOneRms = estimatedOneRms,
            heatmapWeeks = heatmapWeeks,
            muscleAnalysis = muscleAnalysis,
            isEmpty = totalSessions == 0
        )
    }

    private fun epley(weight: Double, reps: Int): Double = weight * (1.0 + reps / 30.0)

    companion object {
        fun provideFactory(repository: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatsViewModel(repository) as T
                }
            }
    }
}
