package net.darapu.projectbd.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val steps: Int = 0,
    val moveCalories: Int = 0,
    val exerciseMinutes: Int = 0,
    val standHours: Int = 0,
    val workoutPlan: String? = null,
    val workoutCompleted: Boolean = false,
    val mealsJson: String? = null
)

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity WHERE date = :date")
    suspend fun getActivityForDate(date: String): DailyActivity?

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    fun getAllActivities(): Flow<List<DailyActivity>>

    @Upsert
    suspend fun insertOrUpdate(activity: DailyActivity)
}

@Database(entities = [DailyActivity::class], version = 3) // Version bumped from 2 to 3
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyActivityDao(): DailyActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projectbd_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
