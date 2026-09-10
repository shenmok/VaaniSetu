package com.example.vaanisetu.network

data class MessagePayload(
    val sender: String,
    val channel: String,
    val langCode: String,
    val urgencyFlag: Int,
    val text: String
)

object PayloadParser {
    fun encode(payload: MessagePayload): String {
        return payload.sender + "|" + payload.channel + "|" + payload.langCode + "|" + payload.urgencyFlag + "|" + payload.text
    }

    fun decode(raw: String): MessagePayload? {
        val parts = raw.split("|", limit = 5)
        if (parts.size != 5) return null
        
        val urgency = parts[3].toIntOrNull() ?: return null
        
        return MessagePayload(
            sender = parts[0],
            channel = parts[1],
            langCode = parts[2],
            urgencyFlag = urgency,
            text = parts[4]
        )
    }
}
