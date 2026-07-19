/**
 * Coordinate-exact PPTX render backend consuming the resolved
 * {@link com.demcha.compose.document.layout.LayoutGraph} — the fixed-layout
 * twin of the PDF backend, sharing identical geometry by construction. The
 * only place Apache POI slide-show lifecycle is allowed in the backend
 * package tree; drawing itself lives in the
 * {@code document.backend.fixed.pptx.handlers} package.
 *
 * <p><b>Experimental</b> ({@code @Beta}): the whole package ships its first
 * release in 2.1.0 — contracts may still change in a minor release; see
 * {@code docs/api-stability.md}.</p>
 *
 * @since 2.1.0
 */
@Beta
package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.api.Beta;
