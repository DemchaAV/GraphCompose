package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * End-to-end proof for {@code addTableOfContents(...)}: each entry prints its
 * anchor's resolved page (forward references), is a clickable go-to link to that
 * page, and a long table of contents paginates across pages.
 */
class TocTest {

    @TempDir
    Path tempDir;

    private static String pageText(PDDocument doc, int oneBasedPage) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(oneBasedPage);
        stripper.setEndPage(oneBasedPage);
        return stripper.getText(doc);
    }

    private static List<Integer> goToTargetPages(PDDocument doc, int zeroBasedPage) throws IOException {
        List<Integer> targets = new ArrayList<>();
        for (PDAnnotation annotation : doc.getPage(zeroBasedPage).getAnnotations()) {
            if (annotation instanceof PDAnnotationLink link && link.getAction() instanceof PDActionGoTo goTo
                    && goTo.getDestination() instanceof PDPageDestination dest) {
                targets.add(dest.retrievePageNumber());
            }
        }
        return targets;
    }

    @Test
    void tableOfContentsPrintsEachEntrysResolvedPage() throws Exception {
        Path out = tempDir.resolve("toc.pdf");
        int intro;
        int appendix;
        try (DocumentSession session = GraphCompose.document(out)
                .pageSize(260, 200)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.pageFlow(page -> {
                page.addTableOfContents(toc -> toc.title("Contents")
                        .entry("Introduction", "intro")
                        .entry("Appendix", "appendix"));
                page.addPageBreak(b -> b.name("b1"));
                page.addSection(s -> s.anchor("intro").addParagraph("Introduction"));
                page.addPageBreak(b -> b.name("b2"));
                page.addSection(s -> s.anchor("appendix").addParagraph("Appendix"));
            });
            intro = session.pageIndex().pageNumberOf("intro").orElseThrow();
            appendix = session.pageIndex().pageNumberOf("appendix").orElseThrow();
            session.buildPdf();
        }

        assertThat(intro).isNotEqualTo(appendix);
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(pageText(doc, 1)).contains("Contents", "Introduction", "Appendix",
                    Integer.toString(intro), Integer.toString(appendix));
        }
    }

    @Test
    void tableOfContentsEntriesLinkToTheirAnchorPages() throws Exception {
        Path out = tempDir.resolve("toc-links.pdf");
        int intro;
        int appendix;
        try (DocumentSession session = GraphCompose.document(out)
                .pageSize(260, 200)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.pageFlow(page -> {
                page.addTableOfContents(toc -> toc.entry("Introduction", "intro").entry("Appendix", "appendix"));
                page.addPageBreak(b -> b.name("b1"));
                page.addSection(s -> s.anchor("intro").addParagraph("Introduction"));
                page.addPageBreak(b -> b.name("b2"));
                page.addSection(s -> s.anchor("appendix").addParagraph("Appendix"));
            });
            intro = session.pageIndex().pageOf("intro").orElseThrow();        // zero-based
            appendix = session.pageIndex().pageOf("appendix").orElseThrow();
            session.buildPdf();
        }

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            // Each entry label is a go-to link jumping to its anchor's page.
            assertThat(goToTargetPages(doc, 0)).contains(intro, appendix);
        }
    }

    @Test
    void longTableOfContentsPaginatesAcrossPages() {
        try (DocumentSession session = GraphCompose.document(tempDir.resolve("toc-long.pdf"))
                .pageSize(240, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> {
                page.addTableOfContents(toc -> {
                    toc.title("Contents");
                    for (int i = 1; i <= 30; i++) {
                        toc.entry("Chapter " + i, "target");
                    }
                });
                page.addPageBreak(b -> b.name("b1"));
                page.addSection(s -> s.anchor("target").addParagraph("Target"));
            });
            // 30 entry rows do not fit one page: the flow paginates them rather
            // than overflowing a single atomic block.
            assertThatCode(session::buildPdf).doesNotThrowAnyException();
            assertThat(session.layoutGraph().totalPages()).isGreaterThan(1);
        }
    }
}
