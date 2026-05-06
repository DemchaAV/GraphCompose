/**
 * Templates v2 theme tokens — spacing, typography, and palette primitives.
 *
 * <p>This package holds the small value records that Templates v2 presets
 * use to externalise spacing, font, and colour decisions. The goal is one
 * source of truth for each token group, so that swapping a token swaps the
 * decision everywhere it is used:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.themes.Spacing} —
 *       moduleGap, lineSpacing, paragraphSpacing, sectionTitleAbove/Below,
 *       headerLineSpacing, listItemSpacing, and content padding.</li>
 *   <li>{@link com.demcha.compose.document.templates.themes.Typography} —
 *       header/body fonts plus six logical type sizes (name, heading,
 *       sub-heading, body, small body, caption).</li>
 * </ul>
 *
 * <p>Future Templates v2 phases will add {@code Palette} (full colour
 * tokens) and migrate {@code BusinessTheme} from the legacy
 * {@code templates.theme} package alongside these tokens.</p>
 *
 * <p>This package is purely value-record types; it has no dependencies on
 * the templates engine, the layout compiler, or any session-side state.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.themes;
