package com.demcha.compose.document.templates.invoice.presets;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * How the Workspace sheet writes its figures.
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
final class WorkspaceText {

    private WorkspaceText() {
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

    /**
     * An amount named by its currency code rather than its symbol — this design
     * writes {@code USD 587.50} on the one figure it names at all.
     *
     * @param currencyCode the ISO code the figures are stated in
     * @param value        the amount
     * @return the amount behind its code, or bare when no currency is stated
     */
    static String coded(String currencyCode, BigDecimal value) {
        return currencyCode == null || currencyCode.isBlank()
                ? money(value)
                : currencyCode.trim() + " " + money(value);
    }

    /**
     * A column label with the currency named after it, which is where this
     * design states it for the figures under it.
     *
     * @param label        the column's own words
     * @param currencyCode the ISO code the figures are stated in
     * @return the label, with the code in brackets when there is one
     */
    static String labelWithCurrency(String label, String currencyCode) {
        return currencyCode == null || currencyCode.isBlank()
                ? label
                : label + " (" + currencyCode.trim() + ")";
    }

    /**
     * A quantity and what it counts, as the design writes them: one run, so the
     * column stays centred on the pair rather than on the number.
     *
     * @param value the quantity
     * @param unit  what it counts; blank when the number stands alone
     * @return the written quantity
     */
    static String quantityWithUnit(BigDecimal value, String unit) {
        String written = quantity(value);
        return unit == null || unit.isBlank() ? written : written + " " + unit;
    }
}
