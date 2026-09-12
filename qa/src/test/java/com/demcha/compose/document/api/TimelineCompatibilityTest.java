package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TimelineBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailEnd;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAnchor;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

/**
 * What has to stay true for code written before the rail moved.
 *
 * <p>The rail used to be a left border on every entry section and is now one line
 * contributed after layout. That is invisible to a caller only if the geometry is
 * identical, the page count is identical, and the places a timeline could already be put
 * still take one — so those are the claims here, rather than anything about the new API.</p>
 *
 * <p>The measurement behind them was a cross-branch one and cannot live in a test: the same
 * eleven documents, written against the pre-rework builder alone, laid out on this branch and
 * on the base, and the two graphs diffed. Every placed node matched to the digit; the 31
 * per-entry borders became 16 rail fragments covering the same span on the same x, worst
 * |Δx| 0 and worst |Δy| 1.4e-14 over sixteen page-instances. What remains here is the part
 * that can be re-checked on one branch: that those invariants still hold.</p>
 */
class TimelineCompatibilityTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    // --- the pre-rework builder ------------------------------------------------

    @Test
    void everyMethodTheOldBuilderHadStillCompilesAndStillLaysOut() {
        // A source-compatibility test, and the assertion is that it compiles: every method
        // TimelineBuilder and TimelineEntryBuilder published before the rework, called in
        // one expression. Adding an overload is how a source break usually arrives — the
        // call that used to resolve stops resolving — so both entry(...) spellings are here
        // together, the marker-first one and the one that names its marker inside.
        LayoutGraph graph = timeline(340, 340, t -> t
                .connector(RAIL, 2.0)
                .gutter(10)
                .markerGap(8)
                .markerColumnWeight(0.18)
                .spacing(14)
                .titleStyle(DocumentTextStyle.builder().size(12).build())
                .metaStyle(DocumentTextStyle.builder().size(7).build())
                .bodyStyle(DocumentTextStyle.builder().size(9).build())
                .keepTogether()
                .keepEntriesTogether()
                .entry(TimelineMarker.dot(8, INK), e -> e
                        .title("Title").title("Title", DocumentTextStyle.builder().size(11).build())
                        .titleStyle(DocumentTextStyle.builder().size(11).build())
                        .meta("Meta").meta("Meta", DocumentTextStyle.builder().size(7).build())
                        .metaStyle(DocumentTextStyle.builder().size(7).build())
                        .body("Body").body("Body", DocumentTextStyle.builder().size(9).build())
                        .bodyStyle(DocumentTextStyle.builder().size(9).build())
                        .add(extra -> extra.addParagraph("Added")))
                .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE),
                        e -> e.title("Second"))
                .entry(TimelineMarker.circle(12, DocumentColor.WHITE, DocumentStroke.of(INK, 1.0)),
                        e -> e.title("Third"))
                .entry(TimelineMarker.square(10, INK), e -> e.title("Fourth")));

        assertThat(rails(graph)).as("and the rail such a timeline draws is still one line").hasSize(1);
    }

    @Test
    void aTimelineThatAsksForNothingNewKeepsTheMarkersWhereTheyWere() {
        // The default, stated as the thing an old document depends on: markers packed to
        // the left of their column and the rail one gutter further left. Every new choice
        // is opt-in, so a builder that names none of them must land here.
        LayoutGraph graph = timeline(340, 300, t -> t.gutter(9)
                .entry(TimelineMarker.dot(6, INK), e -> e.title("Small").body("Body."))
                .entry(TimelineMarker.square(22, INK), e -> e.title("Large").body("Body.")));

        List<ResolvedLayoutAnchor> markers = markers(graph);
        assertThat(markers.get(0).x())
                .as("a 6pt and a 22pt marker still share a left edge, not a centre")
                .isEqualTo(markers.get(1).x(), within(1e-9));
        assertThat(rails(graph).get(0).x())
                .as("and the rail is that edge less the gutter")
                .isEqualTo(markers.get(0).x() - 9.0, within(1e-9));
    }

    // --- pagination -------------------------------------------------------------

    @Test
    void thePagesALegacyTimelineNeedsAreThePagesItTakes() {
        // Page counts, pinned. The rail is contributed after layout and may not add or
        // remove a page; these three shapes are the ones where it would show — a body that
        // runs off the page, an entry taller than a page, and a timeline that starts near
        // the bottom of one.
        assertThat(pagesOf(paginatedScene())).as("body across a break").isEqualTo(2);
        assertThat(pagesOf(veryTallScene())).as("an entry taller than four pages").isEqualTo(5);
        assertThat(pagesOf(nearBottomScene())).as("started low, still one page").isEqualTo(1);
    }

    @Test
    void everyRailFragmentBelongsToOnePageAndStaysInsideIt() {
        // No duplicates, none missing, none reaching into a margin. Checked on the shapes
        // that cross boundaries, where a rail assembled from anchors could be handed a box
        // resolved on a different page.
        for (Map.Entry<String, LayoutGraph> scene : Map.of(
                "paginated", paginatedScene(),
                "very tall", veryTallScene(),
                "near bottom", nearBottomScene()).entrySet()) {
            LayoutGraph graph = scene.getValue();
            List<PlacedFragment> rails = rails(graph);
            List<Integer> occupied = entries(graph).stream()
                    .map(ResolvedLayoutAnchor::pageIndex).distinct().sorted().toList();

            assertThat(rails.stream().map(PlacedFragment::pageIndex).sorted())
                    .as("%s: one rail on each page the entries occupy, and on no other", scene.getKey())
                    .containsExactlyElementsOf(occupied);
            assertThat(rails.stream().map(PlacedFragment::x).distinct())
                    .as("%s: continuation pages do not shift it sideways", scene.getKey())
                    .hasSize(1);

            double height = graph.canvas().height();
            assertThat(rails).allSatisfy(rail -> {
                assertThat(rail.y()).as("%s: inside the bottom margin", scene.getKey())
                        .isGreaterThanOrEqualTo(20.0 - 1e-9);
                assertThat(rail.y() + rail.height()).as("%s: inside the top margin", scene.getKey())
                        .isLessThanOrEqualTo(height - 20.0 + 1e-9);
            });
        }
    }

    @Test
    void nothingInTheDocumentMovesBecauseTheRailChoseADifferentExtent() {
        // The rail is drawing, not layout. Two graphs of one document, ENTRY_BOUNDS and
        // MARKER_TO_MARKER: the rail differs and every anchor, and the page count, do not.
        LayoutGraph bounded = paginatedScene(TimelineRailEnd.ENTRY_BOUND);
        LayoutGraph trimmed = paginatedScene(TimelineRailEnd.MARKER);

        assertThat(trimmed.totalPages()).isEqualTo(bounded.totalPages());
        assertThat(box(markers(trimmed))).as("markers").isEqualTo(box(markers(bounded)));
        assertThat(box(entries(trimmed))).as("entries").isEqualTo(box(entries(bounded)));
        assertThat(rails(trimmed).get(0).height())
                .as("the premise: the rail itself did change")
                .isNotEqualTo(rails(bounded).get(0).height());
    }

    // --- where a timeline can be put ---------------------------------------------

    @Test
    void aTimelineInsideARowCellIsRefusedTheWayItAlwaysWas() {
        // Not a regression and not a new limit: a timeline lays its entries out in rows, a
        // row cannot hold a row, and that was true before any of this. It is pinned because
        // the marker is wrapped in more levels now, and a wrapper that turned into a row
        // would change the message rather than the outcome.
        assertThatIllegalStateException()
                .isThrownBy(() -> {
                    try (DocumentSession session = GraphCompose.document()
                            .pageSize(400, 300).margin(DocumentInsets.of(20)).create()) {
                        session.pageFlow().addRow(row -> {
                            row.columns(DocumentRowColumn.fixed(120), DocumentRowColumn.weight(1.0));
                            row.addSection(cell -> cell.addParagraph("Beside it"));
                            row.addSection(cell -> cell.addTimeline(t -> t.connector(RAIL, 1.5)
                                    .entry(TimelineMarker.dot(8, INK), e -> e.title("In a cell"))));
                        }).build();
                        session.layoutGraph();
                    }
                })
                .withMessageContaining("cannot contain a nested horizontal row");
    }

    @Test
    void aTimelineMeasuresToTheContainerItIsInAndNotToThePage() {
        // Width measurement, which is where a feature that reads resolved geometry goes
        // wrong quietly: the rail comes from the marker's resolved box, so a container that
        // narrows the timeline has to move the rail with it. A page-width assumption
        // anywhere would leave the rail behind in exactly these two containers.
        double onThePage = rails(timeline(360, 300, TimelineCompatibilityTest::plainEntries)).get(0).x();

        double padded;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 300).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow().addSection(section -> {
                section.padding(DocumentInsets.of(16));
                section.addTimeline(t -> {
                    t.connector(RAIL, 1.5);
                    plainEntries(t);
                });
            }).build();
            padded = rails(session.layoutGraph()).get(0).x();
        }

        assertThat(padded)
                .as("sixteen points of padding move the whole timeline sixteen points in")
                .isEqualTo(onThePage + 16.0, within(1e-9));

        double withMargin;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 300).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow().addSection(section -> {
                section.margin(DocumentInsets.of(12));
                section.addTimeline(t -> {
                    t.connector(RAIL, 1.5);
                    plainEntries(t);
                });
            }).build();
            withMargin = rails(session.layoutGraph()).get(0).x();
        }

        assertThat(withMargin)
                .as("and a margin moves it just as padding does")
                .isEqualTo(onThePage + 12.0, within(1e-9));

        double inACard;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 300).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow().addSection(outer -> {
                outer.padding(DocumentInsets.of(10));
                outer.addSection(card -> {
                    card.padding(DocumentInsets.of(8)).cornerRadius(6);
                    card.addTimeline(t -> {
                        t.connector(RAIL, 1.5);
                        plainEntries(t);
                    });
                });
            }).build();
            inACard = rails(session.layoutGraph()).get(0).x();
        }

        assertThat(inACard)
                .as("and a card inside a section adds both insets, with nothing lost between them")
                .isEqualTo(onThePage + 18.0, within(1e-9));
    }

    // --- markers -------------------------------------------------------------------

    @Test
    void everyKindOfMarkerOfOneSizeResolvesToOneBox() {
        // The rail reads a box, never a shape. Five constructions of a 16pt marker — one of
        // them not a single node — and one answer, so nothing downstream can be depending on
        // a marker being an ellipse. The box is the one the recipe draws: a marker that
        // declares a size and draws another reports what it drew, so these are written to
        // draw what they declare, which is what every factory here does.
        for (Map.Entry<String, TimelineMarker> marker : Map.of(
                "dot", TimelineMarker.dot(16, INK),
                "square", TimelineMarker.square(16, INK),
                "numbered", TimelineMarker.numbered(7, 16, INK, DocumentColor.WHITE),
                "outlined circle", TimelineMarker.circle(16, DocumentColor.WHITE, DocumentStroke.of(INK, 1.5)),
                "composed", TimelineMarker.custom(16, 16, column -> column
                        .addLayerStack(stack -> stack
                                .back(circleNode(16, INK))
                                .center(circleNode(9, DocumentColor.WHITE))))).entrySet()) {
            LayoutGraph graph = timeline(320, 260, t -> t.markerOnRail().axisWidth(24)
                    .entry(marker.getValue(), e -> e.title("Marker").body("Body.")));
            ResolvedLayoutAnchor anchor = markers(graph).get(0);

            assertThat(anchor.width()).as("%s: width", marker.getKey()).isEqualTo(16.0, within(1e-9));
            assertThat(anchor.height()).as("%s: height", marker.getKey()).isEqualTo(16.0, within(1e-9));
            assertThat(rails(graph).get(0).x())
                    .as("%s: and the rail through its centre", marker.getKey())
                    .isEqualTo(anchor.pointX(0.5), within(1e-9));
        }
    }

    @Test
    void howThickAMarkersOutlineIsDoesNotMoveAnything() {
        // A stroke is painted about the shape's edge, so a thick one covers more of the
        // page than a thin one while declaring the same box. The declared box is what the
        // timeline reserves and what the anchor reports, and neither may follow the ink.
        LayoutGraph thin = timeline(320, 260, t -> t.markerOnRail().axisWidth(24)
                .entry(TimelineMarker.circle(14, DocumentColor.WHITE, DocumentStroke.of(INK, 0.5)),
                        e -> e.title("Thin").body("Body.")));
        LayoutGraph thick = timeline(320, 260, t -> t.markerOnRail().axisWidth(24)
                .entry(TimelineMarker.circle(14, DocumentColor.WHITE, DocumentStroke.of(INK, 4.0)),
                        e -> e.title("Thick").body("Body.")));

        assertThat(box(markers(thick))).as("same box, thin outline or thick").isEqualTo(box(markers(thin)));
        assertThat(rails(thick).get(0).x()).as("same rail")
                .isEqualTo(rails(thin).get(0).x(), within(1e-9));
    }

    // --- paint order -----------------------------------------------------------------

    @Test
    void theRailIsPaintedBeforeTheEntrysTextAndNotOnlyBeforeItsMarkers() {
        // UNDER_BODY means under the body, and the body of a timeline is mostly text. Draw
        // order is list order in this engine, so the whole contract is an index comparison:
        // every rail fragment precedes every paragraph the timeline draws.
        LayoutGraph graph = timeline(320, 300, t -> t.markerOnRail().axisWidth(24)
                .entry(TimelineMarker.dot(10, INK), e -> e.title("First").body("A body."))
                .entry(TimelineMarker.dot(10, INK), e -> e.title("Second").body("Another body.")));

        int lastRail = -1;
        int firstPaint = Integer.MAX_VALUE;
        for (int i = 0; i < graph.fragments().size(); i++) {
            PlacedFragment fragment = graph.fragments().get(i);
            String kind = fragment.payload() == null ? "" : fragment.payload().getClass().getSimpleName();
            if ("@timeline-rail".equals(fragment.path())) {
                lastRail = Math.max(lastRail, i);
            } else if (kind.contains("Paragraph") || kind.contains("Ellipse")) {
                firstPaint = Math.min(firstPaint, i);
            }
        }
        assertThat(lastRail).as("there is a rail at all").isNotEqualTo(-1);
        assertThat(firstPaint).as("and something painted over it").isNotEqualTo(Integer.MAX_VALUE);
        assertThat(lastRail)
                .as("every rail fragment comes first, so nothing of the entry is hidden by it")
                .isLessThan(firstPaint);
    }

    @Test
    void aRailInsideAFilledPanelIsStillOnThePage() throws Exception {
        // A rail is drawn under the body, and it used to be drawn under the *document's*
        // body: first in the fragment list, before everything. That is beneath the fill of
        // whatever the timeline is inside, so a timeline in a card lost its rail — present
        // in the graph, absent from the page, and invisible to every geometry assertion
        // there is. The only instrument that can see it is a rendered pixel.
        assertThat(railPixelsPainted(section -> { }))
                .as("the control: with nothing over it the rail paints")
                .isGreaterThan(0);
        assertThat(railPixelsPainted(section -> section.softPanel(DocumentColor.WHITE, 8, 14)))
                .as("and a white panel around it does not swallow it")
                .isGreaterThan(0);
        assertThat(railPixelsPainted(section -> section.softPanel(DocumentColor.rgb(245, 245, 250), 8, 14)))
                .as("nor a tinted one")
                .isGreaterThan(0);
    }

    /** How many pixels of exactly the rail's colour survive to the rendered page. */
    private static int railPixelsPainted(Consumer<SectionBuilder> panel) throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 220).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow().addSection(section -> {
                panel.accept(section);
                section.addTimeline(t -> t.connector(RAIL, 1.5)
                        .entry(TimelineMarker.numbered(1, 14, INK, DocumentColor.WHITE),
                                e -> e.title("Kickoff").meta("Jan 2026").body("Scope agreed."))
                        .entry(TimelineMarker.dot(8, INK),
                                e -> e.title("Beta").meta("Mar 2026").body("First external users.")));
            }).build();
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage page = new PDFRenderer(document).renderImageWithDPI(0, 72);
            int painted = 0;
            for (int y = 0; y < page.getHeight(); y++) {
                for (int x = 0; x < page.getWidth(); x++) {
                    int pixel = page.getRGB(x, y);
                    if (((pixel >> 16) & 0xff) == 150 && ((pixel >> 8) & 0xff) == 158
                        && (pixel & 0xff) == 172) {
                        painted++;
                    }
                }
            }
            return painted;
        }
    }

    // --- scenes -----------------------------------------------------------------------

    private static void plainEntries(TimelineBuilder t) {
        t.entry(TimelineMarker.dot(8, INK), e -> e.title("First").body("Body one."))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Body two."));
    }

    private static LayoutGraph paginatedScene() {
        return paginatedScene(null);
    }

    private static LayoutGraph paginatedScene(TimelineRailEnd both) {
        return timeline(320, 170, t -> {
            t.spacing(14);
            if (both != null) {
                t.rail(rail -> rail.from(both).to(both));
            }
            t.entry(TimelineMarker.dot(8, INK), e -> e.title("Runs on").body(longBody(30)))
                    .entry(TimelineMarker.dot(8, INK), e -> e.title("And ends here"));
        });
    }

    private static LayoutGraph veryTallScene() {
        return timeline(300, 140, t -> t
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Enormous").body(longBody(60))));
    }

    private static LayoutGraph nearBottomScene() {
        return document(320, 200, flow -> {
            flow.addParagraph("Filler one.").addParagraph("Filler two.").addParagraph("Filler three.")
                    .addParagraph("Filler four.").addParagraph("Filler five.").addParagraph("Filler six.");
            flow.addTimeline(t -> t.connector(RAIL, 1.5)
                    .entry(TimelineMarker.dot(8, INK), e -> e.title("Starts low").body("Body one."))
                    .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Body two.")));
        });
    }

    // --- helpers ------------------------------------------------------------------------

    private static String box(List<ResolvedLayoutAnchor> anchors) {
        StringBuilder out = new StringBuilder();
        for (ResolvedLayoutAnchor anchor : anchors) {
            out.append(String.format(java.util.Locale.ROOT, "p%d[%.9f,%.9f,%.9f,%.9f] ",
                    anchor.pageIndex(), anchor.x(), anchor.y(), anchor.width(), anchor.height()));
        }
        return out.toString();
    }

    private static com.demcha.compose.document.node.EllipseNode circleNode(double size, DocumentColor fill) {
        return new com.demcha.compose.document.node.EllipseNode(
                "marker", size, size, fill, null, null, null, null, null);
    }

    private static String longBody(int sentences) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < sentences; i++) {
            body.append("Sentence ").append(i).append(" of a body that keeps going. ");
        }
        return body.toString();
    }

    private static int pagesOf(LayoutGraph graph) {
        return graph.totalPages();
    }

    private static List<PlacedFragment> rails(LayoutGraph graph) {
        return graph.fragments().stream().filter(f -> "@timeline-rail".equals(f.path())).toList();
    }

    private static List<ResolvedLayoutAnchor> entries(LayoutGraph graph) {
        return ResolvedLayoutMetadata.from(graph).anchors().stream()
                .filter(a -> "ENTRY".equals(a.id().kind().toString())).toList();
    }

    private static List<ResolvedLayoutAnchor> markers(LayoutGraph graph) {
        return ResolvedLayoutMetadata.from(graph).anchors().stream()
                .filter(a -> "MARKER".equals(a.id().kind().toString())).toList();
    }

    private static LayoutGraph timeline(double width, double height, Consumer<TimelineBuilder> spec) {
        return document(width, height, flow -> flow.addTimeline(t -> {
            t.connector(RAIL, 1.5);
            spec.accept(t);
        }));
    }

    private static LayoutGraph document(double width, double height, Consumer<PageFlowBuilder> content) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(width, height).margin(DocumentInsets.of(20)).create()) {
            PageFlowBuilder flow = session.pageFlow();
            content.accept(flow);
            flow.build();
            return session.layoutGraph();
        } catch (Exception failure) {
            throw new IllegalStateException("layout failed", failure);
        }
    }
}
