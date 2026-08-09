package com.demcha.compose.document.templates.data.receipt;

import java.util.Objects;

/**
 * The status shown on a receipt: the issuer's own word for it, plus the tone
 * a template is allowed to colour on.
 *
 * @param label the issuer's status word, rendered verbatim
 * @param tone  how that word reads — see {@link ReceiptStatusTone}
 */
public record ReceiptStatus(String label, ReceiptStatusTone tone) {

    /**
     * Normalizes the label and defaults a missing tone to
     * {@link ReceiptStatusTone#IN_PROGRESS} — the reading that claims the
     * least about a payment nobody described.
     */
    public ReceiptStatus {
        label = Objects.requireNonNullElse(label, "");
        tone = Objects.requireNonNullElse(tone, ReceiptStatusTone.IN_PROGRESS);
    }

    /**
     * Creates a settled status.
     *
     * @param label the issuer's word for it, for example {@code Completed}
     * @return settled status
     */
    public static ReceiptStatus settled(String label) {
        return new ReceiptStatus(label, ReceiptStatusTone.SETTLED);
    }

    /**
     * Creates an in-progress status.
     *
     * @param label the issuer's word for it, for example {@code Processing}
     * @return in-progress status
     */
    public static ReceiptStatus inProgress(String label) {
        return new ReceiptStatus(label, ReceiptStatusTone.IN_PROGRESS);
    }

    /**
     * Reports whether this status carries a label worth rendering.
     *
     * @return {@code true} when the label is non-blank
     */
    public boolean hasLabel() {
        return !label.isBlank();
    }
}
