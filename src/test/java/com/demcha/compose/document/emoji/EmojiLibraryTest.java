package com.demcha.compose.document.emoji;

import com.demcha.compose.document.svg.SvgIcon;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Resolver coverage for {@link EmojiLibrary}: the starter emoji set ships on the
 * test classpath via the sibling {@code graph-compose-emoji} module's resources,
 * so the default library resolves shortcodes to parsed {@link SvgIcon}s; an
 * absent set degrades cleanly and names the missing artifact.
 */
class EmojiLibraryTest {

    private final EmojiLibrary emoji = EmojiLibrary.getDefault();

    @Test
    void emojiSetIsAvailableOnTheTestClasspath() {
        assertThat(emoji.isAvailable()).isTrue();
    }

    @Test
    void resolvesKnownShortcodeWithOrWithoutColonsAndCaseInsensitively() {
        assertThat(emoji.find(":star:")).isPresent();
        assertThat(emoji.find("star")).isPresent();
        assertThat(emoji.find("  :STAR:  ")).isPresent();
    }

    @Test
    void resolvedGlyphIsAParsedSvgIcon() {
        SvgIcon icon = emoji.require(":white_check_mark:");
        assertThat(icon.layers()).isNotEmpty();
        assertThat(icon.aspectRatio()).isCloseTo(1.0, within(0.01));
    }

    @Test
    void unknownShortcodeResolvesEmpty() {
        assertThat(emoji.find(":definitely_not_an_emoji:")).isEmpty();
    }

    @Test
    void requireThrowsNamingTheUnknownShortcode() {
        assertThatThrownBy(() -> emoji.require(":definitely_not_an_emoji:"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("definitely_not_an_emoji");
    }

    @Test
    void nullBlankAndEmptyColonsResolveEmpty() {
        assertThat(emoji.find(null)).isEmpty();
        assertThat(emoji.find("")).isEmpty();
        assertThat(emoji.find("::")).isEmpty();
    }

    @Test
    void absentEmojiSetReportsUnavailableAndNamesTheArtifact() throws Exception {
        try (URLClassLoader bare = new URLClassLoader(new URL[0], null)) {
            EmojiLibrary absent = new EmojiLibrary(bare);

            assertThat(absent.isAvailable()).isFalse();
            assertThat(absent.find(":star:")).isEmpty();
            assertThatThrownBy(() -> absent.require(":star:"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("graph-compose-emoji");
        }
    }
}
