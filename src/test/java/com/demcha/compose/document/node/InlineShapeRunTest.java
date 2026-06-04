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
        ShapeLayer layer = run.layers().get(0);

        assertThat(run.layers()).hasSize(1);
        assertThat(layer.outline()).isEqualTo(ShapeOutline.circle(6.0));
        assertThat(layer.outline().width()).isEqualTo(6.0, within(EPS));
        assertThat(layer.fill()).isSameAs(FILL);
        assertThat(layer.stroke()).isNull();
        assertThat(run.alignment()).isEqualTo(InlineImageAlignment.CENTER);
        assertThat(run.baselineOffset()).isEqualTo(0.0, within(EPS));
        assertThat(run.linkOptions()).isNull();
    }

    @Test
    void carriesAnyOutlineKind() {
        assertThat(new InlineShapeRun(ShapeOutline.diamond(8, 8), FILL).layers().get(0).outline())
                .isInstanceOf(ShapeOutline.Polygon.class);
        assertThat(new InlineShapeRun(ShapeOutline.star(8, 8), FILL).layers().get(0).outline())
                .isInstanceOf(ShapeOutline.Polygon.class);
        assertThat(new InlineShapeRun(new ShapeOutline.Rectangle(8, 4), FILL).layers().get(0).outline())
                .isInstanceOf(ShapeOutline.Rectangle.class);
    }

    @Test
    void outlinedOnlyShapeIsAllowed() {
        InlineShapeRun run = new InlineShapeRun(ShapeOutline.circle(8), null, STROKE, null, 0.0, null);
        ShapeLayer layer = run.layers().get(0);

        assertThat(layer.fill()).isNull();
        assertThat(layer.stroke()).isSameAs(STROKE);
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

    @Test
    void checkedCheckboxStacksFrameAndMarkLayers() {
        InlineShapeRun box = InlineShapeRun.checkbox(12, true, DocumentColor.BLACK, FILL);

        assertThat(box.layers()).hasSize(2);
        ShapeLayer frame = box.layers().get(0);
        assertThat(frame.outline()).isInstanceOf(ShapeOutline.RoundedRectangle.class);
        assertThat(frame.fill()).isNull();
        assertThat(frame.stroke()).isNotNull();
        ShapeLayer mark = box.layers().get(1);
        assertThat(mark.outline()).isInstanceOf(ShapeOutline.Polygon.class);
        assertThat(mark.fill()).isSameAs(FILL);
        assertThat(box.width()).isEqualTo(12.0, within(EPS));
        assertThat(box.height()).isEqualTo(12.0, within(EPS));
    }

    @Test
    void uncheckedCheckboxIsFrameOnly() {
        InlineShapeRun box = InlineShapeRun.checkbox(12, false, DocumentColor.BLACK, FILL);

        assertThat(box.layers()).hasSize(1);
        assertThat(box.layers().get(0).fill()).isNull();
        assertThat(box.layers().get(0).stroke()).isNotNull();
    }

    @Test
    void defaultCheckboxUsesClassicTick() {
        ShapeOutline preset = InlineShapeRun.checkbox(12, true, DocumentColor.BLACK, FILL)
                .layers().get(1).outline();
        ShapeOutline classic = InlineShapeRun.checkbox(12, true, ShapeOutline.CheckmarkStyle.CLASSIC,
                        DocumentColor.BLACK, FILL)
                .layers().get(1).outline();

        assertThat(((ShapeOutline.Polygon) preset).points())
                .isEqualTo(((ShapeOutline.Polygon) classic).points());
    }

    @Test
    void checkboxStyleOverloadSwapsTheTickDesign() {
        ShapeOutline classic = InlineShapeRun.checkbox(12, true, ShapeOutline.CheckmarkStyle.CLASSIC,
                        DocumentColor.BLACK, FILL)
                .layers().get(1).outline();
        ShapeOutline heavy = InlineShapeRun.checkbox(12, true, ShapeOutline.CheckmarkStyle.HEAVY,
                        DocumentColor.BLACK, FILL)
                .layers().get(1).outline();

        assertThat(((ShapeOutline.Polygon) heavy).points())
                .isNotEqualTo(((ShapeOutline.Polygon) classic).points());
    }

    @Test
    void rawMarkCheckboxUsesGivenOutlineAndGuardsNullWhenChecked() {
        ShapeOutline mark = ShapeOutline.plus(7, 7);
        InlineShapeRun box = InlineShapeRun.checkbox(12, true, mark, DocumentColor.BLACK, FILL);
        assertThat(box.layers().get(1).outline()).isSameAs(mark);

        assertThatThrownBy(() ->
                InlineShapeRun.checkbox(12, true, (ShapeOutline) null, DocumentColor.BLACK, FILL))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mark");

        // An unchecked box ignores the mark, so a null mark is tolerated.
        assertThat(InlineShapeRun.checkbox(12, false, (ShapeOutline) null, DocumentColor.BLACK, FILL).layers())
                .hasSize(1);
    }

    @Test
    void checkboxStyleOverloadRejectsNullStyle() {
        assertThatThrownBy(() ->
                InlineShapeRun.checkbox(12, true, (ShapeOutline.CheckmarkStyle) null, DocumentColor.BLACK, FILL))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("markStyle");
    }
}
