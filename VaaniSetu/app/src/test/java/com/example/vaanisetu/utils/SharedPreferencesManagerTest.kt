package com.example.vaanisetu.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SharedPreferencesManagerTest {

    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockContext = mockk()
        mockPrefs = mockk()
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences("VaaniSetuPrefs", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        
        sharedPreferencesManager = SharedPreferencesManager(mockContext)
    }

    @Test
    fun verifyUserNameIsSavedProperly_TS_5_2() {
        val testName = "Rajan Mehta"
        every { mockPrefs.getString("USER_NAME", "User") } returns testName

        sharedPreferencesManager.saveUserName(testName)
        verify { mockEditor.putString("USER_NAME", testName) }

        val retrievedName = sharedPreferencesManager.getUserName()
        assertEquals(testName, retrievedName)
    }

    @Test
    fun verifyLanguageCodeIsSaved() {
        val testLang = "mr-IN"
        every { mockPrefs.getString("LANGUAGE_CODE", "en-IN") } returns testLang

        sharedPreferencesManager.saveLanguage(testLang)
        verify { mockEditor.putString("LANGUAGE_CODE", testLang) }

        val retrievedLang = sharedPreferencesManager.getLanguage()
        assertEquals(testLang, retrievedLang)
    }

    @Test
    fun verifyOnboardingCompletionFlag() {
        every { mockPrefs.getBoolean("ONBOARDING_COMPLETED", false) } returns true

        sharedPreferencesManager.setOnboardingCompleted()
        verify { mockEditor.putBoolean("ONBOARDING_COMPLETED", true) }

        assertTrue(sharedPreferencesManager.isOnboardingCompleted())
    }
}
