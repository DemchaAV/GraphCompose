package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Where an entry's body sits once the rail runs through the axis.
 *
 * <p>With the rail beside the axis — every timeline written before there was a choice — a
 * body spans the entry and clears the line by the gutter, and none of this applies. With
 * {@code markerOnRail()} the line moved into the middle of the axis column, and a body
 * spanning the entry would be drawn through. So the body is laid out in the content column
 * instead:</p>
 *
 * <pre>
 * LEADING | AXIS | CONTENT
 *         |  ●   | title
 *         |  │   | body line 1
 *         |  │   | body line 2
 *         |  ●   | next entry
 * </pre>
 *
 * <p>It is in the column, not in the row: a row is laid out on one page, and an entry has to
 * be able to be longer than a page. The column it uses is the one the header row resolved,
 * published for it — so a share and a width resolve the same way, and neither is recomputed
 * here.</p>
 */
class TimelineBodyColumnTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    @Test
    void theBodyStartsAndEndsWhereTheContentColumnDoes() {
        // The claim, in the row's own numbers: the body is the content column, not an
        // indent that happens to look like it.
        LayoutGraph graph = timeline(360, 320, t -> t.markerOnRail().axisWidth(28)
                .entry(TimelineMarker.dot(10, INK), e -> e
                        .title("Title").body("A body long enough to wrap onto a second line here.")));

        PlacedNode contentColumn = contentColumn(graph);
        PlacedNode body = body(graph);
        assertThat(body.placementX())
                .as("bodyX is the content column's x")
                .isEqualTo(contentColumn.placementX(), within(1e-9));
        assertThat(body.placementX() + body.placementWidth())
                .as("and it runs to the same right edge")
                .isEqualTo(rowRightEdge(graph), within(1e-9));
    }

    @Test
    void theRailIsLeftOfTheBodyAndNoTextCrossesIt() {
        // The defect this closes, stated as a comparison rather than as a picture: every
        // paragraph the entry draws starts to the right of the line.
        LayoutGraph graph = timeline(360, 320, t -> t.markerOnRail().axisWidth(28)
                .entry(TimelineMarker.dot(10, INK), e -> e
                        .title("Title").meta("meta")
                        .body("A body long enough to wrap onto a second line on this page.")));

        double railX = rails(graph).get(0).x();
        PlacedNode body = body(graph);
        assertThat(railX).as("railX < bodyX").isLessThan(body.placementX());

        assertThat(graph.fragments().stream()
                .filter(f -> f.payload() != null
                             && f.payload().getClass().getSimpleName().contains("Paragraph"))
                .toList())
                .as("no text of the entry is drawn across the rail")
                .allSatisfy(text -> assertThat(text.x()).isGreaterThan(railX));
    }

    @Test
    void aFixedAxisAndAWeightedOnePutTheBodyInTheSamePlaceRelativeToTheirColumns() {
        // The reason this goes through the resolved column rather than through points: a
        // share is not a number until the row is laid out, and both have to work.
        for (Consumer<TimelineBuilder> axis : List.<Consumer<TimelineBuilder>>of(
                t -> t.axisWidth(28), t -> t.markerColumnWeight(0.18))) {
            LayoutGraph graph = timeline(360, 320, t -> {
                t.markerOnRail();
                axis.accept(t);
                t.entry(TimelineMarker.dot(10, INK), e -> e.title("Title").body("A body that wraps."));
            });

            assertThat(body(graph).placementX())
                    .as("the body is the content column, whichever way the axis was sized")
                    .isEqualTo(contentColumn(graph).placementX(), within(1e-9));
            assertThat(rails(graph).get(0).x()).isLessThan(body(graph).placementX());
        }
    }

    @Test
    void aLeadingColumnMovesTheBodyWithTheAxisAndNotPastIt() {
        // Three columns now, and the body belongs to the third. The date column is to the
        // left of the rail, the body to the right of it.
        LayoutGraph graph = timeline(420, 320, t -> t.markerOnRail().axisWidth(28)
                .leadingColumn(DocumentRowColumn.fixed(60))
                .entry(e -> e.marker(TimelineMarker.dot(10, INK))
                        .leading(d -> d.addParagraph("2023"))
                        .title("Title").body("A body that wraps onto a second line.")));

        double railX = rails(graph).get(0).x();
        PlacedNode body = body(graph);
        assertThat(body.placementX()).isEqualTo(contentColumn(graph).placementX(), within(1e-9));
        assertThat(railX).isLessThan(body.placementX());
        assertThat(body.placementX())
                .as("and well right of the date column it is not in")
                .isGreaterThan(60.0);
    }

    @Test
    void aBodyLongerThanAPageStillSplitsAndKeepsItsColumn() {
        // What the row could not do. Two pages at least, one x, one width, and no complaint
        // that an atomic block does not fit.
        LayoutGraph graph = timeline(320, 170, t -> t.markerOnRail().axisWidth(24)
                .entry(TimelineMarker.dot(10, INK), e -> e.title("Runs on").body(longBody(40))));

        PlacedNode body = body(graph);
        assertThat(graph.totalPages()).as("the premise: it crosses pages").isGreaterThanOrEqualTo(2);
        assertThat(body.startPage()).isZero();
        assertThat(body.endPage()).as("one body, several pages").isGreaterThanOrEqualTo(1);
        assertThat(body.placementX())
                .as("continuation pages keep the same bodyX")
                .isEqualTo(contentColumn(graph).placementX(), within(1e-9));
        assertThat(body.placementX() + body.placementWidth())
                .as("and the same bodyWidth")
                .isEqualTo(rowRightEdge(graph), within(1e-9));

        double railX = rails(graph).get(0).x();
        assertThat(rails(graph).stream().map(PlacedFragment::x).distinct())
                .as("and the rail is still one line")
                .hasSize(1);
        assertThat(railX).isLessThan(body.placementX());
    }

    @Test
    void aTimelineThatDoesNotAskForTheRailKeepsItsBodyWhereItAlwaysWas() {
        // The other half, and the one that must not move: no markerOnRail, no published
        // column, body spanning the entry and clearing the rail by the gutter.
        LayoutGraph graph = timeline(360, 320, t -> t.gutter(8)
                .entry(TimelineMarker.dot(10, INK), e -> e.title("Title").body("A body that wraps.")));

        assertThat(graph.nodes()).as("nothing publishes and nothing consumes")
                .noneSatisfy(n -> assertThat(n.nodeKind()).contains("Band"));

        double railX = rails(graph).get(0).x();
        PlacedFragment bodyText = graph.fragments().stream()
                .filter(f -> f.payload() != null
                             && f.payload().getClass().getSimpleName().contains("Paragraph"))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(bodyText.x())
                .as("the body still starts one gutter right of the rail, as it always has")
                .isEqualTo(railX + 8.0, within(1e-9));
    }

    // --- helpers ---------------------------------------------------------------

    private static PlacedNode body(LayoutGraph graph) {
        return graph.nodes().stream()
                .filter(n -> "HorizontalBandContentNode".equals(n.nodeKind()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the body is not in a published column"));
    }

    /** The content column of the first entry's header row, as the row placed it. */
    private static PlacedNode contentColumn(LayoutGraph graph) {
        List<PlacedNode> columns = graph.nodes().stream()
                .filter(n -> n.parentPath() != null && n.parentPath().matches(".*RowNode\\[\\d+]$"))
                .toList();
        return columns.get(columns.size() - 1);
    }

    private static double rowRightEdge(LayoutGraph graph) {
        PlacedNode row = graph.nodes().stream()
                .filter(n -> "RowNode".equals(n.nodeKind()))
                .findFirst().orElseThrow();
        return row.placementX() + row.placementWidth();
    }

    private static List<PlacedFragment> rails(LayoutGraph graph) {
        return graph.fragments().stream().filter(f -> "@timeline-rail".equals(f.path())).toList();
    }

    private static String longBody(int sentences) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < sentences; i++) {
            body.append("Sentence ").append(i).append(" of a body that keeps going. ");
        }
        return body.toString();
    }

    private static LayoutGraph timeline(double width, double height, Consumer<TimelineBuilder> spec) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(width, height).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow().addTimeline(t -> {
                t.connector(RAIL, 1.5);
                spec.accept(t);
            }).build();
            return session.layoutGraph();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("layout failed", failure);
        }
    }
}
