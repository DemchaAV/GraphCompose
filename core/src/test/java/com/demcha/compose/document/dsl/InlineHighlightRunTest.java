package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.InlineHighlightRun;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
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

    @Test
    void paragraphChipInheritsTheParagraphStyleAndOverridesOnlyTheColour() {
        DocumentTextStyle paragraphStyle = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(9)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        InlineHighlightRun run = onlyHighlight(new ParagraphBuilder()
                .textStyle(paragraphStyle)
                .inlineChip(" Paid ", DocumentColor.rgb(0, 100, 0), DocumentColor.rgb(220, 255, 220)));

        // Everything but the colour is the paragraph's, compared as a whole so a
        // component added to DocumentTextStyle later is covered too. Normalized to
        // one shared colour because DocumentColor compares by identity.
        assertThat(run.textStyle().withColor(DocumentColor.BLACK))
                .isEqualTo(paragraphStyle.withColor(DocumentColor.BLACK));
        assertThat(run.textStyle().size()).isEqualTo(9.0, within(1e-9));
        assertThat(run.textStyle().fontName()).isEqualTo(FontName.TIMES_ROMAN);
        assertThat(run.textStyle().decoration()).isEqualTo(DocumentTextDecoration.BOLD);
        assertThat(run.textStyle().color().color()).isEqualTo(new Color(0, 100, 0));
        // The fill and the chip geometry are the caller's, not the paragraph's.
        assertThat(run.background().fill().color()).isEqualTo(new Color(220, 255, 220));
        assertThat(run.background().cornerRadius()).isEqualTo(3.0);
    }

    @Test
    void paragraphChipWithAnExplicitStyleIgnoresTheParagraph() {
        DocumentTextStyle chipStyle = DocumentTextStyle.builder().size(20).build();
        InlineHighlightRun run = onlyHighlight(new ParagraphBuilder()
                .textStyle(DocumentTextStyle.builder().size(9).build())
                .inlineStyledChip("BIG", chipStyle, DocumentColor.GRAY));
        assertThat(run.textStyle().size()).isEqualTo(20.0, within(1e-9));
    }

    @Test
    void paragraphChipReadsTheStyleThatWasSetWhenItWasCalled() {
        // The documented ordering caveat, pinned: the chip snapshots the paragraph
        // style at the call, so a textStyle(...) that lands afterwards does not
        // reach it. Change this test deliberately if the resolution ever moves to
        // build() -- do not let it drift silently.
        InlineHighlightRun run = onlyHighlight(new ParagraphBuilder()
                .inlineChip("x", DocumentColor.rgb(0, 100, 0), DocumentColor.GRAY)
                .textStyle(DocumentTextStyle.builder().size(9).build()));
        assertThat(run.textStyle().size()).isEqualTo(DocumentTextStyle.DEFAULT.size(), within(1e-9));
    }

    private static InlineHighlightRun onlyHighlight(ParagraphBuilder paragraph) {
        return paragraph.build().inlineRuns().stream()
                .filter(InlineHighlightRun.class::isInstance)
                .map(InlineHighlightRun.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no InlineHighlightRun in the paragraph"));
    }

    private static InlineHighlightRun onlyHighlight(RichText rich) {
        return rich.runs().stream()
                .filter(InlineHighlightRun.class::isInstance)
                .map(InlineHighlightRun.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no InlineHighlightRun in " + rich.runs()));
    }
}
