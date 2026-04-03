package net.darapu.projectbd.domain.models

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

enum class FoodType { PROTEIN, CARB, FAT, VEGGIE }
enum class MealType { BREAKFAST, LUNCH, SNACK, DINNER }

data class FoodItem(
    val name: String,
    val protein: Float, // per 100g or 100ml
    val carbs: Float,   // per 100g or 100ml
    val fat: Float,     // per 100g or 100ml
    val type: FoodType,
    val overrideCalories: Float? = null,
    val servingSize: Float = 100f, // weight in grams/ml of one unit
    val servingUnit: String = "g"  // label of the unit (e.g., "Cup", "Bar")
) {
    val calories: Float get() = overrideCalories ?: ((protein * 4) + (carbs * 4) + (fat * 9))
    
    // Helper to get macros for the default serving size (1 unit)
    val proteinPerServing: Float get() = (protein * servingSize) / 100f
    val carbsPerServing: Float get() = (carbs * servingSize) / 100f
    val fatPerServing: Float get() = (fat * servingSize) / 100f
    val caloriesPerServing: Float get() = (calories * servingSize) / 100f
}

data class MealComponent(
    var food: FoodItem,
    var quantity: Float, // Number of units (e.g., 1.5)
    var mealType: MealType = MealType.LUNCH,
    var isEaten: Boolean = true
) {
    val totalGrams: Float get() = quantity * food.servingSize
    val totalProtein: Float get() = (food.protein * totalGrams) / 100f
    val totalCarbs: Float get() = (food.carbs * totalGrams) / 100f
    val totalFat: Float get() = (food.fat * totalGrams) / 100f
    val totalCalories: Float get() = (food.calories * totalGrams) / 100f
}

fun serializeMealPlan(mealPlan: List<MealComponent>): String {
    val jsonArray = JSONArray()
    mealPlan.forEach { component ->
        val obj = JSONObject()
        val foodObj = JSONObject()
        foodObj.put("name", component.food.name)
        foodObj.put("protein", component.food.protein.toDouble())
        foodObj.put("carbs", component.food.carbs.toDouble())
        foodObj.put("fat", component.food.fat.toDouble())
        foodObj.put("type", component.food.type.name)
        foodObj.put("servingSize", component.food.servingSize.toDouble())
        foodObj.put("servingUnit", component.food.servingUnit)
        
        val ovCal = component.food.overrideCalories
        foodObj.put("calories", ovCal?.toDouble() ?: JSONObject.NULL)
        
        obj.put("food", foodObj)
        obj.put("quantity", component.quantity.toDouble())
        obj.put("mealType", component.mealType.name)
        obj.put("isEaten", component.isEaten)
        jsonArray.put(obj)
    }
    return jsonArray.toString()
}

fun deserializeMealPlan(jsonStr: String): List<MealComponent>? {
    return try {
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<MealComponent>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val foodObj = obj.getJSONObject("food")
            val food = FoodItem(
                foodObj.getString("name"),
                foodObj.getDouble("protein").toFloat(),
                foodObj.getDouble("carbs").toFloat(),
                foodObj.getDouble("fat").toFloat(),
                FoodType.valueOf(foodObj.getString("type")),
                if (foodObj.isNull("calories")) null else foodObj.getDouble("calories").toFloat(),
                foodObj.optDouble("servingSize", 100.0).toFloat(),
                foodObj.optString("servingUnit", "g")
            )
            list.add(
                MealComponent(
                    food, 
                    obj.optDouble("quantity", 1.0).toFloat(), 
                    MealType.valueOf(obj.getString("mealType")),
                    obj.optBoolean("isEaten", true)
                )
            )
        }
        list
    } catch (e: Exception) {
        Log.e("DietModels", "Error deserializing meal plan", e)
        null
    }
}
