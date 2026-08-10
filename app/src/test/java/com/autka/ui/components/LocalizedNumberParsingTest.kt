package com.autka.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.DecimalFormatSymbols
import java.util.Locale

class LocalizedNumberParsingTest {
    private val delta = 0.001

    @Test
    fun `Polish input accepts comma decimal and common grouping`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))

        assertEquals(20_000.50, parseLocalizedNonNegativeAmount("20 000,50", pl)!!, delta)
        assertEquals(20_000.50, parseLocalizedNonNegativeAmount("20.000,50", pl)!!, delta)
        assertEquals(20_000.0, parseLocalizedNonNegativeAmount("20.000", pl)!!, delta)
        assertEquals(20_000.50, parseLocalizedNonNegativeAmount("20000.50", pl)!!, delta)
    }

    @Test
    fun `whitespace grouping is validated instead of stripped blindly`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))

        assertEquals(20_000.0, parseLocalizedNonNegativeAmount("20 000", pl)!!, delta)
        assertEquals(20_000.0, parseLocalizedNonNegativeAmount("20\u00A0000", pl)!!, delta)
        assertNull(parseLocalizedNonNegativeAmount("20 00", pl))
    }

    @Test
    fun `US input rejects ambiguous grouping typos but accepts unambiguous foreign decimals`() {
        val us = DecimalFormatSymbols(Locale.US)

        assertEquals(20_000.50, parseLocalizedNonNegativeAmount("20,000.50", us)!!, delta)
        assertEquals(20_000.50, parseLocalizedNonNegativeAmount("20000,50", us)!!, delta)
        assertNull(parseLocalizedNonNegativeAmount("20,00", us))
        assertNull(parseLocalizedNonNegativeAmount("1,5", us))
    }

    @Test
    fun `Indian grouping is accepted without weakening separator consistency`() {
        val india = DecimalFormatSymbols(Locale.forLanguageTag("en-IN"))

        assertEquals(1_234_567.89, parseLocalizedNonNegativeAmount("12,34,567.89", india)!!, delta)
        assertEquals(12_345_678.0, parseLocalizedNonNegativeAmount("1,23,45,678", india)!!, delta)
    }

    @Test
    fun `Arabic digits and separators are normalized`() {
        val arabic = DecimalFormatSymbols(Locale.forLanguageTag("ar-EG"))

        assertEquals(20_000.50, parseLocalizedNonNegativeAmount("٢٠٬٠٠٠٫٥٠", arabic)!!, delta)
    }

    @Test
    fun `mixed foreign grouping is interpreted without changing magnitude`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))
        val us = DecimalFormatSymbols(Locale.US)

        assertEquals(1_234.56, parseLocalizedNonNegativeAmount("1,234.56", pl)!!, delta)
        assertEquals(1_234.56, parseLocalizedNonNegativeAmount("1.234,56", us)!!, delta)
    }

    @Test
    fun `locale decimal with unsupported precision is rejected instead of regrouped`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))
        val us = DecimalFormatSymbols(Locale.US)

        assertNull(parseLocalizedNonNegativeAmount("1,234", pl))
        assertNull(parseLocalizedNonNegativeAmount("1.234", us))
    }

    @Test
    fun `repeated locale decimal separators are rejected`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))
        val us = DecimalFormatSymbols(Locale.US)

        assertNull(parseLocalizedNonNegativeAmount("1,234,56", pl))
        assertNull(parseLocalizedNonNegativeAmount("1.234.56", us))
    }

    @Test
    fun `mixed grouping separators are rejected`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))
        val us = DecimalFormatSymbols(Locale.US)

        assertNull(parseLocalizedNonNegativeAmount("1,234.567", pl))
        assertNull(parseLocalizedNonNegativeAmount("1.234,567", us))
    }

    @Test
    fun `percentage input accepts either decimal mark but never grouping`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))
        val us = DecimalFormatSymbols(Locale.US)

        assertEquals(10.5, parseLocalizedPercentage("10,5", pl)!!, delta)
        assertEquals(10.5, parseLocalizedPercentage("10.5", pl)!!, delta)
        assertEquals(23.5, parseLocalizedPercentage("23,5", us)!!, delta)
        assertEquals(23.5, parseLocalizedPercentage("23.5", us)!!, delta)
        assertNull(parseLocalizedPercentage("0.100", pl))
        assertNull(parseLocalizedPercentage("0,100", us))
        assertNull(parseLocalizedPercentage("1 000", pl))
        assertNull(parseLocalizedPercentage("100.01", us))
    }

    @Test
    fun `percentage typing only treats a single trailing decimal mark as incomplete`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))

        assertTrue(isIncompleteLocalizedPercentage("", pl))
        assertTrue(isIncompleteLocalizedPercentage("10,", pl))
        assertTrue(isIncompleteLocalizedPercentage("10.", pl))
        assertFalse(isIncompleteLocalizedPercentage("12,345", pl))
        assertFalse(isIncompleteLocalizedPercentage("1,2,", pl))
        assertFalse(isIncompleteLocalizedPercentage("10x", pl))
    }

    @Test
    fun `only a valid numeric prefix can be incomplete`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))
        val us = DecimalFormatSymbols(Locale.US)
        val india = DecimalFormatSymbols(Locale.forLanguageTag("en-IN"))

        assertTrue(isIncompleteLocalizedAmount("", pl))
        assertTrue(isIncompleteLocalizedAmount("20,", pl))
        assertTrue(isIncompleteLocalizedAmount("20.", pl))
        assertTrue(isIncompleteLocalizedAmount("1.234.", pl))
        assertTrue(isIncompleteLocalizedAmount("1,234,", us))
        assertTrue(isIncompleteLocalizedAmount("12,34,", india))
        assertTrue(isIncompleteLocalizedAmount("1.234.5", pl))
        assertTrue(isIncompleteLocalizedAmount("1,234,5", us))
        assertTrue(isIncompleteLocalizedAmount("12,34,5", us))
        assertFalse(isIncompleteLocalizedAmount("1,2,", pl))
        assertFalse(isIncompleteLocalizedAmount("1.2.", us))
        assertFalse(isIncompleteLocalizedAmount("12x,", pl))
        assertFalse(isIncompleteLocalizedAmount("1,,", pl))
        assertFalse(isIncompleteLocalizedAmount("20,5", pl))
    }

    @Test
    fun `positive integer accepts locale grouping and rejects malformed groups`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))
        val us = DecimalFormatSymbols(Locale.US)
        val de = DecimalFormatSymbols(Locale.GERMANY)
        val arabic = DecimalFormatSymbols(Locale.forLanguageTag("ar-EG"))

        assertEquals(2000, parsePositiveInteger("2 000", pl))
        assertEquals(2000, parsePositiveInteger("2\u202F000", pl))
        assertEquals(2000, parsePositiveInteger("2,000", us))
        assertEquals(2000, parsePositiveInteger("2.000", de))
        assertEquals(2000, parsePositiveInteger("٢٬٠٠٠", arabic))
        assertNull(parsePositiveInteger("2 00", pl))
        assertNull(parsePositiveInteger("2,00", us))
        assertNull(parsePositiveInteger("2.000", us))
        assertNull(parsePositiveInteger("0", us))
    }

    @Test
    fun `malformed or exponent inputs are rejected`() {
        val pl = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"))

        assertNull(parseLocalizedNonNegativeAmount("12,34,56", pl))
        assertNull(parseLocalizedNonNegativeAmount("1e9", pl))
        assertNull(parseLocalizedNonNegativeAmount("-50", pl))
    }
}
