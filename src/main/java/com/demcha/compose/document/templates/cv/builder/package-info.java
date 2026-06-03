/**
 * Superseded Gen-2 CV preset builder — fluent assembly of
 * {@link com.demcha.compose.document.templates.api.DocumentTemplate}
 * instances from layouts, components, and spec data.
 *
 * <p><strong>Deprecated surface.</strong> This is the older Gen-2 CV
 * builder. It is <em>not</em> the current standard. The current standard is
 * the layered surface
 * {@code com.demcha.compose.document.templates.cv.v2} (data / theme /
 * components / widgets / presets). This package is kept only for backward
 * compatibility and is scheduled for removal in a future major.</p>
 *
 * <p>The single class of interest is
 * {@link com.demcha.compose.document.templates.cv.builder.CvBuilder}.
 * Preset classes wrap one builder call inside their
 * {@code create(BusinessTheme)} factory.</p>
 *
 * <p>New code should target the layered {@code cv.v2} surface instead. See
 * {@code docs/templates/v2-layered/}.</p>
 *
 * @since 1.6.0
 */
@Deprecated(since = "1.7.0", forRemoval = true)
package com.demcha.compose.document.templates.cv.builder;
