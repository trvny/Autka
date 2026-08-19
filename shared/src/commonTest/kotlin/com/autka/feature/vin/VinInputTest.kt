package com.autka.feature.vin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VinInputTest {
    @Test
    fun `input is normalized and capped at 17 characters`() {
        assertEquals("1M8GDM9AXKP042788", VinInput.normalize("1m8g-dm9a xkp042788-extra"))
        assertEquals("1M8GDM9AXKP042788", VinInput.normalize("Ł1m8g-dm9a xkp042788"))
    }

    @Test
    fun `validation rejects wrong length and forbidden letters`() {
        assertFalse(VinInput.isValid("1M8GDM9AXKP04278"))
        assertFalse(VinInput.isValid("1M8GDM9AXIP042788"))
        assertTrue(VinInput.isValid("1M8GDM9AXKP042788"))
    }
}
