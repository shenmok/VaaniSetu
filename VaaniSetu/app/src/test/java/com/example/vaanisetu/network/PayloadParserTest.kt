package com.example.vaanisetu.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayloadParserTest {

    @Test
    fun verifyEncoding_TS_1_1() {
        val payload = MessagePayload(
            sender = "Amit",
            channel = "Medical",
            langCode = "hi-IN",
            urgencyFlag = 1,
            text = "Help"
        )
        val expected = "Amit|Medical|hi-IN|1|Help"
        val actual = PayloadParser.encode(payload)
        assertEquals(expected, actual)
    }

    @Test
    fun verifyDecoding_TS_1_2() {
        val raw = "Priya|Global|en-IN|0|Safe"
        val decoded = PayloadParser.decode(raw)
        
        assertEquals("Priya", decoded?.sender)
        assertEquals("Global", decoded?.channel)
        assertEquals("en-IN", decoded?.langCode)
        assertEquals(0, decoded?.urgencyFlag)
        assertEquals("Safe", decoded?.text)
    }

    @Test
    fun verifyMalformedDataGracefulDrop_TS_1_3() {
        val malformed1 = "Priya|Global|en-IN" // Missing delimiters
        val malformed2 = "Priya|Global|en-IN|NaN|Safe" // Bad urgency flag
        
        assertNull(PayloadParser.decode(malformed1))
        assertNull(PayloadParser.decode(malformed2))
    }
}
