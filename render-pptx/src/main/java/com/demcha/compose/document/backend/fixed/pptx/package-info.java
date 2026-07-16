/**
 * Coordinate-exact PPTX render backend consuming the resolved
 * {@link com.demcha.compose.document.layout.LayoutGraph} — the fixed-layout
 * twin of the PDF backend, sharing identical geometry by construction. The
 * only place Apache POI slide-show lifecycle is allowed in the backend
 * package tree; drawing itself lives in the
 * {@code document.backend.fixed.pptx.handlers} package.
 *
 * @since 2.1.0
 */
package com.demcha.compose.document.backend.fixed.pptx;
