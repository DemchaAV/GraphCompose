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
 * <p>The legacy {@code Entity}-based ECS PDF renderer has moved to the
 * {@code ecs} sub-package and is {@code @Deprecated}; nothing in this package or
 * the canonical pipeline depends on it.</p>
 */
package com.demcha.compose.engine.render.pdf;
