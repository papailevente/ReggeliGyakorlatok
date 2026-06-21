package com.example.reggelirutin

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

import kotlin.time.Duration.Companion.seconds

sealed class Screen(val route: String, val icon: ImageVector, val labelKey: String) {
    object Workout : Screen("workout", Icons.Default.PlayArrow, "workout_tab")
    object History : Screen("history", Icons.Default.History, "history_tab")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            ReggeliRutinTheme {
                ReggeliRutinApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReggeliRutinApp() {
    val context = LocalContext.current
    val currentVersionCode = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try { context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode } catch (_: Exception) { 0L }
        } else {
            @Suppress("DEPRECATION")
            try { context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong() } catch (_: Exception) { 0L }
        }
    }
    
    val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory(context))
    val navController = rememberNavController()
    val updateManager = remember { UpdateManager(context) }
    
    var currentLanguage by remember { mutableStateOf("Hungarian") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFullScreenSplash by remember { mutableStateOf(true) }
    var showMigrationDialog by remember { mutableStateOf(false) }
    var showShowcasePrompt by remember { mutableStateOf(false) }
    
    val updateResultState = remember { mutableStateOf<UpdateResult?>(null) }
    val strings = rememberStrings(currentLanguage)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val lastVersion = updateManager.lastAppVersion.first()
        if (lastVersion != 0L && lastVersion < currentVersionCode) {
            showMigrationDialog = true
        }
        updateManager.updateLastAppVersion(currentVersionCode)

        delay(2.seconds)
        showFullScreenSplash = false

        // Check for Showcase trigger
        val lastShowcase = updateManager.lastShowcaseVersion.first()
        if (lastShowcase < currentVersionCode) {
            showShowcasePrompt = true
        }
        
        // Auto check for update
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.3.4"
        } catch (_: Exception) { "1.0.0" }
        
        val result = updateManager.checkForUpdate(versionName)
        if (result is UpdateResult.NewVersionAvailable) {
            updateResultState.value = result
        }
    }

    val currentUpdateResult = updateResultState.value
    if (currentUpdateResult is UpdateResult.NewVersionAvailable) {
        AlertDialog(
            onDismissRequest = { updateResultState.value = null },
            title = { Text(strings["update_available"] ?: "New version available") },
            text = { 
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) { "1.3.4" }
                Text("${strings["new_version"] ?: "New version"}: v${currentUpdateResult.version}\n${strings["current_version"] ?: "Current version"}: v$currentVersion") 
            },
            confirmButton = {
                Button(onClick = { 
                    updateManager.downloadAndInstall(currentUpdateResult.downloadUrl)
                    updateResultState.value = null
                }) {
                    Text(strings["download_now"] ?: "Download now")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateResultState.value = null }) {
                    Text(strings["later"] ?: "Later")
                }
            }
        )
    }

    if (showShowcasePrompt) {
        AlertDialog(
            onDismissRequest = { 
                showShowcasePrompt = false 
                scope.launch { 
                    updateManager.updateLastShowcaseVersion(currentVersionCode) 
                }
            },
            title = { Text(strings["showcase_title"] ?: "Quick Tour") },
            text = { Text(strings["showcase_ask"] ?: "Would you like a quick tour of the new features?") },
            confirmButton = {
                Button(onClick = { 
                    showShowcasePrompt = false
                    viewModel.showcaseStep.intValue = 0
                    scope.launch { 
                        updateManager.updateLastShowcaseVersion(currentVersionCode) 
                    }
                }) {
                    Text(strings["start_tour"] ?: "Start Tour")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showShowcasePrompt = false 
                    scope.launch { 
                        updateManager.updateLastShowcaseVersion(currentVersionCode) 
                    }
                }) {
                    Text(strings["later"] ?: "Later")
                }
            }
        )
    }

    if (showMigrationDialog) {
        AlertDialog(
            onDismissRequest = { showMigrationDialog = false },
            title = { Text(strings["update_title"] ?: "Application Updated") },
            text = { Text(strings["migration_message"] ?: "Would you like to clear your old workout history for a fresh start in this version?") },
            confirmButton = {
                Button(onClick = { 
                    viewModel.clearHistory()
                    showMigrationDialog = false 
                }) {
                    Text(strings["clear_history"] ?: "Clear History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMigrationDialog = false }) {
                    Text(strings["keep_history"] ?: "Keep History")
                }
            }
        )
    }

    if (showFullScreenSplash) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.splash_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        if (showAboutDialog) {
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) { "1.3.4" }

            AlertDialog(
                onDismissRequest = { if (showAboutDialog) showAboutDialog = false },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings["about_title"] ?: "About")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "v$versionName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                },
                text = { Text("made by Zsenike with Grok and Gemini.") },
                confirmButton = {
                    TextButton(onClick = { if (showAboutDialog) showAboutDialog = false }) { Text("OK") }
                }
            )
        }

        val workoutDone by viewModel.workoutDone

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!workoutDone) {
                        NavigationBar(
                            containerColor = Color.Black.copy(alpha = 0.4f),
                            tonalElevation = 0.dp
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            
                            val items = listOf(Screen.Workout, Screen.History)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    label = { Text(strings[screen.labelKey] ?: screen.labelKey, fontSize = 9.sp) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    colors = NavigationBarItemDefaults.colors(
                                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        indicatorColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    onClick = {
                                        if (screen == Screen.Workout) {
                                            viewModel.startTotalTimer()
                                        }
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController, 
                    startDestination = Screen.Workout.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Workout.route) {
                        WorkoutScreen(
                            strings = strings,
                            viewModel = viewModel,
                            onMenuClick = { menuExpanded = true },
                            menuExpanded = menuExpanded,
                            onMenuDismiss = { if (menuExpanded) menuExpanded = false },
                            onLanguageChange = { if (currentLanguage != it) currentLanguage = it },
                            onAboutClick = { showAboutDialog = true },
                            onCheckUpdate = {
                                scope.launch {
                                    val versionName = try {
                                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.3.4"
                                    } catch (_: Exception) { "1.3.4" }
                                    
                                    when (val result = updateManager.checkForUpdate(versionName, force = true)) {
                                        is UpdateResult.NewVersionAvailable -> {
                                            updateResultState.value = result
                                        }
                                        is UpdateResult.NoUpdate -> {
                                            snackbarHostState.showSnackbar(strings["no_update"] ?: "Already on latest version")
                                        }
                                        else -> {
                                            snackbarHostState.showSnackbar(strings["update_error"] ?: "Error checking for updates")
                                        }
                                    }
                                }
                            },
                            onFinishWorkout = { totalTime, data ->
                                viewModel.saveWorkout(totalTime, data)
                            },
                            onNavigateToHistory = {
                                navController.navigate(Screen.History.route)
                            }
                        )
                    }
                    composable(Screen.History.route) {
                        HistoryScreen(
                            strings = strings,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
