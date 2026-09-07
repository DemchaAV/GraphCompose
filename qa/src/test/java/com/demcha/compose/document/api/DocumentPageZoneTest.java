package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A page zone draws a node subtree, not text slots.
 *
 * <p>The point of the type is that the band goes through the same layout and
 * render path as the body, so the things the body can do work there without the
 * zone knowing about any of them. These tests assert exactly that: a row that
 * lays its children out, an inline chip, a link annotation, and a page number
 * that comes from a Java expression rather than a placeholder token.</p>
 *
 * @author Artem Demchyshyn
 */
class DocumentPageZoneTest {

    private static final DocumentColor CHIP_INK = DocumentColor.rgb(24, 60, 90);
    private static final DocumentColor CHIP_FILL = DocumentColor.rgb(226, 236, 245);

    @Test
    void aZoneDrawsItsSubtreeOnEveryPageWithThatPagesNumbers() throws Exception {
        byte[] pdf = render(DocumentPageZone.footer(36, page -> new RowBuilder()
                .name("FooterRow")
                .addParagraph(paragraph -> paragraph.name("Note").text("Confidential"))
                .flexSpacer()
                .addParagraph(paragraph -> paragraph
                        .name("Counter")
                        .text("Sheet " + page.number() + " of " + page.total()))
                .build()));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            String text = new PDFTextStripper().getText(document);

            assertThat(text)
                    .as("the zone is repeated, and each page's numbers come from its own context")
                    .contains("Sheet 1 of " + document.getNumberOfPages())
                    .contains("Sheet 2 of " + document.getNumberOfPages());
            assertThat(text.split("Confidential", -1).length - 1)
                    .as("the static half of the zone is drawn once per page")
                    .isEqualTo(document.getNumberOfPages());
        }
    }

    @Test
    void aZoneCarriesTheInlineDecorationsTheBodyCarries() throws Exception {
        byte[] pdf = render(DocumentPageZone.footer(36, page -> new ParagraphBuilder()
                .name("Badge")
                .inlineChip("v2.4", CHIP_INK, CHIP_FILL)
                .inlineText(" ")
                .inlineLink("acme.example",
                        new com.demcha.compose.document.node.DocumentLinkOptions("https://acme.example"))
                .build()));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .as("a chip is a node the body already knows how to draw, so the zone gets it free")
                    .contains("v2.4")
                    .contains("acme.example");

            long links = 0;
            for (PDAnnotation annotation : document.getPage(0).getAnnotations()) {
                if (annotation instanceof PDAnnotationLink) {
                    links++;
                }
            }
            assertThat(links)
                    .as("and the link is a real annotation, not just characters")
                    .isGreaterThan(0);
        }
    }

    @Test
    void aPredicateDecidesWhichPagesCarryTheZone() throws Exception {
        byte[] pdf = render(DocumentPageZone.builder()
                .height(36)
                .appliesTo(page -> !page.isFirst())
                .content(page -> new ParagraphBuilder()
                        .name("Counter")
                        .text("Sheet " + page.number())
                        .build())
                .build());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertThat(stripper.getText(document))
                    .as("the cover carries no zone")
                    .doesNotContain("Sheet");

            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertThat(stripper.getText(document))
                    .as("every later page does")
                    .contains("Sheet 2");
        }
    }

    /**
     * A zone reserves by default — the opposite of {@link
     * com.demcha.compose.document.output.DocumentHeaderFooter}, which cannot,
     * because it has to keep rendering documents written before the flag existed.
     */
    @Test
    void aZoneReservesItsBandWithoutBeingAsked() throws Exception {
        byte[] withZone = render(DocumentPageZone.footer(80, page -> new ParagraphBuilder()
                .name("Counter").text("Sheet " + page.number()).build()));

        byte[] withoutZone;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(24))
                .create()) {
            fillBody(document);
            withoutZone = document.toPdfBytes();
        }

        try (PDDocument zoned = Loader.loadPDF(withZone);
             PDDocument bare = Loader.loadPDF(withoutZone)) {
            assertThat(zoned.getNumberOfPages())
                    .as("an 80pt band nobody asked to reserve would leave the page count alone;"
                            + " a zone reserves, so the body has to move")
                    .isGreaterThan(bare.getNumberOfPages());
        }
    }

    /**
     * A zone does not paginate, so content it cannot hold is an error rather than
     * a silent truncation — and the message has to name the zone, not "the page",
     * or it sends the author looking at the wrong number.
     */
    @Test
    void contentTallerThanTheBandIsRefusedWithAMessageAboutTheZone() {
        assertThatThrownBy(() -> render(DocumentPageZone.footer(14, page -> new RowBuilder()
                .name("Overflowing")
                .addParagraph(paragraph -> paragraph
                        .name("TooMuch")
                        .text("This zone is one line deep and its content is a paragraph long enough"
                                + " to need several lines, so it cannot be drawn in the band."))
                .build())))
                .isInstanceOf(AtomicNodeTooLargeException.class)
                .hasMessageContaining("FOOTER")
                .hasMessageContaining("does not fit its declared height")
                .hasMessageContaining("14.0");
    }

    /**
     * The test above overflows through an atomic row, which the compiler refuses
     * itself. Splittable content is the quieter failure mode: a bare paragraph is
     * happy to continue onto a second band-page, and a zone has nowhere to put
     * one — so the tail used to be dropped with nothing but a log line. Same
     * contract for both: refused, named, never truncated.
     */
    @Test
    void splittableContentThatOutgrowsTheBandIsRefusedNotTruncated() {
        assertThatThrownBy(() -> render(DocumentPageZone.footer(24, page -> new ParagraphBuilder()
                .name("Spilling")
                .text("A paragraph is splittable, so the compiler does not refuse it the way it"
                        + " refuses an oversized row: it lays the extra lines onto a second band"
                        + " page, and this sentence is long enough to be sure there are several"
                        + " of them to lose.")
                .build())))
                .isInstanceOf(AtomicNodeTooLargeException.class)
                .hasMessageContaining("FOOTER")
                .hasMessageContaining("does not fit its declared height")
                .hasMessageContaining("24.0");
    }

    /**
     * The header band is the other half of the placement arithmetic — a footer
     * needs no vertical shift at all, a header is lifted by the band's distance
     * from the page's bottom edge, and getting that wrong puts the header where
     * the footer belongs without anything else noticing.
     */
    @Test
    void aHeaderZoneLandsAboveTheBodyAndAFooterZoneBelowIt() throws Exception {
        byte[] pdf;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(24))
                .create()) {

            document.chrome()
                    .zone(DocumentPageZone.header(30, page -> new ParagraphBuilder()
                            .name("Top").text("TOPBAND").build()))
                    .zone(DocumentPageZone.footer(30, page -> new ParagraphBuilder()
                            .name("Bottom").text("BOTTOMBAND").build()));
            fillBody(document);
            pdf = document.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String page = stripper.getText(document);

            assertThat(page.indexOf("TOPBAND"))
                    .as("the header is read before the body")
                    .isGreaterThanOrEqualTo(0)
                    .isLessThan(page.indexOf("Body line 0"));
            assertThat(page.indexOf("BOTTOMBAND"))
                    .as("and the footer after it")
                    .isGreaterThan(page.indexOf("Body line 0"));
        }
    }

    /**
     * Reservation is document-wide on purpose: {@code appliesTo} decides where
     * the band is painted, {@code reserveSpace} takes it out of every page's
     * content area either way — per-page reservation would let the page count
     * depend on which pages carry the zone, which can depend on the page count.
     * Pinned so the coupling stays a decision rather than an accident.
     */
    @Test
    void aHiddenZoneStillReservesItsBand() throws Exception {
        byte[] conditional = render(DocumentPageZone.footer(80, page -> new ParagraphBuilder()
                .name("Counter").text("Sheet " + page.number()).build())
                .toBuilder()
                .appliesTo(page -> !page.isFirst())
                .build());
        byte[] everywhere = render(DocumentPageZone.footer(80, page -> new ParagraphBuilder()
                .name("Counter").text("Sheet " + page.number()).build()));

        try (PDDocument hidden = Loader.loadPDF(conditional);
             PDDocument painted = Loader.loadPDF(everywhere)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertThat(stripper.getText(hidden))
                    .as("the predicate still decides painting")
                    .doesNotContain("Sheet");

            assertThat(lowestBaselineOnFirstPage(hidden))
                    .as("but not geometry: no body glyph reaches into the reserved 80pt band"
                            + " (the page is 240pt tall, so the band starts at y=160 top-down)")
                    .isLessThanOrEqualTo(240 - 80 + 1.0);
            assertThat(hidden.getNumberOfPages())
                    .as("so hiding the zone on the cover reflows nothing")
                    .isEqualTo(painted.getNumberOfPages());
        }
    }

    private static double lowestBaselineOnFirstPage(PDDocument document) throws Exception {
        double[] lowest = {0};
        PDFTextStripper scanner = new PDFTextStripper() {
            @Override
            protected void writeString(String text,
                                       java.util.List<org.apache.pdfbox.text.TextPosition> positions) {
                for (org.apache.pdfbox.text.TextPosition position : positions) {
                    lowest[0] = Math.max(lowest[0], position.getYDirAdj());
                }
            }
        };
        scanner.setStartPage(1);
        scanner.setEndPage(1);
        scanner.getText(document);
        return lowest[0];
    }

    /**
     * The zone rides the body's machinery, and that includes anchors: a page
     * reference placed in a footer resolves against the same settled graph a
     * body reference resolves against, so "see appendix on page N" chrome is one
     * {@code addPageReference} call rather than a hand-maintained number.
     */
    @Test
    void aPageReferenceInsideAZoneResolvesTheBodysAnchors() throws Exception {
        byte[] pdf;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(24))
                .create()) {

            document.chrome().zone(DocumentPageZone.footer(36, page -> new RowBuilder()
                    .name("RefFooter")
                    .addParagraph(paragraph -> paragraph.name("Label").text("See appendix:"))
                    .addPageReference("appendix")
                    .build()));
            fillBody(document);
            document.dsl().pageFlow()
                    .name("Appendix")
                    .anchor("appendix")
                    .addParagraph(paragraph -> paragraph.name("AppendixBody").text("APPENDIXMARK"))
                    .build();
            pdf = document.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            int appendixPage = pageOf(document, "APPENDIXMARK");
            assertThat(appendixPage)
                    .as("the anchor has to land past page 1, or the reference resolving"
                            + " trivially to 1 would prove nothing")
                    .isGreaterThan(1);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertThat(stripper.getText(document))
                    .as("the footer on the first page already knows where the appendix went")
                    .containsPattern("See appendix:\\s*" + appendixPage);
        }
    }

    private static int pageOf(PDDocument document, String needle) throws Exception {
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            if (stripper.getText(document).contains(needle)) {
                return page;
            }
        }
        return -1;
    }

    /**
     * A page field is a node like any other, so it has to survive being handed to
     * the compiler from outside a zone — the case that used to reach the registry
     * with no definition for its kind. Rendered where nothing publishes a page
     * number, it draws a blank rather than failing the document.
     */
    @Test
    void aPageFieldOutsideAZoneRendersBlankRatherThanFailing() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(24))
                .create()) {

            document.add(new com.demcha.compose.document.node.PageFieldNode(
                    com.demcha.compose.document.node.PageFieldKind.NUMBER,
                    com.demcha.compose.document.style.DocumentTextStyle.DEFAULT));
            document.dsl().pageFlow()
                    .name("StrayField")
                    .addParagraph(paragraph -> paragraph.name("Body").text("Body copy."))
                    .build();

            try (PDDocument rendered = Loader.loadPDF(document.toPdfBytes())) {
                assertThat(new PDFTextStripper().getText(rendered))
                        .as("the document still renders; the field simply has nothing to say")
                        .contains("Body copy.");
            }
        }
    }

    @Test
    void writesAShowcaseSheetForReview() throws Exception {
        byte[] pdf = render(DocumentPageZone.footer(40, page -> new RowBuilder()
                .name("Showcase")
                .gap(10)
                .addParagraph(paragraph -> paragraph.name("Note").text("Confidential"))
                .flexSpacer()
                .addParagraph(paragraph -> paragraph.name("Badge").inlineChip("v2.4", CHIP_INK, CHIP_FILL))
                .addParagraph(paragraph -> paragraph.name("Link").inlineLink("acme.example",
                        new com.demcha.compose.document.node.DocumentLinkOptions("https://acme.example")))
                .addParagraph(paragraph -> paragraph.name("Counter")
                        .text(page.number() + " / " + page.total()))
                .build()));

        java.nio.file.Path out = java.nio.file.Path.of("target/visual-tests/page-zone");
        java.nio.file.Files.createDirectories(out);
        java.nio.file.Path file = java.nio.file.Files.write(out.resolve("node-zone-footer.pdf"), pdf);
        assertThat(file).isNotEmptyFile();
    }

    private static byte[] render(DocumentPageZone zone) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(24))
                .create()) {

            document.chrome().zone(zone);
            fillBody(document);
            return document.toPdfBytes();
        }
    }

    private static void fillBody(DocumentSession document) {
        var flow = document.dsl().pageFlow().name("ZoneFixture");
        for (int i = 0; i < 24; i++) {
            int index = i;
            flow.addParagraph(paragraph -> paragraph
                    .name("Body" + index)
                    .text("Body line " + index + " exists to push the flow onto a second page."));
        }
        flow.build();
    }
}
