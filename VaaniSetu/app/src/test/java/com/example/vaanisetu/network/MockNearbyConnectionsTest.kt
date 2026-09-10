package com.example.vaanisetu.network

import android.content.Context
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class MockNearbyConnectionsTest {

    private lateinit var manager: NearbyConnectionsManager
    private lateinit var mockContext: Context
    private lateinit var mockClient: ConnectionsClient

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockClient = mockk(relaxed = true)
        
        // Use reflection or constructor injection to bypass Nearby.getConnectionsClient(context)
        // Since NearbyConnectionsManager calls Nearby.getConnectionsClient directly in init,
        // we might need to mock static, but for simplicity we will pass mockClient via constructor or reflection.
        // Actually, let's just make connectionsClient a var or visible for testing.
    }

    @Test
    fun verifyMockTransmission_TS_4_3() {
        val manager = NearbyConnectionsManager(mockContext, "TestUser", clientForTesting = mockClient)

        // Add a fake endpoint
        manager.connectedEndpoints.add("endpoint123")

        val message = MessagePayload("Amit", "Global", "en-IN", 0, "Test")
        manager.broadcastMessage(message)

        val payloadSlot = slot<Payload>()
        val endpointsSlot = slot<List<String>>()

        verify(exactly = 1) { mockClient.sendPayload(capture(endpointsSlot), capture(payloadSlot)) }

        assertEquals(1, endpointsSlot.captured.size)
        assertEquals("endpoint123", endpointsSlot.captured[0])
        
        val sentBytes = payloadSlot.captured.asBytes()
        val sentString = String(sentBytes!!, StandardCharsets.UTF_8)
        assertEquals("Amit|Global|en-IN|0|Test", sentString)
    }
}
