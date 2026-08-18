package com.autka.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceHealthCachePolicyTest {
    @Test
    fun `cache freshness expires after five minutes`() {
        val storedAt = 1_000L

        assertTrue(isSourceHealthCacheFresh(storedAt, storedAt))
        assertTrue(isSourceHealthCacheFresh(storedAt, storedAt + SOURCE_HEALTH_CACHE_TTL_MS))
        assertFalse(isSourceHealthCacheFresh(storedAt, storedAt + SOURCE_HEALTH_CACHE_TTL_MS + 1))
        assertFalse(isSourceHealthCacheFresh(storedAt, storedAt - 1))
    }
}
