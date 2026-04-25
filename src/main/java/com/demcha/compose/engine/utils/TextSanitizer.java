package com.demcha.compose.engine.utils;

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
}
