package net.darapu.projectbd.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.darapu.projectbd.domain.models.FoodItem
import net.darapu.projectbd.domain.models.FoodType
import net.darapu.projectbd.BuildConfig
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object EdamamApi {
    private const val BASE_URL = "https://api.edamam.com/api/food-database/v2/parser"
    
    private val APP_ID = BuildConfig.EDAMAM_APP_ID.trim()
    private val APP_KEY = BuildConfig.EDAMAM_APP_KEY.trim()

    suspend fun searchFoods(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "$BASE_URL?app_id=$APP_ID&app_key=$APP_KEY&ingr=$encodedQuery"
            return@withContext performRequest(urlString)
        } catch (e: Exception) {
            Log.e("EdamamApi", "Search failed", e)
        }
        emptyList()
    }

    suspend fun getFoodByBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        try {
            val urlString = "$BASE_URL?app_id=$APP_ID&app_key=$APP_KEY&upc=$barcode"
            var results = performRequest(urlString)
            
            if (results.isEmpty() && barcode.length == 12) {
                val retryUrl = "$BASE_URL?app_id=$APP_ID&app_key=$APP_KEY&upc=0$barcode"
                results = performRequest(retryUrl)
            }

            if (results.isNotEmpty()) return@withContext results[0]
        } catch (e: Exception) {
            Log.e("EdamamApi", "Barcode search failed", e)
        }
        null
    }

    private fun performRequest(urlString: String): List<FoodItem> {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = InputStreamReader(connection.inputStream).readText()
                return parseEdamamResponse(response)
            }
        } catch (e: Exception) {
            Log.e("EdamamApi", "Request failed", e)
        }
        return emptyList()
    }

    private fun parseEdamamResponse(response: String): List<FoodItem> {
        val results = mutableListOf<FoodItem>()
        try {
            val json = JSONObject(response)
            
            // Collect all hints to find measures for items that might be in 'parsed'
            val allHints = json.optJSONArray("hints")
            val foodIdToMeasures = mutableMapOf<String, List<Pair<String, Float>>>()
            
            if (allHints != null) {
                for (i in 0 until allHints.length()) {
                    val hint = allHints.getJSONObject(i)
                    val food = hint.optJSONObject("food")
                    val measures = hint.optJSONArray("measures")
                    if (food != null && measures != null) {
                        val mList = mutableListOf<Pair<String, Float>>()
                        for (j in 0 until measures.length()) {
                            val m = measures.getJSONObject(j)
                            mList.add(m.optString("label") to m.optDouble("weight", 100.0).toFloat())
                        }
                        foodIdToMeasures[food.optString("foodId", "")] = mList
                    }
                }
            }

            // 1. Process 'parsed' array (common for barcode scans)
            val parsedArr = json.optJSONArray("parsed")
            if (parsedArr != null && parsedArr.length() > 0) {
                for (i in 0 until parsedArr.length()) {
                    val entry = parsedArr.getJSONObject(i)
                    val foodObj = entry.optJSONObject("food") ?: continue
                    val foodId = foodObj.optString("foodId", "")
                    val measureObj = entry.optJSONObject("measure")
                    
                    var weight = 100f
                    var label = "g"
                    
                    if (measureObj != null) {
                        weight = measureObj.optDouble("weight", 100.0).toFloat()
                        label = measureObj.optString("label", "Serving")
                    }
                    
                    // Try to find specific measure from hints if parsed measure is generic
                    if (label.equals("Serving", true) || label.equals("g", true) || label.equals("Gram", true)) {
                        val hintsMeasures = foodIdToMeasures[foodId]
                        if (hintsMeasures != null && hintsMeasures.isNotEmpty()) {
                            val best = findBestMeasure(hintsMeasures)
                            weight = best.first
                            label = best.second
                        }
                    }
                    
                    results.add(extractFoodFromJson(foodObj, weight, label))
                }
            }
            
            // 2. Process 'hints' if parsed didn't give us what we needed
            if (results.isEmpty() && allHints != null) {
                for (i in 0 until allHints.length()) {
                    val hint = allHints.getJSONObject(i)
                    val foodObj = hint.optJSONObject("food") ?: continue
                    val measures = hint.optJSONArray("measures")
                    
                    val mList = mutableListOf<Pair<String, Float>>()
                    if (measures != null) {
                        for (j in 0 until measures.length()) {
                            val m = measures.getJSONObject(j)
                            mList.add(m.optString("label") to m.optDouble("weight", 100.0).toFloat())
                        }
                    }
                    val (weight, label) = findBestMeasure(mList)
                    results.add(extractFoodFromJson(foodObj, weight, label))
                }
            }
        } catch (e: Exception) {
            Log.e("EdamamApi", "Parsing error", e)
        }
        return results
    }

    private fun findBestMeasure(measures: List<Pair<String, Float>>): Pair<Float, String> {
        if (measures.isEmpty()) return 100f to "g"
        
        val priorityLabels = listOf("Cup", "Bar", "Scoop", "Package", "Container", "Whole", "Unit", "Piece", "Serving")
        for (pLabel in priorityLabels) {
            val match = measures.find { it.first.contains(pLabel, ignoreCase = true) }
            if (match != null) return match.second to match.first
        }
        
        return measures[0].second to measures[0].first
    }

    private fun extractFoodFromJson(food: JSONObject, servingWeight: Float, servingLabel: String): FoodItem {
        val label = food.getString("label")
        val brand = food.optString("brand", "")
        val fullName = if (brand.isNotBlank() && !label.contains(brand, true)) "$brand - $label" else label
        val nutrients = food.getJSONObject("nutrients")
        
        return FoodItem(
            name = fullName,
            protein = nutrients.optDouble("PROCNT", 0.0).toFloat(),
            carbs = nutrients.optDouble("CHOCDF", 0.0).toFloat(),
            fat = nutrients.optDouble("FAT", 0.0).toFloat(),
            type = FoodType.PROTEIN,
            overrideCalories = nutrients.optDouble("ENERC_KCAL", 0.0).toFloat(),
            servingSize = servingWeight,
            servingUnit = servingLabel
        )
    }
}
