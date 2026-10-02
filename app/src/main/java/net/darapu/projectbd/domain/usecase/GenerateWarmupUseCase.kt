package net.darapu.projectbd.domain.usecase

import net.darapu.projectbd.domain.models.SetRecord
import kotlin.math.roundToInt

class GenerateWarmupUseCase {
    /**
     * Generates a list of warm-up sets based on a target working weight.
     * Standard protocol:
     * - 10 reps @ 50%
     * - 5 reps @ 70%
     * - 3 reps @ 85%
     * 
     * @param workingWeight The primary working weight for the exercise.
     * @return A list of SetRecord objects marked as warmups.
     */
    operator fun invoke(workingWeight: Float): List<SetRecord> {
        if (workingWeight <= 0f) return emptyList()
        
        return listOf(
            SetRecord(reps = "10", weight = (workingWeight * 0.5f).roundToNearestFive().toString()),
            SetRecord(reps = "5", weight = (workingWeight * 0.7f).roundToNearestFive().toString()),
            SetRecord(reps = "3", weight = (workingWeight * 0.85f).roundToNearestFive().toString())
        )
    }

    private fun Float.roundToNearestFive(): Int {
        return (this / 5f).roundToInt() * 5
    }
}
