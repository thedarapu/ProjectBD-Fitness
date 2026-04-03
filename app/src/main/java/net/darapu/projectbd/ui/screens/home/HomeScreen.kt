package net.darapu.projectbd.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import net.darapu.projectbd.ui.components.InteractiveActivityRings
import net.darapu.projectbd.ui.components.MetricDetailRow
import net.darapu.projectbd.ui.components.MetricType
import net.darapu.projectbd.ui.components.RingData

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedFitnessMetric by remember { mutableStateOf(MetricType.NONE) }
    var selectedDietMetric by remember { mutableStateOf(MetricType.NONE) }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Daily Progress",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // FITNESS SECTION
        Text("Fitness Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { selectedFitnessMetric = MetricType.NONE }) {
            InteractiveActivityRings(
                rings = listOf(
                    RingData((uiState.moveCalories).toFloat() / uiState.targetMove.coerceAtLeast(1), Color(0xFFFA114F), MetricType.MOVE),
                    RingData((uiState.exerciseMinutes).toFloat() / uiState.targetExercise.coerceAtLeast(1), Color(0xFFADFF2F), MetricType.EXERCISE),
                    RingData((uiState.standHours).toFloat() / uiState.targetStand.coerceAtLeast(1), Color(0xFF00FFFF), MetricType.STAND),
                    RingData((uiState.steps).toFloat() / uiState.targetSteps.coerceAtLeast(1), Color(0xFF00FFCC), MetricType.STEPS)
                ),
                selectedMetric = selectedFitnessMetric,
                size = 220.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${uiState.steps}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "steps", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricDetailRow(
                label = "Move", 
                value = "${uiState.moveCalories} / ${uiState.targetMove} kcal", 
                color = Color(0xFFFA114F),
                icon = Icons.Default.Whatshot,
                isSelected = selectedFitnessMetric == MetricType.MOVE,
                onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.MOVE) MetricType.NONE else MetricType.MOVE }
            )
            MetricDetailRow(
                label = "Exercise", 
                value = "${uiState.exerciseMinutes} / ${uiState.targetExercise} min", 
                color = Color(0xFFADFF2F),
                icon = Icons.Default.Timer,
                isSelected = selectedFitnessMetric == MetricType.EXERCISE,
                onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.EXERCISE) MetricType.NONE else MetricType.EXERCISE }
            )
            MetricDetailRow(
                label = "Stand", 
                value = "${uiState.standHours} / ${uiState.targetStand} hr", 
                color = Color(0xFF00FFFF),
                icon = Icons.Default.AccessibilityNew,
                isSelected = selectedFitnessMetric == MetricType.STAND,
                onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.STAND) MetricType.NONE else MetricType.STAND }
            )
            MetricDetailRow(
                label = "Steps", 
                value = "${uiState.steps} / ${uiState.targetSteps}", 
                color = Color(0xFF00FFCC),
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                isSelected = selectedFitnessMetric == MetricType.STEPS,
                onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.STEPS) MetricType.NONE else MetricType.STEPS }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // NUTRITION SECTION
        Text("Nutrition & Macros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
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
                Text(text = "${uiState.totalCalories.roundToInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricDetailRow(
                label = "Calories", 
                value = "${uiState.totalCalories.roundToInt()} / ${uiState.targetCalories.roundToInt()} kcal", 
                color = Color(0xFFFFA500),
                icon = Icons.Default.Restaurant,
                isSelected = selectedDietMetric == MetricType.MOVE, 
                onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.MOVE) MetricType.NONE else MetricType.MOVE }
            )
            MetricDetailRow(
                label = "Protein", 
                value = "${uiState.totalProtein.roundToInt()} / ${uiState.targetProtein.roundToInt()} g", 
                color = Color(0xFF9370DB),
                icon = Icons.Default.Egg,
                isSelected = selectedDietMetric == MetricType.EXERCISE,
                onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.EXERCISE) MetricType.NONE else MetricType.EXERCISE }
            )
            MetricDetailRow(
                label = "Carbs", 
                value = "${uiState.totalCarbs.roundToInt()} / ${uiState.targetCarbs.roundToInt()} g", 
                color = Color(0xFFFFD700),
                icon = Icons.Default.BakeryDining,
                isSelected = selectedDietMetric == MetricType.STAND,
                onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.STAND) MetricType.NONE else MetricType.STAND }
            )
            MetricDetailRow(
                label = "Fat", 
                value = "${uiState.totalFat.roundToInt()} / ${uiState.targetFat.roundToInt()} g", 
                color = Color(0xFFFF69B4),
                icon = Icons.Default.Icecream,
                isSelected = selectedDietMetric == MetricType.STEPS,
                onClick = { selectedDietMetric = if (selectedDietMetric == MetricType.STEPS) MetricType.NONE else MetricType.STEPS }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
