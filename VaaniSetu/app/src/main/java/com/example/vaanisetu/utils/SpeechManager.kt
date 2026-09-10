package com.example.vaanisetu.utils

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechManager(private val context: Context, private val onSpeechResult: (String) -> Unit) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        textToSpeech = TextToSpeech(context, this)
        setupSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            // Default to English, but we will switch dynamically based on incoming message lang_code
            textToSpeech?.language = Locale("en", "IN")
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
            })
        }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    onSpeechResult("") // Or handle error properly
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onSpeechResult(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(langCode: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode.replace("-", "_"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            android.widget.Toast.makeText(context, "Microphone permission denied!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            android.widget.Toast.makeText(context, "Emulator missing Speech SDK: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore if already stopped or destroyed
        }
    }

    fun speak(text: String, sender: String, langCode: String, urgencyFlag: Int) {
        if (!isTtsInitialized) return

        val utteranceId = System.currentTimeMillis().toString()
        val speechText = "$sender says: $text"
        
        val localeParts = langCode.split("-")
        if (localeParts.size == 2) {
            textToSpeech?.language = Locale(localeParts[0], localeParts[1])
        } else {
            textToSpeech?.language = Locale(langCode)
        }

        if (urgencyFlag == 1) {
            // Emergency: Use Alarm stream and max volume
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            textToSpeech?.setAudioAttributes(audioAttributes)
        } else {
            // Normal: Use Music/Media stream
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
                
            textToSpeech?.setAudioAttributes(audioAttributes)
        }

        // CRITICAL RULE: Never use QUEUE_FLUSH. Always use QUEUE_ADD.
        textToSpeech?.speak(speechText, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    fun destroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}
