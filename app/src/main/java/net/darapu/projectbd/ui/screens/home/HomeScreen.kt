package net.darapu.projectbd.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import net.darapu.projectbd.ui.components.InteractiveActivityRings
import net.darapu.projectbd.ui.components.MetricDetailRow
import net.darapu.projectbd.ui.components.MetricType
import net.darapu.projectbd.ui.components.RingData

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    val healthConnectClient = remember {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (healthConnectClient != null) {
                    coroutineScope.launch {
                        try {
                            val granted = healthConnectClient.permissionController.getGrantedPermissions()
                            if (granted.containsAll(permissions)) {
                                viewModel.syncActivity(healthConnectClient)
                            }
                        } catch (e: Exception) {
                            // Silent fail on home refresh
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

        // RINGS ROW IN CARDS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Fitness Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp).clickable(
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
                        size = 190.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val (value, label) = when (selectedFitnessMetric) {
                            MetricType.MOVE -> "${uiState.moveCalories}" to "kcal"
                            MetricType.EXERCISE -> "${uiState.exerciseMinutes}" to "min"
                            MetricType.STAND -> "${uiState.standHours}" to "hr"
                            MetricType.STEPS -> "${uiState.steps}" to "steps"
                            else -> "${uiState.steps}" to "steps"
                        }
                        Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // COMPACT LEGENDS IN CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricDetailRow(
                        modifier = Modifier.weight(1f),
                        label = "Move", 
                        value = "${uiState.moveCalories} kcal", 
                        color = Color(0xFFFA114F),
                        icon = Icons.Default.Whatshot,
                        isSelected = selectedFitnessMetric == MetricType.MOVE,
                        onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.MOVE) MetricType.NONE else MetricType.MOVE }
                    )
                    MetricDetailRow(
                        modifier = Modifier.weight(1f),
                        label = "Exercise", 
                        value = "${uiState.exerciseMinutes} min", 
                        color = Color(0xFFADFF2F),
                        icon = Icons.Default.Timer,
                        isSelected = selectedFitnessMetric == MetricType.EXERCISE,
                        onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.EXERCISE) MetricType.NONE else MetricType.EXERCISE }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricDetailRow(
                        modifier = Modifier.weight(1f),
                        label = "Stand", 
                        value = "${uiState.standHours} hr", 
                        color = Color(0xFF00FFFF),
                        icon = Icons.Default.AccessibilityNew,
                        isSelected = selectedFitnessMetric == MetricType.STAND,
                        onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.STAND) MetricType.NONE else MetricType.STAND }
                    )
                    MetricDetailRow(
                        modifier = Modifier.weight(1f),
                        label = "Steps", 
                        value = "${uiState.steps}", 
                        color = Color(0xFF00FFCC),
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        isSelected = selectedFitnessMetric == MetricType.STEPS,
                        onClick = { selectedFitnessMetric = if (selectedFitnessMetric == MetricType.STEPS) MetricType.NONE else MetricType.STEPS }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        // WORKOUT FREQUENCY PLACEHOLDER
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Workout Frequency Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Coming Soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
