package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Text an overlay sets at the left and the right of one line is one Word line, split by a tab.
 *
 * <p>A CV entry's head band puts its title at the left and its dates at the right, as two
 * layers of one container. Written one after the other, the dates took a line of their own
 * under every title.</p>
 */
class DocxLinePairTest {

    private static final double PAGE_WIDTH = 400;
    private static final double MARGIN = 20;
    private static final double CONTENT = PAGE_WIDTH - 2 * MARGIN;

    @Test
    void aTitleAtTheLeftAndDatesAtTheRightAreOneLineSplitByARightTab() throws Exception {
        try (XWPFDocument document = export(band(20, 0))) {
            List<XWPFParagraph> paragraphs = written(document);

            assertThat(paragraphs).hasSize(1);
            XWPFParagraph line = paragraphs.get(0);
            assertThat(line.getText()).isEqualTo("MARKETING MANAGER\tJan 2022 - Present");
            CTPPr properties = line.getCTP().getPPr();
            assertThat(properties.getTabs().sizeOfTabArray()).isEqualTo(1);
            assertThat(properties.getTabs().getTabArray(0).getVal().toString()).isEqualTo("right");
            assertThat(DocxTwips.of(properties.getTabs().getTabArray(0).getPos()))
                    .as("at the band's right edge, where the dates end")
                    .isCloseTo(Math.round(CONTENT * 20), org.assertj.core.data.Offset.offset(2L));
        }
    }

    @Test
    void aPairInALayerStackIsOneLineTooNotABandOfTwo() throws Exception {
        try (XWPFDocument document = export(new com.demcha.compose.document.dsl.LayerStackBuilder()
                .name("EntryHead")
                .back(new com.demcha.compose.document.dsl.ShapeBuilder().name("Band").size(CONTENT, 20).build())
                .layer(new ParagraphBuilder().name("Title").text("MARKETING MANAGER")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(12)).build(), LayerAlign.CENTER_LEFT)
                .layer(paragraph("Jan 2022 - Present", TextAlign.RIGHT), LayerAlign.CENTER_RIGHT)
                .build())) {
            assertThat(written(document)).extracting(XWPFParagraph::getText)
                    .containsExactly("MARKETING MANAGER\tJan 2022 - Present");
        }
    }

    @Test
    void aTitleStandingAboveItsBandTakesThatSpaceFromTheGapAbove() throws Exception {
        // The band is shorter than the title's line and the title is pulled up to centre on it:
        // on the page the title's top stands above the band, into the gap over it.
        try (XWPFDocument roomy = export(band(20, 0));
             XWPFDocument pulledUp = export(band(6, -4))) {
            XWPFParagraph plain = written(roomy).get(0);
            XWPFParagraph raised = written(pulledUp).get(0);

            assertThat(before(plain)).as("the gap above, and the band's room above its text")
                    .isGreaterThanOrEqualTo(12L * 20);
            assertThat(before(raised)).as("the gap above, less what the raised title stands into")
                    .isLessThan(12L * 20);
        }
    }

    @Test
    void textHangingBelowItsBandTakesThatSpaceFromTheGapBelow() throws Exception {
        // The next paragraph's margin is measured from the band's bottom, and the text on the
        // page already reaches past it.
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .add(band(6, 4))
                .addParagraph(p -> p.text("After").margin(DocumentInsets.top(10))))) {
            XWPFParagraph after = document.getParagraphs().stream()
                    .filter(paragraph -> "After".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(after)).as("its 10pt, less what the text hangs below the band")
                    .isLessThan(10L * 20);
        }
    }

    @Test
    void aTitleRaisedAboveTheTopOfItsCellLiftsTheRow() throws Exception {
        // An entry laid out as a row: a marker beside the entry's head band, whose title stands
        // above the band. A Word paragraph cannot reach above its cell, so the row is lifted by
        // that much out of the gap above it, and the marker's cell starts that much lower.
        try (XWPFDocument document = DocxExports.withLayout(2 * PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                .addRow(row -> row
                        .addParagraph(p -> p.text("*"))
                        .addSection("Entry", entry -> entry.add(band(6, -4, CONTENT)))))) {
            XWPFParagraph before = document.getParagraphs().stream()
                    .filter(paragraph -> "Before".equals(paragraph.getText())).findFirst().orElseThrow();
            var row = document.getTables().get(0).getRow(0);
            XWPFParagraph marker = row.getCell(0).getParagraphs().get(0);
            XWPFParagraph title = row.getCell(1).getParagraphs().get(0);
            long lifted = 12L * 20 - after(before);

            assertThat(lifted).as("the row stands higher, into the gap above it").isPositive();
            assertThat(before(marker)).as("the marker keeps its place").isEqualTo(lifted);
            assertThat(before(title)).as("the title stands out by what the row was lifted").isZero();
        }
    }

    @Test
    void aTitleRaisedInsideAPaddedCardTakesThePaddingAndLeavesTheCardWhereItIs() throws Exception {
        // On the page the title rises into the card's padding; the fill and its edge stay put.
        try (XWPFDocument document = DocxExports.withLayout(2 * PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                .addSection("Card", card -> card
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(230, 230, 230))
                        .padding(DocumentInsets.of(10))
                        .add(band(6, -4, CONTENT - 20))))) {
            XWPFParagraph before = document.getParagraphs().stream()
                    .filter(paragraph -> "Before".equals(paragraph.getText())).findFirst().orElseThrow();
            var cell = document.getTables().get(0).getRow(0).getCell(0);
            long padding = DocxTwips.of(cell.getCTTc().getTcPr().getTcMar().getTop().getW());

            assertThat(after(before)).as("the card is not lifted").isEqualTo(12L * 20);
            assertThat(padding).as("the title takes its rise out of the padding").isLessThan(10L * 20);
        }
    }

    @Test
    void aCardWhosePaddingCannotTakeTheRiseStaysWhereItIs() throws Exception {
        // 1pt of padding against a rise of several: the rest stays low, the box does not move.
        try (XWPFDocument document = DocxExports.withLayout(2 * PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                .addSection("Card", card -> card
                        .fillColor(com.demcha.compose.document.style.DocumentColor.rgb(230, 230, 230))
                        .padding(DocumentInsets.of(1))
                        .add(band(6, -4, CONTENT - 2))))) {
            XWPFParagraph before = document.getParagraphs().stream()
                    .filter(paragraph -> "Before".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(after(before)).as("the card is not lifted").isEqualTo(12L * 20);
        }
    }

    @Test
    void aRuleAboveARaisedRowKeepsItsPlace() throws Exception {
        // An empty paragraph above the row that draws a line is not a separator to lift into.
        try (XWPFDocument document = DocxExports.withLayout(2 * PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before"))
                .addLine(line -> line.horizontal(CONTENT).thickness(1).margin(DocumentInsets.top(8)))
                .addRow(row -> row.addParagraph(p -> p.text("*"))
                        .addSection("Entry", entry -> entry.add(band(6, -4, CONTENT)))))) {
            XWPFParagraph rule = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetPBdr())
                    .findFirst().orElseThrow();

            assertThat(before(rule)).as("the space above the line stays").isGreaterThanOrEqualTo(8L * 20 - 2);
        }
    }

    @Test
    void aRowAfterARowIsLiftedOutOfTheSeparatorBetweenThem() throws Exception {
        // Two entries back to back: the gap between the tables is held above the separator.
        try (XWPFDocument document = DocxExports.withLayout(2 * PAGE_WIDTH, 600, MARGIN, page -> page
                .addRow(row -> row.addParagraph(p -> p.text("*"))
                        .addSection("First", entry -> entry.add(band(6, -4, CONTENT))))
                .addRow(row -> row.margin(DocumentInsets.top(12)).addParagraph(p -> p.text("*"))
                        .addSection("Second", entry -> entry.add(band(6, -4, CONTENT)))))) {
            var second = document.getTables().get(1).getRow(0);

            assertThat(before(second.getCell(0).getParagraphs().get(0)))
                    .as("the second row's marker starts lower, as the row was lifted")
                    .isPositive();
        }
    }

    @Test
    void theHangReachesTheNextBlockOnlyNotThePageAfterABreak() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .add(band(6, 4))
                .addPageBreak(b -> { })
                .addParagraph(p -> p.text("After").margin(DocumentInsets.top(10))))) {
            XWPFParagraph after = document.getParagraphs().stream()
                    .filter(paragraph -> "After".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(after)).as("a new page: its 10pt whole").isEqualTo(10L * 20);
        }
    }

    @Test
    void theHangStaysInTheCellItHangsIn() throws Exception {
        // The band ends one column of a row; the column beside it starts its own flow.
        try (XWPFDocument document = DocxExports.withLayout(2 * PAGE_WIDTH, 600, MARGIN, page -> page
                .addRow(row -> row
                        .addSection("Left", left -> left.add(band(6, 4, CONTENT)))
                        .addSection("Right", right -> right
                                .addParagraph(p -> p.text("Beside").margin(DocumentInsets.top(10))))))) {
            XWPFParagraph beside = document.getTables().get(0).getRow(0).getCell(1).getParagraphs().stream()
                    .filter(paragraph -> "Beside".equals(paragraph.getText())).findFirst().orElseThrow();

            assertThat(before(beside)).as("its 10pt whole").isEqualTo(10L * 20);
        }
    }

    @Test
    void eachHalfKeepsItsBookmarkAndTheTitleItsHeadingRole() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("See the role").linkTo("role"))
                .addParagraph(p -> p.text("See the dates").linkTo("dates"))
                .add(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                        .name("EntryHead")
                        .rectangle(CONTENT, 20)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Title").text("MARKETING MANAGER").anchor("role")
                                .bookmark(new com.demcha.compose.document.node.DocumentBookmarkOptions("Role", 0))
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(12)).build(), 0, 0, LayerAlign.CENTER_LEFT)
                        .position(new ParagraphBuilder().text("Jan 2022 - Present").anchor("dates")
                                .align(TextAlign.RIGHT).textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build(),
                                0, 0, LayerAlign.CENTER_RIGHT)
                        .build()))) {
            XWPFParagraph line = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().startsWith("MARKETING")).findFirst().orElseThrow();

            assertThat(line.getCTP().getBookmarkStartList())
                    .extracting(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark::getName)
                    .as("a bookmark for each half, so the links reach them")
                    .hasSize(2);
            assertThat(line.getStyleID()).as("the title's outline level").isEqualTo("Heading1");
        }
    }

    @Test
    void threeParagraphsInABandStayOneAfterTheOther() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                .addContainer(head -> head
                        .name("Band")
                        .rectangle(CONTENT, 20)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(paragraph("Left", TextAlign.LEFT), 0, 0, LayerAlign.CENTER_LEFT)
                        .position(paragraph("Middle", TextAlign.CENTER), 0, 0, LayerAlign.CENTER)
                        .position(paragraph("Right", TextAlign.RIGHT), 0, 0, LayerAlign.CENTER_RIGHT)))) {
            assertThat(written(document)).extracting(XWPFParagraph::getText)
                    .containsExactly("Left", "Middle", "Right");
        }
    }

    @Test
    void textsThatNearlyMeetStayOneAfterTheOther() throws Exception {
        // An editor sets text a little wider than the page: a tab the left text reaches would
        // put the dates on a line of their own, out of sight under an exact line.
        double texts = textWidth("MARKETING MANAGER", 12) + textWidth("Jan 2022 - Present", 9);
        try (XWPFDocument close = export(band(20, 0, texts + 2));
             XWPFDocument apart = export(band(20, 0, texts + 8))) {
            assertThat(written(close)).as("2pt between them").hasSize(2);
            assertThat(written(apart)).as("8pt between them").hasSize(1);
        }
    }

    /** How wide the page sets a line of text, as the layout measured it. */
    private static double textWidth(String text, double size) {
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(PAGE_WIDTH, 600).margin(DocumentInsets.of(MARGIN)).create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text(text)
                    .textStyle(DocumentTextStyle.DEFAULT.withSize(size))));
            return session.layoutGraph().fragments().stream()
                    .map(com.demcha.compose.document.layout.PlacedFragment::payload)
                    .filter(com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload.class::isInstance)
                    .map(com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload.class::cast)
                    .mapToDouble(payload -> payload.lines().get(0).width())
                    .findFirst().orElseThrow();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static XWPFDocument export(DocumentNode band) throws Exception {
        return DocxExports.withLayout(PAGE_WIDTH, 600, MARGIN, page -> page
                .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                .add(band));
    }

    /** A head band: the title at the left, the dates at the right, both pulled by {@code pull}. */
    private static DocumentNode band(double height, double pull) {
        return band(height, pull, CONTENT);
    }

    private static DocumentNode band(double height, double pull, double width) {
        return new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("EntryHead")
                .rectangle(width, height)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Title").text("MARKETING MANAGER")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(12)).build(), 0, pull, LayerAlign.CENTER_LEFT)
                .position(paragraph("Jan 2022 - Present", TextAlign.RIGHT), 0, pull, LayerAlign.CENTER_RIGHT)
                .build();
    }

    private static DocumentNode paragraph(String text, TextAlign align) {
        return new ParagraphBuilder().text(text).align(align)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(9)).build();
    }

    private static List<XWPFParagraph> written(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> !paragraph.getText().isBlank() && !"Before".equals(paragraph.getText()))
                .toList();
    }

    private static long after(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getAfter());
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }
}
