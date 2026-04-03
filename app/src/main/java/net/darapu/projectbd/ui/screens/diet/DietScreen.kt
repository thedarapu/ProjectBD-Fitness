package net.darapu.projectbd.ui.screens.diet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import net.darapu.projectbd.data.network.EdamamApi
import net.darapu.projectbd.domain.models.FoodItem
import net.darapu.projectbd.domain.models.FoodType
import net.darapu.projectbd.domain.models.MealComponent
import net.darapu.projectbd.domain.models.MealType
import net.darapu.projectbd.domain.models.deserializeMealPlan
import net.darapu.projectbd.domain.models.serializeMealPlan
import net.darapu.projectbd.ui.components.InteractiveActivityRings
import net.darapu.projectbd.ui.components.MetricDetailRow
import net.darapu.projectbd.ui.components.MetricType
import net.darapu.projectbd.ui.components.RingData
import java.util.Locale
import kotlin.math.roundToInt

fun saveMealsToHistory(context: Context, mealPlan: List<MealComponent>) {
    try {
        val jsonStr = serializeMealPlan(mealPlan)
        context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE).edit {
            putString("last_logged_meals", jsonStr)
        }
    } catch (e: Exception) {
        Log.e("DietScreen", "Error saving meals to history", e)
    }
}

fun loadLastLoggedMeals(context: Context): List<MealComponent>? {
    val jsonStr = context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE)
        .getString("last_logged_meals", null) ?: return null
    return deserializeMealPlan(jsonStr)
}

val localFoodDatabase = listOf(
    FoodItem("Chicken Breast (Raw)", 31f, 0f, 3.6f, FoodType.PROTEIN, servingSize = 100f, servingUnit = "g"),
    FoodItem("Eggs (Whole)", 13f, 1.1f, 11f, FoodType.PROTEIN, servingSize = 50f, servingUnit = "Egg"),
    FoodItem("Paneer", 18f, 1.2f, 20f, FoodType.PROTEIN, servingSize = 100f, servingUnit = "g"),
    FoodItem("Orgain Protein Powder", 45.6f, 32.6f, 8.7f, FoodType.PROTEIN, servingSize = 46f, servingUnit = "Scoop"),
    FoodItem("Barbells Protein Bar", 36.3f, 29f, 14.5f, FoodType.PROTEIN, servingSize = 55f, servingUnit = "Bar"),
    FoodItem("White Rice (Raw)", 7f, 80f, 1f, FoodType.CARB, servingSize = 45f, servingUnit = "g"),
    FoodItem("Bread / Bagels", 9f, 49f, 1f, FoodType.CARB, servingSize = 100f, servingUnit = "g"),
    FoodItem("Milk (Low Fat)", 3.4f, 5f, 1f, FoodType.CARB, servingSize = 240f, servingUnit = "Cup"),
    FoodItem("Oats", 13f, 68f, 6f, FoodType.CARB, servingSize = 40f, servingUnit = "g"),
    FoodItem("Sweet Potato", 2f, 20f, 0.1f, FoodType.CARB, servingSize = 100f, servingUnit = "g"),
    FoodItem("Vegetable (Generic)", 2f, 5f, 0.2f, FoodType.VEGGIE, servingSize = 100f, servingUnit = "g"),
    FoodItem("Broccoli", 2.8f, 7f, 0.3f, FoodType.VEGGIE, servingSize = 100f, servingUnit = "g"),
    FoodItem("Spinach", 2.9f, 3.6f, 0.4f, FoodType.VEGGIE, servingSize = 100f, servingUnit = "g")
)

fun String.capitalizeWords(): String = lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

@Composable
fun DietScreen(
    viewModel: DietViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialogForMeal by remember { mutableStateOf<MealType?>(null) }
    var showSwapDialog by remember { mutableStateOf<MealComponent?>(null) }
    var showEditQtyDialog by remember { mutableStateOf<MealComponent?>(null) }
    var selectedDietMetric by remember { mutableStateOf(MetricType.NONE) }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(240.dp).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { selectedDietMetric = MetricType.NONE }) {
                InteractiveActivityRings(
                    rings = listOf(
                        RingData(uiState.totalCalories / uiState.targetCalories.coerceAtLeast(1f), Color(0xFFFFA500), MetricType.MOVE),
                        RingData(uiState.totalProtein / uiState.targetProtein.coerceAtLeast(1f), Color(0xFF9370DB), MetricType.EXERCISE),
                        RingData(uiState.totalCarbs / uiState.targetCarbs.coerceAtLeast(1f), Color(0xFFFFD700), MetricType.STAND),
                        RingData(uiState.totalFat / uiState.targetFat.coerceAtLeast(1f), Color(0xFFFF69B4), MetricType.STEPS)
                    ),
                    selectedMetric = selectedDietMetric,
                    size = 220.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val centerVal = when(selectedDietMetric) {
                        MetricType.MOVE -> uiState.totalCalories.roundToInt()
                        MetricType.EXERCISE -> uiState.totalProtein.roundToInt()
                        MetricType.STAND -> uiState.totalCarbs.roundToInt()
                        MetricType.STEPS -> uiState.totalFat.roundToInt()
                        else -> uiState.totalCalories.roundToInt()
                    }
                    val centerUnit = if (selectedDietMetric == MetricType.MOVE || selectedDietMetric == MetricType.NONE) "kcal" else "g"
                    Text(text = "$centerVal", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = centerUnit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricDetailRow(label = "Calories", value = "${uiState.totalCalories.roundToInt()} / ${uiState.targetCalories.roundToInt()} kcal", color = Color(0xFFFFA500), icon = Icons.Default.Restaurant, isSelected = selectedDietMetric == MetricType.MOVE, onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.MOVE) MetricType.NONE else MetricType.MOVE })
                MetricDetailRow(label = "Protein", value = "${uiState.totalProtein.roundToInt()} / ${uiState.targetProtein.roundToInt()} g", color = Color(0xFF9370DB), icon = Icons.Default.Egg, isSelected = selectedDietMetric == MetricType.EXERCISE, onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.EXERCISE) MetricType.NONE else MetricType.EXERCISE })
                MetricDetailRow(label = "Carbs", value = "${uiState.totalCarbs.roundToInt()} / ${uiState.targetCarbs.roundToInt()} g", color = Color(0xFFFFD700), icon = Icons.Default.BakeryDining, isSelected = selectedDietMetric == MetricType.STAND, onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.STAND) MetricType.NONE else MetricType.STAND })
                MetricDetailRow(label = "Fat", value = "${uiState.totalFat.roundToInt()} / ${uiState.targetFat.roundToInt()} g", color = Color(0xFFFF69B4), icon = Icons.Default.Icecream, isSelected = selectedDietMetric == MetricType.STEPS, onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.STEPS) MetricType.NONE else MetricType.STEPS })
            }
            
            if (uiState.trackedMeals.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = { 
                    loadLastLoggedMeals(context)?.let { 
                        viewModel.updateMeals(it)
                        Toast.makeText(context, "Copied last logged meals", Toast.LENGTH_SHORT).show() 
                    } 
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.History, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Copy Last Logged Meals")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        MealType.entries.forEach { mealType ->
            val entries = uiState.trackedMeals.filter { it.mealType == mealType }
            val mealTotal = entries.sumOf { it.totalCalories.toDouble() }.toFloat().roundToInt()
            
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${mealType.name.capitalizeWords()} ($mealTotal kcal)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { showAddDialogForMeal = mealType }) { Icon(Icons.Default.Add, contentDescription = "Log Food") }
                }
            }
            
            if (entries.isEmpty()) {
                item { Text("Nothing tracked for ${mealType.name.lowercase()}.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp)) }
            } else {
                items(entries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showEditQtyDialog = entry }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                val displayQty = if (entry.quantity % 1f == 0f) entry.quantity.toInt().toString() else entry.quantity.toString()
                                val unitLabel = entry.food.servingUnit.capitalizeWords()
                                val weightStr = if (entry.food.servingSize % 1f == 0f) entry.food.servingSize.toInt().toString() else entry.food.servingSize.toString()
                                
                                val servingText = if (unitLabel.lowercase() in listOf("g", "ml")) {
                                    "$displayQty $unitLabel"
                                } else {
                                    "$displayQty $unitLabel (${weightStr}g)"
                                }
                                
                                Text(text = entry.food.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(servingText, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("Cal: ${entry.totalCalories.roundToInt()} | P: ${entry.totalProtein.roundToInt()}g | C: ${entry.totalCarbs.roundToInt()}g | F: ${entry.totalFat.roundToInt()}g", fontSize = 11.sp, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = { showSwapDialog = entry }) { Icon(Icons.Default.Edit, contentDescription = "Replace", modifier = Modifier.size(20.dp)) }
                                IconButton(onClick = { viewModel.removeEntry(entry) }) { Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }

    if (showEditQtyDialog != null) {
        val entry = showEditQtyDialog!!
        var qtyText by remember { mutableStateOf(if (entry.quantity % 1f == 0f) entry.quantity.toInt().toString() else entry.quantity.toString()) }
        var unitText by remember { mutableStateOf(entry.food.servingUnit.capitalizeWords()) }
        var sizeText by remember { mutableStateOf(if (entry.food.servingSize % 1f == 0f) entry.food.servingSize.toInt().toString() else entry.food.servingSize.toString()) }
        
        AlertDialog(
            onDismissRequest = { showEditQtyDialog = null },
            title = { Text("Edit Serving & Quantity") },
            text = {
                Column {
                    Text(entry.food.name, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qtyText, onValueChange = { qtyText = it },
                        label = { Text("Number of Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = unitText, onValueChange = { unitText = it },
                        label = { Text("Serving Unit (e.g., Cup, Serving)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sizeText, onValueChange = { sizeText = it },
                        label = { Text("Serving Weight/Volume (g/ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newQty = qtyText.toFloatOrNull() ?: entry.quantity
                    val newSize = sizeText.toFloatOrNull() ?: entry.food.servingSize
                    val newUnit = unitText.ifBlank { entry.food.servingUnit }
                    
                    val updatedFood = entry.food.copy(servingSize = newSize, servingUnit = newUnit)
                    val updatedEntry = entry.copy(food = updatedFood, quantity = newQty)
                    
                    val currentMeals = uiState.trackedMeals.toMutableList()
                    val index = currentMeals.indexOf(entry)
                    if (index != -1) {
                        currentMeals[index] = updatedEntry
                    }
                    viewModel.updateMeals(currentMeals)
                    
                    showEditQtyDialog = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditQtyDialog = null }) { Text("Cancel") } }
        )
    }

    if (showAddDialogForMeal != null || showSwapDialog != null) {
        val entryToReplace = showSwapDialog
        val activeMealType = showAddDialogForMeal ?: entryToReplace?.mealType ?: MealType.LUNCH
        var searchQuery by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
        var isApiLoading by remember { mutableStateOf(false) }
        var hasSearched by remember { mutableStateOf(false) }
        var isScanning by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) isScanning = true }

        if (isScanning) {
            AlertDialog(onDismissRequest = { isScanning = false }, properties = DialogProperties(usePlatformDefaultWidth = false), text = {
                Box(modifier = Modifier.fillMaxSize()) {
                    BarcodeScanner(onBarcodeScanned = { barcode: String ->
                        isScanning = false; isApiLoading = true
                        coroutineScope.launch {
                            val food = EdamamApi.getFoodByBarcode(barcode)
                            if (food != null) { 
                                viewModel.addOrUpdateEntry(food, 1f, activeMealType, entryToReplace); 
                                showSwapDialog = null; showAddDialogForMeal = null 
                            }
                            else Toast.makeText(context, "Food not found", Toast.LENGTH_LONG).show()
                            isApiLoading = false
                        }
                    }, onDismiss = { isScanning = false })
                }
            }, confirmButton = {})
        } else {
            AlertDialog(
                onDismissRequest = { showAddDialogForMeal = null; showSwapDialog = null },
                title = { Text(if (entryToReplace == null) "Log for ${activeMealType.name.capitalizeWords()}" else "Replace ${entryToReplace.food.name}") },
                text = {
                    Column {
                        Button(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) isScanning = true else cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Scan Barcode")
                        }
                        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search foods...") }, trailingIcon = { IconButton(onClick = { if (searchQuery.isNotBlank()) { isApiLoading = true; hasSearched = true; coroutineScope.launch { searchResults = EdamamApi.searchFoods(searchQuery); isApiLoading = false } } }) { Icon(Icons.Default.Search, contentDescription = "Search") } }, singleLine = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isApiLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                        else if (searchResults.isEmpty() && !hasSearched) {
                            Text("Quick Add", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            LazyColumn(modifier = Modifier.height(300.dp)) {
                                items(localFoodDatabase) { newFood -> FoodSelectionCard(newFood) { viewModel.addOrUpdateEntry(newFood, 1f, activeMealType, entryToReplace); showSwapDialog = null; showAddDialogForMeal = null } }
                            }
                        } else if (searchResults.isEmpty() && hasSearched) {
                            Column(modifier = Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No results found.", color = Color.Gray)
                                Button(onClick = { hasSearched = false; searchQuery = "" }) { Text("Back") }
                            }
                        } else {
                            Text("API Results", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            LazyColumn(modifier = Modifier.height(300.dp)) {
                                items(searchResults) { newFood -> FoodSelectionCard(newFood) { viewModel.addOrUpdateEntry(newFood, 1f, activeMealType, entryToReplace); showSwapDialog = null; showAddDialogForMeal = null } }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAddDialogForMeal = null; showSwapDialog = null }) { Text("Cancel") } }
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun BarcodeScanner(onBarcodeScanned: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasScanned by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val scanner = BarcodeScanning.getClient()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !hasScanned) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        val barcodeValue = barcodes[0].rawValue
                                        if (barcodeValue != null) {
                                            if (!hasScanned) {
                                                hasScanned = true
                                                onBarcodeScanned(barcodeValue)
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("BarcodeScanner", "Use case binding failed", e)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
        
        Box(
            modifier = Modifier
                .size(250.dp)
                .border(2.dp, Color.White)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun FoodSelectionCard(newFood: FoodItem, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(newFood.name, fontWeight = FontWeight.Bold)
            val unitLabel = newFood.servingUnit.capitalizeWords()
            val weightStr = if (newFood.servingSize % 1f == 0f) newFood.servingSize.toInt().toString() else newFood.servingSize.toString()
            
            val servingText = if (unitLabel.lowercase() in listOf("g", "ml")) {
                "1 $unitLabel"
            } else {
                "1 $unitLabel (${weightStr}g)"
            }
            
            Text("Serving size: $servingText", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Macros per serving:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Cal: ${newFood.caloriesPerServing.roundToInt()} | P: ${newFood.proteinPerServing.roundToInt()}g | C: ${newFood.carbsPerServing.roundToInt()}g | F: ${newFood.fatPerServing.roundToInt()}g", fontSize = 11.sp, color = Color.Gray)
        }
    }
}