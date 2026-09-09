package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
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
}
