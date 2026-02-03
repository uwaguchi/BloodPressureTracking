package com.example.bloodpressuretracking.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Main screen composable for blood pressure data input.
 * Provides input fields for systolic, diastolic, and pulse.
 *
 * @param onNavigateToRecordList Callback to navigate to record list screen
 * @param viewModel ViewModel for main screen logic
 */
@Composable
fun MainScreen(
    onNavigateToRecordList: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Show result message in snackbar
    LaunchedEffect(uiState.resultMessage) {
        uiState.resultMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "血圧記録",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Systolic input (最高血圧)
            OutlinedTextField(
                value = uiState.systolic,
                onValueChange = viewModel::onSystolicChanged,
                label = { Text("最高血圧 (mmHg)") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                suffix = { Text("mmHg") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Diastolic input (最低血圧)
            OutlinedTextField(
                value = uiState.diastolic,
                onValueChange = viewModel::onDiastolicChanged,
                label = { Text("最低血圧 (mmHg)") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                suffix = { Text("mmHg") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pulse input (脈拍)
            OutlinedTextField(
                value = uiState.pulse,
                onValueChange = viewModel::onPulseChanged,
                label = { Text("脈拍 (bpm)") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.onSubmitClick()
                    }
                ),
                suffix = { Text("bpm") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = viewModel::onSubmitClick,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("送信")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigate to record list button
            OutlinedButton(
                onClick = onNavigateToRecordList,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登録データを確認")
            }
        }
    }
}
