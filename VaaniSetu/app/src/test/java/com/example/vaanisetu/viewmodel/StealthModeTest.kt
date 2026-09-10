package com.example.vaanisetu.viewmodel

import android.os.VibrationEffect
import android.os.Vibrator
import com.example.vaanisetu.utils.SpeechManager
import com.example.vaanisetu.utils.StealthManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StealthModeTest {

    private lateinit var viewModel: WalkieTalkieViewModel
    private val mockSpeechManager: SpeechManager = mockk(relaxed = true)
    private val mockStealthManager: StealthManager = mockk(relaxed = true)
    private val mockVibrator: Vibrator = mockk(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(VibrationEffect::class)
        every { VibrationEffect.createOneShot(any(), any()) } returns mockk()

        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Mock stealth mode to be ACTIVE
        val stealthFlow = MutableStateFlow(true)
        every { mockStealthManager.isStealthModeActive } returns stealthFlow
        
        viewModel = WalkieTalkieViewModel(mockSpeechManager, mockStealthManager, mockVibrator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyStealthModeMuting_TS_4_2() {
        val normalMsg = IncomingMessage("Amit", "hi-IN", 0, "Hello there")

        viewModel.queueIncomingMessage(normalMsg)

        // Assert TTS was NEVER called
        verify(exactly = 0) { mockSpeechManager.speak(any(), any(), any(), any()) }
        
        // Assert Vibrator was called
        verify(exactly = 1) { mockVibrator.vibrate(any<VibrationEffect>()) }
    }
}
