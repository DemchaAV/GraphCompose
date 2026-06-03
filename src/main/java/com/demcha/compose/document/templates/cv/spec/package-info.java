/**
 * Superseded Gen-2 CV specification records — user-facing data types.
 *
 * <p><strong>Deprecated surface.</strong> These are the older Gen-2 CV spec
 * records. They are <em>not</em> the current standard. The current standard
 * is the layered model
 * {@link com.demcha.compose.document.templates.cv.v2.data.CvDocument} in the
 * {@code com.demcha.compose.document.templates.cv.v2} surface. This package
 * is kept only for backward compatibility and is scheduled for removal in a
 * future major.</p>
 *
 * <p>This package holds the immutable records a user fills with their
 * CV content before passing the spec to a Gen-2 preset for rendering:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.cv.spec.CvHeader}
 *       — name, address, phone, email, links (with builder).</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.spec.CvModule}
 *       — one named section: {@code name} (lookup key), {@code title}
 *       (rendered heading), {@code body} (Block).</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.spec.CvSpec}
 *       — header plus ordered list of modules; rejects duplicate
 *       module names; provides {@code findModule(name)} for preset-side
 *       lookups.</li>
 * </ul>
 *
 * <p>New code should target the layered {@code cv.v2} data model instead. See
 * {@code docs/templates/v2-layered/}.</p>
 *
 * @since 1.6.0
 */
@Deprecated(since = "1.7.0", forRemoval = true)
package com.demcha.compose.document.templates.cv.spec;
