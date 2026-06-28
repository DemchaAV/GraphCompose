/**
 * Layered invoice presets — thin orchestrators that compose an invoice
 * on a {@link com.demcha.compose.document.templates.core.theme.BrandTheme}
 * plus the canonical DSL.
 *
 * <p>This package mirrors {@code cv.v2.presets}: each preset is a
 * {@code final} class with a {@code create(BrandTheme)} factory returning
 * a {@link com.demcha.compose.document.templates.api.DocumentTemplate}
 * parameterised on
 * {@link com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec}.
 * The presets read every visual value from the theme and reuse the shared
 * {@code templates.core.*} layer; the invoice family does not own a theme
 * type of its own.</p>
 *
 * @since 2.0.0
 */
package com.demcha.compose.document.templates.invoice.v2.presets;
