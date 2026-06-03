/**
 * Superseded Gen-2 cover-letter specification records — user-facing data
 * types.
 *
 * <p><strong>Deprecated surface.</strong> These are the older Gen-2
 * cover-letter spec records. They are <em>not</em> the current standard. The
 * current standard is the layered model
 * {@link com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument}
 * in the {@code com.demcha.compose.document.templates.coverletter.v2}
 * surface. This package is kept only for backward compatibility and is
 * scheduled for removal in a future major.</p>
 *
 * <p>This package holds the immutable records a user fills with their
 * cover-letter content before passing the spec to a Gen-2 preset for
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
 * <p>New code should target the layered {@code coverletter.v2} data model
 * instead. See {@code docs/templates/v2-layered/}.</p>
 *
 * @since 1.6.0
 */
@Deprecated(since = "1.7.0", forRemoval = true)
package com.demcha.compose.document.templates.coverletter.spec;
