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
}
