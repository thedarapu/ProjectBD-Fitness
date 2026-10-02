package net.darapu.projectbd.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE)
    
    fun getTheme(): String = prefs.getString("app_theme", "System Default") ?: "System Default"
    fun setTheme(theme: String) = prefs.edit { putString("app_theme", theme) }

    fun isOnboardingComplete(): Boolean = prefs.getBoolean("onboarding_complete", false)
    fun setOnboardingComplete(complete: Boolean) = prefs.edit { putBoolean("onboarding_complete", complete) }

    fun getTargetSteps(): Int = prefs.getInt("target_steps", 10000)
    fun getTargetActiveCalories(): Int = prefs.getInt("target_active_calories", 500)
    fun getTargetExerciseMinutes(): Int = prefs.getInt("target_exercise_minutes", 30)
    fun getTargetStandHours(): Int = prefs.getInt("target_stand_hours", 12)

    fun getTargetCalories(): Float = prefs.getFloat("target_calories", 2000f)
    
    fun getWorkoutPlanJson(): String? = prefs.getString("workout_plan_json", null)
    
    fun getExperienceLevel(): String = prefs.getString("experience_level", "BEGINNER") ?: "BEGINNER"
    fun getEquipmentProfile(): String = prefs.getString("equipment_profile", "FULL_GYM") ?: "FULL_GYM"
    fun getDaysToTrain(): Int = prefs.getInt("days_to_train", 3)

    fun saveLiftingProfile(experience: String, equipment: String, daysToTrain: Int) {
        prefs.edit {
            putString("experience_level", experience)
            putString("equipment_profile", equipment)
            putInt("days_to_train", daysToTrain)
        }
    }

    fun saveFitnessTargets(steps: Int, calories: Int, minutes: Int, standHours: Int) {
        prefs.edit {
            putInt("target_steps", steps)
            putInt("target_active_calories", calories)
            putInt("target_exercise_minutes", minutes)
            putInt("target_stand_hours", standHours)
        }
    }
}
