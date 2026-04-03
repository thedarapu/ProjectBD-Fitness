package net.darapu.projectbd.data.network

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.darapu.projectbd.BuildConfig
import net.darapu.projectbd.domain.models.FoodItem
import net.darapu.projectbd.domain.models.FoodType
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object FatSecretApi {

    private const val BASE_URL = "https://platform.fatsecret.com/rest/server.api"

    suspend fun searchFoods(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        val consumerKey = BuildConfig.FATSECRET_CLIENT_ID
        val consumerSecret = BuildConfig.FATSECRET_CLIENT_SECRET

        try {
            val params = TreeMap<String, String>()
            params["method"] = "foods.search"
            params["search_expression"] = query
            params["format"] = "json"
            params["oauth_consumer_key"] = consumerKey
            params["oauth_signature_method"] = "HMAC-SHA1"
            params["oauth_timestamp"] = (System.currentTimeMillis() / 1000).toString()
            params["oauth_nonce"] = UUID.randomUUID().toString().replace("-", "")
            params["oauth_version"] = "1.0"

            val signature = generateOAuthSignature("GET", BASE_URL, params, consumerSecret)
            params["oauth_signature"] = signature

            val urlString = "$BASE_URL?" + params.entries.joinToString("&") {
                "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = InputStreamReader(connection.inputStream).readText()
                val json = JSONObject(response)
                
                if (json.has("error")) {
                    val err = json.getJSONObject("error")
                    Log.e("FatSecretApi", "API Error ${err.optInt("code")}: ${err.optString("message")}")
                    return@withContext emptyList()
                }

                val foodsObj = json.optJSONObject("foods") ?: return@withContext emptyList()
                val foodsArray = foodsObj.optJSONArray("food")
                
                val results = mutableListOf<FoodItem>()
                if (foodsArray != null) {
                    for (i in 0 until foodsArray.length()) {
                        results.add(parseFoodJson(foodsArray.getJSONObject(i)))
                    }
                } else {
                    val singleFood = foodsObj.optJSONObject("food")
                    if (singleFood != null) results.add(parseFoodJson(singleFood))
                }
                return@withContext results
            }
        } catch (e: Exception) {
            Log.e("FatSecretApi", "Search Failed", e)
        }
        return@withContext emptyList()
    }

    suspend fun getFoodByBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        Log.d("FatSecretApi", "Searching for barcode: $barcode")
        
        // 1. First, try Edamam (it's much better for barcodes)
        val edamamFood = EdamamApi.getFoodByBarcode(barcode)
        if (edamamFood != null) {
            Log.d("FatSecretApi", "Found food on Edamam: ${edamamFood.name}")
            return@withContext edamamFood
        }

        // 2. Fallback to FatSecret search (maybe it's in the name)
        Log.d("FatSecretApi", "Barcode not found on Edamam, trying FatSecret search")
        val searchResults = searchFoods(barcode)
        if (searchResults.isNotEmpty()) {
            return@withContext searchResults[0]
        }
        
        // 3. Try with leading zero
        if (barcode.length == 12) {
            val retryEdamam = EdamamApi.getFoodByBarcode("0$barcode")
            if (retryEdamam != null) return@withContext retryEdamam
            
            val retryResults = searchFoods("0$barcode")
            if (retryResults.isNotEmpty()) return@withContext retryResults[0]
        }
        
        Log.d("FatSecretApi", "No results found for barcode: $barcode")
        null
    }

    suspend fun getFoodDetails(foodId: String): FoodItem? = withContext(Dispatchers.IO) {
        Log.d("FatSecretApi", "Fetching details for foodId: $foodId")
        val consumerKey = BuildConfig.FATSECRET_CLIENT_ID
        val consumerSecret = BuildConfig.FATSECRET_CLIENT_SECRET

        try {
            val params = TreeMap<String, String>()
            params["method"] = "food.get"
            params["food_id"] = foodId
            params["format"] = "json"
            params["oauth_consumer_key"] = consumerKey
            params["oauth_signature_method"] = "HMAC-SHA1"
            params["oauth_timestamp"] = (System.currentTimeMillis() / 1000).toString()
            params["oauth_nonce"] = UUID.randomUUID().toString().replace("-", "")
            params["oauth_version"] = "1.0"

            val signature = generateOAuthSignature("GET", BASE_URL, params, consumerSecret)
            params["oauth_signature"] = signature

            val urlString = "$BASE_URL?" + params.entries.joinToString("&") {
                "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
            }

            val connection = URL(urlString).openConnection() as HttpURLConnection
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = InputStreamReader(connection.inputStream).readText()
                Log.d("FatSecretApi", "Details Response: $response")
                val json = JSONObject(response)
                
                if (json.has("error")) {
                    val err = json.getJSONObject("error")
                    Log.e("FatSecretApi", "Details API Error: ${err.optString("message")}")
                    return@withContext null
                }

                val foodObj = json.optJSONObject("food") ?: return@withContext null
                val name = foodObj.getString("food_name")
                
                val servingsContainer = foodObj.optJSONObject("servings")
                if (servingsContainer != null) {
                    val serving: JSONObject? = servingsContainer.optJSONArray("serving")?.let { array ->
                        var found: JSONObject? = if (array.length() > 0) array.getJSONObject(0) else null
                        for (i in 0 until array.length()) {
                            val s = array.getJSONObject(i)
                            if (s.optString("metric_serving_unit") == "g" && s.optString("metric_serving_amount") == "100") {
                                found = s
                                break
                            }
                        }
                        found
                    } ?: servingsContainer.optJSONObject("serving")

                    if (serving != null) {
                        val protein = serving.optString("protein").toFloatOrNull() ?: 0f
                        val carbs = serving.optString("carbohydrate").toFloatOrNull() ?: 0f
                        val fat = serving.optString("fat").toFloatOrNull() ?: 0f
                        val calories = serving.optString("calories").toFloatOrNull() ?: 0f
                        
                        Log.d("FatSecretApi", "Parsed Macros: P:$protein, C:$carbs, F:$fat, Cal:$calories")
                        return@withContext FoodItem(
                            name, protein, carbs, fat, determineFoodType(protein, carbs, fat), calories, 100f, "g"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FatSecretApi", "Get details failed", e)
        }
        null
    }

    private fun generateOAuthSignature(method: String, url: String, params: TreeMap<String, String>, secret: String): String {
        val paramString = params.entries.joinToString("&") {
            "${encode(it.key)}=${encode(it.value)}"
        }
        val baseString = "${method.uppercase()}&${encode(url)}&${encode(paramString)}"
        val signingKey = "${encode(secret)}&"
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(signingKey.toByteArray(), "HmacSHA1"))
        val rawHmac = mac.doFinal(baseString.toByteArray())
        return Base64.encodeToString(rawHmac, Base64.NO_WRAP)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")

    private fun parseFoodJson(item: JSONObject): FoodItem {
        val name = item.getString("food_name")
        val description = item.optString("food_description", "")
        val (protein, carbs, fat, calories) = parseMacros(description)
        return FoodItem(name, protein, carbs, fat, determineFoodType(protein, carbs, fat), calories, 100f, "g")
    }

    private fun parseMacros(description: String): FloatArray {
        var protein = 0f; var carbs = 0f; var fat = 0f; var calories = 0f
        try {
            description.split("|").forEach { part ->
                val clean = part.trim()
                if (clean.contains("Protein:", true)) protein = clean.substringAfter(":").replace("g","").trim().toFloatOrNull() ?: 0f
                if (clean.contains("Carbs:", true)) carbs = clean.substringAfter(":").replace("g","").trim().toFloatOrNull() ?: 0f
                if (clean.contains("Fat:", true)) fat = clean.substringAfter(":").replace("g","").trim().toFloatOrNull() ?: 0f
                if (clean.contains("Calories:", true)) calories = clean.substringAfter(":").replace("kcal","").trim().toFloatOrNull() ?: 0f
            }
        } catch (e: Exception) { }
        return floatArrayOf(protein, carbs, fat, calories)
    }
    
    private fun determineFoodType(protein: Float, carbs: Float, fat: Float): FoodType {
        val total = protein + carbs + fat
        if (total == 0f) return FoodType.VEGGIE
        return when {
            protein/total > 0.4 -> FoodType.PROTEIN
            carbs/total > 0.4 -> FoodType.CARB
            else -> FoodType.FAT
        }
    }
}
