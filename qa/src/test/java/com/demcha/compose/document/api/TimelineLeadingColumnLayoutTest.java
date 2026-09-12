package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one thing a leading column has to do: line up across entries.
 *
 * <p>A {@code DATE | ● | CONTENT} timeline is only that layout if every entry's marker
 * starts at the same x. The dates are not the same length — that is the whole point of the
 * column — so the width cannot come from the text, and this asserts the consequence rather
 * than the mechanism: the same three column positions in every entry, including in an entry
 * that put nothing in its leading column at all.</p>
 *
 * <p>Measured before it was designed. With {@link DocumentRowColumn#auto()} the same two
 * rows put their markers 131pt apart, because an auto column is sized from its own row's
 * content; {@code fixed} and {@code weight} both aligned exactly. That is why
 * {@code leadingColumn(auto())} is rejected at the call rather than shipped as a layout
 * that happens to look right whenever the dates are the same length.</p>
 */
class TimelineLeadingColumnLayoutTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    @Test
    void everyEntrysColumnsStartAtTheSameXWhateverItsDateSays() throws Exception {
        LayoutGraph graph = timeline(DocumentRowColumn.fixed(56));

        Map<Integer, List<Double>> byColumn = columnPositions(graph);
        // Five columns: the leading one, the marker, the content, and a gap either side of
        // the marker. Each gap is a column of its own because a row spaces every pair of
        // its columns by one number, and the two gaps have to be free to differ.
        assertThat(byColumn).as("five columns, four entries").hasSize(5);
        assertThat(byColumn.get(0)).as("every leading column").hasSize(4);

        byColumn.forEach((index, positions) -> assertThat(positions)
                .as("column %d starts at one x in every entry, not one per date length", index)
                .containsOnly(positions.get(0)));

        // And the three that carry something are distinct columns in order, not one
        // collapsed on top of another.
        assertThat(List.of(byColumn.get(0).get(0), byColumn.get(2).get(0), byColumn.get(4).get(0)))
                .isSorted()
                .doesNotHaveDuplicates();
    }

    @Test
    void aWeightedLeadingColumnAlignsJustAsAFixedOneDoes() throws Exception {
        // Both are decided by the row rather than by the text, which is the property that
        // matters; the two are offered so the caller can pick points or a share, not
        // because one of them aligns and the other does not.
        columnPositions(timeline(DocumentRowColumn.weight(0.35)))
                .forEach((index, positions) -> assertThat(positions)
                        .as("column %d", index)
                        .containsOnly(positions.get(0)));
    }

    @Test
    void aLeadingColumnTimelineGeometryIsPinned() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow().addTimeline(entries(DocumentRowColumn.fixed(56))).build();
            LayoutSnapshotAssertions.assertMatches(session, "document/timeline_leading_column");
        }
    }

    /** Four entries: three dates of very different lengths, and one with no date at all. */
    private static Consumer<com.demcha.compose.document.dsl.TimelineBuilder> entries(DocumentRowColumn column) {
        return t -> t
                .connector(RAIL, 1.5)
                .leadingColumn(column)
                .entry(e -> e.marker(TimelineMarker.dot(8, INK))
                        .leading(d -> d.addParagraph("2023"))
                        .title("Senior Engineer").body("Led the layout engine rewrite."))
                .entry(e -> e.marker(TimelineMarker.dot(8, INK))
                        .leading(d -> d.addParagraph("September 2024 - present"))
                        .title("Engineer").body("Shipped the pagination compiler."))
                .entry(e -> e.marker(TimelineMarker.square(8, INK))
                        .leading(d -> d.addParagraph("Q1"))
                        .title("Junior Engineer"))
                .entry(e -> e.marker(TimelineMarker.dot(8, INK))
                        .title("No date at all"));
    }

    private static LayoutGraph timeline(DocumentRowColumn column) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow().addTimeline(entries(column)).build();
            return session.layoutGraph();
        }
    }

    /**
     * Each header row's column boxes, keyed by column index, in entry order.
     *
     * <p>The row's own children, not its descendants — {@code parentPath} is a full path,
     * so a plain "contains RowNode" also collects every paragraph inside every column and
     * groups them under the same index.</p>
     */
    private static Map<Integer, List<Double>> columnPositions(LayoutGraph graph) {
        return graph.nodes().stream()
                .filter(node -> node.parentPath() != null && node.parentPath().matches(".*RowNode\\[\\d+]$"))
                .collect(Collectors.groupingBy(PlacedNode::childIndex, TreeMap::new,
                        Collectors.mapping(PlacedNode::placementX, Collectors.toList())));
    }
}
