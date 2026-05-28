package com.example.reggelirutin

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutViewModel(context: Context) : ViewModel() {
    private val dao = WorkoutDatabase.getDatabase(context).workoutDao()
    val allWorkouts: StateFlow<List<WorkoutWithResults>> = dao.getAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Workout State
    var totalTime = mutableIntStateOf(0)
    var exerciseTime = mutableIntStateOf(0)
    var isTotalRunning = mutableStateOf(false)
    var isExerciseRunning = mutableStateOf(false)

    var currentRestTime = mutableIntStateOf(0)
    var isRestRunning = mutableStateOf(false)
    var restExerciseIndex = mutableIntStateOf(-1)

    // Dynamic Exercise List
    var exercises = mutableStateListOf<Exercise>()
    val currentSet = mutableStateListOf<Int>()
    var workoutDone = mutableStateOf(false)
    var currentExerciseIndex = mutableIntStateOf(0)
    var selectedDay = mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK).let { 
        if (it == java.util.Calendar.SUNDAY) 7 else it - 1 // Convert to 1-7 (Mon-Sun)
    })

    // Mode and Group Management
    var currentMode = mutableStateOf<AppMode>(AppMode.None)
    var selectedGroupId = mutableStateOf<Long?>(null)
    var exerciseGroups = mutableStateListOf<ExerciseGroupEntity>()
    var allGroups = mutableStateListOf<ExerciseGroupEntity>()

    // Showcase State
    var showcaseStep = mutableIntStateOf(-1) // -1 means inactive
    var isShowcaseDismissed = mutableStateOf(false)

    private var totalTimerJob: Job? = null
    private var exerciseTimerJob: Job? = null
    private var restTimerJob: Job? = null

    init {
        viewModelScope.launch {
            // Seed database if empty
            if (dao.getExerciseCount() == 0) {
                seedDatabase()
            }
            
            // Listen to all groups
            launch {
                dao.getAllGroups().collectLatest { groups ->
                    allGroups.clear()
                    allGroups.addAll(groups)
                }
            }
            
            // Listen to groups for the selected day
            launch {
                snapshotFlow { selectedDay.intValue }.collectLatest { day ->
                    dao.getGroupsForDay(day).collectLatest { groups ->
                        exerciseGroups.clear()
                        exerciseGroups.addAll(groups)
                    }
                }
            }

            // Listen to exercise definitions based on mode and group
            snapshotFlow { 
                Triple(selectedDay.intValue, currentMode.value, selectedGroupId.value) 
            }.collectLatest { (day, mode, groupId) ->
                val flow = when {
                    mode == AppMode.Extra && groupId != null -> dao.getExerciseDefinitionsForGroup(groupId, day)
                    else -> dao.getExerciseDefinitionsForDay(day)
                }

                flow.collectLatest { entities ->
                    exercises.clear()
                    exercises.addAll(entities.map { it.toDomain() })
                    
                    // Synchronize sets Done list
                    if (currentSet.size != exercises.size) {
                        currentSet.clear()
                        currentSet.addAll(List(exercises.size) { 0 })
                    }
                    
                    // Reset progress if criteria changes
                    currentExerciseIndex.intValue = 0
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        val defaults = listOf(
            ExerciseEntity(name = "Guggolás", description = "(saját testsúly)", setsReps = "4 × 20-25", totalSets = 1, orderIndex = 0),
            ExerciseEntity(name = "Fekvőtámasz", description = "(rendes, segítővel)", setsReps = "4 sorozat (max → 15-18 → 12-15 → max)", totalSets = 4, orderIndex = 1),
            ExerciseEntity(name = "Glute Bridge", description = "", setsReps = "4 × 20", totalSets = 4, orderIndex = 2),
            ExerciseEntity(name = "Planking", description = "", setsReps = "4 × 60 mp", totalSets = 4, restSeconds = 60, orderIndex = 3),
            ExerciseEntity(name = "Bicepsz Curl", description = "(8 kg)", setsReps = "3 × 12-15", totalSets = 3, orderIndex = 4),
            ExerciseEntity(name = "Overhead Press", description = "(állva, 8 kg)", setsReps = "3 × 12-15", totalSets = 3, orderIndex = 5),
            ExerciseEntity(name = "Oldalemelés", description = "(8 kg)", setsReps = "3 × 12-15", totalSets = 3, orderIndex = 6),
            ExerciseEntity(name = "Bent Over Row", description = "(8 kg)", setsReps = "3 × 12-15", totalSets = 3, orderIndex = 7)
        )
        defaults.forEach { dao.insertExerciseDefinition(it) }
    }

    fun addExercise(name: String, desc: String, reps: String, sets: Int) {
        viewModelScope.launch {
            dao.insertExerciseDefinition(ExerciseEntity(
                name = name,
                description = desc,
                setsReps = reps,
                totalSets = sets,
                orderIndex = exercises.size,
                dayOfWeek = selectedDay.intValue,
                groupId = selectedGroupId.value ?: 0
            ))
        }
    }

    fun addGroup(name: String) {
        viewModelScope.launch {
            dao.insertGroup(ExerciseGroupEntity(
                name = name,
                dayOfWeek = selectedDay.intValue,
                orderIndex = exerciseGroups.size
            ))
        }
    }

    fun updateGroup(group: ExerciseGroupEntity) {
        viewModelScope.launch {
            dao.updateGroup(group)
        }
    }

    fun deleteGroup(group: ExerciseGroupEntity) {
        viewModelScope.launch {
            dao.deleteGroup(group)
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.updateExerciseDefinition(exercise.toEntity())
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.deleteExerciseDefinition(exercise.toEntity())
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in exercises.indices || toIndex !in exercises.indices) return
        val list = exercises.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        
        viewModelScope.launch {
            list.forEachIndexed { index, exercise ->
                dao.updateExerciseDefinition(exercise.toEntity().copy(orderIndex = index))
            }
        }
    }

    fun startTotalTimer() {
        if (totalTimerJob?.isActive == true) return
        isTotalRunning.value = true
        totalTimerJob = viewModelScope.launch {
            while (isTotalRunning.value) {
                delay(1000L)
                totalTime.intValue++
            }
        }
    }

    fun pauseTotalTimer() {
        isTotalRunning.value = false
        totalTimerJob?.cancel()
        
        isExerciseRunning.value = false
        exerciseTimerJob?.cancel()
    }

    fun resetTotalTimer() {
        pauseTotalTimer()
        totalTime.intValue = 0
    }

    fun toggleExerciseTimer() {
        if (isExerciseRunning.value) {
            isExerciseRunning.value = false
            exerciseTimerJob?.cancel()
        } else {
            startExerciseTimer()
        }
    }

    private fun startExerciseTimer() {
        if (exerciseTimerJob?.isActive == true) return
        isExerciseRunning.value = true
        exerciseTimerJob = viewModelScope.launch {
            while (isExerciseRunning.value) {
                delay(1000L)
                exerciseTime.intValue++
            }
        }
    }

    fun resetExerciseTimer() {
        isExerciseRunning.value = false
        exerciseTimerJob?.cancel()
        exerciseTime.intValue = 0
        startExerciseTimer()
    }

    fun startRestTimer(seconds: Int, index: Int) {
        restTimerJob?.cancel()
        currentRestTime.intValue = seconds
        restExerciseIndex.intValue = index
        isRestRunning.value = true
        restTimerJob = viewModelScope.launch {
            while (isRestRunning.value && currentRestTime.intValue > 0) {
                delay(1000L)
                currentRestTime.intValue--
            }
            if (currentRestTime.intValue == 0) {
                isRestRunning.value = false
            }
        }
    }

    fun stopRestTimer() {
        isRestRunning.value = false
        restTimerJob?.cancel()
    }

    fun resetRestTimer() {
        stopRestTimer()
        currentRestTime.intValue = 60
    }

    fun saveWorkout(totalTime: Int, exerciseData: List<Pair<String, Int>>) {
        viewModelScope.launch {
            dao.saveWorkout(totalTime, exerciseData)
        }
    }

    fun moveToNextExercise() {
        if (exercises.isEmpty()) return
        
        val size = exercises.size
        val current = currentExerciseIndex.intValue
        
        for (i in 1..size) {
            val checkIndex = (current + i) % size
            val setsDone = if (checkIndex < currentSet.size) currentSet[checkIndex] else 0
            if (setsDone < exercises[checkIndex].totalSets) {
                currentExerciseIndex.intValue = checkIndex
                return
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearAllHistory()
        }
    }

    private fun ExerciseEntity.toDomain() = Exercise(
        id = id,
        name = name,
        description = description,
        setsReps = setsReps,
        totalSets = totalSets,
        restSeconds = restSeconds,
        orderIndex = orderIndex,
        dayOfWeek = dayOfWeek,
        groupId = groupId
    )

    private fun Exercise.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        description = description,
        setsReps = setsReps,
        totalSets = totalSets,
        restSeconds = restSeconds,
        orderIndex = orderIndex,
        dayOfWeek = dayOfWeek,
        groupId = groupId
    )

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutViewModel(context) as T
        }
    }
}
