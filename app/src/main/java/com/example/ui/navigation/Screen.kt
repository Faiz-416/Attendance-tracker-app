package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Today : Screen(
        route = "today",
        title = "Today",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    )

    object Calendar : Screen(
        route = "calendar",
        title = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    object Analytics : Screen(
        route = "analytics",
        title = "Analytics",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    // Sub-screens
    object ChatGPTImport : Screen(
        route = "chatgpt_import",
        title = "Import Timetable",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    )

    object TimetableManager : Screen(
        route = "timetable_manager",
        title = "Weekly Timetable",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )

    object SubjectManager : Screen(
        route = "subject_manager",
        title = "Subjects",
        selectedIcon = Icons.Filled.Book,
        unselectedIcon = Icons.Outlined.Book
    )

    object Onboarding : Screen(
        route = "onboarding",
        title = "Get Started",
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School
    )

    companion object {
        val bottomNavItems = listOf(Home, Today, Calendar, Analytics, Settings)
    }
}
