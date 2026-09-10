package com.example.vaanisetu.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("VaaniSetuPrefs", Context.MODE_PRIVATE)

    fun saveUserName(name: String) {
        prefs.edit().putString("USER_NAME", name).apply()
    }

    fun getUserName(): String {
        return prefs.getString("USER_NAME", "User") ?: "User"
    }

    fun saveLanguage(langCode: String) {
        prefs.edit().putString("LANGUAGE_CODE", langCode).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("LANGUAGE_CODE", "en-IN") ?: "en-IN"
    }

    fun setOnboardingCompleted() {
        prefs.edit().putBoolean("ONBOARDING_COMPLETED", true).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("ONBOARDING_COMPLETED", false)
    }

    // ── Channel Tracking ─────────────────────────────────────────────
    fun updateChannelActivity(channelName: String, timestamp: Long = System.currentTimeMillis()) {
        if (channelName == "Global") return
        val map = getActiveChannelsMap().toMutableMap()
        map[channelName] = timestamp
        
        // Save as comma-separated: Name1:Time1,Name2:Time2
        val serialized = map.entries.joinToString(",") { "${it.key}:${it.value}" }
        prefs.edit().putString("ACTIVE_CHANNELS", serialized).apply()
    }

    fun getActiveChannels(): List<String> {
        val map = getActiveChannelsMap()
        val fiveMinsAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        
        // Filter out expired channels
        val validChannels = map.filter { it.value > fiveMinsAgo }.keys.toList()
        
        // Resave clean list
        val serialized = validChannels.joinToString(",") { "$it:${map[it]}" }
        prefs.edit().putString("ACTIVE_CHANNELS", serialized).apply()
        
        return validChannels
    }

    private fun getActiveChannelsMap(): Map<String, Long> {
        val serialized = prefs.getString("ACTIVE_CHANNELS", "") ?: ""
        if (serialized.isEmpty()) return emptyMap()
        
        val map = mutableMapOf<String, Long>()
        serialized.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                map[parts[0]] = parts[1].toLongOrNull() ?: 0L
            }
        }
        return map
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }

    fun purgeChannels() {
        prefs.edit().remove("ACTIVE_CHANNELS").apply()
    }
}
