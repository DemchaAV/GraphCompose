/**
 * Shared PDFBox primitives used by the canonical fixed-layout PDF backend.
 *
 * <p>This package holds the PDF support types that the canonical pipeline
 * ({@code com.demcha.compose.document.backend.fixed.pdf}) depends on:</p>
 *
 * <ul>
 *   <li>{@code PdfFont} — PDFBox font loading, embedding, and width metrics, also
 *       consumed by {@code com.demcha.compose.engine.measurement} text
 *       measurement;</li>
 *   <li>{@code GlyphFallbackLogger} — diagnostic logging for code points a font
 *       cannot map;</li>
 *   <li>the {@code helpers} sub-package — the canonical header/footer
 *       ({@code PdfHeaderFooterRenderer}) and watermark
 *       ({@code PdfWatermarkRenderer}) post-processing renderers.</li>
 * </ul>
 *
 * <p>Package policy is to keep PDFBox concerns behind this package (and the
 * canonical {@code document.backend.fixed.pdf} backend) rather than leaking them
 * into backend-neutral engine components or builders.</p>
 *
 * <p>The 2.0 line removed the legacy entity-based PDF renderer that once sat beside
 * this package; nothing here or in the canonical pipeline replaced it, because the
 * canonical backend had already taken over every path it served.</p>
 */
package com.demcha.compose.engine.render.pdf;
