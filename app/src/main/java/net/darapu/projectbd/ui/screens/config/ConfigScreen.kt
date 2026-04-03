package net.darapu.projectbd.ui.screens.config

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.launch
import net.darapu.projectbd.data.local.AppDatabase
import net.darapu.projectbd.data.local.DailyActivity
import net.darapu.projectbd.domain.models.ActivityLevel
import net.darapu.projectbd.domain.usecase.CalculateMacrosUseCase
import net.darapu.projectbd.domain.usecase.WorkoutPlanGenerator
import net.darapu.projectbd.ui.components.ModernDatePickerDialog
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun ConfigScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Configuration", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                AppSettingsSection()
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Diet Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                DietConfigSection()
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Fitness & Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                FitnessConfigSection()
            }
        }


        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AppSettingsSection() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE) }
    
    val savedTheme = sharedPrefs.getString("app_theme", "System Default") ?: "System Default"
    var selectedTheme by remember { mutableStateOf(savedTheme) }
    val themeOptions = listOf("System Default", "Light", "Dark")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Theme:", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(selectedTheme)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                themeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedTheme = option
                            sharedPrefs.edit { putString("app_theme", option) }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FitnessConfigSection() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    
    var targetSteps by remember { mutableStateOf(sharedPrefs.getInt("target_steps", 10000).toString()) }
    var targetActiveCalories by remember { mutableStateOf(sharedPrefs.getInt("target_active_calories", 500).toString()) }
    var targetExerciseMinutes by remember { mutableStateOf(sharedPrefs.getInt("target_exercise_minutes", 30).toString()) }
    var targetStandHours by remember { mutableStateOf(sharedPrefs.getInt("target_stand_hours", 12).toString()) }
    var workoutDays by remember { mutableStateOf(sharedPrefs.getInt("workout_days", 3).toString()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = targetSteps,
            onValueChange = { targetSteps = it },
            label = { Text("Daily Step Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = targetActiveCalories,
            onValueChange = { targetActiveCalories = it },
            label = { Text("Daily Active Calories Target (Move)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = targetExerciseMinutes,
            onValueChange = { targetExerciseMinutes = it },
            label = { Text("Daily Exercise Minutes Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = targetStandHours,
            onValueChange = { targetStandHours = it },
            label = { Text("Daily Stand Hours Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = workoutDays,
            onValueChange = { workoutDays = it },
            label = { Text("Workout Days Per Week") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                sharedPrefs.edit { 
                    putInt("target_steps", targetSteps.toIntOrNull() ?: 10000)
                    putInt("target_active_calories", targetActiveCalories.toIntOrNull() ?: 500)
                    putInt("target_exercise_minutes", targetExerciseMinutes.toIntOrNull() ?: 30)
                    putInt("target_stand_hours", targetStandHours.toIntOrNull() ?: 12)
                    putInt("workout_days", workoutDays.toIntOrNull() ?: 3)
                }
                Toast.makeText(context, "Targets updated!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Fitness Goals")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val goals = sharedPrefs.getStringSet("user_goals", emptySet()) ?: emptySet()
                val days = workoutDays.toIntOrNull() ?: 3
                val plan = WorkoutPlanGenerator.generatePlan(days, goals)
                sharedPrefs.edit { putString("workout_plan_json", plan) }
                
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val existing = db.dailyActivityDao().getActivityForDate(today)
                    db.dailyActivityDao().insertOrUpdate(
                        existing?.copy(workoutPlan = plan) ?: DailyActivity(date = today, workoutPlan = plan)
                    )
                }

                Toast.makeText(context, "Workout Plan Created!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Plan Workouts")
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietConfigSection() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE) }
    
    var dobText by remember { mutableStateOf(sharedPrefs.getString("user_dob", "") ?: "") }
    var ageYears by remember { mutableIntStateOf(sharedPrefs.getInt("user_age", 25)) }
    var weightLbs by remember { mutableStateOf(sharedPrefs.getString("user_weight", "150") ?: "150") }
    var heightFt by remember { mutableStateOf(sharedPrefs.getString("user_height_ft", "5") ?: "5") }
    var heightIn by remember { mutableStateOf(sharedPrefs.getString("user_height_in", "9") ?: "9") }
    var isMale by remember { mutableStateOf(sharedPrefs.getBoolean("user_is_male", true)) }
    
    val savedActivityStr = sharedPrefs.getString("user_activity_level", ActivityLevel.LIGHTLY_ACTIVE.name)
    var selectedActivityLevel by remember { mutableStateOf(ActivityLevel.valueOf(savedActivityStr ?: ActivityLevel.LIGHTLY_ACTIVE.name)) }
    
    val savedGoals = sharedPrefs.getStringSet("user_goals", setOf("Build Muscle")) ?: setOf("Build Muscle")
    var selectedGoals by remember { mutableStateOf(savedGoals) }
    val availableGoals = listOf("Fat Loss", "Build Muscle", "Stamina", "Flexibility", "Maintenance")

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        ModernDatePickerDialog(
            onDateSelected = { millis ->
                if (millis != null) {
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    val date = Date(millis)
                    dobText = sdf.format(date)
                    
                    val dobCalendar = Calendar.getInstance().apply { time = date }
                    val today = Calendar.getInstance()
                    var age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }
                    ageYears = max(0, age)
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(text = if (dobText.isEmpty()) "Select Date of Birth" else "DOB: $dobText (Age: $ageYears)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = weightLbs, onValueChange = { weightLbs = it },
            label = { Text("Weight (lbs)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = heightFt, onValueChange = { heightFt = it },
                label = { Text("Height (ft)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightIn, onValueChange = { heightIn = it },
                label = { Text("Height (in)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isMale, onClick = { isMale = true })
                Text("Male")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !isMale, onClick = { isMale = false })
                Text("Female")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Activity Level:", fontWeight = FontWeight.Bold)
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(selectedActivityLevel.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                ActivityLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(level.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                                Text(level.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            selectedActivityLevel = level
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Goals:", fontWeight = FontWeight.Bold)
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availableGoals.forEach { goalOption ->
                FilterChip(
                    selected = selectedGoals.contains(goalOption),
                    onClick = {
                        selectedGoals = if (selectedGoals.contains(goalOption)) {
                            selectedGoals - goalOption
                        } else {
                            selectedGoals + goalOption
                        }
                    },
                    label = { Text(goalOption) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val targets = CalculateMacrosUseCase().invoke(
                    weightLbs = weightLbs.toFloatOrNull() ?: 150f,
                    heightFt = heightFt.toFloatOrNull() ?: 5f,
                    heightIn = heightIn.toFloatOrNull() ?: 9f,
                    ageYears = ageYears,
                    isMale = isMale,
                    activityLevel = selectedActivityLevel,
                    goals = selectedGoals
                )
                
                sharedPrefs.edit {
                    putString("user_dob", dobText)
                    putInt("user_age", ageYears)
                    putString("user_weight", weightLbs)
                    putString("user_height_ft", heightFt)
                    putString("user_height_in", heightIn)
                    putBoolean("user_is_male", isMale)
                    putString("user_activity_level", selectedActivityLevel.name)
                    putStringSet("user_goals", selectedGoals)
                    putFloat("target_calories", targets.targetCalories)
                    putFloat("target_protein", targets.targetProtein)
                }
                Toast.makeText(context, "Targets updated!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedGoals.isNotEmpty()
        ) {
            Text("Update Macros & Targets")
        }
    }
}