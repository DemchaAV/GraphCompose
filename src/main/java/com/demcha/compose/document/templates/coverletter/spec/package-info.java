/**
 * Templates v2 cover-letter specification records — user-facing data
 * types.
 *
 * <p>This package holds the immutable records a user fills with their
 * cover-letter content before passing the spec to a preset for
 * rendering:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.coverletter.spec.CoverLetterHeader}
 *       — name, address, phone, email, links (with builder).</li>
 *   <li>{@link com.demcha.compose.document.templates.coverletter.spec.CoverLetterSpec}
 *       — header plus greeting, body paragraphs, and closing line.
 *       Body strings may carry markdown markers ({@code **bold**},
 *       {@code *italic*}) for inline emphasis.</li>
 * </ul>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.coverletter.spec;
