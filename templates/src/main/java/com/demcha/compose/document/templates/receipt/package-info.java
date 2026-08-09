/**
 * The layered <em>receipt</em> template family — payment confirmations,
 * transfer advices, card receipts.
 *
 * <h2>Layers</h2>
 *
 * <pre>
 * templates.data.receipt      records: amount, parties, field groups, timeline
 *          │
 * templates.core.theme        BrandTheme.receiptModern() — shared tokens
 *          │
 * receipt.components          ReceiptStyles · FieldRowRenderer · StatusPill
 *          │
 * receipt.widgets             ReceiptMasthead · AmountHero · PartyPair
 *                             DetailGroup · StatusTrail · ReceiptFooter
 *          │
 * receipt.presets             ModernReceipt — sequences the blocks
 * </pre>
 *
 * <h2>Authoring a receipt in four steps</h2>
 *
 * <ol>
 *   <li><b>Describe the payment.</b> Build a
 *       {@link com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec}
 *       — the amount and its caption, the status and its tone, the two
 *       parties, one titled group per block of details, and the steps the
 *       payment went through. Everything is display text; the template never
 *       formats a number or a date.</li>
 *   <li><b>Pick the theme.</b>
 *       {@link com.demcha.compose.document.templates.core.theme.BrandTheme#receiptModern()}
 *       carries the statement look and deliberately no brand colour.</li>
 *   <li><b>Brand it.</b> Load the issuer's mark with
 *       {@link com.demcha.compose.document.templates.core.identity.SvgGlyph#fromResource(String)}
 *       and pass it, with the accent, through
 *       {@code ModernReceipt.Options}.</li>
 *   <li><b>Compose and render.</b>
 *       {@code ModernReceipt.create(theme, options).compose(session, spec)},
 *       then {@code session.buildPdf()}.</li>
 * </ol>
 *
 * <p>Blocks with no data drop out, so the same preset renders a five-row
 * card receipt and a full transfer confirmation with a timeline and a
 * verification code.</p>
 */
package com.demcha.compose.document.templates.receipt;
