/**
 * Legacy ECS PDF render pipeline — the {@code Entity}-based PDFBox renderer that
 * drew resolved entities for the former {@code GraphCompose.pdf(...)} surface.
 *
 * <p>This package is <strong>dead</strong>. The canonical pipeline
 * ({@code GraphCompose.document() -> DocumentSession -> LayoutCompiler ->
 * PdfFixedLayoutBackend}) renders PDFs through
 * {@code com.demcha.compose.document.backend.fixed.pdf} and imports nothing from
 * here, and the {@code GraphCompose.pdf(...)} entry point that drove this renderer
 * has been removed. {@code PdfRenderingSystemECS} and its collaborators
 * ({@code PdfRenderSession}, {@code PdfCanvas}, {@code PdfStream},
 * {@code PdfImageCache}, {@code PdfFileManagerSystem}, {@code PdfGuidesRenderer},
 * the {@code handlers} render-marker dispatch, and the {@code helpers} support
 * primitives) survive only to back the legacy engine regression tests.</p>
 *
 * <p>The genuinely shared PDF code is <em>not</em> here and is <em>not</em>
 * deprecated: {@code engine.render.pdf.PdfFont} and
 * {@code engine.render.pdf.GlyphFallbackLogger}, plus the canonical header/footer
 * and watermark renderers in {@code engine.render.pdf.helpers}, are used by the
 * canonical {@code document.backend.fixed.pdf} backend.</p>
 *
 * @deprecated Legacy ECS PDF renderer, superseded by the canonical
 *     {@code com.demcha.compose.document.backend.fixed.pdf} backend. No public
 *     entry point runs it and it is not on the canonical hot path; it is retained
 *     only for the legacy engine regression tests — a candidate for removal. Do
 *     not extend it or spend optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.render.pdf.ecs;
