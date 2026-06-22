package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.InlineHighlightRun;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Model coverage for the inline highlight "chip": the {@link InlineBackground}
 * value type, the {@link InlineHighlightRun}, and the {@link RichText}
 * {@code highlight}/{@code code}/{@code chip} DSL sugar.
 */
class InlineHighlightRunTest {

    @Test
    void backgroundRejectsNullFillAndNonFiniteOrNegativeRadius() {
        assertThatThrownBy(() -> new InlineBackground(null, 2.0, DocumentInsets.zero()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new InlineBackground(DocumentColor.GRAY, -1.0, DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cornerRadius");
        assertThatThrownBy(() -> new InlineBackground(DocumentColor.GRAY, Double.NaN, DocumentInsets.zero()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void backgroundNormalizesNullPaddingToZero() {
        InlineBackground background = new InlineBackground(DocumentColor.GRAY, 3.0, null);
        assertThat(background.padding()).isEqualTo(DocumentInsets.zero());
    }

    @Test
    void runRejectsNullBackgroundAndNormalizesNullText() {
        assertThatThrownBy(() -> new InlineHighlightRun("x", null, null))
                .isInstanceOf(NullPointerException.class);
        InlineHighlightRun run = new InlineHighlightRun(null, null,
                new InlineBackground(DocumentColor.GRAY, 2.0, DocumentInsets.of(2)));
        assertThat(run.text()).isEmpty();
        assertThat(run.linkTarget()).isNull();
    }

    @Test
    void codeProducesAMonospaceRunWithTheDefaultChip() {
        InlineHighlightRun run = onlyHighlight(RichText.text("").code("io.github.demchaav"));
        assertThat(run.text()).isEqualTo("io.github.demchaav");
        assertThat(run.textStyle().fontName()).isEqualTo(FontName.COURIER);
        // symmetric(1, 4) -> 4 pt each side -> 8 pt horizontal.
        assertThat(run.background().padding().horizontal()).isEqualTo(8.0, within(1e-9));
        assertThat(run.background().cornerRadius()).isEqualTo(3.0);
        assertThat(run.background().fill()).isNotNull();
    }

    @Test
    void chipUsesTheGivenForegroundAndFill() {
        InlineHighlightRun run = onlyHighlight(
                RichText.text("").chip("Paid", DocumentColor.rgb(0, 100, 0), DocumentColor.rgb(220, 255, 220)));
        assertThat(run.text()).isEqualTo("Paid");
        assertThat(run.textStyle().color().color()).isEqualTo(new Color(0, 100, 0));
        assertThat(run.background().fill().color()).isEqualTo(new Color(220, 255, 220));
    }

    @Test
    void highlightCarriesTheExplicitChipAndOptionalLink() {
        DocumentInsets padding = DocumentInsets.symmetric(2, 6);
        InlineHighlightRun plain = onlyHighlight(RichText.text("").highlight(
                "x", DocumentTextStyle.DEFAULT, DocumentColor.GRAY, 4.0, padding));
        assertThat(plain.background().cornerRadius()).isEqualTo(4.0);
        assertThat(plain.background().padding()).isEqualTo(padding);
        assertThat(plain.linkTarget()).isNull();

        InlineHighlightRun linked = onlyHighlight(RichText.text("").highlight(
                "x", DocumentTextStyle.DEFAULT, DocumentColor.GRAY, 4.0, padding,
                new com.demcha.compose.document.node.DocumentLinkOptions("https://example.com")));
        assertThat(linked.linkTarget()).isNotNull();
    }

    private static InlineHighlightRun onlyHighlight(RichText rich) {
        return rich.runs().stream()
                .filter(InlineHighlightRun.class::isInstance)
                .map(InlineHighlightRun.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no InlineHighlightRun in " + rich.runs()));
    }
}
