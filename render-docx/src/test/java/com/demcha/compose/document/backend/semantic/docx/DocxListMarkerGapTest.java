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
                        .isEqualTo(leftIndent(paragraph) + Math.round((DOT + GAP) * 20));
                assertThat(leftIndent(paragraph)).as("the item starts at the column's indent").isEqualTo(600);
            }
        }
    }

    @Test
    void aMarkerOfTextKeepsItsSpace() throws Exception {
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
                    .isEqualTo(leftIndent(paragraph) + Math.round((DOT + GAP) * 20));
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

    private static long leftIndent(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetInd() || !properties.getInd().isSetLeft()
                ? 0 : DocxTwips.of(properties.getInd().getLeft());
    }
}
