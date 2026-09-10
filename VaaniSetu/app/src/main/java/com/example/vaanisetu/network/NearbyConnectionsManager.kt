package com.example.vaanisetu.network

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

import android.annotation.SuppressLint

@SuppressLint("StaticFieldLeak")
object NearbyConnectionsManager {

    private var connectionsClient: ConnectionsClient? = null
    val connectedEndpoints = mutableSetOf<String>()
    private val peerNames = mutableMapOf<String, String>()
    private var localUserName: String = ""
    private var serviceId: String = "com.example.vaanisetu.SERVICE_ID"

    fun init(context: Context, userName: String, clientForTesting: ConnectionsClient? = null) {
        if (connectionsClient == null) {
            connectionsClient = clientForTesting ?: Nearby.getConnectionsClient(context.applicationContext)
            localUserName = userName
        }
    }

    fun getPeerName(endpointId: String): String? = peerNames[endpointId]

    private val _peerCount = MutableStateFlow(0)
    val peerCount: StateFlow<Int> = _peerCount.asStateFlow()

    private val _incomingPayloads = MutableSharedFlow<MessagePayload>(extraBufferCapacity = 10)
    val incomingPayloads: SharedFlow<MessagePayload> = _incomingPayloads.asSharedFlow()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val rawString = String(bytes, StandardCharsets.UTF_8)
                    PayloadParser.decode(rawString)?.let { messagePayload ->
                        _incomingPayloads.tryEmit(messagePayload)
                    }
                }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Store peer's display name from handshake
            peerNames[endpointId] = connectionInfo.endpointName
            // Auto-accept connection in P2P Cluster
            connectionsClient?.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
                _peerCount.value = connectedEndpoints.size
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            _peerCount.value = connectedEndpoints.size
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // Ignore ourselves if we pick up our own broadcast
            if (info.endpointName != localUserName) {
                // Found a valid peer, request connection
                connectionsClient?.requestConnection(localUserName, endpointId, connectionLifecycleCallback)
            }
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient?.startAdvertising(localUserName, serviceId, connectionLifecycleCallback, options)
            ?.addOnSuccessListener { Log.d("Nearby", "Advertising started") }
            ?.addOnFailureListener { Log.e("Nearby", "Advertising failed", it) }
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient?.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            ?.addOnSuccessListener { Log.d("Nearby", "Discovery started") }
            ?.addOnFailureListener { Log.e("Nearby", "Discovery failed", it) }
    }

    fun stopAll() {
        connectionsClient?.stopAdvertising()
        connectionsClient?.stopDiscovery()
        connectionsClient?.stopAllEndpoints()
        connectedEndpoints.clear()
        _peerCount.value = 0
    }

    fun broadcastMessage(messagePayload: MessagePayload) {
        if (connectedEndpoints.isEmpty()) return
        val rawString = PayloadParser.encode(messagePayload)
        val payload = Payload.fromBytes(rawString.toByteArray(StandardCharsets.UTF_8))
        connectionsClient?.sendPayload(connectedEndpoints.toList(), payload)
    }

    // DEBUG: Simulate a peer connection and incoming messages
    fun simulateFakePeerAndMessage() {
        val fakeEndpointId = "sim_alom_${System.currentTimeMillis()}"
        connectedEndpoints.add(fakeEndpointId)
        peerNames[fakeEndpointId] = "Alom"
        _peerCount.value = connectedEndpoints.size
        
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            // "says ... after 5 seconds"
            kotlinx.coroutines.delay(5000)
            _incomingPayloads.emit(MessagePayload(
                sender = "Alom",
                channel = "Global",
                langCode = "hi-IN",
                urgencyFlag = 0,
                text = "lift me aag lagi he"
            ))
            
            // "<pause of 2 second> 2nd floor pe hun me"
            kotlinx.coroutines.delay(2000)
            _incomingPayloads.emit(MessagePayload(
                sender = "Alom",
                channel = "Global",
                langCode = "hi-IN",
                urgencyFlag = 0,
                text = "2nd floor pe hun me"
            ))
            
            // "after 5 seconds, bachao then disconnects"
            kotlinx.coroutines.delay(5000)
            _incomingPayloads.emit(MessagePayload(
                sender = "Alom",
                channel = "Global",
                langCode = "hi-IN",
                urgencyFlag = 1,
                text = "bachao"
            ))
            
            kotlinx.coroutines.delay(1000)
            connectedEndpoints.remove(fakeEndpointId)
            _peerCount.value = connectedEndpoints.size
        }
    }
}
