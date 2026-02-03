package com.example.bloodpressuretracking.ui.navigation

/**
 * Sealed class representing navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Main : Screen("main")
    data object RecordList : Screen("record_list")
}
