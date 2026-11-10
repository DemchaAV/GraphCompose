package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A column sized to its content gives its text a point more in Word than on the page.
 *
 * <p>An auto column is exactly as wide as its widest unwrapped cell: the engine gives it no
 * slack, because on the page it needs none. An editor sets the text in its own substitute
 * for the face and keeps a border's width clear inside the cell, and either is enough to push
 * the widest cell onto a second line — measured on the probe corpus through LibreOffice, a
 * billing table's widest item wrapped and its row doubled once its cells were written at their
 * true size. A point per auto column keeps the page's line breaks.</p>
 *
 * <p>The point comes out of the column's cells' margins, on the side their text does not lean
 * on, so the column stays as wide as the page sets it: widened instead, it moved every rule right
 * of it a point right of the page's (measured in Word, {@code ClassicInvoice}'s line items 0.98 to
 * 1.05pt). What the margins do not hold — beside a composed cell, or padding too thin on the side a
 * cell gives from — widens the column, and the report names it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxEditorSlackTest {

    private static final long SLACK_TWIPS = Math.round(DocxSemanticBackend.EDITOR_COLUMN_SLACK_POINTS * 20);
    /** The engine's cell padding a side, in twips, where nothing states one. */
    private static final long PADDING_TWIPS = Math.round(DocxSemanticBackend.ENGINE_DEFAULT_CELL_PADDING_POINTS * 20);
    private static final String RIGHT_END = "right end";
    /** No rule, so a thin padding gives all it has. */
    private static final DocumentStroke UNRULED = DocumentStroke.of(DocumentColor.BLACK, 0);

    @Test
    void anAutoColumnsTextCellsGiveThePointOutOfTheirRightMargins() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Billing")
                .autoColumns(2)
                .row("Platform subscription", "1 440.00")));

        for (int column = 0; column < 2; column++) {
            assertThat(exported.gridTwips(column)).as("column %d: as wide as the page sets it", column)
                    .isEqualTo(exported.layoutTwips(column));
            assertThat(exported.sideMargins(0, column)).as("column %d: its text set left, the point off its right",
                            column)
                    .containsExactly(PADDING_TWIPS, PADDING_TWIPS - SLACK_TWIPS);
            assertThat(exported.textTwips(0, column)).as("column %d: its text the editor's point wider", column)
                    .isEqualTo(exported.layoutTwips(column) - 2 * PADDING_TWIPS + SLACK_TWIPS);
        }
        assertThat(exported.notes(RIGHT_END)).as("nothing stands right of the page's").isEmpty();
    }

    @Test
    void aCellSetRightGivesThePointOutOfItsLeftMarginAndACentredOneHalfFromEach() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Amounts")
                .autoColumns(2)
                .rowCells(DocumentTableCell.text("1 440.00").withStyle(DocumentTableStyle.builder()
                                .textAnchor(DocumentTableTextAnchor.CENTER_RIGHT).build()),
                        DocumentTableCell.text("Paid").withStyle(DocumentTableStyle.builder()
                                .textAnchor(DocumentTableTextAnchor.CENTER).build()))));

        assertThat(exported.sideMargins(0, 0)).as("set right: its left margin gives it")
                .containsExactly(PADDING_TWIPS - SLACK_TWIPS, PADDING_TWIPS);
        assertThat(exported.sideMargins(0, 1)).as("centred: half from each side")
                .containsExactly(PADDING_TWIPS - SLACK_TWIPS / 2, PADDING_TWIPS - SLACK_TWIPS / 2);
        assertThat(exported.gridTwips(0)).isEqualTo(exported.layoutTwips(0));
        assertThat(exported.gridTwips(1)).isEqualTo(exported.layoutTwips(1));
    }

    @Test
    void aCellSetRightGivesOnlyWhatItsLeftMarginHas() throws Exception {
        // A quarter of a point on its left and 4pt on its right: the right one, which its text leans
        // on, keeps all of it.
        Exported exported = export(page -> page.addTable(t -> t
                .name("Amounts")
                .autoColumns(1)
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(4, 4, 4, 0.25)).stroke(UNRULED)
                        .textAnchor(DocumentTableTextAnchor.CENTER_RIGHT).build())
                .row("1 440.00")));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.layoutTwips(0) + 15);
        assertThat(exported.sideMargins(0, 0)).containsExactly(0L, PADDING_TWIPS);
        assertThat(exported.notes(RIGHT_END)).hasSize(1);
    }

    @Test
    void aCentredCellWithHalfAPointASideHoldsThePointWhole() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Centred")
                .autoColumns(1)
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(4, 0.5, 4, 0.5)).stroke(UNRULED)
                        .textAnchor(DocumentTableTextAnchor.CENTER).build())
                .row("Paid")));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.layoutTwips(0));
        assertThat(exported.sideMargins(0, 0)).containsExactly(0L, 0L);
        assertThat(exported.notes(RIGHT_END)).isEmpty();
    }

    @Test
    void aRightToLeftCellSetAtItsStartGivesThePointOutOfItsLeftMargin() throws Exception {
        // The page sets a right-to-left cell that states no anchor against its right edge.
        Exported exported = export(page -> page.addTable(t -> t
                .name("Hebrew")
                .autoColumns(1)
                .defaultCellStyle(DocumentTableStyle.builder().direction(TextDirection.RTL).build())
                .row("שלום עולם")));

        assertThat(exported.sideMargins(0, 0)).containsExactly(PADDING_TWIPS - SLACK_TWIPS, PADDING_TWIPS);
    }

    @Test
    void aRightToLeftCellSetLeftGivesThePointOutOfItsRightMargin() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Hebrew")
                .autoColumns(1)
                .defaultCellStyle(DocumentTableStyle.builder().direction(TextDirection.RTL)
                        .textAnchor(DocumentTableTextAnchor.CENTER_LEFT).build())
                .row("שלום עולם")));

        assertThat(exported.sideMargins(0, 0)).containsExactly(PADDING_TWIPS, PADDING_TWIPS - SLACK_TWIPS);
    }

    @Test
    void aFixedColumnKeepsTheWidthTheDocumentStatedAndItsMargins() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Fixed")
                .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.auto())
                .row("Label", "Value")));

        assertThat(exported.gridTwips(0)).isEqualTo(Math.round(120 * 20.0));
        assertThat(exported.sideMargins(0, 0)).containsExactly(PADDING_TWIPS, PADDING_TWIPS);
        assertThat(exported.gridTwips(1)).isEqualTo(exported.layoutTwips(1));
        assertThat(exported.sideMargins(0, 1)).containsExactly(PADDING_TWIPS, PADDING_TWIPS - SLACK_TWIPS);
    }

    @Test
    void paddingThinnerThanThePointGivesWhatItHasAndTheRestWidensTheColumnNamed() throws Exception {
        // A quarter of a point on the right: the column is the other three quarters wider.
        Exported exported = export(page -> page.addTable(t -> t
                .name("Tight")
                .autoColumns(1)
                .defaultCellStyle(DocumentTableStyle.builder().padding(new DocumentInsets(4, 0.25, 4, 4))
                        .stroke(UNRULED).build())
                .row("Platform subscription")
                .row("Support")));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.layoutTwips(0) + 15);
        for (int row = 0; row < 2; row++) {
            assertThat(exported.sideMargins(row, 0)).as("row %d: all of its right margin given", row)
                    .containsExactly(80L, 0L);
        }
        assertThat(exported.notes(RIGHT_END)).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Tight");
            assertThat(note.detail()).isEqualTo("its columns sized to their content written 0.75pt wider than the "
                    + "page sets them, the room an editor setting their text in its own face may need, which their "
                    + "cells' margins do not hold: its right end, and what stands right of them, up to that much "
                    + "further right");
        });
    }

    @Test
    void aSideGivesOnlyWhatItHasPastTheInnerHalfOfItsRule() throws Exception {
        // A point on the right beside a 1pt rule, drawn eight eighths wide: half a point is the rule's,
        // half the slack's. Taken further, a line set wider would run onto the rule.
        Exported exported = export(page -> page.addTable(t -> t
                .name("Ruled")
                .autoColumns(1)
                .defaultCellStyle(DocumentTableStyle.builder().padding(new DocumentInsets(4, 1, 4, 4))
                        .stroke(DocumentStroke.of(DocumentColor.BLACK, 1)).build())
                .row("Platform subscription")));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.layoutTwips(0) + SLACK_TWIPS / 2);
        assertThat(exported.sideMargins(0, 0)).containsExactly(80L, 10L);
        assertThat(exported.notes(RIGHT_END)).singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("written 0.5pt wider"));
    }

    @Test
    void aCellSpanningRowsGivesThePointInEveryRowItCovers() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Merged")
                .autoColumns(2)
                .rowCells(DocumentTableCell.text("Tall").rowSpan(2), DocumentTableCell.text("Top"))
                .rowCells(DocumentTableCell.text("Low"))));

        assertThat(exported.sideMargins(0, 0)).containsExactly(PADDING_TWIPS, PADDING_TWIPS - SLACK_TWIPS);
        assertThat(exported.sideMargins(1, 0)).as("the merge's covered position, as its cell")
                .containsExactly(PADDING_TWIPS, PADDING_TWIPS - SLACK_TWIPS);
        assertThat(exported.gridTwips(0)).isEqualTo(exported.layoutTwips(0));
    }

    @Test
    void aHeaderPaddedThinnerThanTheBodyHoldsTheColumnToWhatItGives() throws Exception {
        // Half a point on the header's right: the body's cells give no more, their column the other half wider.
        Exported exported = export(page -> page.addTable(t -> t
                .name("Headed")
                .autoColumns(1)
                .headerRow("Description")
                .headerStyle(DocumentTableStyle.builder().padding(new DocumentInsets(4, 0.5, 4, 4))
                        .stroke(UNRULED).build())
                .row("Platform subscription")));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.layoutTwips(0) + SLACK_TWIPS / 2);
        assertThat(exported.sideMargins(0, 0)).as("the header").containsExactly(80L, 0L);
        assertThat(exported.sideMargins(1, 0)).as("the body").containsExactly(80L, 80L - SLACK_TWIPS / 2);
        assertThat(exported.notes(RIGHT_END)).singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("written 0.5pt wider"));
    }

    @Test
    void anIndentedTableIsIndentedByWhatItsFirstCellKeepsOfItsLeftMargin() throws Exception {
        // Word and LibreOffice measure a table's indent to its first cell's text: indented by the
        // margin as written, a set-right first column's table stood its left rule where the page draws
        // it, as a set-left one's (measured: in a section padded 20pt, Word 0.06pt right, LibreOffice
        // on the page, either way).
        Exported exported = export(page -> page.addSection("Box", box -> box.padding(DocumentInsets.of(20))
                .addTable(t -> t
                        .name("Amounts")
                        .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(80))
                        .defaultCellStyle(DocumentTableStyle.builder()
                                .padding(DocumentInsets.of(6))
                                .textAnchor(DocumentTableTextAnchor.CENTER_RIGHT).build())
                        .row("1 440.00", "Label"))));

        assertThat(exported.sideMargins(0, 0)).containsExactly(120L - SLACK_TWIPS, 120L);
        assertThat(DocxTwips.of(exported.table().getCTTbl().getTblPr().getTblInd().getW()))
                .as("the section's padding and the margin the first cell keeps")
                .isEqualTo(400L + 120L - SLACK_TWIPS);
    }

    @Test
    void aComposedCellKeepsItsMarginsAndItsColumnTakesThePointNamed() throws Exception {
        // A paragraph in a composed cell may run past its text into the margin (measureAtWordsSize):
        // with the margin taken, Word would cut it off at the cell's edge.
        Exported exported = export(page -> page.addTable(t -> t
                .name("Heading")
                .autoColumns(2)
                .row("Platform subscription", "1 440.00")
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Skills").build()),
                        DocumentTableCell.text("Paid"))));

        assertThat(exported.gridTwips(0)).as("the composed cell's column a point wider")
                .isEqualTo(exported.layoutTwips(0) + SLACK_TWIPS);
        assertThat(exported.sideMargins(0, 0)).as("so the text beside it keeps its margins")
                .containsExactly(PADDING_TWIPS, PADDING_TWIPS);
        assertThat(exported.sideMargins(1, 0)).containsExactly(PADDING_TWIPS, PADDING_TWIPS);
        assertThat(exported.gridTwips(1)).as("the other column as the page sets it")
                .isEqualTo(exported.layoutTwips(1));
        assertThat(exported.sideMargins(1, 1)).containsExactly(PADDING_TWIPS, PADDING_TWIPS - SLACK_TWIPS);
        assertThat(exported.notes(RIGHT_END)).singleElement()
                .satisfies(note -> assertThat(note.detail()).startsWith("its columns sized to their content "
                                                                       + "written 1pt wider than the page sets them"));
    }

    @Test
    void aCellSpanningTwoAutoColumnsSharesItsMarginBetweenThem() throws Exception {
        // A point on its right for two columns' points: half of each column's in the margins, half
        // its width.
        Exported exported = export(page -> page.addTable(t -> t
                .name("Spanned")
                .autoColumns(2)
                .row("Platform", "1 440.00")
                .rowCells(DocumentTableCell.text("Total").colSpan(2).withStyle(DocumentTableStyle.builder()
                        .padding(new DocumentInsets(4, 1, 4, 4)).stroke(UNRULED).build()))));

        for (int column = 0; column < 2; column++) {
            assertThat(exported.gridTwips(column)).as("column %d", column)
                    .isEqualTo(exported.layoutTwips(column) + SLACK_TWIPS / 2);
            assertThat(exported.sideMargins(0, column)).as("column %d: half its point off the right", column)
                    .containsExactly(PADDING_TWIPS, PADDING_TWIPS - SLACK_TWIPS / 2);
        }
        assertThat(exported.sideMargins(1, 0)).as("the span: its whole right margin").containsExactly(80L, 0L);
        assertThat(exported.notes(RIGHT_END)).singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("written 1pt wider"));
    }

    @Test
    void aTableWithAStatedWidthKeepsItExactly() throws Exception {
        Exported exported = export(page -> page.addTable(t -> t
                .name("Stated")
                .width(200)
                .autoColumns(2)
                .row("A", "B")));

        assertThat(exported.gridTwips(0) + exported.gridTwips(1)).isEqualTo(Math.round(200 * 20.0));
        assertThat(exported.sideMargins(0, 0)).as("no slack to give").containsExactly(PADDING_TWIPS, PADDING_TWIPS);
        assertThat(exported.notes(RIGHT_END)).isEmpty();
    }

    @Test
    void aRowsAutoColumnTakesItsPointOutOfTheGapAfterItAndTheLastOneFromTheWeightColumn() throws Exception {
        // A table of contents' entry: label, leader, page number. Measured through LibreOffice,
        // with the columns as placed the label wrapped mid-word ("Intr" / "o"). The label takes
        // its point from the 6pt gap after it, and the leader starts where the page starts it;
        // the page number, at the row's end, has no gap after it and takes its point from the
        // leader, which is named.
        ExportedRow exported = exportRow(row -> row
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1), DocumentRowColumn.auto())
                .addParagraph("Intro").addParagraph("…").addParagraph("12"));

        assertThat(exported.gridTwips(0)).as("the label's column where the page sets it").isEqualTo(exported.placedTwips(0));
        assertThat(exported.rightMarginTwips(0)).as("the gap, less the point").isEqualTo(6 * 20 - SLACK_TWIPS);
        assertThat(exported.gridTwips(1)).isEqualTo(exported.placedTwips(1) - SLACK_TWIPS);
        assertThat(exported.gridTwips(2)).isEqualTo(exported.placedTwips(2) + SLACK_TWIPS);
        assertThat(exported.gridTwips(0) + exported.gridTwips(1) + exported.gridTwips(2))
                .as("the row keeps the width the layout placed it at")
                .isEqualTo(exported.placedTwips(0) + exported.placedTwips(1) + exported.placedTwips(2));
        assertThat(exported.notes(RIGHT_END)).singleElement().satisfies(note -> assertThat(note.detail())
                .startsWith("its columns sized to their content written 1pt wider than the page sets them")
                .contains("what stands at the edges between them moves up to that much off where the page sets it"));
    }

    @Test
    void aGapThinnerThanThePointGivesWhatItHasAndTheWeightColumnTheRest() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .spacing(0.4)
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph("Experience").addParagraph("…"));

        assertThat(exported.rightMarginTwips(0)).as("the whole gap given").isZero();
        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0) + SLACK_TWIPS - 8);
        assertThat(exported.notes(RIGHT_END)).singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("written 0.6pt wider"));
    }

    @Test
    void anAutoColumnWhoseNeighbourStandsInByItsMarginNeedsNoPoint() throws Exception {
        // A heading and a rule 9pt in from it, as CharcoalGold draws them: the label's column runs
        // to where the rule starts, past its margin, and already holds the room.
        ExportedRow exported = exportRow(row -> row
                .spacing(0)
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph("Experience")
                .addLine(line -> line.fill().thickness(0.6).margin(new DocumentInsets(0, 0, 0, 9))));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0));
        assertThat(exported.gridTwips(1)).isEqualTo(exported.placedTwips(1));
        assertThat(exported.notes(RIGHT_END)).isEmpty();
    }

    @Test
    void anAutoColumnWithNoGapTakesItsPointFromTheWeightColumnNamed() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .spacing(0)
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph("Experience").addParagraph("…"));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0) + SLACK_TWIPS);
        assertThat(exported.gridTwips(1)).isEqualTo(exported.placedTwips(1) - SLACK_TWIPS);
        assertThat(exported.notes(RIGHT_END)).hasSize(1);
    }

    @Test
    void theRowsPaddingAfterItsLastColumnIsNotAGap() throws Exception {
        // A painted row's panel takes the padding out of its columns: the point stays out of it.
        ExportedRow exported = exportRow(row -> row
                .padding(new DocumentInsets(0, 4, 0, 0))
                .columns(DocumentRowColumn.weight(1), DocumentRowColumn.auto())
                .addParagraph("…").addParagraph("12"));

        assertThat(exported.rightMarginTwips(1)).as("the row's padding, whole").isEqualTo(4 * 20L);
        assertThat(exported.gridTwips(1)).isEqualTo(exported.placedTwips(1) + SLACK_TWIPS);
        assertThat(exported.notes(RIGHT_END)).hasSize(1);
    }

    @Test
    void aChildThatIsNotAParagraphTakesNoneOfThePointFromTheGap() throws Exception {
        // What a section holds may lean either way.
        ExportedRow exported = exportRow(row -> row
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addSection(section -> section.addParagraph("Experience")).addParagraph("…"));

        assertThat(exported.rightMarginTwips(0)).as("the gap, whole").isEqualTo(6 * 20L);
        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0) + SLACK_TWIPS);
    }

    @Test
    void anAutoParagraphsOwnRightSideGivesTheRoomItsParagraphDoesNotIndent() throws Exception {
        // In a cell, a paragraph set left writes its right side 2pt short as its indent: a 3pt
        // margin leaves it 2pt past its text, and it needs none of the point; a 0.4pt one leaves
        // it 0.4pt, and the rest comes out of the weight column.
        java.util.function.DoubleFunction<ExportedRow> margined = margin -> {
            try {
                return exportRow(row -> row
                        .spacing(0)
                        .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Experience").margin(new DocumentInsets(0, margin, 0, 0)))
                        .addParagraph("…"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        ExportedRow wide = margined.apply(3);
        assertThat(wide.gridTwips(0)).isEqualTo(wide.placedTwips(0));
        assertThat(wide.notes(RIGHT_END)).isEmpty();

        ExportedRow thin = margined.apply(0.4);
        assertThat(thin.gridTwips(0)).isEqualTo(thin.placedTwips(0) + SLACK_TWIPS - 8);
        assertThat(thin.notes(RIGHT_END)).singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("written 0.6pt wider"));
    }

    @Test
    void anAutoParagraphsSidePulledOutIsNoRoom() throws Exception {
        // A right margin below zero pulls its side 2pt into the 2pt its neighbour stands in by: its
        // cell holds that side at its text, so the column has no room past the page's line.
        ExportedRow exported = exportRow(row -> row
                .spacing(0)
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph(p -> p.text("Experience").margin(new DocumentInsets(0, -2, 0, 0)))
                .addParagraph(p -> p.text("…").margin(new DocumentInsets(0, 0, 0, 2))));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0) + SLACK_TWIPS);
        assertThat(exported.notes(RIGHT_END)).hasSize(1);
    }

    @Test
    void aCentredAutoParagraphTakesNoneOfThePointFromTheGap() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph(p -> p.text("Experience").align(TextAlign.CENTER)).addParagraph("…"));

        assertThat(exported.rightMarginTwips(0)).as("the gap, whole").isEqualTo(6 * 20L);
    }

    @Test
    void lessThanATenthOfAPointOffIsNotNamed() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .spacing(0.95)
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph("Experience").addParagraph("…"));

        assertThat(exported.rightMarginTwips(0)).isZero();
        assertThat(exported.notes(RIGHT_END)).isEmpty();
    }

    @Test
    void aWeightColumnHoldingASpacerGivesThePointUnnamed() throws Exception {
        // A spacer stands nowhere a reader sees.
        ExportedRow exported = exportRow(row -> row
                .spacing(0)
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph("Experience").addSpacer(0));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0) + SLACK_TWIPS);
        assertThat(exported.notes(RIGHT_END)).isEmpty();
    }

    @Test
    void aRowWithNoAutoColumnIsWrittenAsPlaced() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .columns(DocumentRowColumn.fixed(80), DocumentRowColumn.weight(1))
                .addParagraph("Label").addParagraph("Value"));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0));
        assertThat(exported.gridTwips(1)).isEqualTo(exported.placedTwips(1));
        assertThat(DocxTwips.of(exported.table().getCTTbl().getTblPr().getTblW().getW()))
                .as("columns that fill the row keep the table at its width")
                .isEqualTo(exported.placedTwips(0) + exported.placedTwips(1));
    }

    @Test
    void aRowWithNoWeightColumnHasNoPointToGiveItsFixedColumnAtItsWidth() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .columns(DocumentRowColumn.auto(), DocumentRowColumn.fixed(80))
                .addParagraph("Label").addParagraph("Value"));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0));
        assertThat(exported.gridTwips(1)).as("the fixed column at its own width, the rest of the row empty")
                .isEqualTo(80 * 20L);
    }

    @Test
    void aFixedColumnBesideThemKeepsItsSize() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .spacing(0)
                .columns(DocumentRowColumn.fixed(60), DocumentRowColumn.auto(), DocumentRowColumn.weight(1))
                .addParagraph("Invoice").addParagraph("No. 2041").addParagraph("…"));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0));
        assertThat(exported.gridTwips(1)).isEqualTo(exported.placedTwips(1) + SLACK_TWIPS);
        assertThat(exported.gridTwips(2)).isEqualTo(exported.placedTwips(2) - SLACK_TWIPS);
    }

    @Test
    void twoWeightColumnsGiveThePointInProportionToTheirTextBoxes() throws Exception {
        // A wide gap, so a split by column width — each weight column carries the gap after it as
        // its right margin — lands whole twips away from one by text box. At the row's end, the
        // number has no gap after it to take its point from.
        ExportedRow exported = exportRow(row -> row
                .spacing(60)
                .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(3), DocumentRowColumn.auto())
                .addParagraph("a").addParagraph("b").addParagraph("12"));

        double first = exported.placed()[0] - 60;
        double second = exported.placed()[1] - 60;
        double point = DocxSemanticBackend.EDITOR_COLUMN_SLACK_POINTS;
        assertThat(exported.gridTwips(0))
                .isEqualTo(Math.round((exported.placed()[0] - point * first / (first + second)) * 20.0));
        assertThat(exported.gridTwips(1))
                .isEqualTo(Math.round((exported.placed()[1] - point * second / (first + second)) * 20.0));
    }

    @Test
    void aRowOfWeightsWithNoStatedColumnsIsWrittenAsPlaced() throws Exception {
        ExportedRow exported = exportRow(row -> row
                .weights(1, 2)
                .addParagraph("Label").addParagraph("Value"));

        assertThat(exported.gridTwips(0)).isEqualTo(exported.placedTwips(0));
        assertThat(exported.gridTwips(1)).isEqualTo(exported.placedTwips(1));
    }

    /** A row exported through a session, with the column spans the layout placed it at and the report. */
    private record ExportedRow(XWPFTable table, double[] placed, DocxExportReport report) {

        long gridTwips(int column) {
            return DocxTwips.of(table.getCTTbl().getTblGrid().getGridColArray(column).getW());
        }

        long placedTwips(int column) {
            return Math.round(placed[column] * 20.0);
        }

        /** A column's cell's right margin as written, in twips. */
        long rightMarginTwips(int column) {
            return DocxTwips.of(table.getRow(0).getCell(column).getCTTc().getTcPr().getTcMar().getRight().getW());
        }

        List<DocxExportReport.Note> notes(String subject) {
            return report.bySubject().getOrDefault(subject, List.of());
        }
    }

    private static ExportedRow exportRow(Consumer<RowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addRow(row -> content.accept(row.name("Entry").spacing(6))));
            List<PlacedNode> nodes = session.layoutGraph().nodes();
            PlacedNode row = nodes.stream()
                    .filter(node -> "Entry".equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();
            List<PlacedNode> children = nodes.stream()
                    .filter(node -> row.path().equals(node.parentPath()))
                    .sorted(java.util.Comparator.comparingInt(PlacedNode::childIndex))
                    .toList();
            // A column runs from its child's start to the next child's; the first from the
            // row's edge, the last to it — the gap and padding ride inside the column.
            double[] placed = new double[children.size()];
            for (int index = 0; index < placed.length; index++) {
                double start = index == 0 ? row.placementX() : children.get(index).placementX();
                double end = index == placed.length - 1
                        ? row.placementX() + row.placementWidth()
                        : children.get(index + 1).placementX();
                placed[index] = end - start;
            }
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
            assertThat(report.get().bySubject()).as("laid out").doesNotContainKey("measured geometry");
            return new ExportedRow(document.getTables().get(0), placed, report.get());
        }
    }

    /** A table exported through a session, with the layout the session sized it with and the report. */
    private record Exported(XWPFTable table, List<TableResolvedCell> firstRow, DocxExportReport report) {

        long gridTwips(int column) {
            CTTblGrid grid = table.getCTTbl().getTblGrid();
            return DocxTwips.of(grid.getGridColArray(column).getW());
        }

        long layoutTwips(int column) {
            return Math.round(firstRow.get(column).width() * 20.0);
        }

        /** A cell's left and right margins as written, in twips. */
        long[] sideMargins(int row, int cell) {
            CTTcMar margins = table.getRow(row).getCell(cell).getCTTc().getTcPr().getTcMar();
            return new long[]{DocxTwips.of(margins.getLeft().getW()), DocxTwips.of(margins.getRight().getW())};
        }

        /** The room a cell of one column leaves its text in Word: its column less its side margins. */
        long textTwips(int row, int column) {
            long[] margins = sideMargins(row, column);
            return gridTwips(column) - margins[0] - margins[1];
        }

        List<DocxExportReport.Note> notes(String subject) {
            return report.bySubject().getOrDefault(subject, List.of());
        }
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            List<TableResolvedCell> firstRow = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                    .map(fragment -> ((TableRowFragmentPayload) fragment.payload()).cells())
                    .findFirst()
                    .orElseThrow();
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
            assertThat(report.get().bySubject()).as("laid out").doesNotContainKey("measured geometry");
            return new Exported(document.getTables().get(0), firstRow, report.get());
        }
    }
}
