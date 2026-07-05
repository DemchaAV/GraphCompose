package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link LineBuilder#fill()} stretches a line to the width available where it is
 * placed (its row slot or the content width) instead of its authored width — the
 * flex line behind a dot leader. A non-fill line keeps its fixed width.
 */
class LineFillTest {

    @Test
    void fillLineStretchesToItsRowSlotWhileAFixedLineKeepsItsWidth() {
        double filled = lineWidthInTwoEqualColumns(true);
        double fixed = lineWidthInTwoEqualColumns(false);

        // inner width = 240 - 2*20 = 200; gap 0; two equal weights => 100pt slots.
        assertThat(filled).isCloseTo(100.0, within(0.5));
        // A non-fill line keeps its authored width (LineBuilder default 1.0).
        assertThat(fixed).isCloseTo(1.0, within(0.5));
    }

    @Test
    void fillLineSpansTheContentWidthAtFlowLevel() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addLine(l -> l.name("rule").height(2).fill()));
            assertThat(placedWidth(session, "rule")).isCloseTo(200.0, within(0.5));
        }
    }

    @Test
    void fillIsANoOpForAVerticalLine() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            // Stretching the end point would change the slope, so fill leaves a
            // vertical line at its authored (stroke-width) box instead of 200pt.
            session.pageFlow(page -> page.addLine(l -> l.name("rule").vertical(30).fill()));
            assertThat(placedWidth(session, "rule")).isLessThan(20.0);
        }
    }

    private double lineWidthInTwoEqualColumns(boolean fill) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addRow(r -> {
                r.gap(0).weights(1, 1);
                r.addLine(l -> {
                    l.name("rule").height(2);
                    if (fill) {
                        l.fill();
                    }
                });
                r.addParagraph("X");
            }));
            return placedWidth(session, "rule");
        }
    }

    private double placedWidth(DocumentSession session, String name) {
        PlacedNode node = session.layoutGraph().nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " not placed"));
        return node.placementWidth();
    }
}
