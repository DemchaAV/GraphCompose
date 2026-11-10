package com.demcha.compose.document.svg;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SvgIcon#withText(String)} attaches the text an icon stands for without
 * touching the drawing, and leaves the icon it was called on as it was.
 */
class SvgIconTextTest {

    private static final String CHECK =
            "<svg viewBox='0 0 20 10'><path d='M1 5 L4 8 L9 1' stroke='#2E7D32' fill='none'/></svg>";

    @Test
    void parsedIconStatesNoText() {
        assertThat(SvgIcon.parse(CHECK).text()).isNull();
    }

    @Test
    void withTextReturnsACopyWithTheSameDrawing() {
        SvgIcon plain = SvgIcon.parse(CHECK);

        SvgIcon check = plain.withText("✓");

        assertThat(check).isNotSameAs(plain);
        assertThat(check.text()).isEqualTo("✓");
        assertThat(check.layers()).isEqualTo(plain.layers());
        assertThat(check.sourceWidth()).isEqualTo(plain.sourceWidth());
        assertThat(check.sourceHeight()).isEqualTo(plain.sourceHeight());
        assertThat(plain.text()).as("the original icon is unchanged").isNull();
    }

    @Test
    void nullOrBlankTextClearsIt() {
        SvgIcon check = SvgIcon.parse(CHECK).withText("✓");

        assertThat(check.withText(null).text()).isNull();
        assertThat(check.withText("  ").text()).isNull();
    }

    @Test
    void textIsKeptVerbatim() {
        // surrounding spaces and multi-codepoint sequences are the caller's to choose
        String family = "👩‍👧 ";

        assertThat(SvgIcon.parse(CHECK).withText(family).text()).isEqualTo(family);
    }
}
