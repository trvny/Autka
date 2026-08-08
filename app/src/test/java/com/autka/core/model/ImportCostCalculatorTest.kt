package com.autka.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the landed-cost math down. These figures are the product's whole reason to
 * exist, so the chain (duty -> excise on top of duty -> VAT on top of both) and the
 * excise rate selection must not drift silently.
 */
class ImportCostCalculatorTest {

    private val delta = 0.001

    @Test
    fun `excise rate is 3_1 percent up to and including 2_0L`() {
        assertEquals(0.031, ImportCostCalculator.exciseRate(1600), delta)
        assertEquals(0.031, ImportCostCalculator.exciseRate(2000), delta)
    }

    @Test
    fun `excise rate jumps to 18_6 percent above 2_0L`() {
        assertEquals(0.186, ImportCostCalculator.exciseRate(2001), delta)
        assertEquals(0.186, ImportCostCalculator.exciseRate(3000), delta)
    }

    @Test
    fun `unknown non-electric capacity uses conservative highest rate`() {
        assertEquals(0.186, ImportCostCalculator.exciseRate(null), delta)
        assertEquals(0.186, ImportCostCalculator.exciseRate(null, FuelType.PETROL), delta)
        assertEquals(0.186, ImportCostCalculator.exciseRate(null, FuelType.HYBRID), delta)
        assertEquals(0.186, ImportCostCalculator.exciseRate(null, FuelType.PLUGIN_HYBRID), delta)
    }

    @Test
    fun `small-engine breakdown compounds duty then excise then VAT`() {
        val e = ImportCostCalculator.estimate(
            vehiclePriceUsd = 20_000.0,
            shippingUsd = 2_000.0,
            engineCapacityCc = 1_800,
        )
        // customs = (20_000 + 2_000) * 10% = 2_200
        // excise = (20_000 + 2_000 + 2_200) * 3.1% = 750.2
        // VAT = (20_000 + 2_000 + 2_200 + 750.2) * 23% = 5_738.546
        assertEquals(2_200.0, e.customsDuty.amount, delta)
        assertEquals(750.2, e.exciseDuty.amount, delta)
        assertEquals(5_738.546, e.vat.amount, delta)
        assertEquals(30_688.746, e.total.amount, delta)
    }

    @Test
    fun `large-engine breakdown uses the higher excise rate`() {
        val e = ImportCostCalculator.estimate(
            vehiclePriceUsd = 20_000.0,
            shippingUsd = 2_000.0,
            engineCapacityCc = 3_000,
        )
        // customs = (20_000 + 2_000) * 10% = 2_200
        // excise = (20_000 + 2_000 + 2_200) * 18.6% = 4_501.2
        // VAT = (20_000 + 2_000 + 2_200 + 4_501.2) * 23% = 6_601.276
        assertEquals(2_200.0, e.customsDuty.amount, delta)
        assertEquals(4_501.2, e.exciseDuty.amount, delta)
        assertEquals(6_601.276, e.vat.amount, delta)
        assertEquals(35_302.476, e.total.amount, delta)
    }

    @Test
    fun `unknown-engine estimate cannot undercut known large-engine estimate`() {
        val unknown = ImportCostCalculator.estimate(20_000.0, 2_000.0, null)
        val large = ImportCostCalculator.estimate(20_000.0, 2_000.0, 3_000)
        assertEquals(large.exciseDuty.amount, unknown.exciseDuty.amount, delta)
        assertEquals(large.total.amount, unknown.total.amount, delta)
    }

    @Test
    fun `total equals the sum of its parts`() {
        val e = ImportCostCalculator.estimate(15_000.0, 1_500.0, 2_500)
        val sum = e.vehiclePrice.amount + e.shipping.amount + e.customsDuty.amount +
            e.exciseDuty.amount + e.vat.amount
        assertEquals(e.total.amount, sum, delta)
    }

    @Test
    fun `every component is denominated in USD`() {
        val e = ImportCostCalculator.estimate(10_000.0, 1_000.0, 1_500)
        assertTrue(
            listOf(e.vehiclePrice, e.shipping, e.customsDuty, e.exciseDuty, e.vat, e.total)
                .all { it.currency == Currency.USD },
        )
    }

    @Test
    fun `electric is exempt regardless of capacity`() {
        assertEquals(0.0, ImportCostCalculator.exciseRate(3000, FuelType.ELECTRIC), delta)
        assertEquals(0.0, ImportCostCalculator.exciseRate(null, FuelType.ELECTRIC), delta)
    }

    @Test
    fun `plug-in hybrid is exempt up to 2_0L then 9_3 then 18_6`() {
        assertEquals(0.0, ImportCostCalculator.exciseRate(1800, FuelType.PLUGIN_HYBRID), delta)
        assertEquals(0.093, ImportCostCalculator.exciseRate(2500, FuelType.PLUGIN_HYBRID), delta)
        assertEquals(0.186, ImportCostCalculator.exciseRate(4000, FuelType.PLUGIN_HYBRID), delta)
    }

    @Test
    fun `full hybrid is 1_55 up to 2_0L then 9_3 then 18_6`() {
        assertEquals(0.0155, ImportCostCalculator.exciseRate(1600, FuelType.HYBRID), delta)
        assertEquals(0.093, ImportCostCalculator.exciseRate(3000, FuelType.HYBRID), delta)
        assertEquals(0.186, ImportCostCalculator.exciseRate(4000, FuelType.HYBRID), delta)
    }

    @Test
    fun `combustion fuels keep the 3_1 and 18_6 split`() {
        for (fuel in listOf(FuelType.PETROL, FuelType.DIESEL, FuelType.LPG)) {
            assertEquals(0.031, ImportCostCalculator.exciseRate(2000, fuel), delta)
            assertEquals(0.186, ImportCostCalculator.exciseRate(2001, fuel), delta)
        }
    }

    @Test
    fun `electric estimate has zero excise and unchanged duty plus VAT`() {
        val e = ImportCostCalculator.estimate(
            vehiclePriceUsd = 30_000.0,
            shippingUsd = 2_000.0,
            engineCapacityCc = null,
            fuelType = FuelType.ELECTRIC,
        )
        assertEquals(0.0, e.exciseDuty.amount, delta)
        assertEquals(3_200.0, e.customsDuty.amount, delta)
        assertEquals(8_096.0, e.vat.amount, delta)
        assertEquals(43_296.0, e.total.amount, delta)
    }

    @Test
    fun `default fuel type keeps combustion behaviour for known capacity`() {
        assertEquals(
            ImportCostCalculator.exciseRate(1800),
            ImportCostCalculator.exciseRate(1800, FuelType.UNKNOWN),
            delta,
        )
        assertEquals(0.031, ImportCostCalculator.exciseRate(1800), delta)
    }
}
