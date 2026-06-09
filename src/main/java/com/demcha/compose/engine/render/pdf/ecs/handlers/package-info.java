/**
 * Legacy ECS render-marker handlers for the
 * {@code com.demcha.compose.engine.render.pdf.ecs.PdfRenderingSystemECS} renderer.
 *
 * <p>Part of the dead legacy ECS PDF render pipeline; see
 * {@code com.demcha.compose.engine.render.pdf.ecs} for context. These handlers
 * dispatch {@code Entity}-based render markers to PDFBox drawing operations and
 * are retained only for the legacy engine regression tests. The canonical
 * pipeline uses its own page-fragment render handlers under
 * {@code com.demcha.compose.document.backend.fixed.pdf.handlers}.</p>
 *
 * @deprecated Legacy ECS render handlers, superseded by
 *     {@code com.demcha.compose.document.backend.fixed.pdf.handlers}. Do not
 *     extend or spend optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.render.pdf.ecs.handlers;
