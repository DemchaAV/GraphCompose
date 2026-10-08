package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a panel leaves below and above itself in Word, and what takes it.
 *
 * <p>Word sets a panel's content its whole border and then its margin inside the row, where the
 * page strokes the border on the panel's edge and sets the content its padding inside it: half a
 * border reaches past the panel, and its inner half too where the padding is thinner than that.
 * The space round the panel takes it first, then the panel's padding, the border then drawn that
 * much inside the panel's edge; what neither takes stands the block beyond it lower, and is named.
 * So is a panel's last line hanging past its content, which its padding takes first, and the
 * paragraph Word keeps between two tables. Measured in Word 16 and LibreOffice alike.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPanelTailTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentStroke RULE = DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 2);
    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
            + "<circle cx='12' cy='12' r='10' fill='#1A5694'/></svg>");
    private static final DocumentTextStyle SMALL = DocumentTextStyle.DEFAULT.withSize(7);

    @Test
    void aPaddedCardsBorderBelowComesOutOfItsPaddingWhereNoSpaceTakesIt() throws Exception {
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                        .borders(DocumentBorders.bottom(RULE)).padding(DocumentInsets.of(4)).addParagraph("Inside"))
                .addParagraph("After"));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
        assertThat(bottomMargin(panel(exported))).as("its padding, less the whole border").isEqualTo(2 * 20);
        assertThat(before(paragraph(exported, "After"))).isZero();
    }

    @Test
    void theSpaceBelowACardTakesItsBorderBeforeItsPaddingDoes() throws Exception {
        // There the border stands where the page strokes it, half outside the card.
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                        .borders(DocumentBorders.bottom(RULE)).padding(DocumentInsets.of(4)).addParagraph("Inside"))
                .addParagraph(p -> p.text("After").margin(new DocumentInsets(10, 0, 0, 0))));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
        assertThat(bottomMargin(panel(exported))).as("its padding, less half the border").isEqualTo(3 * 20);
        assertThat(before(paragraph(exported, "After"))).as("the gap, less half the border").isEqualTo(9 * 20);
    }

    @Test
    void aCardWithNoPaddingNamesTheBorderNothingTakes() throws Exception {
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                        .borders(DocumentBorders.bottom(RULE)).addParagraph("Inside"))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands 2pt lower than the page sets it, and what follows with it: the "
                                                + "space the page leaves above it does not hold the border Word draws past "
                                                + "the box of the panel above it (2pt)");
        });
    }

    @Test
    void aCardWithNoPaddingNamesItsTopBorderNothingTakes() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before").addSection("Card", card -> card
                .fillColor(SURFACE).borders(DocumentBorders.top(RULE)).addParagraph("Inside")));

        assertThat(exported.report().bySubject().get("SectionNode")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.path()).contains("Card");
            assertThat(note.detail()).isEqualTo("its content stands 2pt lower than the page sets it, and what follows "
                                                + "up to as much: Word sets it below the whole of its top border, and "
                                                + "neither the space above it nor its padding takes what of the border "
                                                + "reaches past the panel's box");
        });
    }

    @Test
    void twoTouchingCardsNameBothBordersAndTheParagraphBetweenThem() throws Exception {
        // The first one's border below and the paragraph Word keeps between two tables stand the
        // second lower, named together on it; its own border above stands its content lower.
        Exported exported = export(page -> page
                .addSection("First", card -> card.fillColor(SURFACE).borders(DocumentBorders.bottom(RULE))
                        .addParagraph("One"))
                .addSection("Second", card -> card.fillColor(SURFACE).borders(DocumentBorders.top(RULE))
                        .addParagraph("Two")));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Second");
            assertThat(note.detail()).isEqualTo("it stands 2.1pt lower than the page sets it, and what follows with it: "
                                                + "the space the page leaves above it does not hold the border Word draws "
                                                + "past the box of the panel above it (2pt) and the paragraph Word keeps "
                                                + "between two tables (0.1pt)");
        });
        assertThat(exported.report().bySubject().get("SectionNode")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Second");
            assertThat(note.detail()).isEqualTo("its content stands 2pt lower than the page sets it, and what follows "
                                                + "up to as much: Word sets it below the whole of its top border, and "
                                                + "neither the space above it nor its padding takes what of the border "
                                                + "reaches past the panel's box");
        });
    }

    @Test
    void aCardsBorderEndingARowsCellComesOutOfItsPaddingWhereTheRowHoldsNoMore() throws Exception {
        // The card is as tall as the row, which leaves no room under it: its padding takes its
        // border, and the row is as tall as the page's.
        Exported exported = export(page -> page.addRow(row -> row
                        .addSection("Card", card -> card.fillColor(SURFACE).borders(DocumentBorders.bottom(RULE))
                                .padding(DocumentInsets.of(4)).addParagraph("Inside"))
                        .addParagraph("Beside"))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(nestedPanel(exported))).as("its padding, less the whole border").isEqualTo(2 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aCardsBorderEndingARowsCellItsPaddingCannotHoldIsNamedBelowTheRow() throws Exception {
        Exported exported = export(page -> page.addRow(row -> row
                        .addSection("Card", card -> card.fillColor(SURFACE).borders(DocumentBorders.bottom(RULE))
                                .addParagraph("Inside"))
                        .addParagraph("Beside"))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands 2pt lower than the page sets it, and what follows with it: the "
                                                + "space the page leaves above it does not hold the border Word draws past "
                                                + "the box of the panel above it (2pt)");
        });
    }

    @Test
    void aRowsCellReachesPastTheRowByItsHangAndItsCardsBorderTogether() throws Exception {
        // The line hanging below the card's text and the card's border below it reach one past
        // the other: the row stands taller by both.
        Exported exported = export(page -> page.addRow(row -> row
                        .addSection("Card", card -> {
                            card.fillColor(SURFACE).borders(DocumentBorders.bottom(RULE)).padding(DocumentInsets.of(1));
                            contact(card);
                        })
                        .addParagraph("Beside"))
                .addParagraph(p -> p.name("After").text("After")));
        long down = reachBelow(exported, "Iconed");

        assertThat(down).as("the line reaches below the page's").isPositive();
        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands " + points(down + 20) + "pt lower than the page sets it, and "
                                                + "what follows with it: the space the page leaves above it does not hold "
                                                + "the border Word draws past the box of the panel above it (1pt) and the "
                                                + "line hanging below the block above it (" + points(down) + "pt)");
        });
    }

    @Test
    void aCardAloneInATablesColumnTakesItsBorderOutOfItsPadding() throws Exception {
        Exported exported = export(page -> page.addTable(table -> table.columns(DocumentTableColumn.fixed(120))
                        .rowCells(DocumentTableCell.node(card(4))))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(nestedPanel(exported))).as("its padding, less the whole border").isEqualTo(2 * 20);
        assertThat(exported.report().notes()).noneSatisfy(note -> assertThat(note.detail()).contains("border"));
    }

    @Test
    void aCardAloneInATablesColumnNamesTheBorderItsPaddingCannotHold() throws Exception {
        // Nothing beside it holds the border: the row, and what follows, stand that much lower.
        Exported exported = export(page -> page.addTable(table -> table.columns(DocumentTableColumn.fixed(120))
                        .rowCells(DocumentTableCell.node(card(0))))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
        assertThat(exported.report().bySubject().get("SectionNode")).singleElement()
                .extracting(DocxExportReport.Note::detail).isEqualTo("in a table's cell, Word draws the border below "
                        + "the panel ending it 2pt past that panel's box, and the row and what follows stand as much lower");
    }

    @Test
    void aCardBesideAnotherCellInATableTakesItsBorderOutOfItsPadding() throws Exception {
        Exported exported = export(page -> page.addTable(table -> table
                        .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(120))
                        .rowCells(DocumentTableCell.node(card(4)), DocumentTableCell.node(tall())))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(nestedPanel(exported))).as("its padding, less the whole border").isEqualTo(2 * 20);
        assertThat(exported.report().notes()).noneSatisfy(note -> assertThat(note.detail()).contains("border"));
    }

    @Test
    void aCardBesideATallerCellInATableIsNamedUpToItsBorder() throws Exception {
        // The layout does not say how tall a composed cell's content is against its row: the cell
        // beside the card may hold what its padding cannot, and the space under the table does not
        // take it.
        Exported exported = export(page -> page.addTable(table -> table
                        .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(120))
                        .rowCells(DocumentTableCell.node(card(0)), DocumentTableCell.node(tall())))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(before(paragraph(exported, "After"))).as("the space under the table, whole").isZero();
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
        assertThat(exported.report().bySubject().get("SectionNode")).singleElement()
                .extracting(DocxExportReport.Note::detail).isEqualTo("in a table's cell, Word draws the border below "
                        + "the panel ending it 2pt past that panel's box, and the row and what follows stand up to as much "
                        + "lower where no cell beside it holds the border");
    }

    /** A cell of four lines, taller than a card of one. */
    private static DocumentNode tall() {
        return new SectionBuilder().name("Tall").addParagraph("L1").addParagraph("L2").addParagraph("L3")
                .addParagraph("L4").build();
    }

    @Test
    void aCardEndingAColumnOfLayersBesideAnotherIsNamedUpToItsBorder() throws Exception {
        // Side-by-side layers are the cells of one row, which the layout does not measure the
        // columns' content against: what of the card's border its padding cannot hold is named,
        // and the space under the stack takes nothing for it.
        Exported exported = export(page -> page.addLayerStack(stack -> stack.name("Columns")
                        .layer(column("Sidebar", 0, 240, side -> side.addSection("Card", card -> card.fillColor(SURFACE)
                                .borders(DocumentBorders.bottom(RULE)).addParagraph("Inside"))),
                                com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                        .layer(column("Main", 120, 0, main -> main.addParagraph("Line").addParagraph("Line")
                                .addParagraph("Line").addParagraph("Line")), com.demcha.compose.document.node.LayerAlign.TOP_LEFT))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
        assertThat(exported.report().notes()).anySatisfy(note -> assertThat(note.detail()).isEqualTo("in a table's cell, "
                + "Word draws the border below the panel ending it 2pt past that panel's box, and the row and what "
                + "follows stand up to as much lower where no cell beside it holds the border"));
    }

    /** A layer of a stack, held in from the stack's sides as DocxLayerColumnsTest's columns are. */
    private static DocumentNode column(String name, double insetLeft, double insetRight, Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft));
        layer.addSection(name + "Content", section -> content.accept(section.spacing(0)));
        return layer.build();
    }

    /** A card ruled below by {@link #RULE}, padded {@code padding} on every side. */
    private static DocumentNode card(double padding) {
        return new SectionBuilder().name("Card").fillColor(SURFACE).borders(DocumentBorders.bottom(RULE))
                .padding(DocumentInsets.of(padding)).addParagraph("Inside").build();
    }

    @Test
    void aPaddedCardsTopBorderComesOutOfItsPaddingWhereNoSpaceAboveTakesIt() throws Exception {
        Exported exported = export(page -> page.addParagraph("Before").addSection("Card", card -> card
                .fillColor(SURFACE).borders(DocumentBorders.top(RULE)).padding(DocumentInsets.of(4)).addParagraph("Inside")));

        assertThat(exported.report().bySubject()).doesNotContainKey("SectionNode");
        assertThat(topMargin(panel(exported))).as("its padding, less the whole border").isEqualTo(2 * 20);
    }

    @Test
    void anOutlinedCardHoldsTheHeightOfItsContent() throws Exception {
        // The editors read a panel's written height as its content's, its margins and both its
        // borders round it: the page's height, less its padding.
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE).stroke(RULE)
                .padding(DocumentInsets.of(4)).addParagraph("Inside")).addParagraph("After"));

        assertThat(panel(exported).getTableRow().getHeight()).as("the page's height, less its padding")
                .isEqualTo(Math.round((exported.heights().get("Card") - 8) * 20));
        assertThat(exported.report().bySubject()).doesNotContainKey("space above").doesNotContainKey("SectionNode");
    }

    @Test
    void aShapesBordersStayInsideItsOutline() throws Exception {
        // Its layers are centred in it, and its outline holds its height whole: nothing of its
        // borders reaches past it, and nothing is named for them.
        var chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Chip").roundedRect(90, 17.5, 4)
                .stroke(DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 1.1))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00").build()).build();
        Exported exported = export(page -> page.addTable(table -> table
                        .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                        .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(chip)))
                .addParagraph("After"));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
        assertThat(exported.report().notes()).noneSatisfy(note -> assertThat(note.detail()).contains("stands"));
    }

    @Test
    void aPaddedShapeHoldsItsOutlineLessBothBordersAndItsMargins() throws Exception {
        // The page draws the borders and the padding inside the outline: the content held is
        // what is left of it, both borders off, as the editors add both round the row's height.
        var chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Chip").roundedRect(90, 17.5, 4)
                .padding(DocumentInsets.of(2)).stroke(DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 1.1))
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("09:00").build()).build();
        Exported exported = export(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(chip))));
        XWPFTableCell cell = exported.document().getTables().get(0).getRow(0).getCell(1).getTables().get(0)
                .getRow(0).getCell(0);

        assertThat(topMargin(cell)).as("its padding, less half its border").isEqualTo(29);
        assertThat(bottomMargin(cell)).isEqualTo(29);
        // The heavier border at the width Word is given, nine eighths of a point; the lighter one
        // at its own.
        assertThat(cell.getTableRow().getHeight()).as("17.5pt, less both margins and both borders")
                .isEqualTo(Math.round((17.5 - 1.1) * 20) - 2 * 29 - Math.round(9 / 8.0 * 20));
    }

    @Test
    void aPanelsHangingLastLineComesOutOfItsPaddingBelow() throws Exception {
        Exported exported = export(page -> page.spacing(10).addSection("Card", card -> {
            card.fillColor(SURFACE).padding(DocumentInsets.bottom(5));
            contact(card);
        }).addParagraph("After"));
        long down = reachBelow(exported, "Iconed");

        assertThat(down).as("the line reaches below the page's").isPositive();
        assertThat(bottomMargin(panel(exported))).as("the padding less the reach").isEqualTo(5 * 20 - down);
        assertThat(before(paragraph(exported, "After"))).as("the whole gap").isEqualTo(10 * 20);
    }

    @Test
    void aPanelsHangingLastLineWithNoPaddingTakesTheGapUnderIt() throws Exception {
        Exported exported = export(page -> page.spacing(10).addSection("Card", card -> {
            card.fillColor(SURFACE);
            contact(card);
        }).addParagraph("After"));
        long down = reachBelow(exported, "Iconed");

        assertThat(down).as("the line reaches below the page's").isPositive();
        assertThat(before(paragraph(exported, "After"))).as("the gap, less the reach").isEqualTo(10 * 20 - down);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aPanelsHangNothingTakesIsNamed() throws Exception {
        Exported exported = export(page -> page.addSection("Card", card -> {
            card.fillColor(SURFACE);
            contact(card);
        }).addParagraph(p -> p.name("After").text("After")));
        String reach = points(reachBelow(exported, "Iconed"));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands " + reach + "pt lower than the page sets it, and what follows "
                                                + "with it: the space the page leaves above it does not hold the line "
                                                + "hanging below the block above it (" + reach + "pt)");
        });
    }

    @Test
    void aShapesHangingLineIsHeldInItsOutline() throws Exception {
        // Its layers are centred in the outline's height, which holds the line: its padding is
        // left as it is, and nothing is named.
        var chip = new com.demcha.compose.document.dsl.ShapeContainerBuilder().name("Chip").roundedRect(90, 30, 4)
                .padding(new DocumentInsets(0, 0, 6, 0)).fillColor(SURFACE)
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().textStyle(SMALL).inlineText("GitHub  ", SMALL)
                        .inlineSvgIcon(ICON, 10.5, InlineImageAlignment.CENTER, -1.35, null).build()).build();
        Exported exported = export(page -> page.addTable(table -> table
                        .columns(DocumentTableColumn.auto(), DocumentTableColumn.fixed(100))
                        .rowCells(DocumentTableCell.text("Mon"), DocumentTableCell.node(chip)))
                .addParagraph("After"));
        XWPFTableCell cell = exported.document().getTables().get(0).getRow(0).getCell(1).getTables().get(0)
                .getRow(0).getCell(0);

        assertThat(bottomMargin(cell)).isEqualTo(6 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aCardsBorderEndingAPanelComesOutOfThePanelsPadding() throws Exception {
        // The inner card's border reaches past its box into the outer panel's padding, which
        // takes it: the outer panel stays the page's height.
        Exported exported = export(page -> page.addSection("Outer", outer -> outer.fillColor(SURFACE)
                        .padding(DocumentInsets.of(6)).addParagraph("Lead")
                        .addSection("Inner", inner -> inner.fillColor(DocumentColor.WHITE)
                                .borders(DocumentBorders.bottom(RULE)).addParagraph("Inside")))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(panel(exported))).as("its padding, less the inner border").isEqualTo(4 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aCardsBorderEndingAPanelItsPaddingCannotHoldIsNamed() throws Exception {
        Exported exported = export(page -> page.addSection("Outer", outer -> outer.fillColor(SURFACE)
                        .padding(DocumentInsets.of(1)).addParagraph("Lead")
                        .addSection("Inner", inner -> inner.fillColor(DocumentColor.WHITE)
                                .borders(DocumentBorders.bottom(RULE)).addParagraph("Inside")))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(panel(exported))).isZero();
        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands 1pt lower than the page sets it, and what follows with it: the "
                                                + "space the page leaves above it does not hold the border Word draws past "
                                                + "the box of the panel above it (1pt)");
        });
    }

    @Test
    void aCardsBorderBesideATallerCellReachesNoFurtherThanTheRow() throws Exception {
        // The row is as tall as the cell beside the card, whose room under the card holds its
        // border: what follows stands where the page sets it, and nothing is named.
        Exported exported = export(page -> page.addRow(row -> row
                        .addSection("Card", card -> card.fillColor(SURFACE).borders(DocumentBorders.bottom(RULE))
                                .addParagraph("Inside"))
                        .addSection("Tall", tall -> {
                            for (int line = 0; line < 4; line++) {
                                tall.addParagraph("Line");
                            }
                        }))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aMarginBelowZeroUnderAPanelPullsWhatFollowsUp() throws Exception {
        // As a paragraph's does: out of the space under the panel, which keeps its padding.
        Exported exported = export(page -> page.spacing(10).addSection("Card", card -> card.fillColor(SURFACE)
                        .padding(DocumentInsets.of(8)).margin(new DocumentInsets(0, 0, -4, 0)).addParagraph("Inside"))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(panel(exported))).isEqualTo(8 * 20);
        assertThat(before(paragraph(exported, "After"))).as("the gap, less the pull").isEqualTo(6 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aPullNoSpaceGivesIsNamed() throws Exception {
        // The page sets the paragraph over the panel; Word sets no block over another.
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                        .padding(DocumentInsets.of(8)).margin(new DocumentInsets(0, 0, -4, 0)).addParagraph("Inside"))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(panel(exported))).as("the panel keeps its padding").isEqualTo(8 * 20);
        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands 4pt lower than the page sets it, and what follows with it: the "
                                                + "page sets it 4pt over the block above it, and Word sets no block over "
                                                + "another");
        });
    }

    @Test
    void aPullATableBelowCannotGiveIsNamedWithItsTail() throws Exception {
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                        .padding(DocumentInsets.of(8)).margin(new DocumentInsets(0, 0, -4, 0)).addParagraph("Inside"))
                .addTable(this::first));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("First");
            assertThat(note.detail()).isEqualTo("it stands 4.1pt lower than the page sets it, and what follows with it: "
                                                + "the space the page leaves above it does not hold the paragraph Word "
                                                + "keeps between two tables (0.1pt); the page sets it 4pt over the block "
                                                + "above it, and Word sets no block over another");
        });
    }

    @Test
    void aPullASpacerCannotGiveIsNamedOnTheSpacer() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p.text("Pulling").margin(new DocumentInsets(0, 0, -6, 0)))
                .addSpacer(spacer -> spacer.name("Gap").size(0, 2))
                .addParagraph("After"));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Gap");
            assertThat(note.detail()).isEqualTo("it stands 4.1pt lower than the page sets it, and what follows with it: "
                                                + "the page sets it 4.1pt over the block above it, and Word sets no block "
                                                + "over another");
        });
    }

    @Test
    void aPullLeftOverABlockWithNoTopEdgeOfItsOwnIsNamed() throws Exception {
        // A list item takes nothing out of a top edge of its own: the pull is named all the same.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Pulling").margin(new DocumentInsets(0, 0, -4, 0)))
                .addList(list -> list.addItem("Only")));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("ListNode");
            assertThat(note.detail()).isEqualTo("it stands 4pt lower than the page sets it, and what follows with it: the "
                                                + "page sets it 4pt over the block above it, and Word sets no block over "
                                                + "another");
        });
    }

    @Test
    void aPaddedCardEndingAPanelTakesItsBorderOutOfItsOwnPaddingFirst() throws Exception {
        Exported exported = export(page -> page.addSection("Outer", outer -> outer.fillColor(SURFACE)
                        .padding(DocumentInsets.of(6)).addParagraph("Lead")
                        .addSection("Inner", inner -> inner.fillColor(DocumentColor.WHITE)
                                .borders(DocumentBorders.bottom(RULE)).padding(DocumentInsets.of(4)).addParagraph("Inside")))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(bottomMargin(nestedPanel(exported))).as("the inner card's padding, less the whole border")
                .isEqualTo(2 * 20);
        assertThat(bottomMargin(panel(exported))).as("the outer panel's padding, whole").isEqualTo(6 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void theSpaceAPullTakesIsNoRoomForACardsTopBorder() throws Exception {
        // The card's 6pt top margin is the pull's, and its padding holds its top border instead.
        Exported exported = export(page -> page.addParagraph(p -> p.text("Pulling").margin(new DocumentInsets(0, 0, -6, 0)))
                .addSection("Card", card -> card.fillColor(SURFACE).borders(DocumentBorders.top(RULE))
                        .margin(new DocumentInsets(6, 0, 0, 0)).padding(DocumentInsets.of(4)).addParagraph("Inside")));

        assertThat(topMargin(panel(exported))).as("its padding, less the whole border").isEqualTo(2 * 20);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above").doesNotContainKey("SectionNode");
    }

    @Test
    void aContainersEdgeAboveATableTakesTheTailOfTheBlockAbove() throws Exception {
        // The section's top padding is space above its table, as above a paragraph.
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                        .borders(DocumentBorders.bottom(RULE)).addParagraph("Inside"))
                .addSection("Plain", plain -> plain.padding(new DocumentInsets(10, 0, 0, 0)).addTable(this::first)));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void aTailLeftOverTheLastBlockIsNamed() throws Exception {
        // A list item takes no space above out of its own top edge: the border the space above
        // it does not hold is named all the same, at the document's end.
        Exported exported = export(page -> page.addSection("Card", card -> card.fillColor(SURFACE)
                        .borders(DocumentBorders.bottom(RULE)).addParagraph("Inside"))
                .addList(list -> list.addItem("Only")));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("ListNode");
            assertThat(note.detail()).isEqualTo("it stands 2pt lower than the page sets it, and what follows with it: the "
                                                + "space the page leaves above it does not hold the border Word draws past "
                                                + "the box of the panel above it (2pt)");
        });
    }

    @Test
    void aPanelTheLayoutMovesToANewPageLeavesTheTailAbove() throws Exception {
        // What the card above leaves below itself stays on its page, and the padded card on the
        // next one names nothing for it.
        Exported exported = export(page -> page.addSpacer(spacer -> spacer.size(0, 480))
                .addSection("First", card -> card.fillColor(SURFACE).borders(DocumentBorders.bottom(RULE))
                        .addParagraph("One"))
                .addSection("Second", card -> card.keepTogether().fillColor(SURFACE).borders(DocumentBorders.top(RULE))
                        .padding(DocumentInsets.of(4)).addParagraph("Two").addParagraph("Two").addParagraph("Two")
                        .addParagraph("Two").addParagraph("Two").addParagraph("Two")));

        assertThat(exported.pages().get("Second")).as("the layout moves it to the next page").isEqualTo(1);
        assertThat(exported.report().bySubject()).doesNotContainKey("SectionNode");
        // The paragraph Word keeps between the two tables goes with the second, and is named.
        assertThat(exported.report().bySubject().get("space above")).singleElement()
                .extracting(DocxExportReport.Note::detail).isEqualTo("it stands 0.1pt lower than the page sets it, and "
                        + "what follows with it: the space the page leaves above it does not hold the paragraph Word "
                        + "keeps between two tables (0.1pt)");
    }

    @Test
    void aPanelTheLayoutMovesToANewPageKeepsTheSpaceAboveItThere() throws Exception {
        // Word drops a paragraph's space above at the top of a page, as the page keeps it: the
        // panel's 12pt are a line of their own above it, kept with it.
        Exported exported = export(page -> page.addParagraph("Before").addSpacer(spacer -> spacer.size(0, 470))
                .addSection("Second", card -> card.keepTogether().fillColor(SURFACE)
                        .margin(new DocumentInsets(12, 0, 0, 0)).padding(DocumentInsets.of(4)).addParagraph("Two")
                        .addParagraph("Two").addParagraph("Two").addParagraph("Two").addParagraph("Two")
                        .addParagraph("Two")));
        List<org.apache.poi.xwpf.usermodel.IBodyElement> body = exported.document().getBodyElements();
        int panel = body.indexOf(exported.document().getTables().get(0));
        XWPFParagraph line = (XWPFParagraph) body.get(panel - 1);
        CTSpacing spacing = line.getCTP().getPPr().getSpacing();

        assertThat(exported.pages().get("Second")).as("the layout moves it to the next page").isEqualTo(1);
        assertThat(DocxTwips.of(spacing.getLine())).as("its 12pt").isEqualTo(12 * 20);
        assertThat(line.getCTP().getPPr().isSetKeepNext()).isTrue();
    }

    @Test
    void theSpaceAboveACardTakesTheHangOfTheBlockAboveBeforeItsBorder() throws Exception {
        // Half a point more space than the line above hangs: the hang takes it first, and the
        // card's padding holds what its top border then reaches past it; nothing is named.
        long down = reachBelow(hungOverACard(10), "Iconed");
        double gap = down / 20.0 + 0.5;
        Exported exported = hungOverACard(gap);

        assertThat(reachBelow(exported, "Iconed")).as("the same line").isEqualTo(down);
        assertThat(topMargin(panel(exported))).as("its padding less half the border, less the half point the space left")
                .isEqualTo((4 - 1) * 20 - 10);
        assertThat(exported.report().bySubject()).doesNotContainKey("space above").doesNotContainKey("SectionNode");
    }

    /** A contact stack whose last line hangs, then a card ruled above, padded 4pt, {@code gap} below it. */
    private static Exported hungOverACard(double gap) throws Exception {
        return export(page -> page.spacing(gap).addSection("Contact", DocxPanelTailTest::contact)
                .addSection("Card", card -> card.fillColor(SURFACE).borders(DocumentBorders.top(RULE))
                        .padding(DocumentInsets.of(4)).addParagraph("Inside")));
    }

    @Test
    void aHangATableBelowDoesNotTakeIsNamedOnTheTable() throws Exception {
        Exported exported = export(page -> page.addSection("Card", card -> {
            card.fillColor(SURFACE);
            contact(card);
        }).addTable(this::first));

        long down = reachBelow(exported, "Iconed");

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("First");
            assertThat(note.detail()).isEqualTo("it stands " + points(down + 2) + "pt lower than the page sets it, and "
                                                + "what follows with it: the space the page leaves above it does not hold "
                                                + "the line hanging below the block above it (" + points(down) + "pt) and "
                                                + "the paragraph Word keeps between two tables (0.1pt)");
        });
    }

    @Test
    void aParagraphPulledUpStandsBelowNoBorder() throws Exception {
        // An edge below zero pulls a paragraph up, as TealPulse's contact pairs are in their
        // strip: it is no border below a panel, and nothing is named for it.
        Exported exported = export(page -> page.addParagraph("Above")
                .addParagraph(p -> p.text("Pulled").margin(new DocumentInsets(-4, 0, 0, 0)))
                .addRow(row -> row.addParagraph(p -> p.text("Lifted").margin(new DocumentInsets(-4, 0, 0, 0)))
                        .addParagraph("Beside"))
                .addParagraph("Below"));

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
    }

    @Test
    void twoTouchingTablesNameTheParagraphBetweenThem() throws Exception {
        // Word merges two tables with nothing between them, so a paragraph a tenth of a point
        // tall keeps them apart, and stands the second that much lower.
        Exported exported = export(page -> page.addTable(this::first).addTable(this::second));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("Second");
            assertThat(note.detail()).isEqualTo("it stands 0.1pt lower than the page sets it, and what follows with it: "
                                                + "the space the page leaves above it does not hold the paragraph Word keeps "
                                                + "between two tables (0.1pt)");
        });
    }

    @Test
    void twoTablesWithSpaceBetweenThemNameNothing() throws Exception {
        // The paragraph between them takes its tenth of a point out of the space.
        Exported exported = export(page -> page.spacing(4).addTable(this::first).addTable(this::second));
        XWPFParagraph between = (XWPFParagraph) exported.document().getBodyElements().get(1);
        CTSpacing spacing = between.getCTP().getPPr().getSpacing();

        assertThat(exported.report().bySubject()).doesNotContainKey("space above");
        assertThat(DocxTwips.of(spacing.getLine())).as("a tenth of a point").isEqualTo(2);
        assertThat(before(between) + DocxTwips.of(spacing.getAfter() == null ? 0 : spacing.getAfter()))
                .as("the 4pt, less that tenth").isEqualTo(4 * 20 - 2);
    }

    private void first(com.demcha.compose.document.dsl.TableBuilder table) {
        table.name("First").columns(DocumentTableColumn.fixed(100)).rowCells(DocumentTableCell.text("A"));
    }

    private void second(com.demcha.compose.document.dsl.TableBuilder table) {
        table.name("Second").columns(DocumentTableColumn.fixed(100)).rowCells(DocumentTableCell.text("B"));
    }

    /** A contact stack whose last line holds an icon lowered below the page's line. */
    private static void contact(SectionBuilder section) {
        section.spacing(3);
        section.addParagraph(p -> p.textStyle(SMALL).text("London"));
        section.addParagraph(p -> p.name("Iconed").textStyle(SMALL).inlineText("GitHub  ", SMALL)
                .inlineSvgIcon(ICON, 10.5, InlineImageAlignment.CENTER, -1.35, null));
    }

    /** Twips as the report states points: to a hundredth, no trailing zeros. */
    private static String points(long twips) {
        return java.math.BigDecimal.valueOf(Math.round(twips / 20.0 * 100) / 100.0).stripTrailingZeros().toPlainString();
    }

    /**
     * How far a line held to its icon reaches below the page's line, in twips: the exact line Word
     * holds it to, less the page's line, less what of the 3pt gap above it the line starts above
     * the paragraph's top (the space above it written that much shorter).
     */
    private static long reachBelow(Exported exported, String name) {
        // The icon's line — "GitHub" and its icon, in contact() — wherever the tables put it.
        List<XWPFParagraph> paragraphs = new java.util.ArrayList<>(exported.document().getParagraphs());
        exported.document().getTables().forEach(table -> collect(table, paragraphs));
        XWPFParagraph iconed = paragraphs.stream().filter(p -> p.getText().startsWith("GitHub")).findFirst().orElseThrow();
        CTSpacing spacing = iconed.getCTP().getPPr().getSpacing();
        assertThat(spacing.getLineRule()).as("held to its icon")
                .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
        long up = 3 * 20 - (spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0);
        return DocxTwips.of(spacing.getLine()) - up - Math.round(exported.heights().get(name) * 20);
    }

    private static void collect(org.apache.poi.xwpf.usermodel.XWPFTable table, List<XWPFParagraph> paragraphs) {
        table.getRows().forEach(row -> row.getTableCells().forEach(cell -> {
            paragraphs.addAll(cell.getParagraphs());
            cell.getTables().forEach(nested -> collect(nested, paragraphs));
        }));
    }

    /** The cell of the first table in the body: the panel, where the document opens with one. */
    private static XWPFTableCell panel(Exported exported) {
        return exported.document().getTables().get(0).getRow(0).getCell(0);
    }

    /** The panel nested in the first cell of the first table in the body. */
    private static XWPFTableCell nestedPanel(Exported exported) {
        return panel(exported).getTables().get(0).getRow(0).getCell(0);
    }

    private static long topMargin(XWPFTableCell cell) {
        return DocxTwips.of(cell.getCTTc().getTcPr().getTcMar().getTop().getW());
    }

    private static long bottomMargin(XWPFTableCell cell) {
        return DocxTwips.of(cell.getCTTc().getTcPr().getTcMar().getBottom().getW());
    }

    private static XWPFParagraph paragraph(Exported exported, String text) {
        return exported.document().getParagraphs().stream().filter(p -> p.getText().equals(text)).findFirst().orElseThrow();
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }

    /** The export, its report, and each named node's height and first page as the layout places it. */
    private record Exported(XWPFDocument document, DocxExportReport report, java.util.Map<String, Double> heights,
                            java.util.Map<String, Integer> pages) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        java.util.Map<String, Double> heights = new java.util.HashMap<>();
        java.util.Map<String, Integer> pages = new java.util.HashMap<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            session.layoutGraph().nodes().forEach(node -> {
                if (node.semanticName() != null) {
                    heights.putIfAbsent(node.semanticName(), node.placementHeight());
                    pages.putIfAbsent(node.semanticName(), node.startPage());
                }
            });
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), heights, pages);
    }
}
