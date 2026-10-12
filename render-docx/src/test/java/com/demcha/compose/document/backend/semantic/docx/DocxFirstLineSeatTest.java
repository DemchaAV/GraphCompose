package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The first line of a panel or a table's cell stands where the page sets it, where nothing was
 * written above it to raise it into.
 *
 * <p>Word stands an exact line's baseline four fifths of the way down it, and the default text,
 * Helvetica at 14pt, {@link DocxExports#DEFAULT_LINE_RAISE} twips lower than the page. A line
 * opening a cell has no space above it of its own: the cell's padding is its margin, and a row
 * of columns' space above is written above its table. What the space above cannot give, the
 * padding gives, and above a row of columns the space above its table; what follows the line
 * keeps its place.</p>
 */
class DocxFirstLineSeatTest {

    private static final long RAISE = DocxExports.DEFAULT_LINE_RAISE;
    private static final long SMALL_RAISE = DocxExports.SMALL_LINE_RAISE;
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentStroke RULE = DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 2);
    private static final DocumentTextStyle SMALL = DocumentTextStyle.DEFAULT.withSize(7);

    @Test
    void aPanelsFirstLineRisesIntoItsPadding() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before").addSection("Card", card -> card
                .fillColor(SURFACE).padding(DocumentInsets.of(8)).addParagraph("Inside")));
        XWPFTableCell panel = onlyTable(exported.document()).getRow(0).getCell(0);
        XWPFParagraph inside = panel.getParagraphs().get(0);

        assertThat(topMargin(panel)).as("its padding, less what its line rose into").isEqualTo(8 * 20 - RAISE);
        assertThat(after(inside)).as("given back below the line, what follows where it was").isEqualTo(RAISE);
        assertThat(position(inside)).isZero();
        assertThat((long) panel.getTableRow().getHeight()).as("held as much taller as Word adds the margin left")
                .isEqualTo(Math.round((exported.heights().get("Card") - 16) * 20) + RAISE);
    }

    @Test
    void aPanelsTopBorderKeepsWhatItTakesOfThePadding() throws Exception {
        // Padded as far as its top border reaches past its box: the border takes the padding.
        Exported exported = export(page -> page.addParagraph("Before").addSection("Card", card -> card
                .fillColor(SURFACE).borders(DocumentBorders.top(RULE)).padding(DocumentInsets.of(2))
                .addParagraph("Inside")));
        XWPFTableCell panel = onlyTable(exported.document()).getRow(0).getCell(0);

        assertThat(topMargin(panel)).isZero();
        assertThat(after(panel.getParagraphs().get(0))).as("nothing risen, nothing owed").isZero();
        assertThat(exported.report().bySubject()).doesNotContainKey("SectionNode");
    }

    @Test
    void aLineHalfAPointLowAndMoreKeepsItsPosition() throws Exception {
        // Spectral's 30pt line stands 4.9pt low in Word: the position moves it, as before.
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                .padding(DocumentInsets.of(8)).addParagraph(p -> p.text("Title").textStyle(spectral(30)))));
        XWPFTableCell panel = onlyTable(exported.document()).getRow(0).getCell(0);

        assertThat(topMargin(panel)).as("its padding, all of it").isEqualTo(8 * 20);
        assertThat(position(panel.getParagraphs().get(0))).isPositive();
    }

    @Test
    void aComposedCellsFirstLineRisesIntoItsCellsPadding() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(160))
                .rowCells(composed(new ParagraphBuilder().text("Inside").build())))
                .addParagraph("After"))) {
            XWPFTableCell cell = onlyTable(document).getRow(0).getCell(0);
            XWPFParagraph inside = cell.getParagraphs().get(0);

            assertThat(topMargin(cell)).as("its padding, less what its line rose into").isEqualTo(4 * 20 - RAISE);
            assertThat(after(inside)).isEqualTo(RAISE);
            assertThat(position(inside)).isZero();
        }
    }

    @Test
    void aCellBesideOneOpeningWithAPanelKeepsItsPadding() throws Exception {
        // A badge, a painted section, is a table in its cell, which keeps its margin; Word gives the
        // row's cells its margin, the largest, and a line risen into a smaller one would not rise.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                        .columns(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160))
                        .rowCells(composed(new ParagraphBuilder().text("Inside").build()),
                                composed(new SectionBuilder().name("Badge").fillColor(SURFACE)
                                        .padding(DocumentInsets.of(4)).addParagraph("Paid").build())))
                .addParagraph("After"))) {
            XWPFTableCell inside = onlyTable(document).getRow(0).getCell(0);

            assertThat(onlyTable(document).getRow(0).getCell(1).getBodyElements().get(0))
                    .as("the premise: the badge is a table").isInstanceOf(XWPFTable.class);
            assertThat(topMargin(inside)).as("its padding, all of it").isEqualTo(4 * 20);
            assertThat(after(inside.getParagraphs().get(0))).isZero();
        }
    }

    @Test
    void aCellBesideAPictureRisesIntoItsPadding() throws Exception {
        // A picture leaves its cell a paragraph to hold it: the row's margins even.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                        .columns(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160))
                        .rowCells(composed(new ParagraphBuilder().text("Inside").build()),
                                composed(new com.demcha.compose.document.dsl.ImageBuilder()
                                        .source(com.demcha.compose.document.image.DocumentImageData.fromBytes(png()))
                                        .size(12, 12).build())))
                .addParagraph("After"))) {
            XWPFTableCell inside = onlyTable(document).getRow(0).getCell(0);

            assertThat(onlyTable(document).getRow(0).getCell(1).getBodyElements().get(0))
                    .as("the premise: the picture's cell opens with a paragraph").isInstanceOf(XWPFParagraph.class);
            assertThat(topMargin(inside)).as("its padding, less what its line rose into").isEqualTo(4 * 20 - RAISE);
            assertThat(after(inside.getParagraphs().get(0))).isEqualTo(RAISE);
        }
    }

    @Test
    void aPanelSplitOverAPageKeepsItsPadding() throws Exception {
        // Only a panel written whole is held to a height its raise can be added to.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addSection("Card", card -> card
                .fillColor(SURFACE).padding(DocumentInsets.of(8)).addParagraph("First")
                .addPageBreak(b -> b.name("Break")).addParagraph("Second")))) {
            XWPFTableCell first = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(document.getTables()).as("the premise: a table a piece").hasSize(2);
            assertThat(topMargin(first)).as("its padding, all of it").isEqualTo(8 * 20);
            assertThat(after(first.getParagraphs().get(0))).isZero();
        }
    }

    @Test
    void theCellBesideOneThatRoseKeepsItsPlace() throws Exception {
        // Lato at 10pt Word sets high: it does not rise, and takes the row's margin back above it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(160), DocumentTableColumn.fixed(160))
                .rowCells(composed(new ParagraphBuilder().text("Inside").build()),
                        composed(new ParagraphBuilder().text("Beside").textStyle(lato(10)).build())))
                .addParagraph("After"))) {
            XWPFTableCell beside = onlyTable(document).getRow(0).getCell(1);

            assertThat(topMargin(beside)).as("the row's margin").isEqualTo(4 * 20 - RAISE);
            assertThat(before(beside.getParagraphs().get(0))).as("what it gave, above its line").isEqualTo(RAISE);
            assertThat(after(beside.getParagraphs().get(0))).isZero();
        }
    }

    @Test
    void aLineInAComposedCellRisesIntoTheSpaceAboveIt() throws Exception {
        // A composed cell has no placement of its own, but its row is kept on one page.
        DocumentTableCell cell = DocumentTableCell.node(new SectionBuilder().name("Two").spacing(9)
                        .addParagraph("First").addParagraph("Second").build())
                .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.zero()).build());
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(160)).rowCells(cell))
                .addParagraph("After"))) {
            List<XWPFParagraph> lines = onlyTable(document).getRow(0).getCell(0).getParagraphs();

            assertThat(lines).extracting(XWPFParagraph::getText).containsExactly("First", "Second");
            assertThat(before(lines.get(1))).isEqualTo(9 * 20 - RAISE);
            assertThat(after(lines.get(1))).isEqualTo(RAISE);
        }
    }

    @Test
    void aRowsFirstLinesRiseOutOfTheSpaceAboveItsTable() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addRow(row -> row.addParagraph("Left").addParagraph("Right"))
                .addParagraph("After"))) {
            XWPFTable table = onlyTable(document);

            assertThat(after(paragraphBefore(document, table))).as("the gap, less the lift").isEqualTo(10 * 20 - RAISE);
            for (XWPFTableCell cell : table.getRow(0).getTableCells()) {
                assertThat(before(cell.getParagraphs().get(0))).isZero();
                assertThat(after(cell.getParagraphs().get(0))).as("given back below the line").isEqualTo(RAISE);
            }
        }
    }

    @Test
    void aRowUnderAnotherRisesOutOfTheFootOfTheOneAbove() throws Exception {
        // Two rows of columns in a row are two tables held apart by an empty paragraph; where the
        // columns above end padded, the gap is theirs: their cells end with it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(0)
                .addParagraph("Above")
                .addRow(row -> row
                        .addSection("Left", column -> column.padding(new DocumentInsets(0, 0, 10, 0)).addParagraph("Left"))
                        .addSection("Right", column -> column.padding(new DocumentInsets(0, 0, 10, 0)).addParagraph("Right")))
                .addRow(row -> row.addParagraph("Lower").addParagraph("Other"))
                .addParagraph("After"))) {
            XWPFTable upper = document.getTables().get(0);
            XWPFTable lower = document.getTables().get(1);

            for (XWPFTableCell cell : upper.getRow(0).getTableCells()) {
                assertThat(after(cell.getParagraphs().get(0))).as("the gap, less the lift under it")
                        .isEqualTo(10 * 20 - RAISE);
            }
            for (XWPFTableCell cell : lower.getRow(0).getTableCells()) {
                assertThat(after(cell.getParagraphs().get(0))).as("given back below the line").isEqualTo(RAISE);
            }
        }
    }

    @Test
    void aPictureAboveARowKeepsTheSpaceAboveIt() throws Exception {
        // A paragraph holding a picture is no empty separator: its space above places the picture.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(0)
                .addParagraph("Above")
                .addImage(image -> image.source(com.demcha.compose.document.image.DocumentImageData.fromBytes(png()))
                        .size(20, 20).margin(new DocumentInsets(10, 0, 0, 0)))
                .addRow(row -> row.addParagraph("Left").addParagraph("Right"))
                .addParagraph("After"))) {
            XWPFTable table = onlyTable(document);
            XWPFParagraph picture = paragraphBefore(document, table);

            assertThat(picture.getText()).as("the premise: the picture's paragraph, holding no text").isEmpty();
            assertThat(before(picture)).isEqualTo(10 * 20);
            assertThat(after(table.getRow(0).getCell(0).getParagraphs().get(0))).isZero();
        }
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 8,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void aCellWhoseLineCallsForLessStartsThatMuchLower() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addRow(row -> row.addParagraph("Left").addParagraph(p -> p.text("Right").textStyle(SMALL)))
                .addParagraph("After"))) {
            XWPFTableCell small = onlyTable(document).getRow(0).getCell(1);

            assertThat(before(small.getParagraphs().get(0))).as("lifted further than it called for")
                    .isEqualTo(RAISE - SMALL_RAISE);
            assertThat(after(small.getParagraphs().get(0))).as("what it rose").isEqualTo(SMALL_RAISE);
        }
    }

    @Test
    void aCellWordSetsHighStartsLowerByTheWholeLift() throws Exception {
        // Lato at 10pt Word sets high: it does not rise, and keeps its place under the lift.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addRow(row -> row.addParagraph("Left").addParagraph(p -> p.text("Right").textStyle(lato(10))))
                .addParagraph("After"))) {
            XWPFTableCell high = onlyTable(document).getRow(0).getCell(1);

            assertThat(before(high.getParagraphs().get(0))).isEqualTo(RAISE);
            assertThat(after(high.getParagraphs().get(0))).isZero();
        }
    }

    @Test
    void aLineRisenWithItsRowGivesItBackAboveTheLineUnderIt() throws Exception {
        // One distance between two lines, on one side of them: LibreOffice takes the larger of a
        // space below one line and above the next, where Word adds them.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addRow(row -> row.weights(1, 1)
                        .addSection("Entry", column -> column.spacing(4).addParagraph("Title").addParagraph("Detail"))
                        .addParagraph("Right"))
                .addParagraph("After"))) {
            List<XWPFParagraph> entry = onlyTable(document).getRow(0).getCell(0).getParagraphs();

            assertThat(entry).extracting(XWPFParagraph::getText).containsExactly("Title", "Detail");
            assertThat(after(entry.get(0))).isZero();
            // The detail's own raise came out of its 4pt above, and the title's comes back into it.
            assertThat(before(entry.get(1))).isEqualTo(4 * 20);
        }
    }

    @Test
    void aRowOfLinesUnderATenthOfAPointLowIsNotLifted() throws Exception {
        // Helvetica at 4pt stands 0.09pt low in Word: a twip less of space moves a line on Word's
        // grid a step or nothing.
        DocumentTextStyle tiny = DocumentTextStyle.DEFAULT.withSize(4);
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addRow(row -> row.addParagraph(p -> p.text("Left").textStyle(tiny))
                        .addParagraph(p -> p.text("Right").textStyle(tiny)))
                .addParagraph("After"))) {
            XWPFTable table = onlyTable(document);

            assertThat(after(paragraphBefore(document, table))).as("the gap, whole").isEqualTo(10 * 20);
            assertThat(after(table.getRow(0).getCell(0).getParagraphs().get(0))).isZero();
        }
    }

    @Test
    void aRowWhoseColumnOpensWithAPanelIsNotLifted() throws Exception {
        // ClassicInvoice's notes: a column ruled down its left side is a panel in its cell, which
        // would rise whole with the row.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.spacing(10)
                .addParagraph("Above")
                .addRow(row -> row.weights(1, 1)
                        .addSection("Notes", column -> column.accentLeft(DocumentColor.rgb(196, 152, 76), 3)
                                .padding(new DocumentInsets(0, 0, 0, 8)).addParagraph("Notes"))
                        .addParagraph("Right"))
                .addParagraph("After"))) {
            XWPFTable table = onlyTable(document);

            assertThat(table.getRow(0).getCell(0).getBodyElements().get(0)).as("the premise: a panel")
                    .isInstanceOf(XWPFTable.class);
            assertThat(after(paragraphBefore(document, table))).as("the gap, whole").isEqualTo(10 * 20);
            assertThat(after(table.getRow(0).getCell(1).getParagraphs().get(0))).isZero();
        }
    }

    @Test
    void aRowTheLayoutMovesToANewPageIsNotLifted() throws Exception {
        // Seven lines 10pt apart fill the 160pt of the first page, and the row starts the next:
        // the space above it stays on the page above, and lifted out of it the row would stand lower.
        Consumer<PageFlowBuilder> content = page -> {
            page.spacing(10);
            for (int i = 0; i < 7; i++) {
                page.addParagraph("Line " + i);
            }
            page.addRow(row -> row.name("Row").addParagraph("Left").addParagraph("Right"));
            page.addParagraph("After");
        };
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            assertThat(session.layoutGraph().nodes().stream().filter(node -> "Row".equals(node.semanticName()))
                    .findFirst().orElseThrow().startPage()).as("the premise: the row starts the second page").isOne();
        }
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, content)) {
            for (XWPFTableCell cell : onlyTable(document).getRow(0).getTableCells()) {
                assertThat(after(cell.getParagraphs().get(0))).isZero();
            }
        }
    }

    @Test
    void aRowHeldToAHeightIsNotLifted() throws Exception {
        // In a panel, a row is held as tall as the page makes it, which the space its cells take
        // would not make taller: what follows would stand higher by the lift.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page.addSection("Card", card -> card
                .fillColor(SURFACE).spacing(10).addParagraph("Above")
                .addRow(row -> row.addParagraph("Left").addParagraph("Right")).addParagraph("After"))
                .addParagraph("After"))) {
            XWPFTable row = onlyTable(document).getRow(0).getCell(0).getTables().get(0);

            assertThat(row.getRow(0).getCtRow().getTrPr().sizeOfTrHeightArray()).as("the premise: held").isOne();
            for (XWPFTableCell cell : row.getRow(0).getTableCells()) {
                assertThat(after(cell.getParagraphs().get(0))).isZero();
            }
        }
    }

    private static DocumentTableCell composed(com.demcha.compose.document.node.DocumentNode node) {
        return DocumentTableCell.node(node).withStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(4))
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 0)).build());
    }

    private static DocumentTextStyle spectral(double size) {
        return DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(size).build();
    }

    private static DocumentTextStyle lato(double size) {
        return DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
    }

    private static XWPFTable onlyTable(XWPFDocument document) {
        return document.getTables().get(0);
    }

    private static XWPFParagraph paragraphBefore(XWPFDocument document, IBodyElement element) {
        List<IBodyElement> body = document.getBodyElements();
        return (XWPFParagraph) body.get(body.indexOf(element) - 1);
    }

    private static long topMargin(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        return properties == null || !properties.isSetTcMar() || !properties.getTcMar().isSetTop() ? 0
                : DocxTwips.of(properties.getTcMar().getTop().getW());
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraph);
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter());
    }

    /** The {@code w:position} of a paragraph's first run, in half points; 0 for none. */
    private static long position(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getRArray(0).getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).longValue();
    }

    private record Exported(XWPFDocument document, DocxExportReport report, Map<String, Double> heights) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        Map<String, Double> heights = new HashMap<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            session.layoutGraph().nodes().forEach(node -> {
                if (node.semanticName() != null) {
                    heights.putIfAbsent(node.semanticName(), node.placementHeight());
                }
            });
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), heights);
    }
}
