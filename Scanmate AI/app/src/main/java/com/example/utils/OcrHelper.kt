package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object OcrHelper {
    suspend fun extractTextFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): String = runTextRecognition {
        InputImage.fromBitmap(bitmap, rotationDegrees)
    }

    suspend fun extractTextFromFile(context: Context, file: File): String = runTextRecognition {
        InputImage.fromFilePath(context, Uri.fromFile(file))
    }

    private suspend fun runTextRecognition(imageFactory: () -> InputImage): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(imageFactory())
                .addOnSuccessListener { text -> continuation.resume(text.text.trim()) }
                .addOnFailureListener { e -> continuation.resume("OCR failed: ${e.localizedMessage ?: "Unknown error"}") }
        } catch (e: Exception) {
            continuation.resume("OCR failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
