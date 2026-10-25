package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A panel the layout breaks over pages inside the table it holds is a table a page.
 *
 * <p>The layout breaks such a table between rows, repeats its header rows at the top of the next
 * page and goes on with the panel there, no padding above its text. Written as one table, Word
 * broke the panel's row where its lines ran out and repeated no header row of the table nested in
 * its cell: measured in Word 16, {@code ObsidianInvoice}'s and {@code SubscriptionInvoice}'s line
 * items set over two pages stood 24 to 30pt high on the second, and over three one row stood a
 * page early, in LibreOffice 24.2 too. Cut at the row each page starts with, every line stands on
 * the page the layout sets it on in both.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPanelOverPagesTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final String NOTE = "the layout breaks it over pages inside its table";
    private static final double PAGE_HEIGHT = 300;
    // Other top and bottom margins than the document's 20pt, page 2's alone and from a page on, at
    // which a 30-row card's table is cut where each of its four pages starts.
    private static final DocumentInsets MIDDLE = new DocumentInsets(35, 20, 44, 20);
    private static final DocumentInsets LATER = new DocumentInsets(45, 20, 34, 20);
    private static final List<Long> LATER_TWIPS = List.of(900L, 680L);

    @Test
    void aPanelWhoseTableRunsOverAPageIsATableAPageCutAtTheRowsThePagesStartWith() throws Exception {
        Exported exported = export(card(20, true, null, null));

        List<XWPFTable> parts = parts(exported.document());
        assertThat(exported.pages()).as("the premise: the card runs onto a second page").hasSize(2);
        assertThat(parts).hasSize(2);
        assertThat(rowsOf(parts)).as("each part the rows the page sets on its page, under the header")
                .isEqualTo(exported.pages());
    }

    @Test
    void aTableOverThreePagesIsCutTwiceEachPartUnderItsHeader() throws Exception {
        Exported exported = export(card(30, true, null, null));

        assertThat(exported.pages()).as("the premise: three pages").hasSize(3);
        assertThat(rowsOf(parts(exported.document()))).isEqualTo(exported.pages());
    }

    @Test
    void aTableWithNoHeaderRowsIsCutWithNoneAndItsNoteSaysNothingOfOne() throws Exception {
        Exported exported = export(card(20, false, null, null));

        List<List<String>> rows = rowsOf(parts(exported.document()));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).get(0)).as("the second part opens with its first row").startsWith("Row ");
        assertThat(cutNote(exported.report())).doesNotContain("header");
    }

    @Test
    void thePartsStandApartByAShortParagraphThatBreaksThePageBeforeItself() throws Exception {
        XWPFDocument document = export(card(20, true, null, null)).document();

        List<IBodyElement> body = document.getBodyElements();
        int first = body.indexOf(parts(document).get(0));
        XWPFParagraph between = (XWPFParagraph) body.get(first + 1);
        assertThat(between.isPageBreak()).as("a break before it, which adds no page where it opens one").isTrue();
        assertThat(between.getRuns()).as("no break in it, which would").isEmpty();
        assertThat(between.getCTP().getPPr().getSpacing().getLine().toString()).as("a tenth of a point").isEqualTo("2");
        assertThat(body.get(first + 2)).isSameAs(parts(document).get(1));
    }

    @Test
    void aPartAfterACutHasNoTopPaddingAndOneBeforeACutNoBottomPadding() throws Exception {
        // The page sets the card's rows down to the page's foot and closes it on its last page only.
        List<XWPFTable> parts = parts(export(card(30, true, null, null)).document());

        assertThat(margin(parts.get(0), true)).as("the card's padding, its border's room taken").isPositive();
        assertThat(margin(parts.get(0), false)).isZero();
        assertThat(margin(parts.get(1), true)).isZero();
        assertThat(margin(parts.get(1), false)).isZero();
        assertThat(margin(parts.get(2), true)).isZero();
        assertThat(margin(parts.get(2), false)).as("the last closes the card").isPositive();
    }

    @Test
    void whatTheCardHoldsBeforeAndAfterItsTableStaysWithTheFirstAndTheLastPart() throws Exception {
        List<XWPFTable> parts = parts(export(card(20, true,
                above -> above.addParagraph("Heading"), below -> below.addParagraph("Closing"))).document());

        XWPFTableCell first = parts.get(0).getRow(0).getCell(0);
        XWPFTableCell last = parts.get(parts.size() - 1).getRow(0).getCell(0);
        assertThat(texts(first)).contains("Heading").doesNotContain("Closing");
        assertThat(texts(last)).contains("Closing").doesNotContain("Heading");
        IBodyElement closer = first.getBodyElements().get(first.getBodyElements().size() - 1);
        assertThat(closer).as("the first part's cell, ending with its table, ends with a paragraph")
                .isInstanceOf(XWPFParagraph.class);
        assertThat(((XWPFParagraph) closer).getCTP().getPPr().getSpacing().getLine().toString())
                .as("a tenth of a point, as one closing a table in a cell").isEqualTo("2");
    }

    @Test
    void theCutIsNamedWithItsPagesItsFixedBreaksAndTheTopBorderItsPartsStandLowerBy() throws Exception {
        String note = cutNote(export(card(30, true, null, null)).report());

        assertThat(note).contains("cut at the rows pages 2 and 3 start with");
        assertThat(note).contains("opening with the table's header rows");
        assertThat(note).contains("page breaks fixed there");
        assertThat(note).contains("in Word a part after a cut stands about its top border's width, 1pt, lower");
    }

    @Test
    void aPageWhoseRowsHoldAFieldIsLeftWholeAndTheNextOneIsStillCut() throws Exception {
        // A page reference in a row on page 2: the table is not split where a row from page 2's
        // first on holds a field, as a section's split of a table is not, and is where page 3 starts.
        Exported exported = export(page -> page.addSection(card -> card.fillColor(SURFACE)
                .stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6))
                .addParagraph(p -> p.text("Heading").anchor("top"))
                .addTable(table -> {
                    table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100))
                            .header("Item", "Amount").repeatHeader();
                    for (int row = 1; row <= 30; row++) {
                        table.rowCells(DocumentTableCell.text("Row " + row), row == 15
                                ? DocumentTableCell.node(new PageReferenceNode("top", DocumentTextStyle.DEFAULT,
                                        TextAlign.LEFT))
                                : DocumentTableCell.text(String.valueOf(row)));
                    }
                })));

        assertThat(exported.pages()).as("the premise: three pages").hasSize(3);
        assertThat(exported.pages().get(1)).as("the premise: row 15 on page 2").contains("Row 15");
        List<String> first = new ArrayList<>(exported.pages().get(0));
        first.addAll(exported.pages().get(1).subList(1, exported.pages().get(1).size()));
        assertThat(rowsOf(parts(exported.document()))).as("pages 1 and 2 one part, page 3 another")
                .containsExactly(first, exported.pages().get(2));
        assertThat(cutNote(exported.report())).contains("cut at the row page 3 starts with")
                .contains("it is not cut where page 2 starts, at a row its table cannot be split at");
    }

    @Test
    void aCardThatCannotBeCutAtAnyPageIsNamedAsWrittenWhole() throws Exception {
        // An anchored card's bookmark opens in its table's header row, which a part's copy would
        // hold again: the table is split nowhere.
        Exported exported = export(page -> page.addSection(card -> card.anchor("items").fillColor(SURFACE)
                .stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6))
                .addTable(table -> rows(table, 20, true))));

        assertThat(exported.pages()).as("the premise: the card runs onto a second page").hasSize(2);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
        assertThat(cutNote(exported.report())).contains("written as one table")
                .contains("it is not cut where page 2 starts, at a row its table cannot be split at")
                .doesNotContain("page breaks fixed");
    }

    @Test
    void aPageTheCardStartsOutsideItsTableIsNamedAndTheTableStillCut() throws Exception {
        // A heading and a gap fill page 1: the table starts on page 2 and runs onto page 3.
        Exported exported = export(card(20, true, above -> above.addParagraph("Heading")
                .addSpacer(spacer -> spacer.height(225)), null));

        assertThat(exported.cardPages()).as("the premise: the card runs over three pages").isEqualTo(3);
        assertThat(exported.pages().get(0)).as("the premise: no row of the table on page 1").containsExactly("Item");
        assertThat(parts(exported.document())).hasSize(2);
        assertThat(cutNote(exported.report())).contains("it is not cut where page 2 starts, outside its table");
    }

    @Test
    void aPageAboveWhichTheRowsEndTooNearTheFootIsNotCut() throws Exception {
        // Word sets the part's bottom border inside its row: where the page's last row on page 1
        // leaves less room than that above the foot, Word would set that row on a page of its own.
        double gap = gapLeavingTheLastRowNearTheFoot(true);
        Exported exported = export(page -> {
            page.addSpacer(spacer -> spacer.height(gap));
            card(20, true, null, null).accept(page);
        });

        assertThat(parts(exported.document())).as("not cut where page 2 starts").hasSize(1);
        assertThat(cutNote(exported.report())).contains("it is not cut where page 2 starts, the page above setting "
                                                         + "its rows too near its foot")
                .contains("repeating no header row of the table nested in its cell");
    }

    @Test
    void aTableWithNoHeaderRowsCutNowhereIsNamedWithNoWordOfAHeader() throws Exception {
        double gap = gapLeavingTheLastRowNearTheFoot(false);
        Exported exported = export(page -> {
            page.addSpacer(spacer -> spacer.height(gap));
            card(20, false, null, null).accept(page);
        });

        assertThat(cutNote(exported.report())).contains("written as one table").doesNotContain("header");
    }

    @Test
    void aPartOpeningItsPageNeedsRoomForItsTopBorderAndTheParagraphBeforeItToo() throws Exception {
        // A page height at which page 2's last row leaves room for a bottom border and a point,
        // and not for the top border and the hairline paragraph the part opening page 2 sets too.
        double height = heightLeavingTheMiddlePartTooLittleRoom();
        Exported exported = export(height, card(30, true, null, null));

        assertThat(parts(exported.document())).as("cut where page 2 starts, not where page 3 does").hasSize(2);
        assertThat(cutNote(exported.report())).contains("cut at the row page 2 starts with")
                .contains("it is not cut where page 3 starts, the page above setting its rows too near its foot");
    }

    @Test
    void aPageTheCardRunsOntoAfterItsTableIsNamed() throws Exception {
        // A gap after the table moves the card's closing line to page 3.
        Exported exported = export(card(20, true, null, below -> below.addSpacer(spacer -> spacer.height(240))
                .addParagraph("Closing")));

        assertThat(exported.cardPages()).as("the premise: the card runs over three pages").isEqualTo(3);
        assertThat(exported.pages()).as("the premise: its table over two").hasSize(2);
        assertThat(parts(exported.document())).hasSize(2);
        assertThat(cutNote(exported.report())).contains("it is not cut where page 3 starts, outside its table");
    }

    @Test
    void aTableDeeperThanThePanelsOwnChildrenIsNotCut() throws Exception {
        Exported exported = export(page -> page.addSection(card -> card.fillColor(SURFACE)
                .stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6))
                .addSection(inner -> inner.addTable(table -> rows(table, 20, true)))));

        assertThat(exported.cardPages()).as("the premise: the card runs onto a second page").isEqualTo(2);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains(NOTE));
    }

    /**
     * A gap above the card that leaves its table's last row on page 1 less than its 1pt bottom
     * border and a point above the page's foot, found by laying the page out.
     */
    private static double gapLeavingTheLastRowNearTheFoot(boolean header) {
        for (double gap = 0; gap < 40; gap += 0.25) {
            double height = gap;
            try (DocumentSession session = session(page -> {
                page.addSpacer(spacer -> spacer.height(height));
                card(20, header, null, null).accept(page);
            })) {
                DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
                TableNode table = first(session.documentGraph().roots(), TableNode.class);
                double room = layout.lowestRowOn(table, 0) - layout.marginOf(0).bottom();
                if (room < 1.5 && layout.placement(table).endPage() == 1) {
                    return gap;
                }
            }
        }
        throw new AssertionError("no gap leaves the last row near the foot");
    }

    /**
     * A page height at which a 30-row card runs over three pages, its rows on page 1 ending far
     * enough above the foot for the first part's 1pt bottom border and a point, and those on page 2
     * ending room enough for that but not for the second part's 1pt top border and 0.1pt paragraph
     * too, found by laying the pages out.
     */
    private static double heightLeavingTheMiddlePartTooLittleRoom() {
        for (double height = 280; height < 340; height += 0.25) {
            try (DocumentSession session = session(height, card(30, true, null, null))) {
                DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
                TableNode table = first(session.documentGraph().roots(), TableNode.class);
                if (layout.placement(table).startPage() != 0 || layout.placement(table).endPage() != 2) {
                    continue;
                }
                double first = layout.lowestRowOn(table, 0) - layout.marginOf(0).bottom();
                double second = layout.lowestRowOn(table, 1) - layout.marginOf(1).bottom();
                if (first >= 2.05 && second >= 2.05 && second < 3.05) {
                    return height;
                }
            }
        }
        throw new AssertionError("no page height leaves the middle part too little room");
    }

    @Test
    void aPanelInAnEarlierSectionIsCutByItsOwnSectionsLayout() throws Exception {
        // Each section is laid out on its own: the cut reads the card's pages off its section's.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (MultiSectionDocument sections = GraphCompose.documents()
                .section(session(card(20, true, null, null)))
                .section(session(page -> page.addParagraph("Second section")))
                .create()) {
            docx = sections.export(DocxSemanticBackend.builder().reportSink(report::set).build());
        }

        assertThat(parts(new XWPFDocument(new ByteArrayInputStream(docx)))).hasSize(2);
        assertThat(cutNote(report.get())).contains("cut at the row section 1, page 2 starts with");
    }

    @Test
    void aPanelInALaterSectionNamesItsPagesAsItsSectionCountsThem() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (MultiSectionDocument sections = GraphCompose.documents()
                .section(session(page -> page.addParagraph("First section")))
                .section(session(card(20, true, null, null)))
                .create()) {
            docx = sections.export(DocxSemanticBackend.builder().reportSink(report::set).build());
        }

        assertThat(parts(new XWPFDocument(new ByteArrayInputStream(docx)))).hasSize(2);
        assertThat(cutNote(report.get())).contains("cut at the row section 2, page 2 starts with");
    }

    @Test
    void aPageWithOtherMarginsStartingAtACutEndsAWordSectionInTheParagraphBetweenTheParts() throws Exception {
        // Measured in Word and LibreOffice: with one set of margins, a top margin 54pt larger from
        // page 2 on stood every line of pages 2 and 3 52.9pt high; ended at the cut, line for line.
        Exported exported = export(PAGE_HEIGHT, List.of(PageMarginRule.from(2, LATER)), card(30, true, null, null));
        XWPFDocument document = exported.document();

        List<XWPFTable> parts = parts(document);
        assertThat(exported.pages()).as("the premise: three pages at least").hasSizeGreaterThanOrEqualTo(3);
        assertThat(rowsOf(parts)).as("cut as without the margins").isEqualTo(exported.pages());
        XWPFParagraph first = between(document, parts.get(0));
        assertThat(endingASection(document)).containsExactly(first);
        assertThat(first.isPageBreak()).as("the section's own break opens page 2").isFalse();
        assertThat(first.getCTP().getPPr().getSpacing().getLine().toString()).isEqualTo("2");
        assertThat(topAndBottom(first.getCTP().getPPr().getSectPr())).as("page 1's").containsExactly(400L, 400L);
        assertThat(topAndBottom(body(document))).as("page 2's on").isEqualTo(LATER_TWIPS);
        XWPFParagraph second = between(document, parts.get(1));
        assertThat(second.isPageBreak()).as("page 3's margins are page 2's: the page breaks there").isTrue();
        assertThat(marginNotes(exported.report())).singleElement().satisfies(note -> {
            assertThat(note.path()).endsWith("SectionNode[0]");
            assertThat(note.detail()).isEqualTo("page 2's top and bottom margins differ from page 1's: written as a "
                                                + "Word section that starts where the panel is cut at the row of its "
                                                + "table page 2 starts with, in the paragraph between the two parts");
        });
    }

    @Test
    void eachCutWhereTheMarginsChangeEndsASectionOfItsOwn() throws Exception {
        Exported exported = export(PAGE_HEIGHT, List.of(PageMarginRule.page(2, MIDDLE), PageMarginRule.from(3, LATER)),
                card(30, true, null, null));
        XWPFDocument document = exported.document();

        List<XWPFTable> parts = parts(document);
        assertThat(exported.pages()).as("the premise: three pages at least").hasSizeGreaterThanOrEqualTo(3);
        XWPFParagraph first = between(document, parts.get(0));
        XWPFParagraph second = between(document, parts.get(1));
        assertThat(endingASection(document)).containsExactly(first, second);
        assertThat(second.isPageBreak()).isFalse();
        assertThat(topAndBottom(first.getCTP().getPPr().getSectPr())).as("page 1's").containsExactly(400L, 400L);
        assertThat(topAndBottom(second.getCTP().getPPr().getSectPr())).as("page 2's").containsExactly(700L, 880L);
        assertThat(topAndBottom(body(document))).as("page 3's on").isEqualTo(LATER_TWIPS);
        assertThat(marginNotes(exported.report())).extracting(DocxExportReport.Note::detail)
                .satisfiesExactly(
                        page2 -> assertThat(page2).startsWith("page 2's top and bottom margins differ from page 1's: "
                                                              + "written as a Word section"),
                        page3 -> assertThat(page3).startsWith("page 3's top and bottom margins differ from page 2's: "
                                                              + "written as a Word section"));
    }

    @Test
    void aCutAfterOneOnPagesOfTheSameMarginsEndsTheSectionWhereTheyChange() throws Exception {
        Exported exported = export(PAGE_HEIGHT, List.of(PageMarginRule.from(3, LATER)), card(30, true, null, null));
        XWPFDocument document = exported.document();

        List<XWPFTable> parts = parts(document);
        assertThat(exported.pages()).as("the premise: three pages at least").hasSizeGreaterThanOrEqualTo(3);
        XWPFParagraph first = between(document, parts.get(0));
        XWPFParagraph second = between(document, parts.get(1));
        assertThat(first.isPageBreak()).isTrue();
        assertThat(endingASection(document)).containsExactly(second);
        assertThat(topAndBottom(second.getCTP().getPPr().getSectPr())).as("pages 1 and 2's").containsExactly(400L, 400L);
        assertThat(topAndBottom(body(document))).isEqualTo(LATER_TWIPS);
    }

    @Test
    void aCutWhosePageAboveLeavesNoRoomForTheParagraphEndingASectionIsMadeAndTheMarginsNamed() throws Exception {
        // The paragraph ending a section stands under the part on the page above, where a break before
        // it put it on the page after: where page 1's last row leaves room for the part's bottom border
        // and a point, and not for that paragraph too, Word would set it on a page of its own.
        double gap = gapLeavingRoomForTheCutAndNotTheSection();
        Exported exported = export(PAGE_HEIGHT, List.of(PageMarginRule.from(2, LATER)), page -> {
            page.addSpacer(spacer -> spacer.height(gap));
            card(30, true, null, null).accept(page);
        });
        XWPFDocument document = exported.document();

        List<XWPFTable> parts = parts(document);
        assertThat(parts).hasSizeGreaterThanOrEqualTo(2);
        XWPFParagraph first = between(document, parts.get(0));
        assertThat(first.isPageBreak()).as("cut where page 2 starts, its page broken before the paragraph").isTrue();
        assertThat(first.getCTP().getPPr().isSetSectPr()).isFalse();
        assertThat(marginNotes(exported.report())).extracting(DocxExportReport.Note::detail).first().asString()
                .isEqualTo("page 2's top and bottom margins differ from page 1's, which Word sets it with: page 1's rows "
                           + "reach too near its foot for the paragraph that would end a Word section between the parts "
                           + "the panel is cut into there, which would open a page of its own");
    }

    @Test
    void theRoomAboveACutIsReckonedAgainstTheBottomMarginWordSetsThatPageWith() throws Exception {
        // Page 2's own section is refused, so Word sets page 2 with page 1's 20pt foot rather than the
        // 4pt one the page sets it with: under that, its rows leave no room for page 3's section.
        List<PageMarginRule> margins = List.of(PageMarginRule.page(2, new DocumentInsets(20, 20, 4, 20)),
                PageMarginRule.from(3, LATER));
        double gap = gapLeavingRoomForTheCutAndNotTheSection(margins);
        Consumer<PageFlowBuilder> content = page -> {
            page.addSpacer(spacer -> spacer.height(gap));
            card(30, true, null, null).accept(page);
        };
        try (DocumentSession session = session(PAGE_HEIGHT, margins, content)) {
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            TableNode table = first(session.documentGraph().roots(), TableNode.class);
            double rowsEnd = layout.lowestRowOn(table, 1);
            assertThat(rowsEnd - 4).as("the premise: room enough above page 2's own foot").isGreaterThan(3.2);
            assertThat(rowsEnd - 20).as("the premise: and not above page 1's").isLessThan(3.2);
        }
        Exported exported = export(PAGE_HEIGHT, margins, content);

        assertThat(endingASection(exported.document())).isEmpty();
        assertThat(marginNotes(exported.report())).extracting(DocxExportReport.Note::detail)
                .anySatisfy(note -> assertThat(note).startsWith("page 3's top and bottom margins differ from page 1's, "
                                                                + "which Word sets it with: page 2's rows reach too near "
                                                                + "its foot"));
    }

    @Test
    void aSectionEndingAtABodyTablesSplitAndOneAtAPanelsCutAreEachWrittenInPlace() throws Exception {
        // Page 2 starts among a body table's rows, which is split there; page 3 inside the card.
        Exported exported = export(PAGE_HEIGHT, List.of(PageMarginRule.page(2, MIDDLE), PageMarginRule.from(3, LATER)),
                page -> {
                    page.addTable(table -> rows(table, 16, true)).addParagraph("Between");
                    card(30, true, null, null).accept(page);
                });
        XWPFDocument document = exported.document();

        List<XWPFTable> tables = document.getTables();
        List<XWPFTable> parts = parts(document);
        assertThat(tables.get(0).getNumberOfRows()).as("the premise: the body table split where page 2 starts")
                .isLessThan(1 + 16);
        List<XWPFParagraph> ending = endingASection(document);
        assertThat(ending).hasSize(2);
        assertThat(ending.get(0)).as("between the body table's parts").isSameAs(between(document, tables.get(0)));
        assertThat(ending.get(1)).as("between the card's first two parts").isSameAs(between(document, parts.get(0)));
        assertThat(topAndBottom(ending.get(0).getCTP().getPPr().getSectPr())).containsExactly(400L, 400L);
        assertThat(topAndBottom(ending.get(1).getCTP().getPPr().getSectPr())).containsExactly(700L, 880L);
        assertThat(topAndBottom(body(document))).isEqualTo(LATER_TWIPS);
    }

    @Test
    void aPanelInALaterSectionOfSeveralEndsAWordSectionAtItsCut() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (MultiSectionDocument sections = GraphCompose.documents()
                .section(session(page -> page.addParagraph("First section")))
                .section(session(PAGE_HEIGHT, List.of(PageMarginRule.from(2, LATER)), card(30, true, null, null)))
                .create()) {
            docx = sections.export(DocxSemanticBackend.builder().reportSink(report::set).build());
        }
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));

        XWPFParagraph first = between(document, parts(document).get(0));
        List<XWPFParagraph> ending = endingASection(document);
        assertThat(ending).as("the first section's end, then the card's first cut").hasSize(2).last().isSameAs(first);
        assertThat(topAndBottom(first.getCTP().getPPr().getSectPr())).containsExactly(400L, 400L);
        assertThat(topAndBottom(body(document))).isEqualTo(LATER_TWIPS);
        assertThat(marginNotes(report.get())).singleElement().asString()
                .contains("written as a Word section that starts where the panel is cut");
    }

    @Test
    void aCardCutNowhereLeavesThePagesItStartsInsideNamed() throws Exception {
        // An anchored card's bookmark opens in its table's header row: the table is split nowhere.
        Exported exported = export(PAGE_HEIGHT, List.of(PageMarginRule.from(2, LATER)), page -> page.addSection(card -> card
                .anchor("items").fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6))
                .addTable(table -> rows(table, 30, true))));

        assertThat(endingASection(exported.document())).isEmpty();
        assertThat(marginNotes(exported.report())).singleElement().asString()
                .endsWith("each starts inside this block, where the export starts no Word section");
    }

    @Test
    void aPageReferenceSettledAsItsNumberLetsItsPageBeCutWhereTheMarginsDoNotVary() throws Exception {
        // Its anchor marks a block of drawing only, which gets no bookmark: once the document is written
        // the field is taken back to its number, and the cut, decided then, sees no field in the rows.
        Exported exported = export(PAGE_HEIGHT, List.of(), cardReferringToADrawing());

        assertThat(exported.pages()).as("the premise: three pages").hasSize(3);
        assertThat(exported.pages().get(1)).as("the premise: row 15 on page 2").contains("Row 15");
        assertThat(parts(exported.document())).hasSize(3);
    }

    @Test
    void whereTheMarginsVaryTheCutsAreDecidedWhileTheirPageReferencesAreStillFields() throws Exception {
        // A section's margins are planned once it is written, before the references are settled: the
        // row holding one is a field then, and its page is left whole and named, as for any field.
        Exported exported = export(PAGE_HEIGHT, List.of(PageMarginRule.from(2, LATER)), cardReferringToADrawing());

        int page = 0;
        while (!exported.pages().get(page).contains("Row 15")) {
            page++;
        }
        assertThat(page).as("the premise: row 15 on a page the card runs onto").isPositive();
        assertThat(cutNote(exported.report())).contains("it is not cut where page " + (page + 1)
                                                         + " starts, at a row its table cannot be split at");
    }

    @Test
    void aPanelOfAnEarlierSectionWhoseMarginsDoNotVaryIsDecidedOnceTheDocumentIsWritten() throws Exception {
        // Only the panels of the section just written are decided with its margins: one of a section
        // before it, of one set of margins, waits for its page reference to be settled.
        byte[] docx;
        try (MultiSectionDocument sections = GraphCompose.documents()
                .section(session(PAGE_HEIGHT, List.of(), cardReferringToADrawing()))
                .section(session(PAGE_HEIGHT, List.of(PageMarginRule.from(2, LATER)), card(30, true, null, null)))
                .create()) {
            docx = sections.export(new DocxSemanticBackend());
        }
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));

        List<List<String>> rows = rowsOf(parts(document));
        assertThat(rows.stream().filter(part -> part.contains("Row 15") && part.contains("Row 1")))
                .as("the first card is cut where its page 2 starts, though row 15 held a field").isEmpty();
        assertThat(rows.stream().filter(part -> part.contains("Item")).count())
                .as("three parts of the first card, four of the second").isEqualTo(7);
    }

    @Test
    void aPanelOfASectionCountingItsOwnPagesIsDecidedOnceTheDocumentIsWritten() throws Exception {
        // Word would count a section of its own apart, so this one keeps one set of margins: its card
        // is cut where each page starts, row 15's page too once its reference is settled as a number.
        DocumentSession body = session(PAGE_HEIGHT, List.of(PageMarginRule.from(2, LATER)), cardReferringToADrawing());
        body.chrome().zone(com.demcha.compose.document.output.DocumentPageZone.footer(20, page ->
                new com.demcha.compose.document.dsl.RowBuilder().flexSpacer().add(page.pageNumber())
                        .add(page.pageTotal()).build()));
        byte[] docx;
        try (MultiSectionDocument sections = GraphCompose.documents()
                .section(session(page -> page.addParagraph("Cover"))).section(body).create()) {
            docx = sections.export(new DocxSemanticBackend());
        }
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));

        assertThat(endingASection(document)).as("the cover's section ends; the body's does not split").hasSize(1);
        assertThat(rowsOf(parts(document))).as("row 15 in a part of its own page's rows")
                .noneMatch(part -> part.contains("Row 15") && part.contains("Row 1"))
                .anyMatch(part -> part.contains("Row 15"));
    }

    /**
     * A card whose table's row 15 holds a page reference to the anchor of a block of drawing only,
     * which the export writes no bookmark for.
     */
    private static Consumer<PageFlowBuilder> cardReferringToADrawing() {
        return page -> page
                .addSection(drawn -> drawn.anchor("drawn").add(new com.demcha.compose.document.dsl.ShapeBuilder()
                        .size(120, 20).fillColor(DocumentColor.LIGHT_GRAY).build()))
                .addSection(card -> card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .padding(DocumentInsets.of(6))
                        .addTable(table -> {
                            table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100))
                                    .header("Item", "Amount").repeatHeader();
                            for (int row = 1; row <= 30; row++) {
                                table.rowCells(DocumentTableCell.text("Row " + row), row == 15
                                        ? DocumentTableCell.node(new PageReferenceNode("drawn",
                                                DocumentTextStyle.DEFAULT, TextAlign.LEFT))
                                        : DocumentTableCell.text(String.valueOf(row)));
                            }
                        }));
    }

    /**
     * A gap above a 30-row card, with page 2's margins {@link #LATER}, that leaves its table's last row
     * on page 1 room above the page's foot for the first part's 1pt bottom border and a point, and not
     * for the 0.1pt paragraph ending a section under it too, found by laying the page out.
     */
    private static double gapLeavingRoomForTheCutAndNotTheSection() {
        return gapLeavingRoomForTheCutAndNotTheSection(List.of(PageMarginRule.from(2, LATER)));
    }

    /** {@link #gapLeavingRoomForTheCutAndNotTheSection()}, the pages set against some margins. */
    private static double gapLeavingRoomForTheCutAndNotTheSection(List<PageMarginRule> margins) {
        double gap = 0;
        for (int tries = 0; tries < 200; tries++) {
            double height = gap;
            try (DocumentSession session = session(PAGE_HEIGHT, margins, page -> {
                page.addSpacer(spacer -> spacer.height(height));
                card(30, true, null, null).accept(page);
            })) {
                DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
                TableNode table = first(session.documentGraph().roots(), TableNode.class);
                double room = layout.lowestRowOn(table, 0) - layout.marginOf(0).bottom();
                if (room >= 2.0 && room < 2.1) {
                    return gap;
                }
                // Moved down by the gap, the rows keep their page while room is left: aim at 2.05.
                gap += room > 2.05 ? Math.min(room - 2.05, 4) : 0.5;
            }
        }
        throw new AssertionError("no gap leaves room for the cut and not the section");
    }

    @Test
    void aPanelWhoseTableFitsItsPageIsOneTable() throws Exception {
        Exported exported = export(card(8, true, null, null));

        assertThat(exported.pages()).hasSize(1);
        assertThat(parts(exported.document())).hasSize(1);
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains(NOTE));
    }

    @Test
    void aPanelTheLayoutBreaksBetweenParagraphsIsNotCut() throws Exception {
        // Only a table the layout breaks over pages is cut: blocks it moves or breaks otherwise
        // inside a panel are left to Word.
        Exported exported = export(page -> page.addSection(card -> {
            card.fillColor(SURFACE).padding(DocumentInsets.of(6));
            for (int line = 1; line <= 30; line++) {
                card.addParagraph("Line " + line);
            }
        }));

        assertThat(exported.cardPages()).as("the premise: the card runs onto a second page").isGreaterThan(1);
        assertThat(exported.document().getTables()).hasSize(1);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
    }

    @Test
    void aPanelWithAnotherBlockOverPagesBesideItsTableIsNotCut() throws Exception {
        // A paragraph the layout breaks over pages above the table: the table is not the only one.
        Exported exported = export(card(20, true, above -> above.addParagraph("Long " + "word ".repeat(400)), null));

        assertThat(exported.cardPages()).as("the premise: the card runs over pages").isGreaterThan(2);
        assertThat(parts(exported.document())).hasSize(1);
        assertThat(exported.report().notes()).noneMatch(note -> note.detail().contains(NOTE));
    }

    @Test
    void aPanelWithTwoTablesOverPagesIsNotCut() throws Exception {
        Exported exported = export(page -> page.addSection(card -> {
            card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6));
            card.addTable(table -> rows(table, 20, true));
            card.addTable(table -> rows(table, 20, true));
        }));

        assertThat(exported.cardPages()).as("the premise: the card runs over pages").isGreaterThan(2);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
    }

    @Test
    void aTableOverPagesOutsideAPanelIsNotCut() throws Exception {
        // In the body Word repeats a table's header rows itself.
        Exported exported = export(page -> page.addTable(table -> rows(table, 20, true)));

        assertThat(exported.pages()).as("the premise: the table runs onto a second page").hasSize(2);
        assertThat(exported.document().getTables()).hasSize(1);
        assertThat(exported.document().getParagraphs()).noneMatch(XWPFParagraph::isPageBreak);
    }

    /** A card with a 1pt border and 6pt of padding holding a table of {@code count} rows. */
    private static Consumer<PageFlowBuilder> card(int count, boolean header, Consumer<SectionBuilder> above,
                                                  Consumer<SectionBuilder> below) {
        return page -> page.addSection(card -> {
            card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(6));
            if (above != null) {
                above.accept(card);
            }
            card.addTable(table -> rows(table, count, header));
            if (below != null) {
                below.accept(card);
            }
        });
    }

    private static void rows(TableBuilder table, int count, boolean header) {
        table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
        if (header) {
            table.header("Item", "Amount").repeatHeader();
        }
        for (int row = 1; row <= count; row++) {
            table.row("Row " + row, String.valueOf(row));
        }
    }

    /**
     * An export, its report, the rows of its first table the layout sets on each page — header and
     * data rows by their first cell's text — and the pages its first section runs over.
     */
    private record Exported(XWPFDocument document, DocxExportReport report, List<List<String>> pages, int cardPages) {
    }

    private static DocumentSession session(Consumer<PageFlowBuilder> content) {
        return session(300, content);
    }

    private static DocumentSession session(double pageHeight, Consumer<PageFlowBuilder> content) {
        return session(pageHeight, List.of(), content);
    }

    private static DocumentSession session(double pageHeight, List<PageMarginRule> margins,
                                           Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document().pageSize(400, pageHeight).margin(DocumentInsets.of(20))
                .create();
        if (!margins.isEmpty()) {
            session.pageMargins(margins);
        }
        session.pageFlow(content::accept);
        return session;
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        return export(300, content);
    }

    private static Exported export(double pageHeight, Consumer<PageFlowBuilder> content) throws Exception {
        return export(pageHeight, List.of(), content);
    }

    private static Exported export(double pageHeight, List<PageMarginRule> margins, Consumer<PageFlowBuilder> content)
            throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = session(pageHeight, margins, content)) {
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            List<List<String>> pages = new ArrayList<>();
            TableNode table = first(session.documentGraph().roots(), TableNode.class);
            if (table != null) {
                boolean header = table.repeatedHeaderRowCount() > 0;
                for (int row = header ? 1 : 0; row < table.rows().size(); row++) {
                    int page = layout.rowPage(table, row);
                    while (pages.size() <= page) {
                        pages.add(new ArrayList<>(header ? List.of("Item") : List.of()));
                    }
                    pages.get(page).add("Row " + (header ? row : row + 1));
                }
            }
            SectionNode card = first(session.documentGraph().roots(), SectionNode.class);
            int cardPages = card == null || layout.placement(card) == null ? 0
                    : layout.placement(card).endPage() - layout.placement(card).startPage() + 1;
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), pages, cardPages);
        }
    }

    private static <T extends DocumentNode> T first(List<DocumentNode> nodes, Class<T> kind) {
        for (DocumentNode node : nodes) {
            if (kind.isInstance(node)) {
                return kind.cast(node);
            }
            T inside = first(node.children(), kind);
            if (inside != null) {
                return inside;
            }
        }
        return null;
    }

    private static String cutNote(DocxExportReport report) {
        return report.notes().stream().filter(note -> note.detail().contains(NOTE)).findFirst().orElseThrow().detail();
    }

    private static List<DocxExportReport.Note> marginNotes(DocxExportReport report) {
        return report.notes().stream().filter(note -> note.subject().equals(DocxPageMarginSections.PAGE_MARGIN))
                .toList();
    }

    /** The paragraph right after a part of the panel, between it and the next. */
    private static XWPFParagraph between(XWPFDocument document, XWPFTable part) {
        List<IBodyElement> body = document.getBodyElements();
        return (XWPFParagraph) body.get(body.indexOf(part) + 1);
    }

    private static List<XWPFParagraph> endingASection(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSectPr())
                .toList();
    }

    private static CTSectPr body(XWPFDocument document) {
        return document.getDocument().getBody().getSectPr();
    }

    /** A section's top and bottom margins, in twips. */
    private static List<Long> topAndBottom(CTSectPr section) {
        return List.of(((java.math.BigInteger) section.getPgMar().getTop()).longValue(),
                ((java.math.BigInteger) section.getPgMar().getBottom()).longValue());
    }

    /** The panel's tables in the body, those that hold a table in their one cell. */
    private static List<XWPFTable> parts(XWPFDocument document) {
        return document.getTables().stream()
                .filter(table -> table.getNumberOfRows() == 1 && !table.getRow(0).getCell(0).getTables().isEmpty())
                .toList();
    }

    /** Each part's table's rows, by their first cell's text. */
    private static List<List<String>> rowsOf(List<XWPFTable> parts) {
        List<List<String>> rows = new ArrayList<>();
        for (XWPFTable part : parts) {
            List<String> texts = new ArrayList<>();
            for (XWPFTableRow row : part.getRow(0).getCell(0).getTables().get(0).getRows()) {
                texts.add(row.getCell(0).getText());
            }
            rows.add(texts);
        }
        return rows;
    }

    private static List<String> texts(XWPFTableCell cell) {
        return cell.getParagraphs().stream().map(XWPFParagraph::getText).toList();
    }

    private static long margin(XWPFTable part, boolean top) {
        CTTcMar margins = part.getRow(0).getCell(0).getCTTc().getTcPr().getTcMar();
        if (margins == null || !(top ? margins.isSetTop() : margins.isSetBottom())) {
            return 0;
        }
        return DocxTwips.of((top ? margins.getTop() : margins.getBottom()).getW());
    }
}
