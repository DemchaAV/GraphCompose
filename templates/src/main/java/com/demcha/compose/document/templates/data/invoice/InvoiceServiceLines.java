package com.demcha.compose.document.templates.data.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The line-items table of a structured invoice: its column labels and its
 * priced service lines.
 *
 * <p>Distinct from the narrative model's {@link InvoiceLineItem}, whose
 * quantity, unit price and amount are pre-formatted strings: a service
 * line carries {@link BigDecimal} figures plus the unit they are counted
 * in, so the preset formats them against the document's currency and the
 * caller keeps the arithmetic. The column labels are content because the
 * currency code and the wording appear inside them.</p>
 *
 * @param columns the column labels
 * @param lines   the priced service lines, in print order
 */
public record InvoiceServiceLines(Columns columns, List<Line> lines) {

    /**
     * Normalizes an absent column set and freezes the line list.
     */
    public InvoiceServiceLines {
        columns = columns == null ? new Columns(null, null, null, null, null, null) : columns;
        lines = List.copyOf(Objects.requireNonNullElse(lines, List.of()));
    }

    /**
     * The table's column labels, in column order.
     *
     * @param index         label of the line-number column
     * @param description   label of the description column
     * @param servicePeriod label of the service-period column
     * @param quantity      label of the quantity column
     * @param unitPrice     label of the unit-price column
     * @param amount        label of the amount column
     */
    public record Columns(
            String index,
            String description,
            String servicePeriod,
            String quantity,
            String unitPrice,
            String amount) {

        /**
         * Normalizes optional labels to empty strings.
         */
        public Columns {
            index = Objects.requireNonNullElse(index, "");
            description = Objects.requireNonNullElse(description, "");
            servicePeriod = Objects.requireNonNullElse(servicePeriod, "");
            quantity = Objects.requireNonNullElse(quantity, "");
            unitPrice = Objects.requireNonNullElse(unitPrice, "");
            amount = Objects.requireNonNullElse(amount, "");
        }
    }

    /**
     * One priced service line.
     *
     * @param lineNumber    the printed line number
     * @param title         the service title, set apart from the description
     * @param description   the description under the title
     * @param servicePeriod the period this line covers
     * @param quantity      how much was delivered; {@code null} normalizes
     *                      to {@link BigDecimal#ZERO}
     * @param unit          what the quantity counts (e.g. {@code "hrs"})
     * @param unitPrice     the price per unit; {@code null} normalizes to
     *                      {@link BigDecimal#ZERO}
     * @param amount        the line total; {@code null} normalizes to
     *                      {@link BigDecimal#ZERO}
     */
    public record Line(
            int lineNumber,
            String title,
            String description,
            String servicePeriod,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount) {

        /**
         * Normalizes optional fields.
         */
        public Line {
            title = Objects.requireNonNullElse(title, "");
            description = Objects.requireNonNullElse(description, "");
            servicePeriod = Objects.requireNonNullElse(servicePeriod, "");
            quantity = Objects.requireNonNullElse(quantity, BigDecimal.ZERO);
            unit = Objects.requireNonNullElse(unit, "");
            unitPrice = Objects.requireNonNullElse(unitPrice, BigDecimal.ZERO);
            amount = Objects.requireNonNullElse(amount, BigDecimal.ZERO);
        }
    }
}
