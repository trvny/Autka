package com.autka.data.remote.vin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VpicDecoderTest {
    @Test
    fun `decodeJson maps and cleans vPIC values`() {
        val result = VpicDecoder.decodeJson(
            payload = """
                {
                  "Results": [{
                    "VIN": " 1HGCM82633A004352 ",
                    "Make": " HONDA ",
                    "Model": "Accord",
                    "ModelYear": "2003",
                    "FuelTypePrimary": "Gasoline",
                    "ErrorCode": "0, 0",
                    "ErrorText": "should be ignored",
                    "UnusedField": "ignored"
                  }]
                }
            """.trimIndent(),
            fallbackVin = "fallback",
        )

        assertEquals("1HGCM82633A004352", result.vin)
        assertEquals("HONDA", result.make)
        assertEquals("Accord", result.model)
        assertEquals("2003", result.modelYear)
        assertEquals("Gasoline", result.fuelType)
        assertNull(result.decoderWarning)
    }

    @Test
    fun `decodeJson preserves vPIC warning`() {
        val result = VpicDecoder.decodeJson(
            payload = """
                {"Results":[{"VIN":"TEST","ErrorCode":"1","ErrorText":"Invalid VIN"}]}
            """.trimIndent(),
            fallbackVin = "fallback",
        )

        assertEquals("Invalid VIN", result.decoderWarning)
    }

    @Test
    fun `decode rejects empty results`() {
        assertFailsWith<IllegalStateException> {
            VpicDecoder.decodeJson("{\"Results\":[]}", fallbackVin = "TEST")
        }
    }

    @Test
    fun `safe decode returns null for unusable payloads`() {
        assertNull(VpicDecoder.decodeJsonOrNull("not-json", fallbackVin = "TEST"))
        assertNull(VpicDecoder.decodeJsonOrNull("{\"Results\":[]}", fallbackVin = "TEST"))
    }
}
