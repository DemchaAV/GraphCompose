/**
 * The Word export: a document written as an editable {@code .docx}, through Apache POI.
 *
 * <p>{@link com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend} is a
 * semantic backend. It walks the document graph and writes content Word owns — paragraphs,
 * lists, tables, pictures, headers and footers — so Word paginates the text and reflows it
 * after an edit. It also asks for the session's resolved layout, and reads from it what Word
 * cannot work out on its own: how tall a line is set, how wide an {@code auto} column came out,
 * where a drawing stands, which blocks the page kept together. A document handed no layout is
 * still exported, from what it states, but without its drawings and page backgrounds, which
 * need the layout; the report names each of them and what else that leaves to Word.</p>
 *
 * <p>A chart is written as a Word chart, its numbers in a workbook embedded with it; one
 * composed in a table cell beside other drawings, or in a table composed in a cell, is drawn
 * with them instead, over a table of its data. What a Word document cannot hold, or holds only as the nearest thing it owns, is named in a
 * {@link com.demcha.compose.document.backend.semantic.docx.DocxExportReport}, with the path of
 * the node it came from. A backend built with {@code deterministic(true)} writes the same bytes
 * for the same document on every run on one platform; on another platform the bytes differ
 * where Apache POI ends an XML declaration with that platform's line separator.</p>
 *
 * <p>{@link com.demcha.compose.document.backend.semantic.docx.DocxBackendProvider} registers
 * the export with {@link java.util.ServiceLoader} under the format {@code "docx"}, so a
 * session's {@code buildDocx}, {@code writeDocx} and {@code toDocxBytes} find it once this
 * artifact is on the classpath. Those three are the only public types; everything else in the
 * package is package-private.</p>
 *
 * <p><b>Stability.</b> The package carries no package-level marker, because it is not
 * Experimental as a whole: constructing {@code DocxSemanticBackend} and calling
 * {@code export(graph, context)} is Stable. What 2.5.0 added is Experimental ({@code @Beta})
 * element by element — {@code DocxBackendProvider}; {@code DocxExportReport} with its
 * {@code Note} and {@code Severity}; and, on the backend, the constructor that takes a report
 * sink, {@code builder()} and its {@code Builder}, and the {@code requiresResolvedLayout} and
 * {@code exportSections} overrides. See {@code docs/api-stability.md}.</p>
 */
package com.demcha.compose.document.backend.semantic.docx;
