package net.darapu.projectbd.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.darapu.projectbd.data.local.AppDatabase
import net.darapu.projectbd.data.local.DailyActivity
import net.darapu.projectbd.domain.models.WorkoutDay
import net.darapu.projectbd.data.repository.SettingsRepository
import net.darapu.projectbd.domain.models.deserializeWorkoutPlan
import net.darapu.projectbd.domain.models.serializeWorkoutPlan
import java.time.LocalDate

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val workoutDays: List<WorkoutDay> = emptyList(),
    val stepGoal: Int = 10000,
    val moveGoal: Int = 500,
    val exerciseGoal: Int = 30,
    val standGoal: Int = 12,
    val steps: Long = 0,
    val activeCalories: Double = 0.0,
    val exerciseMinutes: Long = 0,
    val standHours: Int = 0
)

class WorkoutViewModel(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val activity = database.dailyActivityDao().getActivityForDate(today)

            val stepGoal = settingsRepository.getTargetSteps()
            val moveGoal = settingsRepository.getTargetActiveCalories()
            val exerciseGoal = settingsRepository.getTargetExerciseMinutes()
            val standGoal = settingsRepository.getTargetStandHours()

            val planStr = activity?.workoutPlan ?: settingsRepository.getWorkoutPlanJson()
            val workoutDays = if (planStr != null) deserializeWorkoutPlan(planStr) ?: emptyList() else emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    workoutDays = workoutDays,
                    stepGoal = stepGoal,
                    moveGoal = moveGoal,
                    exerciseGoal = exerciseGoal,
                    standGoal = standGoal,
                    steps = activity?.steps?.toLong() ?: 0L,
                    activeCalories = activity?.moveCalories?.toDouble() ?: 0.0,
                    exerciseMinutes = activity?.exerciseMinutes?.toLong() ?: 0L,
                    standHours = activity?.standHours ?: 0
                )
            }
        }
    }

    fun updateWorkoutPlan(updatedPlan: List<WorkoutDay>) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val serialized = serializeWorkoutPlan(updatedPlan)
            val existing = database.dailyActivityDao().getActivityForDate(today)
            database.dailyActivityDao().insertOrUpdate(
                existing?.copy(workoutPlan = serialized) ?: DailyActivity(date = today, workoutPlan = serialized)
            )

            _uiState.update { it.copy(workoutDays = updatedPlan) }
        }
    }

    fun updateMetrics(steps: Long, activeCalories: Double, exerciseMinutes: Long, standHours: Int) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = database.dailyActivityDao().getActivityForDate(today)
            database.dailyActivityDao().insertOrUpdate(
                existing?.copy(
                    steps = steps.toInt(),
                    moveCalories = activeCalories.toInt(),
                    exerciseMinutes = exerciseMinutes.toInt(),
                    standHours = standHours
                ) ?: DailyActivity(
                    date = today,
                    steps = steps.toInt(),
                    moveCalories = activeCalories.toInt(),
                    exerciseMinutes = exerciseMinutes.toInt(),
                    standHours = standHours
                )
            )

            _uiState.update {
                it.copy(
                    steps = steps,
                    activeCalories = activeCalories,
                    exerciseMinutes = exerciseMinutes,
                    standHours = standHours
                )
            }
        }
    }
}
