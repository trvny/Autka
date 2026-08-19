package com.autka.data.remote.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceHealthDecoderTest {
    @Test
    fun `decodeJson maps source health and ignores unknown fields`() {
        val sources = SourceHealthDecoder.decodeJson(
            """
            {
              "sources": [{
                "id": "mock",
                "displayName": "Mock",
                "enabled": true,
                "offerCount": 3,
                "lastCompletedAtEpochMs": 1234,
                "lastCompletedOk": true,
                "lastOffersUpserted": 2,
                "futureField": "ignored"
              }]
            }
            """.trimIndent(),
        )

        val source = sources.single()
        assertEquals("mock", source.id)
        assertEquals("Mock", source.displayName)
        assertTrue(source.enabled)
        assertEquals(3, source.offerCount)
        assertEquals(1234L, source.lastCompletedAtEpochMs)
        assertTrue(source.lastCompletedOk == true)
        assertEquals(2, source.lastOffersUpserted)
    }

    @Test
    fun `decodeJson preserves disabled source with nullable health`() {
        val source = SourceHealthDecoder.decodeJson(
            """{"sources":[{"id":"otomoto","displayName":"Otomoto","enabled":false}]}""",
        ).single()

        assertFalse(source.enabled)
        assertNull(source.offerCount)
        assertNull(source.lastCompletedAtEpochMs)
        assertNull(source.lastCompletedOk)
        assertNull(source.lastOffersUpserted)
    }

    @Test
    fun `safe decoder distinguishes malformed payload from empty source list`() {
        assertNotNull(SourceHealthDecoder.decodeJsonOrNull("{\"sources\":[]}"))
        assertNull(SourceHealthDecoder.decodeJsonOrNull("not json"))
    }
}
