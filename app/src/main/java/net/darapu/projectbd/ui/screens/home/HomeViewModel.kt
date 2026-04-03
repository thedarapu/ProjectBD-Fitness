package net.darapu.projectbd.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.darapu.projectbd.data.repository.ActivityRepository
import net.darapu.projectbd.data.repository.SettingsRepository
import net.darapu.projectbd.domain.models.deserializeMealPlan
import java.time.LocalDate

data class HomeUiState(
    val isLoading: Boolean = true,
    val steps: Int = 0,
    val moveCalories: Int = 0,
    val exerciseMinutes: Int = 0,
    val standHours: Int = 0,
    val targetSteps: Int = 10000,
    val targetMove: Int = 500,
    val targetExercise: Int = 30,
    val targetStand: Int = 12,
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val targetCalories: Float = 2000f,
    val targetProtein: Float = 150f,
    val targetFat: Float = 55.5f,
    val targetCarbs: Float = 225f
)

class HomeViewModel(
    private val activityRepository: ActivityRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            activityRepository.getActivityFlowForDate(today).collect { activity ->
                val targetSteps = settingsRepository.getTargetSteps()
                val targetMove = settingsRepository.getTargetActiveCalories()
                val targetExercise = settingsRepository.getTargetExerciseMinutes()
                val targetStand = settingsRepository.getTargetStandHours()

                val targetCalories = settingsRepository.getTargetCalories()
                val targetProtein = settingsRepository.getTargetProtein()
                val targetFat = (targetCalories * 0.25f) / 9f
                val targetCarbs = (targetCalories - (targetProtein * 4) - (targetFat * 9)) / 4f

                val mealPlan = activity?.mealsJson?.let { deserializeMealPlan(it) } ?: emptyList()
                val eatenMeals = mealPlan.filter { it.isEaten }
                val totalCal = eatenMeals.sumOf { it.totalCalories.toDouble() }.toFloat()
                val totalPro = eatenMeals.sumOf { it.totalProtein.toDouble() }.toFloat()
                val totalCar = eatenMeals.sumOf { it.totalCarbs.toDouble() }.toFloat()
                val totalFat = eatenMeals.sumOf { it.totalFat.toDouble() }.toFloat()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        steps = activity?.steps ?: 0,
                        moveCalories = activity?.moveCalories ?: 0,
                        exerciseMinutes = activity?.exerciseMinutes ?: 0,
                        standHours = activity?.standHours ?: 0,
                        targetSteps = targetSteps,
                        targetMove = targetMove,
                        targetExercise = targetExercise,
                        targetStand = targetStand,
                        totalCalories = totalCal,
                        totalProtein = totalPro,
                        totalCarbs = totalCar,
                        totalFat = totalFat,
                        targetCalories = targetCalories,
                        targetProtein = targetProtein,
                        targetFat = targetFat,
                        targetCarbs = targetCarbs
                    )
                }
            }
        }
    }

    fun syncActivity(healthConnectClient: androidx.health.connect.client.HealthConnectClient) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            activityRepository.syncActivity(healthConnectClient)
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
