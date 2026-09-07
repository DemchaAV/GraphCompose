/**
 * Canonical PDF post-processing renderers for the fixed-layout backend:
 * {@code PdfHeaderFooterRenderer} (running headers and footers) and
 * {@code PdfWatermarkRenderer} (page watermarks), applied by
 * {@code com.demcha.compose.document.backend.fixed.pdf.PdfDocumentPostProcessor}.
 *
 * <p><strong>Internal API.</strong> Tagged {@link com.demcha.compose.document.api.Internal}
 * at the package level. Package annotations do not nest &mdash; the marker on the enclosing
 * {@code com.demcha.compose.engine.render.pdf} package says nothing about this one &mdash; so
 * this package declares its own. Both renderers repeat it on the type as well: Javadoc puts
 * a package annotation on this page and nowhere else, so without the type-level marker a
 * caller reading {@code PdfHeaderFooterRenderer} still sees a plain public class while
 * {@code docs/api-stability.md} reserves the right to delete it in any release.</p>
 */
@com.demcha.compose.document.api.Internal
package com.demcha.compose.engine.render.pdf.helpers;
