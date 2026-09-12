package com.demcha.compose.document.style;

import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How {@link DocumentTextStyle} carries tracking: the default costs nothing,
 * the pre-existing constructor still means "no tracking", and the copy methods
 * do not quietly drop it.
 */
class DocumentTextStyleLetterSpacingTest {

    @Test
    void aStyleThatNeverAskedForTrackingHasNone() {
        assertThat(DocumentTextStyle.DEFAULT.letterSpacing()).isEqualTo(DocumentLetterSpacing.NONE);
        assertThat(DocumentTextStyle.builder().build().letterSpacing()).isEqualTo(DocumentLetterSpacing.NONE);
        assertThat(DocumentTextStyle.DEFAULT.letterSpacing().resolve(DocumentTextStyle.DEFAULT.size())).isZero();
    }

    @Test
    void theFourArgumentConstructorStillMeansNoTracking() {
        DocumentTextStyle style =
                new DocumentTextStyle(FontName.HELVETICA, 12, DocumentTextDecoration.BOLD, DocumentColor.BLACK);

        assertThat(style.letterSpacing()).isEqualTo(DocumentLetterSpacing.NONE);
    }

    @Test
    void aNullTrackingNormalizesToNone() {
        DocumentTextStyle viaConstructor =
                new DocumentTextStyle(FontName.HELVETICA, 12, DocumentTextDecoration.DEFAULT, DocumentColor.BLACK, null);
        DocumentTextStyle viaBuilder = DocumentTextStyle.builder().letterSpacing(null).build();

        assertThat(viaConstructor.letterSpacing()).isEqualTo(DocumentLetterSpacing.NONE);
        assertThat(viaBuilder.letterSpacing()).isEqualTo(DocumentLetterSpacing.NONE);
    }

    @Test
    void theBuilderCarriesTrackingThrough() {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .size(24)
                .letterSpacing(DocumentLetterSpacing.ofFontSize(0.12))
                .build();

        assertThat(style.letterSpacing()).isEqualTo(DocumentLetterSpacing.ofFontSize(0.12));
        assertThat(style.letterSpacing().resolve(style.size())).isEqualTo(2.88);
    }

    @Test
    void withSizeKeepsTheTrackingAndRescalesAFontSizeShare() {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .size(10)
                .letterSpacing(DocumentLetterSpacing.ofFontSize(0.1))
                .build();

        DocumentTextStyle bigger = style.withSize(30);

        assertThat(bigger.letterSpacing()).isEqualTo(DocumentLetterSpacing.ofFontSize(0.1));
        // The share is kept, so the resolved amount follows the new size.
        assertThat(bigger.letterSpacing().resolve(bigger.size())).isEqualTo(3.0);
    }

    @Test
    void withColorKeepsTheTracking() {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .letterSpacing(DocumentLetterSpacing.points(1.5))
                .build();

        assertThat(style.withColor(DocumentColor.rgb(255, 0, 0)).letterSpacing())
                .isEqualTo(DocumentLetterSpacing.points(1.5));
    }

    @Test
    void withLetterSpacingReplacesOnlyTheTracking() {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .fontName(FontName.TIMES_ROMAN)
                .size(18)
                .decoration(DocumentTextDecoration.ITALIC)
                .color(DocumentColor.rgb(18, 52, 86))
                .build();

        DocumentTextStyle tracked = style.withLetterSpacing(DocumentLetterSpacing.points(2));

        assertThat(tracked.letterSpacing()).isEqualTo(DocumentLetterSpacing.points(2));
        assertThat(tracked.fontName()).isEqualTo(style.fontName());
        assertThat(tracked.size()).isEqualTo(style.size());
        assertThat(tracked.decoration()).isEqualTo(style.decoration());
        assertThat(tracked.color()).isEqualTo(style.color());
    }

    @Test
    void trackingTakesPartInEquality() {
        DocumentTextStyle plain = DocumentTextStyle.builder().size(12).build();
        DocumentTextStyle tracked = plain.withLetterSpacing(DocumentLetterSpacing.points(1));

        assertThat(tracked).isNotEqualTo(plain);
        assertThat(tracked.withLetterSpacing(DocumentLetterSpacing.NONE)).isEqualTo(plain);
    }
}
