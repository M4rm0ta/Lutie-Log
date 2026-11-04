package com.example.lutielog.ui.screen.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lutielog.data.SetSummary
import com.example.lutielog.data.entity.WorkoutSession
import com.example.lutielog.data.entity.WorkoutSet
import com.example.lutielog.data.repository.WorkoutRepository
import com.example.lutielog.ui.theme.IOSSystemBlue
import com.example.lutielog.ui.theme.IOSSystemGray6
import com.example.lutielog.ui.theme.IOSSystemGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DayGroup(
    val dayKey: String,
    val dayLabel: String,
    val sessions: List<WorkoutSession>
)

@Composable
fun HistoryScreen(repository: WorkoutRepository) {
    val sessions by repository.allSessions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    // 按天分组
    val dayGroups = remember(sessions) { groupByDay(sessions) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "训练历史",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            if (dayGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有训练记录", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("去完成第一次训练吧", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dayGroups, key = { it.dayKey }) { group ->
                        DayCard(group = group, repository = repository, onDelete = { pendingDelete = it })
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        pendingDelete?.let { session ->
            val setCount = remember(session.id) { mutableStateOf<Int?>(null) }
            LaunchedEffect(session.id) { setCount.value = repository.getSetsBySessionOnce(session.id).size }
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除这次训练？", fontWeight = FontWeight.SemiBold) },
                text = {
                    Text(
                        "将删除该次训练及其全部 ${setCount.value ?: ""} 组记录，删除后不可恢复。",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { repository.deleteSession(session.id) }
                        pendingDelete = null
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消") }
                }
            )
        }
    }
}

private fun groupByDay(sessions: List<WorkoutSession>): List<DayGroup> {
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    val labelFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)
    return sessions
        .groupBy { keyFormat.format(Date(it.startTime)) }
        .map { (key, list) ->
            DayGroup(
                dayKey = key,
                dayLabel = labelFormat.format(Date(list.first().startTime)),
                sessions = list.sortedByDescending { it.startTime }
            )
        }
}

@Composable
private fun DayCard(group: DayGroup, repository: WorkoutRepository, onDelete: (WorkoutSession) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val totalDuration = group.sessions.sumOf { it.durationSec }
    val totalVolume = group.sessions.sumOf { it.totalVolume }
    val sessionCount = group.sessions.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 折叠头部
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(IOSSystemBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = IOSSystemBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.dayLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "$sessionCount 次训练 · ${formatDuration(totalDuration)} · ${String.format("%.0f", totalVolume)} kg",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 展开内容
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    group.sessions.forEachIndexed { index, session ->
                        SessionDetail(session = session, repository = repository, onDelete = onDelete)
                        if (index < group.sessions.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDetail(session: WorkoutSession, repository: WorkoutRepository, onDelete: (WorkoutSession) -> Unit) {
    var sets by remember(session.id) { mutableStateOf<List<WorkoutSet>>(emptyList()) }
    LaunchedEffect(session.id) {
        sets = repository.getSetsBySessionOnce(session.id)
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startTime = timeFormat.format(Date(session.startTime))
    // 容错：未正常结束的遗留会话，用最后一组时间补全结束时刻 / 时长 / 训练量
    val lastSetTime = sets.maxOfOrNull { it.createdAt }
    val endMs = session.endTime ?: lastSetTime
    val endTime = endMs?.let { timeFormat.format(Date(it)) } ?: "--:--"
    val durationSec = if (session.durationSec > 0) session.durationSec
    else endMs?.let { ((it - session.startTime).coerceAtLeast(0L) / 1000).toInt() } ?: 0
    val volume = if (session.totalVolume > 0) session.totalVolume
    else sets.sumOf { it.weightKg * it.reps }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$startTime - $endTime",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(IOSSystemGreen.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formatDuration(durationSec),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = IOSSystemGreen
                    )
                }
                IconButton(
                    onClick = { onDelete(session) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除这次训练",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "训练量 ${String.format("%.0f", volume)} kg · ${sets.size} 组",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 动作明细：按动作名分组
        if (sets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            sets.groupBy { it.exerciseName }.forEach { (name, exerciseSets) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(IOSSystemGray6)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(110.dp))
                    Text(
                        text = exerciseSets.joinToString("、") { setText(it) },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun setText(set: WorkoutSet): String = SetSummary.of(set)

private fun formatDuration(sec: Int): String {
    if (sec <= 0) return "0分钟"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return when {
        h > 0 && m > 0 -> "${h}小时${m}分钟"
        h > 0 -> "${h}小时"
        m > 0 -> "${m}分钟"
        else -> "不足1分钟"
    }
}
