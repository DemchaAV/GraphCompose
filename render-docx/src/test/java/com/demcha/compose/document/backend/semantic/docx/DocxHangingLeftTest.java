package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * A container hanging left by a negative margin takes what it holds out past the text beside
 * it in Word, as on the page.
 *
 * <p>{@code SerifHeadline} hangs each section heading — a dash, the title and a rule, set as a
 * row — left by the dash, so every title starts on the same vertical as the text below it. The
 * export wrote no indent below zero, and in Word each title stood that far right of the page's:
 * 17pt in the main column, 5pt in the sidebar.</p>
 */
class DocxHangingLeftTest {

    private static final double HANG = 17.2;
    private static final double DASH = 12.8;
    private static final double TITLE_PADDING = 9.3;

    @Test
    void aHangingRowInTheBodyIsIndentedOutPastTheMargin() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 30, page -> page
                .addParagraph("Above")
                .add(heading()))) {
            XWPFTable row = document.getTables().get(0);

            assertThat(DocxTwips.of(tableProperties(row).getTblInd().getW()))
                    .as("the row starts the hang left of the margin").isEqualTo(Math.round(-HANG * 20));
        }
    }

    @Test
    void aHangingRowInACellTakesTheHangFromItsDrawnFirstColumn() throws Exception {
        // Word keeps a table nested in a cell inside it whatever its indent, and draws no text
        // past a cell's left edge: the dash's column, which writes nothing, gives the hang up.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 30, page -> page
                .addRow(row -> row
                        .weights(3, 1)
                        .addSection(column -> column.add(heading())
                                .addParagraph(p -> p.text("Senior Engineer").margin(new DocumentInsets(0, 0, 0, 8))))
                        .addParagraph("Sidebar")))) {
            XWPFTable nested = nestedTable(document.getTables().get(0).getRow(0).getCell(0));
            CTTblPr properties = tableProperties(nested);
            double firstColumn = DocxTwips.of(nested.getCTTbl().getTblGrid().getGridColArray(0).getW()) / 20.0;
            XWPFParagraph title = paragraphWithText(nested, "PROJECTS");
            CTInd indent = title.getCTP().getPPr() == null ? null : title.getCTP().getPPr().getInd();
            double titleIndent = indent == null || !indent.isSetLeft() ? 0 : DocxTwips.of(indent.getLeft()) / 20.0;

            assertThat(!properties.isSetTblInd() || DocxTwips.of(properties.getTblInd().getW()) >= 0)
                    .as("no indent Word would ignore").isTrue();
            assertThat(firstColumn).as("the dash's column gives up all but a point").isEqualTo(1.0);
            assertThat(firstColumn + titleIndent)
                    .as("the title starts where the page starts it: the dash and its gap, less the hang")
                    .isCloseTo(DASH + TITLE_PADDING - HANG, offset(0.1));
            assertThat(titleIndent).as("and none of it past the cell's left edge").isNotNegative();

            long grid = 0;
            for (var column : nested.getCTTbl().getTblGrid().getGridColList()) {
                grid += DocxTwips.of(column.getW());
            }
            assertThat(DocxTwips.of(properties.getTblW().getW())).as("the table as narrow as its columns")
                    .isEqualTo(grid);

            XWPFParagraph after = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals("Senior Engineer")).findFirst().orElseThrow();
            assertThat(DocxTwips.of(after.getCTP().getPPr().getInd().getLeft()))
                    .as("the text after the heading keeps its own 8pt indent, the side it is set from")
                    .isEqualTo(8 * 20L);
        }
    }

    @Test
    void aListInACellOfAHangingRowMovesLeftWithTheTextBesideIt() throws Exception {
        // The title and a list under it, both at the cell's padding: the list's items stand where
        // the title does, the marker's hanging indent past it, the hang taken from both.
        RowBuilder row = new RowBuilder().name("HeadingRow");
        row.spacing(0);
        row.columns(DocumentRowColumn.fixed(DASH), DocumentRowColumn.auto(), DocumentRowColumn.weight(1.0));
        row.addSection(cell -> cell.spacing(0).padding(5f, 0f, 0f, 0f)
                .addLine(line -> line.name("Dash").horizontal(DASH).thickness(1).color(DocumentColor.BLACK)));
        row.addSection(cell -> cell.spacing(0).padding(0f, 0f, 0f, (float) TITLE_PADDING)
                .addParagraph(paragraph -> paragraph.name("Title").text("PROJECTS"))
                .addList(list -> list.name("Points").bullet().items("One")));
        row.addSection(cell -> cell.spacing(0).padding(5f, 0f, 0f, 4f)
                .addLine(line -> line.name("Tail").fill().thickness(0.5).color(DocumentColor.BLACK)));
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 30, page -> page
                .addRow(outer -> outer
                        .weights(3, 1)
                        .addSection(column -> column.add(new ShapeContainerBuilder().name("Heading")
                                .rectangle(200, 30).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                                .margin(new DocumentInsets(10, 0, 0, -HANG))
                                .position(row.build(), 0, 0, LayerAlign.CENTER_LEFT)
                                .build()))
                        .addParagraph("Sidebar")))) {
            XWPFTable nested = nestedTable(document.getTables().get(0).getRow(0).getCell(0));
            long title = DocxTwips.of(paragraphWithText(nested, "PROJECTS").getCTP().getPPr().getInd().getLeft());
            long item = DocxTwips.of(paragraphWithText(nested, "One").getCTP().getPPr().getInd().getLeft());
            double firstColumn = DocxTwips.of(nested.getCTTbl().getTblGrid().getGridColArray(0).getW()) / 20.0;

            assertThat(firstColumn + title / 20.0).as("the title where the page starts it, less the hang")
                    .isCloseTo(DASH + TITLE_PADDING - HANG, offset(0.1));
            assertThat(item - title).as("the level's own indent, and nothing else between them").isEqualTo(180L);
        }
    }

    @Test
    void textInACellIsNeverHungPastTheCellsLeftEdge() throws Exception {
        // The dash's column gives 11.8pt of the 17.2 and the title sits 2pt into its cell: the
        // rest would take it past the cell's edge, where Word draws no text.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 30, page -> page
                .addRow(row -> row
                        .weights(3, 1)
                        .addSection(column -> column.add(heading(headingRow(DocumentRowColumn.auto(), 2))))
                        .addParagraph("Sidebar")))) {
            XWPFTable nested = nestedTable(document.getTables().get(0).getRow(0).getCell(0));
            double firstColumn = DocxTwips.of(nested.getCTTbl().getTblGrid().getGridColArray(0).getW()) / 20.0;
            XWPFParagraph title = paragraphWithText(nested, "PROJECTS");
            CTInd indent = title.getCTP().getPPr() == null ? null : title.getCTP().getPPr().getInd();

            assertThat(firstColumn).as("the dash's column gave what it could").isEqualTo(1.0);
            assertThat(indent == null || !indent.isSetLeft() || DocxTwips.of(indent.getLeft()) >= 0)
                    .as("its first letter kept").isTrue();
        }
    }

    @Test
    void aFirstColumnWrittenAsARuleKeepsItsWidth() throws Exception {
        // Outside an overlay the dash is a rule: a paragraph whose border spans its column, so
        // narrowing the column would shorten the dash.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 30, page -> page
                .addRow(row -> row
                        .weights(3, 1)
                        .addSection(column -> column
                                .addSection(hanging -> hanging.margin(new DocumentInsets(0, 0, 0, -HANG))
                                        .add(headingRow(DocumentRowColumn.fixed(80))))
                                .addParagraph("Senior Engineer"))
                        .addParagraph("Sidebar")))) {
            XWPFTable nested = nestedTable(document.getTables().get(0).getRow(0).getCell(0));
            double firstColumn = DocxTwips.of(nested.getCTTbl().getTblGrid().getGridColArray(0).getW()) / 20.0;

            assertThat(firstColumn).as("the dash's column, whole").isCloseTo(DASH, offset(0.1));
            CTInd dash = nested.getRow(0).getCell(0).getParagraphs().get(0).getCTP().getPPr().getInd();
            assertThat(dash == null || !dash.isSetRight() || DocxTwips.of(dash.getRight()) <= 10)
                    .as("and the dash across all of it: the hang its cells take moves text, not widths")
                    .isTrue();
        }
    }

    @Test
    void aListAndARuleInAContainerHangingLeftHangWithIt() throws Exception {
        java.util.function.Predicate<XWPFParagraph> item = paragraph -> paragraph.getText().equals("One");
        java.util.function.Predicate<XWPFParagraph> rule = paragraph -> paragraph.getCTP().getPPr() != null
                && paragraph.getCTP().getPPr().isSetPBdr();

        // Against the same section held in by as much: a flush item writes no indent of its own,
        // leaving its numbering level's.
        assertThat(firstIndent(-HANG, item) - firstIndent(HANG, item)).as("the list's items")
                .isEqualTo(Math.round(-2 * HANG * 20));
        assertThat(firstIndent(-HANG, rule) - firstIndent(HANG, rule)).as("the rule")
                .isEqualTo(Math.round(-2 * HANG * 20));
    }

    /** The left indent of the first body paragraph that matches, in a section hung by {@code margin}. */
    private static long firstIndent(double margin, java.util.function.Predicate<XWPFParagraph> which)
            throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 30, page -> page
                .addParagraph("Above")
                .addSection(section -> section.margin(new DocumentInsets(0, 0, 0, margin))
                        .addList(list -> list.bullet().items("One", "Two"))
                        .addLine(line -> line.name("Divider").thickness(1))))) {
            XWPFParagraph paragraph = document.getParagraphs().stream().filter(which).findFirst().orElseThrow();
            CTInd indent = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getInd();
            return indent == null || !indent.isSetLeft() ? 0 : DocxTwips.of(indent.getLeft());
        }
    }

    @Test
    void aParagraphInAContainerHangingLeftIsIndentedOutPastTheMargin() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 30, page -> page
                .addParagraph("Above")
                .addSection(section -> section.margin(new DocumentInsets(0, 0, 0, -HANG))
                        .addParagraph("Hanging")))) {
            XWPFParagraph hanging = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals("Hanging")).findFirst().orElseThrow();

            assertThat(DocxTwips.of(hanging.getCTP().getPPr().getInd().getLeft()))
                    .as("its text starts the hang left of the margin").isEqualTo(Math.round(-HANG * 20));
        }
    }

    private static DocumentNode heading() {
        return heading(headingRow());
    }

    private static DocumentNode heading(DocumentNode row) {
        return new ShapeContainerBuilder().name("Heading")
                .rectangle(200, 12).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(10, 0, 0, -HANG))
                .position(row, 0, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static DocumentNode headingRow() {
        return headingRow(DocumentRowColumn.auto());
    }

    private static DocumentNode headingRow(DocumentRowColumn titleColumn) {
        return headingRow(titleColumn, TITLE_PADDING);
    }

    private static DocumentNode headingRow(DocumentRowColumn titleColumn, double titlePadding) {
        RowBuilder row = new RowBuilder().name("HeadingRow");
        row.spacing(0);
        row.columns(DocumentRowColumn.fixed(DASH), titleColumn, DocumentRowColumn.weight(1.0));
        row.addSection(cell -> cell.spacing(0).padding(5f, 0f, 0f, 0f)
                .addLine(line -> line.name("Dash").horizontal(DASH).thickness(1).color(DocumentColor.BLACK)));
        row.addSection(cell -> cell.spacing(0).padding(0f, 0f, 0f, (float) titlePadding)
                .addParagraph(paragraph -> paragraph.name("Title").text("PROJECTS")));
        row.addSection(cell -> cell.spacing(0).padding(5f, 0f, 0f, 4f)
                .addLine(line -> line.name("Tail").fill().thickness(0.5).color(DocumentColor.BLACK)));
        return row.build();
    }

    private static CTTblPr tableProperties(XWPFTable table) {
        return table.getCTTbl().getTblPr();
    }

    private static XWPFTable nestedTable(XWPFTableCell cell) {
        for (IBodyElement element : cell.getBodyElements()) {
            if (element instanceof XWPFTable table) {
                return table;
            }
        }
        throw new AssertionError("no table nested in the cell");
    }

    private static XWPFParagraph paragraphWithText(XWPFTable table, String text) {
        for (var row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    if (paragraph.getText().equals(text)) {
                        return paragraph;
                    }
                }
            }
        }
        throw new AssertionError("no paragraph reading " + text);
    }
}
