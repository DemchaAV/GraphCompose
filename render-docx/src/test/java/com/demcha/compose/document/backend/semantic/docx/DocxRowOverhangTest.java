package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A cell's last line hanging past its row's foot in Word takes that much of the gap under the row.
 *
 * <p>A line held to its icon reaches as far below the page's line as the icon's ink does, and
 * gives that room back out of the space above what follows ({@link DocxSemanticBackend}'s
 * {@code hangingBelow}). The last line of a cell had no "what follows" there: the cell ended,
 * Word made the row that much taller than the page's, and {@code TimelineMinimal}'s page under
 * its header, the last contact line's icon lowered beside it, stood 1.8pt low.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxRowOverhangTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
            + "<circle cx='12' cy='12' r='10' fill='#1A5694'/></svg>");
    private static final DocumentTextStyle SMALL = DocumentTextStyle.DEFAULT.withSize(7);
    private static final double GAP = 10;

    @Test
    void theTallestCellsHangingLineTakesItsReachOutOfTheGapUnderTheRow() throws Exception {
        Exported exported = export(left -> left.addParagraph(p -> p.textStyle(SMALL).text("Name")),
                DocxRowOverhangTest::contact);
        try (XWPFDocument document = exported.document()) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph iconed = cell.getParagraphs().get(cell.getParagraphs().size() - 1);
            long down = reachBelow(iconed, exported.pageLine());

            assertThat(iconed.getCTP().getPPr().getSpacing().getLineRule()).as("held to its icon")
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
            assertThat(down).as("the line reaches below the page's").isPositive();
            assertThat(before(after(document))).as("the gap less that reach").isEqualTo(Math.round(GAP * 20) - down);
        }
    }

    @Test
    void aHangingLineWithRoomUnderItInItsRowTakesNothing() throws Exception {
        // The cell beside it is taller: the line hangs into the row's own room, and Word's row
        // is as tall as the page's.
        try (XWPFDocument document = export(DocxRowOverhangTest::contact,
                right -> {
                    for (int i = 0; i < 4; i++) {
                        right.addParagraph(p -> p.textStyle(SMALL).text("Line"));
                    }
                }).document()) {
            assertThat(before(after(document))).isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aShorterCellSetAtTheRowsFootHasItsRoomAllTheSame() throws Exception {
        // Set at the row's foot, the shorter cell's line hangs past the row on the page; Word sets
        // the cell's content at the foot too, and the row is as tall as its taller cell.
        try (XWPFDocument document = export(DocxRowOverhangTest::contact,
                right -> {
                    for (int i = 0; i < 4; i++) {
                        right.addParagraph(p -> p.textStyle(SMALL).text("Line"));
                    }
                }, com.demcha.compose.document.node.RowVerticalAlign.BOTTOM).document()) {
            assertThat(before(after(document))).isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aHangingLineTakesItsReachOutOfItsCellsPaddingFirst() throws Exception {
        // The cell holds 5pt under its last line: the line hangs into it, which is written that
        // much shorter, and the row is as tall as the page's.
        Exported exported = export(left -> left.addParagraph(p -> p.textStyle(SMALL).text("Name")),
                right -> {
                    right.padding(DocumentInsets.bottom(5));
                    contact(right);
                });
        try (XWPFDocument document = exported.document()) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph iconed = cell.getParagraphs().get(cell.getParagraphs().size() - 1);
            long down = reachBelow(iconed, exported.pageLine());
            CTSpacing spacing = iconed.getCTP().getPPr().getSpacing();

            assertThat(spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0)
                    .as("the padding less the reach").isEqualTo(5 * 20 - down);
            assertThat(before(after(document))).isEqualTo(Math.round(GAP * 20));
        }
    }

    /** A contact stack whose last line holds an icon lowered as TimelineMinimal's are. */
    private static void contact(SectionBuilder section) {
        section.spacing(3);
        section.addParagraph(p -> p.textStyle(SMALL).text("London"));
        section.addParagraph(p -> p.name("Iconed").textStyle(SMALL).inlineText("GitHub  ", SMALL)
                .inlineSvgIcon(ICON, 10.5, InlineImageAlignment.CENTER, -1.35, null));
    }

    /** The export, and the height the layout gives the icon line. */
    private record Exported(XWPFDocument document, double pageLine) {
    }

    private static Exported export(Consumer<SectionBuilder> left, Consumer<SectionBuilder> right) throws Exception {
        return export(left, right, com.demcha.compose.document.node.RowVerticalAlign.TOP);
    }

    private static Exported export(Consumer<SectionBuilder> left, Consumer<SectionBuilder> right,
                                   com.demcha.compose.document.node.RowVerticalAlign align) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page
                    .spacing(GAP)
                    .addRow(row -> row.verticalAlign(align).addSection("Left", left).addSection("Right", right))
                    .addParagraph(p -> p.textStyle(SMALL).text("After")));
            double pageLine = session.layoutGraph().nodes().stream()
                    .filter(node -> "Iconed".equals(node.semanticName()))
                    .findFirst().orElseThrow().placementHeight();
            return new Exported(new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend()))),
                    pageLine);
        }
    }

    /** How far a line held to its icon reaches below the page's line, in twips: line less page and up. */
    private static long reachBelow(XWPFParagraph iconed, double pageLine) {
        CTSpacing spacing = iconed.getCTP().getPPr().getSpacing();
        long up = 3 * 20 - (spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0);
        long line = DocxTwips.of(spacing.getLine());
        return line - up - Math.round(pageLine * 20);
    }

    private static XWPFParagraph after(XWPFDocument document) {
        return document.getParagraphs().stream().filter(p -> p.getText().equals("After")).findFirst().orElseThrow();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }
}
