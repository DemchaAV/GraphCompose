package com.demcha.compose.document.svg;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentInsets;
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
}
