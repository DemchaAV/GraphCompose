/**
 * Receipt presets — one class per visual style.
 *
 * <p>{@link com.demcha.compose.document.templates.receipt.presets.ModernReceipt}
 * is the reference preset: a bank-grade transfer confirmation built by
 * sequencing the {@code receipt.widgets} blocks over a
 * {@link com.demcha.compose.document.templates.core.theme.BrandTheme}.</p>
 *
 * <p>A preset is a thin orchestrator. Everything that renders lives in the
 * widgets; what a preset owns is the order of the blocks, the branding knobs
 * it exposes, and the handful of decisions no widget can make for it.</p>
 */
package com.demcha.compose.document.templates.receipt.presets;
