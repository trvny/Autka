package com.autka.ui.components

import java.text.DecimalFormatSymbols

/** Parse a non-negative user-entered amount without silently changing its magnitude. */
internal fun parseLocalizedNonNegativeAmount(
    value: String,
    symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance(),
): Double? {
    val compact = normalizeDigitsAndWhitespace(value)
    if (compact.isEmpty()) return null

    val allowedSeparators = setOf('.', ',', ' ', symbols.decimalSeparator, symbols.groupingSeparator)
    if (compact.any { !it.isDigit() && it !in allowedSeparators }) return null

    val separators = compact.withIndex().filter { !it.value.isDigit() }
    if (separators.isEmpty()) {
        return compact.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    }

    fun fractionAfter(index: Int): String = compact.substring(index + 1)
    fun isDecimalCandidate(index: Int): Boolean {
        val fraction = fractionAfter(index)
        return fraction.length in 1..2 && fraction.all(Char::isDigit)
    }

    val localeSeparators = separators.filter { it.value == symbols.decimalSeparator }
    if (localeSeparators.size > 1) return null
    if (localeSeparators.size == 1) {
        val localeSeparator = localeSeparators.single()
        val suffix = fractionAfter(localeSeparator.index)
        // A locale decimal followed by a digit-only fraction is decisive evidence.
        // Reject unsupported precision instead of reinterpreting it as grouping.
        if (suffix.all(Char::isDigit) && suffix.length > 2) return null
    }

    val localeDecimal = localeSeparators
        .singleOrNull { isDecimalCandidate(it.index) }
    val decimal = localeDecimal ?: separators.last().takeIf { last ->
        if (last.value == ' ' || separators.count { it.value == last.value } != 1 ||
            !isDecimalCandidate(last.index)
        ) {
            false
        } else if (last.value != symbols.groupingSeparator) {
            true
        } else {
            // A lone locale grouping mark with a short prefix is ambiguous: `20,00`
            // might be a mistyped 2,000. Accept foreign decimal syntax only when the
            // magnitude cannot plausibly be a grouped local integer, or another
            // separator already disambiguates the integer part.
            val hasOtherSeparator = separators.any { it.value != last.value }
            val prefix = compact.substring(0, last.index)
            hasOtherSeparator || (prefix.length > 3 && prefix.all(Char::isDigit))
        }
    }

    val integerPart = decimal?.let { compact.substring(0, it.index) } ?: compact
    val fractionPart = decimal?.let { fractionAfter(it.index) }

    val integerDigits = normalizeGroupedInteger(integerPart) ?: return null
    if (fractionPart != null && !fractionPart.all(Char::isDigit)) return null

    val canonical = buildString {
        append(if (integerDigits.isEmpty()) "0" else integerDigits)
        if (fractionPart != null) append('.').append(fractionPart)
    }
    return canonical.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
}

/** Empty or a valid partial numeric value is still being typed. */
internal fun isIncompleteLocalizedAmount(
    value: String,
    symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance(),
): Boolean {
    val compact = normalizeDigitsAndWhitespace(value)
    if (compact.isEmpty()) return true

    val recognizedSeparators = setOf('.', ',', ' ', symbols.decimalSeparator, symbols.groupingSeparator)
    if (compact.last() in recognizedSeparators) {
        val grouping = compact.last()
        val prefix = compact.dropLast(1)
        if (prefix.isEmpty()) return true

        val parsedPrefix = parseLocalizedNonNegativeAmount(prefix, symbols)
        if (parsedPrefix != null) {
            // Once the prefix is a decimal value, another separator cannot extend it.
            // A complete integer/grouped integer can start either a fraction or another group.
            return normalizeGroupedInteger(prefix) != null
        }

        // A locale grouping prefix may be incomplete until a following group exists,
        // e.g. Indian 12,34, -> 12,34,000. A decimal separator cannot use this path.
        if (grouping == symbols.decimalSeparator) return false
        return normalizeGroupedInteger("$prefix${grouping}000") != null
    }

    if (!compact.last().isDigit() || parseLocalizedNonNegativeAmount(compact, symbols) != null) {
        return false
    }

    // Grouped integers such as 1,234,5 or 12,34,5 are valid prefixes while the final
    // 3-digit group is still being typed. Only infer this for one consistent grouping
    // character that is not the active locale decimal separator.
    val groupingSeparators = compact.filterNot(Char::isDigit).toSet()
    if (groupingSeparators.size != 1) return false
    val grouping = groupingSeparators.single()
    if (grouping == symbols.decimalSeparator) return false

    val groups = compact.split(grouping)
    val tail = groups.lastOrNull() ?: return false
    if (groups.size < 2 || tail.length !in 1..2 || !tail.all(Char::isDigit)) return false

    val completed = groups.dropLast(1).plus(tail.padEnd(3, '0')).joinToString(grouping.toString())
    return normalizeGroupedInteger(completed) != null
}

/** Engine capacity is an integer: accept Unicode digits and validated locale grouping. */
internal fun parsePositiveInteger(
    value: String,
    symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance(),
): Int? {
    val compact = normalizeDigitsAndWhitespace(value)
    if (compact.isEmpty()) return null

    val groupingSeparator = if (symbols.groupingSeparator.isWhitespace()) ' ' else symbols.groupingSeparator
    val allowedGrouping = setOf(' ', groupingSeparator)
    if (compact.any { !it.isDigit() && it !in allowedGrouping }) return null

    val digits = normalizeGroupedInteger(compact) ?: return null
    return digits.toIntOrNull()?.takeIf { it > 0 }
}

private fun normalizeDigitsAndWhitespace(value: String): String {
    val normalized = buildString {
        var pendingSpace = false
        value.trim().forEach { char ->
            val digit = Character.digit(char, 10)
            when {
                char.isWhitespace() || char == '\u00A0' || char == '\u202F' -> {
                    if (isNotEmpty()) pendingSpace = true
                }
                else -> {
                    if (pendingSpace) {
                        append(' ')
                        pendingSpace = false
                    }
                    if (digit >= 0) append(('0'.code + digit).toChar()) else append(char)
                }
            }
        }
    }
    return normalized.trim()
}

private fun normalizeGroupedInteger(value: String): String? {
    if (value.isEmpty()) return ""
    if (value.all(Char::isDigit)) return value

    val groupingSeparators = value.filterNot(Char::isDigit).toSet()
    if (groupingSeparators.size != 1) return null

    val groups = value.split(groupingSeparators.single())
    if (groups.any { it.isEmpty() }) return null

    val first = groups.first()
    if (first.length !in 1..3) return null

    when (groups.size) {
        1 -> Unit
        2 -> if (groups[1].length != 3) return null
        else -> {
            if (groups.last().length != 3) return null
            val middleLengths = groups.drop(1).dropLast(1).map(String::length).toSet()
            if (middleLengths.size != 1) return null
            val middleSize = middleLengths.single()
            if (middleSize !in 2..3 || first.length > middleSize) return null
        }
    }
    return groups.joinToString("")
}
