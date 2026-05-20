package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end regression for R2 capacity tolerance.
 *
 * <p>{@link DocumentPageSize#A4} resolves to {@code 595.27563 × 841.88977}
 * points — the exact PDF point value for ISO A4. Pre-R2 a node authored
 * at the rounded human input {@code 842.0} pt failed the page-capacity
 * check with {@code "requires outer height 842.0 but page capacity is
 * 841.88977"}. The 0.11 pt mismatch (~0.04 mm — invisible) blocked
 * legitimate full-page background surfaces.</p>
 *
 * <p>After R2 the page-capacity check tolerates up to 0.5 pt of
 * rounding overhead, so the rendered PDF lands on disk and this test
 * passes. Output goes to
 * {@code target/visual-tests/page-capacity/PageCapacityToleranceDemo.pdf}
 * for manual review.</p>
 *
 * @author Artem Demchyshyn
 */
class PageCapacityToleranceDemoTest {

    @Test
    void fullA4HeightShapeWithRoundedHumanInputRendersWithoutCrash() throws Exception {
        Path output = VisualTestOutputs.preparePdf("PageCapacityToleranceDemo", "page-capacity");

        // No margin: inner height == DocumentPageSize.A4.height() ==
        // 841.88977. A shape sized at the human-friendly 842.0
        // overshoots by 0.11 pt and pre-R2 threw
        // AtomicNodeTooLargeException. The new CAPACITY_TOLERANCE
        // absorbs the rounding and lets the render complete.
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.zero())
                .create()) {

            document.add(new ShapeNode(
                    "FullPageBackground",
                    595.0,
                    842.0,
                    new Color(34, 70, 96),
                    DocumentStroke.of(DocumentColor.BLACK, 0),
                    DocumentInsets.zero(),
                    DocumentInsets.zero()));

            Files.write(output, document.toPdfBytes());
        }

        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes)
                .describedAs("Full-page background PDF should render to a real file")
                .hasSizeGreaterThan(500);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII))
                .describedAs("PDF must start with the %PDF- magic header")
                .isEqualTo("%PDF-");
    }
}
