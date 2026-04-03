package net.darapu.projectbd.ui.screens.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.darapu.projectbd.data.local.AppDatabase
import net.darapu.projectbd.data.local.DailyActivity
import net.darapu.projectbd.domain.models.deserializeMealPlan
import net.darapu.projectbd.domain.models.deserializeWorkoutPlan
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryContent() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val activities by db.dailyActivityDao().getAllActivities().collectAsState(initial = emptyList())
    
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateString = selectedDate.toString()

    Column(modifier = Modifier.fillMaxSize()) {
        IndicatorCalendar(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            activities = activities
        )
        
        HorizontalDivider()
        
        Box(modifier = Modifier.weight(1f).padding(8.dp)) {
            DaySummarySection(dateString, forceExpanded = true, showOnlyIfData = true)
        }
    }
}

@Composable
fun IndicatorCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    activities: List<DailyActivity>
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstOfMonth = currentMonth.atDay(1)
    val dayOfWeekOffset = firstOfMonth.dayOfWeek.value % 7 // 0 for Sunday if we start there

    Column(modifier = Modifier.padding(16.dp)) {
        // Month Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }

        // Days of Week Header
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        val totalCells = daysInMonth + dayOfWeekOffset
        val rows = (totalCells + 6) / 7
        
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - dayOfWeekOffset + 1
                    
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayOfMonth)
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()
                            
                            val activityForDay = activities.find { it.date == date.toString() }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { onDateSelected(date) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                )
                                
                                // Indicators
                                if (activityForDay != null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        // Steps: Teal
                                        if (activityForDay.steps > 0) {
                                            Box(modifier = Modifier.size(4.dp).background(Color(0xFF008080), CircleShape))
                                        }
                                        // Workouts: Orange
                                        val hasWorkout = activityForDay.workoutPlan?.let { deserializeWorkoutPlan(it) }?.any { day ->
                                            day.exercises.any { ex -> ex.sets.any { s -> s.reps.isNotEmpty() || s.weight.isNotEmpty() } }
                                        } ?: false
                                        if (hasWorkout) {
                                            Box(modifier = Modifier.size(4.dp).background(Color(0xFFFFA500), CircleShape))
                                        }
                                        // Diet: Blue
                                        val hasDiet = !activityForDay.mealsJson.isNullOrEmpty() && (deserializeMealPlan(activityForDay.mealsJson ?: "")?.any { it.isEaten } == true)
                                        if (hasDiet) {
                                            Box(modifier = Modifier.size(4.dp).background(Color(0xFF2196F3), CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DaySummarySection(dateString: String, activity: DailyActivity? = null, forceExpanded: Boolean = false, showOnlyIfData: Boolean = false) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    
    val activityToUse = activity ?: produceState<DailyActivity?>(initialValue = null, key1 = dateString) {
        value = db.dailyActivityDao().getActivityForDate(dateString)
    }.value
    
    if (activityToUse == null) {
        if (showOnlyIfData) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data recorded for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    var expandedState by remember { mutableStateOf(false) }
    val isExpanded = forceExpanded || expandedState
    
    val allMeals = activityToUse.mealsJson?.let { deserializeMealPlan(it) }
    val eatenMeals = allMeals?.filter { it.isEaten }
    
    val workoutDays = activityToUse.workoutPlan?.let { deserializeWorkoutPlan(it) }
    
    val completedWorkouts = workoutDays?.mapNotNull { day ->
        val completedExercises = day.exercises.mapNotNull { ex ->
            val completedSets = ex.sets.filter { it.reps.isNotEmpty() || it.weight.isNotEmpty() }
            if (completedSets.isNotEmpty()) ex.copy(sets = completedSets.toMutableList()) else null
        }
        if (completedExercises.isNotEmpty()) day.copy(exercises = completedExercises) else null
    }

    val hasData = (eatenMeals?.isNotEmpty() == true) || (completedWorkouts?.isNotEmpty() == true) || (activityToUse.steps > 0)
    
    if (!hasData) {
        if (showOnlyIfData) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data recorded for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!forceExpanded) Modifier.clickable { expandedState = !expandedState } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp).animateContentSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Summary: $dateString",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                if (!forceExpanded) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null
                    )
                }
            }
            
            if (!isExpanded) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (activityToUse.steps > 0) Text("Steps: ${activityToUse.steps}", fontSize = 12.sp)
                    val mealCal = eatenMeals?.sumOf { it.totalCalories.toDouble() }?.toInt() ?: 0
                    if (mealCal > 0) Text("Calories: $mealCal", fontSize = 12.sp)
                    Text("Click for details", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(modifier = if (forceExpanded) Modifier.verticalScroll(rememberScrollState()) else Modifier) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Steps
                    if (activityToUse.steps > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Steps: ${activityToUse.steps}", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Workouts
                    if (completedWorkouts?.isNotEmpty() == true) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Workouts:", fontWeight = FontWeight.Bold)
                                completedWorkouts.forEach { day ->
                                    Text("• ${day.title}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    day.exercises.forEach { ex ->
                                        Text("  - ${ex.name}:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        ex.sets.forEachIndexed { i, set ->
                                            Text("    Set ${i+1}: ${set.reps} reps @ ${set.weight}", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Meals
                    if (eatenMeals?.isNotEmpty() == true) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Meals:", fontWeight = FontWeight.Bold)
                                eatenMeals.groupBy { it.mealType }.forEach { (type, components) ->
                                    Text(text = type.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    components.forEach {
                                        val displayQty = if (it.quantity % 1f == 0f) it.quantity.toInt() else it.quantity
                                        Text("  - ${it.food.name} ($displayQty x ${it.food.servingSize.roundToInt()}${it.food.servingUnit})", fontSize = 11.sp)
                                    }
                                }
                                val totalCal = eatenMeals.sumOf { it.totalCalories.toDouble() }.toInt()
                                Text("Total Calories: $totalCal", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
