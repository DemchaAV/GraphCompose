/**
 * Document-building discovery helpers. {@link com.demcha.compose.document.showcase.FontShowcase}
 * renders a preview of the bundled font families by authoring a document through
 * the canonical DSL, so it lives in the document layer (it needs a render backend
 * to produce output) rather than the backend-neutral {@code com.demcha.compose.font}
 * catalog. Reached through {@code GraphCompose.renderAvailableFontsPreview(...)}.
 */
package com.demcha.compose.document.showcase;
