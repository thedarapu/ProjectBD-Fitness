package net.darapu.projectbd.ui.screens.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.darapu.projectbd.data.local.AppDatabase
import net.darapu.projectbd.data.local.DailyActivity
import net.darapu.projectbd.domain.models.FoodItem
import net.darapu.projectbd.domain.models.MealComponent
import net.darapu.projectbd.domain.models.MealType
import net.darapu.projectbd.data.repository.SettingsRepository
import net.darapu.projectbd.domain.models.deserializeMealPlan
import net.darapu.projectbd.domain.models.serializeMealPlan
import java.time.LocalDate

data class DietUiState(
    val isLoading: Boolean = true,
    val trackedMeals: List<MealComponent> = emptyList(),
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val targetCalories: Float = 2000f,
    val targetProtein: Float = 150f,
    val targetFat: Float = 55.5f,
    val targetCarbs: Float = 225f
)

class DietViewModel(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DietUiState())
    val uiState: StateFlow<DietUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val activity = database.dailyActivityDao().getActivityForDate(today)

            val targetCalories = settingsRepository.getTargetCalories()
            val targetProtein = settingsRepository.getTargetProtein()
            val targetFat = (targetCalories * 0.25f) / 9f
            val targetCarbs = (targetCalories - (targetProtein * 4) - (targetFat * 9)) / 4f

            val mealPlan = activity?.mealsJson?.let { deserializeMealPlan(it) } ?: emptyList()
            
            updateStateWithMeals(mealPlan, targetCalories, targetProtein, targetFat, targetCarbs, isLoading = false)
        }
    }

    fun addOrUpdateEntry(newFood: FoodItem, quantity: Float, mealType: MealType, toReplace: MealComponent?) {
        val newEntry = MealComponent(newFood, quantity, mealType, isEaten = true)
        val currentMeals = _uiState.value.trackedMeals.toMutableList()
        
        if (toReplace != null) {
            val index = currentMeals.indexOf(toReplace)
            if (index != -1) {
                currentMeals[index] = newEntry
            }
        } else {
            currentMeals.add(newEntry)
        }

        saveMeals(currentMeals)
    }

    fun removeEntry(entry: MealComponent) {
        val currentMeals = _uiState.value.trackedMeals.filter { it != entry }
        saveMeals(currentMeals)
    }

    fun updateMeals(meals: List<MealComponent>) {
        saveMeals(meals)
    }

    private fun saveMeals(meals: List<MealComponent>) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val jsonStr = serializeMealPlan(meals)
            
            val existing = database.dailyActivityDao().getActivityForDate(today)
            database.dailyActivityDao().insertOrUpdate(
                existing?.copy(mealsJson = jsonStr) ?: DailyActivity(date = today, mealsJson = jsonStr)
            )

            updateStateWithMeals(
                meals = meals, 
                targetCalories = _uiState.value.targetCalories,
                targetProtein = _uiState.value.targetProtein,
                targetFat = _uiState.value.targetFat,
                targetCarbs = _uiState.value.targetCarbs,
                isLoading = false
            )
        }
    }

    private fun updateStateWithMeals(
        meals: List<MealComponent>,
        targetCalories: Float,
        targetProtein: Float,
        targetFat: Float,
        targetCarbs: Float,
        isLoading: Boolean
    ) {
        val totalCal = meals.sumOf { it.totalCalories.toDouble() }.toFloat()
        val totalPro = meals.sumOf { it.totalProtein.toDouble() }.toFloat()
        val totalCar = meals.sumOf { it.totalCarbs.toDouble() }.toFloat()
        val totalFat = meals.sumOf { it.totalFat.toDouble() }.toFloat()

        _uiState.update {
            it.copy(
                isLoading = isLoading,
                trackedMeals = meals,
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
