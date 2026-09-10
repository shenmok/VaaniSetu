package com.example.vaanisetu.viewmodel

import com.example.vaanisetu.utils.SpeechManager
import com.example.vaanisetu.utils.StealthManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyOverrideTest {

    private lateinit var viewModel: WalkieTalkieViewModel
    private val mockSpeechManager: SpeechManager = mockk(relaxed = true)
    private val mockStealthManager: StealthManager = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Mock stealth mode to be ACTIVE, proving emergency overrides it
        val stealthFlow = MutableStateFlow(true)
        every { mockStealthManager.isStealthModeActive } returns stealthFlow
        
        viewModel = WalkieTalkieViewModel(mockSpeechManager, mockStealthManager, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyEmergencyBypass_TS_4_1() = runTest {
        val emergencyMsg = IncomingMessage("Priya", "en-IN", 1, "Send ambulance")

        var receivedEvent: IncomingMessage? = null
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) { 
            receivedEvent = viewModel.emergencyEvent.first() 
        }

        viewModel.queueIncomingMessage(emergencyMsg)

        job.join()
        
        // Assert event emitted to UI
        assertEquals("Send ambulance", receivedEvent?.text)
        assertEquals(1, receivedEvent?.urgencyFlag)

        // Assert TTS was called DESPITE stealth mode being active
        verify(exactly = 1) { mockSpeechManager.speak("Send ambulance", "Priya", "en-IN", 1) }
    }
}
