package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.PathNode;
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
import java.util.List;

import static com.demcha.compose.document.style.DocumentPathSegment.close;
import static com.demcha.compose.document.style.DocumentPathSegment.cubicTo;
import static com.demcha.compose.document.style.DocumentPathSegment.lineTo;
import static com.demcha.compose.document.style.DocumentPathSegment.moveTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the path primitive's range — a stroked Bézier wave, a filled blob
 * with overshooting control points, and a mixed line/curve ribbon closed for
 * filling — and writes a human-review PDF. Asserts the curves visibly paint
 * over a blank page.
 */
class PathPrimitiveDemoTest {

    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void pathPrimitivePaintsCurvesFillsAndMixedRibbons() throws Exception {
        byte[] pdf = sheet();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        BufferedImage page = VISUAL.renderPages(pdf).get(0);
        BufferedImage blank = VISUAL.renderPages(blankPage()).get(0);
        ImageDiff.Result diff = ImageDiff.compare(blank, page, 8);
        assertThat(diff.mismatchedPixelCount())
                .as("path curves, fills and ribbons must paint over the blank page (%s)", diff.summary())
                .isGreaterThan(500L);

        Path out = Path.of("target/visual-tests/path/path_primitive.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, pdf);
        javax.imageio.ImageIO.write(page, "png",
                out.resolveSibling("path_primitive.png").toFile());
    }

    private static byte[] blankPage() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 320)
                .margin(DocumentInsets.of(22))
                .create()) {
            document.pageFlow().name("Blank").build();
            return document.toPdfBytes();
        }
    }

    private static byte[] sheet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 320)
                .margin(DocumentInsets.of(22))
                .create()) {
            // Stroked S-wave — pure cubic Bézier spans, no tessellation.
            document.add(new PathNode("Wave", 316, 60,
                    List.of(moveTo(0.0, 0.5),
                            cubicTo(0.25, 1.1, 0.25, -0.1, 0.5, 0.5),
                            cubicTo(0.75, 1.1, 0.75, -0.1, 1.0, 0.5)),
                    null,
                    DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 2.4),
                    DocumentInsets.zero(), DocumentInsets.bottom(14)));

            // Filled blob — closed curves with overshooting control points.
            document.add(new PathNode("Blob", 110, 96,
                    List.of(moveTo(0.5, 1.0),
                            cubicTo(1.12, 0.94, 0.96, 0.08, 0.5, 0.0),
                            cubicTo(0.04, 0.08, -0.12, 0.94, 0.5, 1.0),
                            close()),
                    DocumentColor.rgb(235, 205, 160),
                    DocumentStroke.of(DocumentColor.rgb(140, 90, 30), 1.4),
                    DocumentInsets.zero(), DocumentInsets.bottom(14)));

            // Mixed ribbon — lines + curves in one closed, filled subpath.
            document.add(new PathNode("Ribbon", 316, 64,
                    List.of(moveTo(0.0, 0.85),
                            lineTo(0.18, 0.85),
                            cubicTo(0.42, 1.05, 0.58, 0.55, 0.82, 0.75),
                            lineTo(1.0, 0.75),
                            lineTo(1.0, 0.15),
                            cubicTo(0.6, -0.05, 0.4, 0.45, 0.0, 0.25),
                            close()),
                    DocumentColor.rgb(208, 226, 213),
                    DocumentStroke.of(DocumentColor.rgb(60, 110, 80), 1.2),
                    DocumentInsets.zero(), DocumentInsets.zero()));

            return document.toPdfBytes();
        }
    }
}
