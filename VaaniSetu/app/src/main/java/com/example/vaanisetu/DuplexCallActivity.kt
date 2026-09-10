package com.example.vaanisetu

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaanisetu.network.NearbyConnectionsManager
import com.example.vaanisetu.network.MessagePayload
import com.example.vaanisetu.utils.SharedPreferencesManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Full-duplex call screen. Continuous STT loop with echo suppression.
 * Incoming text is read aloud via TTS as "[Name] says: [text]".
 */
class DuplexCallActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var prefsManager: SharedPreferencesManager
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isTtsSpeaking = false

    private lateinit var callPeerName: TextView
    private lateinit var callStatus: TextView
    private lateinit var callTranscript: RecyclerView
    private lateinit var listeningDot: View
    private lateinit var listeningLabel: TextView
    private lateinit var btnBack: ImageButton

    private val transcriptMessages = mutableListOf<TranscriptItem>()
    private lateinit var transcriptAdapter: TranscriptAdapter

    private var peerEndpointId: String = ""
    private var peerName: String = ""

    data class TranscriptItem(val sender: String, val text: String, val timestamp: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duplex_call)

        prefsManager = SharedPreferencesManager(this)
        peerEndpointId = intent.getStringExtra("PEER_ENDPOINT_ID") ?: ""
        peerName = intent.getStringExtra("PEER_NAME") ?: "Unknown"

        bindViews()
        callPeerName.text = getString(R.string.calling_format, peerName)

        setupTranscript()

        // Init TTS
        tts = TextToSpeech(this, this)

        // Init Nearby (Singleton)
        NearbyConnectionsManager.init(this, prefsManager.getUserName())

        // Observe incoming
        lifecycleScope.launch {
            NearbyConnectionsManager.incomingPayloads.collect { payload ->
                addToTranscript(payload.sender, payload.text)
                speakMessage(payload.sender + " says: " + payload.text)
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.forLanguageTag(prefsManager.getLanguage())
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isTtsSpeaking = true
                    // Pause STT while TTS speaks (echo suppression)
                    stopListening()
                }
                override fun onDone(utteranceId: String?) {
                    isTtsSpeaking = false
                    // Resume STT after TTS finishes
                    runOnUiThread { startListening() }
                }
                override fun onError(utteranceId: String?) {
                    isTtsSpeaking = false
                    runOnUiThread { startListening() }
                }
            })
            // Start continuous listening
            startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        tts?.shutdown()
    }

    private fun bindViews() {
        callPeerName = findViewById(R.id.callPeerName)
        callStatus = findViewById(R.id.callStatus)
        callTranscript = findViewById(R.id.callTranscript)
        listeningDot = findViewById(R.id.listeningDot)
        listeningLabel = findViewById(R.id.listeningLabel)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupTranscript() {
        transcriptAdapter = TranscriptAdapter(transcriptMessages)
        callTranscript.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        callTranscript.adapter = transcriptAdapter
    }

    private fun addToTranscript(sender: String, text: String) {
        val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
        transcriptMessages.add(TranscriptItem(sender, text, time))
        transcriptAdapter.notifyItemInserted(transcriptMessages.size - 1)
        callTranscript.scrollToPosition(transcriptMessages.size - 1)
    }

    // ── Continuous STT Loop ──────────────────────────────────────────
    private fun startListening() {
        if (isTtsSpeaking) return // Don't listen while speaking (echo suppression)

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return

                // Add to local transcript
                addToTranscript(getString(R.string.you_label), text)

                // Broadcast to peer
                val payload = MessagePayload(
                    sender = prefsManager.getUserName(),
                    channel = "duplex",
                    langCode = prefsManager.getLanguage(),
                    urgencyFlag = 0,
                    text = text
                )
                NearbyConnectionsManager.broadcastMessage(payload)

                // Restart STT loop
                startListening()
            }
            override fun onError(error: Int) {
                // Restart on error (timeout, no speech detected, etc.)
                startListening()
            }
            override fun onReadyForSpeech(params: Bundle?) {
                listeningDot.visibility = View.VISIBLE
                listeningLabel.text = getString(R.string.listening)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                listeningDot.visibility = View.INVISIBLE
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, prefsManager.getLanguage())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) { /* permissions may be missing */ }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
    }

    private fun speakMessage(text: String) {
        if (!isTtsReady) return
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, "duplex_" + System.currentTimeMillis())
    }

    // ── Simple transcript adapter ────────────────────────────────────
    inner class TranscriptAdapter(private val items: List<TranscriptItem>) :
        RecyclerView.Adapter<TranscriptAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val sender: TextView = itemView.findViewById(R.id.senderNameText)
            val text: TextView = itemView.findViewById(R.id.messageText)
            val time: TextView = itemView.findViewById(R.id.timestampText)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.sender.text = item.sender
            holder.text.text = item.text
            holder.time.text = item.timestamp
        }

        override fun getItemCount() = items.size
    }
}
