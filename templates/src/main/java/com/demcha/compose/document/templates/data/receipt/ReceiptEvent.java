package com.demcha.compose.document.templates.data.receipt;

import java.util.Objects;

/**
 * One step in a payment's life — {@code Instructed}, {@code Sent to the
 * clearing scheme}, {@code Settled}.
 *
 * <p>The step a payment reached is the question a support agent is asked
 * about a receipt, and the source document usually answers it with a single
 * status word. Carrying the steps lets a template show the whole run.</p>
 *
 * @param label     step name
 * @param timestamp when the step happened, already formatted for display
 * @param detail    optional one-line explanation shown under the step
 */
public record ReceiptEvent(String label, String timestamp, String detail) {

    /**
     * Normalizes every field to non-null display text.
     */
    public ReceiptEvent {
        label = Objects.requireNonNullElse(label, "");
        timestamp = Objects.requireNonNullElse(timestamp, "");
        detail = Objects.requireNonNullElse(detail, "");
    }

    /**
     * Creates a step with no explanatory line.
     *
     * @param label     step name
     * @param timestamp formatted timestamp
     * @return receipt event
     */
    public static ReceiptEvent of(String label, String timestamp) {
        return new ReceiptEvent(label, timestamp, "");
    }
}
