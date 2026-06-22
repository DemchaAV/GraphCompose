package com.demcha.compose.document.emoji;

import com.demcha.compose.document.svg.SvgIcon;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void indexedGlyphThatCannotBeParsedResolvesEmptyAndRequireExplains(@TempDir Path classpathRoot) throws Exception {
        // A set whose index points at a glyph the SVG parser rejects (here an svg
        // with no drawable geometry). find() must stay lenient — empty, so callers
        // fall back to literal text — and require()'s message must distinguish
        // "indexed but unrenderable" from "unknown shortcode".
        Path svgDir = Files.createDirectories(classpathRoot.resolve("emoji/svg"));
        Files.writeString(classpathRoot.resolve("emoji/emoji-index.properties"), "broken=0bad1\n");
        Files.writeString(svgDir.resolve("0bad1.svg"), "<svg viewBox='0 0 10 10'/>");
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classpathRoot.toUri().toURL()}, null)) {
            EmojiLibrary lib = new EmojiLibrary(loader);

            assertThat(lib.isAvailable()).isTrue();
            assertThat(lib.find("broken")).isEmpty();
            assertThatThrownBy(() -> lib.require("broken"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("could not be rendered");
        }
    }
}
