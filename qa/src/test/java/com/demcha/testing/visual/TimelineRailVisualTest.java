package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailExtent;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

/**
 * What a timeline looks like: the finished visual model, one baseline per scene.
 *
 * <p>The first of these was recorded before the rail rework, to catch what a coordinate
 * cannot: a rail in the wrong colour, or not drawn at all, moves no geometry and passes every
 * snapshot. It has not changed since, which is the compatibility claim of the whole rework and
 * the reason it is still here.</p>
 *
 * <p>These images are a coarse net, deliberately — see the budget below. The sharp claims
 * about paint order live where a platform cannot blur them: on the fragment list, and in a
 * render that counts rail-coloured pixels rather than comparing two pictures.</p>
 *
 * <p>The rest are the scenes the rework made possible or made ambiguous — markers strung on
 * the line rather than beside it, a rail trimmed to the outer markers, a rail crossing pages,
 * a ring the line disappears behind. Each is paired with an assertion in
 * {@code TimelineVisualScenarioGeometryTest} that says in numbers what the picture shows, and
 * the two extents are drawn on one identical scene so the pair can be read as a diff.</p>
 *
 * <p>Deliberately small pages. A full-A4 baseline drifts across platforms by more than the
 * signal it carries; a tight page keeps the comparison meaningful.</p>
 */
class TimelineRailVisualTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    // The baselines are committed as Windows-rendered PNGs and compared on Linux CI, where
    // PDFBox text rasterisation drifts: measured on these very scenes, 716 to 2 539 pixels of
    // a structurally identical page, worst per-channel delta 202. A budget is therefore not
    // optional, and the same one ShapeContainerVisualRegressionTest arrived at for the same
    // reason is the right order of magnitude.
    //
    // Be clear about what that costs. A paint-order flip on these scenes moves 129 to 178
    // pixels — an order of magnitude *below* the drift — so these images cannot be the guard
    // for it, and they are not: whether the rail is painted under the markers and under the
    // text is asserted on the fragment list itself (TimelineRailGeometryTest,
    // TimelineVisualScenarioGeometryTest), and whether it survives an opaque panel is asserted
    // by counting rail-coloured pixels on a rendered page (TimelineCompatibilityTest), which
    // asks a question no platform difference can answer wrongly. What these baselines catch is
    // gross visual change: a rail not drawn at all, a marker missing, content moving column,
    // a colour swapped, a scene reflowing.
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard()
            .perPixelTolerance(6)
            .mismatchedPixelBudget(3_000);

    // The paginated scene is a wall of body text on a small page, so text — the part that
    // drifts — is most of the image: 6 598 pixels of 45 000 on the same comparison. Kept
    // separate rather than loosening every scene to the worst one.
    private static final PdfVisualRegression VISUAL_TEXT_DENSE = PdfVisualRegression.standard()
            .perPixelTolerance(6)
            .mismatchedPixelBudget(8_000);

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

    @Test
    void markersOfEverySizeSitOnOneRailWithNoDateColumnToHelp() throws Exception {
        // The same three sizes as the scene above, with the date column taken away. Two
        // pictures rather than one because a leading column is the first thing suspected
        // when a line bends, and here there is none to suspect: 6, 14 and 24pt on one axis.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(12)
                            .markerOnRail()
                            .axisWidth(28)
                            .entry(TimelineMarker.dot(6, INK), e -> e
                                    .title("Small").body("Six points across."))
                            .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE), e -> e
                                    .title("Medium").body("Fourteen, and numbered."))
                            .entry(TimelineMarker.square(24, INK), e -> e
                                    .title("Large").body("Twenty-four, and square.")))
                    .build();

            VISUAL.assertMatchesBaseline("timeline-dsl/marker-on-rail-sizes", session);
        }
    }

    @Test
    void entryBoundsRunsThroughEveryEntryAndOverTheSpacingBetweenThem() throws Exception {
        // Half of a pair: this scene and the next differ by one argument and nothing else.
        // Entries of deliberately different heights, 16pt apart — and the line covers the
        // gaps, because an entry's spacing is padding inside its own box rather than a hole
        // between two boxes. That is the default every existing timeline draws.
        try (DocumentSession session = extentScene(TimelineRailExtent.ENTRY_BOUNDS)) {
            VISUAL.assertMatchesBaseline("timeline-dsl/entry-bounds", session);
        }
    }

    @Test
    void markerToMarkerStopsAtTheOuterMarkersOnTheVerySameScene() throws Exception {
        // The other half. Same page, same entries, same rail — and now the line begins at
        // the first marker and ends at the last, with the tall entry's body hanging below
        // it. Read against its twin, the diff is the two ends and nothing else.
        try (DocumentSession session = extentScene(TimelineRailExtent.MARKER_TO_MARKER)) {
            VISUAL.assertMatchesBaseline("timeline-dsl/marker-to-marker", session);
        }
    }

    @Test
    void aPaginatedTimelineDrawsItsRailOnEveryPageItReaches() throws Exception {
        // Three pages out of two entries, and the only scene here that a single page cannot
        // show at all: the line starts at the first marker, runs the full band of the page
        // that holds nothing but the first entry's body, and stops at the second marker on
        // the last. Three fragments of one logical rail, each bounded by its own page.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 150)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(14)
                            .markerOnRail()
                            .axisWidth(24)
                            .rail(rail -> rail.extent(TimelineRailExtent.MARKER_TO_MARKER))
                            .entry(TimelineMarker.dot(10, INK), e -> e
                                    .title("Runs on").body(longBody()))
                            .entry(TimelineMarker.dot(10, INK), e -> e
                                    .title("And ends here")))
                    .build();

            VISUAL_TEXT_DENSE.assertMatchesBaseline("timeline-dsl/paginated-marker-to-marker", session);
        }
    }

    @Test
    void aHollowMarkerBreaksTheRailCleanlyBecauseTheRailIsUnderIt() throws Exception {
        // TimelineMarker.circle(size, fill, stroke) with the page's own colour as the fill:
        // where the line passes through a ring it is covered, so it reads as broken at each
        // stop instead of crossing three of them. Pure paint order — the geometry is
        // identical either way — which is why only a picture can be the evidence.
        DocumentStroke ring = DocumentStroke.of(INK, 1.2);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 210)
                .margin(DocumentInsets.of(18))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(12)
                            .markerOnRail()
                            .axisWidth(26)
                            .entry(TimelineMarker.circle(10, DocumentColor.WHITE, ring), e -> e
                                    .title("Hollow").body("The line stops inside the ring."))
                            .entry(TimelineMarker.circle(14, DocumentColor.WHITE, ring), e -> e
                                    .title("Hollow, larger").body("And starts again below it."))
                            .entry(TimelineMarker.circle(20, DocumentColor.WHITE, ring), e -> e
                                    .title("Hollow, larger still").body("Whatever the diameter.")))
                    .build();

            VISUAL.assertMatchesBaseline("timeline-dsl/outlined-marker", session);
        }
    }

    /** The scene both extent baselines draw; they differ by this argument and nothing else. */
    private static DocumentSession extentScene(TimelineRailExtent extent) throws Exception {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 250)
                .margin(DocumentInsets.of(18))
                .create();
        session.pageFlow()
                .addTimeline(t -> t
                        .connector(RAIL, 1.5)
                        .spacing(16)
                        .rail(rail -> rail.extent(extent))
                        .entry(TimelineMarker.dot(9, INK), e -> e
                                .title("Tall entry").meta("2023 - Present")
                                .body("A body long enough to run to three lines on a page this "
                                      + "narrow, so that this entry is plainly the tallest here."))
                        .entry(TimelineMarker.dot(9, INK), e -> e.title("One line only"))
                        .entry(TimelineMarker.dot(9, INK), e -> e
                                .title("Middling").body("Two lines, more or less.")))
                .build();
        return session;
    }

    /** Long enough to take the first entry across two page breaks on a 150pt page. */
    private static String longBody() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            body.append("Sentence ").append(i).append(" of a body that keeps going. ");
        }
        return body.toString();
    }

    private static EllipseNode circle(double size, DocumentColor fill) {
        return new EllipseNode("marker", size, size, fill, null, null, null, null, null);
    }
}
