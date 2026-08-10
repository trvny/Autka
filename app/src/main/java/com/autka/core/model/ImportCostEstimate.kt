package com.autka.core.model

/**
 * Rough landed-cost breakdown for importing a US vehicle into Poland.
 * These are ESTIMATES for comparison only -- real figures depend on the
 * customs valuation, the specific vehicle, and current rates.
 */
data class ImportCostEstimate(
    val vehiclePrice: Money,
    val shipping: Money,
    val customsDuty: Money,   // EU passenger-car duty (commonly 10%)
    val exciseDuty: Money,    // PL akcyza -- depends on engine capacity and drivetrain
    val vat: Money,           // VAT on (price + shipping + duty + excise)
    val total: Money,
    val usesConservativeExcise: Boolean = false,
)

object ImportCostCalculator {
    const val DEFAULT_VEHICLE_PRICE_USD = 20_000.0
    const val DEFAULT_US_SHIPPING_USD = 2_400.0
    const val DEFAULT_EU_CUSTOMS_DUTY_RATE = 0.10
    const val DEFAULT_PL_VAT_RATE = 0.23

    private const val MAX_EXCISE_RATE = 0.186

    /**
     * Polish excise (akcyza) as a fraction of the dutiable value. Depends on BOTH engine
     * capacity and drivetrain (2026 rates, ustawa o podatku akcyzowym art. 105/109a/163a):
     *   - Petrol/diesel/LPG:       3.1% up to 2.0L, else 18.6%
     *   - Full hybrid (HEV/MHEV):  1.55% up to 2.0L, 9.3% for 2.0-3.5L, else 18.6%
     *   - Plug-in hybrid (PHEV):   0% up to 2.0L through 2029, 9.3% for 2.0-3.5L, else 18.6%
     *   - Electric / hydrogen:     0% (exempt)
     *
     * Unknown non-exempt capacity deliberately uses the highest current rate rather than
     * silently assuming a small engine. The returned estimate flags that conservative
     * fallback so the UI can explain it until the real cc is known.
     */
    fun exciseRate(engineCapacityCc: Int?, fuelType: FuelType = FuelType.UNKNOWN): Double {
        if (fuelType == FuelType.ELECTRIC || fuelType == FuelType.HYDROGEN) return 0.0
        val cc = engineCapacityCc ?: return MAX_EXCISE_RATE
        return when (fuelType) {
            FuelType.PLUGIN_HYBRID -> when {
                cc <= 2000 -> 0.0
                cc <= 3500 -> 0.093
                else -> MAX_EXCISE_RATE
            }
            FuelType.HYBRID -> when {
                cc <= 2000 -> 0.0155
                cc <= 3500 -> 0.093
                else -> MAX_EXCISE_RATE
            }
            else -> if (cc <= 2000) 0.031 else MAX_EXCISE_RATE
        }
    }

    fun estimate(
        vehiclePriceUsd: Double,
        shippingUsd: Double,
        engineCapacityCc: Int?,
        fuelType: FuelType = FuelType.UNKNOWN,
        customsDutyRate: Double = DEFAULT_EU_CUSTOMS_DUTY_RATE,
        vatRate: Double = DEFAULT_PL_VAT_RATE,
    ): ImportCostEstimate {
        require(customsDutyRate in 0.0..1.0) { "customsDutyRate must be between 0 and 1" }
        require(vatRate in 0.0..1.0) { "vatRate must be between 0 and 1" }

        val price = vehiclePriceUsd
        val shipping = shippingUsd
        val customs = (price + shipping) * customsDutyRate
        val excise = (price + shipping + customs) * exciseRate(engineCapacityCc, fuelType)
        val vat = (price + shipping + customs + excise) * vatRate
        val total = price + shipping + customs + excise + vat
        val usesConservativeExcise = engineCapacityCc == null &&
            fuelType != FuelType.ELECTRIC && fuelType != FuelType.HYDROGEN
        fun usd(v: Double) = Money(v, Currency.USD)
        return ImportCostEstimate(
            vehiclePrice = usd(price),
            shipping = usd(shipping),
            customsDuty = usd(customs),
            exciseDuty = usd(excise),
            vat = usd(vat),
            total = usd(total),
            usesConservativeExcise = usesConservativeExcise,
        )
    }
}
