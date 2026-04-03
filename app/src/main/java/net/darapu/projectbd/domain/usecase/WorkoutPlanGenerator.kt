package net.darapu.projectbd.domain.usecase

import net.darapu.projectbd.domain.models.Exercise
import net.darapu.projectbd.domain.models.WorkoutDay
import net.darapu.projectbd.domain.models.serializeWorkoutPlan

object WorkoutPlanGenerator {
    private fun createExercises(list: String): List<Exercise> {
        return list.split(", ").map { Exercise(it, 3, "8-12", "Moderate") }
    }

    fun generatePlan(days: Int, goals: Set<String>): String {
        val exerciseMap = mapOf(
            "Push" to "Smith Machine Bench Press, Dumbbell Overhead Press, Cable Tricep Pushdowns, Dumbbell Lateral Raises",
            "Pull" to "Lat Pulldown Machine, Dumbbell Rows, Cable Face Pulls, Dumbbell Bicep Curls",
            "Legs" to "Smith Machine Squats, Leg Press Machine, Leg Curl Machine, Smith Machine Calf Raises",
            "Full Body" to "Smith Machine Squats, Smith Machine Bench Press, Dumbbell Rows, Dumbbell Overhead Press",
            "Upper" to "Smith Machine Bench Press, Dumbbell Rows, Dumbbell Overhead Press, Lat Pulldown Machine",
            "Lower" to "Smith Machine Squats, Leg Press Machine, Leg Curl Machine, Smith Machine Calf Raises",
            "Flexibility" to "Sun Salutations, Hamstring Stretch, Cobra Pose, Child's Pose"
        )
        
        val muscleGroups = mapOf(
            "Push" to "Chest, Shoulders, Triceps",
            "Pull" to "Back, Biceps",
            "Legs" to "Quads, Hamstrings, Calves",
            "Full Body" to "Full Body",
            "Upper" to "Chest, Back, Shoulders",
            "Lower" to "Quads, Hamstrings"
        )

        val plan = mutableListOf<WorkoutDay>()
        
        when (days) {
            1 -> plan.add(WorkoutDay("Day 1 (Full Body)", createExercises(exerciseMap["Full Body"]!!)))
            2 -> {
                plan.add(WorkoutDay("Day 1 (${muscleGroups["Upper"]})", createExercises(exerciseMap["Upper"]!!)))
                plan.add(WorkoutDay("Day 2 (${muscleGroups["Lower"]})", createExercises(exerciseMap["Lower"]!!)))
            }
            3 -> {
                plan.add(WorkoutDay("Day 1 (${muscleGroups["Push"]})", createExercises(exerciseMap["Push"]!!)))
                plan.add(WorkoutDay("Day 2 (${muscleGroups["Pull"]})", createExercises(exerciseMap["Pull"]!!)))
                plan.add(WorkoutDay("Day 3 (${muscleGroups["Legs"]})", createExercises(exerciseMap["Legs"]!!)))
            }
            4 -> {
                plan.add(WorkoutDay("Day 1 (${muscleGroups["Upper"]})", createExercises(exerciseMap["Upper"]!!)))
                plan.add(WorkoutDay("Day 2 (${muscleGroups["Lower"]})", createExercises(exerciseMap["Lower"]!!)))
                plan.add(WorkoutDay("Day 3 (${muscleGroups["Upper"]})", createExercises(exerciseMap["Upper"]!!)))
                plan.add(WorkoutDay("Day 4 (${muscleGroups["Lower"]})", createExercises(exerciseMap["Lower"]!!)))
            }
            5 -> {
                plan.add(WorkoutDay("Day 1 (${muscleGroups["Push"]})", createExercises(exerciseMap["Push"]!!)))
                plan.add(WorkoutDay("Day 2 (${muscleGroups["Pull"]})", createExercises(exerciseMap["Pull"]!!)))
                plan.add(WorkoutDay("Day 3 (${muscleGroups["Legs"]})", createExercises(exerciseMap["Legs"]!!)))
                plan.add(WorkoutDay("Day 4 (${muscleGroups["Upper"]})", createExercises(exerciseMap["Upper"]!!)))
                plan.add(WorkoutDay("Day 5 (${muscleGroups["Lower"]})", createExercises(exerciseMap["Lower"]!!)))
            }
            6 -> {
                plan.add(WorkoutDay("Day 1 (${muscleGroups["Push"]})", createExercises(exerciseMap["Push"]!!)))
                plan.add(WorkoutDay("Day 2 (${muscleGroups["Pull"]})", createExercises(exerciseMap["Pull"]!!)))
                plan.add(WorkoutDay("Day 3 (${muscleGroups["Legs"]})", createExercises(exerciseMap["Legs"]!!)))
                plan.add(WorkoutDay("Day 4 (${muscleGroups["Push"]})", createExercises(exerciseMap["Push"]!!)))
                plan.add(WorkoutDay("Day 5 (${muscleGroups["Pull"]})", createExercises(exerciseMap["Pull"]!!)))
                plan.add(WorkoutDay("Day 6 (${muscleGroups["Legs"]})", createExercises(exerciseMap["Legs"]!!)))
            }
            else -> {
                for (i in 1..days) {
                    plan.add(WorkoutDay("Day $i (Mixed)", createExercises(exerciseMap["Full Body"]!!)))
                }
            }
        }

        if ("Flexibility" in goals) {
            plan.add(WorkoutDay("Daily (Flexibility)", createExercises(exerciseMap["Flexibility"]!!)))
        }

        return serializeWorkoutPlan(plan)
    }
}
