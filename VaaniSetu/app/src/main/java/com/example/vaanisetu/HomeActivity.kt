package com.example.vaanisetu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaanisetu.network.NearbyConnectionsManager
import com.example.vaanisetu.ui.PeerAdapter
import com.example.vaanisetu.ui.PeerInfo
import com.example.vaanisetu.utils.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Home Screen — the main entry point after onboarding.
 * Shows connected peers, connectivity status, and mode buttons.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var nearbyManager: NearbyConnectionsManager
    private lateinit var peerAdapter: PeerAdapter

    // UI refs
    private lateinit var peerRecyclerView: RecyclerView
    private lateinit var emptyPeersText: TextView
    private lateinit var peerCountBadge: TextView
    private lateinit var iconBluetooth: ImageView
    private lateinit var labelBluetooth: TextView
    private lateinit var iconWifi: ImageView
    private lateinit var labelWifi: TextView
    private lateinit var languageDropdown: Spinner
    private lateinit var btnProfile: ImageButton
    private lateinit var btnPttMode: MaterialButton
    private lateinit var btnAlert: MaterialButton

    // Emergency overlay (inflated on top of this activity)
    private lateinit var emergencyOverlayContainer: FrameLayout

    // TTS for emergency alert readout
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled — Nearby will work or show errors */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        prefsManager = SharedPreferencesManager(this)

        // Check onboarding
        if (!prefsManager.isOnboardingCompleted()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Re-request permissions if missing (user directive)
        requestMissingPermissions()

        // Init TTS for emergency readout
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val lang = prefsManager.getLanguage()
                tts?.language = Locale.forLanguageTag(lang)
            }
        }

        bindViews()
        setupLanguageDropdown()
        setupPeerList()
        setupButtons()
        inflateEmergencyOverlay()

        // Init Nearby
        nearbyManager = NearbyConnectionsManager(this, prefsManager.getUserName())
        observePeers()
    }

    override fun onResume() {
        super.onResume()
        // Always re-request if permissions were revoked
        requestMissingPermissions()
        try {
            nearbyManager.startAdvertising()
            nearbyManager.startDiscovery()
        } catch (_: Exception) { /* permissions may not be granted yet */ }
        updateConnectivityIcons(true)
    }

    override fun onPause() {
        super.onPause()
        // Don't stop — let networking run
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        try { nearbyManager.stopAll() } catch (_: Exception) {}
    }

    // ── Views ────────────────────────────────────────────────────────
    private fun bindViews() {
        peerRecyclerView = findViewById(R.id.peerRecyclerView)
        emptyPeersText = findViewById(R.id.emptyPeersText)
        peerCountBadge = findViewById(R.id.peerCountBadge)
        iconBluetooth = findViewById(R.id.iconBluetooth)
        labelBluetooth = findViewById(R.id.labelBluetooth)
        iconWifi = findViewById(R.id.iconWifi)
        labelWifi = findViewById(R.id.labelWifi)
        languageDropdown = findViewById(R.id.languageDropdownHome)
        btnProfile = findViewById(R.id.btnProfile)
        btnPttMode = findViewById(R.id.btnPttMode)
        btnAlert = findViewById(R.id.btnAlert)
    }

    // ── Language Dropdown ─────────────────────────────────────────────
    private fun setupLanguageDropdown() {
        val languages = arrayOf(
            getString(R.string.lang_english),
            getString(R.string.lang_hindi),
            getString(R.string.lang_marathi)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageDropdown.adapter = adapter

        val savedLang = prefsManager.getLanguage()
        val index = when (savedLang) {
            "hi-IN" -> 1
            "mr-IN" -> 2
            else -> 0
        }
        languageDropdown.setSelection(index)

        languageDropdown.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tag = when (position) {
                    1 -> "hi-IN"
                    2 -> "mr-IN"
                    else -> "en-IN"
                }
                prefsManager.saveLanguage(tag)
                tts?.language = Locale.forLanguageTag(tag)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    // ── Peer List ─────────────────────────────────────────────────────
    private fun setupPeerList() {
        peerAdapter = PeerAdapter { peer ->
            // Launch duplex call with this peer
            val intent = Intent(this, DuplexCallActivity::class.java)
            intent.putExtra("PEER_ENDPOINT_ID", peer.endpointId)
            intent.putExtra("PEER_NAME", peer.displayName)
            startActivity(intent)
        }
        peerRecyclerView.layoutManager = LinearLayoutManager(this)
        peerRecyclerView.adapter = peerAdapter
    }

    private fun observePeers() {
        lifecycleScope.launch {
            nearbyManager.peerCount.collectLatest { count ->
                peerCountBadge.text = count.toString()
                emptyPeersText.visibility = if (count == 0) View.VISIBLE else View.GONE
                peerRecyclerView.visibility = if (count > 0) View.VISIBLE else View.GONE

                // Build peer list from connected endpoints
                val peers = nearbyManager.connectedEndpoints.map { endpointId ->
                    val name = nearbyManager.getPeerName(endpointId) ?: endpointId
                    PeerInfo(endpointId, name)
                }
                peerAdapter.updatePeers(peers)
                updateConnectivityIcons(count > 0)
            }
        }

        // Also observe incoming emergency payloads
        lifecycleScope.launch {
            nearbyManager.incomingPayloads.collect { payload ->
                if (payload.urgencyFlag == 1) {
                    triggerEmergencyAlert(payload.sender + " says: " + payload.text)
                }
            }
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────
    private fun setupButtons() {
        // Profile icon → re-open onboarding for name/language edit
        btnProfile.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java)
            intent.putExtra("EDIT_MODE", true)
            startActivity(intent)
        }

        // PTT Channel Mode → existing walkie-talkie screen
        btnPttMode.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Alert button → send emergency alert to all connected peers + show overlay
        btnAlert.setOnClickListener {
            val alertText = getString(R.string.emergency_alert_default)
            // Broadcast urgency=1 to all peers
            val payload = com.example.vaanisetu.network.MessagePayload(
                sender = prefsManager.getUserName(),
                channel = "global",
                langCode = prefsManager.getLanguage(),
                urgencyFlag = 1,
                text = alertText
            )
            nearbyManager.broadcastMessage(payload)
            triggerEmergencyAlert(prefsManager.getUserName() + " says: " + alertText)
        }
    }

    // ── Emergency Overlay ─────────────────────────────────────────────
    private fun inflateEmergencyOverlay() {
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.homeRoot)
        val overlay = LayoutInflater.from(this).inflate(R.layout.overlay_emergency, rootLayout, false)
        rootLayout.addView(overlay)
        emergencyOverlayContainer = overlay.findViewById(R.id.emergencyOverlay)
        emergencyOverlayContainer.setOnClickListener {
            emergencyOverlayContainer.visibility = View.GONE
        }
    }

    private fun triggerEmergencyAlert(message: String) {
        // Show overlay
        emergencyOverlayContainer.visibility = View.VISIBLE
        emergencyOverlayContainer.findViewById<TextView>(R.id.emergencyAlertText)?.text = message

        // Play at max volume using USAGE_ALARM to bypass DND
        if (isTtsReady) {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

            val params = Bundle()
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            tts?.speak(message, TextToSpeech.QUEUE_ADD, params, "emergency_" + System.currentTimeMillis())
        }
    }

    // ── Connectivity Icons ────────────────────────────────────────────
    private fun updateConnectivityIcons(hasConnections: Boolean) {
        val activeColor = if (hasConnections) 0xFF2196F3.toInt() else 0xFF555555.toInt()
        val wifiColor = if (hasConnections) 0xFF4CAF50.toInt() else 0xFF555555.toInt()
        iconBluetooth.setColorFilter(activeColor)
        labelBluetooth.setTextColor(activeColor)
        iconWifi.setColorFilter(wifiColor)
        labelWifi.setTextColor(wifiColor)
    }

    // ── Permissions ───────────────────────────────────────────────────
    private fun requestMissingPermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_SCAN)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
