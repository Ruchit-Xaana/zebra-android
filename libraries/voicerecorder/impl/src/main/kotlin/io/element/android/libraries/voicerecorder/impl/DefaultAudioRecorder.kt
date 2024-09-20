/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.voicerecorder.impl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.voicerecorder.api.SpeechRecognitionListener
import timber.log.Timber
import javax.inject.Inject

@SingleIn(RoomScope::class)
class DefaultAudioRecorder@Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private lateinit var speechRecognizer: SpeechRecognizer
    private var listener: SpeechRecognitionListener? = null

    fun startRecording(listener: SpeechRecognitionListener) {
        this.listener = listener
        if(SpeechRecognizer.isRecognitionAvailable(context)){
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer.setRecognitionListener(object : RecognitionListener{
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    //Handle error and send message
                    listener.onError(error)
                    Timber.e("Speech recognition error: $error")
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.get(0) ?: ""
                    // Handle recognized text
                    listener.onTextRecognized(recognizedText)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = matches?.get(0) ?: ""
                    // Handle partial text
                    listener.onTextRecognized(partialText)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val recognitionIntent = Intent (
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // Set appropriate language
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) // Get only one result
                }
            )
            speechRecognizer.startListening(recognitionIntent)
        }
    }

    fun stopRecording() {
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
    }
}
