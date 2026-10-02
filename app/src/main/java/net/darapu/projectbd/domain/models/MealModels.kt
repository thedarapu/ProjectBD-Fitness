package net.darapu.projectbd.domain.models

import org.json.JSONArray
import org.json.JSONObject

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}

data class Food(
    val name: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val servingSize: Float,
    val servingUnit: String
)

data class MealComponent(
    val food: Food,
    val quantity: Float, // in multiples of servingSize
    val isEaten: Boolean = false,
    val mealType: MealType
) {
    val totalCalories: Float get() = food.caloriesPer100g * (food.servingSize * quantity) / 100f
}

fun deserializeMealPlan(jsonStr: String): List<MealComponent>? {
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<MealComponent>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val foodObj = obj.getJSONObject("food")
            val food = Food(
                name = foodObj.getString("name"),
                caloriesPer100g = foodObj.getDouble("caloriesPer100g").toFloat(),
                proteinPer100g = foodObj.getDouble("proteinPer100g").toFloat(),
                carbsPer100g = foodObj.getDouble("carbsPer100g").toFloat(),
                fatPer100g = foodObj.getDouble("fatPer100g").toFloat(),
                servingSize = foodObj.getDouble("servingSize").toFloat(),
                servingUnit = foodObj.getString("servingUnit")
            )
            list.add(MealComponent(
                food = food,
                quantity = obj.getDouble("quantity").toFloat(),
                isEaten = obj.optBoolean("isEaten", false),
                mealType = MealType.valueOf(obj.getString("mealType"))
            ))
        }
        list
    } catch (e: Exception) {
        null
    }
}
