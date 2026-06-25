package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * End-to-end proof for {@code GraphCompose.documents()}: several sections with
 * their own page geometry and chrome are concatenated into one PDF, navigation
 * resolves across section boundaries, and each section is numbered from its own
 * first page.
 */
class MultiSectionDocumentTest {

    @TempDir
    Path tempDir;

    private static DocumentSession cover() {
        DocumentSession cover = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(24))
                .create();
        // A clickable jump to an anchor declared in a LATER section.
        cover.pageFlow(page -> page.addParagraph(p -> p.text("Go to the introduction").linkTo("intro")));
        return cover;
    }

    private static DocumentSession body() {
        DocumentSession body = GraphCompose.document()
                .pageSize(240, 360)
                .margin(DocumentInsets.of(24))
                .create();
        body.footer(DocumentHeaderFooter.builder().centerText("Page {page} of {pages}").build());
        body.pageFlow(page -> {
            page.addSection(s -> s.anchor("intro").addParagraph("Introduction"));
            page.addPageBreak(b -> b.name("body-break"));
            page.addSection(s -> s.addParagraph("More body"));
        });
        return body;
    }

    private static String pageText(PDDocument doc, int oneBasedPage) throws IOException {
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
    void concatenatesSectionsKeepingEachSectionsPageSize() throws Exception {
        Path out = tempDir.resolve("mixed.pdf");
        try (MultiSectionDocument doc = GraphCompose.documents(out)
                .section(cover())
                .section(body())
                .create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            // cover (1 page) + body (2 pages)
            assertThat(pdf.getNumberOfPages()).isEqualTo(3);
            assertThat(pdf.getPage(0).getMediaBox().getWidth()).isEqualTo(300f);
            assertThat(pdf.getPage(1).getMediaBox().getWidth()).isEqualTo(240f);
            assertThat(pdf.getPage(2).getMediaBox().getWidth()).isEqualTo(240f);
        }
    }

    @Test
    void linksResolveAcrossSections() throws Exception {
        Path out = tempDir.resolve("cross-link.pdf");
        try (MultiSectionDocument doc = GraphCompose.documents(out)
                .section(cover())
                .section(body())
                .create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            // The cover's link targets "intro", which lives on the body's first
            // page — global (zero-based) page index 1 in the combined document.
            assertThat(goToTargetPages(pdf, 0)).contains(1);
        }
    }

    @Test
    void numbersEachSectionFromItsOwnFirstPage() throws Exception {
        Path out = tempDir.resolve("numbering.pdf");
        try (MultiSectionDocument doc = GraphCompose.documents(out)
                .section(cover())
                .section(body())
                .create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            // Body's first page is the document's 2nd page, yet its footer counts
            // from the section's own first page: "Page 1 of 2", not "Page 2 of 3".
            assertThat(pageText(pdf, 2)).contains("Page 1 of 2");
            assertThat(pageText(pdf, 3)).contains("Page 2 of 2");
            // The cover has no footer.
            assertThat(pageText(pdf, 1)).doesNotContain("Page");
        }
    }

    @Test
    void bookmarkAndExternalLinkInALaterSectionRebaseToGlobalPages() throws Exception {
        Path out = tempDir.resolve("rebased-chrome.pdf");
        DocumentSession cover = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));

        DocumentSession body = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        body.pageFlow(page -> page.addParagraph(p -> p.text("Chapter body")
                .bookmark(new DocumentBookmarkOptions("Chapter"))
                .link(new DocumentLinkOptions("https://example.com/chapter"))));

        try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            // The bookmark and the external link live in the SECOND section, so
            // both must land on the body's global page (index 1), not page 0.
            PDDocumentOutline outline = pdf.getDocumentCatalog().getDocumentOutline();
            PDOutlineItem chapter = outline.getFirstChild();
            assertThat(chapter.getTitle()).isEqualTo("Chapter");
            assertThat(((PDPageDestination) chapter.getDestination()).retrievePageNumber()).isEqualTo(1);

            assertThat(pdf.getPage(0).getAnnotations()).noneMatch(a -> a instanceof PDAnnotationLink);
            assertThat(pdf.getPage(1).getAnnotations())
                    .anyMatch(a -> a instanceof PDAnnotationLink link && link.getAction() instanceof PDActionURI);
        }
    }

    @Test
    void duplicateAnchorAcrossSectionsResolvesToTheLastSection() throws Exception {
        Path out = tempDir.resolve("dup-anchor.pdf");
        DocumentSession first = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        first.pageFlow(page -> page.addSection(s -> s.anchor("dup")
                .addParagraph(p -> p.text("Jump to dup").linkTo("dup"))));

        DocumentSession second = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        second.pageFlow(page -> page.addSection(s -> s.anchor("dup").addParagraph("Second dup")));

        try (MultiSectionDocument doc = GraphCompose.documents(out).section(first).section(second).create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            // "dup" is declared in both sections; the last registration wins, so
            // the link on page 0 jumps to the second section's page (index 1).
            assertThat(goToTargetPages(pdf, 0)).contains(1);
        }
    }

    @Test
    void sectionSnapshotsReturnsOnePerSection() {
        try (MultiSectionDocument doc = GraphCompose.documents()
                .section(cover())
                .section(body())
                .create()) {
            assertThat(doc.sectionSnapshots()).hasSize(2);
        }
    }

    @Test
    void builderRejectsAnEmptyDocument() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> GraphCompose.documents().create())
                .withMessageContaining("at least one section");
    }

    @Test
    void gradientStrokeInALaterSectionRegistersOnItsOwnPage() throws Exception {
        Path out = tempDir.resolve("gradient-section.pdf");
        DocumentColor a = DocumentColor.rgb(167, 139, 250);
        DocumentColor b = DocumentColor.rgb(97, 40, 217);
        DocumentPaint axis = new DocumentPaint.LinearAxis(
                List.of(new DocumentPaint.Stop(0.0, a), new DocumentPaint.Stop(1.0, b)), 0.0, 0.0, 1.0, 1.0);

        DocumentSession cover = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));

        DocumentSession body = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        body.pageFlow().name("Flow").addPath(p -> p.size(120, 60)
                .moveTo(0, 0.5).curveTo(0.25, 1, 0.75, 0, 1, 0.5)
                .stroke(DocumentStroke.of(a, 3))
                .strokePaint(axis)).build();

        try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            // The gradient stroke is on the BODY (section 2, page index 1). Its shading
            // pattern must register on the body's own page resources, not the cover's.
            assertThat(patternCount(pdf, 1)).isEqualTo(1);
            assertThat(patternCount(pdf, 0)).isZero();
        }
    }

    @Test
    void internalLinkSourceInALaterSectionLandsOnItsGlobalPage() throws Exception {
        Path out = tempDir.resolve("backward-link.pdf");
        DocumentSession cover = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        cover.pageFlow(page -> page.addSection(s -> s.anchor("home").addParagraph("Home")));

        DocumentSession middle = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        middle.pageFlow(page -> page.addParagraph("Middle"));

        DocumentSession last = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        last.pageFlow(page -> page.addParagraph(p -> p.text("Back to home").linkTo("home")));

        try (MultiSectionDocument doc = GraphCompose.documents(out)
                .section(cover).section(middle).section(last).create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(3);
            // The link SOURCE is on the third section (global page 2, cumulative offset 2);
            // its annotation must land on page 2 and target the cover (page 0).
            assertThat(goToTargetPages(pdf, 2)).contains(0);
            assertThat(goToTargetPages(pdf, 0)).isEmpty();
        }
    }

    @Test
    void footerCanSkipASectionsOwnFirstPage() throws Exception {
        Path out = tempDir.resolve("skip-first.pdf");
        DocumentSession cover = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));

        DocumentSession body = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        body.footer(DocumentHeaderFooter.builder()
                .centerText("Folio {page}")
                .numbering(DocumentPageNumbering.builder().showOnFirstPage(false).build())
                .build());
        body.pageFlow(page -> {
            page.addParagraph("Body one");
            page.addPageBreak(b -> b.name("body-break"));
            page.addParagraph("Body two");
        });

        try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            // Body's own first page (document page 2) suppresses the footer; its
            // second page (document page 3) shows "Folio 2" — the section-local window.
            assertThat(pageText(pdf, 2)).doesNotContain("Folio");
            assertThat(pageText(pdf, 3)).contains("Folio 2");
        }
    }

    @Test
    void metadataIsTakenFromTheFirstSectionThatDeclaresIt() throws Exception {
        Path out = tempDir.resolve("metadata.pdf");
        DocumentSession cover = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        cover.metadata(DocumentMetadata.builder().title("Cover wins").build());
        cover.pageFlow(page -> page.addParagraph("Cover"));

        DocumentSession body = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(24)).create();
        body.metadata(DocumentMetadata.builder().title("Body loses").build());
        body.pageFlow(page -> page.addParagraph("Body"));

        try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            assertThat(pdf.getDocumentInformation().getTitle()).isEqualTo("Cover wins");
        }
    }

    @Test
    void closeIsIdempotentThenRejectsFurtherRendering() {
        MultiSectionDocument doc = GraphCompose.documents().section(cover()).section(body()).create();
        doc.close();
        doc.close(); // idempotent — no throw on the second call
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(doc::toPdfBytes);
    }

    @Test
    void buildPdfWithoutADefaultOutputFileThrows() {
        try (MultiSectionDocument doc = GraphCompose.documents().section(cover()).section(body()).create()) {
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(doc::buildPdf)
                    .withMessageContaining("default output file");
        }
    }

    private static int patternCount(PDDocument doc, int zeroBasedPage) throws IOException {
        int count = 0;
        for (COSName ignored : doc.getPage(zeroBasedPage).getResources().getPatternNames()) {
            count++;
        }
        return count;
    }
}
