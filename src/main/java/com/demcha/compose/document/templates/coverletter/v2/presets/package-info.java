/**
 * Templates v2 cover-letter presets — one per paired CV preset.
 *
 * <p>Each preset is a thin orchestrator that reads colour, font, and
 * spacing from its paired {@code CvTheme.<brand>()} (the single source
 * of truth shared with the CV), renders the same masthead treatment as
 * the CV, and delegates the letter body to the shared
 * {@code coverletter.v2.components.LetterBody}. The result is a CV and a
 * cover letter that read as one matched set.</p>
 */
package com.demcha.compose.document.templates.coverletter.v2.presets;
