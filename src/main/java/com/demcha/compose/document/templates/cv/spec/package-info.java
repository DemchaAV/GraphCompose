/**
 * Templates v2 CV specification records — user-facing data types.
 *
 * <p>This package holds the immutable records a user fills with their
 * CV content before passing the spec to a preset for rendering:</p>
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
 * <p>This is the v2 replacement for the legacy
 * {@code com.demcha.compose.document.templates.data.cv.*} mutable
 * Lombok beans. The legacy package remains during the migration
 * window and will be removed in Phase G.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.cv.spec;
