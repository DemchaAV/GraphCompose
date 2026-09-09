package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

/**
 * Freezes the geometry the timeline DSL renders today, exactly.
 *
 * <p>The rail is about to stop being a per-entry left border and become one logical
 * axis anchored to resolved markers. Nothing pinned the current output: the DSL had
 * three node-level tests and a blank-page pixel smoke, and the {@code timeline_minimal}
 * baselines in this repository belong to the CV preset, which renders through
 * {@code TimelineAxisWidget} and never calls {@code addTimeline}.</p>
 *
 * <p>A node-level test and an exact snapshot answer different questions and the rework
 * needs both: the node test says the builder still sets the gutter, the snapshot says
 * the gutter still lands where it did. A 3 pt drift passes the first and fails the
 * second.</p>
 */
class TimelineLayoutSnapshotTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    @Test
    void classicTimelineGeometryIsPinned() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(14)
                            .entry(TimelineMarker.dot(8, INK), e -> e
                                    .title("Senior Engineer")
                                    .meta("2023 - Present")
                                    .body("Led the layout engine rewrite."))
                            .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE), e -> e
                                    .title("Engineer")
                                    .meta("2021 - 2023")
                                    .body("Shipped the pagination compiler."))
                            .entry(TimelineMarker.square(8, INK), e -> e
                                    .title("Junior Engineer")
                                    .meta("2019 - 2021")))
                    .build();

            LayoutSnapshotAssertions.assertMatches(session, "document/timeline_classic");
        }
    }

    @Test
    void aTimelineThatCrossesAPageBoundaryIsPinnedToo() throws Exception {
        // The rail's page behaviour is the part the rework is most likely to move, so
        // the freeze has to cover a timeline that actually paginates — today three
        // separate borders happen to abut across the break.
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            body.append("Sentence ").append(i).append(" of a body long enough to run on. ");
        }

        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 170)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .entry(TimelineMarker.dot(8, INK), e -> e
                                    .title("First").meta("2023").body(body.toString()))
                            .entry(TimelineMarker.dot(8, INK), e -> e
                                    .title("Second").meta("2021").body("Short.")))
                    .build();

            LayoutSnapshotAssertions.assertMatches(session, "document/timeline_paginated");
        }
    }
}
