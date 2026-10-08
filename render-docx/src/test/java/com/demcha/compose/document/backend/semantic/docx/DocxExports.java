package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Exports a small document both ways: with the compiled layout behind it, and without.
 *
 * <p>The backend asks for the resolved layout and uses it where it has one — measured line
 * heights, resolved column widths — so a session export and a bare export are two different
 * outputs of the same writer. Both are real: a session is how the export is reached, and a
 * caller holding a graph and a canvas can call {@code export} directly, which is also what
 * happens when a document cannot be laid out at all. Tests that pin the fallback have to
 * ask for it.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxExports {

    /**
     * How far a line of the default text — Helvetica at 14pt — is raised into the space written
     * above it, in twips: the page seats its baseline 10.05pt down its 12.95pt line, Word four
     * fifths of the way down, 0.31pt lower ({@code DocxBaselineSeatTest} holds it to that). A
     * test of the space between blocks of default text reads the space above such a line as that
     * much less, and the space below it as that much more.
     */
    static final long DEFAULT_LINE_RAISE = 6;
    /** {@link #DEFAULT_LINE_RAISE} for the default face at 7pt, the size tests set small text in. */
    static final long SMALL_LINE_RAISE = 3;

    private DocxExports() {
    }

    /** Exports through a session, so the backend is handed the compiled layout. */
    static XWPFDocument withLayout(double pageWidth, double pageHeight, double margin,
                                   Consumer<PageFlowBuilder> content) throws Exception {
        byte[] docx;
        try (DocumentSession session = session(pageWidth, pageHeight, margin, content)) {
            docx = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    /** Exports the same document with no layout behind it, the way a bare caller does. */
    static XWPFDocument withoutLayout(double pageWidth, double pageHeight, double margin,
                                      Consumer<PageFlowBuilder> content) throws Exception {
        Captured captured = new Captured();
        byte[] docx;
        try (DocumentSession session = session(pageWidth, pageHeight, margin, content)) {
            session.export(captured);
            docx = new DocxSemanticBackend().export(captured.graph,
                    new SemanticExportContext(captured.canvas, List.of(), null, null));
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    /** The report of an export with no layout behind it, the way a bare caller makes one. */
    static DocxExportReport reportWithoutLayout(double pageWidth, double pageHeight, double margin,
                                                Consumer<PageFlowBuilder> content) throws Exception {
        Captured captured = new Captured();
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report =
                new java.util.concurrent.atomic.AtomicReference<>();
        try (DocumentSession session = session(pageWidth, pageHeight, margin, content)) {
            session.export(captured);
            new DocxSemanticBackend(report::set).export(captured.graph,
                    new SemanticExportContext(captured.canvas, List.of(), null, null));
        }
        return report.get();
    }

    /** Exports with neither a layout nor a canvas, as a caller holding only the graph does. */
    static XWPFDocument withoutCanvas(Consumer<PageFlowBuilder> content) throws Exception {
        Captured captured = new Captured();
        byte[] docx;
        try (DocumentSession session = session(595, 842, 36, content)) {
            session.export(captured);
            docx = new DocxSemanticBackend().export(captured.graph,
                    new SemanticExportContext(null, List.of(), null, null));
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    private static DocumentSession session(double pageWidth, double pageHeight, double margin,
                                           Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document()
                .pageSize(pageWidth, pageHeight)
                .margin(DocumentInsets.of(margin))
                .create();
        session.pageFlow(content::accept);
        return session;
    }

    /** Takes the graph and the canvas the session would hand any backend. */
    private static final class Captured implements SemanticBackend<byte[]> {

        private DocumentGraph graph;
        private LayoutCanvas canvas;

        @Override
        public String name() {
            return "capture";
        }

        @Override
        public byte[] export(DocumentGraph documentGraph, SemanticExportContext context) {
            this.graph = documentGraph;
            this.canvas = context.canvas();
            return new byte[0];
        }
    }
}
