package net.darapu.projectbd.domain.models

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class SetRecord(
    var reps: String = "",
    var weight: String = ""
)

data class Exercise(
    val name: String,
    val targetSets: Int,
    val targetReps: String,
    val targetWeight: String,
    val sets: MutableList<SetRecord> = mutableListOf()
)

data class WorkoutDay(
    val title: String,
    val exercises: List<Exercise>
)

fun serializeWorkoutPlan(plan: List<WorkoutDay>): String {
    val jsonArray = JSONArray()
    plan.forEach { day ->
        val dayObj = JSONObject()
        dayObj.put("title", day.title)
        val exercisesArray = JSONArray()
        day.exercises.forEach { exercise ->
            val exObj = JSONObject()
            exObj.put("name", exercise.name)
            exObj.put("targetSets", exercise.targetSets)
            exObj.put("targetReps", exercise.targetReps)
            exObj.put("targetWeight", exercise.targetWeight)
            
            val setsArray = JSONArray()
            exercise.sets.forEach { set ->
                val setObj = JSONObject()
                setObj.put("reps", set.reps)
                setObj.put("weight", set.weight)
                setsArray.put(setObj)
            }
            exObj.put("sets", setsArray)
            exercisesArray.put(exObj)
        }
        dayObj.put("exercises", exercisesArray)
        jsonArray.put(dayObj)
    }
    return jsonArray.toString()
}

fun deserializeWorkoutPlan(jsonStr: String): List<WorkoutDay>? {
    return try {
        val jsonArray = JSONArray(jsonStr)
        val plan = mutableListOf<WorkoutDay>()
        for (i in 0 until jsonArray.length()) {
            val dayObj = jsonArray.getJSONObject(i)
            val title = dayObj.getString("title")
            val exercisesArray = dayObj.getJSONArray("exercises")
            val exercises = mutableListOf<Exercise>()
            for (j in 0 until exercisesArray.length()) {
                val exObj = exercisesArray.getJSONObject(j)
                val sets = mutableListOf<SetRecord>()
                if (exObj.has("sets")) {
                    val setsArray = exObj.getJSONArray("sets")
                    for (k in 0 until setsArray.length()) {
                        val setObj = setsArray.getJSONObject(k)
                        sets.add(SetRecord(setObj.optString("reps", ""), setObj.optString("weight", "")))
                    }
                }
                
                // For backward compatibility or initial generation
                if (sets.isEmpty()) {
                    val targetSetsCount = exObj.optInt("targetSets", exObj.optInt("sets", 3))
                    repeat(targetSetsCount) { sets.add(SetRecord()) }
                }

                exercises.add(
                    Exercise(
                        exObj.getString("name"),
                        exObj.optInt("targetSets", exObj.optInt("sets", 3)),
                        exObj.optString("targetReps", exObj.optString("reps", "8-12")),
                        exObj.optString("targetWeight", exObj.optString("weight", "Target")),
                        sets
                    )
                )
            }
            plan.add(WorkoutDay(title, exercises))
        }
        plan
    } catch (e: Exception) {
        Log.e("WorkoutModels", "Error deserializing workout plan", e)
        null
    }
}
