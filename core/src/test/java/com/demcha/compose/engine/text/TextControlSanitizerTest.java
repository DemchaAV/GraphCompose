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

    @Test
    void removeExceptFormattingControlsKeepsTheSteeringCharactersAndNothingElse() {
        // LRM, RLM and ALM survive — they are the author's instruction to the
        // bidirectional algorithm. The zero-width space, the BOM and a control
        // character are category C with no downstream purpose and still go.
        assertThat(TextControlSanitizer.removeExceptFormattingControls(
                "a\u200Eb\u200Fc\u061Cd\u200Be\uFEFFf\u0007g"))
                .isEqualTo("a\u200Eb\u200Fc\u061Cdefg");
    }

    @Test
    void removeExceptFormattingControlsMatchesRemoveWhenNoMarkIsPresent() {
        String plain = "Java\u0000 \u2022 Kotlin\u200B";

        assertThat(TextControlSanitizer.removeExceptFormattingControls(plain))
                .describedAs("a document without direction marks must sanitize exactly "
                        + "as it did before the exemption existed")
                .isEqualTo(TextControlSanitizer.remove(plain));
    }

    @Test
    void removeDirectionMarksStripsExactlyTheBidiControls() {
        assertThat(TextControlSanitizer.removeDirectionMarks(
                "a\u200E\u200F\u061C\u202A\u202E\u2066\u2069b"))
                .isEqualTo("ab");
        assertThat(TextControlSanitizer.removeDirectionMarks("a\u200Bb"))
                .describedAs("only the direction characters are its business")
                .isEqualTo("a\u200Bb");
    }

    @Test
    void isBidiControlCoversTheMarksEmbeddingsOverridesAndIsolates() {
        for (int codePoint : new int[]{0x061C, 0x200E, 0x200F, 0x202A, 0x202B, 0x202C,
                0x202D, 0x202E, 0x2066, 0x2067, 0x2068, 0x2069}) {
            assertThat(TextControlSanitizer.isBidiControl(codePoint))
                    .describedAs("U+%04X", codePoint)
                    .isTrue();
        }
        assertThat(TextControlSanitizer.isBidiControl('a')).isFalse();
        assertThat(TextControlSanitizer.isBidiControl(0x200B)).isFalse();
        assertThat(TextControlSanitizer.isBidiControl(0xFEFF)).isFalse();
    }

    @Test
    void theJoiningControlsSurviveLayoutSanitizingBecauseTheShaperIsTheirReader() {
        // Written as escapes: a raw zero-width control is invisible in review.
        String zwnj = "‌";
        String zwj = "‍";

        assertThat(TextControlSanitizer.removeExceptFormattingControls("ب" + zwnj + "ه"))
                .describedAs("U+200C is category C like any other control, but deleting it "
                        + "here removes the author's instruction before the shaper — the "
                        + "only thing that reads it — has run")
                .isEqualTo("ب" + zwnj + "ه");
        assertThat(TextControlSanitizer.removeExceptFormattingControls("ب" + zwj))
                .isEqualTo("ب" + zwj);
    }

    @Test
    void theJoiningControlsSurviveTheSeamThatStripsTheDirectionMarks() {
        // That seam runs once the bidirectional algorithm has read the direction marks —
        // which says nothing about the joining controls, whose reader is further along:
        // a backend that shapes the text itself. Stripping them here is what handed
        // PowerPoint a word it joined straight back up. The PDF glyph seam removes them
        // instead, and measurement passes through the same seam, so nothing drifts.
        assertThat(TextControlSanitizer.removeDirectionMarks("ب‌ه‏"))
                .describedAs("the direction mark goes, the non-joiner stays")
                .isEqualTo("ب‌ه");
    }
}
