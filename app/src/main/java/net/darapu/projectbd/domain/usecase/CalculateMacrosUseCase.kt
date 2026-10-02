package net.darapu.projectbd.domain.usecase

import net.darapu.projectbd.domain.models.ActivityLevel

data class MacroTargets(
    val targetCalories: Float,
    val targetProtein: Float,
    val targetCarbs: Float,
    val targetFat: Float
)

class CalculateMacrosUseCase {
    operator fun invoke(
        weightLbs: Float,
        heightFt: Float,
        heightIn: Float,
        ageYears: Int,
        isMale: Boolean,
        activityLevel: ActivityLevel,
        goals: Set<String>
    ): MacroTargets {
        // Simple Mifflin-St Jeor Equation
        val weightKg = weightLbs * 0.453592f
        val heightCm = (heightFt * 12 + heightIn) * 2.54f
        
        val bmr = if (isMale) {
            (10 * weightKg) + (6.25f * heightCm) - (5 * ageYears) + 5
        } else {
            (10 * weightKg) + (6.25f * heightCm) - (5 * ageYears) - 161
        }
        
        var tdee = bmr * activityLevel.multiplier
        
        // Adjust based on goals
        if (goals.contains("Fat Loss")) {
            tdee -= 500
        } else if (goals.contains("Build Muscle")) {
            tdee += 300
        }
        
        // Protein: 0.8g to 1.2g per lb of bodyweight
        val proteinGrams = if (goals.contains("Build Muscle")) {
            weightLbs * 1.0f
        } else {
            weightLbs * 0.8f
        }
        
        // Fat: 25% of calories
        val fatGrams = (tdee * 0.25f) / 9f
        
        // Carbs: Remainder
        val carbsGrams = (tdee - (proteinGrams * 4) - (fatGrams * 9)) / 4f
        
        return MacroTargets(
            targetCalories = tdee,
            targetProtein = proteinGrams,
            targetCarbs = carbsGrams,
            targetFat = fatGrams
        )
    }
}
