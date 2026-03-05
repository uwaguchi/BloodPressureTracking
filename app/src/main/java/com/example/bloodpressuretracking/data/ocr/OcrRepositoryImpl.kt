package com.example.bloodpressuretracking.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class OcrRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: BloodPressureOcrParser
) : OcrRepository {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyzeImage(uri: Uri): OcrResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val raw = visionText.text
                        val result = parser.parse(raw)
                        continuation.resume(
                            if (result is OcrResult.Failure) result.copy(rawText = raw)
                            else result
                        )
                    }
                    .addOnFailureListener {
                        continuation.resume(OcrResult.Failure(OcrFailureReason.NO_TEXT_DETECTED))
                    }
            }
        } catch (e: Exception) {
            OcrResult.Failure(OcrFailureReason.NO_TEXT_DETECTED)
        }
    }
}
