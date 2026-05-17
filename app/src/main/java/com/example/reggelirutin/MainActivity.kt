package com.example.reggelirutin

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val icon: ImageVector, val labelKey: String) {
    object Workout : Screen("workout", Icons.Default.PlayArrow, "workout_tab")
    object History : Screen("history", Icons.Default.History, "history_tab")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
    val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory(context))
    val navController = rememberNavController()
    
    var currentLanguage by remember { mutableStateOf("Hungarian") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val strings = rememberStrings(currentLanguage)

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings["about_title"] ?: "About") },
            text = { Text("made by Zsenike with Grok and Gemini.") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
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
            bottomBar = {
                if (!workoutDone) {
                    NavigationBar(
                        modifier = Modifier.height(48.dp),
                        containerColor = Color.Black.copy(alpha = 0.4f),
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)
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
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (workoutDone) 0.dp else 48.dp
                )
            ) {
                composable(Screen.Workout.route) {
                    WorkoutScreen(
                        strings = strings,
                        viewModel = viewModel,
                        onMenuClick = { menuExpanded = true },
                        menuExpanded = menuExpanded,
                        onMenuDismiss = { menuExpanded = false },
                        onLanguageChange = { currentLanguage = it },
                        onAboutClick = { showAboutDialog = true },
                        onFinishWorkout = { _, _ ->
                            // ViewModel already handles saving
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
