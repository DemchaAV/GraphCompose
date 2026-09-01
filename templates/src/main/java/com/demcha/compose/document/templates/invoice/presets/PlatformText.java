package com.demcha.compose.document.templates.invoice.presets;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * How the Platform invoice writes its figures.
 *
 * <p>This design states its currency once per money column and writes the
 * figures under it bare, so nothing here carries a code. The locale is stated
 * rather than inherited, so the same data renders the same sheet on any
 * machine.</p>
 */
final class PlatformText {

    /** A metered rate is quoted to four places; an amount and a count to two. */
    private static final int RATE_MAX_DIGITS = 4;
    private static final int MONEY_DIGITS = 2;

    private PlatformText() {
    }

    /** An amount, grouped and to two places. */
    static String money(BigDecimal value) {
        return decimal(value, MONEY_DIGITS, MONEY_DIGITS);
    }

    /** An amount behind its currency code, for the one figure that stands alone. */
    static String coded(String currencyCode, BigDecimal value) {
        return currencyCode == null || currencyCode.isBlank()
                ? money(value)
                : currencyCode.trim() + " " + money(value);
    }

    /**
     * A unit price at the precision it is quoted at.
     *
     * <p>Metered rates are fractions of a currency unit — an hour of compute at
     * 0.0670 rounds to 0.07 at an amount's two places, which is a different price
     * and multiplies out to a different bill. A rate is written at its own scale,
     * never at fewer than two places and never at more than four.</p>
     */
    static String rate(BigDecimal value) {
        int digits = Math.min(Math.max(value.scale(), MONEY_DIGITS), RATE_MAX_DIGITS);
        return decimal(value, digits, digits);
    }

    /**
     * A usage figure, written the way the design writes it.
     *
     * <p>Usage that names a unit is a measurement and carries two places even
     * when it lands on a whole number — 730.00 hours, 250.00 gigabytes — because
     * the meter reads to that precision and a bare "730" claims it did not.
     * Usage that names no unit is a tally of whole things, and the design writes
     * it bare.</p>
     *
     * @param value the metered figure
     * @param unit  what it counts, blank when the line counts whole things
     * @return the figure as the sheet prints it
     */
    static String usage(BigDecimal value, String unit) {
        if (unit == null || unit.isBlank()) {
            BigDecimal stripped = value.stripTrailingZeros();
            return stripped.scale() <= 0
                    ? stripped.toBigInteger().toString()
                    : stripped.toPlainString();
        }
        return decimal(value, MONEY_DIGITS, MONEY_DIGITS);
    }

    private static String decimal(BigDecimal value, int min, int max) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(min);
        format.setMaximumFractionDigits(max);
        return format.format(value);
    }
}
