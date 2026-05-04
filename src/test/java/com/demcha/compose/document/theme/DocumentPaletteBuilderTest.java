package com.demcha.compose.document.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.document.style.DocumentColor;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class DocumentPaletteBuilderTest {

    @Test
    void builderProducesEquivalentPaletteRegardlessOfTokenOrder() {
        DocumentPalette ascending = DocumentPalette.builder()
                .primary(new Color(10, 20, 30))
                .accent(new Color(40, 50, 60))
                .surface(new Color(70, 80, 90))
                .surfaceMuted(new Color(100, 110, 120))
                .textPrimary(new Color(130, 140, 150))
                .textMuted(new Color(160, 170, 180))
                .rule(new Color(190, 200, 210))
                .build();

        DocumentPalette shuffled = DocumentPalette.builder()
                .rule(new Color(190, 200, 210))
                .textMuted(new Color(160, 170, 180))
                .textPrimary(new Color(130, 140, 150))
                .surfaceMuted(new Color(100, 110, 120))
                .surface(new Color(70, 80, 90))
                .accent(new Color(40, 50, 60))
                .primary(new Color(10, 20, 30))
                .build();

        // DocumentColor is currently not value-equal; compare by recursive
        // field values until DocumentColor gains a proper equals().
        assertThat(shuffled).usingRecursiveComparison().isEqualTo(ascending);
    }

    @Test
    void builderAcceptsBothDocumentColorAndAwtColorForEachToken() {
        DocumentPalette mixed = DocumentPalette.builder()
                .primary(DocumentColor.of(new Color(10, 20, 30)))
                .accent(new Color(40, 50, 60))
                .surface(DocumentColor.of(Color.WHITE))
                .surfaceMuted(new Color(100, 110, 120))
                .textPrimary(DocumentColor.of(Color.BLACK))
                .textMuted(new Color(160, 170, 180))
                .rule(DocumentColor.of(new Color(190, 200, 210)))
                .build();

        assertThat(mixed.primary().color()).isEqualTo(new Color(10, 20, 30));
        assertThat(mixed.surface().color()).isEqualTo(Color.WHITE);
    }

    @Test
    void buildWithoutAnyTokensThrowsIllegalStateNamingEveryMissingField() {
        assertThatThrownBy(() -> DocumentPalette.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("primary")
                .hasMessageContaining("accent")
                .hasMessageContaining("surface")
                .hasMessageContaining("surfaceMuted")
                .hasMessageContaining("textPrimary")
                .hasMessageContaining("textMuted")
                .hasMessageContaining("rule");
    }

    @Test
    void buildWithSomeTokensThrowsIllegalStateNamingOnlyMissingOnes() {
        assertThatThrownBy(() -> DocumentPalette.builder()
                .primary(Color.BLACK)
                .accent(Color.WHITE)
                .surface(Color.LIGHT_GRAY)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("primary")
                .hasMessageNotContaining("accent")
                .hasMessageNotContaining("surface,")
                .hasMessageContaining("surfaceMuted")
                .hasMessageContaining("textPrimary")
                .hasMessageContaining("textMuted")
                .hasMessageContaining("rule");
    }

    @Test
    void setterRejectsNullColor() {
        assertThatThrownBy(() -> DocumentPalette.builder().primary((DocumentColor) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("primary");
    }

    @Test
    void deprecatedOfFactoryStillProducesEquivalentPaletteForBackwardCompat() {
        @SuppressWarnings("deprecation")
        DocumentPalette legacy = DocumentPalette.of(
                new Color(10, 20, 30),
                new Color(40, 50, 60),
                new Color(70, 80, 90),
                new Color(100, 110, 120),
                new Color(130, 140, 150),
                new Color(160, 170, 180),
                new Color(190, 200, 210));

        DocumentPalette modern = DocumentPalette.builder()
                .primary(new Color(10, 20, 30))
                .accent(new Color(40, 50, 60))
                .surface(new Color(70, 80, 90))
                .surfaceMuted(new Color(100, 110, 120))
                .textPrimary(new Color(130, 140, 150))
                .textMuted(new Color(160, 170, 180))
                .rule(new Color(190, 200, 210))
                .build();

        assertThat(legacy).usingRecursiveComparison().isEqualTo(modern);
    }
}
