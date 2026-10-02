package net.darapu.projectbd.ui.screens.onboarding

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import net.darapu.projectbd.domain.models.ExperienceLevel
import net.darapu.projectbd.domain.models.EquipmentProfile
import net.darapu.projectbd.data.repository.SettingsRepository

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    var currentStep by remember { mutableIntStateOf(0) }
    
    var experienceLevel by remember { mutableStateOf(ExperienceLevel.BEGINNER) }
    var equipmentProfile by remember { mutableStateOf(EquipmentProfile.FULL_GYM) }
    var daysToTrain by remember { mutableIntStateOf(3) }
    
    var targetSteps by remember { mutableStateOf("10000") }
    var targetActiveCalories by remember { mutableStateOf("500") }
    var targetExerciseMinutes by remember { mutableStateOf("30") }
    var targetStandHours by remember { mutableStateOf("12") }
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (currentStep) {
                    0 -> OnboardingStep(
                        title = "Welcome to ProjectBD!",
                        subtitle = "Let's personalize your strength training journey. What is your experience level?"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExperienceLevel.entries.forEach { level ->
                                OutlinedCard(
                                    onClick = { experienceLevel = level },
                                    border = BorderStroke(
                                        2.dp, 
                                        if (experienceLevel == level) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (experienceLevel == level) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                                         else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(level.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text(level.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    1 -> OnboardingStep(
                        title = "Available Equipment",
                        subtitle = "This helps us generate workouts tailored to your environment."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            EquipmentProfile.entries.forEach { profile ->
                                OutlinedCard(
                                    onClick = { equipmentProfile = profile },
                                    border = BorderStroke(
                                        2.dp, 
                                        if (equipmentProfile == profile) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (equipmentProfile == profile) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                                         else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(profile.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text(profile.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    2 -> OnboardingStep(
                        title = "Training Frequency",
                        subtitle = "How many days per week can you realistically commit to training?"
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$daysToTrain Days",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = daysToTrain.toFloat(),
                                onValueChange = { daysToTrain = it.toInt() },
                                valueRange = 1f..7f,
                                steps = 5,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Text(
                                text = when(daysToTrain) {
                                    in 1..2 -> "Light Frequency"
                                    in 3..4 -> "Moderate Frequency"
                                    in 5..6 -> "High Frequency"
                                    else -> "Elite Athlete"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    3 -> OnboardingStep(
                        title = "Daily Activity Goals",
                        subtitle = "While we focus on lifting, daily movement still matters."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = targetSteps,
                                onValueChange = { targetSteps = it },
                                label = { Text("Daily Steps") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = targetActiveCalories,
                                onValueChange = { targetActiveCalories = it },
                                label = { Text("Active Calories (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = targetExerciseMinutes,
                                onValueChange = { targetExerciseMinutes = it },
                                label = { Text("Exercise Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    4 -> OnboardingStep(
                        title = "All Set!",
                        subtitle = "You're ready to start your strength-focused training plan."
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Lifting Profile:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Experience: ${experienceLevel.displayName}")
                                Text("• Equipment: ${equipmentProfile.displayName}")
                                Text("• Frequency: $daysToTrain days/week")
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Activity Targets:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Steps: $targetSteps")
                                Text("• Calories: $targetActiveCalories kcal")
                                Text("• Exercise: $targetExerciseMinutes min")
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
                        if (currentStep < 4) {
                            currentStep++
                        } else {
                            settingsRepository.saveLiftingProfile(
                                experience = experienceLevel.name,
                                equipment = equipmentProfile.name,
                                daysToTrain = daysToTrain
                            )
                            settingsRepository.saveFitnessTargets(
                                steps = targetSteps.toIntOrNull() ?: 10000,
                                calories = targetActiveCalories.toIntOrNull() ?: 500,
                                minutes = targetExerciseMinutes.toIntOrNull() ?: 30,
                                standHours = targetStandHours.toIntOrNull() ?: 12
                            )
                            settingsRepository.setOnboardingComplete(true)
                            onComplete()
                        }
                    }
                ) {
                    Text(if (currentStep == 4) "Finish" else "Next")
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
