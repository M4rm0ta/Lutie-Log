package com.example.lutielog.ui.screen.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.os.SystemClock
import com.example.lutielog.data.MetricInputType
import com.example.lutielog.data.MetricSpec
import com.example.lutielog.data.entity.Exercise
import com.example.lutielog.data.entity.WorkoutSet
import com.example.lutielog.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 一个已完成动作及其全部组记录
data class ExerciseGroup(
    val groupId: Long,             // 组内唯一标识，避免同一动作重复完成时 LazyColumn key 冲突
    val exercise: Exercise,
    val sets: List<WorkoutSet>
)

// 待撤销的删除
data class PendingUndo(val set: WorkoutSet)

data class TrainUiState(
    val sessionId: Long? = null,
    val isTraining: Boolean = false,
    val selectedExercise: Exercise? = null,
    val supersetPartner: Exercise? = null, // 超级组伙伴动作
    val activeExerciseId: Long? = null,    // 当前正在录入的动作（普通模式=selectedExercise）
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val currentSets: List<WorkoutSet> = emptyList(), // 未归档的组（超级组时含两个动作）
    val completedGroups: List<ExerciseGroup> = emptyList(),
    // 动态录入输入：metric key -> 文本
    val metricInputs: Map<String, String> = emptyMap(),
    val setType: String = "normal",        // 当前待录入组类型：normal/warmup/drop
    val restSeconds: Int = 90,             // 当前动作休息时长
    val timerRunning: Boolean = false,
    val timerSeconds: Int = 0,
    val defaultRestSeconds: Int = 90,
    val showExercisePicker: Boolean = false,
    val pickingPartner: Boolean = false,   // 弹窗是否用于选超级组伙伴
    val showSetCountDialog: Boolean = false,
    val targetSetCount: Int = 4,
    val setCountInput: String = "4",
    val isFinishing: Boolean = false,
    val timerJustFinished: Boolean = false,
    val pendingUndo: PendingUndo? = null
)

class TrainViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainUiState())
    val uiState: StateFlow<TrainUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    // 已完成动作组的自增序号，保证每个 ExerciseGroup 的 groupId 唯一
    private var groupSeq = 0L
    // 倒计时结束时刻（基于 SystemClock.elapsedRealtime，灭屏/后台仍真实流逝，不受改系统时间影响）
    private var timerEndAtRealtime: Long = 0L

    init {
        viewModelScope.launch {
            repository.allExercises.collect { exercises ->
                _uiState.value = _uiState.value.copy(exercises = exercises)
            }
        }
    }

    // 当前激活的动作（超级组时可能是伙伴）
    private fun activeExerciseOf(state: TrainUiState): Exercise? {
        val main = state.selectedExercise ?: return null
        val partner = state.supersetPartner
        return if (state.activeExerciseId != null && state.activeExerciseId == partner?.id) partner else main
    }

    fun startTraining() {
        viewModelScope.launch {
            val sessionId = repository.startSession()
            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                isTraining = true,
                showExercisePicker = true
            )
        }
    }

    // 用户主动点"添加下一个动作"才打开弹窗
    fun addNextExercise() {
        _uiState.value = _uiState.value.copy(showExercisePicker = true, pickingPartner = false)
    }

    fun selectExercise(exercise: Exercise) {
        val state = _uiState.value
        // 正在为超级组选伙伴
        if (state.pickingPartner) {
            setPartner(exercise)
            return
        }
        val single = exercise.trackMode == "single"
        _uiState.value = state.copy(
            selectedExercise = exercise,
            supersetPartner = null,
            activeExerciseId = exercise.id,
            restSeconds = exercise.restSec,
            setType = "normal",
            showExercisePicker = false,
            // 有氧单条：不询问组数，直接进入录入，目标条数固定 1
            showSetCountDialog = !single,
            targetSetCount = if (single) 1 else state.targetSetCount,
            setCountInput = state.targetSetCount.toString(),
            metricInputs = emptyMap(),
            currentSets = emptyList()
        )
        if (single) prefillFromLast(exercise)
    }

    // 进入超级组：标记下次弹窗用于选伙伴
    fun startPickingPartner() {
        _uiState.value = _uiState.value.copy(showExercisePicker = true, pickingPartner = true)
    }

    private fun setPartner(exercise: Exercise) {
        _uiState.value = _uiState.value.copy(
            supersetPartner = exercise,
            activeExerciseId = _uiState.value.selectedExercise?.id,
            showExercisePicker = false,
            pickingPartner = false,
            metricInputs = emptyMap(),
            setType = "normal"
        )
    }

    // 取消超级组（伙伴已记录的组保留、随后归档）
    fun removePartner() {
        _uiState.value = _uiState.value.copy(
            supersetPartner = null,
            activeExerciseId = _uiState.value.selectedExercise?.id,
            metricInputs = emptyMap()
        )
    }

    // 超级组：在主动作/伙伴间切换录入
    fun toggleActiveExercise() {
        val state = _uiState.value
        val main = state.selectedExercise ?: return
        val partner = state.supersetPartner ?: return
        val newExercise = if (state.activeExerciseId == partner.id) main else partner
        _uiState.value = state.copy(
            activeExerciseId = newExercise.id,
            metricInputs = emptyMap(),
            setType = "normal",
            restSeconds = newExercise.restSec
        )
    }

    fun confirmSetCount() {
        val count = _uiState.value.setCountInput.toIntOrNull() ?: 4
        val finalCount = count.coerceIn(1, 20)
        val exercise = _uiState.value.selectedExercise
        _uiState.value = _uiState.value.copy(
            targetSetCount = finalCount,
            showSetCountDialog = false,
            currentSets = emptyList(),
            metricInputs = emptyMap()
        )
        exercise?.let { prefillFromLast(it) }
    }

    // 组数弹窗点"返回"：关掉组数弹窗、清空已选动作、回到动作选择
    fun cancelSetCount() {
        _uiState.value = _uiState.value.copy(
            showSetCountDialog = false,
            selectedExercise = null,
            activeExerciseId = null,
            currentSets = emptyList(),
            metricInputs = emptyMap(),
            showExercisePicker = true
        )
    }

    fun updateSetCountInput(value: String) {
        _uiState.value = _uiState.value.copy(setCountInput = value.filter { it.isDigit() }.take(2))
    }

    fun closeExercisePicker() {
        _uiState.value = _uiState.value.copy(showExercisePicker = false, pickingPartner = false)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // 切换当前待录入组类型
    fun setSetType(type: String) {
        _uiState.value = _uiState.value.copy(setType = type)
    }

    // 调整休息时长并记忆到当前动作（超级组时可能是伙伴）
    fun adjustRestSeconds(seconds: Int) {
        val state = _uiState.value
        val target = activeExerciseOf(state)
        _uiState.value = state.copy(restSeconds = seconds)
        if (target != null && target.restSec != seconds) {
            viewModelScope.launch { runCatching { repository.updateExercise(target.copy(restSec = seconds)) } }
        }
    }

    // 动态字段输入：按字段类型过滤（整数/小数）
    fun updateMetric(key: String, raw: String) {
        val type = MetricSpec.field(key)?.type
        val filtered = when (type) {
            MetricInputType.INT -> raw.filter { it.isDigit() }.take(4)
            MetricInputType.DECIMAL -> {
                var dot = false
                buildString {
                    for (c in raw.filter { it.isDigit() || it == '.' }) {
                        if (c == '.') {
                            if (dot) continue
                            dot = true
                            append(c)
                        } else {
                            if (dot && toString().substringAfter('.').length >= 2) continue
                            append(c)
                        }
                    }
                }
            }
            else -> raw
        }
        _uiState.value = _uiState.value.copy(
            metricInputs = _uiState.value.metricInputs + (key to filtered)
        )
    }

    fun getFilteredExercises(): List<Exercise> {
        val query = _uiState.value.searchQuery.trim()
        return if (query.isEmpty()) {
            _uiState.value.exercises
        } else {
            _uiState.value.exercises.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.muscleGroup.contains(query, ignoreCase = true)
            }
        }
    }

    // 开练时预填上次该动作的记录
    private fun prefillFromLast(exercise: Exercise) {
        viewModelScope.launch {
            val last = repository.getPrefillSet(exercise.id) ?: return@launch
            val inputs = mutableMapOf<String, String>()
            MetricSpec.keys(exercise.metrics).forEach { key ->
                val value = when (key) {
                    "weight" -> if (last.weightKg > 0) MetricSpec.trimNum(last.weightKg) else ""
                    "reps" -> if (last.reps > 0) last.reps.toString() else ""
                    "dur_sec" -> if (last.durationSec > 0) last.durationSec.toString() else ""
                    "dur_min" -> if (last.durationSec > 0) MetricSpec.trimNum(last.durationSec / 60.0) else ""
                    "dist_km" -> if (last.distanceM >= 1000) MetricSpec.trimNum(last.distanceM / 1000.0) else ""
                    "dist_m" -> if (last.distanceM > 0 && last.distanceM < 1000) last.distanceM.toInt().toString() else ""
                    "speed" -> if (last.speed > 0) MetricSpec.trimNum(last.speed) else ""
                    "incline" -> if (last.incline > 0) MetricSpec.trimNum(last.incline) else ""
                    "resistance" -> if (last.resistanceLevel > 0) last.resistanceLevel.toString() else ""
                    "calories" -> if (last.calories > 0) last.calories.toString() else ""
                    else -> ""
                }
                if (value.isNotEmpty()) inputs[key] = value
            }
            val s = _uiState.value
            if (s.activeExerciseId == exercise.id && s.metricInputs.isEmpty()) {
                _uiState.value = s.copy(metricInputs = inputs)
            }
        }
    }

    fun addSet() {
        val state = _uiState.value
        val sessionId = state.sessionId ?: return
        val exercise = activeExerciseOf(state) ?: return
        val input = state.metricInputs

        fun text(key: String): String = input[key]?.trim().orEmpty()
        fun intVal(key: String): Int = text(key).toIntOrNull() ?: 0
        fun dblVal(key: String): Double = text(key).toDoubleOrNull() ?: 0.0

        val weightKg = dblVal("weight")
        val reps = intVal("reps")
        val durationSec = intVal("dur_sec") + (dblVal("dur_min") * 60).toInt()
        val distanceM = dblVal("dist_km") * 1000.0 + intVal("dist_m").toDouble()
        val speed = dblVal("speed")
        val incline = dblVal("incline")
        val resistance = intVal("resistance")
        val calories = intVal("calories")

        // 至少填写一个有效指标才允许保存
        val hasAny = weightKg > 0 || reps > 0 || durationSec > 0 || distanceM > 0 ||
                speed > 0 || incline > 0 || resistance > 0 || calories > 0
        if (!hasAny) return

        viewModelScope.launch {
            // 挂起期间状态可能已变化，统一基于最新状态计算，避免覆盖其他操作
            val latest = _uiState.value
            val mySets = latest.currentSets.filter { it.exerciseId == exercise.id }
            val set = WorkoutSet(
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setIndex = mySets.size + 1,
                weightKg = weightKg,
                reps = reps,
                durationSec = durationSec,
                recordType = exercise.recordType,
                setType = latest.setType,
                distanceM = distanceM,
                speed = speed,
                incline = incline,
                resistanceLevel = resistance,
                calories = calories
            )
            val newId = repository.insertSet(set)
            val savedSet = set.copy(id = newId)
            val beforeUpdate = _uiState.value
            val newSets = beforeUpdate.currentSets + savedSet

            _uiState.value = beforeUpdate.copy(
                currentSets = newSets,
                metricInputs = emptyMap(),
                setType = "normal"
            )

            // 是否启动组间歇
            val current = _uiState.value
            val partner = current.supersetPartner
            val shouldRest = when {
                exercise.trackMode == "single" -> false
                partner != null -> {
                    // 超级组：两个动作各完成一组（一轮）才休息
                    val mc = newSets.count { it.exerciseId == current.selectedExercise?.id }
                    val pc = newSets.count { it.exerciseId == partner.id }
                    mc == pc && mc < current.targetSetCount
                }
                else -> newSets.size < current.targetSetCount
            }
            if (shouldRest) {
                // 超级组一轮结束：把当前录入动作重置回主动作，休息后从主动作开始下一轮，
                // 避免 activeExerciseId 停留在伙伴动作、导致下一轮数据记到错误动作上
                if (partner != null && current.selectedExercise != null) {
                    _uiState.value = _uiState.value.copy(
                        activeExerciseId = current.selectedExercise.id,
                        metricInputs = emptyMap()
                    )
                }
                startTimer(current.restSeconds)
            }
        }
    }

    // 右滑复制：复制一组数据并追加
    fun copySet(set: WorkoutSet) {
        viewModelScope.launch {
            val state = _uiState.value
            val mySets = state.currentSets.filter { it.exerciseId == set.exerciseId }
            val copy = set.copy(id = 0, setIndex = mySets.size + 1, createdAt = System.currentTimeMillis())
            val newId = repository.insertSet(copy)
            _uiState.value = state.copy(currentSets = state.currentSets + copy.copy(id = newId))
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            val state = _uiState.value
            val set = state.currentSets.find { it.id == setId } ?: return@launch
            repository.deleteSet(setId)
            _uiState.value = state.copy(
                currentSets = state.currentSets.filterNot { it.id == setId },
                pendingUndo = PendingUndo(set)
            )
        }
    }

    // 撤销删除：把组重新追加
    fun undoDelete() {
        viewModelScope.launch {
            val state = _uiState.value
            val set = state.pendingUndo?.set ?: return@launch
            val mySets = state.currentSets.filter { it.exerciseId == set.exerciseId }
            val restored = set.copy(id = 0, setIndex = mySets.size + 1)
            val newId = repository.insertSet(restored)
            _uiState.value = state.copy(
                currentSets = state.currentSets + restored.copy(id = newId),
                pendingUndo = null
            )
        }
    }

    fun clearPendingUndo() {
        if (_uiState.value.pendingUndo != null) {
            _uiState.value = _uiState.value.copy(pendingUndo = null)
        }
    }

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        // 记录"结束时刻"，每次按真实时间反推剩余，后台/灭屏期间也在走，回前台即校准
        timerEndAtRealtime = SystemClock.elapsedRealtime() + seconds * 1000L
        _uiState.value = _uiState.value.copy(
            timerRunning = true,
            timerSeconds = seconds,
            timerJustFinished = false
        )
        timerJob = viewModelScope.launch {
            while (isActive) {
                val remainMs = timerEndAtRealtime - SystemClock.elapsedRealtime()
                if (remainMs <= 0L) {
                    _uiState.value = _uiState.value.copy(
                        timerRunning = false,
                        timerSeconds = 0,
                        timerJustFinished = true
                    )
                    break
                }
                _uiState.value = _uiState.value.copy(
                    timerSeconds = ((remainMs + 999L) / 1000L).toInt()
                )
                delay(250L)
            }
        }
    }

    // App 从后台回到前台时立即按真实时间校准一次
    fun refreshTimerOnResume() {
        if (!_uiState.value.timerRunning) return
        val remainMs = timerEndAtRealtime - SystemClock.elapsedRealtime()
        if (remainMs <= 0L) {
            timerJob?.cancel()
            _uiState.value = _uiState.value.copy(
                timerRunning = false,
                timerSeconds = 0,
                timerJustFinished = true
            )
        } else {
            _uiState.value = _uiState.value.copy(
                timerSeconds = ((remainMs + 999L) / 1000L).toInt()
            )
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timerRunning = false)
    }

    fun skipTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timerRunning = false, timerSeconds = 0)
    }

    fun resetTimerFinishedFlag() {
        _uiState.value = _uiState.value.copy(timerJustFinished = false)
    }

    // 完成当前动作：归档到已完成列表，不自动弹窗，由用户决定是否继续
    fun finishExercise() {
        val state = _uiState.value
        val main = state.selectedExercise ?: return
        if (state.currentSets.isEmpty()) return

        val groups = state.completedGroups.toMutableList()
        state.currentSets.filter { it.exerciseId == main.id }.takeIf { it.isNotEmpty() }?.let {
            groups.add(ExerciseGroup(++groupSeq, main, it))
        }
        val partner = state.supersetPartner
        if (partner != null) {
            state.currentSets.filter { it.exerciseId == partner.id }.takeIf { it.isNotEmpty() }?.let {
                groups.add(ExerciseGroup(++groupSeq, partner, it))
            }
        }

        _uiState.value = state.copy(
            completedGroups = groups,
            selectedExercise = null,
            supersetPartner = null,
            activeExerciseId = null,
            currentSets = emptyList(),
            metricInputs = emptyMap(),
            setType = "normal",
            timerRunning = false
        )
        timerJob?.cancel()
    }

    fun finishTraining() {
        val sessionId = _uiState.value.sessionId ?: return
        _uiState.value = _uiState.value.copy(isFinishing = true)
        viewModelScope.launch {
            repository.finishSession(sessionId)
            timerJob?.cancel()
            // 重置训练相关状态，但保留已加载的动作库（Room Flow 仅在数据变化时才重新发射，
            // 若一并清空会导致下次开始训练时动作列表为空）
            val cachedExercises = _uiState.value.exercises
            _uiState.value = TrainUiState(exercises = cachedExercises)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun provideFactory(repository: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrainViewModel(repository) as T
                }
            }
    }
}
