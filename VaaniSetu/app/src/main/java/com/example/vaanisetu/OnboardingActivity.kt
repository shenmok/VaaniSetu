package com.example.vaanisetu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.vaanisetu.utils.SharedPreferencesManager
import java.util.Locale

class OnboardingActivity : AppCompatActivity() {

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var nameInput: EditText
    private lateinit var languageSpinner: Spinner
    private lateinit var startButton: Button
    private lateinit var micButton: ImageButton

    // Ordered list of language names to display in the spinner
    private val languageDisplayNames = arrayOf("English", "Hindi", "Marathi")
    // Corresponding BCP-47 language codes required by Phase 1 limits
    private val languageCodes = arrayOf("en-IN", "hi-IN", "mr-IN")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true
        permissions.entries.forEach {
            if (!it.value) allGranted = false
        }
        if (!allGranted) {
            Toast.makeText(this, "Core permissions required for Walkie-Talkie.", Toast.LENGTH_LONG).show()
        }
    }

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = SharedPreferencesManager(this)

        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        // Route to Home if already onboarded (unless in edit mode)
        if (prefsManager.isOnboardingCompleted() && !isEditMode) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_onboarding)

        nameInput = findViewById(R.id.nameInput)
        languageSpinner = findViewById(R.id.languageSpinner)
        startButton = findViewById(R.id.startButton)
        micButton = findViewById(R.id.micButton)

        setupSpinner()
        requestCorePermissions()

        // Pre-fill in edit mode
        if (isEditMode) {
            nameInput.setText(prefsManager.getUserName())
        }

        startButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = "Please enter your name"
                return@setOnClickListener
            }

            val selectedIndex = languageSpinner.selectedItemPosition
            val selectedLangCode = languageCodes[selectedIndex]

            prefsManager.saveUserName(name)
            prefsManager.saveLanguage(selectedLangCode)
            prefsManager.setOnboardingCompleted()

            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        micButton.setOnClickListener {
            // Speech Recognizer logic goes here (to be implemented fully in Phase 1.5)
            Toast.makeText(this, "Speech to Text triggered (Phase 1.5)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageDisplayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        // System language detection for default selection
        val defaultLang = Locale.getDefault().language
        val defaultIndex = when (defaultLang) {
            "hi" -> 1
            "mr" -> 2
            else -> 0
        }
        languageSpinner.setSelection(defaultIndex)
    }

    private fun requestCorePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
