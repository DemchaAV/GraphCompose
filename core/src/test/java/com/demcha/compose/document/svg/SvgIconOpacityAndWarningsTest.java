package com.demcha.compose.document.svg;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.demcha.compose.document.style.DocumentPaint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reader coverage for the opacity family ({@code opacity} /
 * {@code fill-opacity} / {@code stroke-opacity}, multiplied down the tree
 * into per-layer paint alpha) and the two degradation signals: the
 * {@code fill-rule="evenodd"} approximation warning and the
 * unknown-element tally that keeps a mask or pattern from vanishing
 * silently.
 */
class SvgIconOpacityAndWarningsTest {

    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void captureWarnings() {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SvgIconReader.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void releaseAppender() {
        logger.detachAppender(appender);
    }

    private List<String> warnings() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    // ------------------------------------------------------------------
    // Opacity family
    // ------------------------------------------------------------------

    @Test
    void fillOpacityScalesTheFlatFillAlpha() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff0000" fill-opacity="0.5"/>
                </svg>""");

        assertThat(icon.layers()).hasSize(1);
        assertThat(icon.layers().get(0).fill().color().getAlpha()).isEqualTo(128);
    }

    @Test
    void strokeOpacityScalesTheStrokeAlphaOnly() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path d="M0 0 L10 10" fill="none" stroke="#000000"
                        stroke-width="2" stroke-opacity="0.25"/>
                </svg>""");

        SvgIcon.Layer layer = icon.layers().get(0);
        assertThat(layer.fill()).isNull();
        assertThat(layer.stroke().color().color().getAlpha()).isEqualTo(64);
    }

    @Test
    void groupOpacityMultipliesIntoDescendantPaint() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <g opacity="0.5">
                    <rect width="10" height="10" fill="#ff0000" fill-opacity="0.5"/>
                  </g>
                </svg>""");

        assertThat(icon.layers().get(0).fill().color().getAlpha()).isEqualTo(64);
    }

    @Test
    void opacityMultipliesAlphaTheColourAlreadyCarries() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff000080" fill-opacity="0.5"/>
                </svg>""");

        // 8-digit hex alpha 0x80 (128/255) times fill-opacity 0.5 → 64.
        assertThat(icon.layers().get(0).fill().color().getAlpha()).isEqualTo(64);
    }

    @Test
    void percentageOpacityIsAccepted() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff0000" fill-opacity="50%"/>
                </svg>""");

        assertThat(icon.layers().get(0).fill().color().getAlpha()).isEqualTo(128);
    }

    @Test
    void zeroFillOpacityHidesTheFillButKeepsTheStroke() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff0000" fill-opacity="0"
                        stroke="#000000" stroke-width="1"/>
                </svg>""");

        SvgIcon.Layer layer = icon.layers().get(0);
        assertThat(layer.fill()).isNull();
        assertThat(layer.stroke()).isNotNull();
    }

    @Test
    void zeroOpacityDropsTheLayerEntirely() {
        assertThatThrownBy(() -> SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff0000" opacity="0"/>
                </svg>"""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no drawable geometry");
    }

    @Test
    void partialOpacityOnAGradientFillPaintsOpaqueAndWarns() {
        // The DocumentPaint contract refuses translucent stops (shadings carry
        // no alpha), so a partial opacity cannot reach a gradient fill — the
        // gradient stays opaque and the degradation is said once.
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <linearGradient id="g">
                    <stop offset="0" stop-color="#ff0000"/>
                    <stop offset="1" stop-color="#0000ff"/>
                  </linearGradient>
                  <rect width="10" height="10" fill="url(#g)" fill-opacity="0.5"/>
                </svg>""");

        SvgIcon.Layer layer = icon.layers().get(0);
        DocumentPaint.LinearAxis paint = (DocumentPaint.LinearAxis) layer.fillPaint();
        assertThat(paint.stops()).allSatisfy(stop ->
                assertThat(stop.color().color().getAlpha()).isEqualTo(255));
        assertThat(warnings()).anyMatch(message ->
                message.contains("opacity on a gradient fill"));
    }

    @Test
    void zeroOpacityHidesAGradientFillEntirely() {
        assertThatThrownBy(() -> SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <linearGradient id="g">
                    <stop offset="0" stop-color="#ff0000"/>
                    <stop offset="1" stop-color="#0000ff"/>
                  </linearGradient>
                  <rect width="10" height="10" fill="url(#g)" fill-opacity="0"/>
                </svg>"""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no drawable geometry");
    }

    @Test
    void malformedOpacityNamesTheAttributeAndTheElement() {
        assertThatThrownBy(() -> SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff0000" fill-opacity="solid"/>
                </svg>"""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fill-opacity")
                .hasMessageContaining("must be a number")
                .hasMessageContaining("<rect");
    }

    // ------------------------------------------------------------------
    // fill-rule
    // ------------------------------------------------------------------

    @Test
    void evenOddFillWarnsOncePerIconAndStillRenders() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <path d="M0 0 H10 V10 H0 Z M2 2 H8 V8 H2 Z" fill-rule="evenodd"/>
                  <circle cx="5" cy="5" r="2" fill-rule="evenodd"/>
                </svg>""");

        assertThat(icon.layers()).hasSize(2);
        List<String> warned = warnings().stream()
                .filter(message -> message.contains("fill-rule=evenodd")).toList();
        assertThat(warned).hasSize(1);
        assertThat(warned.get(0)).contains("approximated");
    }

    @Test
    void nonZeroAndInheritFillRulesStayQuiet() {
        SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill-rule="nonzero"/>
                  <circle cx="5" cy="5" r="2" fill-rule="inherit"/>
                </svg>""");

        assertThat(warnings()).noneMatch(message -> message.contains("fill-rule"));
    }

    @Test
    void unknownFillRuleIsRefusedWithTheAlternatives() {
        assertThatThrownBy(() -> SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill-rule="winding"/>
                </svg>"""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported fill-rule 'winding'");
    }

    // ------------------------------------------------------------------
    // Unknown-element tally
    // ------------------------------------------------------------------

    @Test
    void unknownElementsAreWarnedInsteadOfVanishing() {
        SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <mask id="m"><rect width="10" height="10"/></mask>
                  <rect width="10" height="10" fill="#ff0000"/>
                </svg>""");

        assertThat(warnings()).anyMatch(message ->
                message.contains("skipped unsupported element(s)") && message.contains("mask"));
    }

    @Test
    void unknownElementsAreNamedWhenTheIconEndsUpEmpty() {
        assertThatThrownBy(() -> SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <pattern id="p"><rect width="2" height="2"/></pattern>
                </svg>"""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no drawable geometry")
                .hasMessageContaining("skipped pattern");
    }

    @Test
    void maskAndFilterAttributesWarnEvenWithDefinitionsInDefs() {
        // The standard exporter idiom: the <mask> definition sits in <defs>
        // (never walked), only the referencing attribute is visible.
        SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <defs><mask id="m"><rect width="5" height="5" fill="#fff"/></mask></defs>
                  <rect width="10" height="10" fill="#ff0000" mask="url(#m)"/>
                  <circle cx="5" cy="5" r="2" style="filter:url(#f)"/>
                </svg>""");

        List<String> degraded = warnings().stream()
                .filter(message -> message.contains("approximated")).toList();
        assertThat(degraded).hasSize(1);
        assertThat(degraded.get(0))
                .contains("mask (painted unmasked)")
                .contains("filter (painted unfiltered)");
    }

    @Test
    void gradientStrokePartialOpacityWarnsToo() {
        SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <linearGradient id="g">
                    <stop offset="0" stop-color="#ff0000"/>
                    <stop offset="1" stop-color="#0000ff"/>
                  </linearGradient>
                  <path d="M0 0 L10 10" fill="none" stroke="url(#g)"
                        stroke-width="2" stroke-opacity="0.5"/>
                </svg>""");

        assertThat(warnings()).anyMatch(message ->
                message.contains("opacity on a gradient stroke"));
    }

    @Test
    void fillOpacityInheritsAndAnExplicitValueReplacesIt() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <g fill-opacity="0.5">
                    <rect width="10" height="4" fill="#ff0000"/>
                    <rect y="6" width="10" height="4" fill="#ff0000" fill-opacity="0.25"/>
                  </g>
                </svg>""");

        // First rect inherits 0.5; the second's own value replaces (not
        // multiplies) the inherited one, per SVG property inheritance.
        assertThat(icon.layers().get(0).fill().color().getAlpha()).isEqualTo(128);
        assertThat(icon.layers().get(1).fill().color().getAlpha()).isEqualTo(64);
    }

    @Test
    void opacityInStyleAttributeIsHonoured() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" style="fill:#ff0000;opacity:0.5"/>
                </svg>""");

        assertThat(icon.layers().get(0).fill().color().getAlpha()).isEqualTo(128);
    }

    @Test
    void zeroStrokeOpacityHidesTheStrokeButKeepsTheFill() {
        SvgIcon icon = SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff0000"
                        stroke="#000000" stroke-width="1" stroke-opacity="0"/>
                </svg>""");

        SvgIcon.Layer layer = icon.layers().get(0);
        assertThat(layer.fill()).isNotNull();
        assertThat(layer.stroke()).isNull();
    }

    @Test
    void nanOpacityIsRefusedNotSilentlyHidden() {
        assertThatThrownBy(() -> SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <rect width="10" height="10" fill="#ff0000" opacity="NaN"/>
                </svg>"""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opacity")
                .hasMessageContaining("must be a number");
    }

    @Test
    void structureOnlyElementsStayOutOfTheTally() {
        SvgIcon.parse("""
                <svg viewBox="0 0 10 10">
                  <title>icon</title>
                  <desc>a square</desc>
                  <defs><clipPath id="c"><rect width="5" height="5"/></clipPath></defs>
                  <rect width="10" height="10" fill="#ff0000"/>
                </svg>""");

        assertThat(warnings()).noneMatch(message ->
                message.contains("skipped unsupported element(s)"));
    }
}
