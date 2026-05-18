package com.example.reggelirutin

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    strings: Map<String, String>,
    viewModel: WorkoutViewModel,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onAboutClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    onFinishWorkout: (Int, List<Pair<String, Int>>) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    
    val totalTime by viewModel.totalTime
    val exerciseTime by viewModel.exerciseTime
    val isTotalRunning by viewModel.isTotalRunning
    val isExerciseRunning by viewModel.isExerciseRunning
    val currentRestTime by viewModel.currentRestTime
    val isRestRunning by viewModel.isRestRunning
    var workoutDone by viewModel.workoutDone
    
    val exercises = viewModel.exercises
    val currentSet = viewModel.currentSet
    val scope = rememberCoroutineScope()

    var showExerciseManager by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val totalSetsRequired = remember(exercises.size, exercises.sumOf { it.totalSets }) { 
        exercises.sumOf { it.totalSets } 
    }
    val setsDone = currentSet.sum()
    val progress by animateFloatAsState(
        targetValue = if (totalSetsRequired > 0) setsDone.toFloat() / totalSetsRequired.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "SunProgress"
    )

    LaunchedEffect(currentRestTime) {
        if (currentRestTime == 0 && isRestRunning) {
            viewModel.stopRestTimer()
            playNotification(context)
        }
    }

    if (showExerciseManager) {
        ExerciseManagerDialog(
            strings = strings,
            exercises = exercises,
            viewModel = viewModel,
            onDismiss = { showExerciseManager = false },
            onAdd = { exerciseToEdit = Exercise(name = "", description = "", setsReps = "", totalSets = 3) },
            onEdit = { exerciseToEdit = it },
            onDelete = { viewModel.deleteExercise(it) }
        )
    }

    exerciseToEdit?.let { exercise ->
        ExerciseEditDialog(
            strings = strings,
            exercise = exercise,
            onDismiss = { exerciseToEdit = null },
            onSave = { 
                if (it.id == 0L) viewModel.addExercise(it.name, it.description, it.setsReps, it.totalSets)
                else viewModel.updateExercise(it)
                exerciseToEdit = null
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (!workoutDone) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(32.dp)
                        ) {
                            Text("☰", fontSize = 16.sp, color = Color.White)
                        }
                        
                        Text(
                            text = "🏋️ ${strings["title"]} 🏋️",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = onMenuDismiss
                        ) {
                            // Nyelvek menüpont (Csoportosítva)
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(strings["menu_languages"] ?: "Languages")
                                        Spacer(Modifier.weight(1f))
                                        Text(when(strings["title"]) {
                                            "Morning Routine" -> "🇺🇸"
                                            "Morgenroutine" -> "🇩🇪"
                                            else -> "🇭🇺"
                                        })
                                    }
                                },
                                onClick = { showLanguageMenu = !showLanguageMenu }
                            )

                            if (showLanguageMenu) {
                                DropdownMenuItem(
                                    text = { Text("   🇭🇺 Hungarian") }, 
                                    onClick = { onLanguageChange("Hungarian"); showLanguageMenu = false; onMenuDismiss() }
                                )
                                DropdownMenuItem(
                                    text = { Text("   🇩🇪 Deutsch") }, 
                                    onClick = { onLanguageChange("Deutsch"); showLanguageMenu = false; onMenuDismiss() }
                                )
                                DropdownMenuItem(
                                    text = { Text("   🇺🇸 English") }, 
                                    onClick = { onLanguageChange("English"); showLanguageMenu = false; onMenuDismiss() }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }

                            // Gyakorlatok menüpont
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(strings["menu_exercises"] ?: "Exercises")
                                    }
                                },
                                onClick = { showExerciseManager = true; onMenuDismiss() }
                            )

                            DropdownMenuItem(
                                text = { Text(strings["history_tab"] ?: "History") },
                                onClick = { onNavigateToHistory(); onMenuDismiss() }
                            )
                            DropdownMenuItem(
                                text = { Text(strings["check_updates"] ?: "Check for Updates") },
                                onClick = { onCheckUpdate(); onMenuDismiss() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(strings["about_title"] ?: "About") },
                                onClick = { onAboutClick(); onMenuDismiss() }
                            )
                            // Verziószám megjelenítése
                            val versionName = remember(context) {
                                try {
                                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                                } catch (_: Exception) {
                                    "1.0.6"
                                }
                            }
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = "v$versionName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4FC3F7), // Világos kék szín
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    ) 
                                },
                                onClick = { },
                                enabled = false
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SunBackground(progress)

            if (workoutDone) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatTime(totalTime),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "🏆",
                        fontSize = 120.sp
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 48.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    FilledIconButton(
                        onClick = { (context as? Activity)?.finish() },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("✕", fontSize = 32.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    TimerCard(
                        title = strings["total_time"]!!,
                        time = totalTime,
                        isRunning = isTotalRunning,
                        onStart = { viewModel.startTotalTimer() },
                        onPause = { viewModel.pauseTotalTimer() },
                        onReset = { viewModel.resetTotalTimer() },
                        showReset = true,
                        backgroundColor = Color(0xFF8DB600).copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                        strings = strings
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimerCard(
                            title = strings["exercise_time"]!!,
                            time = exerciseTime,
                            isRunning = isExerciseRunning,
                            showReset = true,
                            onStartPause = { viewModel.toggleExerciseTimer() },
                            onReset = { viewModel.resetExerciseTimer() },
                            modifier = Modifier.weight(1f),
                            strings = strings,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )

                        TimerCard(
                            title = strings["rest"]!!,
                            time = currentRestTime,
                            isRunning = isRestRunning,
                            onStartPause = {
                                if (currentRestTime <= 0) {
                                    viewModel.startRestTimer(60, -1)
                                } else {
                                    if (isRestRunning) viewModel.stopRestTimer() else viewModel.startRestTimer(currentRestTime, -1)
                                }
                            },
                            onReset = { viewModel.resetRestTimer() },
                            modifier = Modifier.weight(1f),
                            strings = strings,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = strings["exercises"]!!, 
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(exercises) { index, exercise ->
                            ExerciseItem(
                                exercise = exercise,
                                currentSet = if (index < currentSet.size) currentSet[index] else 0,
                                isDone = index < currentSet.size && currentSet[index] >= exercise.totalSets,
                                onSetIncrement = {
                                    if (index < currentSet.size && currentSet[index] < exercise.totalSets) {
                                        currentSet[index]++
                                        
                                        if (currentSet.sum() == totalSetsRequired) {
                                            viewModel.pauseTotalTimer()
                                            viewModel.resetExerciseTimer()
                                            viewModel.stopRestTimer()
                                            
                                            val data = exercises.mapIndexed { idx, ex -> 
                                                ex.name to currentSet[idx]
                                            }
                                            
                                            scope.launch {
                                                delay(1000)
                                                workoutDone = true
                                                delay(3000)
                                                onFinishWorkout(totalTime, data)
                                            }
                                        } else {
                                            viewModel.startRestTimer(exercise.restSeconds, index)
                                        }
                                    }
                                },
                                onSetDecrement = {
                                    if (index < currentSet.size && currentSet[index] > 0) currentSet[index]--
                                },
                                strings = strings
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    RutinButton(
                        onClick = { 
                            viewModel.pauseTotalTimer()
                            viewModel.resetExerciseTimer()
                            viewModel.stopRestTimer()
                            
                            val data = exercises.mapIndexed { idx, ex -> 
                                ex.name to currentSet[idx]
                            }
                            
                            scope.launch {
                                workoutDone = true
                                delay(3000)
                                onFinishWorkout(totalTime, data)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = strings["finish"]!!,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseManagerDialog(
    strings: Map<String, String>,
    exercises: List<Exercise>,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Exercise) -> Unit,
    onDelete: (Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings["menu_exercises"] ?: "Exercises")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(exercises) { index, exercise ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reorder buttons (Side-by-side and 50% larger icons)
                        Row {
                            IconButton(
                                onClick = { viewModel.moveExercise(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                            IconButton(
                                onClick = { viewModel.moveExercise(index, index + 1) },
                                enabled = index < exercises.size - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        
                        Text(
                            text = exercise.name, 
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp // 30% larger characters
                        )
                        
                        IconButton(onClick = { onEdit(exercise) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDelete(exercise) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (index < exercises.size - 1) {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
fun ExerciseEditDialog(
    strings: Map<String, String>,
    exercise: Exercise,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(exercise.name) }
    var description by remember { mutableStateOf(exercise.description) }
    var reps by remember { mutableStateOf(exercise.setsReps) }
    var sets by remember { mutableStateOf(exercise.totalSets.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["edit_exercise"] ?: "Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings["name"] ?: "Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(strings["description"] ?: "Description") })
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text(strings["reps"] ?: "Reps") })
                OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text(strings["sets_count"] ?: "Sets") })
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(exercise.copy(
                    name = name, 
                    description = description, 
                    setsReps = reps, 
                    totalSets = sets.toIntOrNull() ?: 1
                )) 
            }) {
                Text(strings["save"] ?: "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
