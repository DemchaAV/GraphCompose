package com.demcha.compose.document.templates.invoice.presets;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * How the Metered invoice writes its figures.
 *
 * <p>The locale is stated rather than inherited, so the same data renders the
 * same sheet on any machine.</p>
 */
final class MeteredText {

    /** A metered rate is quoted to four places; an amount to two. */
    private static final int RATE_MAX_DIGITS = 4;
    private static final int MONEY_DIGITS = 2;

    private MeteredText() {
    }

    /** An amount, grouped and to two places. */
    private static String money(BigDecimal value) {
        return decimal(value, MONEY_DIGITS, MONEY_DIGITS);
    }

    /**
     * A unit price at the precision it is quoted at.
     *
     * <p>Metered rates are fractions of a currency unit — an hour of compute at
     * 0.0680 rounds to 0.07 at an amount's two places, which is a different
     * price and multiplies out to a different bill. A rate is written at its own
     * scale, never at fewer than two places and never at more than four.</p>
     *
     * @param value the rate
     * @return the rate as the sheet prints it
     */
    private static String rate(BigDecimal value) {
        int digits = Math.min(Math.max(value.scale(), MONEY_DIGITS), RATE_MAX_DIGITS);
        return decimal(value, digits, digits);
    }

    /** An amount behind its currency code, the way every figure on the sheet is written. */
    static String codedMoney(String currencyCode, BigDecimal value) {
        return code(currencyCode, money(value));
    }

    /** A rate behind its currency code. */
    static String codedRate(String currencyCode, BigDecimal value) {
        return code(currencyCode, rate(value));
    }

    /** A count and the thing it counts, or the count alone when the line names no unit. */
    static String quantityWithUnit(BigDecimal value, String unit) {
        BigDecimal stripped = value.stripTrailingZeros();
        String written = stripped.scale() <= 0
                ? stripped.toBigInteger().toString()
                : stripped.toPlainString();
        return unit == null || unit.isBlank() ? written : written + " " + unit;
    }

    private static String code(String currencyCode, String written) {
        return currencyCode == null || currencyCode.isBlank()
                ? written
                : currencyCode.trim() + " " + written;
    }

    private static String decimal(BigDecimal value, int min, int max) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(min);
        format.setMaximumFractionDigits(max);
        return format.format(value);
    }
}
