package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;

/**
 * The masthead of a structured invoice: the document title and the
 * label / value metadata printed beside it.
 *
 * <p>The metadata is an authored list rather than fixed fields, so an
 * invoice prints exactly the rows its business needs — number, dates,
 * project, purchase order, contract reference — in the order and wording
 * it uses. That is why the labels live in the data: they differ per
 * sender, and a preset cannot know them.</p>
 *
 * @param title   the document title (e.g. {@code "INVOICE"})
 * @param entries the metadata rows, in print order
 */
public record InvoiceMasthead(String title, List<Entry> entries) {

    /**
     * Normalizes the title and freezes the entry list.
     */
    public InvoiceMasthead {
        title = Objects.requireNonNullElse(title, "");
        entries = List.copyOf(Objects.requireNonNullElse(entries, List.of()));
    }

    /**
     * One metadata row.
     *
     * @param label      the row label
     * @param value      the row value
     * @param emphasized whether the value carries emphasis — the due date
     *                   is the usual case; presets are expected to set an
     *                   emphasized value apart
     */
    public record Entry(String label, String value, boolean emphasized) {

        /**
         * Normalizes optional fields to empty strings.
         */
        public Entry {
            label = Objects.requireNonNullElse(label, "");
            value = Objects.requireNonNullElse(value, "");
        }
    }
}
