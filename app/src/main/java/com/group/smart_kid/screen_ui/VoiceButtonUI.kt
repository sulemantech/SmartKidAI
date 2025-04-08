package com.group.smart_kid.screen_ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat

@Composable
fun VoiceButtonUI(

    mainActivity: MainActivity, predictionCallback: (String) -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(mainActivity) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
    }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            startSpeechRecognition(speechRecognizer, recognizerIntent) { result ->
                predictionCallback(result)
                isListening = false
            }
        } else {
            predictionCallback("Permission denied!")
        }
    }

    // Glowing mic overlay
    if (isListening) {
        ListeningDialog {
            // Optional: allow tap to cancel
            isListening = false
            speechRecognizer.stopListening()
        }
    }

    val context = LocalContext.current

    Button(onClick = {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            isListening = true
            startSpeechRecognition(speechRecognizer, recognizerIntent) { result ->
                predictionCallback(result)
                isListening = false
            }
        }
    }) {
        BasicText("Speak", style = TextStyle(color = Color.White))
    }
}

fun startSpeechRecognition(
    speechRecognizer: SpeechRecognizer, recognizerIntent: Intent, onResult: (String) -> Unit
) {
    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                onResult(it.firstOrNull() ?: "No speech recognized")
            }
        }

        override fun onError(error: Int) {
            if(error == 7) {
                onResult("No speech recognized")
            } else {
                onResult("Error: $error")
            }

        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    speechRecognizer.startListening(recognizerIntent)
}

