package com.example.lutielog.ui.screen.train

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import com.example.lutielog.data.MetricInputType
import com.example.lutielog.data.MetricSpec
import com.example.lutielog.data.SetSummary
import com.example.lutielog.data.entity.Exercise
import com.example.lutielog.data.entity.WorkoutSet
import com.example.lutielog.data.repository.WorkoutRepository
import com.example.lutielog.ui.theme.IOSSystemBlue
import com.example.lutielog.ui.theme.IOSSystemGreen
import com.example.lutielog.ui.theme.IOSSystemGray6

@Composable
fun TrainScreen(repository: WorkoutRepository) {
    val viewModel: TrainViewModel = viewModel(factory = TrainViewModel.provideFactory(repository))
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.timerJustFinished) {
        if (state.timerJustFinished) {
            vibrate(context)
            viewModel.resetTimerFinishedFlag()
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // 触发组间休息时自动收起键盘，避免遮挡休息界面
    LaunchedEffect(state.timerRunning) {
        if (state.timerRunning) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    // 删除组后弹出撤销
    LaunchedEffect(state.pendingUndo) {
        if (state.pendingUndo != null) {
            val result = snackbarHostState.showSnackbar(
                message = "已删除该组",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.clearPendingUndo()
        }
    }

    // 回到前台时按真实时间校准组间歇倒计时，保证后台/灭屏期间计时不丢、到点正常震动
    DisposableEffect(Unit) {
        val owner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTimerOnResume()
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!state.isTraining) {
            NotTrainingContent(onStart = { viewModel.startTraining() })
        } else {
            TrainingContent(state = state, viewModel = viewModel)
        }

        if (state.showExercisePicker) {
            ExercisePickerDialog(
                exercises = viewModel.getFilteredExercises(),
                searchQuery = state.searchQuery,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onSelect = { viewModel.selectExercise(it) },
                onDismiss = { viewModel.closeExercisePicker() }
            )
        }

        if (state.showSetCountDialog && state.selectedExercise != null) {
            SetCountDialog(
                exerciseName = state.selectedExercise!!.name,
                input = state.setCountInput,
                onInputChange = { viewModel.updateSetCountInput(it) },
                onConfirm = { viewModel.confirmSetCount() },
                onDismiss = { viewModel.cancelSetCount() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * 震动：强提示，兼容各版本
 */
private fun vibrate(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 强双脉冲：震500ms-停200ms-震500ms，使用最大振幅
            val timings = longArrayOf(0, 500, 200, 500)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
        }
    } catch (e: Exception) {
        // 静默失败
    }
}

@Composable
private fun NotTrainingContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "准备好开始训练了吗？",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IOSSystemBlue)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始训练", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TrainingContent(state: TrainUiState, viewModel: TrainViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 已完成动作组
            items(state.completedGroups, key = { it.groupId }) { group ->
                CompletedGroupCard(group = group)
            }

            // 当前动作训练区
            val exercise = state.selectedExercise
            if (exercise != null) {
                item(key = "current") {
                    CurrentExerciseSection(state = state, viewModel = viewModel)
                }
            } else {
                // 没有当前动作：显示添加下一动作入口
                item(key = "add_next") {
                    AddNextExerciseSection(
                        completedCount = state.completedGroups.size,
                        onAddNext = { viewModel.addNextExercise() }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // 计时器
        if (state.timerRunning || state.timerSeconds > 0) {
            RestTimer(
                seconds = state.timerSeconds,
                isRunning = state.timerRunning,
                restSeconds = state.restSeconds,
                onAdjust = { s ->
                    viewModel.adjustRestSeconds(s)
                    // 正在休息时立即应用新时长
                    if (state.timerRunning) viewModel.startTimer(s)
                },
                onPause = { viewModel.pauseTimer() },
                onResume = { viewModel.startTimer(state.timerSeconds) },
                onSkip = { viewModel.skipTimer() }
            )
        }

        // 底部操作区：结束训练入口始终可达，避免做完直接退出导致会话未收尾
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (state.selectedExercise == null) {
                // 动作间隙：醒目的描边主按钮
                OutlinedButton(
                    onClick = { viewModel.finishTraining() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("结束本次训练", fontSize = 15.sp)
                }
            } else {
                // 动作进行中：低调文字按钮，直接按已记录的训练组收尾
                TextButton(
                    onClick = { viewModel.finishTraining() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "结束并保存本次训练",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedGroupCard(group: ExerciseGroup) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IOSSystemGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = IOSSystemGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.exercise.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${group.sets.size} 组 · " + group.sets.joinToString(" / ") { setSummary(it) },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    group.sets.forEach { set ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第${set.setIndex}组",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(text = setSummary(set), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

private fun setSummary(set: WorkoutSet): String = SetSummary.of(set)

@Composable
private fun CurrentExerciseSection(state: TrainUiState, viewModel: TrainViewModel) {
    val main = state.selectedExercise ?: return
    val partner = state.supersetPartner
    // 当前正在录入的动作（超级组时可能是伙伴）
    val exercise = if (state.activeExerciseId != null && state.activeExerciseId == partner?.id) partner else main
    val metricKeys = MetricSpec.keys(exercise.metrics)
    val isSingle = exercise.trackMode == "single"
    val isTimedSet = exercise.metrics == "dur_sec"
    val activeSets = state.currentSets.filter { it.exerciseId == exercise.id }
    val isLastSet = activeSets.isNotEmpty() &&
            (isSingle || activeSets.size >= state.targetSetCount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 动作标题 + 进度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.muscleGroup, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val badge = when {
                            partner != null -> "超级组"
                            isSingle -> "单条"
                            isTimedSet -> "计时"
                            else -> null
                        }
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(IOSSystemBlue.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(badge, fontSize = 10.sp, color = IOSSystemBlue, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                if (isSingle) {
                    Text(
                        text = if (activeSets.isEmpty()) "单条记录" else "已记录",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IOSSystemBlue
                    )
                } else {
                    Text(
                        text = "${activeSets.size}/${state.targetSetCount}组",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IOSSystemBlue
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // 进度条（单条有氧不显示组数进度）
            if (!isSingle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(IOSSystemGray6)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                if (state.targetSetCount > 0)
                                    (activeSets.size.toFloat() / state.targetSetCount).coerceIn(0f, 1f)
                                else 0f
                            )
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(IOSSystemBlue)
                    )
                }
            }

            // 超级组控制
            if (!isSingle) {
                Spacer(modifier = Modifier.height(10.dp))
                if (partner == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IOSSystemGray6)
                            .clickable { viewModel.startPickingPartner() }
                            .padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = IOSSystemBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("组成超级组（加一个伙伴动作交替做）", fontSize = 13.sp, color = IOSSystemBlue, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IOSSystemBlue.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = IOSSystemBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "超级组：${main.name} + ${partner.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.toggleActiveExercise() }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("切换", fontSize = 12.sp)
                        }
                        TextButton(onClick = { viewModel.removePartner() }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("取消", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 当前组记录（左滑删除、右滑复制）
            activeSets.forEach { set ->
                key(set.id) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                // 右滑：先接受滑动（currentValue 变为 StartToEnd），
                                // 真正的复制在下面 LaunchedEffect 中只做一次，避免手势过程中 confirmValueChange 被反复触发导致重复复制
                                SwipeToDismissBoxValue.StartToEnd -> true
                                SwipeToDismissBoxValue.EndToStart -> { viewModel.deleteSet(set.id); true }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )
                    // 右滑复制：currentValue 每次到达 StartToEnd 只复制一次，随后弹回原位
                    LaunchedEffect(dismissState) {
                        snapshotFlow { dismissState.currentValue }
                            .filter { it == SwipeToDismissBoxValue.StartToEnd }
                            .collect {
                                viewModel.copySet(set)
                                dismissState.reset()
                            }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val (bgColor, bgIcon, bgAlign) = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Triple(IOSSystemBlue, Icons.Default.ContentCopy, Alignment.CenterStart)
                                SwipeToDismissBoxValue.EndToStart -> Triple(Color(0xFFFF3B30), Icons.Default.Delete, Alignment.CenterEnd)
                                else -> Triple(Color.Transparent, null, Alignment.Center)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = bgAlign
                            ) {
                                if (bgIcon != null) Icon(bgIcon, contentDescription = null, tint = Color.White)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val typeColor = when (set.setType) {
                                "warmup" -> Color(0xFFFF9500)
                                "drop" -> Color(0xFFAF52DE)
                                else -> IOSSystemBlue
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(typeColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${set.setIndex}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = typeColor)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (set.setType != "normal") {
                                val typeLabel = if (set.setType == "warmup") "热身" else "递减"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(typeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(typeLabel, fontSize = 9.sp, color = typeColor, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(setSummary(set), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteSet(set.id) }, modifier = Modifier.size(28.dp)) {
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

            if (activeSets.isEmpty()) {
                Text(
                    text = if (isSingle) "填写本次数据后保存（各项选填）" else "填写数据，完成第一组",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 组类型选择 + 动态指标录入（单条保存后隐藏录入）
            if (!isSingle || activeSets.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // 组类型分段
                val typeOptions = listOf("normal" to "正式", "warmup" to "热身", "drop" to "递减")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(IOSSystemGray6)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    typeOptions.forEach { (typeKey, typeLabel) ->
                        val selected = state.setType == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { viewModel.setSetType(typeKey) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                typeLabel,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) IOSSystemBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                metricKeys.chunked(2).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val field = MetricSpec.field(key) ?: return@forEach
                            OutlinedTextField(
                                value = state.metricInputs[key] ?: "",
                                onValueChange = { viewModel.updateMetric(key, it) },
                                label = { Text("${field.label} (${field.unit})") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (field.type == MetricInputType.INT)
                                        KeyboardType.Number else KeyboardType.Decimal
                                )
                            )
                        }
                        if (rowKeys.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            if (isLastSet && activeSets.isNotEmpty()) {
                Button(
                    onClick = { viewModel.finishExercise() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IOSSystemGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("完成该动作", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                // 达到目标后仍允许追加组
                if (!isSingle) {
                    TextButton(
                        onClick = { viewModel.addSet() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("再加一组", fontSize = 14.sp)
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.addSet() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IOSSystemBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSingle) "保存记录" else "完成本组", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AddNextExerciseSection(completedCount: Int, onAddNext: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddNext() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IOSSystemBlue.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = IOSSystemBlue, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (completedCount > 0) "添加下一个动作" else "选择第一个动作",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = IOSSystemBlue
            )
        }
    }
}

@Composable
private fun RestTimer(
    seconds: Int,
    isRunning: Boolean,
    restSeconds: Int,
    onAdjust: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit
) {
    val minutes = seconds / 60
    val secs = seconds % 60
    val timeText = String.format("%02d:%02d", minutes, secs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IOSSystemBlue.copy(alpha = 0.08f))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("组间休息", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timeText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IOSSystemBlue)
            }
            Row {
                if (isRunning) {
                    TextButton(onClick = onPause) { Text("暂停") }
                } else {
                    TextButton(onClick = onResume) { Text("继续") }
                }
                TextButton(onClick = onSkip) { Text("跳过") }
            }
        }
        // 休息时长快捷选择（按动作记忆）
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(60, 90, 120, 180).forEach { opt ->
                val selected = restSeconds == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) IOSSystemBlue else MaterialTheme.colorScheme.surface)
                        .clickable { onAdjust(opt) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val label = when {
                        opt < 60 -> "${opt}秒"
                        opt == 90 -> "1分半"
                        opt % 60 == 0 -> "${opt / 60}分钟"
                        else -> "${opt / 60}分${opt % 60}秒"
                    }
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val grouped = exercises.groupBy { it.muscleGroup }
    val muscleOrder = listOf("胸", "背", "腿", "肩", "手臂", "核心", "有氧")
    val sortedGroups = muscleOrder.filter { it in grouped } + grouped.keys.filterNot { it in muscleOrder }

    // 双模式：肌肉图 / 列表；肌肉图内部前后面与下钻部位
    var pickerMode by remember { mutableStateOf("body") }
    var frontView by remember { mutableStateOf(true) }
    var bodyGroup by remember { mutableStateOf<String?>(null) }

    val searching = searchQuery.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择动作", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("搜索动作或部位") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(10.dp))

                when {
                    // 1) 搜索：平铺结果，两种模式通用
                    searching -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                            items(exercises, key = { "s_${it.id}" }) { ex ->
                                ExerciseRow(exercise = ex, onSelect = onSelect)
                            }
                        }
                    }

                    // 2) 肌肉图模式
                    pickerMode == "body" -> {
                        // 点击肌肉后短暂高亮再下钻，让用户看到选中反馈
                        var tappedGroup by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(tappedGroup) {
                            val g = tappedGroup
                            if (g != null) {
                                delay(240)
                                bodyGroup = g
                                tappedGroup = null
                            }
                        }
                        val group = bodyGroup
                        if (group == null) {
                            // 前/后面切换
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                listOf(true to "正面", false to "背面").forEach { (isFront, label) ->
                                    val active = frontView == isFront
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) IOSSystemBlue else IOSSystemGray6)
                                            .clickable { frontView = isFront }
                                            .padding(horizontal = 20.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            MuscleBodySelector(
                                front = frontView,
                                selectedGroup = tappedGroup,
                                onSelect = { tappedGroup = it }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val visibleGroups = if (frontView) {
                                listOf("胸", "核心", "肩", "手臂", "腿")
                            } else {
                                listOf("肩", "背", "手臂", "腿")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                visibleGroups.forEach { group ->
                                    val selected = tappedGroup == group
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) IOSSystemBlue else IOSSystemGray6)
                                            .clickable { tappedGroup = group }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = group,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // 有氧无对应肌肉区块，独立入口
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IOSSystemGreen.copy(alpha = 0.12f))
                                    .clickable { bodyGroup = "有氧" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("有氧 / 心肺（跑步机、单车、跳绳…）", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = IOSSystemGreen)
                            }
                        } else {
                            // 某部位下钻动作列表
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { bodyGroup = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回肌肉图")
                                }
                                Text("$group 训练", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(
                                    "${grouped[group]?.size ?: 0} 个动作",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                                items(grouped[group] ?: emptyList(), key = { "g_${it.id}" }) { ex ->
                                    ExerciseRow(exercise = ex, onSelect = onSelect)
                                }
                            }
                        }
                    }

                    // 3) 列表模式：按部位分组罗列
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                            sortedGroups.forEach { group ->
                                val groupExercises = grouped[group] ?: return@forEach
                                item(key = "header_$group") {
                                    Text(
                                        text = group,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IOSSystemBlue,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }
                                items(groupExercises, key = { it.id }) { ex ->
                                    ExerciseRow(exercise = ex, onSelect = onSelect)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // 模式分段（搜索时隐藏）
                if (!searching) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IOSSystemGray6)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("body" to "肌肉图", "list" to "列表").forEach { (key, label) ->
                            val selected = pickerMode == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable {
                                        pickerMode = key
                                        bodyGroup = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) IOSSystemBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 肌肉图素材 CC BY 4.0 署名（仅肌肉图总览时显示）
                if (!searching && pickerMode == "body" && bodyGroup == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "肌肉图源：Ryan Graves / Figma Community（CC BY 4.0）",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/** 动作选择行：名称 + 部位 + 记录模式小标签 + 收藏标 */
@Composable
private fun ExerciseRow(exercise: Exercise, onSelect: (Exercise) -> Unit) {
    val modeBadge = when {
        exercise.trackMode == "single" -> "单条"
        exercise.metrics == "dur_sec" -> "计时"
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(exercise) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = exercise.name, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.muscleGroup, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (modeBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(IOSSystemBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(modeBadge, fontSize = 9.sp, color = IOSSystemBlue)
                    }
                }
            }
        }
        if (exercise.isFavorite) Text("★", color = Color(0xFFFFC107))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun SetCountDialog(
    exerciseName: String,
    input: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                Text(
                    text = "这个动作做几组？",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("组数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("建议 3-5 组，最多 20 组", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IOSSystemBlue)
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("返回") }
        }
    )
}
