package com.demcha.compose.document.templates.invoice.presets;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * How the Merchant invoice writes its figures.
 *
 * <p>This design states its currency once in each money column's caption and
 * writes the figures under it bare, carrying the code only on the grand total,
 * which stands under no caption. The locale is stated rather than inherited, so
 * the same data renders the same sheet on any machine.</p>
 */
final class MerchantText {

    private MerchantText() {
    }

    /** An amount, grouped and to two places, with no currency against it. */
    static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    /** An amount behind its currency code, for the one figure that stands alone. */
    static String coded(String currencyCode, BigDecimal value) {
        return currencyCode == null || currencyCode.isBlank()
                ? money(value)
                : currencyCode.trim() + " " + money(value);
    }

    /** A caption with its currency stated once, the way this design labels a money column. */
    static String captionWithCurrency(String caption, String currencyCode) {
        return currencyCode == null || currencyCode.isBlank() || caption.isBlank()
                ? caption
                : caption + " (" + currencyCode.trim() + ")";
    }

    /**
     * A count, bare, with no fraction when it has none.
     *
     * <p>A line that counts nothing — a fee charged as it falls rather than per
     * unit — prints the design's own dash instead of a zero, because a zero in a
     * quantity column reads as none delivered rather than as not counted.</p>
     */
    static String quantity(BigDecimal value) {
        if (value.signum() == 0) {
            return "—";
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() <= 0
                ? stripped.toBigInteger().toString()
                : stripped.toPlainString();
    }
}
