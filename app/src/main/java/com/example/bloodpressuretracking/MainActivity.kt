package com.example.bloodpressuretracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.bloodpressuretracking.ui.AppViewModel
import com.example.bloodpressuretracking.ui.navigation.AppNavHost
import com.example.bloodpressuretracking.ui.navigation.Screen
import com.example.bloodpressuretracking.ui.theme.BloodPressureTrackingTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the BloodPressureTracking app.
 * Handles session check and navigation setup.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BloodPressureTrackingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BloodPressureApp()
                }
            }
        }
    }
}

/**
 * Root composable for the app.
 * Handles session checking and navigation.
 */
@Composable
fun BloodPressureApp(
    viewModel: AppViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    when {
        // Show loading while checking session
        uiState.isCheckingSession -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // Navigate based on authentication state
        else -> {
            val startDestination = if (uiState.isAuthenticated) {
                Screen.Main.route
            } else {
                Screen.Login.route
            }

            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                onLoginSuccess = { viewModel.onLoginSuccess() }
            )
        }
    }
}
