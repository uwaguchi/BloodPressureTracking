package com.example.bloodpressuretracking.data.ocr

import android.net.Uri

interface OcrRepository {
    suspend fun analyzeImage(uri: Uri): OcrResult
}
