package net.darapu.projectbd.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.Flow
import net.darapu.projectbd.data.local.AppDatabase
import net.darapu.projectbd.data.local.DailyActivity
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class ActivityMetrics(
    val steps: Long = 0,
    val activeCalories: Double = 0.0,
    val exerciseMinutes: Long = 0,
    val standHours: Int = 0
)

class ActivityRepository(private val database: AppDatabase) {

    fun getActivityFlowForDate(date: String): Flow<DailyActivity?> {
        return database.dailyActivityDao().getActivityFlowForDate(date)
    }

    suspend fun getActivityForDate(date: String): DailyActivity? {
        return database.dailyActivityDao().getActivityForDate(date)
    }

    suspend fun syncActivity(healthConnectClient: HealthConnectClient): ActivityMetrics {
        val metrics = readActivityMetrics(healthConnectClient)
        val today = LocalDate.now().toString()
        val existing = database.dailyActivityDao().getActivityForDate(today)
        
        database.dailyActivityDao().insertOrUpdate(
            existing?.copy(
                steps = metrics.steps.toInt(),
                moveCalories = metrics.activeCalories.toInt(),
                exerciseMinutes = metrics.exerciseMinutes.toInt(),
                standHours = metrics.standHours
            ) ?: DailyActivity(
                date = today,
                steps = metrics.steps.toInt(),
                moveCalories = metrics.activeCalories.toInt(),
                exerciseMinutes = metrics.exerciseMinutes.toInt(),
                standHours = metrics.standHours
            )
        )
        return metrics
    }

    private suspend fun readActivityMetrics(healthConnectClient: HealthConnectClient): ActivityMetrics {
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
            Log.e("ActivityRepository", "Error reading health metrics", e)
            ActivityMetrics()
        }
    }
    
    suspend fun updateDirectly(steps: Int, moveCalories: Int, exerciseMinutes: Int, standHours: Int) {
        val today = LocalDate.now().toString()
        val existing = database.dailyActivityDao().getActivityForDate(today)
        database.dailyActivityDao().insertOrUpdate(
            existing?.copy(
                steps = steps,
                moveCalories = moveCalories,
                exerciseMinutes = exerciseMinutes,
                standHours = standHours
            ) ?: DailyActivity(
                date = today,
                steps = steps,
                moveCalories = moveCalories,
                exerciseMinutes = exerciseMinutes,
                standHours = standHours
            )
        )
    }

    suspend fun updateWorkoutPlan(planJson: String) {
        val today = LocalDate.now().toString()
        val existing = database.dailyActivityDao().getActivityForDate(today)
        database.dailyActivityDao().insertOrUpdate(
            existing?.copy(workoutPlan = planJson) ?: DailyActivity(date = today, workoutPlan = planJson)
        )
    }
}
