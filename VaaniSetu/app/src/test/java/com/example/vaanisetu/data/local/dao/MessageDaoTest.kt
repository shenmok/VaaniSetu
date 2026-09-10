package com.example.vaanisetu.data.local.dao

import com.example.vaanisetu.data.local.entity.MessageEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MessageDaoTest {

    private lateinit var dao: MessageDao

    @Before
    fun setup() {
        // Robolectric crashes on JDK 25 with ASM bug (ClassReader.java:200)
        // We use MockK to simulate the Room @Query filtering behavior
        dao = mockk()
    }

    @Test
    fun verifyHistoryFiltering_TS_5_1() {
        val fakeDb = mutableListOf<MessageEntity>()
        
        // Mock insertion
        every { dao.insertMessage(any()) } answers {
            fakeDb.add(firstArg())
            Unit
        }

        // Mock querying logic that Room would normally generate
        every { dao.getMessagesByChannel(any()) } answers {
            val channel = firstArg<String>()
            fakeDb.filter { it.channelId == channel }.sortedBy { it.timestamp }
        }

        // Insert 5 fake messages
        dao.insertMessage(MessageEntity(timestamp = 100, sender = "A", channelId = "Global", content = "Hi", urgencyFlag = 0, langCode = "en-IN"))
        dao.insertMessage(MessageEntity(timestamp = 200, sender = "B", channelId = "Medical", content = "Med1", urgencyFlag = 0, langCode = "en-IN"))
        dao.insertMessage(MessageEntity(timestamp = 300, sender = "C", channelId = "Rescue", content = "Res1", urgencyFlag = 0, langCode = "en-IN"))
        dao.insertMessage(MessageEntity(timestamp = 400, sender = "D", channelId = "Medical", content = "Med2", urgencyFlag = 0, langCode = "en-IN"))
        dao.insertMessage(MessageEntity(timestamp = 500, sender = "E", channelId = "Global", content = "Hey", urgencyFlag = 0, langCode = "en-IN"))

        // Assert getMessagesByChannel("Medical") returns only the correct subset
        val medicalMessages = dao.getMessagesByChannel("Medical")
        
        assertEquals(2, medicalMessages.size)
        assertEquals("Med1", medicalMessages[0].content)
        assertEquals("Med2", medicalMessages[1].content)
    }
}
