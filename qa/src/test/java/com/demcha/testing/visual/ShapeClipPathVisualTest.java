package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic visual check that a {@code ShapeOutline.Path} clip actually
 * shapes the rendered pixels — the gap the {@code %PDF-} smoke tests cannot
 * cover. A full-box red layer clipped to an upward triangle must paint roughly
 * half the box (a triangle is half its bounding box) with the wide base at the
 * BOTTOM. So a dropped clip (≈100% red), an empty clip (≈0% red), a wrong scale,
 * and a y-flip (apex/base swapped) all fail this test.
 *
 * <p>Self-contained on purpose: it samples the in-memory render instead of
 * diffing a committed PNG baseline, so it stays deterministic and
 * anti-aliasing-tolerant across platforms without a blessed pixel reference.</p>
 */
class ShapeClipPathVisualTest {

    private static final int BOX = 120;

    @Test
    void pathClipShapesTheRenderedPixelsRightSideUp() throws Exception {
        BufferedImage page = PdfVisualRegression.standard()
                .renderPages(renderTriangleClip())
                .get(0);

        int w = page.getWidth();
        int h = page.getHeight();
        long red = 0;
        long redTop = 0;
        long redBottom = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isRed(page.getRGB(x, y))) {
                    red++;
                    if (y < h / 2) {
                        redTop++;
                    } else {
                        redBottom++;
                    }
                }
            }
        }

        double fraction = (double) red / ((long) w * h);
        assertThat(fraction)
                .as("clipped red triangle should cover ~half the box "
                        + "(clip applied, right scale) — got %.3f", fraction)
                .isBetween(0.30, 0.70);
        assertThat(redBottom)
                .as("wide base at the bottom, narrow apex at the top — guards a y-flip")
                .isGreaterThan(redTop * 2);
    }

    /** A 120x120 container clipping a full-box red layer to an upward triangle. */
    private static byte[] renderTriangleClip() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(BOX, BOX)
                .margin(DocumentInsets.zero())
                .create()) {
            session.add(new ShapeContainerBuilder()
                    .name("TriangleClip")
                    .path(BOX, BOX, List.of(
                            DocumentPathSegment.moveTo(0.5, 1.0),   // apex, top (y grows up)
                            DocumentPathSegment.lineTo(1.0, 0.0),   // base, bottom-right
                            DocumentPathSegment.lineTo(0.0, 0.0),   // base, bottom-left
                            DocumentPathSegment.close()))
                    .clipPolicy(ClipPolicy.CLIP_PATH)
                    .layer(new ShapeBuilder()
                            .size(BOX, BOX)
                            .fillColor(DocumentColor.rgb(220, 20, 20))
                            .build())
                    .build());
            return session.toPdfBytes();
        }
    }

    private static boolean isRed(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r > 140 && g < 110 && b < 110;
    }
}
