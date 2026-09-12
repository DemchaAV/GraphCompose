package com.demcha.compose.document.templates.invoice.presets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * How the Obsidian invoice writes its figures, and the one figure it works out
 * for itself.
 *
 * <p>Money is written with the currency's mark against the digits, so every
 * figure names its own currency and no column states one. The locale is stated
 * rather than inherited, so the same data renders the same sheet on any
 * machine.</p>
 */
final class ObsidianText {

    private ObsidianText() {
    }

    /**
     * An amount behind its currency mark.
     *
     * @param currencyCode the code the invoice is billed in
     * @param value        the amount
     * @return the amount as the sheet prints it
     */
    static String money(String currencyCode, BigDecimal value) {
        return mark(currencyCode) + amount(value);
    }

    /**
     * The tax carried on one line.
     *
     * <p>This design gives the tax its own money column, where the model carries
     * a rate. The figure is not a second thing to state: a line already gives the
     * quantity, the unit price and the total it comes to, so the tax is the
     * difference between what was charged and what the goods cost. Reading it off
     * the line rather than asking for it again is also what keeps the column and
     * the total consistent when either moves.</p>
     *
     * @param currencyCode the code the invoice is billed in
     * @param quantity     how much was delivered
     * @param unitPrice    the price per unit
     * @param amount       the line total
     * @return the tax as the sheet prints it
     */
    static String lineTax(String currencyCode, BigDecimal quantity, BigDecimal unitPrice,
                          BigDecimal amount) {
        BigDecimal net = quantity.multiply(unitPrice);
        BigDecimal tax = amount.subtract(net).max(BigDecimal.ZERO);
        return money(currencyCode, tax.setScale(2, RoundingMode.HALF_UP));
    }

    /** A count, bare, with no fraction when it has none. */
    static String quantity(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() <= 0
                ? stripped.toBigInteger().toString()
                : stripped.toPlainString();
    }

    private static String mark(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return "";
        }
        try {
            return Currency.getInstance(currencyCode.trim()).getSymbol(Locale.ENGLISH);
        } catch (IllegalArgumentException notACurrency) {
            return currencyCode.trim();
        }
    }

    private static String amount(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }
}
