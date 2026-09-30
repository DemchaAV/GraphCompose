package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The space above a block reaches Word where Word has nowhere to write it as the page does.
 *
 * <p>{@code PaymentsInvoice}'s header rule is pulled 16.4pt up out of the space its header
 * stack leaves below itself, and its metadata grid is padded 6.2pt down its column. Word has
 * no negative space above a paragraph and no space above a table: the pull was dropped and the
 * padding lost, and in Word the grid stood 6.3pt high and the rule and the page under it 10pt
 * low.</p>
 */
class DocxSpaceAboveTest {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();

    @Test
    void aRulePulledUpTakesItsPullOutOfTheSpaceOwedAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Header").textStyle(BODY).margin(new DocumentInsets(0, 0, 20, 0)))
                .addLine(line -> line.horizontal(200).thickness(1).color(DocumentColor.BLACK)
                        .margin(new DocumentInsets(-8, 0, 0, 0))))) {
            XWPFParagraph rule = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().getPPr() != null && paragraph.getCTP().getPPr().isSetPBdr())
                    .findFirst().orElseThrow();

            assertThat(DocxTwips.of(rule.getCTP().getPPr().getSpacing().getBefore()) / 20.0)
                    .as("20pt owed less the 8pt it is pulled up").isCloseTo(12, within(0.1));
        }
    }

    @Test
    void aParagraphPulledUpFurtherThanTheSpaceAboveItHasNoSpaceAbove() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Header").textStyle(BODY).margin(new DocumentInsets(0, 0, 4, 0)))
                .addParagraph(p -> p.text("Pulled").textStyle(BODY).margin(new DocumentInsets(-10, 0, 0, 0))))) {
            XWPFParagraph pulled = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Pulled")).findFirst().orElseThrow();
            var spacing = pulled.getCTP().getPPr().getSpacing();

            assertThat(spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0L).isZero();
        }
    }

    @Test
    void aPageReferencePulledUpTakesItsPullOutOfTheSpaceOwedAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Header").textStyle(BODY).margin(new DocumentInsets(0, 0, 20, 0)))
                .add(new com.demcha.compose.document.node.PageReferenceNode("Ref", "terms", BODY,
                        com.demcha.compose.document.node.TextAlign.LEFT, "", DocumentInsets.zero(),
                        new DocumentInsets(-8, 0, 0, 0)))
                .addSection(s -> s.anchor("terms").addParagraph(p -> p.text("Terms").textStyle(BODY))))) {
            XWPFParagraph reference = document.getParagraphs().get(1);

            assertThat(reference.getCTP().xmlText()).as("the page reference's paragraph").contains("PAGEREF");
            assertThat(DocxTwips.of(reference.getCTP().getPPr().getSpacing().getBefore()) / 20.0)
                    .as("20pt owed less the 8pt it is pulled up").isCloseTo(12, within(0.1));
        }
    }

    @Test
    void aParagraphPulledUpInsideASectionPulledUpAddsToItsPull() throws Exception {
        // 10pt owed below the header, the section pulls up 4pt and its paragraph 3pt more: the
        // page sets the paragraph 3pt below the header's box.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Header").textStyle(BODY).margin(new DocumentInsets(0, 0, 10, 0)))
                .addSection("Pulled", section -> section
                        .margin(new DocumentInsets(-4, 0, 0, 0))
                        .addParagraph(p -> p.text("Pulled").textStyle(BODY).margin(new DocumentInsets(-3, 0, 0, 0)))))) {
            XWPFParagraph pulled = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Pulled")).findFirst().orElseThrow();

            assertThat(DocxTwips.of(pulled.getCTP().getPPr().getSpacing().getBefore()) / 20.0)
                    .as("10 less 4 less 3").isCloseTo(3, within(0.1));
        }
    }

    @Test
    void aLinePulledUpIntoTheLineAboveInACellTakesItsFoot() throws Exception {
        // CobaltRota's lockup: a subtitle pulled up under its wordmark, in a cell with nothing
        // above to give the pull. The wordmark's line, its letters standing on the baseline, is
        // written 2pt shorter, its text lowered by the four fifths of that Word's baseline would
        // rise, and the subtitle has no space above it.
        DocumentTextStyle wordmark = DocumentTextStyle.builder().fontName(FontName.LATO).size(21).build();
        XWPFParagraph[] plain = lockup(wordmark, 0);
        XWPFParagraph[] pulled = lockup(wordmark, -2);

        double shorter = (DocxTwips.of(lineOf(plain[0])) - DocxTwips.of(lineOf(pulled[0]))) / 20.0;
        assertThat(shorter).as("the wordmark's line gives up the pull").isCloseTo(2, within(0.06));
        assertThat(positionOf(pulled[0]) - positionOf(plain[0])).as("its text lowered as its baseline would rise")
                .isEqualTo(-Math.round(2 * 0.8 * 2));
        assertThat(DocxTwips.of(lineOf(pulled[1]))).as("the pulled line whole, the pull taken above it")
                .isEqualTo(DocxTwips.of(lineOf(plain[1])));
    }

    @Test
    void aLineAboveWithItsTextShadedIsLeftWhole() throws Exception {
        // A shaded run fills its exact line in Word: shortened, the chip would lose its foot.
        DocumentTextStyle wordmark = DocumentTextStyle.builder().fontName(FontName.LATO).size(21).build();
        DocumentTextStyle subtitle = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Head", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addSection("Lockup", cell -> cell
                                .addParagraph(p -> p.inlineHighlight("HALL", wordmark,
                                        DocumentColor.rgb(255, 230, 150), 0, DocumentInsets.zero()))
                                .addParagraph(p -> p.text("QUAYSIDE BAR").textStyle(subtitle)
                                        .margin(new DocumentInsets(-2, 0, 0, 0))))
                        .addParagraph(p -> p.text("MONDAY").textStyle(subtitle))))) {
            XWPFParagraph shaded = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);

            assertThat(positionOf(shaded)).as("not lowered: its line was not cut").isGreaterThan(-3);
            assertThat(shaded.getRuns().get(0).getCTR().getRPr().sizeOfShdArray()).as("a shaded run").isPositive();
        }
    }

    @Test
    void aPullDeeperThanTheLettersAboveLeaveRoomForRisesInsideItsOwnLine() throws Exception {
        // Under 12pt text the line's foot has about 3pt below the letters; the rest of an 8pt
        // pull comes out of the top of the pulled line itself.
        DocumentTextStyle small = DocumentTextStyle.builder().fontName(FontName.LATO).size(12).build();
        XWPFParagraph[] plain = lockup(small, 0);
        XWPFParagraph[] pulled = lockup(small, -8);

        assertThat(DocxTwips.of(lineOf(pulled[0]))).as("the line above gave some").isLessThan(DocxTwips.of(lineOf(plain[0])));
        assertThat(DocxTwips.of(lineOf(pulled[1]))).as("and the pulled line the rest").isLessThan(DocxTwips.of(lineOf(plain[1])));
    }

    /** A wordmark over a 10pt subtitle pulled up by {@code pull}, in a row's cell. */
    private static XWPFParagraph[] lockup(DocumentTextStyle wordmark, double pull) throws Exception {
        DocumentTextStyle subtitle = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Head", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addSection("Lockup", cell -> cell
                                .addParagraph(p -> p.text("HALL").textStyle(wordmark))
                                .addParagraph(p -> p.text("QUAYSIDE BAR").textStyle(subtitle)
                                        .margin(new DocumentInsets(pull, 0, 0, 0))))
                        .addParagraph(p -> p.text("MONDAY").textStyle(subtitle))))) {
            List<XWPFParagraph> paragraphs = document.getTables().get(0).getRow(0).getCell(0).getParagraphs();
            return new XWPFParagraph[]{paragraphs.get(0), paragraphs.get(1)};
        }
    }

    private static Object lineOf(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing().getLine();
    }

    private static long beforeOf(XWPFParagraph paragraph) {
        var spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
    }

    private static int positionOf(XWPFParagraph paragraph) {
        var run = paragraph.getRuns().get(0).getCTR();
        return run.isSetRPr() && run.getRPr().sizeOfPositionArray() > 0
                ? ((Number) run.getRPr().getPositionArray(0).getVal()).intValue() : 0;
    }

    @Test
    void aRowOpeningAPaddedColumnKeepsThePaddingAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addRow("Split", row -> row
                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Issuer").textStyle(BODY))
                        .addSection("Meta", cell -> cell
                                .padding(new DocumentInsets(6, 0, 0, 0))
                                .addRow("Grid", grid -> grid
                                        .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1))
                                        .addParagraph(p -> p.text("Label").textStyle(BODY))
                                        .addParagraph(p -> p.text("Value").textStyle(BODY))))))) {
            XWPFTableCell meta = document.getTables().get(0).getRow(0).getCell(1);
            List<IBodyElement> content = meta.getBodyElements();
            int grid = indexOfTheFirstTable(content);

            assertThat(grid).as("something holds the space above the grid").isPositive();
            double above = 0;
            for (int i = 0; i < grid; i++) {
                var spacing = ((XWPFParagraph) content.get(i)).getCTP().getPPr().getSpacing();
                above += (spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0) / 20.0
                         + DocxTwips.of(spacing.getLine()) / 20.0;
            }
            assertThat(above).as("the column's 6pt, and a hairline").isCloseTo(6, within(0.2));
        }
    }

    private static int indexOfTheFirstTable(List<IBodyElement> content) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) instanceof XWPFTable) {
                return i;
            }
        }
        return -1;
    }
}
