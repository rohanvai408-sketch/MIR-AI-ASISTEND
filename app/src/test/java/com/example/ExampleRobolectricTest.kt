package com.example

import com.example.session.AssistantState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MirUnitTest {

    @Test
    fun `verify assistant states have sassy labels and quotes`() {
        assertEquals("Standing By", AssistantState.IDLE.label)
        assertEquals("Listening", AssistantState.LISTENING.label)
        assertEquals("Processing", AssistantState.THINKING.label)
        assertEquals("Speaking", AssistantState.SPEAKING.label)
        assertEquals("Executing", AssistantState.EXECUTING_TOOL.label)
        assertEquals("Attention", AssistantState.ERROR.label)
        assertTrue(AssistantState.IDLE.quote.contains("Mir"))
        assertTrue(AssistantState.SPEAKING.quote.contains("brilliant"))
    }

    @Test
    fun `verify state quote non-empty for all states`() {
        AssistantState.values().forEach { state ->
            assertTrue(state.label.isNotBlank())
            assertTrue(state.quote.isNotBlank())
        }
    }

    @Test
    fun `verify help WhatsApp number is 7699935408`() {
        assertEquals("7699935408", com.example.ui.components.HELP_WHATSAPP_NUMBER)
    }
}
