package net.darapu.projectbd.domain.usecase

import net.darapu.projectbd.domain.models.ActivityLevel
import kotlin.math.max

data class MacroTargets(
    val targetCalories: Float,
    val targetProtein: Float
)

class CalculateMacrosUseCase {
    fun invoke(
        weightLbs: Float,
        heightFt: Float,
        heightIn: Float,
        ageYears: Int,
        isMale: Boolean,
        activityLevel: ActivityLevel,
        goals: Set<String>
    ): MacroTargets {
        val w = weightLbs * 0.453592f
        val h = (heightFt * 30.48f) + (heightIn * 2.54f)
        val a = ageYears.toFloat()
        
        var bmr = (10 * w) + (6.25f * h) - (5 * a)
        bmr += if (isMale) 5 else -161
        val baseTdee = bmr * activityLevel.multiplier
        
        var calModifier = 0f
        var proteinMultiplier = 1.8f
        
        if ("Fat Loss" in goals) {
            calModifier -= 500f
            proteinMultiplier = max(proteinMultiplier, 2.2f)
        }
        if ("Build Muscle" in goals) {
            calModifier += 300f
            proteinMultiplier = max(proteinMultiplier, 2.0f)
        }
        if ("Stamina" in goals) {
            calModifier += 200f
        }
        
        val targetCalories = baseTdee + calModifier
        val targetProtein = w * proteinMultiplier

        return MacroTargets(targetCalories, targetProtein)
    }
}
