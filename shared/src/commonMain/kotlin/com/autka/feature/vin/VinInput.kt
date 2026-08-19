package com.autka.feature.vin

object VinInput {
    private const val VIN_LENGTH = 17
    private val forbiddenLetters = setOf('I', 'O', 'Q')

    fun normalize(value: String): String = value
        .filter { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' }
        .uppercase()
        .take(VIN_LENGTH)

    fun isValid(vin: String): Boolean =
        vin.length == VIN_LENGTH && vin.all { char ->
            char.isDigit() || (char in 'A'..'Z' && char !in forbiddenLetters)
        }
}
