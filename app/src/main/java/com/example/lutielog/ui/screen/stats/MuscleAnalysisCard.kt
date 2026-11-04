package com.example.lutielog.ui.screen.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lutielog.R
import com.example.lutielog.ui.theme.IOSSystemBlue
import com.example.lutielog.ui.theme.IOSSystemOrange
import com.example.lutielog.ui.theme.IOSSystemRed

private val FRONT_PARTS = listOf(
    "胸" to R.drawable.muscle_front_chest,
    "核心" to R.drawable.muscle_front_core,
    "肩" to R.drawable.muscle_front_shoulders,
    "手臂" to R.drawable.muscle_front_arms,
    "腿" to R.drawable.muscle_front_legs
)

private val BACK_PARTS = listOf(
    "肩" to R.drawable.muscle_back_shoulders,
    "背" to R.drawable.muscle_back_back,
    "手臂" to R.drawable.muscle_back_arms,
    "腿" to R.drawable.muscle_back_legs
)

@Composable
fun MuscleAnalysisCard(analysis: List<MuscleAnalysis>) {
    var front by remember { mutableIntStateOf(1) } // 1=正面 0=背面
    var mode by remember { mutableIntStateOf(0) }  // 0=训练量 1=恢复中 2=久未练

    val now = System.currentTimeMillis()
    val heatColor = when (mode) {
        0 -> IOSSystemBlue
        1 -> IOSSystemOrange
        else -> IOSSystemRed
    }

    fun intensityOf(group: String): Float {
        val a = analysis.firstOrNull { it.muscleGroup == group } ?: return 0f
        return when (mode) {
            0 -> {
                val max = analysis.maxOf { it.volume }.coerceAtLeast(1.0)
                (a.volume / max).toFloat()
            }
            1 -> {
                // 恢复中：刚练（需恢复）色深，72h 后已恢复色浅；从未训练则无需恢复
                val last = a.lastTrainedAt ?: return 0f
                val hours = (now - last) / 3_600_000.0
                (1f - hours / 72f).toFloat().coerceIn(0f, 1f)
            }
            else -> {
                // 久未训练：越久没练色越深，满 7 天到顶；从未训练视为最久未练
                val last = a.lastTrainedAt ?: return 1f
                val days = (now - last) / 86_400_000.0
                (days / 7f).toFloat().coerceIn(0f, 1f)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("肌肉分析", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(12.dp))
            // 模式分段
            val modeLabels = listOf("训练量去向", "恢复中", "久未训练")
            SegmentedRow(
                options = modeLabels,
                selected = mode,
                onSelect = { mode = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            // 人体图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(286.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F3F7))
            ) {
                val bodyDrawable = if (front == 1)
                    R.drawable.muscle_front_body else R.drawable.muscle_back_body
                Image(
                    painter = painterResource(bodyDrawable),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(286.dp),
                    contentScale = ContentScale.Fit
                )
                val parts = if (front == 1) FRONT_PARTS else BACK_PARTS
                parts.forEach { (group, drawId) ->
                    val inten = intensityOf(group)
                    // 原始灰色肌肉（保留解剖细节）
                    Image(
                        painter = painterResource(drawId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(286.dp),
                        contentScale = ContentScale.Fit,
                        alpha = 0.5f
                    )
                    // 热力着色
                    if (inten > 0.02f) {
                        Image(
                            painter = painterResource(drawId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(286.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(heatColor, BlendMode.SrcIn),
                            alpha = 0.2f + 0.8f * inten
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // 正背面切换
            SegmentedRow(
                options = listOf("正面", "背面"),
                selected = if (front == 1) 0 else 1,
                onSelect = { front = if (it == 0) 1 else 0 },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            // 图例
            val (lowLabel, highLabel) = when (mode) {
                0 -> "少" to "多"
                1 -> "已恢复" to "刚练"
                else -> "近期" to "久未练"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(lowLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(heatColor.copy(alpha = 0.15f), heatColor)
                            )
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(highLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "肌肉图：Ryan Graves / CC BY 4.0",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SegmentedRow(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFEDEFF4))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSel = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(index) }
                    .background(if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .padding(vertical = 7.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSel) IOSSystemBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
