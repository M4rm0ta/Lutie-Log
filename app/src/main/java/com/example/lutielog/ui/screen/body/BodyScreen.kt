package com.example.lutielog.ui.screen.body

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lutielog.data.entity.BodyWeight
import com.example.lutielog.data.repository.WorkoutRepository
import com.example.lutielog.ui.theme.IOSSystemBlue
import com.example.lutielog.ui.theme.IOSSystemGray6
import com.example.lutielog.ui.theme.IOSSystemGreen
import com.example.lutielog.ui.theme.IOSSystemRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BodyScreen(repository: WorkoutRepository) {
    val viewModel: BodyViewModel = viewModel(factory = BodyViewModel.provideFactory(repository))
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题 + 添加
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "身体数据",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IOSSystemBlue)
                        .clickable { viewModel.showAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "记录体重", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            if (state.weights.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有体重记录", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "点右上角 + 记录第一次体重",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { CurrentWeightCard(state) }
                    item { WeightTrendCard(state.chartPoints) }
                    item {
                        WeightHistoryCard(
                            weights = state.weights,
                            onDelete = { viewModel.delete(it) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        if (state.showAddDialog) {
            AddWeightDialog(
                weightInput = state.weightInput,
                fatInput = state.fatInput,
                onWeightChange = viewModel::updateWeightInput,
                onFatChange = viewModel::updateFatInput,
                onSave = viewModel::save,
                onDismiss = viewModel::dismissAdd
            )
        }
    }
}

@Composable
private fun CurrentWeightCard(state: BodyUiState) {
    val latest = state.weights.first()
    val previous = state.weights.getOrNull(1)
    val diff = if (previous != null) latest.weightKg - previous.weightKg else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("当前体重", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        String.format("%.1f", latest.weightKg),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(" kg", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                }
                if (latest.bodyFatPct > 0) {
                    Text("体脂率 ${String.format("%.1f", latest.bodyFatPct)}%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (diff != null) {
                val (changeColor, changeText) = when {
                    kotlin.math.abs(diff) < 0.05 -> MaterialTheme.colorScheme.onSurfaceVariant to "持平"
                    diff > 0 -> IOSSystemRed to "↑ ${String.format("%.1f", diff)}kg"
                    else -> IOSSystemGreen to "↓ ${String.format("%.1f", -diff)}kg"
                }
                Text(changeText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = changeColor)
            }
        }
    }
}

@Composable
private fun WeightTrendCard(points: List<BodyWeight>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("体重趋势", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            if (points.size < 2) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("再记录一次即可看到趋势", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                WeightTrendChart(points = points, modifier = Modifier.fillMaxWidth().height(160.dp))
            }
        }
    }
}

@Composable
private fun WeightTrendChart(points: List<BodyWeight>, modifier: Modifier) {
    val weights = points.map { it.weightKg }
    val minW = weights.minOrNull() ?: 0.0
    val maxW = weights.maxOrNull() ?: 0.0
    val range = (maxW - minW).coerceAtLeast(1.0)
    val low = minW - range * 0.2
    val high = maxW + range * 0.2
    val fillBrush = Brush.verticalGradient(
        listOf(IOSSystemBlue.copy(alpha = 0.22f), IOSSystemBlue.copy(alpha = 0.0f))
    )

    Canvas(modifier = modifier) {
        val n = points.size
        val stepX = size.width / (n - 1).coerceAtLeast(1)
        fun yOf(w: Double): Float = ((high - w) / (high - low) * size.height).toFloat()

        val linePath = Path().apply {
            moveTo(0f, yOf(weights.first()))
            points.forEachIndexed { i, p -> lineTo(i * stepX, yOf(p.weightKg)) }
        }
        val areaPath = Path().apply {
            moveTo(0f, yOf(weights.first()))
            points.forEachIndexed { i, p -> lineTo(i * stepX, yOf(p.weightKg)) }
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(areaPath, brush = fillBrush)
        drawPath(
            linePath,
            color = IOSSystemBlue,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        points.forEachIndexed { i, p ->
            val center = Offset(i * stepX, yOf(p.weightKg))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = center)
            drawCircle(IOSSystemBlue, radius = 3.5.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun WeightHistoryCard(weights: List<BodyWeight>, onDelete: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                "历史记录",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )
            weights.forEach { rec ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(formatDate(rec.measuredAt), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        val sub = if (rec.bodyFatPct > 0)
                            "体脂 ${String.format("%.1f", rec.bodyFatPct)}%" else ""
                        if (sub.isNotEmpty()) {
                            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        String.format("%.1f kg", rec.weightKg),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IOSSystemBlue
                    )
                    IconButton(onClick = { onDelete(rec.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddWeightDialog(
    weightInput: String,
    fatInput: String,
    onWeightChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录体重", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = onWeightChange,
                    label = { Text("体重 (kg)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = fatInput,
                    onValueChange = onFatChange,
                    label = { Text("体脂率 (%) · 选填") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("保存", color = IOSSystemBlue, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatDate(ts: Long): String {
    return SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(ts))
}
