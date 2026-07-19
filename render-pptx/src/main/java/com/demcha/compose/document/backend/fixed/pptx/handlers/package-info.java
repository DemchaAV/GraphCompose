/**
 * Fragment render handlers for the fixed-layout PPTX backend: one small,
 * stateless handler per payload type, dispatched by
 * {@link com.demcha.compose.document.backend.fixed.pptx.PptxFixedLayoutBackend}
 * in graph order — the PPTX twin of the PDF backend's handler package.
 *
 * <p><b>Experimental</b> ({@code @Beta}): the whole package ships its first
 * release in 2.1.0 — contracts may still change in a minor release; see
 * {@code docs/api-stability.md}.</p>
 *
 * @since 2.1.0
 */
@Beta
package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.api.Beta;
