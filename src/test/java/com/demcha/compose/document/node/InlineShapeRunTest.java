package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class InlineShapeRunTest {

    private static final double EPS = 1e-6;
    private static final DocumentColor FILL = DocumentColor.of(new Color(40, 90, 180));
    private static final DocumentStroke STROKE = DocumentStroke.of(DocumentColor.BLACK, 0.5);

    @Test
    void filledShapeConvenienceConstructorKeepsOutlineAndDefaults() {
        InlineShapeRun run = new InlineShapeRun(ShapeOutline.circle(6.0), FILL);

        assertThat(run.outline()).isEqualTo(ShapeOutline.circle(6.0));
        assertThat(run.outline().width()).isEqualTo(6.0, within(EPS));
        assertThat(run.fill()).isSameAs(FILL);
        assertThat(run.stroke()).isNull();
        assertThat(run.alignment()).isEqualTo(InlineImageAlignment.CENTER);
        assertThat(run.baselineOffset()).isEqualTo(0.0, within(EPS));
        assertThat(run.linkOptions()).isNull();
    }

    @Test
    void carriesAnyOutlineKind() {
        assertThat(new InlineShapeRun(ShapeOutline.diamond(8, 8), FILL).outline())
                .isInstanceOf(ShapeOutline.Polygon.class);
        assertThat(new InlineShapeRun(ShapeOutline.star(8, 8), FILL).outline())
                .isInstanceOf(ShapeOutline.Polygon.class);
        assertThat(new InlineShapeRun(new ShapeOutline.Rectangle(8, 4), FILL).outline())
                .isInstanceOf(ShapeOutline.Rectangle.class);
    }

    @Test
    void outlinedOnlyShapeIsAllowed() {
        InlineShapeRun run = new InlineShapeRun(ShapeOutline.circle(8), null, STROKE, null, 0.0, null);

        assertThat(run.fill()).isNull();
        assertThat(run.stroke()).isSameAs(STROKE);
        assertThat(run.alignment()).isEqualTo(InlineImageAlignment.CENTER);
    }

    @Test
    void nullAlignmentNormalizesToCenter() {
        InlineShapeRun run = new InlineShapeRun(ShapeOutline.circle(5), FILL, null, null, 0.0, null);

        assertThat(run.alignment()).isEqualTo(InlineImageAlignment.CENTER);
    }

    @Test
    void invisibleShapeWithoutFillOrStrokeIsRejected() {
        assertThatThrownBy(() ->
                new InlineShapeRun(ShapeOutline.circle(6), null, null, InlineImageAlignment.CENTER, 0.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fill");
    }

    @Test
    void nullOutlineIsRejected() {
        assertThatThrownBy(() -> new InlineShapeRun(null, FILL))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nonFiniteBaselineOffsetIsRejected() {
        assertThatThrownBy(() ->
                new InlineShapeRun(ShapeOutline.circle(6), FILL, null, InlineImageAlignment.CENTER,
                        Double.POSITIVE_INFINITY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baselineOffset");
    }

    @Test
    void filledConvenienceConstructorRejectsNullFill() {
        assertThatThrownBy(() -> new InlineShapeRun(ShapeOutline.circle(6), null))
                .isInstanceOf(NullPointerException.class);
    }
}
