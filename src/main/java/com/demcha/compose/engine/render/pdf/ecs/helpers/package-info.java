/**
 * Legacy ECS PDF render helper primitives: {@code TableCellBox} (cell-box drawing
 * for the ECS table-row handler) and {@code PdfBookmarkBuilder} (PDF outline
 * assembly from {@code Entity} bookmark components).
 *
 * <p>Part of the dead legacy ECS PDF render pipeline; see
 * {@code com.demcha.compose.engine.render.pdf.ecs} for context. Retained only for
 * the legacy engine regression tests. The canonical header/footer and watermark
 * renderers live in {@code com.demcha.compose.engine.render.pdf.helpers} and are
 * <em>not</em> deprecated.</p>
 *
 * @deprecated Legacy ECS PDF render helpers — a candidate for removal. Do not
 *     extend or spend optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.render.pdf.ecs.helpers;
