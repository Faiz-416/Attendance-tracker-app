package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.navigation.Screen
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.importflow.ChatGPTImportScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.subjects.SubjectManagementScreen
import com.example.ui.screens.timetable.TimetableScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel

@Composable
fun MainScreen(viewModel: AttendanceViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val academicSettings by viewModel.academicSettings.collectAsStateWithLifecycle()
    val startDestination = if (academicSettings.isSetupCompleted) Screen.Home.route else Screen.Onboarding.route

    val bottomBarScreens = listOf(
        Screen.Home,
        Screen.Today,
        Screen.Calendar,
        Screen.Analytics,
        Screen.Settings
    )

    val showBottomBar = currentDestination?.route in bottomBarScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextWhite,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav_bar")
                ) {
                    bottomBarScreens.forEach { screen ->
                        val selected = currentDestination?.route == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentDestination?.route != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CharcoalBlack,
                                selectedTextColor = BrightMint,
                                indicatorColor = EmeraldPrimary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        },
        containerColor = CharcoalBlack
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    viewModel = viewModel,
                    onFinishOnboarding = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onImportWithChatGPT = {
                        viewModel.startImportWorkflow()
                        navController.navigate(Screen.ChatGPTImport.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToToday = { navController.navigate(Screen.Today.route) },
                    onNavigateToImport = {
                        viewModel.startImportWorkflow()
                        navController.navigate(Screen.ChatGPTImport.route)
                    },
                    onNavigateToSubjects = { navController.navigate(Screen.SubjectManager.route) },
                    onNavigateToTimetable = { navController.navigate(Screen.TimetableManager.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                )
            }

            composable(Screen.Today.route) {
                TodayScreen(
                    viewModel = viewModel,
                    onNavigateToImport = {
                        viewModel.startImportWorkflow()
                        navController.navigate(Screen.ChatGPTImport.route)
                    },
                    onNavigateToSubjects = { navController.navigate(Screen.SubjectManager.route) }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = viewModel)
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToSubjects = { navController.navigate(Screen.SubjectManager.route) },
                    onNavigateToTimetable = { navController.navigate(Screen.TimetableManager.route) },
                    onNavigateToImport = {
                        viewModel.startImportWorkflow()
                        navController.navigate(Screen.ChatGPTImport.route)
                    }
                )
            }

            composable(Screen.ChatGPTImport.route) {
                ChatGPTImportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onImportSuccess = {
                        navController.navigate(Screen.TimetableManager.route) {
                            popUpTo(Screen.ChatGPTImport.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.TimetableManager.route) {
                TimetableScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToImport = {
                        viewModel.startImportWorkflow()
                        navController.navigate(Screen.ChatGPTImport.route)
                    },
                    onNavigateToSubjects = { navController.navigate(Screen.SubjectManager.route) }
                )
            }

            composable(Screen.SubjectManager.route) {
                SubjectManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
