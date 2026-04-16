/**
 * Deprecated bridge package for the original V2 semantic document experiment.
 *
 * <p>The canonical public API now lives under
 * {@link com.demcha.compose.document.api},
 * {@link com.demcha.compose.document.dsl},
 * {@link com.demcha.compose.document.model},
 * {@link com.demcha.compose.document.layout}, and the related
 * {@code com.demcha.compose.document.backend.*} packages.</p>
 *
 * <p>This package tree remains available for one transition release so existing
 * integrations can migrate incrementally, but new code should not import
 * {@code com.demcha.compose.v2.*} as its primary authoring surface.</p>
 */
@Deprecated(forRemoval = false)
package com.demcha.compose.v2;
