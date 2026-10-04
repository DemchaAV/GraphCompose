package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A list item whose marker is a picture alone starts its text where the page does: the marker's
 * width and {@code markerGap} past where the item starts.
 *
 * <p>The marker was followed by a space, a little over two points where the page leaves the
 * gap asked for: {@code TealPulse}'s skills and highlights, a 3.2pt dot and a 9.2pt gap, stood
 * 6.3 to 6.7pt left of the page's in both editors. Word draws a picture at the size it is written, so
 * a tab to a stop there puts the text in place — where the picture, its edges included, clears
 * the stop; a marker of text, which Word sets in its own widths, keeps its space.</p>
 */
class DocxListMarkerGapTest {

    private static final double DOT = 3.4;
    private static final double GAP = 9.56;

    @Test
    void aDrawnMarkersItemStandsTheMarkerAndItsGapPastTheItemsStart() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addSection("Column", column -> column.padding(new DocumentInsets(0, 0, 0, 30))
                        .addList(list -> list.name("Skills")
                                .marker(m -> m.dot(DOT, DocumentColor.rgb(0, 128, 128)))
                                .markerGap(GAP).hangingIndent(true).normalizeMarkers(false)
                                .addItem("Patient Assessment").addItem("Care Planning"))))) {
            for (String item : new String[] {"Patient Assessment", "Care Planning"}) {
                XWPFParagraph paragraph = item(document, item);

                assertThat(paragraph.getText()).isEqualTo("\t" + item);
                assertThat(tabStop(paragraph)).as("past the column's 30pt indent, the dot and its gap")
                        .isEqualTo(firstLineStart(paragraph) + Math.round((DOT + GAP) * 20));
                assertThat(firstLineStart(paragraph)).as("the item starts at the column's indent").isEqualTo(600);
                assertThat(leftIndent(paragraph)).as("and its lines hang at its text").isEqualTo(tabStop(paragraph));
            }
        }
    }

    @Test
    void aWordListOfATextMarkerTakesTheColumnTheLayoutSetsIt() throws Exception {
        // MidnightNavy's and Panel's bullets: the level's marker column was a stated 9pt.
        try (XWPFDocument wide = DocxExports.withLayout(400, 300, 20, page -> page
                     .addList(list -> list.marker("•").markerGap(GAP).hangingIndent(true).items("Alpha", "Beta")));
             XWPFDocument stated = DocxExports.withLayout(400, 300, 20, page -> page
                     .addList(list -> list.marker("•").markerGap(GAP).items("Alpha", "Beta")))) {
            long[] column = levelZero(wide);

            assertThat(column[0]).as("the column, as wide as it hangs").isEqualTo(column[1]);
            assertThat(column[0]).as("the bullet's width and the gap").isGreaterThan(Math.round(GAP * 20));
            long[] prefix = levelZero(stated);
            assertThat(prefix[0]).as("a list without the flag takes the page's prefix of spaces")
                    .isEqualTo(prefix[1]).isNotEqualTo(column[0]).isNotEqualTo(180);
            assertThat(DocxListLevels.markerFollowedByASpace(stated)).as("its marker a space ahead").isTrue();
            assertThat(DocxListLevels.markerFollowedByASpace(wide)).as("a column's marker a tab ahead").isFalse();
        }
    }

    @Test
    void aWordListInAPaddedColumnTakesItsInsetAndTheColumn() throws Exception {
        // The column's inset written on the paragraph replaces the level's indent, so the
        // level's column is added back on top of it.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addSection("Column", column -> column.padding(new DocumentInsets(0, 0, 0, 30))
                        .addList(list -> list.marker("•").markerGap(GAP).hangingIndent(true).items("Alpha"))))) {
            long[] column = levelZero(document);
            XWPFParagraph paragraph = item(document, "Alpha");

            assertThat(leftIndent(paragraph)).isEqualTo(600 + column[0]);
            assertThat(firstLineStart(paragraph)).as("its marker at the column's inset").isEqualTo(600);
        }
    }

    @Test
    void aTextMarkersRichItemWithTooLittleGapKeepsItsSpace() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addList(list -> list.marker("•").markerGap(0).hangingIndent(true)
                        .addItem(rich -> rich.bold("Label:").plain(" description"))))) {
            XWPFParagraph paragraph = item(document, "description");

            assertThat(paragraph.getText()).isEqualTo("• Label: description");
            assertThat(paragraph.getCTP().getPPr().isSetTabs()).isFalse();
        }
    }

    @Test
    void aWordListSetSmallerThanTheDocumentDrawsItsMarkerInItsOwnSize() throws Exception {
        // Word draws a level's marker in the item's mark, which an exact line leaves the
        // document's 11pt: an 8pt list's bullet would come out wider than the one measured.
        var body = com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(11);
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addParagraph(p -> p.text("The body of the page, set in the document's own size.").textStyle(body))
                .addParagraph(p -> p.text("And more of it, so that eleven points is its size.").textStyle(body))
                .addList(list -> list.marker("•").markerGap(1).hangingIndent(true)
                        .textStyle(body.withSize(8)).items("Alpha", "Beta")))) {
            var level = document.getNumbering().getAbstractNum(java.math.BigInteger.ZERO).getAbstractNum()
                    .getLvlArray(0);

            assertThat(levelZero(document)[0]).as("the column is measured").isNotEqualTo(180);
            assertThat(level.isSetRPr()).as("the level names the list's style").isTrue();
            assertThat(level.getRPr().getSzArray(0).getVal()).isEqualTo(java.math.BigInteger.valueOf(16));
        }
    }

    @Test
    void aTextMarkerWithTooLittleGapKeepsTheStatedColumn() throws Exception {
        // Word may set the bullet a few hundredths wider; with no gap to cover that, a column
        // at the page's would leave it past its tab.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addList(list -> list.marker("•").markerGap(0).hangingIndent(true).items("Alpha")))) {
            assertThat(levelZero(document)).containsExactly(180, 180);
        }
    }

    @Test
    void aTextMarkersRichItemHangsAtTheColumnTheLayoutSetsIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addList(list -> list.marker("•").markerGap(GAP).hangingIndent(true)
                        .addItem(rich -> rich.bold("Label:").plain(" a description long enough to run onto a "
                                + "second line in the column it is set in, so its lines hang"))))) {
            XWPFParagraph paragraph = item(document, "lines hang");

            assertThat(paragraph.getText()).startsWith("•\tLabel:");
            assertThat(firstLineStart(paragraph)).isZero();
            assertThat(leftIndent(paragraph)).as("every line after the first at the text").isEqualTo(tabStop(paragraph))
                    .isGreaterThan(Math.round(GAP * 20));
        }
    }

    @Test
    void aRichMarkerOfTextKeepsItsSpace() throws Exception {
        // Word sets a text marker in its own widths: a stop past it could fall short of it.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addList(list -> list.marker(m -> m.color("•", DocumentColor.rgb(0, 128, 128)))
                        .markerGap(GAP).hangingIndent(true).addItem("Patient Assessment")))) {
            XWPFParagraph paragraph = item(document, "Patient Assessment");

            assertThat(paragraph.getText()).isEqualTo("• Patient Assessment");
            assertThat(paragraph.getCTP().getPPr() == null || !paragraph.getCTP().getPPr().isSetTabs()).isTrue();
        }
    }

    @Test
    void aDrawnMarkerWithNoGapToClearKeepsItsSpace() throws Exception {
        // Its picture, edges and all, reaches past a stop at the dot's width: a tab there would
        // run on to Word's next default stop, half an inch on.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addList(list -> list.marker(m -> m.dot(4, DocumentColor.rgb(0, 128, 128)))
                        .markerGap(0).hangingIndent(true).addItem("Patient Assessment")))) {
            XWPFParagraph paragraph = item(document, "Patient Assessment");

            assertThat(paragraph.getText()).isEqualTo(" Patient Assessment");
        }
    }

    @Test
    void aDrawnMarkerWithABlankRunBesideItKeepsItsSpace() throws Exception {
        // The layout drops the blank run and Word sets it: a stop measured past the dot alone
        // could fall inside the space, and the tab run on half an inch.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addList(list -> list.marker(m -> m.dot(4, DocumentColor.rgb(0, 128, 128)).plain(" "))
                        .textStyle(com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(14))
                        .hangingIndent(true).addItem("Patient Assessment")))) {
            XWPFParagraph paragraph = item(document, "Patient Assessment");

            assertThat(paragraph.getText()).doesNotContain("\t");
        }
    }

    @Test
    void aTopLevelItemOfAListWithRichItemsStandsWhereThePageSetsItToo() throws Exception {
        // Rich items make the list a tree, written item by item; a top-level one carrying the
        // list's marker is placed as a flat one is.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addRow("Columns", row -> row
                        .addSection("Side", side -> side.addList(list -> list
                                .marker(m -> m.dot(DOT, DocumentColor.rgb(0, 128, 128)))
                                .markerGap(GAP).hangingIndent(true)
                                .addItem(rich -> rich.bold("Label:").plain(" description"))))
                        .addSection("Main", main -> main.addParagraph("Main"))))) {
            XWPFParagraph paragraph = item(document, "Label: description");

            assertThat(paragraph.getText()).startsWith("\t");
            assertThat(tabStop(paragraph)).as("in a cell, from the cell's text edge")
                    .isEqualTo(firstLineStart(paragraph) + Math.round((DOT + GAP) * 20));
        }
    }

    @Test
    void aListThatNestsKeepsItsSpaceAtTheTopLevelToo() throws Exception {
        // Its nested levels are not measured: a top level at the page's column could stand
        // right of the text of the items nested under it.
        try (XWPFDocument drawn = DocxExports.withLayout(400, 300, 20, page -> page
                     .addList(list -> list.marker(m -> m.dot(DOT, DocumentColor.rgb(0, 128, 128)))
                             .markerGap(GAP).hangingIndent(true)
                             .addItem("Parent", parent -> parent.addItem("Child"))));
             XWPFDocument text = DocxExports.withLayout(400, 300, 20, page -> page
                     .addList(list -> list.marker("•").markerGap(GAP).hangingIndent(true)
                             .addItem(rich -> rich.bold("Label:").plain(" parent"),
                                     parent -> parent.addItem("Child"))))) {
            for (XWPFParagraph parent : java.util.List.of(item(drawn, "Parent"), item(text, "parent"))) {
                assertThat(parent.getText()).doesNotContain("\t");
                assertThat(parent.getCTP().getPPr() == null || !parent.getCTP().getPPr().isSetTabs()).isTrue();
            }
            assertThat(item(text, "parent").getText()).isEqualTo("• Label: parent");
        }
    }

    @Test
    void aNestedItemsDrawnMarkerKeepsItsSpace() throws Exception {
        // A nested item stands after its depth's indent, which the list's measure of its first
        // item's marker does not hold.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addList(list -> list.marker(m -> m.dot(DOT, DocumentColor.rgb(0, 128, 128)))
                        .markerFor(1, drawnDot())
                        .markerGap(GAP).hangingIndent(true)
                        .addItem("Parent", parent -> parent.addItem("Child"))))) {
            XWPFParagraph child = item(document, "Child");

            assertThat(child.getRuns()).as("the child's marker is drawn too")
                    .anyMatch(run -> !run.getCTR().getDrawingList().isEmpty());
            assertThat(child.getText()).doesNotContain("\t");
        }
    }

    /** A dot marker, as a list built with one carries it. */
    private static com.demcha.compose.document.node.ListMarker drawnDot() {
        return new com.demcha.compose.document.dsl.ListBuilder()
                .marker(m -> m.dot(DOT, DocumentColor.rgb(0, 128, 128))).addItem("x").build().marker();
    }

    private static XWPFParagraph item(XWPFDocument document, String text) {
        java.util.List<XWPFParagraph> paragraphs = new java.util.ArrayList<>(document.getParagraphs());
        document.getTables().forEach(table -> table.getRows().forEach(row -> row.getTableCells()
                .forEach(cell -> paragraphs.addAll(cell.getParagraphs()))));
        return paragraphs.stream().filter(p -> p.getText().endsWith(text)).findFirst().orElseThrow();
    }

    private static long tabStop(XWPFParagraph paragraph) {
        var stops = paragraph.getCTP().getPPr().getTabs().getTabArray();
        assertThat(stops).hasSize(1);
        return DocxTwips.of(stops[0].getPos());
    }

    /** The left and hanging indents of a document's first list definition's top level. */
    private static long[] levelZero(XWPFDocument document) {
        var indent = document.getNumbering().getAbstractNum(java.math.BigInteger.ZERO).getAbstractNum()
                .getLvlArray(0).getPPr().getInd();
        return new long[] {DocxTwips.of(indent.getLeft()), DocxTwips.of(indent.getHanging())};
    }

    /** Where an item's first line starts: its left indent less what it hangs by. */
    private static long firstLineStart(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        long hanging = properties == null || !properties.isSetInd() || !properties.getInd().isSetHanging()
                ? 0 : DocxTwips.of(properties.getInd().getHanging());
        return leftIndent(paragraph) - hanging;
    }

    private static long leftIndent(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetInd() || !properties.getInd().isSetLeft()
                ? 0 : DocxTwips.of(properties.getInd().getLeft());
    }
}
