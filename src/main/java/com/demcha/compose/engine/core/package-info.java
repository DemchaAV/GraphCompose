/**
 * Core ECS primitives that hold entity/component graphs and shared traversal
 * state.
 *
 * <p>This package contains the foundation used by the runtime pipeline. In the
 * normal PDF pipeline the high-level flow is:</p>
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
 *   <li>{@code engine.layout}: low-level layout systems</li>
 *   <li>{@code engine.pagination}: page-breaking helpers</li>
 *   <li>{@code engine.measurement}: text measurement contracts</li>
 *   <li>{@code engine.render}: backend-neutral render-pass contracts and dispatch helpers</li>
 * </ul>
 *
 * <p>Project policy is to keep backend-specific lifecycle concerns out of the
 * shared engine layer wherever possible. The engine should reason in terms of
 * resolved geometry, entity ordering, and render-session seams, while PDFBox,
 * DOCX, or PPTX specifics stay in backend-owned packages.</p>
 */
package com.demcha.compose.engine.core;
