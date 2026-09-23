package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A horizontal line or a divider is Word's own rule: the bottom border of an empty paragraph.
 *
 * <p>Both were dropped with the rest of the drawing, so every rule a template draws under a
 * heading or between entries was missing in Word. As a paragraph border the rule flows with
 * the text and a reader moves or deletes it as a line of the document.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxRuleTest {

    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    @Test
    void aFillLineIsAParagraphBorderAcrossTheWidth() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Experience"))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 2)).fill()))) {
            XWPFParagraph rule = document.getParagraphs().get(1);
            CTBorder bottom = rule.getCTP().getPPr().getPBdr().getBottom();

            assertThat(rule.getText()).isEmpty();
            assertThat(bottom.getVal()).isEqualTo(STBorder.SINGLE);
            // w:sz counts eighths of a point.
            assertThat(bottom.getSz().intValue()).isEqualTo(16);
            assertThat(hex(bottom.getColor())).isEqualTo("1A5694");
            assertThat(DocxTwips.of(rule.getCTP().getPPr().getInd().getLeft())).isZero();
            assertThat(DocxTwips.of(rule.getCTP().getPPr().getInd().getRight()))
                    .as("a fill line runs to the right margin")
                    .isZero();
        }
    }

    @Test
    void aLineShorterThanTheColumnEndsWhereItEnds() throws Exception {
        // Page 400 wide with 20pt margins: 360pt of text width, a 200pt line.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 1))))) {
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(160 * 20L);
        }
    }

    @Test
    void aDividerIsARuleOfItsThicknessAndColour() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Above"))
                .addDivider(d -> d.width(200).thickness(3).color(ACCENT).margin(DocumentInsets.symmetric(6, 0))))) {
            CTPPr properties = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(properties.getPBdr().getBottom().getSz().intValue()).isEqualTo(24);
            assertThat(hex(properties.getPBdr().getBottom().getColor())).isEqualTo("1A5694");
            assertThat(DocxTwips.of(properties.getSpacing().getBefore())).isEqualTo(6 * 20L);
            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(160 * 20L);
        }
    }

    @Test
    void theSpaceAroundTheStrokeInItsBoxIsKept() throws Exception {
        // A 1pt stroke centred in a 12pt box: 5.5pt above it, 5.5pt below.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.size(200, 12).from(0, 6).to(200, 6).stroke(DocumentStroke.of(ACCENT, 1)))
                .addParagraph(p -> p.text("Below")))) {
            CTPPr rule = document.getParagraphs().get(0).getCTP().getPPr();
            CTPPr below = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(DocxTwips.of(rule.getSpacing().getLine())).isEqualTo(110L);
            assertThat(DocxTwips.of(below.getSpacing().getBefore())).isEqualTo(110L);
        }
    }

    @Test
    void aDashedLineIsDashedAndTheReportSaysTheLengthsAreWords() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 1)).dashed(6, 3))
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 1)).dashed(1, 2)));
        try (document) {
            assertThat(bottomOf(document, 0).getVal()).isEqualTo(STBorder.DASHED);
            assertThat(bottomOf(document, 1).getVal()).as("dashes no longer than the stroke").isEqualTo(STBorder.DOTTED);
            assertThat(report.get().bySubject().get("dash pattern")).hasSize(2);
        }
    }

    @Test
    void aThickStrokeStopsAtWordsThickestBorder() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(200).stroke(DocumentStroke.of(ACCENT, 20))))) {
            assertThat(bottomOf(document, 0).getSz().intValue()).isEqualTo(96);
        }
    }

    @Test
    void aRuleInsideACardIsInTheCardsCell() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Card", card -> card
                .fillColor(DocumentColor.rgb(238, 243, 249))
                .addParagraph(p -> p.text("Inside"))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).fill())))) {
            List<XWPFParagraph> inCell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs();

            assertThat(inCell.get(1).getCTP().getPPr().getPBdr().getBottom().getVal()).isEqualTo(STBorder.SINGLE);
        }
    }

    @Test
    void anAnchorOnARuleIsABookmarkOnItsParagraph() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).anchor("rule")))) {
            assertThat(document.getParagraphs().get(0).getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("rule");
        }
    }

    @Test
    void whatIsNotARuleIsStillReportedAsDropped() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page
                .addLine(l -> l.name("Rail").vertical(40).stroke(DocumentStroke.of(ACCENT, 1)))
                .addShape(s -> s.name("Block").size(100, 30).fillColor(ACCENT))
                .addShape(s -> s.name("Pill").size(100, 4).fillColor(ACCENT).cornerRadius(DocumentCornerRadius.of(2))));
        try (document) {
            assertThat(document.getParagraphs()).isEmpty();
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED))
                    .as("a vertical line, a box taller than a border, a rounded bar")
                    .isEqualTo(3);
        }
    }

    @Test
    void linesLaidOverEachOtherInALayerStackAreNotRulesInTheFlow() throws Exception {
        // A skill meter: a track and the fill laid over it. As rules they came out as two bars,
        // one under the other.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page.addLayerStack(stack -> {
            stack.layer(new com.demcha.compose.document.dsl.LineBuilder().name("Track")
                    .horizontal(120).stroke(DocumentStroke.of(DocumentColor.rgb(200, 200, 200), 3)).anchor("track").build());
            stack.layer(new com.demcha.compose.document.dsl.LineBuilder().name("Filled")
                    .horizontal(80).stroke(DocumentStroke.of(ACCENT, 3)).build());
        }).addPageReference("track"));
        try (document) {
            assertThat(document.getParagraphs()).noneMatch(p -> p.getCTP().getPPr() != null
                                                               && p.getCTP().getPPr().isSetPBdr());
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isEqualTo(2);
            assertThat(document.getParagraphs())
                    .as("the dropped line has no bookmark, so no field may point at one")
                    .noneMatch(p -> p.getCTP().xmlText().contains("PAGEREF"));
        }
    }

    @Test
    void aTranslucentRuleIsFlattenedAgainstWhatIsUnderIt() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 128), 1))))) {
            // Half-opaque black over white.
            assertThat(hex(bottomOf(document, 0).getColor())).isEqualTo("7F7F7F");
        }
    }

    @Test
    void anInvisibleRuleKeepsItsPlaceAndDrawsNothing() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 0), 1)))
                .addParagraph(p -> p.text("After")))) {
            CTPPr rule = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(rule.isSetPBdr()).isFalse();
            assertThat(rule.getSpacing().isSetLine()).isTrue();
        }
    }

    @Test
    void aStrokeThickerThanItsBoxTakesItsExtraFromTheSpaceBelow() throws Exception {
        // horizontal() sizes the box from the stroke set before it: a 1pt box, a 3pt stroke. On
        // the page the stroke spills out of the box; in Word it takes room, so the room comes
        // off the 10pt margin below.
        try (XWPFDocument document = export(page -> page
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 3)).margin(DocumentInsets.bottom(10)))
                .addParagraph(p -> p.text("After")))) {
            CTPPr after = document.getParagraphs().get(1).getCTP().getPPr();

            // Box 1pt; Word takes 0.1 + 3: 2.1pt comes off the 10pt.
            assertThat(DocxTwips.of(after.getSpacing().getBefore())).isEqualTo(158L);
        }
    }

    @Test
    void aRuleInAPaddedSectionStartsAndEndsInsideIt() throws Exception {
        try (XWPFDocument document = export(page -> page.addSection("Plain", s -> s
                .padding(DocumentInsets.of(20))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).fill())))) {
            CTPPr properties = document.getParagraphs().get(0).getCTP().getPPr();

            assertThat(DocxTwips.of(properties.getInd().getLeft())).isEqualTo(20 * 20L);
            assertThat(DocxTwips.of(properties.getInd().getRight())).isEqualTo(20 * 20L);
        }
    }

    @Test
    void aSolidPaintedBarIsARuleToo() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addShape(s -> s.size(100, 2).fill(com.demcha.compose.document.style.DocumentPaint.solid(ACCENT))))) {
            assertThat(hex(bottomOf(document, 0).getColor())).isEqualTo("1A5694");
        }
    }

    @Test
    void aLinkOnARuleIsReportedAsNotCarried() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        XWPFDocument document = withReport(report, page -> page
                .addParagraph(p -> p.text("Top").anchor("top"))
                .addLine(l -> l.horizontal(100).stroke(DocumentStroke.of(ACCENT, 1)).linkTo("top")));
        try (document) {
            assertThat(report.get().bySubject()).containsKey("rule link");
        }
    }

    @Test
    void theStrokeIsPlacedFromTheTopOfTheBoxAsThePageMeasuresItFromTheBottom() {
        var node = new com.demcha.compose.document.dsl.LineBuilder()
                .size(100, 10).from(0, 8).to(100, 8)
                .stroke(DocumentStroke.of(ACCENT, 2)).build();

        assertThat(DocxRules.of(node).centreFromTop()).isEqualTo(2.0);
    }

    private static CTBorder bottomOf(XWPFDocument document, int paragraph) {
        return document.getParagraphs().get(paragraph).getCTP().getPPr().getPBdr().getBottom();
    }

    private static String hex(Object value) {
        if (value instanceof byte[] bytes) {
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02X", b & 0xFF));
            }
            return out.toString();
        }
        return String.valueOf(value);
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 400, 20, content);
    }

    private static XWPFDocument withReport(AtomicReference<DocxExportReport> report,
                                           Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new XWPFDocument(new java.io.ByteArrayInputStream(docx));
        }
    }
}
