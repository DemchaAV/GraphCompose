package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.semantic.docx.DocxFontFaces.Face;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The line an editor sets each face the document ships in where nothing fixes the line's
 * height ({@link DocxNaturalLine}). A face it does not ship — a standard PDF name, one that
 * may not travel — is set in whatever the reader's machine substitutes, so it has none.
 *
 * <p>Kept under the declared name exactly as the runs spell it. One family is written per name
 * ({@code DocxFontTable.resolve}), so no two faces can meet over one of these keys.</p>
 *
 * <p>A family given up for a name another one declares keeps no line of its own here. Its runs
 * read the key their own spelling and slot make, so they read the line of a face the holder
 * shipped where both of those match one the holder filled, and no line at all where either
 * does not — a spelling differing in case, or a slot the holder left empty. So there are two
 * answers, a line or none, by three routes, and the two answers are not the same to the
 * writer, which can hold a paragraph to a line it has and cannot to one it has not. Recorded
 * as a limit in the backend capability details, with a test per route.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxShippedLines {
    /** No face shipped: an export written without a font table. */
    static final DocxShippedLines NONE = new DocxShippedLines(Map.of(), List.of());

    private final Map<FontName, FontFamilyDefinition> families;
    private final Map<String, DocxNaturalLine> lines = new java.util.HashMap<>();
    /** The file of each face with a line whose character map is not read yet. */
    private final Map<String, byte[]> files = new java.util.HashMap<>();
    /** Each face's character map, read the first time a text is asked of it; empty where unreadable. */
    private final Map<String, java.util.Optional<org.apache.fontbox.ttf.CmapLookup>> characters =
            new java.util.HashMap<>();

    DocxShippedLines(Map<FontName, FontFamilyDefinition> families, List<DocxFontTable.Embedded> embedded) {
        this.families = families;
        for (DocxFontTable.Embedded family : embedded) {
            for (Face face : family.faces()) {
                if (face.line() != null) {
                    lines.put(family.wordFamily() + '\n' + face.element(), face.line());
                    files.put(family.wordFamily() + '\n' + face.element(), face.font());
                }
            }
        }
    }

    /**
     * Whether a face the document ships, with a line, holds a glyph of its own for every letter
     * of a text, so that an editor sets the text in it, not in a face it falls back to.
     *
     * @param fontName the face a style names
     * @param slot     which of its family's faces, as {@link #of(FontName, String)}
     * @param text     the letters
     * @return whether it does; false where the document does not ship that face, it has no line,
     *         or its Unicode character map cannot be read
     */
    boolean holds(FontName fontName, String slot, String text) {
        String wordFamily = wordFamilyOf(fontName);
        return wordFamily != null && holds(wordFamily, slot, text);
    }

    /**
     * The same, by the family a run names in Word.
     *
     * @param wordFamily the family's name as the font table writes it
     * @param slot       which of its faces, as {@link #of(FontName, String)}
     * @param text       the letters
     * @return whether it does, as {@link #holds(FontName, String, String)}
     */
    boolean holds(String wordFamily, String slot, String text) {
        String face = wordFamily + '\n' + slot;
        java.util.Optional<org.apache.fontbox.ttf.CmapLookup> map = characters.get(face);
        if (map == null) {
            byte[] file = files.remove(face);
            if (file == null) {
                return false;
            }
            map = characterMapOf(file);
            characters.put(face, map);
        }
        org.apache.fontbox.ttf.CmapLookup read = map.orElse(null);
        return read != null && text.codePoints().allMatch(letter -> read.getGlyphId(letter) > 0);
    }

    /** A face's Unicode character map; empty where it holds none, or cannot be read. */
    private static java.util.Optional<org.apache.fontbox.ttf.CmapLookup> characterMapOf(byte[] file) {
        try (org.apache.fontbox.ttf.TrueTypeFont font = new org.apache.fontbox.ttf.TTFParser()
                .parse(new org.apache.pdfbox.io.RandomAccessReadBuffer(file))) {
            return java.util.Optional.ofNullable(font.getUnicodeCmapLookup(true));
        } catch (IOException | RuntimeException unreadable) {
            return java.util.Optional.empty();
        }
    }

    /**
     * The line a face is set in.
     *
     * @param fontName the face a style names
     * @param slot     which of its family's faces: {@code embedRegular}, {@code embedBold},
     *                 {@code embedItalic} or {@code embedBoldItalic}
     * @return its line, or null where the document does not ship that face
     */
    DocxNaturalLine of(FontName fontName, String slot) {
        String wordFamily = wordFamilyOf(fontName);
        return wordFamily == null ? null : of(wordFamily, slot);
    }

    /** The family a style's face is written as in Word, or null where none is known. */
    private String wordFamilyOf(FontName fontName) {
        FontFamilyDefinition family = fontName == null ? null : families.get(FontLibrary.resolveFamily(fontName));
        return family == null ? null : family.wordFamily();
    }

    /**
     * The line a face is set in, by the family a run names in Word.
     *
     * @param wordFamily the family's name as the font table writes it
     * @param slot       which of its faces, as {@link #of(FontName, String)}
     * @return its line, or null where the document does not ship that face
     */
    DocxNaturalLine of(String wordFamily, String slot) {
        return lines.get(wordFamily + '\n' + slot);
    }
}
