package net.darapu.projectbd.domain.usecase

import kotlin.math.roundToInt

class CalculateOneRepMaxUseCase {
    /**
     * Calculates the estimated 1-Rep Max using the Epley formula:
     * 1RM = weight * (1 + 0.0333 * reps)
     * 
     * @param weight The weight lifted (lbs or kg)
     * @param reps The number of repetitions performed
     * @return The estimated 1RM rounded to the nearest integer
     */
    operator fun invoke(weight: Float, reps: Int): Int {
        if (reps <= 0 || weight <= 0f) return 0
        if (reps == 1) return weight.roundToInt()
        
        val oneRepMax = weight * (1 + 0.0333f * reps)
        return oneRepMax.roundToInt()
    }
}
