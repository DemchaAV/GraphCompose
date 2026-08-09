/**
 * Records describing a payment receipt — a transfer confirmation, a direct
 * debit advice, a card payment receipt.
 *
 * <p>{@link com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec}
 * is the document-level input; it wraps a
 * {@link com.demcha.compose.document.templates.data.receipt.ReceiptData}
 * carrying the hero amount, the two parties, the titled field groups, the
 * status timeline, and the footer small print.</p>
 *
 * <p>Every value in here is display text the issuing system already decided.
 * These records hold no rendering, theming, or DSL dependency, which is what
 * lets the same receipt render through any preset.</p>
 */
package com.demcha.compose.document.templates.data.receipt;
