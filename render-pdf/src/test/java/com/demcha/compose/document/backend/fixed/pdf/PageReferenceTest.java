package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * End-to-end proof that {@code addPageReference(anchor)} prints the page a forward
 * anchor lands on: the reference sits on page 1, its target several pages later,
 * and the rendered number on page 1 equals {@code pageIndex().pageNumberOf} — so
 * the two-pass resolve converges and is backend-correct.
 */
class PageReferenceTest {

    @TempDir
    Path tempDir;

    private static String pageText(PDDocument doc, int oneBasedPage) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(oneBasedPage);
        stripper.setEndPage(oneBasedPage);
        return stripper.getText(doc);
    }

    @Test
    void pageReferencePrintsTheForwardAnchorsResolvedPage() throws Exception {
        Path out = tempDir.resolve("page-ref.pdf");
        int resolved;
        try (DocumentSession session = GraphCompose.document(out)
                .pageSize(220, 180)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.pageFlow(page -> {
                page.addRow(r -> r.gap(4).columns(com.demcha.compose.document.style.DocumentRowColumn.auto(),
                                com.demcha.compose.document.style.DocumentRowColumn.auto())
                        .addParagraph("Appendix on page")
                        .addPageReference("appendix", DocumentTextStyle.DEFAULT, TextAlign.LEFT));
                page.addPageBreak(b -> b.name("b1"));
                page.addParagraph("Body");
                page.addPageBreak(b -> b.name("b2"));
                page.addSection(s -> s.anchor("appendix").addParagraph("Appendix"));
            });
            resolved = session.pageIndex().pageNumberOf("appendix").orElseThrow();
            session.buildPdf();
        }

        assertThat(resolved).isGreaterThan(1);                          // a genuine forward reference
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            // The reference on page 1 prints exactly the page pageIndex() resolved.
            assertThat(pageText(doc, 1)).contains("Appendix on page", Integer.toString(resolved));
        }
    }

    @Test
    void multipleReferencesEachReportTheirFinalPage() throws Exception {
        Path out = tempDir.resolve("multi-ref.pdf");
        int appendix;
        int glossary;
        try (DocumentSession session = GraphCompose.document(out)
                .pageSize(220, 180)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.pageFlow(page -> {
                page.addRow(r -> r.gap(4).addParagraph("Appendix")
                        .addPageReference("appendix", DocumentTextStyle.DEFAULT, TextAlign.RIGHT));
                page.addRow(r -> r.gap(4).addParagraph("Glossary")
                        .addPageReference("glossary", DocumentTextStyle.DEFAULT, TextAlign.RIGHT));
                page.addPageBreak(b -> b.name("b1"));
                page.addSection(s -> s.anchor("appendix").addParagraph("Appendix"));
                page.addPageBreak(b -> b.name("b2"));
                page.addSection(s -> s.anchor("glossary").addParagraph("Glossary"));
            });
            appendix = session.pageIndex().pageNumberOf("appendix").orElseThrow();
            glossary = session.pageIndex().pageNumberOf("glossary").orElseThrow();
            session.buildPdf();
        }

        assertThat(appendix).isNotEqualTo(glossary);                   // distinct target pages
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            // Each reference on page 1 prints the page its own anchor finally lands on.
            assertThat(pageText(doc, 1)).contains(Integer.toString(appendix), Integer.toString(glossary));
        }
    }

    @Test
    void unresolvedReferenceRendersWithoutThrowing() {
        try (DocumentSession session = GraphCompose.document(tempDir.resolve("unresolved.pdf"))
                .pageSize(220, 180)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.pageFlow(page -> {
                page.addParagraph("No such anchor below");
                page.addPageReference("missing");
            });
            // An unknown anchor resolves to empty → renders its placeholder, no throw.
            assertThat(session.pageIndex().pageNumberOf("missing")).isEmpty();
            assertThatCode(session::buildPdf).doesNotThrowAnyException();
        }
    }
}
