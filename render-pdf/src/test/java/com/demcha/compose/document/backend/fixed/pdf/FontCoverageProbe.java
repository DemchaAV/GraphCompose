package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.ArrayList;
import java.util.List;

/**
 * Asks a bundled family which code points it can actually encode.
 *
 * <p>The measurement library is the one used here: it carries the bundled families and
 * needs no owning document, and encodability is a property of the font program, so the
 * measurement face answers exactly as the embedded render face would.</p>
 */
final class FontCoverageProbe {

    private static final FontLibrary LIBRARY = PdfFontLibraryFactory.measurementLibrary(List.of());

    private FontCoverageProbe() {
    }

    /** Returns the face a family resolves the given decoration to. */
    static PDFont face(FontName name, TextDecoration decoration) {
        PdfFont font = LIBRARY.getFont(name, PdfFont.class).orElseThrow(
                () -> new AssertionError("family not in the bundled catalog: " + name));
        return font.fontType(decoration);
    }

    /** Returns the code points in the range the face cannot encode, formatted for the failure message. */
    static List<String> unencodable(FontName name, TextDecoration decoration, int first, int last) {
        PDFont font = face(name, decoration);
        List<String> missing = new ArrayList<>();
        for (int codePoint = first; codePoint <= last; codePoint++) {
            if (!canEncode(font, codePoint)) {
                missing.add(String.format("U+%04X", codePoint));
            }
        }
        return missing;
    }

    /**
     * Whether the family's regular face can encode every code point in the range.
     *
     * <p>Stops at the first miss rather than collecting them, so sweeping every bundled
     * family across a large block costs about as much as the families that actually
     * cover it — the rest fail on their first code point.</p>
     */
    static boolean covers(FontName name, int first, int last) {
        PDFont font = face(name, TextDecoration.DEFAULT);
        for (int codePoint = first; codePoint <= last; codePoint++) {
            if (!canEncode(font, codePoint)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Like {@link #unencodable}, but only over the code points in the range that Unicode
     * defines as letters.
     *
     * <p>A script block is not a solid run of letters: it has unassigned gaps, and
     * punctuation and modifiers sit among the letters. No font carries the gaps, so a
     * plain range sweep reports every family as incomplete and the assertion has to be
     * weakened to a shorter range — which then silently stops covering the letters that
     * live past it. Asking Unicode which code points are letters keeps the range whole
     * and the claim honest, and it moves with the JDK instead of with a hand-kept list.</p>
     */
    static List<String> unencodableLetters(FontName name, TextDecoration decoration, int first, int last) {
        PDFont font = face(name, decoration);
        List<String> missing = new ArrayList<>();
        for (int codePoint = first; codePoint <= last; codePoint++) {
            if (Character.isLetter(codePoint) && !canEncode(font, codePoint)) {
                missing.add(String.format("U+%04X", codePoint));
            }
        }
        return missing;
    }

    /** Whether the family's regular face can encode every letter in the range. */
    static boolean coversLetters(FontName name, int first, int last) {
        PDFont font = face(name, TextDecoration.DEFAULT);
        for (int codePoint = first; codePoint <= last; codePoint++) {
            if (Character.isLetter(codePoint) && !canEncode(font, codePoint)) {
                return false;
            }
        }
        return true;
    }

    /** Whether the family's regular face can encode every one of the given code points. */
    static boolean covers(FontName name, int... codePoints) {
        PDFont font = face(name, TextDecoration.DEFAULT);
        for (int codePoint : codePoints) {
            if (!canEncode(font, codePoint)) {
                return false;
            }
        }
        return true;
    }

    private static boolean canEncode(PDFont font, int codePoint) {
        try {
            font.encode(new String(Character.toChars(codePoint)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
