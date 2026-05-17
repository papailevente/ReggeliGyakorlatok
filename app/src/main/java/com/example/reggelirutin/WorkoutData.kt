package com.example.reggelirutin

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class Exercise(
    val id: Long = 0,
    val name: String,
    val description: String,
    val setsReps: String,
    val totalSets: Int = 4,
    val restSeconds: Int = 60,
    val orderIndex: Int = 0
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val totalTimeSeconds: Int
)

@Entity(
    tableName = "exercise_results",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class ExerciseResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseName: String,
    val setsDone: Int
)

@Entity(tableName = "exercise_definitions")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val setsReps: String,
    val totalSets: Int,
    val restSeconds: Int = 60,
    val orderIndex: Int = 0
)

data class WorkoutWithResults(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val results: List<ExerciseResultEntity>
)

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertExerciseResults(results: List<ExerciseResultEntity>)

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutWithResults>>

    @Transaction
    suspend fun saveWorkout(totalTime: Int, exerciseData: List<Pair<String, Int>>) {
        val workoutId = insertWorkout(WorkoutEntity(timestamp = System.currentTimeMillis(), totalTimeSeconds = totalTime))
        val results = exerciseData.map { (name, sets) ->
            ExerciseResultEntity(workoutId = workoutId, exerciseName = name, setsDone = sets)
        }
        insertExerciseResults(results)
    }

    // Exercise Definition Management
    @Query("SELECT * FROM exercise_definitions ORDER BY orderIndex ASC")
    fun getAllExerciseDefinitions(): Flow<List<ExerciseEntity>>

    @Insert
    suspend fun insertExerciseDefinition(exercise: ExerciseEntity)

    @Update
    suspend fun updateExerciseDefinition(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExerciseDefinition(exercise: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercise_definitions")
    suspend fun getExerciseCount(): Int
}

@Database(
    entities = [WorkoutEntity::class, ExerciseResultEntity::class, ExerciseEntity::class], 
    version = 2, 
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: android.content.Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                .fallbackToDestructiveMigration() // Dev app: simplified migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
