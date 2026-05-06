/**
 * Templates v2 CV presets — flat copy-and-tweak recipe classes.
 *
 * <p>Each preset is a small final class with one static
 * {@code create(BusinessTheme)} factory method whose body fully
 * configures a {@link com.demcha.compose.document.templates.cv.builder.CvBuilder}
 * to produce a ready-to-use
 * {@link com.demcha.compose.document.templates.api.DocumentTemplate}.
 * No inheritance, no abstract base — every visual choice is visible
 * in the preset's source.</p>
 *
 * <p>To customise a preset: copy the {@code create(...)} method body
 * into your own class and adjust the {@code CvBuilder} calls (slot
 * placements, module style, spacing tokens, layout choice). The
 * surrounding session lifecycle is unchanged.</p>
 *
 * <p>Phase D ships the pilot preset
 * {@link com.demcha.compose.document.templates.cv.presets.ModernProfessional};
 * the remaining 13 CV presets land in Phase E of the Templates v2
 * migration.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.cv.presets;
