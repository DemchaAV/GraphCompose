package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page geometry is a value: a layout graph carries it, and two layouts laid out alike are equal
 * and print alike.
 */
class PageGeometryTest {

    private static final LayoutCanvas CANVAS = LayoutCanvas.from(400, 600, Margin.of(36));

    @Test
    void twoResolversOfTheSameCanvasAndOverridesAreEqual() {
        PageGeometry one = PageGeometry.of(CANVAS, List.of(new PageMarginOverride(1, Integer.MAX_VALUE,
                new Margin(36, 36, 136, 36))));
        PageGeometry other = PageGeometry.of(CANVAS, List.of(new PageMarginOverride(1, Integer.MAX_VALUE,
                new Margin(36, 36, 136, 36))));

        assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
        assertThat(one.toString()).isEqualTo(other.toString());
        assertThat(one).isNotEqualTo(PageGeometry.of(CANVAS, List.of()));
    }

    @Test
    void aLayoutGraphGivenNoGeometryHasTheCanvasMarginOnEveryPage() {
        LayoutGraph graph = new LayoutGraph(CANVAS, 2, List.of(), List.of());

        assertThat(graph.pageGeometry()).isEqualTo(PageGeometry.of(CANVAS, List.of()));
        assertThat(graph.pageGeometry().marginForPage(1)).isEqualTo(CANVAS.margin());
    }
}
