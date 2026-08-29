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
