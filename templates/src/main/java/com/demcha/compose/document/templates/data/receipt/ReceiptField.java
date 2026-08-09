package com.demcha.compose.document.templates.data.receipt;

import java.util.Objects;

/**
 * One label/value pair on a payment receipt — {@code Value date → 07 Jul 2026},
 * {@code Sort code → 04-00-75}.
 *
 * <p>The value is display text, already formatted by the caller: a receipt
 * restates what a payment system decided, so the template never parses an
 * amount, a date, or an account number, and never reformats one.</p>
 *
 * @param label field caption shown in the left column
 * @param value field value shown in the right column
 * @param emphasized whether the value renders in the emphasised weight —
 *                   used for the one field per group a reader looks for first
 */
public record ReceiptField(String label, String value, boolean emphasized) {

    /**
     * Normalizes both sides to non-null display text.
     */
    public ReceiptField {
        label = Objects.requireNonNullElse(label, "");
        value = Objects.requireNonNullElse(value, "");
    }

    /**
     * Creates a plain field.
     *
     * @param label field caption
     * @param value field value
     * @return an unemphasised field
     */
    public static ReceiptField of(String label, String value) {
        return new ReceiptField(label, value, false);
    }

    /**
     * Creates a field whose value renders in the emphasised weight.
     *
     * @param label field caption
     * @param value field value
     * @return an emphasised field
     */
    public static ReceiptField emphasized(String label, String value) {
        return new ReceiptField(label, value, true);
    }
}
