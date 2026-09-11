/**
 * The visual blocks a receipt preset arranges.
 *
 * <p>Each widget owns one block of the page and takes
 * {@code (host, data, …, theme)}:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.receipt.widgets.ReceiptMasthead}
 *       — issuer mark, document title, hairline.</li>
 *   <li>{@link com.demcha.compose.document.templates.receipt.widgets.AmountHero}
 *       — the amount, the status chip, and the value/operation dates.</li>
 *   <li>{@link com.demcha.compose.document.templates.receipt.widgets.PartyPair}
 *       — payer and beneficiary cards with the direction between them.</li>
 *   <li>{@link com.demcha.compose.document.templates.receipt.widgets.DetailGroup}
 *       — one titled block of label/value rows.</li>
 *   <li>{@link com.demcha.compose.document.templates.receipt.widgets.StatusTrail}
 *       — the payment's steps as a connected timeline.</li>
 *   <li>{@link com.demcha.compose.document.templates.receipt.widgets.ReceiptFooter}
 *       — verification QR code, support lines, small print.</li>
 * </ul>
 *
 * <p>Every widget skips itself when its data is absent, so a preset can
 * sequence all six without guarding each one.</p>
 */
package com.demcha.compose.document.templates.receipt.widgets;
