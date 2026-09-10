package com.example.vaanisetu.viewmodel

import com.example.vaanisetu.utils.SpeechManager
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalkieTalkieViewModelTest {

    private lateinit var viewModel: WalkieTalkieViewModel
    private val mockSpeechManager: SpeechManager = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = WalkieTalkieViewModel(mockSpeechManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyPttStateAndHaptics_TS_3_1() = runTest {
        // Assert initial state
        assertEquals(PttState.IDLE, viewModel.pttState.value)

        // Trigger PTT
        viewModel.onPttPressed("en-IN")

        // Verify state is LISTENING
        assertEquals(PttState.LISTENING, viewModel.pttState.value)
        
        // Verify startListening called
        verify(exactly = 1) { mockSpeechManager.startListening("en-IN") }

        // Trigger release
        viewModel.onPttReleased()
        assertEquals(PttState.IDLE, viewModel.pttState.value)
        verify(exactly = 1) { mockSpeechManager.stopListening() }
    }

    @Test
    fun verifyMessageQueueing_TS_3_2() {
        val msg1 = IncomingMessage("Amit", "hi-IN", 0, "Hello")
        val msg2 = IncomingMessage("Priya", "en-IN", 1, "Emergency")

        // Inject sequentially at T=0ms equivalent
        viewModel.queueIncomingMessage(msg1)
        viewModel.queueIncomingMessage(msg2)

        // Assert both messages pushed to TTS queue
        assertEquals(2, viewModel.ttsQueue.size)
        assertEquals("Amit", viewModel.ttsQueue[0].sender)
        assertEquals("Priya", viewModel.ttsQueue[1].sender)

        // Verify SpeechManager called speak for both
        verify(exactly = 1) { mockSpeechManager.speak("Hello", "Amit", "hi-IN", 0) }
        verify(exactly = 1) { mockSpeechManager.speak("Emergency", "Priya", "en-IN", 1) }
    }
}
