/**
 * Bidirectional text resolution for the layout pipeline.
 *
 * <p>Ownership: shared engine foundation. The types here work on plain strings and
 * embedding levels and know nothing about the document surface; callers in
 * {@code com.demcha.compose.document.layout} translate the public direction option
 * into the base direction this package understands.</p>
 */
package com.demcha.compose.engine.text.bidi;
