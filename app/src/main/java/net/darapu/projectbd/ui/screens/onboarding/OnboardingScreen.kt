package net.darapu.projectbd.ui.screens.onboarding

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import net.darapu.projectbd.domain.models.ActivityLevel
import net.darapu.projectbd.domain.usecase.CalculateMacrosUseCase
import net.darapu.projectbd.ui.components.ModernDatePickerDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE) }
    var currentStep by remember { mutableIntStateOf(0) }
    
    var dobText by remember { mutableStateOf("") }
    var ageYears by remember { mutableIntStateOf(25) }
    var weightLbs by remember { mutableStateOf("150") }
    var heightFt by remember { mutableStateOf("5") }
    var heightIn by remember { mutableStateOf("9") }
    var isMale by remember { mutableStateOf(true) }
    var selectedGoals by remember { mutableStateOf(setOf<String>()) }
    var selectedActivityLevel by remember { mutableStateOf(ActivityLevel.LIGHTLY_ACTIVE) }
    var targetSteps by remember { mutableStateOf("10000") }
    var targetActiveCalories by remember { mutableStateOf("500") }
    var targetExerciseMinutes by remember { mutableStateOf("30") }
    var targetStandHours by remember { mutableStateOf("12") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val availableGoals = listOf("Fat Loss", "Build Muscle", "Stamina", "Flexibility", "Maintenance")

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 9f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (currentStep) {
                    0 -> OnboardingStep(
                        title = "Welcome to ProjectBD!",
                        subtitle = "Let's set up your personalized diet profile. First, what's your date of birth?"
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(if (dobText.isEmpty()) "Select Date of Birth" else "DOB: $dobText (Age: $ageYears)")
                        }
                    }
                    1 -> OnboardingStep(
                        title = "Current Weight",
                        subtitle = "This helps us calculate your baseline calorie needs."
                    ) {
                        OutlinedTextField(
                            value = weightLbs,
                            onValueChange = { weightLbs = it },
                            label = { Text("Weight (lbs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> OnboardingStep(
                        title = "What's your height?",
                        subtitle = "Height is a key factor in determining your body mass index and TDEE."
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = heightFt,
                                onValueChange = { heightFt = it },
                                label = { Text("Feet") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = heightIn,
                                onValueChange = { heightIn = it },
                                label = { Text("Inches") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    3 -> OnboardingStep(
                        title = "Select Gender",
                        subtitle = "Metabolic rates vary between biological genders."
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FilterChip(
                                selected = isMale,
                                onClick = { isMale = true },
                                label = { Text("Male") },
                                leadingIcon = if (isMale) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                            )
                            FilterChip(
                                selected = !isMale,
                                onClick = { isMale = false },
                                label = { Text("Female") },
                                leadingIcon = if (!isMale) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                            )
                        }
                    }
                    4 -> OnboardingStep(
                        title = "Activity Level",
                        subtitle = "How active is your lifestyle? This determines your base calorie burn."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActivityLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = selectedActivityLevel == level,
                                    onClick = { selectedActivityLevel = level },
                                    label = { 
                                        Column {
                                            Text(level.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                                            Text(level.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    5 -> OnboardingStep(
                        title = "Activity Goals",
                        subtitle = "What are your target daily activity metrics?"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = targetSteps,
                                onValueChange = { targetSteps = it },
                                label = { Text("Target Daily Steps") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = targetActiveCalories,
                                onValueChange = { targetActiveCalories = it },
                                label = { Text("Target Active Calories (Move)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = targetExerciseMinutes,
                                onValueChange = { targetExerciseMinutes = it },
                                label = { Text("Target Exercise Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = targetStandHours,
                                onValueChange = { targetStandHours = it },
                                label = { Text("Target Stand Hours") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    6 -> OnboardingStep(
                        title = "What are your goals?",
                        subtitle = "Select one or more to adjust your calorie and protein targets."
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableGoals.forEach { goal ->
                                FilterChip(
                                    selected = selectedGoals.contains(goal),
                                    onClick = {
                                        selectedGoals = if (selectedGoals.contains(goal)) {
                                            selectedGoals - goal
                                        } else {
                                            selectedGoals + goal
                                        }
                                    },
                                    label = { Text(goal) }
                                )
                            }
                        }
                    }
                    7 -> OnboardingStep(
                        title = "All Set!",
                        subtitle = "We're ready to calculate your quantified diet plan."
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Summary:", fontWeight = FontWeight.Bold)
                                Text("• Age: $ageYears")
                                Text("• Weight: $weightLbs lbs")
                                Text("• Height: $heightFt'$heightIn\"")
                                Text("• Gender: ${if (isMale) "Male" else "Female"}")
                                Text("• Activity: ${selectedActivityLevel.name.replace("_", " ")}")
                                Text("• Targets: $targetSteps steps, $targetActiveCalories kcal, $targetExerciseMinutes min, $targetStandHours stand")
                                Text("• Goals: ${selectedGoals.joinToString(", ")}")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 7) {
                            currentStep++
                        } else {
                            val targets = CalculateMacrosUseCase().invoke(
                                weightLbs = weightLbs.toFloatOrNull() ?: 150f,
                                heightFt = heightFt.toFloatOrNull() ?: 5f,
                                heightIn = heightIn.toFloatOrNull() ?: 9f,
                                ageYears = ageYears,
                                isMale = isMale,
                                activityLevel = selectedActivityLevel,
                                goals = selectedGoals
                            )

                            sharedPrefs.edit().apply {
                                putString("user_dob", dobText)
                                putInt("user_age", ageYears)
                                putString("user_weight", weightLbs)
                                putString("user_height_ft", heightFt)
                                putString("user_height_in", heightIn)
                                putBoolean("user_is_male", isMale)
                                putString("user_activity_level", selectedActivityLevel.name)
                                putInt("target_steps", targetSteps.toIntOrNull() ?: 10000)
                                putInt("target_active_calories", targetActiveCalories.toIntOrNull() ?: 500)
                                putInt("target_exercise_minutes", targetExerciseMinutes.toIntOrNull() ?: 30)
                                putInt("target_stand_hours", targetStandHours.toIntOrNull() ?: 12)
                                putStringSet("user_goals", selectedGoals)
                                putFloat("target_calories", targets.targetCalories)
                                putFloat("target_protein", targets.targetProtein)
                                putBoolean("onboarding_complete", true)
                            }.apply()
                            onComplete()
                        }
                    },
                    enabled = when (currentStep) {
                        0 -> dobText.isNotEmpty()
                        6 -> selectedGoals.isNotEmpty()
                        else -> true
                    }
                ) {
                    Text(if (currentStep == 7) "Finish" else "Next")
                }
            }
        }
    }
}

@Composable
fun OnboardingStep(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        content()
    }
}
