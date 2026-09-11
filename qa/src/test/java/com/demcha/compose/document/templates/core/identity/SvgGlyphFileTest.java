package com.demcha.compose.document.templates.core.identity;

import com.demcha.compose.document.style.ShapeOutline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SvgGlyph#fromFile(java.nio.file.Path)} — the variant that loads a mark
 * a caller supplies rather than one a template ships with.
 */
class SvgGlyphFileTest {

    private static final String SQUARE = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
              <path fill="#000000" d="M10 10 L90 10 L90 90 L10 90 Z"/>
            </svg>
            """;

    /** Same drawing in a frame twice as wide — a different aspect ratio to detect. */
    private static final String WIDE = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 100">
              <path fill="#000000" d="M10 10 L190 10 L190 90 L10 90 Z"/>
            </svg>
            """;

    @Test
    void loadsAGlyphFromDisk(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("mark.svg"), SQUARE);

        SvgGlyph glyph = SvgGlyph.fromFile(file);

        assertThat(glyph.aspectRatio()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
        ShapeOutline outline = glyph.outline(24);
        assertThat(outline).isNotNull();
    }

    @Test
    void rereadsTheFileOnEveryCall(@TempDir Path dir) throws Exception {
        // The classpath variant caches forever because a jar entry cannot change.
        // A file can: a caller who replaces the logo must not keep getting the old
        // one for the life of the JVM.
        Path file = dir.resolve("mark.svg");
        Files.writeString(file, SQUARE);
        assertThat(SvgGlyph.fromFile(file).aspectRatio())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));

        Files.writeString(file, WIDE);
        assertThat(SvgGlyph.fromFile(file).aspectRatio())
                .isCloseTo(2.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void reportsAMissingFileByName(@TempDir Path dir) {
        Path missing = dir.resolve("nowhere.svg");

        assertThatThrownBy(() -> SvgGlyph.fromFile(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nowhere.svg");
    }

    @Test
    void rejectsANullPath() {
        assertThatThrownBy(() -> SvgGlyph.fromFile(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("file");
    }

    @Test
    void refusesAnSvgWithNothingToDraw(@TempDir Path dir) throws Exception {
        // Fails loudly rather than yielding a glyph that renders as nothing — the
        // rejection comes from the SVG reader, before the flattening step.
        Path file = Files.writeString(dir.resolve("empty.svg"),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"></svg>");

        assertThatThrownBy(() -> SvgGlyph.fromFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no drawable geometry");
    }
}
