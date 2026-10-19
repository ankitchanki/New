package com.vibe.stashdrop.features.circletosearch

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class OcrAndBarcodeProcessor : AutoCloseable {
    private val text = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodes = BarcodeScanning.getClient()
    data class Result(val text: String, val codes: List<String>)
    suspend fun process(bitmap: Bitmap): Result = coroutineScope {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognized = async { text.process(image).await().text }
        val scanned = async { barcodes.process(image).await().mapNotNull { it.rawValue } }
        Result(recognized.await(), scanned.await())
    }
    override fun close() { text.close(); barcodes.close() }
}
