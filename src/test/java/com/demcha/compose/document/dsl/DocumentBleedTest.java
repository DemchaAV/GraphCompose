package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentBleed;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Placement contract for {@link DocumentBleed}: a bled section's fill reaches the
 * trimmed page edge, while an identical non-bled section stays inside the content
 * margin (the byte-identical baseline).
 */
class DocumentBleedTest {

    private static final double EPS = 1e-3;
    private static final int PAGE_W = 400;
    private static final int PAGE_H = 300;
    private static final int MARGIN = 20;
    private static final Color BAND = new Color(11, 22, 33);

    @Test
    void bledSectionFillReachesPageEdges() {
        PlacedFragment fill = bandFill(DocumentBleed.of(DocumentEdge.LEFT, DocumentEdge.RIGHT, DocumentEdge.TOP));

        assertThat(fill.x()).as("left edge").isEqualTo(0.0, within(EPS));
        assertThat(fill.width()).as("full page width").isEqualTo(PAGE_W, within(EPS));
        assertThat(fill.y() + fill.height()).as("top edge").isEqualTo(PAGE_H, within(EPS));
    }

    @Test
    void nonBledSectionFillStaysInsideContentMargin() {
        PlacedFragment fill = bandFill(DocumentBleed.none());

        // The fill begins at the content-area left edge and never crosses any
        // page margin — the byte-identical baseline a bled fill departs from.
        assertThat(fill.x()).as("left content edge").isEqualTo(MARGIN, within(EPS));
        assertThat(fill.x() + fill.width()).as("right stays inside margin")
                .isLessThanOrEqualTo(PAGE_W - MARGIN + EPS);
        assertThat(fill.y() + fill.height()).as("top stays inside margin")
                .isLessThanOrEqualTo(PAGE_H - MARGIN + EPS);
    }

    /** Compiles a single coloured band with the given bleed and returns its fill fragment. */
    private PlacedFragment bandFill(DocumentBleed bleed) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_W, PAGE_H)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {

            SectionNode band = new SectionBuilder()
                    .name("Band")
                    .fillColor(DocumentColor.of(BAND))
                    .bleed(bleed)
                    .addParagraph("Masthead")
                    .build();
            session.add(band);

            LayoutGraph graph = session.layoutGraph();
            return graph.fragments().stream()
                    .filter(this::isBandFill)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no band fill fragment emitted"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isBandFill(PlacedFragment fragment) {
        return fragment.payload() instanceof ShapeFragmentPayload payload
                && payload.fillColor() != null
                && payload.fillColor().getRGB() == BAND.getRGB();
    }

    @Test
    void factoriesProduceExpectedEdgeSets() {
        assertThat(DocumentBleed.none().any()).isFalse();
        assertThat(DocumentBleed.all().any()).isTrue();
        for (DocumentEdge edge : DocumentEdge.values()) {
            assertThat(DocumentBleed.all().bleeds(edge)).as("all bleeds %s", edge).isTrue();
        }

        DocumentBleed sides = DocumentBleed.of(DocumentEdge.LEFT, DocumentEdge.RIGHT);
        assertThat(sides.bleeds(DocumentEdge.LEFT)).isTrue();
        assertThat(sides.bleeds(DocumentEdge.RIGHT)).isTrue();
        assertThat(sides.bleeds(DocumentEdge.TOP)).isFalse();
        assertThat(sides.bleeds(DocumentEdge.BOTTOM)).isFalse();

        assertThat(DocumentBleed.of()).isEqualTo(DocumentBleed.none());
        assertThat(new DocumentBleed(null).any()).isFalse();
    }
}
