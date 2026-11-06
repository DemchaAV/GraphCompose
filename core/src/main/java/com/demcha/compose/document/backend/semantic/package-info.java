/**
 * Semantic export: backends that write a document's structure into an editable format, rather
 * than drawing it page by page.
 *
 * <p>A {@link com.demcha.compose.document.backend.semantic.SemanticBackend} is handed the
 * document graph — the authored tree — and writes from it the format's own structure, which the
 * application that opens a written file paginates. A backend that answers
 * {@link com.demcha.compose.document.backend.semantic.SemanticBackend#requiresResolvedLayout()}
 * with {@code true} is also handed the same session's compiled layout, in
 * {@link com.demcha.compose.document.backend.semantic.SemanticExportContext#layoutGraph()}, to
 * read measurements the engine has already made; where the document could not be laid out,
 * {@link com.demcha.compose.document.backend.semantic.SemanticExportContext#layoutFailure()}
 * says why. A semantic backend reads the layout to fill in what its format leaves open — for
 * Word, how tall a line is or where a floating drawing stands; a backend that draws every page
 * at the coordinates the layout gives is a fixed-layout backend instead.</p>
 *
 * <p>{@link com.demcha.compose.document.backend.semantic.SemanticBackendProvider} and
 * {@link com.demcha.compose.document.backend.semantic.SemanticBackendProviders} let a session
 * find a backend by its format through {@link java.util.ServiceLoader}, and
 * {@link com.demcha.compose.document.backend.semantic.SemanticSection} carries one section of a
 * multi-section document to
 * {@link com.demcha.compose.document.backend.semantic.SemanticBackend#exportSections}.</p>
 *
 * <p>The backends live in the render modules. {@code DocxSemanticBackend}, in
 * {@code graph-compose-render-docx}, writes an editable Word file. {@code PptxSemanticBackend},
 * in {@code graph-compose-render-pptx}, writes no file: it checks that every node is one it
 * supports and returns a
 * {@link com.demcha.compose.document.backend.semantic.SemanticExportManifest} of them. Slides
 * come from the fixed-layout PPTX backend.</p>
 *
 * <p><b>Stability.</b> The package is Stable and carries no package-level marker. What 2.5.0
 * added is Experimental ({@code @Beta}) element by element: {@code SemanticBackendProvider},
 * {@code SemanticBackendProviders} and {@code SemanticSection};
 * {@code SemanticBackend.requiresResolvedLayout} and {@code exportSections}; and the
 * {@code layoutGraph} and {@code layoutFailure} components of {@code SemanticExportContext},
 * the canonical constructor that takes them, and {@code requireLayoutGraph}. See
 * {@code docs/api-stability.md}.</p>
 */
package com.demcha.compose.document.backend.semantic;
