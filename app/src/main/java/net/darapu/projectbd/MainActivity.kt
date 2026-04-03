package net.darapu.projectbd

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.darapu.projectbd.data.local.AppDatabase
import net.darapu.projectbd.data.repository.SettingsRepository
import net.darapu.projectbd.ui.screens.config.ConfigScreen
import net.darapu.projectbd.ui.screens.diet.DietScreen
import net.darapu.projectbd.ui.screens.diet.DietViewModel
import net.darapu.projectbd.ui.screens.history.HistoryContent
import net.darapu.projectbd.ui.screens.home.HomeScreen
import net.darapu.projectbd.ui.screens.home.HomeViewModel
import net.darapu.projectbd.ui.screens.onboarding.OnboardingScreen
import net.darapu.projectbd.ui.screens.workout.WorkoutScreen
import net.darapu.projectbd.ui.screens.workout.WorkoutViewModel
import net.darapu.projectbd.ui.theme.ProjectBDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsRepository = remember { SettingsRepository(context) }
            val sharedPrefs = remember { context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE) }
            
            var themePref by remember { mutableStateOf(settingsRepository.getTheme()) }
            DisposableEffect(sharedPrefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "app_theme") {
                        themePref = prefs.getString("app_theme", "System Default") ?: "System Default"
                    }
                }
                sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val isDarkTheme = when (themePref) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            ProjectBDTheme(darkTheme = isDarkTheme) {
                var onboardingComplete by remember { mutableStateOf(settingsRepository.isOnboardingComplete()) }

                if (!onboardingComplete) {
                    OnboardingScreen(onComplete = { onboardingComplete = true })
                } else {
                    MainScreen()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Diet : Screen("diet", "Diet", Icons.Filled.Favorite)
    object Workout : Screen("workout", "Workout", Icons.Filled.Star)
    object Config : Screen("config", "Config", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        Screen.Home,
        Screen.Diet,
        Screen.Workout,
        Screen.Config
    )
    
    var showHistoryView by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (showHistoryView) "Activity History" else "ProjectBD") },
                navigationIcon = {
                    if (showHistoryView) {
                        IconButton(onClick = { showHistoryView = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!showHistoryView) {
                        IconButton(onClick = { showHistoryView = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "History View")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!showHistoryView) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
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
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (showHistoryView) {
                HistoryContent()
            } else {
                NavHost(navController = navController, startDestination = Screen.Home.route, modifier = Modifier.fillMaxSize()) {
                    composable(Screen.Home.route) {
                        val viewModel = remember { HomeViewModel(AppDatabase.getDatabase(context), SettingsRepository(context)) }
                        HomeScreen(viewModel = viewModel)
                    }
                    composable(Screen.Diet.route) {
                        val viewModel = remember { DietViewModel(AppDatabase.getDatabase(context), SettingsRepository(context)) }
                        DietScreen(viewModel = viewModel)
                    }
                    composable(Screen.Workout.route) {
                        val viewModel = remember { WorkoutViewModel(AppDatabase.getDatabase(context), SettingsRepository(context)) }
                        WorkoutScreen(viewModel = viewModel)
                    }
                    composable(Screen.Config.route) {
                        ConfigScreen()
                    }
                }
            }
        }
    }
}