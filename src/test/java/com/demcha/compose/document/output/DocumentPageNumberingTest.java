package com.demcha.compose.document.output;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the {@link DocumentPageNumbering} value object and its default, and that
 * a {@link DocumentHeaderFooter} carries numbering through its builder while
 * defaulting to the pre-1.9 behaviour.
 */
class DocumentPageNumberingTest {

    @Test
    void defaultIsDecimalNoOffsetShownEverywhere() {
        DocumentPageNumbering def = DocumentPageNumbering.DEFAULT;
        assertThat(def.getStartAt()).isEqualTo(1);
        assertThat(def.getCountFrom()).isEqualTo(1);
        assertThat(def.isShowOnFirstPage()).isTrue();
        assertThat(def.getStyle()).isEqualTo(DocumentPageNumberStyle.DECIMAL);
    }

    @Test
    void builderRoundTrips() {
        DocumentPageNumbering numbering = DocumentPageNumbering.builder()
                .startAt(1)
                .countFrom(3)
                .showOnFirstPage(false)
                .style(DocumentPageNumberStyle.LOWER_ROMAN)
                .build();
        assertThat(numbering.getCountFrom()).isEqualTo(3);
        assertThat(numbering.isShowOnFirstPage()).isFalse();
        assertThat(numbering.getStyle()).isEqualTo(DocumentPageNumberStyle.LOWER_ROMAN);
        assertThat(numbering.toBuilder().build().getStyle()).isEqualTo(DocumentPageNumberStyle.LOWER_ROMAN);
    }

    @Test
    void headerFooterDefaultsToDefaultNumberingAndCarriesOverrides() {
        assertThat(DocumentHeaderFooter.builder().centerText("{page}").build().getNumbering())
                .isSameAs(DocumentPageNumbering.DEFAULT);

        DocumentPageNumbering roman = DocumentPageNumbering.builder()
                .style(DocumentPageNumberStyle.UPPER_ROMAN).build();
        assertThat(DocumentHeaderFooter.builder().numbering(roman).build().getNumbering())
                .isSameAs(roman);
    }
}
