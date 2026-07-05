/**
 * Canonical PDF backend for the semantic GraphCompose document pipeline.
 *
 * <p>The package owns PDFBox integration, page-scoped render sessions, shared
 * render-pass caches, and the handler registry that paints resolved fragments.</p>
 *
 * <h2>Thread safety</h2>
 *
 * <p>{@link com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend}
 * is immutable after construction. One backend instance can be reused across
 * documents and shared across threads; each
 * {@link com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend#render
 * render(...)} call allocates its own
 * {@link com.demcha.compose.document.backend.fixed.pdf.PdfRenderSession} and
 * {@link com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment},
 * which themselves are <strong>not thread-safe</strong> and live only for the
 * duration of one render pass.</p>
 *
 * <h2>Extension points</h2>
 *
 * <p>{@link com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler}
 * is the public extension point for adding or overriding fragment painters.
 * Custom handlers register through
 * {@link com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend.Builder#addHandler
 * PdfFixedLayoutBackend.Builder#addHandler(PdfFragmentRenderHandler)}; if a
 * custom handler reports the same {@code payloadType()} as a built-in default
 * it replaces the default for that backend instance.</p>
 */
package com.demcha.compose.document.backend.fixed.pdf;
