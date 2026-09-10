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
    private lateinit var peerCounterText: TextView
    private lateinit var speakingIndicator: View
    private lateinit var languageDropdown: Spinner
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var pttButton: ImageButton
    private lateinit var modeToggleLabel: TextView
    private lateinit var emergencyOverlayContainer: FrameLayout

    // Emergency preset buttons
    private lateinit var btnMedical: ImageButton
    private lateinit var btnFire: ImageButton
    private lateinit var btnWater: ImageButton

    // ── State ────────────────────────────────────────────────────────
    private enum class AppMode { PHONE, PTT, EMERGENCY }
    private var currentMode = AppMode.PHONE
    private var isFirstPttPress = true  // For the two-pulse haptic per Design1.md

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var speechManager: SpeechManager
    private lateinit var stealthManager: StealthManager
    private lateinit var viewModel: WalkieTalkieViewModel

    private lateinit var nearbyManager: com.example.vaanisetu.network.NearbyConnectionsManager

    // ── Lifecycle ────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsManager = SharedPreferencesManager(this)
        stealthManager = StealthManager(this)
        
        nearbyManager = com.example.vaanisetu.network.NearbyConnectionsManager(this, prefsManager.getUserName())
        
        speechManager = SpeechManager(this) { recognizedText ->
            // Pass correct BCP-47 tag and build payload instantly
            val langCode = prefsManager.getLanguage()
            val sender = prefsManager.getUserName()
            val channel = viewModel.currentChannel.value
            val urgency = if (currentMode == AppMode.EMERGENCY) 1 else 0
            
            val payload = com.example.vaanisetu.network.MessagePayload(
                sender = sender,
                channel = channel,
                langCode = langCode,
                urgencyFlag = urgency,
                text = recognizedText
            )
            nearbyManager.broadcastMessage(payload)
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
        setupModeToggle()
        setupEmergencyPresets()
        setupMessageList()
        observeViewModel()

        // Start in Phone Mode (Blue)
        applyMode(AppMode.PHONE)
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
            nearbyManager.incomingPayloads.collect { payload ->
                viewModel.processNetworkPayload(payload)
            }
        }
    }

    // ── View binding ─────────────────────────────────────────────────
    private fun bindViews() {
        modeIndicatorBar = findViewById(R.id.modeIndicatorBar)
        peerCounterText = findViewById(R.id.peerCounterText)
        speakingIndicator = findViewById(R.id.speakingIndicator)
        languageDropdown = findViewById(R.id.languageDropdown)
        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        pttButton = findViewById(R.id.pttButton)
        modeToggleLabel = findViewById(R.id.modeToggleLabel)

        btnMedical = findViewById(R.id.btnEmergencyMedical)
        btnFire = findViewById(R.id.btnEmergencyFire)
        btnWater = findViewById(R.id.btnEmergencyWater)

        // Inflate the emergency overlay and attach it to the root
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        val overlay = LayoutInflater.from(this).inflate(R.layout.overlay_emergency, rootLayout, false)
        rootLayout.addView(overlay)
        emergencyOverlayContainer = overlay.findViewById(R.id.emergencyOverlay)
    }

    // ── Language dropdown ────────────────────────────────────────────
    private fun setupLanguageDropdown() {
        val languages = arrayOf(
            getString(R.string.lang_english),
            getString(R.string.lang_hindi),
            getString(R.string.lang_marathi)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageDropdown.adapter = adapter

        // Pre-select from saved preference
        val savedLang = prefsManager.getLanguage()
        val index = when (savedLang) {
            "hi-IN" -> 1
            "mr-IN" -> 2
            else -> 0
        }
        languageDropdown.setSelection(index)

        // Instantly pass correct BCP-47 tag to preferences
        languageDropdown.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tag = when (position) {
                    1 -> "hi-IN"
                    2 -> "mr-IN"
                    else -> "en-IN"
                }
                prefsManager.saveLanguage(tag)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
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
        val vibrator = getSystemService<Vibrator>() ?: return
        if (isFirstPttPress) {
            // Two-pulse pattern: 11ms micro-vibration then 43ms main haptic
            val pattern = longArrayOf(0, 11, 30, 43)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            isFirstPttPress = false
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(43, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun triggerReleaseHaptic() {
        val vibrator = getSystemService<Vibrator>() ?: return
        // Light double-tap on release
        val pattern = longArrayOf(0, 15, 30, 15)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
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
    private fun setupModeToggle() {
        // Long-press PTT to toggle between Phone and PTT modes
        pttButton.setOnLongClickListener {
            when (currentMode) {
                AppMode.PHONE -> applyMode(AppMode.PTT)
                AppMode.PTT -> applyMode(AppMode.PHONE)
                AppMode.EMERGENCY -> {} // Cannot toggle during emergency
            }
            true
        }
    }

    private fun applyMode(mode: AppMode) {
        currentMode = mode
        when (mode) {
            AppMode.PHONE -> {
                modeIndicatorBar.setBackgroundColor(0xFF1A237E.toInt())  // Navy
                pttButton.setBackgroundResource(R.drawable.bg_ptt_button_blue)
                modeToggleLabel.text = getString(R.string.mode_phone)
                emergencyOverlayContainer.visibility = View.GONE
            }
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
        }
    }

    // ── Emergency presets ───────────────────────────────────────────
    private fun setupEmergencyPresets() {
        btnMedical.setOnClickListener {
            applyMode(AppMode.EMERGENCY)
            emergencyOverlayContainer.findViewById<TextView>(R.id.emergencyAlertText)?.text =
                getString(R.string.emergency_medical)
            // Payload send will be wired in Phase 1.4
        }
        btnFire.setOnClickListener {
            applyMode(AppMode.EMERGENCY)
            emergencyOverlayContainer.findViewById<TextView>(R.id.emergencyAlertText)?.text =
                getString(R.string.emergency_fire)
            // Payload send will be wired in Phase 1.4
        }
        btnWater.setOnClickListener {
            applyMode(AppMode.EMERGENCY)
            emergencyOverlayContainer.findViewById<TextView>(R.id.emergencyAlertText)?.text =
                getString(R.string.emergency_flood)
            // Payload send will be wired in Phase 1.4
        }

        // Dismiss emergency overlay on tap
        emergencyOverlayContainer.setOnClickListener {
            applyMode(AppMode.PHONE)
        }
    }

    // ── Message list ─────────────────────────────────────────────────
    private fun setupMessageList() {
        messageRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        // Adapter will be connected in Phase 1.5/1.6 when channel logic is built
    }

    // ── Public methods for other managers ─────────────────────────────
    fun updatePeerCount(count: Int) {
        peerCounterText.text = getString(R.string.peer_count_format, count)
    }

    fun triggerEmergencyAlert(message: String) {
        applyMode(AppMode.EMERGENCY)
        emergencyOverlayContainer.findViewById<TextView>(R.id.emergencyAlertText)?.text = message
    }
}