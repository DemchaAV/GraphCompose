package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.testing.visual.ImageDiff;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders a semantic timeline (dot / numbered / outlined-circle markers paired
 * with title + meta + body) and writes a human-review PDF. Asserts it renders a
 * valid, non-empty document.
 */
class TimelineDemoTest {

    private static final DocumentColor ACCENT = DocumentColor.rgb(40, 90, 120);
    private static final DocumentColor RULE = DocumentColor.rgb(118, 128, 146);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 250, 246);
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void timelineRendersMarkersAndRail() throws Exception {
        byte[] pdf = sheet();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        BufferedImage page = VISUAL.renderPages(pdf).get(0);
        BufferedImage blank = VISUAL.renderPages(blankPage()).get(0);
        ImageDiff.Result diff = ImageDiff.compare(blank, page, 8);
        assertThat(diff.mismatchedPixelCount())
                .as("the timeline must paint markers, rails and text over the blank page (%s)", diff.summary())
                .isGreaterThan(500L);

        Path out = Path.of("target/visual-tests/timeline/timeline.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, pdf);
        javax.imageio.ImageIO.write(page, "png",
                out.resolveSibling("timeline.png").toFile());
    }

    private static byte[] blankPage() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 320)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(22))
                .create()) {
            document.pageFlow().name("Blank").build();
            return document.toPdfBytes();
        }
    }

    private static byte[] sheet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 320)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(22))
                .create()) {
            document.pageFlow()
                    .name("TimelineSheet")
                    .addTimeline(t -> t
                            .connector(RULE, 2.5)
                            .entry(TimelineMarker.dot(9, ACCENT), e -> e
                                    .title("Senior Engineer")
                                    .meta("2021 - present")
                                    .body("Led the rendering pipeline rewrite and mentored three engineers."))
                            .entry(TimelineMarker.numbered(2, 16, ACCENT, DocumentColor.WHITE), e -> e
                                    .title("Engineer")
                                    .meta("2019 - 2021")
                                    .body("Shipped the layout engine and the table system."))
                            .entry(TimelineMarker.circle(10, DocumentColor.WHITE, DocumentStroke.of(ACCENT, 1.4)), e -> e
                                    .title("Junior Engineer")
                                    .meta("2017 - 2019")
                                    .body("Built internal tooling and CI automation.")))
                    .build();
            return document.toPdfBytes();
        }
    }
}
