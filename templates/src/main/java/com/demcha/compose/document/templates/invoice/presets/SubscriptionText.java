package com.demcha.compose.document.templates.invoice.presets;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * How the Subscription invoice writes its figures.
 *
 * <p>This design writes money with the currency's <em>symbol</em> against the
 * digits rather than with a code in front or a caption above, so every figure on
 * the sheet carries its own currency and none of the columns state one. The
 * locale is stated rather than inherited, so the same data renders the same
 * sheet on any machine.</p>
 */
final class SubscriptionText {

    private SubscriptionText() {
    }

    /**
     * An amount behind its currency symbol.
     *
     * @param currencyCode the code the invoice is billed in
     * @param value        the amount
     * @return the amount as the sheet prints it
     */
    static String money(String currencyCode, BigDecimal value) {
        return symbol(currencyCode) + amount(value);
    }

    /**
     * A count, bare, with no fraction when it has none.
     *
     * @param value the count
     * @return the count as the sheet prints it
     */
    static String quantity(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() <= 0
                ? stripped.toBigInteger().toString()
                : stripped.toPlainString();
    }

    /**
     * The symbol a code is written with, or the code itself when the runtime
     * knows no symbol for it.
     *
     * <p>Either way it goes hard against the digits, which is this design's own
     * rule and not only a convention for the short marks: a sheet that prints
     * the code there is still readable, and one that prints nothing at all names
     * no currency anywhere. The columns are measured for a one-character mark
     * though, so a three-letter code leaves the amount that much less room.</p>
     */
    private static String symbol(String currencyCode) {
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
