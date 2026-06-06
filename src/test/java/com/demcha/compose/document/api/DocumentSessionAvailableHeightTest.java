package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Covers {@link DocumentSession#availableHeight()} as a faithful alias of
 * {@code canvas().innerHeight()} (the usable content height).
 */
class DocumentSessionAvailableHeightTest {

    @Test
    void availableHeightAliasesCanvasInnerHeight() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(40))
                .create()) {
            assertThat(document.availableHeight())
                    .isEqualTo(document.canvas().innerHeight());
            // 600pt page height minus 40pt top + 40pt bottom margins
            assertThat(document.availableHeight()).isCloseTo(520.0, within(0.5));
        }
    }
}
