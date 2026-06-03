/**
 * Superseded Gen-2 CV presets — flat copy-and-tweak recipe classes.
 *
 * <p><strong>Deprecated surface.</strong> These are the older Gen-2 CV
 * presets. They are <em>not</em> the current standard. The current standard
 * is the layered surface
 * {@code com.demcha.compose.document.templates.cv.v2.presets}. This package
 * is kept only for backward compatibility and is scheduled for removal in a
 * future major.</p>
 *
 * <p>Each preset is a small final class with one static
 * {@code create(BusinessTheme)} factory method whose body fully
 * configures a {@link com.demcha.compose.document.templates.cv.builder.CvBuilder}
 * to produce a ready-to-use
 * {@link com.demcha.compose.document.templates.api.DocumentTemplate}.</p>
 *
 * <p>New code should target the layered {@code cv.v2} presets instead. See
 * {@code docs/templates/v2-layered/}.</p>
 *
 * @since 1.6.0
 */
@Deprecated(since = "1.7.0", forRemoval = true)
package com.demcha.compose.document.templates.cv.presets;
