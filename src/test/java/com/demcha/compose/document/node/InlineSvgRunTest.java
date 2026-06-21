package com.demcha.compose.document.node;

import com.demcha.compose.document.svg.SvgIcon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class InlineSvgRunTest {

    private static final double EPS = 1e-6;

    private static SvgIcon icon() {
        return SvgIcon.parse("""
                <svg viewBox="0 0 24 24">
                  <path d="M2 2 H22 V22 H2 Z" fill="rgb(196, 30, 58)"/>
                </svg>
                """);
    }

    @Test
    void convenienceConstructorKeepsIconAndDefaults() {
        SvgIcon icon = icon();
        InlineSvgRun run = new InlineSvgRun(icon, 12.0, 12.0);

        assertThat(run.icon()).isSameAs(icon);
        assertThat(run.width()).isEqualTo(12.0, within(EPS));
        assertThat(run.height()).isEqualTo(12.0, within(EPS));
        assertThat(run.alignment()).isEqualTo(InlineImageAlignment.CENTER);
        assertThat(run.baselineOffset()).isEqualTo(0.0, within(EPS));
        assertThat(run.linkTarget()).isNull();
    }

    @Test
    void nullAlignmentNormalizesToCenter() {
        InlineSvgRun run = new InlineSvgRun(icon(), 10.0, 10.0, null, 0.0, (DocumentLinkOptions) null);

        assertThat(run.alignment()).isEqualTo(InlineImageAlignment.CENTER);
    }

    @Test
    void externalLinkOptionsAreWrappedIntoExternalTarget() {
        InlineSvgRun run = new InlineSvgRun(icon(), 10.0, 10.0, InlineImageAlignment.CENTER, 0.0,
                new DocumentLinkOptions("https://example.com"));

        assertThat(run.linkTarget()).isInstanceOf(ExternalLinkTarget.class);
    }

    @Test
    void nullIconIsRejected() {
        assertThatThrownBy(() -> new InlineSvgRun(null, 10.0, 10.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("icon");
    }

    @Test
    void nonPositiveWidthIsRejected() {
        assertThatThrownBy(() -> new InlineSvgRun(icon(), 0.0, 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width");
        assertThatThrownBy(() -> new InlineSvgRun(icon(), -3.0, 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width");
    }

    @Test
    void nonFiniteWidthIsRejected() {
        assertThatThrownBy(() -> new InlineSvgRun(icon(), Double.POSITIVE_INFINITY, 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width");
        assertThatThrownBy(() -> new InlineSvgRun(icon(), Double.NaN, 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width");
    }

    @Test
    void nonPositiveHeightIsRejected() {
        assertThatThrownBy(() -> new InlineSvgRun(icon(), 10.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("height");
    }

    @Test
    void nonFiniteBaselineOffsetIsRejected() {
        assertThatThrownBy(() -> new InlineSvgRun(icon(), 10.0, 10.0,
                InlineImageAlignment.CENTER, Double.POSITIVE_INFINITY, (DocumentLinkOptions) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baselineOffset");
    }
}
