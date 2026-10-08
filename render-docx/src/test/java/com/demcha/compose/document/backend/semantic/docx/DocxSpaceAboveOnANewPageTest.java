package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph the layout moves to a new page keeps the space the layout leaves above it there,
 * in Word.
 *
 * <p>Word drops a paragraph's space above at the top of a page and keeps its line's height. The
 * layout keeps a block's top edge there — the containers opening with it — and the gap before
 * it too when the gap itself did not fit at the foot of the page above. {@code Executive}'s
 * second page opens with the spacer between two entries 3pt below the margin, and
 * {@code ModernProfessional}'s with a heading its section pads 8pt down; in Word both pages
 * stood that much high.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxSpaceAboveOnANewPageTest {

    private static final double GAP = 4;
    private static final double SPACER = 3;
    private static final double PADDING = 8;

    /**
     * The page's content is 160pt tall: 300 x 200 with a 20pt margin. A spacer fills it to a
     * height; its height less its hairline is owed below it, and written above the paragraph
     * that follows.
     */
    private static final double CONTENT = 160;

    @Test
    void aSpacerTheGapBeforeCarriesToANewPageHoldsTheGapInItsLine() throws Exception {
        // 157pt leave 3, less than the gap: the gap and the spacer open the second page.
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 3)
                .spacer(0, SPACER)
                .addParagraph("After"))) {
            CTSpacing spacing = spacing(spacerOnThePageAfter(document));

            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(before(spacing)).as("the filler's height, owed below it, and no gap")
                    .isEqualTo(owed(CONTENT - 3));
            assertThat(DocxTwips.of(spacing.getLine())).as("the gap, and the hairline's height")
                    .isEqualTo(Math.round(GAP * 20) + HAIRLINE);
        }
    }

    @Test
    void aSpacerOfNoHeightTheGapBeforeCarriesToANewPageHoldsTheGapInItsLine() throws Exception {
        // A spacer of no height that opens a page holds the gap the layout carries there in its
        // line, as a taller one does.
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 3)
                .spacer(0, 0)
                .addParagraph("After"))) {
            CTSpacing spacing = spacing(spacerOnThePageAfter(document));

            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).as("the gap, and the hairline's height")
                    .isEqualTo(Math.round(GAP * 20) + HAIRLINE);
        }
    }

    @Test
    void aSpacerTheGapBeforeStaysAboveIsWrittenAsBefore() throws Exception {
        // 150pt leave 10: the gap fits at the foot of the first page, the spacer does not.
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 10)
                .spacer(0, 8)
                .addParagraph("After"))) {
            CTSpacing spacing = spacing(spacerOnThePageAfter(document));

            assertThat(before(spacing)).as("the filler's height and the gap, which the page leaves above")
                    .isEqualTo(owed(CONTENT - 10 + GAP));
            assertThat(DocxTwips.of(spacing.getLine())).isEqualTo(HAIRLINE);
        }
    }

    @Test
    void aParagraphItsSectionPadsDownOnANewPageHoldsThePaddingInALineKeptWithIt() throws Exception {
        // 150pt leave 10: the gap fits at the foot of the first page, the section does not.
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 10)
                .addSection("Title", section -> section
                        .padding(new DocumentInsets(PADDING, 0, 0, 0))
                        .addParagraph("Heading")))) {
            XWPFParagraph heading = paragraphWith(document, "Heading");
            XWPFParagraph line = paragraphBefore(document, heading);
            CTSpacing spacing = spacing(line);

            assertThat(line.getText()).isEmpty();
            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).as("the section's padding, a line's height")
                    .isEqualTo(Math.round(PADDING * 20));
            assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with the heading").isTrue();
            assertThat(before(spacing)).as("the filler's height and the gap stay above the line")
                    .isEqualTo(owed(CONTENT - 10 + GAP));
            assertThat(before(spacing(heading))).as("its edge is the line's").isZero();
        }
    }

    @Test
    void aParagraphWhoseGapCarriesToANewPageHoldsTheGapWithItsEdge() throws Exception {
        // 157pt leave 3, less than the gap: the gap and the section open the second page.
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 3)
                .addSection("Title", section -> section
                        .padding(new DocumentInsets(PADDING, 0, 0, 0))
                        .addParagraph("Heading")))) {
            CTSpacing spacing = spacing(paragraphBefore(document, paragraphWith(document, "Heading")));

            assertThat(DocxTwips.of(spacing.getLine())).as("the gap and the padding")
                    .isEqualTo(Math.round((GAP + PADDING) * 20));
            assertThat(before(spacing)).as("the filler's height alone").isEqualTo(owed(CONTENT - 3));
        }
    }

    @Test
    void aParagraphWithNoEdgeOfItsOwnHoldsAGapCarriedToANewPage() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 3)
                .addParagraph("After"))) {
            XWPFParagraph after = paragraphWith(document, "After");
            CTSpacing spacing = spacing(paragraphBefore(document, after));

            assertThat(DocxTwips.of(spacing.getLine())).as("the gap").isEqualTo(Math.round(GAP * 20));
            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(before(spacing(after))).isZero();
        }
    }

    @Test
    void aHeadingHoistedOutOfASectionBegunOnThePageAboveHoldsNothing() throws Exception {
        // 126pt and the gap leave 30: the section and its 8pt padding begin on the first page,
        // and the heading, kept with the body's first line, moves to the second without them.
        java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content = page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 30 - GAP)
                .addSection("Experience", section -> section
                        .padding(new DocumentInsets(PADDING, 0, 0, 0))
                        .addSection("Title", title -> title.keepWithNext().addParagraph("Heading"))
                        .addParagraph("Body"));
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(300, 200).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            List<com.demcha.compose.document.layout.PlacedNode> nodes = session.layoutGraph().nodes();
            assertThat(nodes).filteredOn(n -> "Experience".equals(n.semanticName()))
                    .singleElement().extracting(n -> n.startPage()).as("the section begins on the first page")
                    .isEqualTo(0);
            assertThat(nodes).filteredOn(n -> "Title".equals(n.semanticName()))
                    .singleElement().extracting(n -> n.startPage()).as("the heading opens the second")
                    .isEqualTo(1);
        }
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, content)) {
            XWPFParagraph heading = paragraphWith(document, "Heading");

            assertThat(document.getParagraphs()).as("the filler, the heading and the body, no line")
                    .hasSize(3);
            assertThat(before(spacing(heading))).as("the padding, left for Word to drop")
                    .isEqualTo(owed(CONTENT - 30 - GAP + GAP + PADDING));
        }
    }

    @Test
    void aPullOutOfTheBlockOnThePageAboveLeavesTheLineWhole() throws Exception {
        // The paragraph above pulls what follows it 5pt up, on its own page; the section opens
        // the next page at its padding all the same.
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacer(0, CONTENT - 20)
                .addParagraph(p -> p.text("Above").margin(new DocumentInsets(0, 0, -5, 0)))
                .addSection("Title", section -> section
                        .padding(new DocumentInsets(PADDING, 0, 0, 0))
                        .addParagraph("Heading")))) {
            CTSpacing spacing = spacing(paragraphBefore(document, paragraphWith(document, "Heading")));

            assertThat(DocxTwips.of(spacing.getLine())).as("the padding, none of it pulled")
                    .isEqualTo(Math.round(PADDING * 20));
        }
    }

    @Test
    void aParagraphInATableCellIsLeftToItsTable() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 3)
                .addRow(row -> row.addParagraph(p -> p.text("In a cell")
                        .margin(new DocumentInsets(PADDING, 0, 0, 0)))))) {
            XWPFParagraph first = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);

            assertThat(first.getText()).as("no line in the cell before it").isEqualTo("In a cell");
        }
    }

    @Test
    void theGapBetweenItsLinesComesOffTheLineHoldingItsEdge() throws Exception {
        // A paragraph of n lines has n - 1 gaps on the page and n in Word; the one too many
        // comes off the space above it, which on the new page is the line holding its edge.
        try (XWPFDocument document = wrapped(2, 1.4)) {
            XWPFParagraph text = paragraphWith(document, WRAPPED);
            CTSpacing spacing = spacing(paragraphBefore(document, text));

            assertThat(DocxTwips.of(spacing.getLine())).as("its 2pt edge, less the 1.4pt gap").isEqualTo(40 - 28);
            assertThat(before(spacing)).isEqualTo(owed(CONTENT - 10 + GAP));
            assertThat(before(spacing(text))).isZero();
        }
    }

    @Test
    void whatTheLineCannotGiveOfTheGapComesOffTheSpaceAboveIt() throws Exception {
        try (XWPFDocument document = wrapped(200, 1, 1.5);
             XWPFDocument onOnePage = wrapped(600, 1, 1.5)) {
            XWPFParagraph text = paragraphWith(document, WRAPPED);
            CTSpacing spacing = spacing(paragraphBefore(document, text));

            assertThat(DocxTwips.of(spacing.getLine())).as("a twip left of its 1pt edge").isEqualTo(1);
            assertThat(before(spacing)).as("the 11 twips of the gap the line could not give")
                    .isEqualTo(owed(CONTENT - 10 + GAP) - 11);
            assertThat(DocxTwips.of(spacing(text).getLine()))
                    .as("its lines as tall as where the whole gap comes off the space above it on one page")
                    .isEqualTo(DocxTwips.of(spacing(paragraphWith(onOnePage, WRAPPED)).getLine()));
        }
    }

    private static final String WRAPPED = "A paragraph long enough to wrap onto a second line in a "
            + "column of two hundred and sixty points, and a third.";

    /** A wrapped paragraph with a top edge and a gap between its lines, moved to a new page. */
    private static XWPFDocument wrapped(double edge, double lineSpacing) throws Exception {
        return wrapped(200, edge, lineSpacing);
    }

    private static XWPFDocument wrapped(double pageHeight, double edge, double lineSpacing) throws Exception {
        return DocxExports.withLayout(300, pageHeight, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 10)
                .addParagraph(p -> p.text(WRAPPED).lineSpacing(lineSpacing)
                        .margin(new DocumentInsets(edge, 0, 0, 0))));
    }

    @Test
    void aParagraphOnThePageOfTheBlockBeforeItIsWrittenAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 600, 20, page -> page
                .spacing(GAP)
                .spacer(0, CONTENT - 10)
                .addSection("Title", section -> section
                        .padding(new DocumentInsets(PADDING, 0, 0, 0))
                        .addParagraph("Heading")))) {
            XWPFParagraph heading = paragraphWith(document, "Heading");

            assertThat(document.getParagraphs()).as("the spacer and the heading, no line between them").hasSize(2);
            assertThat(before(spacing(heading))).isEqualTo(owed(CONTENT - 10 + GAP + PADDING));
        }
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

    private static XWPFParagraph spacerOnThePageAfter(XWPFDocument document) {
        return paragraphBefore(document, paragraphWith(document, "After"));
    }

    private static XWPFParagraph paragraphWith(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(p -> p.getText().equals(text)).findFirst().orElseThrow();
    }

    private static XWPFParagraph paragraphBefore(XWPFDocument document, XWPFParagraph paragraph) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.indexOf(paragraph) - 1);
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(CTSpacing spacing) {
        return spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
    }
}
