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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the backend-neutral guarantee: {@code DocumentSession.pageIndex()} resolves
 * an anchor to the same page the rendered PDF's {@code linkTo(anchor)} go-to
 * action jumps to. The read API (anchor markers in the layout graph) and the PDF
 * destination writer are independent passes over the same fragments; this test
 * fails if they ever diverge.
 */
class PageIndexAnchorAgreementTest {

    @TempDir
    Path tempDir;

    private static List<PDAnnotationLink> goToLinks(PDDocument document, int pageIndex) throws IOException {
        List<PDAnnotationLink> result = new ArrayList<>();
        for (PDAnnotation annotation : document.getPage(pageIndex).getAnnotations()) {
            if (annotation instanceof PDAnnotationLink link && link.getAction() instanceof PDActionGoTo) {
                result.add(link);
            }
        }
        return result;
    }

    @Test
    void pageIndexResolvesToTheSamePageTheGoToDestinationTargets() throws Exception {
        Path out = tempDir.resolve("agreement.pdf");

        int pageIndexTarget;
        try (DocumentSession session = GraphCompose.document(out)
                .pageSize(220, 140)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow(page -> {
                page.addParagraph(p -> p.inlineLinkTo("see target", "target"));   // GoTo link on page 1
                page.addPageBreak(b -> b.name("brk"));
                page.addSection(s -> s.anchor("target").addParagraph("Target"));    // anchor on page 2
            });
            pageIndexTarget = session.pageIndex().pageOf("target").orElseThrow();
            session.buildPdf();
        }

        try (PDDocument document = Loader.loadPDF(out.toFile())) {
            List<PDAnnotationLink> links = goToLinks(document, 0);
            assertThat(links).isNotEmpty();
            // Every go-to annotation for the anchor targets the page pageIndex() reported.
            for (PDAnnotationLink link : links) {
                PDPageDestination destination =
                        (PDPageDestination) ((PDActionGoTo) link.getAction()).getDestination();
                assertThat(destination.retrievePageNumber()).isEqualTo(pageIndexTarget);
            }
        }
    }
}
