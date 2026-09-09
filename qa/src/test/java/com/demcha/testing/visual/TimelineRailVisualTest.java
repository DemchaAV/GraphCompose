package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

/**
 * Pixel baseline for the timeline DSL, ahead of the rail rework.
 *
 * <p>The layout snapshots pin where things land; this pins what they look like. The two
 * catch different regressions: a rail drawn in the wrong colour, drawn on top of its
 * markers instead of under them, or not drawn at all, moves no geometry and passes every
 * snapshot.</p>
 *
 * <p>Deliberately a small page. A full-A4 baseline drifts across platforms by more than
 * the signal it carries; a tight page keeps the comparison meaningful.</p>
 *
 * <p>The marker sizes differ on purpose — 6, 14 and 9 pt. Keeping the rail aligned under
 * markers of different sizes is a stated goal of the rework, and today the marker centre
 * is {@code margin + gutter + size/2} while the rail sits at the section edge, so this
 * baseline records a rail the markers do <em>not</em> sit on. That is the behaviour being
 * preserved or deliberately changed, and either way it should be visible in a diff.</p>
 */
class TimelineRailVisualTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void classicTimelineLooksTheWayItDoesToday() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 240)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(12)
                            .entry(TimelineMarker.dot(6, INK), e -> e
                                    .title("Senior Engineer")
                                    .meta("2023 - Present")
                                    .body("Led the layout engine rewrite."))
                            .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE), e -> e
                                    .title("Engineer")
                                    .meta("2021 - 2023"))
                            .entry(TimelineMarker.square(9, INK), e -> e
                                    .title("Junior Engineer")
                                    .meta("2019 - 2021")))
                    .build();

            VISUAL.assertMatchesBaseline("timeline-dsl/classic", session);
        }
    }

    @Test
    void aDateColumnRendersBesideTheRailAndTheMarkersStayInLine() throws Exception {
        // The layout test says the columns line up; this says the page actually draws that
        // way — that the dates are painted in a column of their own rather than wrapping
        // into the marker's, and that the rail is still one line down the left. The third
        // entry has no date, which is where an empty column would collapse if it did.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 200)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(12)
                            .leadingColumn(DocumentRowColumn.fixed(70))
                            .entry(e -> e.marker(TimelineMarker.dot(6, INK))
                                    .leading(d -> d.addParagraph("2023"))
                                    .title("Senior Engineer")
                                    .body("Led the layout engine rewrite."))
                            .entry(e -> e.marker(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE))
                                    .leading(d -> d.addParagraph("Sept 2021"))
                                    .title("Engineer"))
                            .entry(e -> e.marker(TimelineMarker.square(9, INK))
                                    .title("No date at all")))
                    .build();

            VISUAL.assertMatchesBaseline("timeline-dsl/leading-column", session);
        }
    }

    @Test
    void aCustomMarkerIsDrawnWhateverItIsMadeOf() throws Exception {
        // A marker the timeline has never heard of: a ring, a disc and a pip stacked, and
        // a bordered pill. Both declare their own box and neither needed a line in
        // TimelineBuilder. The baseline is here because "it lays out" and "it is painted"
        // are different claims — a marker recipe that drew nothing would pass every layout
        // assertion in this repository.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 170)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(12)
                            .axisWidth(22)
                            .entry(TimelineMarker.custom(18, 18, column -> column
                                    .addLayerStack(stack -> stack
                                            .back(circle(18, INK))
                                            .center(circle(11, DocumentColor.WHITE))
                                            .center(circle(5, INK)))),
                                    e -> e.title("Composed of three").meta("one declared box"))
                            .entry(TimelineMarker.custom(22, 12, column -> column
                                    .addShape(shape -> shape
                                            .size(22, 12)
                                            .cornerRadius(6)
                                            .fillColor(DocumentColor.WHITE)
                                            .stroke(DocumentStroke.of(INK, 1.0))
                                            .margin(DocumentInsets.zero()))),
                                    e -> e.title("Not square either")))
                    .build();

            VISUAL.assertMatchesBaseline("timeline-dsl/custom-marker", session);
        }
    }

    @Test
    void markersOfEverySizeSitOnOneRailWhenTheyAreAnchoredToIt() throws Exception {
        // DATE | ● | CONTENT, with markers of 6, 14 and 24pt. The geometry is asserted in
        // TimelineRailGeometryTest — every marker's centre is the rail's x, to 1e-9. This
        // is the picture of it: one straight axis with three very different markers strung
        // on it, and the dates in their own column to its left.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 210)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(12)
                            .markerOnRail()
                            .axisWidth(28)
                            .leadingColumn(DocumentRowColumn.fixed(54))
                            .entry(e -> e.marker(TimelineMarker.dot(6, INK))
                                    .leading(d -> d.addParagraph("2023"))
                                    .title("Senior Engineer")
                                    .body("A small dot, centred on the axis."))
                            .entry(e -> e.marker(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE))
                                    .leading(d -> d.addParagraph("2021"))
                                    .title("Engineer")
                                    .body("A numbered disc, centred on the same axis."))
                            .entry(e -> e.marker(TimelineMarker.square(24, INK))
                                    .leading(d -> d.addParagraph("2019"))
                                    .title("Junior Engineer")
                                    .body("And a square four times the dot's size.")))
                    .build();

            VISUAL.assertMatchesBaseline("timeline-dsl/marker-on-rail", session);
        }
    }

    private static EllipseNode circle(double size, DocumentColor fill) {
        return new EllipseNode("marker", size, size, fill, null, null, null, null, null);
    }
}
