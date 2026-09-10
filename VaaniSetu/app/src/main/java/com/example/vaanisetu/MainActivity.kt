package com.example.vaanisetu

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaanisetu.utils.SharedPreferencesManager
import com.example.vaanisetu.utils.SpeechManager
import com.example.vaanisetu.utils.StealthManager
import com.example.vaanisetu.viewmodel.WalkieTalkieViewModel

/**
 * Main screen of VaaniSetu.
 *
 * Three operating modes reflected by the 3dp mode-indicator bar colour:
 *   Blue  (#1A237E) — Phone Mode  (full-duplex, PTT OFF)
 *   Orange(#FF6F00) — PTT Mode    (half-duplex, walkie-talkie)
 *   Red   (#B71C1C) — Emergency Alert incoming
 */
class MainActivity : AppCompatActivity() {

    // ── UI references ───────────────────────────────────────────────
    private lateinit var modeIndicatorBar: View
    private lateinit var btnBack: TextView
    private lateinit var speakingIndicator: View
    private lateinit var languageDropdown: Spinner
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var pttButton: com.google.android.material.button.MaterialButton
    private lateinit var modeToggleLabel: TextView
    private lateinit var emergencyOverlayContainer: FrameLayout

    // ── State ────────────────────────────────────────────────────────
    private enum class AppMode { PHONE, PTT, EMERGENCY }
    private var currentMode = AppMode.PHONE
    private var isFirstPttPress = true  // For the two-pulse haptic per Design1.md

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var speechManager: SpeechManager
    private lateinit var stealthManager: StealthManager
    private lateinit var viewModel: WalkieTalkieViewModel

    // ── Lifecycle ────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsManager = SharedPreferencesManager(this)
        stealthManager = StealthManager(this)
        
        com.example.vaanisetu.network.NearbyConnectionsManager.init(this, prefsManager.getUserName())
        
        speechManager = SpeechManager(this) { recognizedText, isFinal ->
            val langCode = prefsManager.getLanguage()
            val sender = prefsManager.getUserName()
            val channel = viewModel.currentChannel.value
            val urgency = if (currentMode == AppMode.EMERGENCY) 1 else 0

            if (!isFinal) {
                // Update live text preview
                viewModel.updateLiveSpeechText(recognizedText)
            } else {
                // Final result
                viewModel.updateLiveSpeechText("")
                if (recognizedText.isNotBlank()) {
                    val payload = com.example.vaanisetu.network.MessagePayload(
                        sender = sender,
                        channel = channel,
                        langCode = langCode,
                        urgencyFlag = urgency,
                        text = recognizedText
                    )
                    com.example.vaanisetu.network.NearbyConnectionsManager.broadcastMessage(payload)
                    viewModel.queueIncomingMessage(
                        com.example.vaanisetu.viewmodel.IncomingMessage(sender, langCode, urgency, recognizedText),
                        isOwnMessage = true
                    )
                }
            }
        }
        val vibrator = getSystemService(android.os.Vibrator::class.java)
        
        // Initialize Room DB
        val db = androidx.room.Room.databaseBuilder(
            applicationContext,
            com.example.vaanisetu.data.local.AppDatabase::class.java, "vaanisetu-db"
        ).build()
        
        viewModel = WalkieTalkieViewModel(speechManager, stealthManager, vibrator, db.messageDao())

        bindViews()
        setupLanguageDropdown()
        setupPttButton()
        setupMessageList()
        observeViewModel()

        // Start strictly in PTT Mode (Orange)
        applyMode(AppMode.PTT)
        
        // Restore active channels (activity within last 5 minutes)
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val fiveMinsAgo = System.currentTimeMillis() - (5 * 60 * 1000)
            val activeChannels = db.messageDao().getActiveChannels(fiveMinsAgo)
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                for (channel in activeChannels) {
                    addChannelTab(channel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stealthManager.start()
    }

    override fun onPause() {
        super.onPause()
        stealthManager.stop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.pttState.collect { state ->
                // Update UI based on PTT state if needed
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.emergencyEvent.collect { message ->
                triggerEmergencyAlert(message.text)
            }
        }
        lifecycleScope.launchWhenStarted {
            com.example.vaanisetu.network.NearbyConnectionsManager.incomingPayloads.collect { payload ->
                viewModel.processNetworkPayload(payload)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.channelMessages.collect { messages ->
                messageAdapter.setMessages(messages)
                if (messages.isNotEmpty()) {
                    messageRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.liveSpeechText.collect { text ->
                if (text.isBlank()) {
                    liveSpeechPreview.visibility = android.view.View.GONE
                } else {
                    liveSpeechPreview.visibility = android.view.View.VISIBLE
                    liveSpeechPreview.text = text
                }
            }
        }
    }

    private lateinit var btnAddChannel: TextView
    private lateinit var tabGlobal: TextView
    private lateinit var liveSpeechPreview: TextView

    // ── View binding ─────────────────────────────────────────────────
    private fun bindViews() {
        modeIndicatorBar = findViewById(R.id.modeIndicatorBar)
        btnBack = findViewById(R.id.btnBack)
        speakingIndicator = findViewById(R.id.speakingIndicator)
        languageDropdown = findViewById(R.id.languageDropdown)
        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        pttButton = findViewById(R.id.pttButton)
        modeToggleLabel = findViewById(R.id.modeToggleLabel)
        btnAddChannel = findViewById(R.id.btnAddChannel)
        tabGlobal = findViewById(R.id.tabGlobal)
        liveSpeechPreview = findViewById(R.id.liveSpeechPreview)

        btnBack.setOnClickListener { finish() }

        tabGlobal.setOnClickListener {
            viewModel.switchChannel("Global")
            updateChannelTabs("Global")
        }

        btnAddChannel.setOnClickListener {
            showCreateChannelDialog()
        }

        // Inflate the emergency overlay and attach it to the root
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        val overlay = LayoutInflater.from(this).inflate(R.layout.overlay_emergency, rootLayout, false)
        rootLayout.addView(overlay)
        emergencyOverlayContainer = overlay.findViewById(R.id.emergencyOverlay)
        
        emergencyOverlayContainer.setOnClickListener {
            applyMode(AppMode.PTT)
        }
    }

    // ── Language dropdown ────────────────────────────────────────────
    private fun setupLanguageDropdown() {
        val languages = arrayOf(
            getString(R.string.lang_english),
            getString(R.string.lang_hindi),
            getString(R.string.lang_marathi),
            getString(R.string.lang_gujarati),
            getString(R.string.lang_kannada),
            getString(R.string.lang_malayalam),
            getString(R.string.lang_tamil),
            getString(R.string.lang_telugu),
            getString(R.string.lang_odia),
            getString(R.string.lang_bengali)
        )
        
        val tags = arrayOf(
            "en-IN", "hi-IN", "mr-IN", "gu-IN", "kn-IN", 
            "ml-IN", "ta-IN", "te-IN", "or-IN", "bn-IN"
        )
        
        val adapter = ArrayAdapter(this, R.layout.item_spinner, languages)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        languageDropdown.adapter = adapter
        languageDropdown.setPopupBackgroundResource(android.R.color.background_dark)

        val savedLang = prefsManager.getLanguage()
        val index = tags.indexOf(savedLang).takeIf { it >= 0 } ?: 0
        languageDropdown.setSelection(index)

        languageDropdown.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tag = tags[position]
                if (prefsManager.getLanguage() != tag) {
                    prefsManager.saveLanguage(tag)
                    
                    // Translate entire app UI instantly
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags(tag)
                    )
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun showCreateChannelDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.channel_name_hint)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.channel_create_title))
            .setView(input)
            .setPositiveButton(getString(R.string.channel_create)) { _, _ ->
                val channelName = input.text.toString().trim()
                if (channelName.isNotEmpty()) {
                    addChannelTab(channelName)
                    viewModel.switchChannel(channelName)
                    updateChannelTabs(channelName)
                }
            }
            .setNegativeButton(getString(R.string.channel_cancel), null)
            .show()
    }

    private fun addChannelTab(channelName: String) {
        val layout = findViewById<android.widget.LinearLayout>(R.id.channelTabsLayout)
        val newTab = TextView(this).apply {
            text = channelName
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(
                (20 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt()
            )
            setOnClickListener {
                viewModel.switchChannel(channelName)
                updateChannelTabs(channelName)
            }
        }
        // Insert right before the + New button
        layout.addView(newTab, layout.childCount - 1)
    }

    private fun updateChannelTabs(activeChannel: String) {
        val layout = findViewById<android.widget.LinearLayout>(R.id.channelTabsLayout)
        for (i in 0 until layout.childCount) {
            val tab = layout.getChildAt(i) as TextView
            if (tab.id == R.id.btnAddChannel) continue
            val tabName = if (tab.id == R.id.tabGlobal) "Global" else tab.text.toString()
            if (tabName.equals(activeChannel, ignoreCase = true)) {
                tab.setTextColor(0xFFFFFFFF.toInt()) // active white
            } else {
                tab.setTextColor(0xFF888888.toInt()) // inactive gray
            }
        }
    }

    // ── PTT button ───────────────────────────────────────────────────
    // Design1.md: "On first PTT press in a new session, add a single 11ms
    // micro-vibration before the main 43ms haptic. This two-pulse pattern
    // (short-long) feels like a real radio 'click'"
    private fun setupPttButton() {
        pttButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (currentMode == AppMode.PTT) {
                        triggerPttHaptic()
                        showSpeakingIndicator(true)
                        val langCode = prefsManager.getLanguage()
                        viewModel.onPttPressed(langCode)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (currentMode == AppMode.PTT) {
                        triggerReleaseHaptic()
                        showSpeakingIndicator(false)
                        viewModel.onPttReleased()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerPttHaptic() {
        try {
            val vibrator = getSystemService<Vibrator>() ?: return
            if (isFirstPttPress) {
                // Two-pulse pattern: 11ms micro-vibration then 43ms main haptic
                val pattern = longArrayOf(0, 11, 30, 43)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                isFirstPttPress = false
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(43, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // Ignore haptic crashes on some emulators
        }
    }

    private fun triggerReleaseHaptic() {
        try {
            val vibrator = getSystemService<Vibrator>() ?: return
            // Light double-tap on release
            val pattern = longArrayOf(0, 15, 30, 15)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (e: Exception) {
            // Ignore haptic crashes on some emulators
        }
    }

    // ── Speaking indicator ────────────────────────────────────────────
    // Design1.md: size pulse 8dp→11dp, NOT alpha/opacity
    private fun showSpeakingIndicator(show: Boolean) {
        if (show) {
            speakingIndicator.visibility = View.VISIBLE
            val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_speaking_indicator)
            speakingIndicator.startAnimation(pulseAnim)
        } else {
            speakingIndicator.clearAnimation()
            speakingIndicator.visibility = View.GONE
        }
    }

    // ── Mode switching ───────────────────────────────────────────────
    private fun applyMode(mode: AppMode) {
        currentMode = mode
        when (mode) {
            AppMode.PTT -> {
                modeIndicatorBar.setBackgroundColor(0xFFFF6F00.toInt())  // Amber
                pttButton.setBackgroundResource(R.drawable.bg_ptt_button_orange)
                modeToggleLabel.text = getString(R.string.mode_ptt)
                emergencyOverlayContainer.visibility = View.GONE
            }
            AppMode.EMERGENCY -> {
                modeIndicatorBar.setBackgroundColor(0xFFB71C1C.toInt())  // Red
                pttButton.setBackgroundResource(R.drawable.bg_ptt_button_red)
                modeToggleLabel.text = getString(R.string.mode_emergency)
                // Design1.md: full-screen solid red override
                emergencyOverlayContainer.visibility = View.VISIBLE
            }
            else -> {}
        }
    }



    private lateinit var messageAdapter: com.example.vaanisetu.ui.MessageAdapter

    // ── Message list ─────────────────────────────────────────────────
    private fun setupMessageList() {
        messageAdapter = com.example.vaanisetu.ui.MessageAdapter()
        messageRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messageRecyclerView.adapter = messageAdapter
    }

    // ── Public methods for other managers ─────────────────────────────


    fun triggerEmergencyAlert(message: String) {
        applyMode(AppMode.EMERGENCY)
        emergencyOverlayContainer.findViewById<TextView>(R.id.emergencyAlertText)?.text = message
    }
}