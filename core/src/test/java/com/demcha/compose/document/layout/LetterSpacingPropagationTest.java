package com.demcha.compose.document.layout;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tracking crossing the one seam between the public style and the engine style.
 *
 * <p>The public value keeps its unit; the engine is handed points. This is the
 * only place that knows the unit and the font size at the same time, so it is
 * the only place that can resolve one against the other &mdash; and the only
 * place that has to be right about it.</p>
 */
class LetterSpacingPropagationTest {

    @Test
    void aStyleWithoutTrackingReachesTheEngineAsZero() {
        TextStyle engineStyle = DocumentNodeAdapters.toTextStyle(DocumentTextStyle.DEFAULT);

        assertThat(engineStyle.letterSpacing()).isZero();
    }

    @Test
    void aNullStyleFallsBackToTheEngineDefaultWhichHasNoTracking() {
        assertThat(DocumentNodeAdapters.toTextStyle(null).letterSpacing()).isZero();
        assertThat(TextStyle.DEFAULT_STYLE.letterSpacing()).isZero();
    }

    @Test
    void pointsCrossTheSeamUnchanged() {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .size(24)
                .letterSpacing(DocumentLetterSpacing.points(1.2))
                .build();

        assertThat(DocumentNodeAdapters.toTextStyle(style).letterSpacing()).isEqualTo(1.2);
    }

    @Test
    void aFontSizeShareIsResolvedAgainstTheStylesOwnSizeBeforeTheEngineSeesIt() {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .size(24)
                .letterSpacing(DocumentLetterSpacing.ofFontSize(0.12))
                .build();

        // 12% of 24pt. The engine never learns the share existed.
        assertThat(DocumentNodeAdapters.toTextStyle(style).letterSpacing()).isEqualTo(2.88);
    }

    @Test
    void theShareIsResolvedAgainstTheNormalizedSizeNotTheRequestedOne() {
        // DocumentTextStyle folds a non-positive size onto 14pt; the tracking
        // has to follow the size the text is actually set at.
        DocumentTextStyle style = DocumentTextStyle.builder()
                .size(0)
                .letterSpacing(DocumentLetterSpacing.ofFontSize(0.5))
                .build();

        assertThat(style.size()).isEqualTo(14.0);
        assertThat(DocumentNodeAdapters.toTextStyle(style).letterSpacing()).isEqualTo(7.0);
    }

    @Test
    void negativeTrackingSurvivesTheCrossing() {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .size(20)
                .letterSpacing(DocumentLetterSpacing.ofFontSize(-0.05))
                .build();

        assertThat(DocumentNodeAdapters.toTextStyle(style).letterSpacing()).isEqualTo(-1.0);
    }

    @Test
    void everyOtherComponentStillCrossesAsItDid() {
        DocumentTextStyle style = new DocumentTextStyle(
                FontName.TIMES_ROMAN, 18, DocumentTextDecoration.BOLD, DocumentColor.rgb(17, 34, 51),
                DocumentLetterSpacing.points(0.75));

        TextStyle engineStyle = DocumentNodeAdapters.toTextStyle(style);

        assertThat(engineStyle.fontName()).isEqualTo(FontName.TIMES_ROMAN);
        assertThat(engineStyle.size()).isEqualTo(18.0);
        assertThat(engineStyle.color()).isEqualTo(DocumentColor.rgb(17, 34, 51).color());
        assertThat(engineStyle.letterSpacing()).isEqualTo(0.75);
    }

    @Test
    void theEngineStylesFourArgumentShapeStillMeansNoTracking() {
        TextStyle style = new TextStyle(
                FontName.HELVETICA, 12,
                com.demcha.compose.engine.components.content.text.TextDecoration.DEFAULT,
                java.awt.Color.BLACK);

        assertThat(style.letterSpacing()).isZero();
    }
}
