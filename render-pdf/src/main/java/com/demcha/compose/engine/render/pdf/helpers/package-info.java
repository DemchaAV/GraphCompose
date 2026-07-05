/**
 * Canonical PDF post-processing renderers for the fixed-layout backend:
 * {@code PdfHeaderFooterRenderer} (running headers and footers) and
 * {@code PdfWatermarkRenderer} (page watermarks), applied by
 * {@code com.demcha.compose.document.backend.fixed.pdf.PdfDocumentPostProcessor}.
 *
 * <p>The legacy ECS render helpers ({@code TableCellBox},
 * {@code PdfBookmarkBuilder}) have moved to the deprecated
 * {@code com.demcha.compose.engine.render.pdf.ecs.helpers} package.</p>
 */
package com.demcha.compose.engine.render.pdf.helpers;
