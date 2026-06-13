package com.demcha.compose.document.svg;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentPathSegment.CubicTo;
import com.demcha.compose.document.style.DocumentPathSegment.LineTo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Icon-reader coverage: layer order and paints, SVG defaults and
 * inheritance, shape-to-path lowering, group transforms, viewBox handling,
 * the security posture (DOCTYPE refused), and the DSL bridge.
 */
class SvgIconTest {

    @Test
    void twoToneIconKeepsLayerOrderAndPaints() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 24 24">
                  <path d="M0 0 H24 V24 H0 Z" fill="#fde9e3"/>
                  <path d="M4 4 H20 V20 H4 Z" fill="rgb(196, 30, 58)" stroke="#333" stroke-width="2"/>
                </svg>
                """);

        assertThat(icon.layers()).hasSize(2);
        assertThat(icon.layers().get(0).fill().color()).isEqualTo(new java.awt.Color(253, 233, 227));
        assertThat(icon.layers().get(0).stroke()).isNull();
        SvgIcon.Layer top = icon.layers().get(1);
        assertThat(top.fill().color()).isEqualTo(new java.awt.Color(196, 30, 58));
        assertThat(top.stroke()).isNotNull();
        assertThat(top.stroke().width()).isEqualTo(2.0);
        assertThat(icon.aspectRatio()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void missingFillPaintsBlackAndNoneSkipsTheFill() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path d="M0 0 H10"/>
                  <path d="M0 5 H10" fill="none" stroke="#08f"/>
                </svg>
                """);

        assertThat(icon.layers().get(0).fill().color()).isEqualTo(new java.awt.Color(0, 0, 0));
        assertThat(icon.layers().get(1).fill()).isNull();
        assertThat(icon.layers().get(1).stroke()).isNotNull();
    }

    @Test
    void invisibleElementsProduceNoLayers() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path d="M0 0 H10" fill="none"/>
                  <path d="M0 5 H10" fill="#000"/>
                </svg>
                """);

        assertThat(icon.layers()).hasSize(1);
    }

    @Test
    void basicShapesLowerToPathGeometry() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 20 20">
                  <rect x="1" y="1" width="8" height="6" fill="#111"/>
                  <circle cx="14" cy="4" r="3" fill="#222"/>
                  <polygon points="2,12 8,12 5,18" fill="#333"/>
                  <line x1="10" y1="19" x2="19" y2="19" stroke="#444"/>
                </svg>
                """);

        assertThat(icon.layers()).hasSize(4);
        // The circle arrives as two arc-derived cubics.
        assertThat(icon.layers().get(1).geometry().segments().stream()
                .filter(CubicTo.class::isInstance).count()).isGreaterThanOrEqualTo(2);
        // The polygon closes; the line stays open stroke-only geometry.
        assertThat(icon.layers().get(3).geometry().segments().get(1)).isInstanceOf(LineTo.class);
    }

    @Test
    void groupTransformsAccumulateExactly() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 20 20">
                  <g transform="translate(2 2) scale(2)">
                    <path d="M0 0 H4" fill="#000"/>
                  </g>
                </svg>
                """);

        // (0,0)→(2,2) and (4,0)→(10,2): normalized x = 10/20, y flips.
        LineTo end = (LineTo) icon.layers().get(0).geometry().segments().get(1);
        assertThat(end.x()).isCloseTo(0.5, within(1e-9));
        assertThat(end.y()).isCloseTo(1.0 - 2.0 / 20.0, within(1e-9));
    }

    @Test
    void rotateAboutACenterMatchesTheSvgDefinition() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 20 20">
                  <path d="M10 0 H20" fill="#000" transform="rotate(90 10 10)"/>
                </svg>
                """);

        // rotate(90°, 10, 10) maps (10,0)→(20,10) and (20,0)→(20,20).
        LineTo end = (LineTo) icon.layers().get(0).geometry().segments().get(1);
        assertThat(end.x()).isCloseTo(1.0, within(1e-9));
        assertThat(end.y()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void groupPaintInheritsAndStyleAttributeWins() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <g fill="#0a0">
                    <path d="M0 0 H10"/>
                    <path d="M0 5 H10" style="fill: #00a"/>
                  </g>
                </svg>
                """);

        assertThat(icon.layers().get(0).fill().color()).isEqualTo(new java.awt.Color(0, 170, 0));
        assertThat(icon.layers().get(1).fill().color()).isEqualTo(new java.awt.Color(0, 0, 170));
    }

    @Test
    void widthHeightAttributesBackstopAMissingViewBox() {
        SvgIcon icon = SvgIcon.parse("""
                <svg width="48px" height="24px">
                  <path d="M0 0 H48 V24 Z" fill="#000"/>
                </svg>
                """);

        assertThat(icon.aspectRatio()).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void doctypeIsRefused() {
        assertThatThrownBy(() -> SvgIcon.parse("""
                <!DOCTYPE svg [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <svg viewBox="0 0 1 1"><path d="M0 0 H1" fill="#000"/></svg>
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not parseable");
    }

    @Test
    void contractViolationsThrowWithContext() {
        assertThatThrownBy(() -> SvgIcon.parse("<svg viewBox='0 0 10 10'/>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no drawable geometry");
        assertThatThrownBy(() -> SvgIcon.parse("<div/>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("root element");
        assertThatThrownBy(() -> SvgIcon.parse(
                "<svg viewBox='0 0 10 10'><path d='M0 0 H1' fill='magenta-ish'/></svg>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported SVG colour");
        assertThatThrownBy(() -> SvgIcon.parse(
                "<svg viewBox='0 0 10 10'><path d='M0 0 H1' fill='#000' transform='skewX(20)'/></svg>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported transform");
    }

    @Test
    void dslBridgeStacksLayersIntoTheFlow() throws Exception {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="11" fill="#fde9e3"/>
                  <path d="M6 6 H18 V18 H6 Z" fill="#c41e3a"/>
                </svg>
                """);

        try (DocumentSession document = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(12))
                .create()) {
            document.pageFlow(page -> page.addSvgIcon(icon, 48));

            assertThat(document.layoutGraph().nodes()).extracting(PlacedNode::path)
                    .anyMatch(path -> path.contains("SvgLayer0"))
                    .anyMatch(path -> path.contains("SvgLayer1"));
            byte[] pdf = document.toPdfBytes();
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }

    // ------------------------------------------------------------------
    // Gradients
    // ------------------------------------------------------------------

    @Test
    void userSpaceLinearGradientMapsToTheExactAxisInIconSpace() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs>
                    <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="10" y2="10">
                      <stop offset="0" stop-color="#A78BFA"/>
                      <stop offset="1" stop-color="#6128D9"/>
                    </linearGradient>
                  </defs>
                  <path d="M0 0 H10 V10 Z" fill="url(#g)"/>
                </svg>
                """);

        SvgIcon.Layer layer = icon.layers().get(0);
        DocumentPaint.LinearAxis axis = (DocumentPaint.LinearAxis) layer.fillPaint();
        // SVG user (0,0) is the top-left → normalized (0,1); (10,10) → (1,0).
        assertThat(axis.x0()).isCloseTo(0.0, within(1e-9));
        assertThat(axis.y0()).isCloseTo(1.0, within(1e-9));
        assertThat(axis.x1()).isCloseTo(1.0, within(1e-9));
        assertThat(axis.y1()).isCloseTo(0.0, within(1e-9));
        // The flat colour stays populated as the degradation target.
        assertThat(layer.fill().color()).isEqualTo(new java.awt.Color(167, 139, 250));
    }

    @Test
    void gradientStrokeKeepsWidthAndFirstStopFallback() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 96 96">
                  <defs>
                    <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="22" y1="24" x2="74" y2="72">
                      <stop offset="0" stop-color="#A78BFA"/>
                      <stop offset="1" stop-color="#6128D9"/>
                    </linearGradient>
                  </defs>
                  <path d="M68 24 L34 24" fill="none" stroke="url(#g)" stroke-width="7"/>
                </svg>
                """);

        SvgIcon.Layer layer = icon.layers().get(0);
        assertThat(layer.fillPaint()).isNull();
        assertThat(layer.fill()).isNull();
        assertThat(layer.strokePaint()).isInstanceOf(DocumentPaint.LinearAxis.class);
        assertThat(layer.stroke().width()).isEqualTo(7.0);
        assertThat(layer.stroke().color().color()).isEqualTo(new java.awt.Color(167, 139, 250));
    }

    @Test
    void boundingBoxGradientInterpolatesTheShapeBbox() {
        // Default gradientUnits=objectBoundingBox, default axis 0%,0% → 100%,0%.
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs>
                    <linearGradient id="g">
                      <stop offset="0" stop-color="#000"/>
                      <stop offset="1" stop-color="#fff"/>
                    </linearGradient>
                  </defs>
                  <rect x="2" y="2" width="6" height="4" fill="url(#g)"/>
                </svg>
                """);

        DocumentPaint.LinearAxis axis =
                (DocumentPaint.LinearAxis) icon.layers().get(0).fillPaint();
        // Bbox: x ∈ [0.2, 0.8]; SVG bbox-top y=2 → normalized 0.8.
        assertThat(axis.x0()).isCloseTo(0.2, within(1e-9));
        assertThat(axis.y0()).isCloseTo(0.8, within(1e-9));
        assertThat(axis.x1()).isCloseTo(0.8, within(1e-9));
        assertThat(axis.y1()).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void hrefSuppliesStopsAcrossOneHop() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs>
                    <linearGradient id="base">
                      <stop offset="0" stop-color="#111"/>
                      <stop offset="30%" stop-color="#555"/>
                      <stop offset="1" stop-color="#999"/>
                    </linearGradient>
                    <linearGradient id="g" href="#base" gradientUnits="userSpaceOnUse"
                                    x1="0" y1="5" x2="10" y2="5"/>
                  </defs>
                  <path d="M0 0 H10 V10 Z" fill="url(#g)"/>
                </svg>
                """);

        DocumentPaint.LinearAxis axis =
                (DocumentPaint.LinearAxis) icon.layers().get(0).fillPaint();
        assertThat(axis.stops()).hasSize(3);
        assertThat(axis.stops().get(1).offset()).isCloseTo(0.3, within(1e-9));
    }

    @Test
    void gradientTransformShiftsTheAxis() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs>
                    <linearGradient id="g" gradientUnits="userSpaceOnUse"
                                    x1="0" y1="0" x2="5" y2="0" gradientTransform="translate(5 0)">
                      <stop offset="0" stop-color="#000"/>
                      <stop offset="1" stop-color="#fff"/>
                    </linearGradient>
                  </defs>
                  <path d="M0 0 H10 V10 Z" fill="url(#g)"/>
                </svg>
                """);

        DocumentPaint.LinearAxis axis =
                (DocumentPaint.LinearAxis) icon.layers().get(0).fillPaint();
        assertThat(axis.x0()).isCloseTo(0.5, within(1e-9));
        assertThat(axis.x1()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void radialGradientMapsCentreAndRadius() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs>
                    <radialGradient id="g" gradientUnits="userSpaceOnUse" cx="5" cy="5" r="5">
                      <stop offset="0" stop-color="#fff"/>
                      <stop offset="1" stop-color="#000"/>
                    </radialGradient>
                  </defs>
                  <circle cx="5" cy="5" r="5" fill="url(#g)"/>
                </svg>
                """);

        DocumentPaint.RadialCircle circle =
                (DocumentPaint.RadialCircle) icon.layers().get(0).fillPaint();
        assertThat(circle.cx()).isCloseTo(0.5, within(1e-9));
        assertThat(circle.cy()).isCloseTo(0.5, within(1e-9));
        assertThat(circle.r()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void gradientCornersWithoutAPdfAnalogueFailLoudly() {
        String defs = """
                <svg viewBox="0 0 10 10">
                  <defs>%s</defs>
                  <path d="M0 0 H10 V10 Z" fill="url(#g)"/>
                </svg>
                """;

        assertThatThrownBy(() -> SvgIcon.parse(defs.formatted(
                "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("references no");
        assertThatThrownBy(() -> SvgIcon.parse(defs.formatted("""
                <linearGradient id="g" spreadMethod="reflect">
                  <stop offset="0" stop-color="#000"/><stop offset="1" stop-color="#fff"/>
                </linearGradient>""")))
                .hasMessageContaining("spreadMethod");
        assertThatThrownBy(() -> SvgIcon.parse(defs.formatted("""
                <linearGradient id="g">
                  <stop offset="0" stop-color="#000" stop-opacity="0.5"/>
                  <stop offset="1" stop-color="#fff"/>
                </linearGradient>""")))
                .hasMessageContaining("stop-opacity");
        assertThatThrownBy(() -> SvgIcon.parse(defs.formatted("""
                <radialGradient id="g" fx="0.2" fy="0.2">
                  <stop offset="0" stop-color="#000"/><stop offset="1" stop-color="#fff"/>
                </radialGradient>""")))
                .hasMessageContaining("focal");
    }

    @Test
    void malformedGradientNumberSaysWhatExpectedANumber() {
        // A non-numeric gradient coordinate must read in the reader's house
        // style (named + reason) instead of leaking a bare JDK parse message.
        assertThatThrownBy(() -> SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs>
                    <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="abc" y1="0" x2="10" y2="10">
                      <stop offset="0" stop-color="#000"/><stop offset="1" stop-color="#fff"/>
                    </linearGradient>
                  </defs>
                  <path d="M0 0 H10 V10 Z" fill="url(#g)"/>
                </svg>
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a number")
                .hasMessageContaining("abc");
    }

    @Test
    void nodeFormPackagesLayersAtTheRequestedWidth() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 48 24">
                  <path d="M0 0 H48 V24 Z" fill="#123456"/>
                </svg>
                """);

        var stack = icon.node(96);
        assertThat(stack.layers()).hasSize(1);
        var path = (com.demcha.compose.document.node.PathNode) stack.layers().get(0).node();
        assertThat(path.width()).isEqualTo(96.0);
        assertThat(path.height()).isCloseTo(48.0, within(1e-9));
        assertThatThrownBy(() -> icon.node(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    // ------------------------------------------------------------------
    // Stroke style + scaling
    // ------------------------------------------------------------------

    @Test
    void strokeStyleAttributesReachTheLayer() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 20 20">
                  <path d="M0 10 H20" fill="none" stroke="#08f" stroke-width="3"
                        stroke-linecap="round" stroke-linejoin="bevel" stroke-dasharray="4 2"/>
                </svg>
                """);

        SvgIcon.Layer layer = icon.layers().get(0);
        assertThat(layer.lineCap()).isEqualTo(com.demcha.compose.document.style.DocumentLineCap.ROUND);
        assertThat(layer.lineJoin()).isEqualTo(com.demcha.compose.document.style.DocumentLineJoin.BEVEL);
        assertThat(layer.dashArray()).containsExactly(4.0, 2.0);
        // Stroke width stays in user units on the layer.
        assertThat(layer.stroke().width()).isEqualTo(3.0);
    }

    @Test
    void groupStrokeStyleInheritsToChildren() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 20 20">
                  <g stroke="#000" stroke-width="2" stroke-linecap="square">
                    <path d="M0 5 H20" fill="none"/>
                  </g>
                </svg>
                """);

        assertThat(icon.layers().get(0).lineCap())
                .isEqualTo(com.demcha.compose.document.style.DocumentLineCap.SQUARE);
    }

    @Test
    void nodeFormScalesStrokeWidthAndDashWithTheGeometry() {
        // 100-unit frame rendered at 25 pt → 0.25× scale.
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 100 100">
                  <path d="M0 50 H100" fill="none" stroke="#000" stroke-width="8" stroke-dasharray="12 4"/>
                </svg>
                """);

        var path = (com.demcha.compose.document.node.PathNode) icon.node(25).layers().get(0).node();
        assertThat(path.stroke().width()).isCloseTo(2.0, within(1e-9));
        assertThat(path.dashPattern().segments()).containsExactly(3.0, 1.0);
        assertThat(path.lineCap()).isNotNull();
    }

    @Test
    void pxAndPtStrokeWidthsParse() {
        SvgIcon px = SvgIcon.parse("""
                <svg viewBox="0 0 10 10"><path d="M0 0 H10" fill="none" stroke="#000" stroke-width="2px"/></svg>
                """);
        assertThat(px.layers().get(0).stroke().width()).isEqualTo(2.0);

        SvgIcon pt = SvgIcon.parse("""
                <svg viewBox="0 0 10 10"><path d="M0 0 H10" fill="none" stroke="#000" stroke-width="72pt"/></svg>
                """);
        assertThat(pt.layers().get(0).stroke().width()).isCloseTo(96.0, within(1e-9));
    }

    @Test
    void namedAndRgbaColoursResolveOnShapes() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path d="M0 0 H10" fill="rebeccapurple"/>
                  <path d="M0 5 H10" fill="rgba(20, 80, 95, 0.5)"/>
                </svg>
                """);

        assertThat(icon.layers().get(0).fill().color()).isEqualTo(new java.awt.Color(102, 51, 153));
        assertThat(icon.layers().get(1).fill().color().getAlpha()).isEqualTo(128);
    }

    @Test
    void unsupportedContentElementsAreSkippedButGeometrySurvives() {
        // <text> has no vector analogue; it is dropped (with a one-time log
        // warning) while the path beside it still renders.
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 20 20">
                  <text x="2" y="10" fill="#000">Hi</text>
                  <path d="M0 0 H20 V20 Z" fill="#123456"/>
                </svg>
                """);

        assertThat(icon.layers()).hasSize(1);
        assertThat(icon.layers().get(0).fill().color()).isEqualTo(new java.awt.Color(18, 52, 86));
    }
}
