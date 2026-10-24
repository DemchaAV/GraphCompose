package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A row or an alignment composed in a table's cell stands where the page puts it.
 *
 * <p>A cell built from a node lays its content out under the table's path, so neither a row
 * nor an alignment in it has a place of its own in the layout. A row the page divides by what
 * its children measure had its columns left to Word, which sized them to their text — an
 * amount the page sets at the cell's right edge stood at its middle — and an alignment's child
 * was written from the cell's left. The paragraphs' lines are the table's, and say where each
 * box starts.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxComposedCellGeometryTest {

    /** The cell's text starts 4pt in from its edge at 20, as its margin in the file says. */
    private static final double CELL_TEXT_LEFT = 24;
    private static final double CELL_TEXT_RIGHT = 316;

    @Test
    void aRowSetAtEitherEndOfItsCellKeepsTheColumnsThePageGivesIt() throws Exception {
        Export export = inACell(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                .addParagraph("Total").addParagraph("$120").build());
        double amount = export.lineAt("$120").x();

        assertThat(export.rowGrid()).containsExactly(
                twips(amount - CELL_TEXT_LEFT), twips(CELL_TEXT_RIGHT - amount));
        assertThat(DocxExports.besidesTheExactLines(export.report())).isEmpty();
    }

    @Test
    void aRowWithAnAutoColumnInACellStartsTheNextWhereThePageDoes() throws Exception {
        Export export = inACell(new RowBuilder().columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph("Label:").addParagraph("A value set after its label").build());
        double value = export.lineAt("A value set after its label").x();

        // An auto column is a point wider than its text, taken from the weight column beside it.
        assertThat(export.rowGrid()).containsExactly(
                twips(value - CELL_TEXT_LEFT + 1), twips(CELL_TEXT_RIGHT - value - 1));
    }

    @Test
    void aSpacerThatGrowsInACellTakesTheRoomBetweenItsNeighbours() throws Exception {
        Export export = inACell(new RowBuilder().spacing(8)
                .addParagraph("Left").flexSpacer().addParagraph("Right").build());
        PlacedFragment left = export.lineAt("Left");
        double right = export.lineAt("Right").x();
        // The spacer starts a gap past the line before it; each column runs to the next's start.
        double spacer = left.x() + left.width() + 8;

        assertThat(export.rowGrid()).containsExactly(
                twips(spacer - CELL_TEXT_LEFT),
                twips(right - spacer),
                twips(CELL_TEXT_RIGHT - right));
    }

    @Test
    void aRowWhoseChildTheLayoutDoesNotReportInACellIsNamed() throws Exception {
        // A picture composed in a cell has no lines to say where it starts.
        Export export = inACell(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                .addParagraph("Logo").add(picture(20)).build());

        assertThat(export.rowGrid()).as("left to Word, as before").isEmpty();
        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("RowNode"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).isEqualTo(
                        "its columns are Word's, sized to their text: the page divides it by its arrangement, and "
                        + "composed in a table cell, it holds a child the layout does not place in the cell"));
    }

    @Test
    void aSpacerThatDoesNotGrowStandsWhereTheArrangementSpreadsTheRoom() throws Exception {
        // Between two lines, an arrangement gives each of the two gaps half the room they leave.
        Export export = inACell(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                .addParagraph("Left").addSpacer(10).addParagraph("Right").build());
        PlacedFragment left = export.lineAt("Left");
        double right = export.lineAt("Right").x();
        double spacer = left.x() + left.width() + (right - left.x() - left.width() - 10) / 2;

        assertThat(export.rowGrid()).containsExactly(
                twips(spacer - CELL_TEXT_LEFT),
                twips(right - spacer),
                twips(CELL_TEXT_RIGHT - right));
    }

    @Test
    void aRowInAPanelInsideACellIsNamed() throws Exception {
        Export export = inACell(new SectionBuilder().fillColor(DocumentColor.rgb(238, 242, 247))
                .add(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                        .addParagraph("Total").addParagraph("$120").build())
                .build());

        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("RowNode"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).endsWith(
                        "by its arrangement, and written inside a panel or another table in its table cell"));
    }

    @Test
    void aRowCentredInItsCellKeepsWhereThePageCentresItsChildren() throws Exception {
        Export export = inACell(new RowBuilder().arrangement(RowArrangement.CENTER)
                .addParagraph("One").addParagraph("Two").build());
        double one = export.lineAt("One").x();
        double two = export.lineAt("Two").x();

        // The first column holds the room before the first child, as a placed row's does.
        assertThat(export.rowGrid()).containsExactly(twips(two - CELL_TEXT_LEFT), twips(CELL_TEXT_RIGHT - two));
        assertThat(one).as("the page centres the pair").isGreaterThan(CELL_TEXT_LEFT + 50);
    }

    @Test
    void aSpacerSpreadAroundStandsWhereThePageSetsIt() throws Exception {
        Export export = inACell(new RowBuilder().arrangement(RowArrangement.SPACE_AROUND)
                .addParagraph("Left").addSpacer(10).addParagraph("Right").build());
        PlacedFragment left = export.lineAt("Left");
        double right = export.lineAt("Right").x();
        double spacer = left.x() + left.width() + (right - left.x() - left.width() - 10) / 2;

        assertThat(export.rowGrid()).containsExactly(
                twips(spacer - CELL_TEXT_LEFT), twips(right - spacer), twips(CELL_TEXT_RIGHT - right));
    }

    @Test
    void aPaddedRowInACellTakesItsColumnsFromItsLinesInsideThePadding() throws Exception {
        Export export = inACell(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                .padding(new DocumentInsets(0, 12, 0, 8)).margin(new DocumentInsets(0, 6, 0, 10))
                .addParagraph("Total").addParagraph("$120").build());
        double amount = export.lineAt("$120").x();
        // The row stands inside its margins, and its padding stays in its outer columns.
        double rowLeft = CELL_TEXT_LEFT + 10;

        assertThat(export.lineAt("Total").x()).isEqualTo(rowLeft + 8, org.assertj.core.data.Offset.offset(0.01));
        assertThat(export.rowGrid()).containsExactly(
                twips(amount - rowLeft), twips(CELL_TEXT_RIGHT - 6 - amount));
    }

    @Test
    void anEmptyParagraphInARowOfACellStandsInAColumnOfItsOwn() throws Exception {
        Export export = inACell(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                .addParagraph("Discount").addParagraph("").build());

        assertThat(export.rowGrid()).hasSize(2);
        assertThat(DocxExports.besidesTheExactLines(export.report())).isEmpty();
    }

    @Test
    void aSpacerInARowOfStatedColumnsInACellIsNamed() throws Exception {
        // Stated columns give the spacer a slot of its own, which no line reports.
        Export export = inACell(new RowBuilder()
                .columns(DocumentRowColumn.weight(2), DocumentRowColumn.weight(1), DocumentRowColumn.auto())
                .addParagraph("A description").addSpacer(0).addParagraph("$1,200").build());

        assertThat(export.rowGrid()).isEmpty();
        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("RowNode"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).endsWith(
                        "by an auto column, and composed in a table cell, it holds a child the layout does not "
                        + "place in the cell"));
    }

    @Test
    void anAlignedRowInACellSaysNothing() throws Exception {
        // A row is as wide as the width it is set in: the page aligns nothing.
        Export export = inACell(new AlignNode(new RowBuilder().weights(1, 1)
                .addParagraph("One").addParagraph("Two").build(), HorizontalAlign.CENTER));

        assertThat(export.report().notes()).noneMatch(note -> note.subject().equals("Align"));
    }

    @Test
    void aLinePulledUpPastItsCellsTopStillStandsInItsCell() throws Exception {
        // A section pulls its first line up past the cell's padding: the line is still the cell's.
        Export export = inACell(new SectionBuilder().margin(new DocumentInsets(-8, 0, 0, 0))
                .add(new AlignNode(new ParagraphBuilder().text("Total due").build(), HorizontalAlign.RIGHT))
                .build());

        assertThat(export.report().notes()).noneMatch(note -> note.subject().equals("Align"));
        assertThat(DocxTwips.of(export.paragraph("Total due").getCTP().getPPr().getInd().getLeft()))
                .isEqualTo(twips(export.lineAt("Total due").x() - CELL_TEXT_LEFT));
    }

    @Test
    void aRowWhoseColumnsAreStatedInACellIsWrittenAsBefore() throws Exception {
        Export export = inACell(new RowBuilder().weights(1, 2).addParagraph("One").addParagraph("Two").build());

        assertThat(export.rowGrid()).hasSize(2);
        assertThat(DocxExports.besidesTheExactLines(export.report())).isEmpty();
    }

    @Test
    void aParagraphRightAlignedInACellIsHeldInToWhereThePageSetsIt() throws Exception {
        Export export = inACell(new AlignNode(new ParagraphBuilder().text("Total due").build(), HorizontalAlign.RIGHT));
        double start = export.lineAt("Total due").x();

        assertThat(DocxTwips.of(export.paragraph("Total due").getCTP().getPPr().getInd().getLeft()))
                .isEqualTo(twips(start - CELL_TEXT_LEFT));
        assertThat(DocxExports.besidesTheExactLines(export.report())).isEmpty();
    }

    @Test
    void aPictureCentredInACellIsHeldInToWhereThePageCentresIt() throws Exception {
        Export export = inACell(new AlignNode(picture(40), HorizontalAlign.CENTER));
        // 292pt between the cell's margins, a 40pt picture: 126pt either side.
        CTInd indent = export.pictureParagraph().getCTP().getPPr().getInd();

        assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(126 * 20L);
        assertThat(DocxTwips.of(indent.getRight())).isEqualTo(126 * 20L);
        assertThat(DocxExports.besidesTheExactLines(export.report())).isEmpty();
    }

    @Test
    void anAlignmentOfAChildTheLayoutDoesNotReportInACellIsNamed() throws Exception {
        Export export = inACell(new AlignNode(new SectionBuilder().addParagraph("Caption").build(),
                HorizontalAlign.CENTER));

        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("Align"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).isEqualTo(
                        "composed in a table cell, its child is written from the cell's left, where the page "
                        + "centres it: the layout does not say how wide a SectionNode composed in a cell is"));
    }

    @Test
    void anAlignmentInAPanelInsideACellIsNamed() throws Exception {
        Export export = inACell(new SectionBuilder().fillColor(DocumentColor.rgb(238, 242, 247))
                .add(new AlignNode(new ParagraphBuilder().text("Total due").build(), HorizontalAlign.RIGHT))
                .build());

        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("Align"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).endsWith(
                        "right-aligns it: written inside a panel or another table in its cell, it has no box of "
                        + "its own to be held in to"));
    }

    @Test
    void anAlignedParagraphWhoseLinesStandInAnotherCellIsNamedNotHeldIn() throws Exception {
        // Read as markdown, the first cell's "**Total**" is laid out as "Total": the plain "Total"
        // after it takes that line, in the first cell, before its own.
        Export export = export(page -> page.addTable(table -> table
                        .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("**Total**").build()),
                                DocumentTableCell.node(new AlignNode(new ParagraphBuilder().text("Total").build(),
                                        HorizontalAlign.RIGHT)))),
                true);

        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("Align"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).endsWith(
                        "right-aligns it: the lines found for it stand outside its cell"));
        XWPFParagraph total = export.document().getTables().get(0).getRow(0).getCell(1).getParagraphs().get(0);
        assertThat(total.getCTP().getPPr() == null || !total.getCTP().getPPr().isSetInd())
                .as("not held in to the other cell's place").isTrue();
    }

    @Test
    void anAlignedParagraphWhoseLinesStandInTheRowAboveIsNamedNotHeldIn() throws Exception {
        // The same column, a row apart: the lines are across the cell's width, but not in it.
        Export export = export(page -> page.addTable(table -> table
                        .columns(DocumentTableColumn.fixed(300))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("**Total**").build()))
                        .rowCells(DocumentTableCell.node(new AlignNode(new ParagraphBuilder().text("Total").build(),
                                HorizontalAlign.RIGHT)))),
                true);

        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("Align"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).endsWith(
                        "right-aligns it: the lines found for it stand outside its cell"));
    }

    @Test
    void aRowWhoseLinesStandInAnotherCellIsNamed() throws Exception {
        Export export = export(page -> page.addTable(table -> table
                        .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("**Total**").build()),
                                DocumentTableCell.node(new RowBuilder().arrangement(RowArrangement.SPACE_BETWEEN)
                                        .addParagraph("Total").addParagraph("$1").build()))),
                true);

        assertThat(export.report().notes())
                .filteredOn(note -> note.subject().equals("RowNode"))
                .singleElement()
                .satisfies(note -> assertThat(note.detail()).endsWith(
                        "composed in a table cell, it holds a child the layout does not place in the cell"));
    }

    @Test
    void aPaddedPictureSizedToItsCellKeepsThePagesWidth() throws Exception {
        // Unsized and wider than its cell, the picture is drawn the width the cell leaves it, less
        // its padding: 292pt less 20.
        Export export = inACell(new AlignNode(new ImageBuilder().name("Banner")
                .source(DocumentImageData.fromBytes(png(400))).padding(DocumentInsets.of(10)).build(),
                HorizontalAlign.CENTER));

        assertThat(export.document().getDocument().xmlText())
                .contains("cx=\"" + Math.round(272 * 12700.0) + "\"");
        assertThat(export.report().notes()).noneMatch(note -> note.subject().equals("Align"));
    }

    @Test
    void anAlignmentWithNoLayoutBehindTheExportIsNotTakenForOneInACell() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 600, 20, page -> page
                .addAligned(HorizontalAlign.CENTER, new ParagraphBuilder().text("Centred").build()));

        assertThat(report.notes()).noneMatch(note -> note.subject().equals("Align"));
    }

    @Test
    void aDrawingAlignedInAPanelInsideACellSaysNothing() throws Exception {
        // Its table draws it where the page draws it, the alignment's place included.
        Export export = inACell(new SectionBuilder().fillColor(DocumentColor.rgb(238, 242, 247))
                .add(new AlignNode(new ShapeBuilder().size(12, 12).fillColor(DocumentColor.rgb(26, 86, 148)).build(),
                        HorizontalAlign.CENTER))
                .addParagraph("Label")
                .build());

        assertThat(export.report().notes()).noneMatch(note -> note.subject().equals("Align"));
    }

    @Test
    void aLeftAlignmentInACellIsWrittenFromTheLeftAndSaysNothing() throws Exception {
        Export export = inACell(new AlignNode(new ParagraphBuilder().text("Total due").build(), HorizontalAlign.LEFT));

        XWPFParagraph paragraph = export.paragraph("Total due");
        assertThat(paragraph.getCTP().getPPr() == null || !paragraph.getCTP().getPPr().isSetInd()
                   || DocxTwips.of(paragraph.getCTP().getPPr().getInd().getLeft()) == 0).isTrue();
        assertThat(DocxExports.besidesTheExactLines(export.report())).isEmpty();
    }

    private static long twips(double points) {
        return Math.round(points * 20);
    }

    private static DocumentNode picture(double size) {
        return new ImageBuilder().name("Logo").source(DocumentImageData.fromBytes(png(40))).size(size, size).build();
    }

    private static byte[] png(int pixels) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Export inACell(DocumentNode content) throws Exception {
        return export(page -> page.addTable(table -> table.columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(content))));
    }

    private record Export(XWPFDocument document, DocxExportReport report, List<PlacedFragment> fragments) {

        /** The table's line of the given text, where the page set it. */
        PlacedFragment lineAt(String text) {
            return fragments.stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload lines
                                        && lines.lines().size() == 1 && lines.lines().get(0).text().equals(text))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no line reading " + text));
        }

        /** The grid of the row's table, nested in the cell, in twips. */
        List<Long> rowGrid() {
            XWPFTable outer = document.getTables().get(0);
            List<XWPFTable> nested = outer.getRow(0).getCell(0).getTables();
            if (nested.isEmpty() || nested.get(0).getCTTbl().getTblGrid() == null) {
                return List.of();
            }
            return nested.get(0).getCTTbl().getTblGrid().getGridColList().stream()
                    .map(CTTblGridCol::getW).map(DocxTwips::of).toList();
        }

        XWPFParagraph paragraph(String text) {
            return document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().equals(text))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no paragraph reading " + text));
        }

        XWPFParagraph pictureParagraph() {
            return document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(paragraph -> paragraph.getRuns().stream().anyMatch(run -> !run.getEmbeddedPictures().isEmpty()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no picture in the cell"));
        }
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        return export(content, false);
    }

    private static Export export(Consumer<PageFlowBuilder> content, boolean markdown) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20))
                .markdown(markdown).create()) {
            session.pageFlow(content::accept);
            byte[] bytes = session.export(new DocxSemanticBackend(report::set));
            return new Export(new XWPFDocument(new ByteArrayInputStream(bytes)), report.get(),
                    session.layoutGraph().fragments());
        }
    }
}
