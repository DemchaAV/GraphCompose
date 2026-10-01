package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's lines are set in a measure as much wider or narrower than the page's as Word
 * sets its text.
 *
 * <p>Word states a type size in half points. {@code EngineeringResume}'s 7.8pt profile, set at
 * 8pt, took a line more than on the page, and its 6.9pt skills broke a word onto a line of its
 * own: each column stood 8 to 9pt low under them.</p>
 */
class DocxWordSizeMeasureTest {

    /** The body's width on a 400pt page with 20pt margins. */
    private static final double ROOM = 360;

    @Test
    void aSizeWordSetsLargerWidensTheMeasureByAsMuch() throws Exception {
        assertThat(rightIndent(paragraph(7.8, TextAlign.LEFT)))
                .as("one line: the measure 8/7.8 of the page's, in full")
                .isEqualTo(-Math.round(ROOM * (8 / 7.8 - 1) * 20));
    }

    @Test
    void aSizeWordSetsSmallerNarrowsIt() throws Exception {
        assertThat(rightIndent(paragraph(9.2, TextAlign.LEFT)))
                .isEqualTo(Math.round(ROOM * (1 - 9 / 9.2) * 20));
    }

    @Test
    void aSizeWordStatesAsItIsLeavesTheMeasureAlone() throws Exception {
        assertThat(rightIndent(paragraph(9.5, TextAlign.LEFT))).isZero();
    }

    @Test
    void aCentredLineIsLeftWhereThePageSetsIt() throws Exception {
        assertThat(rightIndent(paragraph(7.8, TextAlign.CENTER))).isZero();
    }

    @Test
    void theMeasureIsTheOneTheLineThatGrowsMostNeeds() throws Exception {
        // EngineeringResume's projects: a 7.35pt title, set at 7.5, fills most of the first
        // line, and 7.1pt prose, set at 7, the lines under it. Averaged over the paragraph's
        // letters the measure would narrow, and the title's line would no longer fit.
        String title = "GraphCompose (Java 21, PDFBox, Maven, JMH) - Declarative Java PDF layout";
        String prose = " engine. Semantic templates, snapshot testing, and the pipelines that run on them ".repeat(4);
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.rich(rich -> rich.size(title, 7.35).size(prose, 7.1))))) {
            double letters = (title.length() * 7.5 + prose.length() * 7.0) / (title.length() * 7.35 + prose.length() * 7.1);

            assertThat(letters).as("the letters' average narrows").isLessThan(1);
            assertThat(rightIndent(text(document, "GraphCompose")))
                    .as("the title's line is given the room it grows by").isNegative();
        }
    }

    @Test
    void aRunOfItsOwnSizeIsWrittenAtTheSizeTheMeasureIsTakenFor() throws Exception {
        // The body sets Normal at 12pt; the small print states its own size on its run.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text("A body paragraph long enough to set the document's own size.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(12)))
                .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(7.8))))) {
            XWPFParagraph small = text(document, "Platform engineer");

            assertThat(DocxTwips.of(small.getRuns().get(0).getCTR().getRPr().getSzArray(0).getVal()))
                    .as("8pt, in half points").isEqualTo(16L);
            assertThat(rightIndent(small)).isEqualTo(-Math.round(ROOM * (8 / 7.8 - 1) * 20));
        }
    }

    @Test
    void aParagraphInACellIsGivenItsShareOfTheCellsMeasure() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addRow("Page", row -> row
                        .columns(DocumentRowColumn.fixed(200), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(7.8)))
                        .addParagraph("Main")))) {
            XWPFParagraph cell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(p -> p.getText().startsWith("Platform engineer")).findFirst().orElseThrow();

            assertThat(-rightIndent(cell)).as("wider, by its share of a column no wider than 200pt")
                    .isPositive()
                    .isLessThanOrEqualTo(Math.round(200 * (8 / 7.8 - 1) * 20));
        }
    }

    @Test
    void aLineThePageBrokeJustShortOfItsNextWordStaysBroken() throws Exception {
        // CompactMono's "and" was 0.1pt from fitting on the page; given the measure in
        // proportion, Word fitted it. Find a measure as close, then hold Word's clear of it.
        String text = "Built backend services and production document rendering pipelines processing two "
                      + "million documents per month and cutting render latency from seconds to milliseconds.";
        for (double width = 300; width < 360; width += 0.05) {
            double room = width;
            List<ParagraphLine> lines = laidOut(room, text);
            double[] tie = lineAndNextWord(lines);
            if (tie == null || !(tie[1] - room > 0) || !(tie[1] - room < 0.2)) {
                continue;
            }
            try (XWPFDocument document = DocxExports.withLayout(room + 40, 400, 20, page -> page
                    .addParagraph(p -> p.text(text).textStyle(DocumentTextStyle.DEFAULT.withSize(7.95))))) {
                double measure = room - rightIndent(text(document, "Built backend")) / 20.0;
                double grows = 8 / 7.95;

                assertThat(measure).as("every line fits").isGreaterThanOrEqualTo(tie[0] * grows);
                assertThat(measure).as("the next word does not, by a point")
                        .isLessThanOrEqualTo(tie[1] * grows - 0.99);
            }
            return;
        }
        throw new AssertionError("no measure within 0.2pt of a line and its next word");
    }

    /**
     * The widest line, and the narrowest line with a space and its next line's first word, as
     * the page lays them out; a word's width is its own line's, laid out alone.
     */
    private static double[] lineAndNextWord(List<ParagraphLine> lines) {
        double widest = 0;
        double nearest = Double.POSITIVE_INFINITY;
        double space = widthAlone("a a") - 2 * widthAlone("a");
        for (int index = 0; index < lines.size(); index++) {
            widest = Math.max(widest, lines.get(index).width());
            if (index + 1 < lines.size()) {
                String next = lines.get(index + 1).text().strip().split("\\s+")[0];
                nearest = Math.min(nearest, lines.get(index).width() + space + widthAlone(next));
            }
        }
        return lines.size() < 2 ? null : new double[]{widest, nearest};
    }

    private static double widthAlone(String text) {
        return laidOut(500, text).get(0).width();
    }

    private static List<ParagraphLine> laidOut(double room, String text) {
        try (DocumentSession session = GraphCompose.document().pageSize(room + 40, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text(text)
                    .textStyle(DocumentTextStyle.DEFAULT.withSize(7.95))));
            return session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                    .flatMap(fragment -> ((ParagraphFragmentPayload) fragment.payload()).lines().stream())
                    .toList();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static long rightIndent(XWPFParagraph paragraph) {
        CTInd indent = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getInd();
        return indent == null || !indent.isSetRight() ? 0 : DocxTwips.of(indent.getRight());
    }

    private static XWPFParagraph paragraph(double size, TextAlign align) throws Exception {
        XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(size))
                        .align(align)));
        return text(document, "Platform engineer");
    }

    private static XWPFParagraph text(XWPFDocument document, String start) {
        return document.getParagraphs().stream()
                .filter(p -> p.getText().startsWith(start))
                .findFirst().orElseThrow();
    }
}
