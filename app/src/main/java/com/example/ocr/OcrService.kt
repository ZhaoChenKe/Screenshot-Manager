package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

interface OcrService {
    suspend fun recognizeText(context: Context, uri: Uri): String
}

class MlKitOcrService : OcrService {

    companion object {
        private const val TAG = "MlKitOcrService"
    }

    // ChineseTextRecognizer recognizes both Chinese characters, Latin characters, numbers, and symbols
    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override suspend fun recognizeText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputImage = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: Throwable) {
                // If loading by filepath fails (e.g. custom stream), decode scaled bitmap
                val bitmap = loadScaledBitmap(context, uri) ?: return@withContext ""
                InputImage.fromBitmap(bitmap, 0)
            }

            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val fullText = visionText.text
                        continuation.resume(fullText.trim())
                    }
                    .addOnFailureListener { error ->
                        Log.w(TAG, "OCR recognition failed for $uri: ${error.message}")
                        continuation.resume("")
                    }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Unexpected error during OCR processing for $uri", e)
            ""
        }
    }

    private fun loadScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            var scale = 1
            while (options.outWidth / scale > maxDimension || options.outHeight / scale > maxDimension) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error decoding bitmap for OCR", e)
            null
        }
    }
}
