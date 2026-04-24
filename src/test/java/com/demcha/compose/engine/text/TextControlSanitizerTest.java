package com.demcha.compose.engine.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextControlSanitizerTest {

    @Test
    void removeShouldStripControlCodePointsWithoutChangingVisibleSymbols() {
        assertThat(TextControlSanitizer.remove("Java\u0000 \u2022 Kotlin\u200B"))
                .isEqualTo("Java \u2022 Kotlin");
    }

    @Test
    void replaceShouldUseCallerReplacementForControlCodePoints() {
        assertThat(TextControlSanitizer.replace("Line\u0001Break", " "))
                .isEqualTo("Line Break");
    }
}
