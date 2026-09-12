/**
 * Internal renderers behind the receipt widgets.
 *
 * <p>{@link com.demcha.compose.document.templates.receipt.components.ReceiptStyles}
 * composes the family's text styles from the
 * {@link com.demcha.compose.document.templates.core.theme.BrandTheme};
 * {@link com.demcha.compose.document.templates.receipt.components.FieldRowRenderer}
 * draws one label/value row; and
 * {@link com.demcha.compose.document.templates.receipt.components.StatusPill}
 * draws the rounded status chip.</p>
 *
 * <p>These take {@code (host, data, theme)} and hold no state. They are
 * public because the widgets live in a sibling package, not because callers
 * are expected to reach for them — compose a receipt through
 * {@code receipt.presets} instead.</p>
 */
package com.demcha.compose.document.templates.receipt.components;
