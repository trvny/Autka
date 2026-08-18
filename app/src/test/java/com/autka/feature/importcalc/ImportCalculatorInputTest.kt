package com.autka.feature.importcalc

import com.autka.core.model.FuelType
import java.text.DecimalFormatSymbols
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportCalculatorInputTest {
    private val us = DecimalFormatSymbols(Locale.US)

    @Test
    fun `valid defaults produce a current estimate`() {
        val state = evaluate(
            vehiclePrice = "20000",
            shipping = "2400",
            customs = "10",
            vat = "23",
            engine = "1800",
        )

        assertFalse(state.vehiclePriceInvalid)
        assertFalse(state.shippingInvalid)
        assertFalse(state.customsRateInvalid)
        assertFalse(state.vatRateInvalid)
        assertTrue(state.assumptionsReady)
        assertTrue(state.assumptionsAreDefault)
        assertTrue(state.engineRequired)
        assertFalse(state.engineInvalid)
        assertNotNull(state.estimate)
    }

    @Test
    fun `partial decimal stays editable without becoming an error`() {
        val state = evaluate(vehiclePrice = "20000.")

        assertFalse(state.vehiclePriceInvalid)
        assertNull(state.estimate)
    }

    @Test
    fun `blank required amount is invalid even while blank is a typing prefix`() {
        val state = evaluate(vehiclePrice = "")

        assertTrue(state.vehiclePriceInvalid)
        assertNull(state.estimate)
    }

    @Test
    fun `custom assumptions stay ready but are no longer defaults`() {
        val state = evaluate(shipping = "2500")

        assertTrue(state.assumptionsReady)
        assertFalse(state.assumptionsAreDefault)
        assertNotNull(state.estimate)
    }

    @Test
    fun `blank combustion engine keeps the conservative estimate`() {
        val state = evaluate(engine = "")

        assertTrue(state.engineRequired)
        assertFalse(state.engineInvalid)
        assertTrue(state.estimate?.usesConservativeExcise == true)
    }

    @Test
    fun `invalid combustion engine blocks a new estimate`() {
        val state = evaluate(engine = "nope")

        assertTrue(state.engineInvalid)
        assertNull(state.estimate)
    }

    @Test
    fun `electric ignores stale engine text`() {
        val state = evaluate(engine = "nope", fuel = FuelType.ELECTRIC)

        assertFalse(state.engineRequired)
        assertFalse(state.engineInvalid)
        assertNotNull(state.estimate)
        assertTrue(state.estimate?.exciseDuty?.amount == 0.0)
    }

    private fun evaluate(
        vehiclePrice: String = "20000",
        shipping: String = "2400",
        customs: String = "10",
        vat: String = "23",
        engine: String = "1800",
        fuel: FuelType = FuelType.PETROL,
    ) = evaluateImportCalculatorInput(
        fields = ImportCalculatorFields(
            vehiclePriceText = vehiclePrice,
            shippingText = shipping,
            customsRateText = customs,
            vatRateText = vat,
            engineText = engine,
            fuel = fuel,
        ),
        numberSymbols = us,
    )
}
