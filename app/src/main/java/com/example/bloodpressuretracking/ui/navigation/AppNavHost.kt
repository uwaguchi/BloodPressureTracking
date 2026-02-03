package com.example.bloodpressuretracking.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.bloodpressuretracking.ui.login.LoginScreen
import com.example.bloodpressuretracking.ui.main.MainScreen
import com.example.bloodpressuretracking.ui.recordlist.RecordListScreen

/**
 * Navigation host for the app.
 * Handles navigation between Login, Main, and RecordList screens.
 *
 * @param navController Navigation controller
 * @param startDestination Starting screen route
 * @param onLoginSuccess Callback when login is successful
 * @param modifier Modifier for the NavHost
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Login screen
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    onLoginSuccess()
                    navController.navigate(Screen.Main.route) {
                        // Clear login from back stack
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Main screen (blood pressure input)
        composable(route = Screen.Main.route) {
            MainScreen(
                onNavigateToRecordList = {
                    navController.navigate(Screen.RecordList.route)
                }
            )
        }

        // Record list screen
        composable(route = Screen.RecordList.route) {
            RecordListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
