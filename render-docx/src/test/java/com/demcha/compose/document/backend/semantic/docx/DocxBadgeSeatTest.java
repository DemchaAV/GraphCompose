package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A badge stands where the page draws it: its initials seated by their cap band
 * ({@link TextVerticalAlign}) in its shape, and the shape on its row's line in a lifted row.
 *
 * <p>Centred in the shape as Word centres a line of them, {@code NorthlineProposal}'s monogram
 * stood 0.9pt low; its step badges, placed from their cell's paragraph, stood 1.35pt above their
 * row, the space their lifted row's cells start lower by written above that paragraph.</p>
 */
class DocxBadgeSeatTest {

    /** A 24pt line, which an 88pt disc's inscribed square holds with room for any seat of it. */
    private static final DocumentTextStyle TEXT = DocumentTextStyle.builder()
            .fontName(FontName.LATO).size(20).color(DocumentColor.WHITE).build();

    /** A 45.7pt line, whose capitals stand well below its ascent: seated, it passes that square. */
    private static final DocumentTextStyle DISPLAY = DocumentTextStyle.builder()
            .fontName(FontName.SPECTRAL).size(30).color(DocumentColor.WHITE).build();

    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<path d='M0 0 L10 0 L10 10 Z' fill='#ff0000'/></svg>");

    @Test
    void initialsSeatedByTheirCapBandStandOffTheShapesMiddleAsThePageSeatsThem() throws Exception {
        Badge plain = badge(TextVerticalAlign.DEFAULT, Shapes.DISC, TEXT);
        assertThat(plain.rise()).as("on its baseline, centred as the editors centre its line").isZero();
        assertThat(plain.top()).as("reaching out to the disc, past the square inscribed in it").isNegative();

        for (TextVerticalAlign seat : List.of(TextVerticalAlign.TOP, TextVerticalAlign.CENTER,
                TextVerticalAlign.BOTTOM)) {
            Badge seated = badge(seat, Shapes.DISC, TEXT);
            double onThePage = plain.baseline() - seated.baseline();

            assertThat(Math.abs(onThePage)).as(seat + ": the page moves its initials").isGreaterThan(0.5);
            assertThat(seated.rise()).as(seat + ": as far as the page moves them, up where it raises them")
                    .isCloseTo(onThePage, within(0.01));
            assertThat(Math.min(seated.top(), seated.bottom()))
                    .as(seat + ": neither inset below zero, which LibreOffice takes for none").isZero();
            assertThat(seated.across()).as(seat + ": the shape wraps its text as an unseated badge's")
                    .isEqualTo(plain.across());
        }
    }

    @Test
    void initialsTooTallForTheirSeatReachOutAtBothSides() throws Exception {
        // In the inscribed square less twice the seat, Word set the line low by half what it passed:
        // 3.8pt seated at the top, 5.6pt at the bottom.
        Badge plain = badge(TextVerticalAlign.DEFAULT, Shapes.DISC, DISPLAY);
        for (TextVerticalAlign seat : List.of(TextVerticalAlign.TOP, TextVerticalAlign.BOTTOM)) {
            Badge seated = badge(seat, Shapes.DISC, DISPLAY);

            assertThat(seated.rise()).as(seat + ": as far as the page moves them")
                    .isCloseTo(plain.baseline() - seated.baseline(), within(0.01));
            assertThat(seated.top() + seated.bottom()).as(seat + ": the room an unseated badge's line has")
                    .isCloseTo(plain.top() + plain.bottom(), within(1L));
        }
    }

    @Test
    void initialsLoweredByTheirSeatHaveTheInsetAboveThem() throws Exception {
        Badge plain = badge(TextVerticalAlign.DEFAULT, Shapes.CHIP, TEXT);
        Badge seated = badge(TextVerticalAlign.BOTTOM, Shapes.CHIP, TEXT);

        assertThat(seated.bottom()).as("none below, which would raise them").isZero();
        assertThat(seated.top()).as("twice the lowering above").isPositive();
        assertThat(seated.rise()).isCloseTo(plain.baseline() - seated.baseline(), within(0.01));
    }

    @Test
    void aBadgeAloneInALiftedRowsCellStaysOnItsLine() throws Exception {
        // NorthlineProposal's step row, lifted for its title's first line: the badge's cell, which
        // that line does not open, starts lower by space above its paragraph. Word places a shape
        // anchored in a paragraph from its top, that space included: placed from the paragraph's
        // line, the badge stood 1.35pt above its row.
        DocumentTextStyle number = DocumentTextStyle.builder().size(8.5).color(DocumentColor.WHITE).build();
        DocumentTextStyle title = DocumentTextStyle.builder()
                .fontName(FontName.SPECTRAL).size(16).build();
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, page -> page
                .addParagraph(p -> p.text("Scope of work").margin(DocumentInsets.bottom(12)))
                .addRow("Step", row -> row
                        .gap(12)
                        .columns(DocumentRowColumn.fixed(26), DocumentRowColumn.fixed(150),
                                DocumentRowColumn.weight(1))
                        .add(new ShapeContainerBuilder().name("StepBadge").roundedRect(26, 15, 2)
                                .fillColor(ACCENT)
                                .center(new ParagraphBuilder().text("01").textStyle(number).build())
                                .build())
                        .addParagraph(p -> p.text("Discovery").textStyle(title))
                        .addParagraph(p -> p.text("Stakeholder interviews and a brand audit.")))) ) {
            XWPFParagraph carrier = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
            assertThat(carrier.getCTP().xmlText()).as("the badge is its cell's paragraph's")
                    .contains("<w:txbxContent>");
            assertPlacedOnItsLine(carrier, "the cell started lower than its lifted row");
        }
    }

    @Test
    void aDrawingAloneInATablesCellStaysOnItsLineUnderThePaddingWrittenAboveIt() throws Exception {
        // Word gives every cell of a row the row's largest top margin, so a cell padded more than
        // the others has the rest written as space above its paragraph (evenTheRowsMargins).
        DocumentTableStyle deep = DocumentTableStyle.builder().padding(DocumentInsets.of(10)).build();
        DocumentTableStyle shallow = DocumentTableStyle.builder().padding(DocumentInsets.of(2)).build();
        try (XWPFDocument document = DocxExports.withLayout(500, 400, 30, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(40), DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(ICON.node(16)).withStyle(deep),
                        DocumentTableCell.text("Label").withStyle(shallow))))) {
            XWPFParagraph carrier = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
            assertThat(carrier.getCTP().xmlText()).as("the drawing is its cell's paragraph's")
                    .contains("layoutInCell=\"1\"");
            assertPlacedOnItsLine(carrier, "the cell's padding past the row's, written above its paragraph");
        }
    }

    /**
     * Asserts the shape anchored in a cell's paragraph stands on that paragraph's line: placed
     * from the paragraph's top, as Word and LibreOffice place it, below the space above its line.
     */
    private static void assertPlacedOnItsLine(XWPFParagraph carrier, String spaceAbove) {
        long before = ((Number) carrier.getCTP().getPPr().getSpacing().getBefore()).longValue();
        Matcher offset = Pattern.compile("relativeFrom=\"paragraph\"><wp:posOffset>(-?\\d+)<")
                .matcher(carrier.getCTP().xmlText());
        assertThat(offset.find()).as("placed from its paragraph").isTrue();

        assertThat(before).as(spaceAbove).isPositive();
        assertThat(Long.parseLong(offset.group(1))).as("placed below that space, on the paragraph's line")
                .isEqualTo(before * Units.EMU_PER_POINT / 20);
    }

    /** The two shapes a badge is drawn as, each holding the initials centred. */
    private enum Shapes {
        DISC(text -> new ShapeContainerBuilder().name("Monogram").circle(88).fillColor(ACCENT).center(text)
                .build()),
        CHIP(text -> new ShapeContainerBuilder().name("Step").roundedRect(80, 56, 10).fillColor(ACCENT)
                .center(text).build());

        private final Function<DocumentNode, DocumentNode> holding;

        Shapes(Function<DocumentNode, DocumentNode> holding) {
            this.holding = holding;
        }
    }

    /**
     * A badge's text box as written and its initials as the page draws them.
     *
     * @param top      the box's top inset, in EMUs
     * @param bottom   its bottom inset, in EMUs
     * @param across   its left and right insets as written
     * @param baseline the initials' baseline on the page, in points from its top
     */
    private record Badge(long top, long bottom, String across, double baseline) {

        /** How far the text stands above the shape's middle, in points: Word centres it between the insets. */
        double rise() {
            return (bottom - top) / 2.0 / Units.EMU_PER_POINT;
        }
    }

    private static Badge badge(TextVerticalAlign seat, Shapes shape, DocumentTextStyle style) throws Exception {
        DocumentNode initials = new ParagraphBuilder().text("AR").textStyle(style).verticalAlign(seat).build();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(40))
                .create()) {
            session.pageFlow(page -> page.add(shape.holding.apply(initials)));
            double baseline = baselineOf('A', session.toPdfBytes());
            String xml;
            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                xml = document.getDocument().xmlText();
            }
            assertThat(xml).as("the initials are the shape's text").contains("<w:txbxContent>");
            Matcher insets = Pattern.compile("(lIns=\"-?\\d+\") tIns=\"(-?\\d+)\" (rIns=\"-?\\d+\") bIns=\"(-?\\d+)\"")
                    .matcher(xml);
            assertThat(insets.find()).isTrue();
            return new Badge(Long.parseLong(insets.group(2)), Long.parseLong(insets.group(4)),
                    insets.group(1) + " " + insets.group(3), baseline);
        }
    }

    /** Where the page stands the first of a letter's baseline, in points from the page's top. */
    private static double baselineOf(char letter, byte[] pdf) throws IOException {
        List<TextPosition> letters = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    letters.add(text);
                }
            };
            stripper.getText(document);
        }
        return letters.stream().filter(text -> text.getUnicode().equals(String.valueOf(letter)))
                .findFirst().orElseThrow().getYDirAdj();
    }
}
