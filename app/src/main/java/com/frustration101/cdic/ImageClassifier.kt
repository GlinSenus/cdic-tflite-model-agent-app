package com.frustration101.cdic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class ImageClassifier(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private val inputImageSize = 256  // Change this if your model uses a different input size
    private val modelPath = "model3b.tflite"
    private val labels: List<String>

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(modelBuffer)
        labels = FileUtil.loadLabels(context, "labels.txt")
    }

    
	 
fun classify(image: Bitmap): String {
    val squareBitmap = cropToSquare(image)
    val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, inputImageSize, inputImageSize, true)
    val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

    // Model output
    val output = Array(1) { FloatArray(labels.size) }
    interpreter.run(inputBuffer, output)

    val results = output[0]

    // Pair each label with its confidence
    val labelResults = labels.zip(results.toList())

    // Sort by confidence (highest first)
    val sortedResults = labelResults.sortedByDescending { it.second }

    // Take top 3 that have >= 0.20 confidence
    val topPredictions = sortedResults.filter { it.second >= 0.20f }.take(3)

    if (topPredictions.isEmpty()) return "No confident prediction."

    // Build a readable string result
    val resultText = topPredictions.joinToString("\n") { (label, confidence) ->
        "- $label (%.2f%%)".format(confidence * 100)
    }

    return resultText
} 
	 
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = min(width, height)

        val left = 0
        val top = (height - size) / 2  // cuts from top and bottom
        val right = left + size
        val bottom = top + size

        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    /**
     * Convert bitmap to ByteBuffer for model input.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputImageSize * inputImageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputImageSize * inputImageSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixelIndex = 0
        for (i in 0 until inputImageSize) {
            for (j in 0 until inputImageSize) {
                val pixelValue = intValues[pixelIndex++]

                // Extract RGB channels, normalize to [0,1]
                val r = ((pixelValue shr 16) and 0xFF) / 255.0f
                val g = ((pixelValue shr 8) and 0xFF) / 255.0f
                val b = (pixelValue and 0xFF) / 255.0f

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }

        return byteBuffer
    }

    fun close() {
        interpreter.close()
    }
}