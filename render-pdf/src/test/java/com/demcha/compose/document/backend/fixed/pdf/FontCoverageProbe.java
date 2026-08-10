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

    private static boolean canEncode(PDFont font, int codePoint) {
        try {
            font.encode(new String(Character.toChars(codePoint)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
