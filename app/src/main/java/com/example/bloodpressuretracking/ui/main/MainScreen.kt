package com.example.bloodpressuretracking.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainScreen(
    onNavigateToRecordList: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var captureUri by remember { mutableStateOf<Uri?>(null) }

    // 撮影ランチャー (要件 1.4)
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onImageCaptured(if (success) captureUri else null)
    }

    // 権限リクエストランチャー (要件 1.2, 1.3)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.prepareCaptureUri()
            captureUri = uri
            takePictureLauncher.launch(uri)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // スナックバー表示
    LaunchedEffect(uiState.resultMessage) {
        uiState.resultMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // OCRエラーダイアログ (要件 5.1, 5.2, 5.3)
    if (uiState.showOcrRetryDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onOcrDismiss,
            title = { Text("認識できませんでした") },
            text = { Text(uiState.ocrErrorMessage ?: "数値を読み取れませんでした") },
            confirmButton = {
                Button(onClick = {
                    viewModel.onOcrRetry()
                    val uri = viewModel.prepareCaptureUri()
                    captureUri = uri
                    takePictureLauncher.launch(uri)
                }) { Text("再撮影") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onOcrDismiss) { Text("手動で入力") }
            }
        )
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
            // タイトル
            Text(
                text = "血圧記録",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // カメラボタン行 (要件 6.1, 6.2, 6.4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "カメラで入力",
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        when (viewModel.onCameraButtonClick(hasPermission)) {
                            CameraAction.RequestPermission ->
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            CameraAction.LaunchCamera -> {
                                val uri = viewModel.prepareCaptureUri()
                                captureUri = uri
                                takePictureLauncher.launch(uri)
                            }
                        }
                    },
                    enabled = !uiState.isSubmitting && !uiState.isAnalyzing && uiState.cameraAvailable
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "カメラで入力")
                }
            }

            // OCR処理中ローディング (要件 2.2)
            if (uiState.isAnalyzing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 最高血圧入力
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
                suffix = { Text("mmHg") },
                supportingText = if (uiState.isOcrFilled && uiState.systolic.isNotEmpty()) {
                    { Text("カメラから自動入力", style = MaterialTheme.typography.labelSmall) }
                } else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 最低血圧入力
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
                suffix = { Text("mmHg") },
                supportingText = if (uiState.isOcrFilled && uiState.diastolic.isNotEmpty()) {
                    { Text("カメラから自動入力", style = MaterialTheme.typography.labelSmall) }
                } else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 脈拍入力
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
                suffix = { Text("bpm") },
                supportingText = if (uiState.isOcrFilled && uiState.pulse.isNotEmpty()) {
                    { Text("カメラから自動入力", style = MaterialTheme.typography.labelSmall) }
                } else null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 送信ボタン (要件 6.4)
            Button(
                onClick = viewModel::onSubmitClick,
                enabled = !uiState.isSubmitting && !uiState.isAnalyzing,
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

            // 登録データ確認ボタン
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
