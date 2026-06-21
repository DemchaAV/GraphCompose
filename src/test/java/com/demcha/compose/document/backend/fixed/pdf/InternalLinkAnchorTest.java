package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Engine acceptance tests for in-PDF navigation: named anchors plus internal
 * {@code GoTo} links with two-pass (forward-reference) resolution.
 *
 * @author Artem Demchyshyn
 */
class InternalLinkAnchorTest {

    private static byte[] render(DocumentBody body) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 220)
                .margin(DocumentInsets.of(16))
                .create()) {
            body.compose(session);
            return session.toPdfBytes();
        }
    }

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
    void forwardReferenceResolvesToGoTo() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                // link appears BEFORE its target anchor (forward reference)
                .addRich(RichText.text("Jump to the ").linkTo("introduction", "intro"))
                .addParagraph(p -> p.text("Introduction body").anchor("intro"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDAnnotationLink> links = goToLinks(document, 0);
            assertThat(links).hasSize(1);
            PDActionGoTo action = (PDActionGoTo) links.get(0).getAction();
            assertThat(action.getDestination()).isInstanceOf(PDPageXYZDestination.class);
            PDPageXYZDestination dest = (PDPageXYZDestination) action.getDestination();
            assertThat(dest.retrievePageNumber()).isZero();
            assertThat(dest.getZoom()).isZero();
            assertThat(new PDFTextStripper().getText(document)).contains("introduction");
        }
    }

    @Test
    void backwardReferenceResolvesToGoTo() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addParagraph(p -> p.text("Introduction body").anchor("intro"))
                .addRich(RichText.text("Back to the ").linkTo("introduction", "intro"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(goToLinks(document, 0)).hasSize(1);
        }
    }

    @Test
    void unknownAnchorEmitsNoAnnotationButKeepsText() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addRich(RichText.text("A ").linkTo("dangling link", "does-not-exist"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getPage(0).getAnnotations()).isEmpty();
            assertThat(new PDFTextStripper().getText(document)).contains("dangling link");
        }
    }

    @Test
    void multiPageDestinationPointsAtCorrectPage() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addRich(RichText.text("Skip ahead to the ").linkTo("appendix", "appendix"))
                .addPageBreak(b -> b.name("Break"))
                .addParagraph(p -> p.text("Appendix").anchor("appendix"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
            List<PDAnnotationLink> links = goToLinks(document, 0);
            assertThat(links).hasSize(1);
            PDPageXYZDestination dest =
                    (PDPageXYZDestination) ((PDActionGoTo) links.get(0).getAction()).getDestination();
            assertThat(dest.retrievePageNumber()).isEqualTo(1);
        }
    }

    @Test
    void wrappedInternalLinkEmitsMultipleAnnotationsAllResolved() throws Exception {
        // A long internal link that wraps across lines produces several link
        // rectangles (one per span/line fragment); every one must resolve to the
        // same destination.
        String longText = "this internal link label is intentionally long so that it wraps "
                          + "across several lines inside the narrow content column";
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addRich(RichText.text("Reference: ").linkTo(longText, "target"))
                .addParagraph(p -> p.text("Target").anchor("target"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDAnnotationLink> links = goToLinks(document, 0);
            assertThat(links.size()).isGreaterThanOrEqualTo(2);
            // All wrapped annotations point at the same destination page.
            for (PDAnnotationLink link : links) {
                PDPageXYZDestination dest =
                        (PDPageXYZDestination) ((PDActionGoTo) link.getAction()).getDestination();
                assertThat(dest.retrievePageNumber()).isZero();
            }
        }
    }

    @Test
    void externalLinkStillEmitsUriAction() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addRich(RichText.text("Open ").link("the site", "https://example.com"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDAnnotationLink link = (PDAnnotationLink) document.getPage(0).getAnnotations().get(0);
            assertThat(link.getAction()).isInstanceOf(PDActionURI.class);
            assertThat(((PDActionURI) link.getAction()).getURI()).isEqualTo("https://example.com");
        }
    }

    @Test
    void sectionAnchorIsNavigable() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addRich(RichText.text("Go to ").linkTo("body", "body"))
                .addSection("Body", s -> s.anchor("body").addParagraph("Section body text"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDAnnotationLink> links = goToLinks(document, 0);
            assertThat(links).hasSize(1);
            PDPageXYZDestination dest =
                    (PDPageXYZDestination) ((PDActionGoTo) links.get(0).getAction()).getDestination();
            assertThat(dest.retrievePageNumber()).isZero();
        }
    }

    @Test
    void shapeInternalLinkIsNavigable() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addShape(s -> s.size(24, 24).fillColor(DocumentColor.of(Color.RED)).linkTo("note"))
                .addParagraph(p -> p.text("Note").anchor("note"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(goToLinks(document, 0)).hasSize(1);
        }
    }

    @Test
    void tableAnchorIsNavigable() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addRich(RichText.text("Go to ").linkTo("table", "tbl"))
                .addTable(t -> t.anchor("tbl")
                        .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                        .header("A", "B")
                        .row("1", "2"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(goToLinks(document, 0)).hasSize(1);
        }
    }

    @Test
    void duplicateAnchorKeepsLastRegistration() throws Exception {
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addParagraph(p -> p.text("First target").anchor("dup"))
                .addRich(RichText.text("Jump to ").linkTo("dup", "dup"))
                .addPageBreak(b -> b.name("Break"))
                .addParagraph(p -> p.text("Second target").anchor("dup"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDAnnotationLink> links = goToLinks(document, 0);
            assertThat(links).hasSize(1);
            PDPageXYZDestination dest =
                    (PDPageXYZDestination) ((PDActionGoTo) links.get(0).getAction()).getDestination();
            // Last registration wins: the second "dup" on page 2 (index 1).
            assertThat(dest.retrievePageNumber()).isEqualTo(1);
        }
    }

    @Test
    void anchoredParagraphSplitAcrossPagesEmitsSingleHeadDestination() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            body.append("Line ").append(i).append(" of a long anchored paragraph that must split. ");
        }
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addParagraph(p -> p.text(body.toString()).anchor("long"))
                .addRich(RichText.text("Back to ").linkTo("start", "long"))
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            int total = 0;
            int destPage = -1;
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                for (PDAnnotationLink link : goToLinks(document, page)) {
                    total++;
                    destPage = ((PDPageXYZDestination)
                            ((PDActionGoTo) link.getAction()).getDestination()).retrievePageNumber();
                }
            }
            // The anchor marker is emitted only on the split paragraph's head
            // fragment, so there is exactly one destination and it is page 0.
            assertThat(total).isEqualTo(1);
            assertThat(destPage).isZero();
        }
    }

    @Test
    void rendersInPdfNavigationDocumentArtifact() throws Exception {
        // A bidirectional footnote: body [1] -> fn-1, and the note marker -> fnref-1.
        byte[] pdf = render(session -> session.dsl().pageFlow().name("Flow")
                .addParagraph(p -> p.anchor("fnref-1")
                        .inlineText("A claim that needs evidence")
                        .inlineLinkTo("[1]", "fn-1"))
                .addParagraph(p -> p.text("Body paragraph between the reference and the notes."))
                .addParagraph(p -> p.anchor("fn-1")
                        .inlineLinkTo("[1]", "fnref-1")
                        .inlineText(" Supporting evidence for the claim."))
                .build());

        Path output = VisualTestOutputs.preparePdf("internal-links", "internal-links");
        Files.write(output, pdf);
        assertThat(Files.size(output)).isPositive();
        assertThat(new String(pdf, 0, 8, java.nio.charset.StandardCharsets.US_ASCII)).startsWith("%PDF-");
    }

    @FunctionalInterface
    private interface DocumentBody {
        void compose(DocumentSession session);
    }
}
