/**
 * Layered rota presets — thin orchestrators that compose a staff rota on the
 * canonical DSL.
 *
 * <p>Each preset is a {@code final} class whose {@code create} factory returns a
 * {@link com.demcha.compose.document.templates.api.DocumentTemplate}
 * parameterised on
 * {@link com.demcha.compose.document.templates.data.rota.StructuredRotaDocumentSpec},
 * carrying its measured geometry and palette as preset-local tokens because it
 * reproduces one designed look rather than a themeable family
 * ({@code CobaltRota}).</p>
 *
 * <p>A rota is read across a row rather than down a page, so a preset here is
 * built on one table whose columns are the document's own days. Colour belongs
 * to the preset and meaning to the document: the model states a
 * {@link com.demcha.compose.document.templates.data.rota.ShiftStatus} and the
 * preset decides what that status looks like.</p>
 *
 * @since 2.4.0
 */
package com.demcha.compose.document.templates.rota.presets;
