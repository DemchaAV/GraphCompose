package com.demcha.compose.loyaut_core.utils;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;

public final class TextSanitizer {

    private static final Map<Integer, String> REPLACEMENTS = Map.ofEntries(
            Map.entry(0x2192, "->"),   // → right arrow
            Map.entry(0x2190, "<-"),   // ← left arrow
            Map.entry(0x2022, "*"),    // • bullet
            Map.entry(0x2013, "-"),    // – en dash
            Map.entry(0x2014, "--"),   // — em dash
            Map.entry(0x2018, "'"),    // ‘
            Map.entry(0x2019, "'"),    // ’
            Map.entry(0x201C, "\""),   // “
            Map.entry(0x201D, "\""),   // ”
            Map.entry(0x2026, "..."),  // … ellipsis
            Map.entry(0x00A0, " ")    // NBSP
    );

    private TextSanitizer() {}

    /** Normalize + replace (no font checks). */
    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        StringBuilder out = new StringBuilder(normalized.length());

        for (int i = 0; i < normalized.length(); ) {
            int cp = normalized.codePointAt(i);
            i += Character.charCount(cp);

            // Keep common whitespace, remove other control chars
            if (Character.isISOControl(cp) && cp != '\n' && cp != '\r' && cp != '\t') {
                continue;
            }

            String candidate = REPLACEMENTS.getOrDefault(cp, new String(Character.toChars(cp)));
            out.append(candidate); // <-- IMPORTANT: append and continue
        }

        return out.toString();
    }


    /** Make text safe for a specific PDFBox font (e.g. PDType1Font.COURIER). */
    public static String sanitizeForFont(String raw, PDFont font) {
        String replaced = sanitize(raw);
        if (font == null || replaced.isBlank()) return replaced;

        StringBuilder out = new StringBuilder(replaced.length());

        for (int i = 0; i < replaced.length(); ) {
            int cp = replaced.codePointAt(i);
            i += Character.charCount(cp);

            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);       // throws if not supported
                out.append(ch);
            } catch (IllegalArgumentException | IOException ex) {
                out.append('?');       // fallback for unsupported / encoding problems
            }
        }

        return out.toString();
    }
}
