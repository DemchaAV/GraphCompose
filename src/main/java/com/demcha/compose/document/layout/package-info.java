/**
 * Semantic graph preparation, layout compilation, pagination, and fragment emission.
 *
 * <p>This package translates semantic nodes into prepared measurements, paged
 * placements, and renderer-facing fragment payloads.</p>
 *
 * <p><strong>Internal API.</strong> Types in this package are tagged
 * {@link com.demcha.compose.document.api.Internal} at the package level
 * because they are implementation detail of the layout pipeline and may
 * change in any release without notice. Library users should not depend
 * on these types &mdash; build against {@code DocumentSession},
 * {@code DocumentDsl}, {@code BusinessTheme}, the template APIs, or the
 * {@link com.demcha.compose.document.backend} SPIs instead.</p>
 *
 * <p>Architecture-guard tests may inspect this annotation reflectively
 * to enforce the boundary.</p>
 */
@com.demcha.compose.document.api.Internal
package com.demcha.compose.document.layout;
