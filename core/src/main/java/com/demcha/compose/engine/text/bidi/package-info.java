/**
 * Bidirectional text resolution for the layout pipeline.
 *
 * <p>Ownership: shared engine foundation. The types here work on plain strings and
 * embedding levels and know nothing about the document surface; callers in
 * {@code com.demcha.compose.document.layout} translate the public direction option
 * into the base direction this package understands.</p>
 *
 * <p><strong>Internal API.</strong> Tagged {@link com.demcha.compose.document.api.Internal}
 * at the package level: the prose above has always said so, but only the annotation is
 * something a guard test can read, and without it these types look from the outside like
 * a supported surface.</p>
 */
@com.demcha.compose.document.api.Internal
package com.demcha.compose.engine.text.bidi;
