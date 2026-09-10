package com.example.vaanisetu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.vaanisetu.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesByChannel(channelId: String): List<MessageEntity>

    @Query("UPDATE messages SET content = :newContent WHERE id = :id")
    fun updateMessageContent(id: Long, newContent: String)

    @Query("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY timestamp DESC LIMIT 10000)")
    fun enforceHistoryLimit()

    @Query("SELECT channelId FROM messages WHERE channelId != 'Global' GROUP BY channelId HAVING MAX(timestamp) > :timeThreshold")
    fun getActiveChannels(timeThreshold: Long): List<String>
}
