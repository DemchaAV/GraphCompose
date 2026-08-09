package com.demcha.compose.document.templates.data.receipt;

/**
 * How a receipt's status reads, independent of the word used for it.
 *
 * <p>Payment systems each have their own vocabulary — {@code Completed},
 * {@code Settled}, {@code Executed} all mean the money arrived. The tone is
 * what a template can colour on; the label stays whatever the issuer calls
 * it, so a template never has to recognise a status word to render it
 * correctly.</p>
 */
public enum ReceiptStatusTone {

    /** The payment reached its destination. */
    SETTLED,

    /** The payment is on its way and nothing is wrong. */
    IN_PROGRESS,

    /** The payment needs someone to look at it — held, returned, disputed. */
    ATTENTION,

    /** The payment did not happen — rejected, cancelled, failed. */
    FAILED
}
