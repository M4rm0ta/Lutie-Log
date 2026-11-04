package com.example.lutielog.ui.screen.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lutielog.data.entity.BodyWeight
import com.example.lutielog.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BodyUiState(
    val weights: List<BodyWeight> = emptyList(), // 接口返回倒序（最新在前）
    val weightInput: String = "",
    val fatInput: String = "",
    val showAddDialog: Boolean = false
) {
    // 图表用正序（旧 -> 新）
    val chartPoints: List<BodyWeight> get() = weights.sortedBy { it.measuredAt }
}

class BodyViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyUiState())
    val uiState: StateFlow<BodyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allBodyWeights.collect { list ->
                _uiState.value = _uiState.value.copy(weights = list)
            }
        }
    }

    fun showAdd() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, weightInput = "", fatInput = "")
    }

    fun dismissAdd() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun updateWeightInput(v: String) {
        _uiState.value = _uiState.value.copy(weightInput = filterDecimal(v))
    }

    fun updateFatInput(v: String) {
        _uiState.value = _uiState.value.copy(fatInput = filterDecimal(v))
    }

    private fun filterDecimal(raw: String): String {
        var dot = false
        return buildString {
            for (c in raw.filter { it.isDigit() || it == '.' }) {
                if (c == '.') {
                    if (dot) continue
                    dot = true
                    append(c)
                } else {
                    if (dot && toString().substringAfter('.').length >= 1) continue
                    append(c)
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val w = state.weightInput.trim().toDoubleOrNull() ?: return
        if (w !in 20.0..300.0) return
        val fat = state.fatInput.trim().toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            repository.insertBodyWeight(
                BodyWeight(
                    weightKg = w,
                    bodyFatPct = fat,
                    measuredAt = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(showAddDialog = false)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteBodyWeight(id) }
    }

    companion object {
        fun provideFactory(repository: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BodyViewModel(repository) as T
            }
    }
}
