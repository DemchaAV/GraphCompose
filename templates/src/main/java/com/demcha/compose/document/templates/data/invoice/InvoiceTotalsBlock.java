package com.demcha.compose.document.templates.data.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The totals stack of a structured invoice: the rows leading up to the
 * grand total, and the total band itself.
 *
 * <p>Distinct from the narrative model's {@link InvoiceSummaryRow} list,
 * where the grand total is the last row and is recognised by convention:
 * here the total is its own labelled band with its own amount, so a preset
 * renders it without inspecting the list.</p>
 *
 * @param rows        the rows above the total (subtotal, tax, discounts),
 *                    in print order
 * @param totalLabel  the label of the total band
 * @param totalAmount the amount of the total band; {@code null} normalizes
 *                    to {@link BigDecimal#ZERO}
 */
public record InvoiceTotalsBlock(List<Row> rows, String totalLabel, BigDecimal totalAmount) {

    /**
     * Normalizes optional fields and freezes the row list.
     */
    public InvoiceTotalsBlock {
        rows = List.copyOf(Objects.requireNonNullElse(rows, List.of()));
        totalLabel = Objects.requireNonNullElse(totalLabel, "");
        totalAmount = Objects.requireNonNullElse(totalAmount, BigDecimal.ZERO);
    }

    /**
     * One row above the total band.
     *
     * @param label  the row label, carrying its own wording (e.g. a tax
     *               name and rate)
     * @param amount the row amount; {@code null} normalizes to
     *               {@link BigDecimal#ZERO}
     */
    public record Row(String label, BigDecimal amount) {

        /**
         * Normalizes optional fields.
         */
        public Row {
            label = Objects.requireNonNullElse(label, "");
            amount = Objects.requireNonNullElse(amount, BigDecimal.ZERO);
        }
    }
}
