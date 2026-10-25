package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A list, a line pair, and a table, a row or a panel opening a band keep the space the layout
 * leaves above them on the page it moves them to, in Word.
 *
 * <p>Word drops the space above a paragraph at the top of a page. Measured in Word 16 and
 * LibreOffice on Windows: a list with a 14pt margin the layout moved to a new page stood 14pt high
 * in both, one padded 10pt or opening a section padded 10pt, 10pt; so did one opening a stack of
 * one layer or a band with a 14pt margin, and a title and its dates set as one line in such a
 * stack. A row, a table or a panel opening a band with a 14pt margin stood 14pt high too: the
 * band's margin was left on the page above. Each held in a line kept with it, as a paragraph's
 * edge is, stood where the page sets it, within 0.1pt of the paragraphs round it.</p>
 *
 * <p>So do a rule, and a table, a row or a panel whose gap above it the layout carries onto the new
 * page, where the gap does not fit at the foot of the page above. Measured in Word 16 and
 * LibreOffice 24.2: {@code ObsidianInvoice}'s footer divider stood 13pt high, its margin, and
 * {@code MeteredInvoice}'s closing row about 13pt, its 12.4pt gap; held in a line, within 0.7pt.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBlockOpeningAPageTest {

    private static final double EDGE = 12;

    /**
     * The page's content is 160pt tall: 300 x 200 with a 20pt margin. A spacer fills 150pt of it,
     * and nothing taller than 10pt fits under it.
     */
    private static final double CONTENT = 160;

    @Test
    void aListsMarginIsHeldInALineKeptWithItsFirstItem() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(list(DocumentInsets.top(EDGE), DocumentInsets.zero()));
        assertOpensTheSecondPage(content, "List");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph opening = paragraphWith(document, "Opening");
            XWPFParagraph line = paragraphBefore(document, opening);
            CTSpacing spacing = spacing(line);

            assertThat(line.getText()).isEmpty();
            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).as("the margin, a line's height").isEqualTo(Math.round(EDGE * 20));
            assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with the item").isTrue();
            assertThat(before(spacing)).as("the filler's height stays above the line").isEqualTo(owed(CONTENT - 10));
            assertThat(before(spacing(opening))).as("its edge is the line's").isZero();
        }
    }

    @Test
    void aListsPaddingIsHeld() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(list(DocumentInsets.zero(), DocumentInsets.top(9)));
        assertOpensTheSecondPage(content, "List");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph opening = paragraphWith(document, "Opening");

            assertThat(lineBefore(document, opening)).isEqualTo(180);
            assertThat(before(spacing(opening))).isZero();
        }
    }

    @Test
    void aListOpeningAPaddedSectionHoldsTheSectionsPadding() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.spacer(0, CONTENT - 10)
                .addSection(section -> section.padding(DocumentInsets.top(10))
                        .add(list(DocumentInsets.zero(), DocumentInsets.zero())));
        assertOpensTheSecondPage(content, "List");
        try (XWPFDocument document = export(content)) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening"))).isEqualTo(200);
        }
    }

    @Test
    void aListOpeningABandHoldsTheBandsMargin() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .margin(DocumentInsets.top(EDGE))
                .topLeft(list(DocumentInsets.zero(), DocumentInsets.zero()))
                .bottomRight(new ParagraphBuilder().text("Caption").build()).build());
        assertOpensTheSecondPage(content, "List");
        try (XWPFDocument document = export(content)) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening"))).isEqualTo(Math.round(EDGE * 20));
            assertThat(paragraphBefore(document, paragraphWith(document, "Caption")).getText())
                    .as("no line before the second layer").isEqualTo("Opening");
        }
    }

    @Test
    void aWrappedFirstItemTakesTheGapBetweenItsLinesOutOfTheLine() throws Exception {
        // As a paragraph's does where a line holds its edge: Word drops the space above the item, so
        // what the gap took from it would set the item's first line that much low — 2.2pt, measured.
        Consumer<PageFlowBuilder> content = movedToANewPage(new ListBuilder().name("List")
                .items("Opening words that run on past the width of this page and wrap onto a second line")
                .lineSpacing(4).margin(DocumentInsets.top(EDGE)).build());
        assertOpensTheSecondPage(content, "List");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph opening = document.getParagraphs().stream()
                    .filter(p -> p.getText().startsWith("Opening")).findFirst().orElseThrow();

            assertThat(lineBefore(document, opening)).as("the margin less the 4pt gap").isEqualTo(Math.round(EDGE * 20) - 80);
            assertThat(before(spacing(opening))).isZero();
        }
    }

    @Test
    void aListOnItsPageHoldsNoLine() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 600, 20,
                movedToANewPage(list(DocumentInsets.top(EDGE), DocumentInsets.zero())))) {
            XWPFParagraph opening = paragraphWith(document, "Opening");

            assertThat(DocxTwips.of(spacing(paragraphBefore(document, opening)).getLine()))
                    .as("the filler's hairline, no line held").isEqualTo(HAIRLINE);
            assertThat(before(spacing(opening))).as("its margin above it").isPositive();
        }
    }

    @Test
    void aListInARowsCellHoldsNoLine() throws Exception {
        // The row's table opens the page; the list's items are its cell's.
        Consumer<PageFlowBuilder> content = page -> page.spacer(0, CONTENT - 10)
                .addRow(row -> row.add(new SectionBuilder().add(list(DocumentInsets.top(EDGE), DocumentInsets.zero())).build())
                        .addParagraph("Beside"));
        assertOpensTheSecondPage(content, "List");
        try (XWPFDocument document = export(content)) {
            assertThat(document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0).getText())
                    .as("the item opens its cell").isEqualTo("Opening");
        }
    }

    @Test
    void aTableOpeningABandHoldsTheBandsMargin() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(bandOpeningWith(new TableBuilder().name("Block")
                .autoColumns(2).row("Opening", "Beside").build()));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph line = paragraphBefore(document, document.getTables().get(0));

            assertThat(line.getText()).isEmpty();
            assertThat(spacing(line).getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing(line).getLine())).as("the band's margin").isEqualTo(Math.round(EDGE * 20));
            assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with the table").isTrue();
        }
    }

    @Test
    void aRowOpeningABandHoldsTheBandsMargin() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(bandOpeningWith(new RowBuilder().name("Block")
                .addParagraph("Opening").addParagraph("Beside").build()));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .isEqualTo(Math.round(EDGE * 20));
        }
    }

    @Test
    void aPanelOpeningABandHoldsTheBandsMargin() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(bandOpeningWith(new SectionBuilder()
                .fillColor(DocumentColor.LIGHT_GRAY).padding(DocumentInsets.of(6))
                .addParagraph(p -> p.name("Block").text("Opening")).build()));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .isEqualTo(Math.round(EDGE * 20));
        }
    }

    @Test
    void theGapAboveABandOpeningWithATableStaysOnThePageAbove() throws Exception {
        // The layout starts the band at its own top edge on the new page: the paragraph's 8pt below
        // it is left at the foot of the page above, and the line holds the band's margin alone.
        assertTheGapStaysAbove(new TableBuilder().name("Block").autoColumns(2).row("Opening", "Beside").build(), 0);
    }

    @Test
    void aRowOpeningABandLeavesOnlyTheGapOnThePageAbove() throws Exception {
        // The band measures its space to the row's first paragraph, past the row's 4pt padding,
        // which the row writes above itself: the line holds it with the band's margin, and the page
        // above keeps the gap alone.
        assertTheGapStaysAbove(new RowBuilder().name("Block").padding(DocumentInsets.top(4))
                .addParagraph("Opening").addParagraph("Beside").build(), 4);
    }

    @Test
    void aPanelOpeningABandLeavesOnlyTheGapOnThePageAbove() throws Exception {
        // The band measures its space to the panel's paragraph, past the panel's 4pt margin.
        assertTheGapStaysAbove(new SectionBuilder().fillColor(DocumentColor.LIGHT_GRAY)
                .padding(DocumentInsets.of(6)).margin(DocumentInsets.top(4))
                .addParagraph(p -> p.name("Block").text("Opening")).build(), 4);
    }

    @Test
    void aTableOpeningABandAfterItsBackdropHoldsTheBandsMargin() throws Exception {
        // A backdrop drawn first is on the new page already, and the table after it starts no page
        // of its own: the band opens the page, and the table is its first block.
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder().margin(DocumentInsets.top(EDGE))
                .back(new com.demcha.compose.document.dsl.ShapeBuilder().size(200, 30)
                        .fillColor(DocumentColor.LIGHT_GRAY).build())
                .topLeft(new TableBuilder().name("Block").autoColumns(2).row("Opening", "Beside").build())
                .bottomRight(new ParagraphBuilder().text("Caption").margin(DocumentInsets.top(30)).build())
                .build());
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .isEqualTo(Math.round(EDGE * 20));
        }
    }

    /**
     * {@code block} opening a band the layout moves to a new page, under a paragraph whose 8pt below
     * it the layout leaves at the foot of the page above; {@code own} is the block's own space above
     * its table, which the line holds with the band's margin.
     */
    private static void assertTheGapStaysAbove(DocumentNode block, double own) throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.spacer(0, CONTENT - 30)
                .addParagraph(p -> p.name("Above").text("Above").margin(DocumentInsets.bottom(8)))
                .add(bandOpeningWith(block));
        assertOpensTheSecondPage(content, "Block");
        assertThat(startPage(content, "Above")).as("the premise: the paragraph stays on the first page").isZero();
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .as("the band's margin and the block's own edge").isEqualTo(Math.round((EDGE + own) * 20));
            assertThat(after(spacing(paragraphWith(document, "Above"))))
                    .as("the paragraph's margin, and the raise its line owes below it")
                    .isEqualTo(160 + DocxExports.DEFAULT_LINE_RAISE);
        }
    }

    @Test
    void aLinePairOpeningItsPageHoldsItsStacksMargin() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(pair(new LayerStackBuilder().name("Pair")
                .margin(DocumentInsets.top(EDGE))));
        assertOpensTheSecondPage(content, "Pair");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph line = paragraphWith(document, "Opening\tBeside");

            assertThat(lineBefore(document, line)).isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing(line))).isZero();
        }
    }

    @Test
    void aLinePairOpeningABandHoldsTheBandsMargin() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(bandOpeningWith(pair(new LayerStackBuilder().name("Block"))));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening\tBeside"))).isEqualTo(Math.round(EDGE * 20));
        }
    }

    @Test
    void aLinePairSetDownItsStackHoldsThatRoomToo() throws Exception {
        // Both halves 4pt down the stack: the line starts 4pt below its top, inside it, and the
        // space above the line on the new page is the margin and those 4pt.
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder().name("Pair")
                .margin(DocumentInsets.top(EDGE))
                .topLeft(new ParagraphBuilder().text("Opening").margin(DocumentInsets.top(4)).build())
                .topRight(new ParagraphBuilder().text("Beside").align(TextAlign.RIGHT)
                        .margin(DocumentInsets.top(4)).build())
                .build());
        assertOpensTheSecondPage(content, "Pair");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph line = paragraphWith(document, "Opening\tBeside");

            assertThat(lineBefore(document, line)).as("12pt and 4pt").isEqualTo(320);
            assertThat(before(spacing(line))).isZero();
        }
    }

    @Test
    void aListHoldingNoLineTakesItsGapFromNoEarlierOne() throws Exception {
        // A picture opening the second page holds its margin in a line; a second picture fills the
        // page under it, and the list opening the third, with no edge to hold and nothing written
        // above its first item, takes the gap between that item's lines from no line.
        Consumer<PageFlowBuilder> content = page -> page.spacer(0, CONTENT - 10)
                .addImage(image -> image.name("Picture").source(picture()).size(60, 30).margin(DocumentInsets.top(EDGE)))
                .addImage(image -> image.source(picture()).size(200, CONTENT - EDGE - 30 - 5))
                .add(new ListBuilder().name("List")
                        .items("Opening words that run on past the width of this page and wrap onto a second line")
                        .lineSpacing(4).build());
        assertThat(startPage(content, "Picture")).as("the premise: the picture opens the second page").isEqualTo(1);
        assertThat(startPage(content, "List")).as("the premise: the list opens the third page").isEqualTo(2);
        try (XWPFDocument document = export(content)) {
            XWPFParagraph picture = document.getParagraphs().stream()
                    .filter(p -> p.getRuns().stream().anyMatch(run -> !run.getEmbeddedPictures().isEmpty()))
                    .findFirst().orElseThrow();

            assertThat(lineBefore(document, picture)).as("the picture's margin, whole").isEqualTo(Math.round(EDGE * 20));
        }
    }

    @Test
    void aLinePairSetAboveItsOverlayIsLeftAsItWas() throws Exception {
        // Its halves offset 3pt up, its text starts above the shape's top: the line would hold the
        // margin, not the space above the text, and is not written.
        Consumer<PageFlowBuilder> content = movedToANewPage(new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Pair").rectangle(260, 20).margin(DocumentInsets.top(EDGE))
                .clipPolicy(com.demcha.compose.document.style.ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().text("Opening").build(), 0, -3,
                        com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().text("Beside").build(), 0, -3,
                        com.demcha.compose.document.node.LayerAlign.TOP_RIGHT)
                .build());
        assertOpensTheSecondPage(content, "Pair");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph line = paragraphWith(document, "Opening\tBeside");

            assertThat(DocxTwips.of(spacing(paragraphBefore(document, line)).getLine()))
                    .as("the filler's hairline, no line held").isEqualTo(HAIRLINE);
            assertThat(before(spacing(line))).as("its space above, as before").isPositive();
        }
    }

    @Test
    void aRuleTheLayoutMovesToANewPageHoldsItsMarginInALineKeptWithIt() throws Exception {
        // A rule's space above is its paragraph's, which Word and LibreOffice drop at the top of a
        // page: ObsidianInvoice's footer divider, moved to a new page with its 13pt margin, stood
        // 13pt high in both.
        Consumer<PageFlowBuilder> content = movedToANewPage(rule(EDGE));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph rule = rule(document);

            assertThat(lineBefore(document, rule)).as("the rule's margin, a line's height").isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing(paragraphBefore(document, rule))))
                    .as("the filler's height stays above the line").isEqualTo(owed(CONTENT - 10));
            assertThat(before(spacing(rule))).as("its edge is the line's").isZero();
        }
    }

    @Test
    void aRuleWhoseGapCarriesToANewPageHoldsTheGapWithItsMargin() throws Exception {
        // 157pt leave 3, less than the gap: the gap and the rule open the second page.
        Consumer<PageFlowBuilder> content = page -> page.spacing(GAP).spacer(0, CONTENT - 3).add(rule(EDGE));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineBefore(document, rule(document))).as("the gap and the margin")
                    .isEqualTo(Math.round((GAP + EDGE) * 20));
        }
    }

    @Test
    void aRuleOnThePageOfTheBlockBeforeItHoldsNoLine() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.spacer(0, CONTENT - 40).add(rule(EDGE));
        assertThat(startPage(content, "Block")).as("the premise: the rule stays on the first page").isZero();
        try (XWPFDocument document = export(content)) {
            XWPFParagraph rule = rule(document);

            assertThat(DocxTwips.of(spacing(paragraphBefore(document, rule)).getLine()))
                    .as("the filler's hairline, no line held").isEqualTo(HAIRLINE);
            assertThat(before(spacing(rule))).as("the filler's height and its margin, as before")
                    .isEqualTo(owed(CONTENT - 40 + EDGE));
        }
    }

    @Test
    void aRowWhoseGapCarriesToANewPageHoldsTheGap() throws Exception {
        // 157pt leave 3, less than the gap: the layout starts the row 4pt below the second page's
        // margin. MeteredInvoice's closing row, the gap above it carried so, stood 12.4pt high in Word
        // and LibreOffice.
        Consumer<PageFlowBuilder> content = gapCarried(0, row(0));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph line = paragraphBefore(document, document.getTables().get(0));

            assertThat(lineHeight(line)).as("the gap").isEqualTo(Math.round(GAP * 20));
            assertThat(after(spacing(paragraphBefore(document, line))))
                    .as("the filler's height alone below it, the gap not left on the page above as well")
                    .isEqualTo(owed(CONTENT - 3));
        }
    }

    @Test
    void aRowWhoseGapStaysAboveHoldsNoLine() throws Exception {
        // 150pt leave 10: the gap fits at the foot of the first page, the row does not.
        Consumer<PageFlowBuilder> content = page -> page.spacing(GAP).spacer(0, CONTENT - 10).add(row(0));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(DocxTwips.of(spacing(paragraphBefore(document, document.getTables().get(0))).getLine()))
                    .as("the filler's hairline, no line held").isEqualTo(HAIRLINE);
        }
    }

    @Test
    void aRowsOwnMarginIsHeldWithTheGapAndTheSpaceBelowTheBlockAboveStaysThere() throws Exception {
        // The filler's 6pt below it fit at the foot of the first page and the gap did not: the layout
        // starts the row its own 6pt and the gap below the margin.
        Consumer<PageFlowBuilder> content = gapCarried(6, row(6));
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph line = paragraphBefore(document, document.getTables().get(0));

            assertThat(lineHeight(line)).as("the gap and the row's margin").isEqualTo(Math.round((GAP + 6) * 20));
            assertThat(after(spacing(paragraphBefore(document, line))))
                    .as("the filler's height and its margin below it stay on the page above")
                    .isEqualTo(owed(CONTENT - 3));
        }
    }

    @Test
    void aRowSetDownItsStackOfOneLayerHoldsNoneOfTheOffset() throws Exception {
        // The layer stands 12pt down its stack, which is named, not written; no gap is carried, and
        // no line holds the offset, where the note says Word writes the row.
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .position(row(0), 0, EDGE, LayerAlign.TOP_LEFT).build());
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(DocxTwips.of(spacing(paragraphBefore(document, document.getTables().get(0))).getLine()))
                    .as("the filler's hairline, no line held").isEqualTo(HAIRLINE);
        }
        assertThat(DocxExports.report(300, 200, 20, content).bySubject().get("layer offset"))
                .as("the offset named").isNotNull();
    }

    @Test
    void aRowOpeningAPaddedSectionHoldsTheGapWithThePadding() throws Exception {
        Consumer<PageFlowBuilder> content = gapCarried(6, new SectionBuilder().padding(DocumentInsets.top(PADDING))
                .add(row(0)).build());
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .as("the gap and the section's padding").isEqualTo(Math.round((GAP + PADDING) * 20));
        }
    }

    @Test
    void aTableWhoseGapCarriesToANewPageHoldsTheGapWithItsMargin() throws Exception {
        Consumer<PageFlowBuilder> content = gapCarried(0, new TableBuilder().name("Block").margin(DocumentInsets.top(6))
                .autoColumns(2).row("Opening", "Beside").build());
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .as("the gap and the table's margin").isEqualTo(Math.round((GAP + 6) * 20));
        }
    }

    @Test
    void aPaintedRowWhoseGapCarriesToANewPageHoldsTheGap() throws Exception {
        Consumer<PageFlowBuilder> content = gapCarried(0, new RowBuilder().name("Block").fillColor(DocumentColor.LIGHT_GRAY)
                .addParagraph("Opening").addParagraph("Beside").build());
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .as("the gap").isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aPanelWhoseGapCarriesToANewPageHoldsTheGap() throws Exception {
        Consumer<PageFlowBuilder> content = gapCarried(0, new SectionBuilder().name("Block")
                .fillColor(DocumentColor.LIGHT_GRAY).padding(DocumentInsets.of(6))
                .addParagraph("Opening").build());
        assertOpensTheSecondPage(content, "Block");
        try (XWPFDocument document = export(content)) {
            assertThat(lineHeight(paragraphBefore(document, document.getTables().get(0))))
                    .as("the gap").isEqualTo(Math.round(GAP * 20));
        }
    }

    /** A gap between blocks, which does not fit in the 3pt the filler leaves at the foot of the first page. */
    private static final double GAP = 4;

    /** A section's padding above a block opening the second page with it. */
    private static final double PADDING = 8;

    /**
     * A 157pt filler with {@code below} of it its margin below, {@link #GAP} between the blocks, and
     * {@code block}: the 3pt the filler leaves at the foot of the first page do not hold the gap.
     */
    private static Consumer<PageFlowBuilder> gapCarried(double below, DocumentNode block) {
        return page -> page.spacing(GAP)
                .addSection(s -> s.margin(DocumentInsets.bottom(below)).spacer(0, CONTENT - 3 - below))
                .add(block);
    }

    /** A row of two paragraphs with {@code margin} above it, named for the layout's placement of it. */
    private static DocumentNode row(double margin) {
        return new RowBuilder().name("Block").margin(DocumentInsets.top(margin))
                .addParagraph("Opening").addParagraph("Beside").build();
    }

    /** A rule with {@code margin} above it, named for the layout's placement of it. */
    private static DocumentNode rule(double margin) {
        return new com.demcha.compose.document.dsl.LineBuilder().name("Block").horizontal(200).thickness(1)
                .margin(DocumentInsets.top(margin)).build();
    }

    /** The paragraph a rule is written as: the one with a border. */
    private static XWPFParagraph rule(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetPBdr())
                .findFirst().orElseThrow();
    }

    /** A list of one item, named for the layout's placement of it. */
    private static DocumentNode list(DocumentInsets margin, DocumentInsets padding) {
        return new ListBuilder().name("List").items("Opening").margin(margin).padding(padding).build();
    }

    /** A title at the left and its dates at the right of one stack, which the export sets as one line. */
    private static DocumentNode pair(LayerStackBuilder stack) {
        return stack.topLeft(new ParagraphBuilder().text("Opening").build())
                .topRight(new ParagraphBuilder().text("Beside").align(TextAlign.RIGHT).build())
                .build();
    }

    /** A band with a 12pt margin opening with {@code first}, a caption at its foot. */
    private static DocumentNode bandOpeningWith(DocumentNode first) {
        return new LayerStackBuilder().margin(DocumentInsets.top(EDGE))
                .topLeft(first)
                .bottomRight(new ParagraphBuilder().text("Caption").margin(DocumentInsets.top(30)).build())
                .build();
    }

    /** A block under a 150pt filler, which the layout moves to the second page. */
    private static Consumer<PageFlowBuilder> movedToANewPage(DocumentNode block) {
        return page -> page.spacer(0, CONTENT - 10).add(block);
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(300, 200, 20, content);
    }

    /** The premise of a test of a block opening a page: the layout starts it on the second. */
    private static void assertOpensTheSecondPage(Consumer<PageFlowBuilder> content, String name) {
        assertThat(startPage(content, name)).as("the premise: %s opens the second page", name).isEqualTo(1);
    }

    private static int startPage(Consumer<PageFlowBuilder> content, String name) {
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            return session.layoutGraph().nodes().stream()
                    .filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow().startPage();
        }
    }

    /** The height of the exact line holding a block's top edge, in twips. */
    private static long lineBefore(XWPFDocument document, XWPFParagraph paragraph) {
        return lineHeight(paragraphBefore(document, paragraph));
    }

    /** The height of an empty exact line kept with what follows it, in twips. */
    private static long lineHeight(XWPFParagraph line) {
        CTSpacing spacing = spacing(line);
        assertThat(line.getText()).as("a line holding no text").isEmpty();
        assertThat(spacing.getLineRule()).as("a line holding its edge").isEqualTo(STLineSpacingRule.EXACT);
        assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with what follows").isTrue();
        return DocxTwips.of(spacing.getLine());
    }

    /** A spacer's paragraph is a tenth of a point tall, in twips (see holdToHairline). */
    private static final long HAIRLINE = 2;

    /**
     * The space owed above the block after the filler, in twips: {@code points} of it, less the
     * filler's own hairline, which is part of its height.
     */
    private static long owed(double points) {
        return Math.round(points * 20) - HAIRLINE;
    }

    private static XWPFParagraph paragraphWith(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(p -> p.getText().equals(text)).findFirst().orElseThrow();
    }

    private static XWPFParagraph paragraphBefore(XWPFDocument document, XWPFParagraph paragraph) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.indexOf(paragraph) - 1);
    }

    /** The body paragraph just before a table. */
    private static XWPFParagraph paragraphBefore(XWPFDocument document, XWPFTable table) {
        List<IBodyElement> body = document.getBodyElements();
        IBodyElement before = body.get(body.indexOf(table) - 1);
        assertThat(before).as("a paragraph before the table").isInstanceOf(XWPFParagraph.class);
        return (XWPFParagraph) before;
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(CTSpacing spacing) {
        return spacing != null && spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
    }

    private static long after(CTSpacing spacing) {
        return spacing != null && spacing.isSetAfter() ? DocxTwips.of(spacing.getAfter()) : 0;
    }

    private static DocumentImageData picture() {
        BufferedImage image = new BufferedImage(60, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", png);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
        return DocumentImageData.fromBytes(png.toByteArray());
    }
}
