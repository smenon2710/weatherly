package com.example.weatherly.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatErrorMessageTest {

    @Test fun `401 and 402 point to Settings, never to a field above`() {
        for (code in listOf(401, 402)) for (ownKey in listOf(true, false)) {
            val msg = ChatRepository.httpMessage(code, ownKey)
            assertTrue(msg, msg.contains("Settings"))
            assertFalse(msg, msg.contains("above"))
        }
    }

    @Test fun `shared-key users are never told about their own OpenRouter account`() {
        for (code in listOf(401, 402, 429, 500)) {
            assertFalse(ChatRepository.httpMessage(code, ownKey = false).contains("your OpenRouter account"))
        }
    }

    @Test fun `own-key users are told their key or account is the problem`() {
        assertTrue(ChatRepository.httpMessage(401, ownKey = true).contains("Your OpenRouter key"))
        assertTrue(ChatRepository.httpMessage(402, ownKey = true).contains("your OpenRouter account"))
    }
}
