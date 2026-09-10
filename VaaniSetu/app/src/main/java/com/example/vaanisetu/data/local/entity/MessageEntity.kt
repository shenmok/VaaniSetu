package com.example.vaanisetu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sender: String,
    val channelId: String,
    val content: String,
    val urgencyFlag: Int,
    val langCode: String,
    val isRead: Boolean = false
)
