package com.group.smart_kid.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteHelper(private val context: Context) {

    private var interpreter: Interpreter? = null

    init {
        interpreter = Interpreter(loadModelFile())
        val inputShape = interpreter?.getInputTensor(0)?.shape()
        Log.d(
            "Model Input Shape",
            "Shape: ${inputShape?.joinToString()}"
        )  // Should log: [1, 28, 28, 1]
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("digits.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true) // Resize image to 28x28

        val byteBuffer =
            ByteBuffer.allocateDirect(4 * 1 * 28 * 28) // 4 bytes per float (1 batch, 28x28, 1 channel)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(28 * 28)
        resizedBitmap.getPixels(pixels, 0, 28, 0, 0, 28, 28)

        for (i in pixels.indices) {
            val pixelValue = pixels[i]
            val r = (pixelValue shr 16 and 0xFF) / 255.0f  // Normalize Red
            val g = (pixelValue shr 8 and 0xFF) / 255.0f   // Normalize Green
            val b = (pixelValue and 0xFF) / 255.0f         // Normalize Blue

            var grayscale = (r + g + b) / 3.0f // Convert to grayscale (average of RGB)

            // Check if inversion is needed (if digits are black on white instead of white on black)
            grayscale = 1.0f - grayscale // Invert if necessary

            byteBuffer.putFloat(grayscale) // Add normalized value to buffer
        }

        return byteBuffer
    }


    // Run inference with correct input shape (1,28,28,1)
    fun predict(bitmap: Bitmap): FloatArray {
        val inputBuffer = preprocessImage(bitmap)

        val outputSize = 10
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize).order(ByteOrder.nativeOrder())

        interpreter?.run(inputBuffer, outputBuffer)

        // Convert ByteBuffer to FloatArray
        outputBuffer.rewind()
        return FloatArray(outputSize) { outputBuffer.float }
    }
}
