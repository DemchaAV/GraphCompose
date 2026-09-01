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
 *
 * <p><strong>Internal API.</strong> Tagged {@link com.demcha.compose.document.api.Internal}
 * at the package level, like the engine packages that stayed in {@code graph-compose-core}.
 * {@code docs/api-stability.md} has always put the whole {@code com.demcha.compose.engine}
 * tree in the Internal tier &mdash; any release, no deprecation window &mdash; and names this
 * package as the part of it that ships from {@code graph-compose-render-pdf}. Only the
 * annotation is something a guard test can read, and without it {@code PdfFont} and
 * {@code GlyphFallbackLogger} look from the outside like a supported surface. Both repeat
 * the marker on the type, because Javadoc renders a package annotation on this page only
 * &mdash; never on the class pages the package covers.</p>
 */
@com.demcha.compose.document.api.Internal
package com.demcha.compose.engine.render.pdf;
