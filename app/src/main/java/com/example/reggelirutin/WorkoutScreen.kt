package com.example.reggelirutin

import android.app.Activity
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val currentMode by viewModel.currentMode
    val selectedGroupId by viewModel.selectedGroupId
    
    val totalTime by viewModel.totalTime
    val exerciseTime by viewModel.exerciseTime
    val isTotalRunning by viewModel.isTotalRunning
    val isExerciseRunning by viewModel.isExerciseRunning
    val currentRestTime by viewModel.currentRestTime
    val isRestRunning by viewModel.isRestRunning
    var workoutDone by viewModel.workoutDone
    
    val exercises = viewModel.exercises
    val currentSet = viewModel.currentSet
    val currentExerciseIndex by viewModel.currentExerciseIndex
    val scope = rememberCoroutineScope()

    val totalSetsRequired = remember(exercises.size, exercises.sumOf { it.totalSets }) { 
        exercises.sumOf { it.totalSets } 
    }

    val listState = rememberLazyListState()

    val allSetsDone = remember(currentSet.toList(), totalSetsRequired) {
        currentSet.sum() == totalSetsRequired && totalSetsRequired > 0
    }

    val infiniteTransition = rememberInfiniteTransition(label = "FinishButtonFlash")
    val flashingButtonColor by infiniteTransition.animateColor(
        initialValue = Color.Blue.copy(alpha = 0.6f),
        targetValue = Color.Red.copy(alpha = 0.6f),
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FinishButtonColor"
    )

    LaunchedEffect(currentExerciseIndex) {
        if (exercises.isNotEmpty() && !workoutDone) {
            listState.animateScrollToItem(currentExerciseIndex)
        }
    }

    var showExerciseManager by remember { mutableStateOf(false) }
    var showGroupManager by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var groupToEdit by remember { mutableStateOf<ExerciseGroupEntity?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }

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
            onAdd = { exerciseToEdit = Exercise(name = "", description = "", setsReps = "", totalSets = 3, groupId = viewModel.selectedGroupId.value ?: 0) },
            onEdit = { exerciseToEdit = it },
            onDelete = { viewModel.deleteExercise(it) }
        )
    }

    if (showGroupManager) {
        GroupManagerDialog(
            strings = strings,
            groups = viewModel.exerciseGroups,
            viewModel = viewModel,
            onDismiss = { showGroupManager = false },
            onAdd = { groupToEdit = ExerciseGroupEntity(name = "") },
            onEdit = { groupToEdit = it },
            onDelete = { viewModel.deleteGroup(it) }
        )
    }

    exerciseToEdit?.let { exercise ->
        ExerciseEditDialog(
            strings = strings,
            exercise = exercise,
            groups = viewModel.allGroups,
            onDismiss = { exerciseToEdit = null },
            onSave = { 
                if (it.id == 0L) viewModel.addExercise(it.name, it.description, it.setsReps, it.totalSets)
                else viewModel.updateExercise(it)
                exerciseToEdit = null
            }
        )
    }

    groupToEdit?.let { group ->
        GroupEditDialog(
            strings = strings,
            group = group,
            onDismiss = { groupToEdit = null },
            onSave = {
                if (it.id == 0L) viewModel.addGroup(it.name)
                else viewModel.updateGroup(it)
                groupToEdit = null
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
                        .statusBarsPadding()
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
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(strings["menu_languages"] ?: "Languages")
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

                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(strings["select_mode"] ?: "Change Mode")
                                    }
                                },
                                onClick = { 
                                    viewModel.currentMode.value = AppMode.None
                                    viewModel.selectedGroupId.value = null
                                    onMenuDismiss() 
                                }
                            )

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
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(strings["groups"] ?: "Groups")
                                    }
                                },
                                onClick = { showGroupManager = true; onMenuDismiss() }
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
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SunBackground(progress)

            if (workoutDone) {
                CompletionScreen(totalTime, context)
            } else {
                when {
                    currentMode == AppMode.None -> {
                        ModeSelectionScreen(strings) { viewModel.currentMode.value = it }
                    }
                    currentMode == AppMode.Extra && selectedGroupId == null -> {
                        GroupSelectionScreen(strings, viewModel.exerciseGroups) { viewModel.selectedGroupId.value = it }
                    }
                    else -> {
                        WorkoutContent(
                            strings = strings,
                            viewModel = viewModel,
                            exercises = exercises,
                            currentSet = currentSet,
                            currentExerciseIndex = currentExerciseIndex,
                            totalTime = totalTime,
                            exerciseTime = exerciseTime,
                            currentRestTime = currentRestTime,
                            isTotalRunning = isTotalRunning,
                            isExerciseRunning = isExerciseRunning,
                            isRestRunning = isRestRunning,
                            listState = listState,
                            allSetsDone = allSetsDone,
                            flashingButtonColor = flashingButtonColor,
                            totalSetsRequired = totalSetsRequired,
                            onFinishWorkout = onFinishWorkout
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectionScreen(strings: Map<String, String>, onModeSelected: (AppMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings["select_mode"] ?: "Choose Mode",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { onModeSelected(AppMode.Normal) },
                modifier = Modifier.size(140.dp, 80.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings["mode_normal"] ?: "Normal", fontSize = 18.sp)
            }
            
            Button(
                onClick = { onModeSelected(AppMode.Extra) },
                modifier = Modifier.size(140.dp, 80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(strings["mode_extra"] ?: "Extra", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun GroupSelectionScreen(
    strings: Map<String, String>, 
    groups: List<ExerciseGroupEntity>, 
    onGroupSelected: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings["groups"] ?: "Groups",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(groups) { _, group ->
                Card(
                    onClick = { onGroupSelected(group.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = group.name, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutContent(
    strings: Map<String, String>,
    viewModel: WorkoutViewModel,
    exercises: List<Exercise>,
    currentSet: List<Int>,
    currentExerciseIndex: Int,
    totalTime: Int,
    exerciseTime: Int,
    currentRestTime: Int,
    isTotalRunning: Boolean,
    isExerciseRunning: Boolean,
    isRestRunning: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    allSetsDone: Boolean,
    flashingButtonColor: Color,
    totalSetsRequired: Int,
    onFinishWorkout: (Int, List<Pair<String, Int>>) -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
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
                    if (currentRestTime <= 0) viewModel.startRestTimer(60, -1)
                    else {
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

        ScrollableTabRow(
            selectedTabIndex = viewModel.selectedDay.intValue - 1,
            containerColor = Color.Transparent,
            contentColor = Color.Yellow,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedDay.intValue - 1]),
                    color = Color.Yellow
                )
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            (1..7).forEach { day ->
                Tab(
                    selected = viewModel.selectedDay.intValue == day,
                    onClick = { viewModel.selectedDay.intValue = day },
                    text = { 
                        Text(
                            text = strings["day_$day"] ?: day.toString(),
                            fontSize = 14.sp,
                            fontWeight = if (viewModel.selectedDay.intValue == day) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        Text(
            text = strings["exercises"]!!, 
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 40.dp)
        ) {
            itemsIndexed(exercises) { index, exercise ->
                ExerciseItem(
                    exercise = exercise,
                    currentSet = if (index < currentSet.size) currentSet[index] else 0,
                    isDone = index < currentSet.size && currentSet[index] >= exercise.totalSets,
                    isCurrent = index == currentExerciseIndex,
                    scale = 1.0f,
                    onSetIncrement = {
                        if (index < currentSet.size && currentSet[index] < exercise.totalSets) {
                            viewModel.currentSet[index]++
                            if (viewModel.currentSet.sum() == totalSetsRequired) {
                                viewModel.pauseTotalTimer()
                                viewModel.resetExerciseTimer()
                                viewModel.stopRestTimer()
                            } else {
                                viewModel.moveToNextExercise()
                                viewModel.startRestTimer(exercise.restSeconds, index)
                            }
                        }
                    },
                    onSetDecrement = {
                        if (index < currentSet.size && currentSet[index] > 0) viewModel.currentSet[index]--
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
                val data = exercises.mapIndexed { idx, ex -> ex.name to currentSet[idx] }
                scope.launch {
                    viewModel.workoutDone.value = true
                    delay(3000)
                    onFinishWorkout(totalTime, data)
                }
            },
            containerColor = if (allSetsDone) flashingButtonColor else null,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(text = strings["finish"]!!, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun CompletionScreen(totalTime: Int, context: android.content.Context) {
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
        Text(text = "🏆", fontSize = 120.sp)
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
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = null) }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(exercises) { index, exercise ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            IconButton(onClick = { viewModel.moveExercise(index, index - 1) }, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = { viewModel.moveExercise(index, index + 1) }, enabled = index < exercises.size - 1, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Text(text = exercise.name, modifier = Modifier.weight(1f).padding(start = 8.dp), fontSize = 18.sp)
                        IconButton(onClick = { onEdit(exercise) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDelete(exercise) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun GroupManagerDialog(
    strings: Map<String, String>,
    groups: List<ExerciseGroupEntity>,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ExerciseGroupEntity) -> Unit,
    onDelete: (ExerciseGroupEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings["groups"] ?: "Groups")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = null) }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(groups) { _, group ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = group.name, modifier = Modifier.weight(1f), fontSize = 18.sp)
                        IconButton(onClick = { onEdit(group) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDelete(group) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun GroupEditDialog(
    strings: Map<String, String>,
    group: ExerciseGroupEntity,
    onDismiss: () -> Unit,
    onSave: (ExerciseGroupEntity) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var selectedDay by remember { mutableStateOf(group.dayOfWeek) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["edit_group"] ?: "Edit Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings["group_name"] ?: "Group Name") })
                Spacer(Modifier.height(8.dp))
                Text(text = strings["select_day"] ?: "Active on:")
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    DayChip(label = "∞", isSelected = selectedDay == 0, onClick = { selectedDay = 0 })
                    (1..7).forEach { day ->
                        DayChip(label = strings["day_$day"] ?: day.toString(), isSelected = selectedDay == day, onClick = { selectedDay = day })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(group.copy(name = name, dayOfWeek = selectedDay)) }) {
                Text(strings["save"] ?: "Save")
            }
        }
    )
}

@Composable
fun DayChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) BorderStroke(1.dp, Color.Yellow.copy(alpha = 0.5f)) else null,
        modifier = Modifier.padding(2.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(text = label, fontSize = 12.sp, color = if (isSelected) Color.Yellow else Color.White)
        }
    }
}

@Composable
fun ExerciseEditDialog(
    strings: Map<String, String>,
    exercise: Exercise,
    groups: List<ExerciseGroupEntity>,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(exercise.name) }
    var description by remember { mutableStateOf(exercise.description) }
    var reps by remember { mutableStateOf(exercise.setsReps) }
    var sets by remember { mutableStateOf(exercise.totalSets.toString()) }
    var selectedDay by remember { mutableStateOf(exercise.dayOfWeek) }
    var selectedGroupId by remember { mutableStateOf(exercise.groupId) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["edit_exercise"] ?: "Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings["name"] ?: "Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(strings["description"] ?: "Description") })
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text(strings["reps"] ?: "Reps") })
                OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text(strings["sets_count"] ?: "Sets") })
                
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        val groupName = groups.find { it.id == selectedGroupId }?.name ?: strings["all_groups"] ?: "All Groups"
                        Text(groupName)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text(strings["all_groups"] ?: "All Groups") }, onClick = { selectedGroupId = 0; expanded = false })
                        groups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = { selectedGroupId = group.id; expanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(text = strings["select_day"] ?: "Active on:")
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    DayChip(label = "∞", isSelected = selectedDay == 0, onClick = { selectedDay = 0 })
                    (1..7).forEach { day ->
                        DayChip(label = strings["day_$day"] ?: day.toString(), isSelected = selectedDay == day, onClick = { selectedDay = day })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(exercise.copy(name = name, description = description, setsReps = reps, totalSets = sets.toIntOrNull() ?: 1, dayOfWeek = selectedDay, groupId = selectedGroupId)) 
            }) { Text(strings["save"] ?: "Save") }
        }
    )
}
