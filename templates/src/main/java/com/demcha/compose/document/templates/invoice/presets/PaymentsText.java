package com.demcha.compose.document.templates.invoice.presets;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * How the Payments sheet writes its figures.
 *
 * <h2>The locale is stated, never inherited</h2>
 *
 * <p>Both the grouping of a number and the symbol for a currency code are
 * locale-dependent — {@code USD} is {@code $} in English and {@code US$} in the
 * root locale, {@code JPY} is {@code ¥} or {@code JP¥} — so a preset that let
 * the JVM's default locale decide would render a different sheet on a different
 * machine, and its baselines would move with the machine rather than with the
 * design. Both helpers below pin {@link Locale#ENGLISH}.</p>
 */
final class PaymentsText {

    private PaymentsText() {
    }

    /** An amount, grouped and always to two places. */
    static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    /** A quantity, written without trailing zeros a whole number does not need. */
    static String quantity(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() <= 0 ? stripped.toBigInteger().toString() : stripped.toPlainString();
    }

    /**
     * The symbol for an ISO currency code, or the code itself when there is no
     * symbol for it and the empty string when a document states no currency.
     *
     * @param currencyCode the ISO code the figures are stated in
     * @return what to print before an amount
     */
    static String symbol(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return "";
        }
        try {
            return Currency.getInstance(currencyCode.trim()).getSymbol(Locale.ENGLISH);
        } catch (IllegalArgumentException notACurrency) {
            return currencyCode.trim();
        }
    }

    /** An amount behind its currency's symbol. */
    static String amount(String currencyCode, BigDecimal value) {
        return symbol(currencyCode) + money(value);
    }
}
