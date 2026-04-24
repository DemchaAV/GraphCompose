/**
 * PDF backend implementation for the GraphCompose rendering pipeline.
 *
 * <p>This package contains the concrete PDF renderer and its backend-owned
 * lifecycle helpers. The shared engine reaches this package only after layout
 * and pagination have already produced resolved, page-aware entities.</p>
 *
 * <p>Main responsibilities here include:</p>
 *
 * <ul>
 *   <li>opening and closing one PDF render session per render pass</li>
 *   <li>managing page-local {@code PDPageContentStream} reuse</li>
 *   <li>dispatching engine render markers to PDF-specific handlers</li>
 *   <li>owning PDF-only helper primitives such as image caching and page surfaces</li>
 * </ul>
 *
 * <p>Package policy is to keep PDFBox concerns here rather than leaking them
 * into shared engine components or builders. Engine-side render markers stay
 * backend-neutral, while this package decides how those markers are translated
 * into concrete PDFBox drawing operations.</p>
 *
 * <p>See the sibling {@code handlers} package for render-marker dispatch
 * implementations and the {@code helpers} package for PDF-only support objects
 * that are not themselves engine render markers.</p>
 */
package com.demcha.compose.engine.render.pdf;
