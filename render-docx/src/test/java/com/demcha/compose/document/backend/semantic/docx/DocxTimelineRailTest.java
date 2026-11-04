package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.shape.StrokeColor;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A timeline's rail in Word: one bar per page, the rail's width, where the layout draws it, held by
 * the entry it starts in on that page.
 *
 * <p>The layout draws the rail after everything else is placed, one stroke per page, as the left
 * side of a box no node owns. The export drew it as a line of no width, which LibreOffice 24.2 turned
 * a quarter round its middle at some places on the page — three of the ten rails of the fidelity
 * corpus came out as horizontal strokes — and anchored it in the paragraph nearest its top, which
 * under a section heading was the heading. It is now a filled bar, which both editors draw where the
 * page does, anchored in its own entry.</p>
 *
 * <p>What the rail still does not do is named rather than pinned here: it keeps the length the layout
 * gives it, so an entry that grows in the editor pushes the entries after it past its end.</p>
 */
class DocxTimelineRailTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(0xC0, 0x30, 0x30);
    private static final DocumentColor DOT = DocumentColor.rgb(0x1A, 0x56, 0x94);
    private static final String RAIL_HEX = "C03030";

    @Test
    void aLeftSideIsABarTheStrokesWidthCentredOnThatSide() {
        PlacedFragment rail = PlacedFragment.withZeroInsets("@timeline-rail", 0, 1, 52.7, 100, 2, 120,
                new ShapeFragmentPayload(null, null, null, null, null,
                        new SideBorders(null, null, null, new Stroke(new StrokeColor(Color.RED), 2)), null));

        List<DocxDrawings.Shape> shapes = DocxDrawings.of(rail, 400);

        assertThat(shapes).singleElement().satisfies(shape -> {
            assertThat(shape.kind()).isEqualTo(DocxDrawings.Kind.RECT);
            assertThat(shape.x()).isCloseTo(51.7, within(1e-9));
            assertThat(shape.top()).isCloseTo(180, within(1e-9));
            assertThat(shape.width()).isEqualTo(2);
            assertThat(shape.height()).isEqualTo(120);
            assertThat(shape.fill()).isEqualTo(Color.RED);
            assertThat(shape.stroke()).as("filled, not outlined").isNull();
            assertThat(shape.page()).isEqualTo(1);
        });
    }

    @Test
    void aTopSideIsABarAcrossCentredOnTheTop() {
        PlacedFragment box = PlacedFragment.withZeroInsets("Root[0]", 0, 0, 40, 100, 90, 30,
                new ShapeFragmentPayload(null, null, null, null, null,
                        new SideBorders(new Stroke(new StrokeColor(Color.BLUE), 3), null, null, null), null));

        assertThat(DocxDrawings.of(box, 400)).singleElement().satisfies(shape -> {
            assertThat(shape.kind()).isEqualTo(DocxDrawings.Kind.RECT);
            assertThat(shape.x()).isEqualTo(40);
            assertThat(shape.top()).isCloseTo(400 - 130 - 1.5, within(1e-9));
            assertThat(shape.width()).isEqualTo(90);
            assertThat(shape.height()).isEqualTo(3);
        });
    }

    @Test
    void aSideOfNoWidthDrawsNothing() {
        PlacedFragment box = PlacedFragment.withZeroInsets("Root[0]", 0, 0, 40, 100, 90, 30,
                new ShapeFragmentPayload(null, null, null, null, null,
                        new SideBorders(null, null, null, new Stroke(new StrokeColor(Color.BLUE), 0)), null));

        assertThat(DocxDrawings.of(box, 400)).isEmpty();
    }

    @Test
    void besideTheMarkersEachPagesRailIsOneBarInTheEntryItStartsIn() throws Exception {
        assertRails(false);
    }

    @Test
    void throughTheMarkersEachPagesRailIsOneBarInTheEntryItStartsIn() throws Exception {
        assertRails(true);
    }

    /**
     * Seven entries on a page that holds five: the rail is drawn on two pages, each page's part a
     * filled bar of the layout's length and the rail's width, never a line, and each anchored in a
     * paragraph of the first entry the layout starts on that page, not in the heading above the
     * timeline, which is the paragraph nearest the first part's top.
     */
    private static void assertRails(boolean onTheRail) throws Exception {
        try (DocumentSession session = sevenEntries(onTheRail)) {
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            List<PlacedFragment> rails = layout.passFragments();
            assertThat(rails).extracting(PlacedFragment::pageIndex).as("a part on each page").containsExactly(0, 1);
            for (PlacedFragment rail : rails) {
                String block = layout.anchoredBlockAt(rail);
                assertThat(block).as("an anchored block holds page %d's rail", rail.pageIndex()).isNotNull();
                assertThat(layout.holdersOf(block)).as("a node of the tree, with its holders").isNotEmpty();
            }

            byte[] docx = session.export(new DocxSemanticBackend());
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                String xml = document.getDocument().xmlText();
                assertThat(xml).as("no line of the rail's colour").doesNotContainPattern(
                        "(?s)prst=\"line\"(?:(?!</wp:anchor>).)*" + RAIL_HEX);

                List<String> bars = anchorsOfColour(xml);
                assertThat(bars).hasSameSizeAs(rails);
                for (int index = 0; index < rails.size(); index++) {
                    PlacedFragment rail = rails.get(index);
                    assertThat(bars.get(index))
                            .contains("prst=\"rect\"")
                            .contains("<wp:extent cx=\"" + Units.toEMU(1.5) + "\" cy=\""
                                      + Units.toEMU(rail.height()) + "\"/>")
                            .contains("<a:solidFill><a:srgbClr val=\"" + RAIL_HEX + "\"/></a:solidFill>");
                }

                List<String> elements = bodyTexts(document);
                assertThat(carrierOf(document, 0)).as("page 1's rail, in the first entry")
                        .isEqualTo(entryOf(elements, "Role 1"));
                assertThat(carrierOf(document, 1)).as("page 2's rail, in the first entry the layout starts there")
                        .isEqualTo(entryOf(elements, "Role 6"));
            }
        }
    }

    @Test
    void everyPageOfARailIsReckonedUnderTheOneNoteThatNamesIt() {
        // Each page's part is held by a block of its own, but the report names the rail once, under
        // the pass's path: a part nothing carries has to be counted against that note, or it would
        // be named dropped under an entry's path beside a note saying it is drawn.
        DocxDrawingAnchors.Owner first = new DocxDrawingAnchors.Owner("timeline rail", "@timeline-rail", false,
                "Root[0]/Timeline[1]/Entry[0]");
        DocxDrawingAnchors.Owner second = new DocxDrawingAnchors.Owner("timeline rail", "@timeline-rail", false,
                "Root[0]/Timeline[1]/Entry[5]");

        assertThat(first.noted()).isEqualTo(second.noted())
                .isEqualTo(new DocxDrawingAnchors.Owner("timeline rail", "@timeline-rail"));
        assertThat(first.noted().heldBy()).isEqualTo("@timeline-rail");
    }

    @Test
    void aTimelineInAnotherOnesEntryHasItsRailInItsOwnFirstEntry() throws Exception {
        // Two anchored blocks hold the inner rail's top-left corner, the outer entry and the inner
        // timeline's first entry, and the inner one is taken. The outer entry's paragraphs include
        // the inner timeline's, and the one level with the rail's top is taken from either, so this
        // holds the outcome rather than which of the two blocks is asked.
        DocumentColor inner = DocumentColor.rgb(0x30, 0xA0, 0x30);
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report =
                new java.util.concurrent.atomic.AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(outer -> outer
                    .connector(RAIL, 1.5)
                    .entry(TimelineMarker.dot(8, DOT), e -> e.title("Outer entry").add(below -> below
                            .addTimeline(nested -> nested
                                    .connector(inner, 1.0)
                                    .entry(TimelineMarker.dot(6, DOT), n -> n.title("Inner 1").body("First."))
                                    .entry(TimelineMarker.dot(6, DOT), n -> n.title("Inner 2").body("Second.")))))
                    .entry(TimelineMarker.dot(8, DOT), e -> e.title("Outer 2"))));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        assertThat(report.get().bySubject()).as("laid out, so the rails are drawn at all")
                .containsKey("timeline rail");

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFTable holding = innermostTableHolding(document.getTables(), "30A030");
            assertThat(holding).as("a table of the inner timeline carries its rail").isNotNull();
            assertThat(holding.getText()).as("the inner timeline's first entry, not the outer entry's text")
                    .contains("Inner 1").doesNotContain("Outer entry");
        }
    }

    @Test
    void aTimelineRightUnderAnotherHasItsRailInItsOwnFirstEntry() throws Exception {
        // Nothing between the two timelines: the second rail starts at the foot of the first
        // timeline's last entry, which is shorter than the second's first. A block whose foot the
        // rail starts at does not hold it; counted as holding it, the smaller one took the rail.
        DocumentColor second = DocumentColor.rgb(0x30, 0x30, 0xC0);
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report =
                new java.util.concurrent.atomic.AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(30))
                .create()) {
            session.pageFlow(flow -> flow.spacing(0)
                    .addTimeline(t -> t.connector(RAIL, 1.5)
                            .entry(TimelineMarker.dot(8, DOT), e -> e.title("Role A1").body("First."))
                            .entry(TimelineMarker.dot(8, DOT), e -> e.title("Role A2")))
                    .addTimeline(t -> t.connector(second, 1.5)
                            .entry(TimelineMarker.dot(8, DOT), e -> e.title("Role B1")
                                    .body("Built the export pipeline and its tests, then carried it through "
                                          + "three releases with every document re-measured line by line."))
                            .entry(TimelineMarker.dot(8, DOT), e -> e.title("Role B2"))));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        assertThat(report.get().bySubject()).as("laid out, so the rails are drawn at all")
                .containsKey("timeline rail");

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFTable holding = innermostTableHolding(document.getTables(), "3030C0");
            assertThat(holding).as("a table carries the second rail").isNotNull();
            assertThat(holding.getText()).as("the second timeline's first entry, not the first one's last")
                    .contains("Role B1").doesNotContain("Role A2");
        }
    }

    /** The innermost table, nested ones searched first, whose markup holds a bar of the colour. */
    private static XWPFTable innermostTableHolding(List<XWPFTable> tables, String colour) {
        for (XWPFTable table : tables) {
            if (!table.getCTTbl().xmlText().contains("<a:srgbClr val=\"" + colour + "\"")) {
                continue;
            }
            for (var row : table.getRows()) {
                for (var cell : row.getTableCells()) {
                    XWPFTable nested = innermostTableHolding(cell.getTables(), colour);
                    if (nested != null) {
                        return nested;
                    }
                }
            }
            return table;
        }
        return null;
    }

    /** Seven entries on a page that holds five, under a heading and above a closing paragraph. */
    private static DocumentSession sevenEntries(boolean onTheRail) {
        DocumentSession session = GraphCompose.document().pageSize(400, 300).margin(DocumentInsets.of(30)).create();
        session.pageFlow(flow -> {
            flow.addParagraph("Experience");
            flow.addTimeline(t -> {
                if (onTheRail) {
                    t.markerOnRail();
                }
                t.connector(RAIL, 1.5);
                for (int index = 1; index <= 7; index++) {
                    int n = index;
                    t.entry(TimelineMarker.dot(8, DOT), e -> e
                            .title("Role " + n).meta("20" + (10 + n) + " - 20" + (11 + n))
                            .body("Built the export pipeline and its tests."));
                }
            });
            flow.addParagraph("After the timeline.");
        });
        return session;
    }

    /** Every anchored drawing of the rail's colour, in the order the document holds them. */
    private static List<String> anchorsOfColour(String xml) {
        List<String> found = new ArrayList<>();
        for (String anchor : xml.split("<wp:anchor")) {
            String body = anchor.contains("</wp:anchor>") ? anchor.substring(0, anchor.indexOf("</wp:anchor>")) : "";
            if (body.contains(RAIL_HEX)) {
                found.add(body);
            }
        }
        return found;
    }

    /** The text of each element of the body, a table's cells joined. */
    private static List<String> bodyTexts(XWPFDocument document) {
        List<String> texts = new ArrayList<>();
        for (IBodyElement element : document.getBodyElements()) {
            texts.add(element instanceof XWPFParagraph paragraph ? paragraph.getText()
                    : element instanceof XWPFTable table ? table.getText() : "");
        }
        return texts;
    }

    /**
     * The entry an element of the body belongs to: the number of the last title at or before it, or
     * 0 before the first.
     */
    private static int entryOf(List<String> elements, String title) {
        int entry = 0;
        for (String text : elements) {
            if (text.contains("Role ")) {
                entry++;
            }
            if (text.contains(title)) {
                return entry;
            }
        }
        throw new AssertionError("no element holds " + title);
    }

    /** The entry, counted as {@link #entryOf} counts, of the element carrying the rail's n-th bar. */
    private static int carrierOf(XWPFDocument document, int n) {
        int entry = 0;
        int seen = 0;
        for (IBodyElement element : document.getBodyElements()) {
            String text = element instanceof XWPFParagraph paragraph ? paragraph.getText()
                    : element instanceof XWPFTable table ? table.getText() : "";
            if (text.contains("Role ")) {
                entry++;
            }
            String xml = element instanceof XWPFParagraph paragraph ? paragraph.getCTP().xmlText()
                    : element instanceof XWPFTable table ? table.getCTTbl().xmlText() : "";
            seen += anchorsOfColour(xml).size();
            if (seen > n) {
                return entry;
            }
        }
        throw new AssertionError("no element carries bar " + n);
    }
}
