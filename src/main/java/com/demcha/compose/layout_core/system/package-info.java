/**
 * Core engine systems that turn entity/component graphs into laid-out and
 * rendered documents.
 *
 * <p>This package contains the main runtime orchestration layer of GraphCompose:
 * layout passes, pagination entry points, rendering system contracts, and
 * renderer-facing integration helpers. In the normal PDF pipeline the high-level
 * flow is:</p>
 *
 * <ol>
 *   <li>builders populate {@code Entity} graphs and attach components</li>
 *   <li>{@code LayoutSystem} resolves geometry, hierarchy depth, and layer order</li>
 *   <li>{@code PageBreaker} assigns final page-aware {@code Placement}</li>
 *   <li>a backend renderer such as {@code PdfRenderingSystemECS} consumes the resolved entities</li>
 * </ol>
 *
 * <p>The package is intentionally split further into subpackages with narrower
 * responsibilities:</p>
 *
 * <ul>
 *   <li>{@code interfaces}: backend-neutral system SPI contracts</li>
 *   <li>{@code rendering}: renderer-neutral ordering and handler-dispatch helpers</li>
 *   <li>{@code utils}: lower-level layout and pagination helpers</li>
 *   <li>{@code implemented_systems}: concrete backend implementations such as PDF</li>
 * </ul>
 *
 * <p>Project policy is to keep backend-specific lifecycle concerns out of the
 * shared engine layer wherever possible. The engine should reason in terms of
 * resolved geometry, entity ordering, and render-session seams, while PDFBox,
 * DOCX, or PPTX specifics stay in backend-owned packages.</p>
 */
package com.demcha.compose.layout_core.system;
