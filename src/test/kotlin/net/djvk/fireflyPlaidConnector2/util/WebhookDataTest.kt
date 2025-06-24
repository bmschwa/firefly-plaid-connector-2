package net.djvk.fireflyPlaidConnector2.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


import net.djvk.fireflyPlaidConnector2.constants.IntervalMilliSecs

class WebhookDataTest {

    private val json = Json

    @Test
    fun `serialize WebhookData to JSON`() {
        val data = WebhookData(IntervalMilliSecs(1500))
        val jsonString = json.encodeToString(data)

        assertEquals("""{"loopDurationMs":1500}""", jsonString)
    }

    @Test
    fun `deserialize JSON to WebhookData`() {
        val jsonString = """{"loopDurationMs":1500}"""
        val data = json.decodeFromString<WebhookData>(jsonString)

        assertEquals(WebhookData(IntervalMilliSecs(1500)), data)
    }

    @Test
    fun `round-trip serialize and deserialize`() {
        val original = WebhookData(IntervalMilliSecs(3000))
        val jsonString = json.encodeToString(original)
        val restored = json.decodeFromString<WebhookData>(jsonString)

        assertEquals(original, restored)
    }
}