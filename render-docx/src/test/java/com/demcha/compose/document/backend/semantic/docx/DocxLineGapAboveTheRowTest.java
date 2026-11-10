package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A paragraph opening a cell of a row of columns, or of a stack's columns, takes the gap above its
 * first line from the space above the row, where neither its own space above nor a padding holds
 * it: the row is lifted out of that space, and its lines stand the page's distance apart.
 *
 * <p>Word sets a paragraph's lines one height apart, the gap between them inside the line, and a
 * paragraph of {@code n} lines holds a gap too many, which comes off the space above it. Opening a
 * row's column it has none, and its gaps were shared out over its lines instead:
 * {@code OrangeOps}' degree, two lines beside its badge, stood 0.45pt low and 0.45pt high.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLineGapAboveTheRowTest {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().fontName(FontName.LATO).size(9).build();
    private static final double GAP = 3;
    private static final double ABOVE = 10;
    private static final String WRAPPING = "Research, audit, insights, and strategy across the brand";
    private static final double PAGE_WIDTH = 400;
    private static final double MARGIN = 20;
    private static final double SIDEBAR = 180;
    private static final double MAIN = PAGE_WIDTH - 2 * MARGIN - SIDEBAR;

    @Test
    void aParagraphOpeningARowsColumnTakesTheGapFromTheSpaceAboveTheRow() throws Exception {
        try (Export lifted = export(page -> aRow(page, ABOVE, WRAPPING, GAP));
             Export alone = export(page -> aRow(page, ABOVE, "Phase", GAP))) {
            double line = lifted.lineOnThePage("Text", GAP);

            assertThat(lineTwips(lifted.paragraph("Research"))).as("the line and the whole gap, the page's distance apart")
                    .isCloseTo(Math.round((line + GAP) * 20), within(1L));
            assertThat(after(lifted.paragraph("Above"))).as("the row lifted out of the space above it")
                    .isEqualTo(after(alone.paragraph("Above")) - Math.round(GAP * 20));
            assertThat(before(lifted.paragraph("Beside"))).as("the cell beside it starts as much lower inside")
                    .isEqualTo(before(alone.paragraph("Beside")) + Math.round(GAP * 20));
        }
    }

    @Test
    void aRowWithNoSpaceAboveItHasItsParagraphsGapsSharedOut() throws Exception {
        try (Export export = export(page -> page.addRow(row -> twoColumns(row, WRAPPING, GAP)))) {
            double line = export.lineOnThePage("Text", GAP);

            assertThat(lineTwips(export.paragraph("Research"))).as("the line and half the gap, as before")
                    .isCloseTo(Math.round((line + GAP / 2) * 20), within(1L));
        }
    }

    @Test
    void aColumnOpeningWithAPanelStartsLowerBelowAHairline() throws Exception {
        // Painted, the column beside is a panel written at the top of its cell, which has no space
        // above it in Word: a hairline paragraph above it holds the lift, or it would rise whole.
        try (Export export = export(page -> {
            page.addParagraph(p -> p.name("Above").text("Above").textStyle(BODY).margin(DocumentInsets.bottom(ABOVE)));
            page.addRow(row -> row.name("Row")
                    .addSection("Left", left -> left.addParagraph(p -> p.name("Text").text(WRAPPING).textStyle(BODY).lineSpacing(GAP)))
                    .addSection("Right", right -> right.fillColor(DocumentColor.rgb(240, 244, 248))
                            .addParagraph(p -> p.name("Beside").text("Beside").textStyle(BODY))));
        })) {
            double line = export.lineOnThePage("Text", GAP);
            XWPFTableCell beside = export.document().getTables().get(0).getRow(0).getCell(1);

            assertThat(lineTwips(export.paragraph("Research"))).as("the line and the whole gap")
                    .isCloseTo(Math.round((line + GAP) * 20), within(1L));
            assertThat(beside.getBodyElements().get(1)).as("the premise: the panel, a table in the cell")
                    .isInstanceOf(XWPFTable.class);
            XWPFParagraph hairline = (XWPFParagraph) beside.getBodyElements().get(0);
            assertThat(before(hairline) + lineTwips(hairline)).as("the hairline above it holding the lift")
                    .isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aRowOpeningItsPageIsNotLifted() throws Exception {
        // Word drops the space above a page's first block: there is nothing to lift the row out of.
        try (Export export = export(page -> {
            page.spacer(0, 520);
            aRow(page, ABOVE, WRAPPING, GAP);
        })) {
            assertThat(export.placed("Row").startPage()).as("the premise: the row opens the second page").isOne();
            double line = export.lineOnThePage("Text", GAP);

            assertThat(lineTwips(export.paragraph("Research"))).as("the line and half the gap, as before")
                    .isCloseTo(Math.round((line + GAP / 2) * 20), within(1L));
        }
    }

    @Test
    void aGapTooSmallForItsLinesPositionToTakeBackIsSharedOut() throws Exception {
        // Lifted by 0.6pt, the first of two lines would stand 0.36pt high, less than its position
        // moves it, and a cell's last paragraph has no space under it to lower it into. Its lines,
        // as tall as Lato's own and the gap's half, are written at a least height, whose text
        // stands on the line's foot: the middle of the two stands a quarter of the gap low, which
        // the row is lifted by.
        double gap = 0.6;
        try (Export export = export(page -> aRow(page, ABOVE, WRAPPING, gap));
             Export alone = export(page -> aRow(page, ABOVE, "Phase", gap))) {
            double line = export.lineOnThePage("Text", gap);

            assertThat(lineTwips(export.paragraph("Research"))).as("the line and half the gap, as before")
                    .isCloseTo(Math.round((line + gap / 2) * 20), within(1L));
            assertThat(export.paragraph("Research").getCTP().getPPr().getSpacing().getLineRule())
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AT_LEAST);
            assertThat(after(export.paragraph("Above"))).as("lifted by the middle line's quarter gap")
                    .isEqualTo(after(alone.paragraph("Above")) - Math.round(gap / 4 * 20));
        }
    }

    @Test
    void aStacksColumnTakesTheGapFromTheSpaceAboveTheStack() throws Exception {
        try (Export lifted = export(page -> aStack(page, ABOVE, WRAPPING, false));
             Export alone = export(page -> aStack(page, ABOVE, "Phase", false))) {
            double line = lifted.lineOnThePage("Text", GAP);

            assertThat(lineTwips(lifted.paragraph("Research"))).isCloseTo(Math.round((line + GAP) * 20), within(1L));
            assertThat(after(lifted.paragraph("Above"))).isEqualTo(after(alone.paragraph("Above")) - Math.round(GAP * 20));
        }
    }

    @Test
    void aRowHeldToAHeightIsHeldAsMuchTallerAsItIsLifted() throws Exception {
        // A rule as tall as the band, drawn over the columns, holds the row at the band's height, and
        // the text is shorter: held as it was, the lifted row would end that much higher.
        try (Export lifted = export(page -> aStack(page, ABOVE, WRAPPING, true));
             Export unlifted = export(page -> aStack(page, 0, WRAPPING, true))) {
            assertThat(lineTwips(unlifted.paragraph("Research"))).as("the premise: no space to lift the row out of")
                    .isLessThan(lineTwips(lifted.paragraph("Research")));
            assertThat(heldTwips(lifted)).as("the premise: the row held to the band's height").isPositive();

            assertThat(heldTwips(lifted)).isEqualTo(heldTwips(unlifted) + Math.round(GAP * 20));
        }
    }

    /** A paragraph {@code above} points over a row: its first column the text, its second a line beside it. */
    private static void aRow(PageFlowBuilder page, double above, String text, double gap) {
        page.addParagraph(p -> p.name("Above").text("Above").textStyle(BODY).margin(DocumentInsets.bottom(above)));
        page.addRow(row -> twoColumns(row, text, gap));
        page.addParagraph(p -> p.name("Under").text("Under").textStyle(BODY));
    }

    private static void twoColumns(com.demcha.compose.document.dsl.RowBuilder row, String text, double gap) {
        row.name("Row")
                .addSection("Left", left -> left.addParagraph(p -> p.name("Text").text(text).textStyle(BODY).lineSpacing(gap)))
                .addSection("Right", right -> right.addParagraph(p -> p.name("Beside").text("Beside").textStyle(BODY)));
    }

    /**
     * A paragraph {@code above} points over a stack of two columns, the text opening the first and a
     * line beside it in the second; with a rule as tall as 60pt drawn across where {@code ruled}.
     */
    private static void aStack(PageFlowBuilder page, double above, String text, boolean ruled) {
        page.addParagraph(p -> p.name("Above").text("Above").textStyle(BODY).margin(DocumentInsets.bottom(above)));
        page.addLayerStack(stack -> {
            stack.name("Columns")
                    .layer(column("First", 0, MAIN, first -> first.addParagraph(p -> p.name("Text").text(text)
                            .textStyle(BODY).lineSpacing(GAP))), LayerAlign.TOP_LEFT)
                    .layer(column("Second", SIDEBAR, 0, second -> second.addParagraph(p -> p.name("Beside")
                            .text("Beside").textStyle(BODY))), LayerAlign.TOP_LEFT);
            if (ruled) {
                stack.layer(sleeve("Rule", SIDEBAR - 4, new com.demcha.compose.document.dsl.LineBuilder().name("RuleLine")
                        .vertical(60).thickness(0.5).color(DocumentColor.rgb(200, 200, 200)).build()), LayerAlign.TOP_LEFT);
            }
        });
        page.addParagraph(p -> p.name("Under").text("Under").textStyle(BODY));
    }

    /** One column as a full-width layer, inset to its band, as a two-column CV lays it out. */
    private static DocumentNode column(String name, double insetLeft, double insetRight, Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft));
        layer.addSection(name + "Content", section -> content.accept(section.spacing(0)));
        return layer.build();
    }

    private static DocumentNode sleeve(String name, double insetLeft, DocumentNode child) {
        SectionBuilder sleeve = new SectionBuilder();
        sleeve.name(name).spacing(0).padding(DocumentInsets.zero()).margin(new DocumentInsets(0, 0, 0, insetLeft));
        sleeve.add(child);
        return sleeve.build();
    }

    private static long heldTwips(Export export) {
        for (XWPFTable table : export.document().getTables()) {
            XWPFTableRow row = table.getRow(0);
            if (row.getTableCells().size() == 2 && row.getCtRow().isSetTrPr() && row.getCtRow().getTrPr().sizeOfTrHeightArray() > 0) {
                return DocxTwips.of(row.getCtRow().getTrPr().getTrHeightArray(0).getVal());
            }
        }
        return 0;
    }

    private static long lineTwips(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine());
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = spacingOf(paragraph);
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTSpacing spacing = spacingOf(paragraph);
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter());
    }

    private static CTSpacing spacingOf(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing() ? null : properties.getSpacing();
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        DocumentSession session = GraphCompose.document().pageSize(PAGE_WIDTH, 600).margin(DocumentInsets.of(MARGIN)).create();
        session.pageFlow(content::accept);
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        return new Export(session, document);
    }

    private record Export(DocumentSession session, XWPFDocument document) implements AutoCloseable {

        /** The paragraph, in the body or a table's cell at any depth, whose text starts so. */
        XWPFParagraph paragraph(String text) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (paragraph.getText().startsWith(text)) {
                    return paragraph;
                }
            }
            for (XWPFTable table : document.getTables()) {
                XWPFParagraph found = paragraph(table, text);
                if (found != null) {
                    return found;
                }
            }
            throw new AssertionError("no paragraph reads " + text);
        }

        private static XWPFParagraph paragraph(XWPFTable table, String text) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().startsWith(text)) {
                            return paragraph;
                        }
                    }
                    for (XWPFTable nested : cell.getTables()) {
                        XWPFParagraph found = paragraph(nested, text);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
            return null;
        }

        /** The height of one of a two-line paragraph's lines on the page, its gap taken off. */
        double lineOnThePage(String name, double gap) {
            return (placed(name).placementHeight() - gap) / 2;
        }

        PlacedNode placed(String name) {
            return session.layoutGraph().nodes().stream().filter(node -> name.equals(node.semanticName()))
                    .findFirst().orElseThrow(() -> new AssertionError("nothing placed is named " + name));
        }

        @Override
        public void close() throws Exception {
            document.close();
            session.close();
        }
    }
}
