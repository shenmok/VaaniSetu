package com.example.vaanisetu.viewmodel

import com.example.vaanisetu.network.MessagePayload
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelRouterTest {

    private lateinit var viewModel: WalkieTalkieViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = WalkieTalkieViewModel(mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyChannelFiltering_TS_2_1() {
        // Subscribe to Medical
        viewModel.switchChannel("Medical")

        val msgMedical = MessagePayload("Amit", "Medical", "hi-IN", 0, "Need bandages")
        val msgRescue = MessagePayload("Priya", "Rescue", "en-IN", 0, "On my way")

        viewModel.processNetworkPayload(msgMedical)
        viewModel.processNetworkPayload(msgRescue)

        // Only Medical should be in the queue
        assertEquals(1, viewModel.ttsQueue.size)
        assertEquals("Need bandages", viewModel.ttsQueue[0].text)
        assertEquals("Medical", msgMedical.channel)
    }

    @Test
    fun verifyGlobalAutoJoin_TS_2_2() {
        // Subscribe to something else
        viewModel.switchChannel("Rescue")

        val msgGlobal = MessagePayload("Admin", "Global", "en-IN", 1, "Attention everyone")

        viewModel.processNetworkPayload(msgGlobal)

        // Global message should bypass the 'Rescue' filter and be emitted
        assertEquals(1, viewModel.ttsQueue.size)
        assertEquals("Attention everyone", viewModel.ttsQueue[0].text)
    }
}
