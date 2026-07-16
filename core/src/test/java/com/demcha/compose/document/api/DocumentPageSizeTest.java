package com.demcha.compose.document.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the slide preset page sizes and their orientation behaviour. The slide
 * presets must match the PowerPoint defaults exactly (13.333 × 7.5 in and
 * 10 × 7.5 in at 72 pt/in) so a slide-sized document maps 1:1 onto a
 * presentation canvas.
 */
class DocumentPageSizeTest {

    @Test
    void slidePresetsMatchThePowerPointDefaults() {
        assertThat(DocumentPageSize.SLIDE_16_9.width()).isEqualTo(960.0);
        assertThat(DocumentPageSize.SLIDE_16_9.height()).isEqualTo(540.0);
        assertThat(DocumentPageSize.SLIDE_16_9.width() / DocumentPageSize.SLIDE_16_9.height())
                .isEqualTo(16.0 / 9.0);

        assertThat(DocumentPageSize.SLIDE_4_3.width()).isEqualTo(720.0);
        assertThat(DocumentPageSize.SLIDE_4_3.height()).isEqualTo(540.0);
        assertThat(DocumentPageSize.SLIDE_4_3.width() / DocumentPageSize.SLIDE_4_3.height())
                .isEqualTo(4.0 / 3.0);
    }

    @Test
    void slidePresetsAreAlreadyLandscape() {
        assertThat(DocumentPageSize.SLIDE_16_9.landscape()).isSameAs(DocumentPageSize.SLIDE_16_9);
        assertThat(DocumentPageSize.SLIDE_4_3.landscape()).isSameAs(DocumentPageSize.SLIDE_4_3);
    }

    @Test
    void portraitFlipsASlidePreset() {
        DocumentPageSize portrait = DocumentPageSize.SLIDE_16_9.portrait();
        assertThat(portrait.width()).isEqualTo(540.0);
        assertThat(portrait.height()).isEqualTo(960.0);
    }
}
