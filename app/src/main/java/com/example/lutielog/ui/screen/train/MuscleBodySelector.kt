package com.example.lutielog.ui.screen.train

import android.graphics.Matrix
import android.graphics.Region
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lutielog.R
import com.example.lutielog.ui.theme.IOSSystemBlue
import com.example.lutielog.ui.theme.IOSSystemGray6
import kotlin.math.roundToInt

/**
 * 肌肉图采用 Android VectorDrawable 渲染（前端资源稳定显示），
 * 命中检测仍按 SVG 数据生成 Region，保证每个肌肉块都可点。
 */
private const val HIT_SCALE = 20f

private class PreparedBody(
    val regions: List<RegionData>
)

private class RegionData(
    val group: String,
    val hit: Region
)

private data class DrawableRegion(
    val group: String,
    val drawableId: Int
)

private val FRONT_REGIONS = listOf(
    DrawableRegion("胸", R.drawable.muscle_front_chest),
    DrawableRegion("核心", R.drawable.muscle_front_core),
    DrawableRegion("肩", R.drawable.muscle_front_shoulders),
    DrawableRegion("手臂", R.drawable.muscle_front_arms),
    DrawableRegion("腿", R.drawable.muscle_front_legs)
)

private val BACK_REGIONS = listOf(
    DrawableRegion("肩", R.drawable.muscle_back_shoulders),
    DrawableRegion("背", R.drawable.muscle_back_back),
    DrawableRegion("手臂", R.drawable.muscle_back_arms),
    DrawableRegion("腿", R.drawable.muscle_back_legs)
)

private fun parsePath(pathData: String, offsetX: Float): Path {
    // 当前数据无横向偏移；优先使用 Android 自带解析器，失败再退回自包含解析器。
    if (offsetX == 0f) {
        try {
            val parsed = PathParser().parsePathString(pathData).toPath()
            return parsed
        } catch (_: Throwable) {
            // 继续使用下方的轻量解析器
        }
    }
    return SvgPath.toCompose(pathData, offsetX)
}

private fun prepareBody(front: Boolean): PreparedBody {
    val offsetX = if (front) 0f else -BodyMuscleData.BACK_OFFSET_X
    val regionSrc = if (front) BodyMuscleData.FRONT else BodyMuscleData.BACK

    val clip = Region(
        0,
        0,
        (BodyMuscleData.VIEW_W * HIT_SCALE).toInt(),
        (BodyMuscleData.VIEW_H * HIT_SCALE).toInt()
    )
    val regions = regionSrc.groupBy { it.group }.map { (group, bodyRegions) ->
        val merged = Path()
        bodyRegions.forEach { br ->
            br.paths.forEach {
                merged.addPath(parsePath(it, offsetX))
            }
        }
        val hit = android.graphics.Path(merged.asAndroidPath()).apply {
            transform(Matrix().apply { setScale(HIT_SCALE, HIT_SCALE) })
        }
        val region = Region().apply { setPath(hit, clip) }
        RegionData(group, region)
    }
    return PreparedBody(regions)
}

@Composable
fun MuscleBodySelector(
    front: Boolean,
    selectedGroup: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prepared = remember(front) { prepareBody(front) }
    val regionList = if (front) FRONT_REGIONS else BACK_REGIONS
    val bodyDrawable = if (front) R.drawable.muscle_front_body else R.drawable.muscle_back_body

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(286.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(IOSSystemGray6.copy(alpha = 0.55f))
            .pointerInput(front) {
                detectTapGestures { offset ->
                    val scale = minOf(size.width / BodyMuscleData.VIEW_W, size.height / BodyMuscleData.VIEW_H)
                    val ox = (size.width - BodyMuscleData.VIEW_W * scale) / 2f
                    val oy = (size.height - BodyMuscleData.VIEW_H * scale) / 2f
                    val vx = (offset.x - ox) / scale
                    val vy = (offset.y - oy) / scale
                    val hx = (vx * HIT_SCALE).roundToInt()
                    val hy = (vy * HIT_SCALE).roundToInt()
                    prepared.regions
                        .firstOrNull { it.hit.contains(hx, hy) }
                        ?.let { onSelect(it.group) }
                }
            }
    ) {
        Image(
            painter = painterResource(bodyDrawable),
            contentDescription = "人体肌肉图",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        regionList.forEach { region ->
            val active = region.group == selectedGroup
            Image(
                painter = painterResource(region.drawableId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(
                        when {
                            active -> 1f
                            selectedGroup == null -> 0.75f
                            else -> 0.28f
                        }
                    ),
                contentScale = ContentScale.Fit
            )
        }

        if (selectedGroup != null) {
            Text(
                text = "$selectedGroup 部位",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IOSSystemBlue.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
