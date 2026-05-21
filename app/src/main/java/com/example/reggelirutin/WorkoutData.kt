package com.example.reggelirutin

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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

    @Query("DELETE FROM workouts")
    suspend fun clearAllHistory()
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                // Új tábla létrehozása, ha még nem létezett
                connection.execSQL("CREATE TABLE IF NOT EXISTS `exercise_definitions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `setsReps` TEXT NOT NULL, `totalSets` INTEGER NOT NULL, `restSeconds` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL)")
                
                // Opcionális: Ha a tábla már létezett, de új oszlopok kerültek bele (pl. restSeconds, orderIndex)
                // SQLite-ban nincs "ADD COLUMN IF NOT EXISTS", ezért ha biztos benne, hogy ezek újak, akkor:
                // connection.execSQL("ALTER TABLE `exercise_definitions` ADD COLUMN `restSeconds` INTEGER NOT NULL DEFAULT 60")
                // connection.execSQL("ALTER TABLE `exercise_definitions` ADD COLUMN `orderIndex` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: android.content.Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(true) // Ha a migráció nem sikerül, törli és újratölti
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
