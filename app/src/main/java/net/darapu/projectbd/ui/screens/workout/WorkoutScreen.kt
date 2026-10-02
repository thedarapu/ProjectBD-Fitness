package net.darapu.projectbd.ui.screens.workout

import net.darapu.projectbd.data.repository.ActivityMetrics

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import net.darapu.projectbd.domain.models.Exercise
import net.darapu.projectbd.ui.components.InteractiveActivityRings
import net.darapu.projectbd.ui.components.MetricDetailRow
import net.darapu.projectbd.ui.components.MetricType
import net.darapu.projectbd.ui.components.RingData
import net.darapu.projectbd.ui.components.MuscleGroupMap
import net.darapu.projectbd.domain.models.SetRecord
import net.darapu.projectbd.domain.models.WorkoutDay
import net.darapu.projectbd.domain.usecase.CalculateOneRepMaxUseCase
import java.time.Duration
import java.time.ZonedDateTime
import androidx.compose.animation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape


@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        val healthConnectClient = remember {
            try {
                HealthConnectClient.getOrCreate(context)
            } catch (e: Exception) {
                Log.e("WorkoutScreen", "Health Connect Init Error", e)
                null
            }
        }

        var permissionsGranted by remember { mutableStateOf(false) }
        var isRefreshing by remember { mutableStateOf(false) }

        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )

        val refreshMetrics = { isSilent: Boolean ->
            if (healthConnectClient != null && permissionsGranted) {
                viewModel.updateMetrics(healthConnectClient, isSilent)
            }
        }

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(permissions)) {
                permissionsGranted = true
                refreshMetrics(false)
            } else {
                Toast.makeText(context, "Some permissions not granted. Activity rings may be incomplete.", Toast.LENGTH_LONG).show()
                permissionsGranted = true
                refreshMetrics(false)
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (healthConnectClient != null) {
                        coroutineScope.launch {
                            try {
                                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                                val allGranted = granted.containsAll(permissions)
                                permissionsGranted = allGranted
                                if (allGranted) {
                                    viewModel.updateMetrics(healthConnectClient, isSilent = true)
                                }
                            } catch (e: Exception) {
                                Log.e("WorkoutScreen", "Error on resume", e)
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

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Removed Daily Activity section as per user request
            
            item {
                Text(
                    text = "Workout Plan",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.workoutDays.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No workout plan created yet.", textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                Toast.makeText(context, "Please go to Config to create a plan.", Toast.LENGTH_LONG).show()
                            }) {
                                Icon(Icons.Default.Build, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Plan in Config")
                            }
                        }
                    }
                }
            } else {
                items(uiState.workoutDays.size) { dayIndex ->
                    WorkoutDayCard(
                        day = uiState.workoutDays[dayIndex],
                        restTimerSeconds = uiState.restTimerSeconds,
                        isTimerRunning = uiState.isTimerRunning,
                        onDayUpdated = { updatedDay ->
                            val newList = uiState.workoutDays.toMutableList()
                            newList[dayIndex] = updatedDay
                            viewModel.updateWorkoutPlan(newList)
                        },
                        onStartTimer = { viewModel.startRestTimer(90) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Persistent Rest Timer Overlay
        if (uiState.isTimerRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    onClick = { viewModel.cancelRestTimer() },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Rest Timer: ${uiState.restTimerSeconds}s",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// WorkoutActivityRings section removed per user request

@Composable
fun WorkoutDayCard(
    day: WorkoutDay, 
    restTimerSeconds: Int,
    isTimerRunning: Boolean,
    onDayUpdated: (WorkoutDay) -> Unit,
    onStartTimer: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                day.exercises.forEachIndexed { exIndex, exercise ->
                    ExerciseSection(
                        exercise = exercise,
                        onExerciseUpdated = { updatedEx ->
                            val newExercises = day.exercises.toMutableList()
                            newExercises[exIndex] = updatedEx
                            onDayUpdated(day.copy(exercises = newExercises))
                        },
                        onStartTimer = onStartTimer
                    )
                    if (exIndex < day.exercises.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseSection(
    exercise: Exercise, 
    onExerciseUpdated: (Exercise) -> Unit, 
    onStartTimer: () -> Unit
) {
    val oneRepMaxUseCase = remember { CalculateOneRepMaxUseCase() }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Target: ${exercise.targetSets} sets of ${exercise.targetReps} (${exercise.targetWeight})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (exercise.primaryMuscles.isNotEmpty() || exercise.secondaryMuscles.isNotEmpty()) {
                MuscleGroupMap(
                    exercise = exercise,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        
        exercise.sets.forEachIndexed { setIndex, setRecord ->
            val weightVal = setRecord.weight.toFloatOrNull() ?: 0f
            val repsVal = setRecord.reps.toIntOrNull() ?: 0
            val estimated1RM = if (weightVal > 0 && repsVal > 0) oneRepMaxUseCase(weightVal, repsVal) else 0

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = setRecord.reps.isNotEmpty() && setRecord.weight.isNotEmpty(), // Simplified completion check
                    onCheckedChange = { if (it) onStartTimer() },
                    modifier = Modifier.size(24.dp)
                )

                Text(text = "Set ${setIndex + 1}", modifier = Modifier.width(45.dp), fontSize = 14.sp)
                
                OutlinedTextField(
                    value = setRecord.reps,
                    onValueChange = { 
                        val newSets = exercise.sets.toMutableList()
                        newSets[setIndex] = setRecord.copy(reps = it)
                        onExerciseUpdated(exercise.copy(sets = newSets))
                    },
                    label = { Text("Reps", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = setRecord.weight,
                    onValueChange = { 
                        val newSets = exercise.sets.toMutableList()
                        newSets[setIndex] = setRecord.copy(weight = it)
                        onExerciseUpdated(exercise.copy(sets = newSets))
                    },
                    label = { Text("Weight", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                    if (estimated1RM > 0) {
                        Text("1RM", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("$estimated1RM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        IconButton(onClick = {
                            val newSets = exercise.sets.toMutableList()
                            newSets.removeAt(setIndex)
                            onExerciseUpdated(exercise.copy(sets = newSets))
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Set", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        TextButton(
            onClick = {
                val newSets = exercise.sets.toMutableList()
                newSets.add(SetRecord())
                onExerciseUpdated(exercise.copy(sets = newSets))
            },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Set")
        }
    }
}
