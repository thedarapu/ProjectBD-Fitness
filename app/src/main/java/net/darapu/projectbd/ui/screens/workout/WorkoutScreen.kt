package net.darapu.projectbd.ui.screens.workout

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
import net.darapu.projectbd.domain.models.SetRecord
import net.darapu.projectbd.domain.models.WorkoutDay
import java.time.Duration
import java.time.ZonedDateTime

data class ActivityMetrics(
    val steps: Long = 0,
    val activeCalories: Double = 0.0,
    val exerciseMinutes: Long = 0,
    val standHours: Int = 0
)

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

    val refreshMetrics = {
        if (healthConnectClient != null && permissionsGranted) {
            coroutineScope.launch {
                isRefreshing = true
                val metrics = readActivityMetrics(healthConnectClient)
                viewModel.updateMetrics(metrics.steps, metrics.activeCalories, metrics.exerciseMinutes, metrics.standHours)
                isRefreshing = false
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(permissions)) {
            permissionsGranted = true
            refreshMetrics()
        } else {
            Toast.makeText(context, "Some permissions not granted. Activity rings may be incomplete.", Toast.LENGTH_LONG).show()
            permissionsGranted = true
            refreshMetrics()
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
                                val metrics = readActivityMetrics(healthConnectClient)
                                viewModel.updateMetrics(metrics.steps, metrics.activeCalories, metrics.exerciseMinutes, metrics.standHours)
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
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Daily Activity",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isRefreshing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }

        item {
            if (healthConnectClient == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Health Connect is not available on this device.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open Play Store", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Install Health Connect")
                    }
                }
            } else if (!permissionsGranted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Permissions required to read activity data.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            requestPermissionLauncher.launch(permissions)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Health Permissions")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(settingsIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Health Connect Settings")
                    }
                }
            } else {
                WorkoutActivityRings(
                    metrics = ActivityMetrics(uiState.steps, uiState.activeCalories, uiState.exerciseMinutes, uiState.standHours),
                    goals = ActivityMetrics(uiState.stepGoal.toLong(), uiState.moveGoal.toDouble(), uiState.exerciseGoal.toLong(), uiState.standGoal)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { refreshMetrics() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    enabled = !isRefreshing
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRefreshing) "Refreshing..." else "Refresh Activity")
                }
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Text(
                text = "Workout Plan",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
                    onDayUpdated = { updatedDay ->
                        val newList = uiState.workoutDays.toMutableList()
                        newList[dayIndex] = updatedDay
                        viewModel.updateWorkoutPlan(newList)
                    }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun WorkoutActivityRings(metrics: ActivityMetrics, goals: ActivityMetrics) {
    var selectedMetric by remember { mutableStateOf(MetricType.NONE) }
    
    val moveProgress = if (goals.activeCalories > 0) (metrics.activeCalories / goals.activeCalories).toFloat() else 0f
    val exerciseProgress = if (goals.exerciseMinutes > 0) (metrics.exerciseMinutes.toFloat() / goals.exerciseMinutes.toFloat()) else 0f
    val standProgress = if (goals.standHours > 0) (metrics.standHours.toFloat() / goals.standHours.toFloat()) else 0f
    val stepsProgress = if (goals.steps > 0) (metrics.steps.toFloat() / goals.steps.toFloat()) else 0f

    InteractiveActivityRings(
        rings = listOf(
            RingData(moveProgress, Color(0xFFFA114F), MetricType.MOVE),
            RingData(exerciseProgress, Color(0xFFADFF2F), MetricType.EXERCISE),
            RingData(standProgress, Color(0xFF00FFFF), MetricType.STAND),
            RingData(stepsProgress, Color(0xFF00FFCC), MetricType.STEPS)
        ),
        selectedMetric = selectedMetric,
        size = 240.dp
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricDetailRow(
            label = "Move", 
            value = "${metrics.activeCalories.toInt()} / ${goals.activeCalories.toInt()} kcal", 
            color = Color(0xFFFA114F),
            icon = Icons.Default.Whatshot,
            isSelected = selectedMetric == MetricType.MOVE,
            onClick = { selectedMetric = if (selectedMetric == MetricType.MOVE) MetricType.NONE else MetricType.MOVE }
        )
        MetricDetailRow(
            label = "Exercise", 
            value = "${metrics.exerciseMinutes} / ${goals.exerciseMinutes} min", 
            color = Color(0xFFADFF2F),
            icon = Icons.Default.Timer,
            isSelected = selectedMetric == MetricType.EXERCISE,
            onClick = { selectedMetric = if (selectedMetric == MetricType.EXERCISE) MetricType.NONE else MetricType.EXERCISE }
        )
        MetricDetailRow(
            label = "Stand", 
            value = "${metrics.standHours} / ${goals.standHours} hr", 
            color = Color(0xFF00FFFF),
            icon = Icons.Default.AccessibilityNew,
            isSelected = selectedMetric == MetricType.STAND,
            onClick = { selectedMetric = if (selectedMetric == MetricType.STAND) MetricType.NONE else MetricType.STAND }
        )
        MetricDetailRow(
            label = "Steps", 
            value = "${metrics.steps} / ${goals.steps}", 
            color = Color(0xFF00FFCC),
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            isSelected = selectedMetric == MetricType.STEPS,
            onClick = { selectedMetric = if (selectedMetric == MetricType.STEPS) MetricType.NONE else MetricType.STEPS }
        )
    }
}

suspend fun readActivityMetrics(healthConnectClient: HealthConnectClient): ActivityMetrics {
    val now = ZonedDateTime.now()
    val startOfDay = now.toLocalDate().atStartOfDay(now.zone).toInstant()
    val endOfDay = now.toInstant()
    
    return try {
        val aggregateResponse = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
        )
        
        val steps = aggregateResponse[StepsRecord.COUNT_TOTAL] ?: 0L
        val activeCals = aggregateResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        
        val exerciseResponse = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
        )
        val exerciseMillis = exerciseResponse.records.sumOf { 
            Duration.between(it.startTime, it.endTime).toMillis() 
        }
        val exerciseMinutes = exerciseMillis / (1000 * 60)
        
        var standHours = 0
        for (i in 0 until now.hour + 1) {
            val hourStart = now.toLocalDate().atStartOfDay(now.zone).plusHours(i.toLong()).toInstant()
            val hourEnd = hourStart.plus(Duration.ofHours(1))
            
            val hourAggregate = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(hourStart, if (hourEnd.isBefore(endOfDay)) hourEnd else endOfDay)
                )
            )
            
            val hourSteps = hourAggregate[StepsRecord.COUNT_TOTAL] ?: 0L
            val hourCals = hourAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
            
            if (hourSteps > 50 || hourCals > 5.0) {
                standHours++
            }
        }

        ActivityMetrics(steps, activeCals, exerciseMinutes, standHours)
    } catch (e: Exception) {
        Log.e("WorkoutScreen", "Error reading health metrics", e)
        ActivityMetrics()
    }
}

@Composable
fun WorkoutDayCard(day: WorkoutDay, onDayUpdated: (WorkoutDay) -> Unit) {
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
                        }
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
fun ExerciseSection(exercise: Exercise, onExerciseUpdated: (Exercise) -> Unit) {
    Column {
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
        
        exercise.sets.forEachIndexed { setIndex, setRecord ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                
                IconButton(onClick = {
                    val newSets = exercise.sets.toMutableList()
                    newSets.removeAt(setIndex)
                    onExerciseUpdated(exercise.copy(sets = newSets))
                }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Set", tint = Color.Gray, modifier = Modifier.size(16.dp))
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
