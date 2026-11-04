package com.example.lutielog.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lutielog.data.repository.WorkoutRepository
import com.example.lutielog.ui.theme.IOSSystemBlue
import com.example.lutielog.ui.theme.IOSSystemGray6
import com.example.lutielog.ui.theme.IOSSystemGreen
import com.example.lutielog.ui.theme.IOSSystemOrange
import com.example.lutielog.ui.theme.IOSSystemRed
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import androidx.compose.ui.graphics.SolidColor

@Composable
fun StatsScreen(repository: WorkoutRepository) {
    val viewModel: StatsViewModel = viewModel(factory = StatsViewModel.provideFactory(repository))
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "数据统计",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            if (state.isEmpty) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("完成训练后这里会生成统计", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("先去记录一次训练吧", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 概览卡片 2x2
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OverviewCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.FitnessCenter,
                            tint = IOSSystemBlue,
                            value = "${state.totalSessions}",
                            label = "总训练次数"
                        )
                        OverviewCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Schedule,
                            tint = IOSSystemGreen,
                            value = formatHours(state.totalDurationMin),
                            label = "总训练时长"
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OverviewCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalFireDepartment,
                            tint = IOSSystemOrange,
                            value = "${String.format("%.0f", state.totalVolume / 1000)}k",
                            label = "总训练量 kg"
                        )
                        OverviewCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.EmojiEvents,
                            tint = IOSSystemRed,
                            value = "${state.totalSets}",
                            label = "总完成组数"
                        )
                    }
                }

                // 近7天训练量柱状图
                item {
                    WeeklyVolumeCard(weekVolume = state.weekVolume, weekSessions = state.weekSessions)
                }

                // 年度训练热力图
                item {
                    HeatmapCard(weeks = state.heatmapWeeks)
                }

                // 肌肉分析
                if (state.muscleAnalysis.isNotEmpty()) {
                    item {
                        MuscleAnalysisCard(analysis = state.muscleAnalysis)
                    }
                }

                // 部位分布
                if (state.muscleDistribution.isNotEmpty()) {
                    item {
                        MuscleDistributionCard(distribution = state.muscleDistribution)
                    }
                }

                // 个人纪录
                if (state.personalRecords.isNotEmpty()) {
                    item {
                        PersonalRecordCard(records = state.personalRecords)
                    }
                }

                // 估算 1RM
                if (state.estimatedOneRms.isNotEmpty()) {
                    item {
                        EstimatedOneRmCard(records = state.estimatedOneRms)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyVolumeCard(weekVolume: List<DayVolume>, weekSessions: Int) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = weekVolume.map { it.dayLabel }

    LaunchedEffect(weekVolume) {
        modelProducer.runTransaction {
            columnModel {
                series(weekVolume.map { it.volume })
            }
        }
    }

    val bottomFormatter = CartesianValueFormatter { _, value, _ ->
        val idx = value.toInt()
        if (idx in labels.indices) labels[idx] else ""
    }

    val column = rememberLineComponent(
        fill = Fill(SolidColor(IOSSystemBlue)),
        thickness = 14.dp
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("近 7 天训练量 (kg)", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("本周 $weekSessions 练", fontSize = 13.sp, color = IOSSystemBlue, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(column)
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        valueFormatter = CartesianValueFormatter { _, value, _ ->
                            formatAxisVolume(value)
                        }
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter)
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
        }
    }
}

@Composable
private fun MuscleDistributionCard(distribution: List<MuscleStat>) {
    val colorMap = mapOf(
        "胸" to IOSSystemBlue,
        "背" to IOSSystemGreen,
        "腿" to IOSSystemOrange,
        "肩" to IOSSystemRed,
        "手臂" to Color(0xFFAF52DE),
        "核心" to Color(0xFFFF9500)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("部位训练分布", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(14.dp))
            distribution.forEach { stat ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stat.muscleGroup, fontSize = 14.sp, modifier = Modifier.width(44.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(IOSSystemGray6)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(stat.ratio.coerceIn(0.02f, 1f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(colorMap[stat.muscleGroup] ?: IOSSystemBlue)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${stat.setCount}组", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                }
            }
        }
    }
}

@Composable
private fun PersonalRecordCard(records: List<PersonalRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("个人纪录 PR", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            records.take(10).forEach { record ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(record.exerciseName, fontSize = 15.sp)
                    Text(
                        "${String.format("%.1f", record.maxWeight)} kg × ${record.reps}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IOSSystemBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(weeks: List<HeatmapWeek>) {
    val maxVol = remember(weeks) {
        weeks.flatMap { it.days }.filterNotNull().maxOfOrNull { it.volume } ?: 1.0
    }
    val trainedDays = weeks.flatMap { it.days }.filterNotNull().count { it.trained }
    // 初始定位到最右侧（最近一周）
    val scrollState = rememberScrollState(initial = Int.MAX_VALUE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("近一年训练", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("已练 $trainedDays 天", fontSize = 13.sp, color = IOSSystemBlue, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                weeks.forEach { week ->
                    Column(
                        modifier = Modifier.padding(end = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        week.days.forEach { day ->
                            if (day == null) {
                                Box(modifier = Modifier.size(11.dp))
                            } else {
                                val cellColor = when {
                                    !day.trained -> Color(0xFFEDEFF4)
                                    else -> {
                                        val ratio = (day.volume / maxVol).toFloat().coerceIn(0.12f, 1f)
                                        IOSSystemBlue.copy(alpha = 0.22f + 0.78f * ratio)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(cellColor)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("少", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.5.dp)).background(Color(0xFFEDEFF4)))
                Spacer(modifier = Modifier.width(3.dp))
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.5.dp)).background(IOSSystemBlue.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.width(3.dp))
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.5.dp)).background(IOSSystemBlue.copy(alpha = 0.7f)))
                Spacer(modifier = Modifier.width(3.dp))
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.5.dp)).background(IOSSystemBlue))
                Spacer(modifier = Modifier.width(6.dp))
                Text("多（训练量）", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EstimatedOneRmCard(records: List<Estimated1RM>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("估算 1RM", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "按 Epley 公式估算单次最大重量，仅供参考",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            records.take(12).forEach { rec ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rec.exerciseName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "来自 ${String.format("%.1f", rec.weightKg)}kg × ${rec.reps}次",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${String.format("%.1f", rec.estimatedOneRm)} kg",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = IOSSystemBlue
                    )
                }
            }
        }
    }
}

// Y 轴训练量刻度：小数值直接显示整数，上千缩写为 k，避免数据较小时全部取整成 0
private fun formatAxisVolume(value: Double): String = when {
    value >= 100000 -> String.format("%.0fk", value / 1000)
    value >= 1000 -> String.format("%.1fk", value / 1000)
    else -> String.format("%.0f", value)
}

private fun formatHours(totalMin: Int): String {
    return when {
        totalMin >= 60 -> {
            val h = totalMin / 60
            val m = totalMin % 60
            if (m > 0) "${h}时${m}分" else "${h}小时"
        }
        totalMin > 0 -> "${totalMin}分"
        else -> "0分"
    }
}
