package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Where the axis column starts, and what a later phase may and may not read off it.
 *
 * <p>Measured, and the answer has two halves. The column's <b>x is steady</b> under markers
 * of every size, which is the property a rail needs. Its <b>width is not the column's</b>:
 * the section placed in the column shrinks to the marker it holds, so a 6pt dot reports 6pt
 * and a 14pt disc 14pt in a column declared at 20. The declared width acts as a cap, not as
 * the section's size.</p>
 *
 * <p>That matters for the phases that derive the rail. {@code axisX + axisWidth * alignment}
 * cannot take {@code axisWidth} from the graph — the column's own slot is not a node, and
 * the number that <em>is</em> there belongs to the marker. Reading it would put the rail
 * back where it is today, drifting with marker size. The marker's own resolved box is the
 * thing to anchor on, which is what the resolved-layout seam reports.</p>
 *
 * <p>Both sizing strategies are asserted, because they resolve by different routes: a
 * weight is a share of what the row has left, a fixed width is points. Neither is
 * converted into the other — there is no row width at the point where that conversion
 * would have to happen — so each has to be shown to hold on its own.</p>
 */
class TimelineAxisColumnLayoutTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    @Test
    void aFixedAxisStartsAtTheSameXUnderMarkersOfEverySize() throws Exception {
        List<PlacedNode> axis = axisColumns(t -> t.axisWidth(20));

        assertThat(axis).hasSize(3);
        assertThat(axis.stream().map(PlacedNode::placementX))
                .as("same x under a 6pt dot, a 14pt disc and a 24pt one")
                .containsOnly(axis.get(0).placementX());
    }

    @Test
    void aWeightedAxisStartsAtTheSameXToo() throws Exception {
        List<PlacedNode> axis = axisColumns(t -> t.markerColumnWeight(0.12));

        assertThat(axis.stream().map(PlacedNode::placementX))
                .containsOnly(axis.get(0).placementX());
    }

    @Test
    void theSectionInTheAxisIsTheMarkersWidthAndNotTheColumnsSoNoRailMayBeDerivedFromIt() throws Exception {
        // The measurement that decides how the rail is built later. Under a 20pt axis the
        // three markers report 6, 14 and 20 — their own widths, with the declared width
        // acting as a cap on the last. A rail computed as axisX + width/2 from these
        // numbers would sit at three different x down one timeline, which is the defect
        // being reworked, not a new one.
        List<Double> widths = axisColumns(t -> t.axisWidth(20)).stream()
                .map(PlacedNode::placementWidth).toList();

        assertThat(widths)
                .as("the marker's width, capped by the column, not the column's width")
                .containsExactly(6.0, 14.0, 20.0);
    }

    @Test
    void theDefaultAxisIsAShareOfTheRowAndSoMovesWithThePageWidth() throws Exception {
        // The assertion no single-page snapshot can make, and the one that catches the
        // conversion this phase forbids. Converting the default weight into points is
        // right on exactly one page width: 0.10 resolves to 24pt on the 320pt page the
        // layout snapshots use, so replacing the default with Fixed(24) leaves both of
        // them green — and moves every timeline on any other width. Here the same
        // timeline is laid out twice, and a share has to give two answers.
        double narrow = contentColumnX(320);
        double wide = contentColumnX(480);

        assertThat(narrow)
                .as("a weighted axis is wider on a wider page, so the content starts further in")
                .isLessThan(wide);
    }

    @Test
    void aFixedAxisDoesNotMoveWithThePageWidth() throws Exception {
        // The other half of the same fact, and the reason both strategies exist: points
        // are points. A caller who wants the markers the same distance from the edge on
        // every page asks for this one.
        assertThat(contentColumnX(320, t -> t.axisWidth(20)))
                .as("same margin, same gutter, same 20pt axis — so the content starts in the same place")
                .isEqualTo(contentColumnX(480, t -> t.axisWidth(20)), within(1e-9));
    }

    @Test
    void aMarkerOfThreeShapesSitsExactlyWhereAMarkerOfOneDoes() throws Exception {
        // The contract a rail will depend on: how a marker is built must not be visible in
        // the geometry around it. Both of these declare 16×16; one draws a single ellipse
        // and the other draws three stacked ones.
        double single = axisAndContent(TimelineMarker.dot(16, INK));
        double composed = axisAndContent(TimelineMarker.custom(16, 16, column ->
                column.addLayerStack(stack -> stack
                        .back(circle(16, INK))
                        .center(circle(10, DocumentColor.WHITE))
                        .center(circle(4, INK)))));

        assertThat(composed)
                .as("three fragments or one, the content beside the marker starts in the same place")
                .isEqualTo(single, within(1e-9));
    }

    @Test
    void aWeightAndAPointWidthAreNotTheSameNumberAndAreNotTreatedAsOne() throws Exception {
        // The reason the two survive to the layout separately: on this page the default
        // weight and a 20pt request land in different places. A builder that "helpfully"
        // converted a weight into points would have to pick one page width to be right on,
        // and would move every timeline already written on any other width.
        double weighted = axisColumns(t -> t.markerColumnWeight(0.10)).get(0).placementX();
        double fixed = axisColumns(t -> t.axisWidth(20)).get(0).placementX();

        assertThat(weighted).isEqualTo(fixed, within(1e-9));

        // Same x — both columns start at the entry's left edge — but the content beside
        // them does not, because the axis they reserve is a different width.
        double weightedContent = contentColumns(t -> t.markerColumnWeight(0.10)).get(0).placementX();
        double fixedContent = contentColumns(t -> t.axisWidth(20)).get(0).placementX();
        assertThat(weightedContent).isNotEqualTo(fixedContent);
    }

    /** The axis column of each entry, in entry order. */
    private static List<PlacedNode> axisColumns(Consumer<TimelineBuilder> sizing) throws Exception {
        return headerColumns(sizing, 0);
    }

    /** The content column beside the marker, in entry order. */
    private static List<PlacedNode> contentColumns(Consumer<TimelineBuilder> sizing) throws Exception {
        return headerColumns(sizing, 1);
    }

    private static List<PlacedNode> headerColumns(Consumer<TimelineBuilder> sizing, int index) throws Exception {
        return columnsOf(timeline(sizing), index);
    }

    private static List<PlacedNode> columnsOf(LayoutGraph graph, int index) {
        return graph.nodes().stream()
                .filter(node -> node.parentPath() != null && node.parentPath().matches(".*RowNode\\[\\d+]$"))
                .filter(node -> node.childIndex() == index)
                .toList();
    }

    private static EllipseNode circle(double size, DocumentColor fill) {
        return new EllipseNode("marker", size, size, fill, null, null, null, null, null);
    }

    /** Where the content beside one marker starts, with a fixed axis wide enough to hold it. */
    private static double axisAndContent(TimelineMarker marker) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow().addTimeline(t -> t
                    .connector(RAIL, 1.5)
                    .axisWidth(20)
                    .entry(marker, e -> e.title("Beside"))).build();
            return columnsOf(session.layoutGraph(), 1).get(0).placementX();
        }
    }

    /** Where the content beside the marker starts, on a page of the given width. */
    private static double contentColumnX(double pageWidth) throws Exception {
        return contentColumnX(pageWidth, t -> { });
    }

    private static double contentColumnX(double pageWidth, Consumer<TimelineBuilder> sizing) throws Exception {
        return columnsOf(timeline(sizing, pageWidth), 1).get(0).placementX();
    }

    private static LayoutGraph timeline(Consumer<TimelineBuilder> sizing) throws Exception {
        return timeline(sizing, 320);
    }

    private static LayoutGraph timeline(Consumer<TimelineBuilder> sizing, double pageWidth) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(pageWidth, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow().addTimeline(t -> {
                t.connector(RAIL, 1.5);
                sizing.accept(t);
                t.entry(TimelineMarker.dot(6, INK), e -> e.title("Small"))
                        .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE), e -> e.title("Medium"))
                        .entry(TimelineMarker.numbered(3, 24, INK, DocumentColor.WHITE), e -> e.title("Large"));
            }).build();
            return session.layoutGraph();
        }
    }
}
