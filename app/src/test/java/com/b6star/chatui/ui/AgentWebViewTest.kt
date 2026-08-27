package com.b6star.chatui.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentWebViewTest {
    @Test
    fun normalizeResourceIndentation_replacesMalformedCompiledSpacePrefix() {
        val compiledResourceLine = ("\u0002" + "0") + "   private val apiKey: String"

        assertEquals(
            "    private val apiKey: String",
            normalizeResourceIndentation(compiledResourceLine)
        )
    }

    @Test
    fun normalizeResourceIndentation_replacesLiteralUnicodeSpaceEscapes() {
        val escapedResourceLine =
            "\\u0020\\u0020\\u0020\\u0020private val apiKey: String"

        assertEquals(
            "    private val apiKey: String",
            normalizeResourceIndentation(escapedResourceLine)
        )
    }
}
