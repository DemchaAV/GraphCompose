package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A column's last line hanging past its stack's foot in Word takes that much of the gap under the
 * stack, as a row's cell's does ({@link DocxRowOverhangTest}).
 *
 * <p>A stack whose layers are side-by-side columns is written as the cells of one row. The row
 * read nothing of what its columns hang below themselves: Word made it that much taller than the
 * page's, and {@code OrangeOps}' achievement cards, each a stack of a badge column and a text
 * column, stood 0.1 to 0.15pt lower with every card, in Word and in LibreOffice.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLayerColumnsOverhangTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
            + "<circle cx='12' cy='12' r='10' fill='#1A5694'/></svg>");
    private static final DocumentTextStyle SMALL = DocumentTextStyle.DEFAULT.withSize(7);
    private static final double PAGE = 400;
    private static final double MARGIN = 20;
    private static final double SIDEBAR = 120;
    private static final double MAIN = PAGE - 2 * MARGIN - SIDEBAR;
    private static final double GAP = 10;
    /** What the small line after the stack is raised into the space above it, and owes below. */
    private static final long RAISE = DocxExports.SMALL_LINE_RAISE;

    @Test
    void theTallestColumnsHangingLineTakesItsReachOutOfTheGapUnderTheStack() throws Exception {
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, DocxLayerColumnsOverhangTest::contact), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> main.addParagraph(p -> p.textStyle(SMALL).text("Name"))),
                        LayerAlign.TOP_LEFT))) {
            XWPFParagraph iconed = lastParagraph(export.document().getTables().get(0).getRow(0).getCell(0));
            long down = reachBelow(iconed, export.placed("Iconed").placementHeight());

            assertThat(iconed.getCTP().getPPr().getSpacing().getLineRule()).as("held to its icon")
                    .isEqualTo(STLineSpacingRule.EXACT);
            assertThat(down).as("the line reaches below the page's").isPositive();
            assertThat(before(after(export.document()))).as("the gap less that reach")
                    .isEqualTo(Math.round(GAP * 20) - down - RAISE);
        }
    }

    @Test
    void theStacksPaddingBelowIsUnderItsColumnsAndTakesTheHangWithTheGap() throws Exception {
        // The padding is written below the stack's table, as space above what follows: the column
        // ends at the padding, and the line hangs into it.
        double padding = 1;
        try (Export export = export(stack -> stack
                .padding(DocumentInsets.bottom(padding))
                .layer(column("Sidebar", 0, MAIN, DocxLayerColumnsOverhangTest::contact), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> main.addParagraph(p -> p.textStyle(SMALL).text("Name"))),
                        LayerAlign.TOP_LEFT))) {
            XWPFParagraph iconed = lastParagraph(export.document().getTables().get(0).getRow(0).getCell(0));
            long down = reachBelow(iconed, export.placed("Iconed").placementHeight());

            assertThat(down).as("the line reaches below the page's").isPositive();
            assertThat(before(after(export.document()))).as("the padding and the gap less that reach")
                    .isEqualTo(Math.round((GAP + padding) * 20) - down - RAISE);
        }
    }

    @Test
    void columnsHangingSideBySideTakeTheFurthestReachOnce() throws Exception {
        // Word's row is as tall as its tallest column: the two reaches are one.
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, DocxLayerColumnsOverhangTest::contact), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, DocxLayerColumnsOverhangTest::contact), LayerAlign.TOP_LEFT))) {
            XWPFParagraph left = lastParagraph(export.document().getTables().get(0).getRow(0).getCell(0));
            XWPFParagraph right = lastParagraph(export.document().getTables().get(0).getRow(0).getCell(1));
            double pageLine = export.placed("Iconed").placementHeight();
            long down = reachBelow(left, pageLine);

            assertThat(reachBelow(right, pageLine)).as("the premise: both lines reach as far").isEqualTo(down);
            assertThat(down).as("the line reaches below the page's").isPositive();
            assertThat(before(after(export.document()))).as("the gap less one reach")
                    .isEqualTo(Math.round(GAP * 20) - down - RAISE);
        }
    }

    @Test
    void aHangingLineWithRoomUnderItInItsColumnTakesNothing() throws Exception {
        // The column beside it is taller: the line hangs into the row's own room, and Word's row
        // is as tall as the page's.
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, DocxLayerColumnsOverhangTest::contact), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> {
                    for (int i = 0; i < 4; i++) {
                        main.addParagraph(p -> p.textStyle(SMALL).text("Line"));
                    }
                }), LayerAlign.TOP_LEFT))) {
            assertThat(reachBelow(lastParagraph(export.document().getTables().get(0).getRow(0).getCell(0)),
                    export.placed("Iconed").placementHeight())).as("the premise: the line hangs").isPositive();

            assertThat(before(after(export.document()))).isEqualTo(Math.round(GAP * 20) - RAISE);
        }
    }

    @Test
    void aColumnOfTwoLayersHangsFromTheLowerOne() throws Exception {
        // The main column's title layer ends at its top; the layer under it holds the hanging line.
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, side -> side.addParagraph(p -> p.textStyle(SMALL).text("Name"))),
                        LayerAlign.TOP_LEFT)
                .layer(column("TitleLayer", SIDEBAR, 0, top -> top.addParagraph(p -> p.textStyle(SMALL).text("Title"))),
                        LayerAlign.TOP_LEFT)
                .layer(column("MainLayer", SIDEBAR, 0, main -> {
                    main.addSpacer(spacer -> spacer.width(100).height(12));
                    contact(main);
                }), LayerAlign.TOP_LEFT))) {
            XWPFParagraph iconed = lastParagraph(export.document().getTables().get(0).getRow(0).getCell(1));

            assertThat(export.document().getTables().get(0).getRow(0).getCell(1).getText())
                    .as("the premise: the two layers are one column").contains("Title", "GitHub");
            assertThat(iconed.getCTP().getPPr().getSpacing().getLineRule()).as("the premise: held to its icon")
                    .isEqualTo(STLineSpacingRule.EXACT);
            // The space a line raised above it owes moves the line's own space above, so its reach
            // is not read off it here: what matters is that the gap gives some of it.
            assertThat(before(after(export.document()))).as("the gap less the lower layer's reach")
                    .isLessThan(Math.round(GAP * 20) - RAISE);
        }
    }

    @Test
    void aColumnsLastLayerTakesTheHangOutOfItsMarginBelowFirst() throws Exception {
        // Its margin below is inside the stack's foot, and owed under its last line in the cell.
        double margin = 0.5;
        try (Export export = export(stack -> stack
                .layer(column("Sidebar", 0, MAIN, DocxLayerColumnsOverhangTest::contact, margin), LayerAlign.TOP_LEFT)
                .layer(column("Main", SIDEBAR, 0, main -> main.addParagraph(p -> p.textStyle(SMALL).text("Name"))),
                        LayerAlign.TOP_LEFT))) {
            XWPFParagraph iconed = lastParagraph(export.document().getTables().get(0).getRow(0).getCell(0));
            long down = reachBelow(iconed, export.placed("Iconed").placementHeight());

            assertThat(down).as("the premise: the line reaches past the margin").isGreaterThan(Math.round(margin * 20));
            assertThat(before(after(export.document()))).as("the gap less what the margin does not take")
                    .isEqualTo(Math.round(GAP * 20) - (down - Math.round(margin * 20)) - RAISE);
        }
    }

    /** A contact column whose last line holds an icon lowered as TimelineMinimal's are. */
    private static void contact(SectionBuilder section) {
        section.spacing(3);
        section.addParagraph(p -> p.textStyle(SMALL).text("London"));
        section.addParagraph(p -> p.name("Iconed").textStyle(SMALL).inlineText("GitHub  ", SMALL)
                .inlineSvgIcon(ICON, 10.5, InlineImageAlignment.CENTER, -1.35, null));
    }

    private static DocumentNode column(String name, double insetLeft, double insetRight,
                                       Consumer<SectionBuilder> content) {
        return column(name, insetLeft, insetRight, content, 0);
    }

    private static DocumentNode column(String name, double insetLeft, double insetRight,
                                       Consumer<SectionBuilder> content, double marginBelow) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft))
                .margin(DocumentInsets.bottom(marginBelow));
        layer.addSection(name + "Content", section -> content.accept(section.spacing(0)));
        return layer.build();
    }

    private static Export export(Consumer<LayerStackBuilder> layers) throws Exception {
        DocumentSession session = GraphCompose.document().pageSize(PAGE, PAGE)
                .margin(DocumentInsets.of(MARGIN)).create();
        session.pageFlow(page -> page
                .spacing(GAP)
                .addLayerStack(stack -> layers.accept(stack.name("Columns")))
                .addParagraph(p -> p.textStyle(SMALL).text("After")));
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        return new Export(session, document);
    }

    private static XWPFParagraph lastParagraph(XWPFTableCell cell) {
        return cell.getParagraphs().get(cell.getParagraphs().size() - 1);
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

    private record Export(DocumentSession session, XWPFDocument document) implements AutoCloseable {

        PlacedNode placed(String name) {
            return session.layoutGraph().nodes().stream()
                    .filter(node -> name.equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public void close() throws Exception {
            document.close();
            session.close();
        }
    }
}
