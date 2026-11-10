package com.demcha.compose.document.emoji;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link EmojiSequences} turns a glyph key back into the emoji as text, in the
 * fully-qualified form Unicode defines: U+FE0F after every character whose
 * default presentation is text, and nowhere else.
 */
class EmojiSequencesTest {

    @Test
    void emojiPresentationCharacterNeedsNoSelector() {
        assertThat(EmojiSequences.fullyQualified("1f680")).isEqualTo("🚀"); // rocket
    }

    @Test
    void textDefaultCharacterGetsTheEmojiSelectorBack() {
        assertThat(EmojiSequences.fullyQualified("2764")).isEqualTo("❤️"); // red heart
        assertThat(EmojiSequences.fullyQualified("a9")).isEqualTo("©️"); // copyright
    }

    @Test
    void zwjSequenceKeepsEveryJoinerAndQualifiesEachTextDefaultPart() {
        // woman technologist: both parts are emoji-presentation, so nothing is added
        assertThat(EmojiSequences.fullyQualified("1f469-200d-1f4bb"))
                .isEqualTo("👩‍💻");
        // man health worker: the staff of Aesculapius is text-default
        assertThat(EmojiSequences.fullyQualified("1f468-200d-2695"))
                .isEqualTo("👨‍⚕️");
        // eye in speech bubble: both parts are text-default
        assertThat(EmojiSequences.fullyQualified("1f441-200d-1f5e8"))
                .isEqualTo("👁️‍🗨️");
    }

    @Test
    void skinToneModifierQualifiesTheCharacterItFollows() {
        // index pointing up with a light skin tone: no selector between base and modifier
        assertThat(EmojiSequences.fullyQualified("261d-1f3fb")).isEqualTo("☝🏻");
    }

    @Test
    void keycapBaseIsQualifiedBeforeTheEnclosingKeycap() {
        assertThat(EmojiSequences.fullyQualified("0023-20e3")).isEqualTo("#️⃣");
    }

    @Test
    void presentationSelectorAlreadyInTheKeyIsNotDoubled() {
        assertThat(EmojiSequences.fullyQualified("2764-fe0f")).isEqualTo("❤️");
        assertThat(EmojiSequences.fullyQualified("2764-fe0e")).isEqualTo("❤︎");
    }

    @Test
    void keyThatIsNotHexCodepointsSpellsNothing() {
        assertThat(EmojiSequences.fullyQualified(null)).isNull();
        assertThat(EmojiSequences.fullyQualified("  ")).isNull();
        assertThat(EmojiSequences.fullyQualified("company-logo")).isNull();
        assertThat(EmojiSequences.fullyQualified("1f680--1f680")).isNull();
        assertThat(EmojiSequences.fullyQualified("110000")).isNull();
    }

    @Test
    void textDefaultTableIsStrictlyAscending() {
        int[] table = EmojiSequences.TEXT_DEFAULT;
        assertThat(IntStream.range(1, table.length).allMatch(i -> table[i - 1] < table[i]))
                .as("binarySearch needs a strictly ascending table")
                .isTrue();
    }

    /**
     * The table is Unicode data copied into source; this is what keeps it true.
     * JDK 21 added the two emoji property methods, so on an older JDK there is
     * nothing to compare with and the check is skipped; CI's JDK matrix runs it.
     */
    @Test
    void textDefaultTableMatchesTheRunningJdksUnicodeData() throws Throwable {
        MethodHandle isEmoji = emojiProperty("isEmoji");
        MethodHandle isEmojiPresentation = emojiProperty("isEmojiPresentation");
        assumeTrue(isEmoji != null && isEmojiPresentation != null,
                "Character.isEmoji / isEmojiPresentation need JDK 21+");

        List<Integer> expected = new ArrayList<>();
        for (int codepoint = 0; codepoint <= Character.MAX_CODE_POINT; codepoint++) {
            if ((boolean) isEmoji.invokeExact(codepoint) && !(boolean) isEmojiPresentation.invokeExact(codepoint)) {
                expected.add(codepoint);
            }
        }

        assertThat(EmojiSequences.TEXT_DEFAULT)
                .as("Emoji=Yes and Emoji_Presentation=No on JDK %s", Runtime.version())
                .containsExactly(expected.stream().mapToInt(Integer::intValue).toArray());
    }

    private static MethodHandle emojiProperty(String name) {
        try {
            return MethodHandles.publicLookup().findStatic(Character.class, name,
                    MethodType.methodType(boolean.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }
}
