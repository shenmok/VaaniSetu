package com.example.vaanisetu.viewmodel

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaanisetu.utils.SpeechManager
import com.example.vaanisetu.utils.StealthManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PttState { IDLE, LISTENING }

data class IncomingMessage(
    val sender: String, 
    val langCode: String, 
    val urgencyFlag: Int, 
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val id: Long = 0
)

class WalkieTalkieViewModel(
    private val speechManager: SpeechManager?,
    private val stealthManager: StealthManager? = null,
    private val vibrator: Vibrator? = null,
    private val messageDao: com.example.vaanisetu.data.local.dao.MessageDao? = null
) : ViewModel() {

    private val _pttState = MutableStateFlow(PttState.IDLE)
    val pttState: StateFlow<PttState> = _pttState.asStateFlow()

    private val _hapticEvent = MutableSharedFlow<Boolean>()
    val hapticEvent: SharedFlow<Boolean> = _hapticEvent.asSharedFlow()

    private val _emergencyEvent = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 1)
    val emergencyEvent: SharedFlow<IncomingMessage> = _emergencyEvent.asSharedFlow()

    // For test verification
    val ttsQueue = mutableListOf<IncomingMessage>()

    init {
        switchChannel("Global")
    }

    fun onPttPressed(langCode: String) {
        _pttState.value = PttState.LISTENING
        viewModelScope.launch {
            _hapticEvent.emit(true)
        }
        speechManager?.startListening(langCode)
    }

    fun onPttReleased() {
        _pttState.value = PttState.IDLE
        speechManager?.stopListening()
    }

    // ── Channel Logic ────────────────────────────────────────────────
    private val _currentChannel = MutableStateFlow("Global")
    val currentChannel: StateFlow<String> = _currentChannel.asStateFlow()

    private val _channelMessages = MutableStateFlow<List<IncomingMessage>>(emptyList())
    val channelMessages: StateFlow<List<IncomingMessage>> = _channelMessages.asStateFlow()

    private val _liveSpeechText = MutableStateFlow("")
    val liveSpeechText: StateFlow<String> = _liveSpeechText.asStateFlow()

    // Tracks last message timestamp to concatenate if < 30s
    private var lastOwnMessageTime = 0L

    fun switchChannel(newChannel: String) {
        _currentChannel.value = newChannel
        
        // Load history from DB
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val history = messageDao?.getMessagesByChannel(newChannel) ?: emptyList()
            val mapped = history.map { 
                IncomingMessage(it.sender, it.langCode, it.urgencyFlag, it.content, it.timestamp, it.id) 
            }
            _channelMessages.value = mapped
        }
    }

    fun processNetworkPayload(payload: com.example.vaanisetu.network.MessagePayload) {
        // TS-2.1 & TS-2.2: Channel Filtering and Global Auto-Join
        if (payload.channel.equals(_currentChannel.value, ignoreCase = true) || 
            payload.channel.equals("Global", ignoreCase = true) ||
            payload.urgencyFlag == 1) { // Emergency always bypasses filters
            
            val incoming = IncomingMessage(
                sender = payload.sender,
                langCode = payload.langCode,
                urgencyFlag = payload.urgencyFlag,
                text = payload.text
            )
            queueIncomingMessage(incoming)
        }
    }

    fun updateLiveSpeechText(text: String) {
        _liveSpeechText.value = text
    }

    fun queueIncomingMessage(message: IncomingMessage, isOwnMessage: Boolean = false) {
        ttsQueue.add(message)
        
        val currentMessages = _channelMessages.value.toMutableList()
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val msgTimeStr = sdf.format(java.util.Date(message.timestamp))
        
        var concatenated = false
        var updatedMsgId = 0L
        var combinedText = ""
        
        if (currentMessages.isNotEmpty()) {
            val lastMsg = currentMessages.last()
            val lastTimeStr = sdf.format(java.util.Date(lastMsg.timestamp))
            
            if (lastMsg.sender == message.sender && lastTimeStr == msgTimeStr) {
                // Same sender, same minute -> Concatenate!
                combinedText = lastMsg.text + " " + message.text
                val updatedMsg = lastMsg.copy(text = combinedText)
                currentMessages[currentMessages.lastIndex] = updatedMsg
                _channelMessages.value = currentMessages
                concatenated = true
                updatedMsgId = updatedMsg.id
            }
        }
        
        if (!concatenated) {
            _channelMessages.value = currentMessages + message
        }

        // Persist to Room DB
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (concatenated && updatedMsgId != 0L) {
                // Update existing row
                messageDao?.updateMessageContent(updatedMsgId, combinedText)
            } else {
                // Insert new row
                val newId = messageDao?.insertMessage(
                    com.example.vaanisetu.data.local.entity.MessageEntity(
                        timestamp = message.timestamp,
                        sender = message.sender,
                        channelId = _currentChannel.value,
                        content = message.text,
                        urgencyFlag = message.urgencyFlag,
                        langCode = message.langCode
                    )
                ) ?: 0L
                
                // Update the memory object with the actual DB id so future concatenations work
                if (newId != 0L) {
                    val msgs = _channelMessages.value.toMutableList()
                    val last = msgs.last()
                    if (last.timestamp == message.timestamp) {
                        msgs[msgs.lastIndex] = last.copy(id = newId)
                        _channelMessages.value = msgs
                    }
                }
            }
            // Enforce max 10MB (approx 10000 rows) history limit
            messageDao?.enforceHistoryLimit()
        }
        
        if (message.urgencyFlag == 1) {
            viewModelScope.launch { _emergencyEvent.emit(message) }
            if (!isOwnMessage) {
                speechManager?.speak(message.text, message.sender, message.langCode, message.urgencyFlag)
            }
        } else {
            if (stealthManager?.isStealthModeActive?.value == true) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else if (!isOwnMessage) {
                speechManager?.speak(message.text, message.sender, message.langCode, message.urgencyFlag)
            }
        }
    }
}
