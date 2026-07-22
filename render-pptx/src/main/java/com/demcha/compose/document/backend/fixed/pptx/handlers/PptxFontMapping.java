package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;

import java.util.Locale;

/**
 * Maps the engine's {@link FontName} identifiers onto PPTX font families and
 * style flags.
 *
 * <p>PPTX references fonts by family name and lets the viewer resolve them,
 * so the standard-14 PostScript families map to their metric-compatible
 * system fonts (Helvetica → Arial, Times → Times New Roman, Courier →
 * Courier New) — page geometry is safe either way because every line is
 * pre-wrapped and placed by the engine, but metric-compatible glyphs keep
 * intra-line rendering close. Binary families keep their real family name
 * with the style suffix stripped ({@code Roboto-Bold} → {@code Roboto});
 * the bold/italic axis travels as run flags, combining the name's own style
 * suffix with the span's {@link TextDecoration}.</p>
 *
 * @since 2.1.0
 */
@Internal
public final class PptxFontMapping {

    private static final double ARIAL_ASCENT_RATIO = 1854.0 / 2048.0;
    private static final double TIMES_NEW_ROMAN_ASCENT_RATIO = 1825.0 / 2048.0;
    private static final double COURIER_NEW_ASCENT_RATIO = 1705.0 / 2048.0;

    private PptxFontMapping() {
    }

    /**
     * Maps one logical engine font identifier to a viewer-facing PPTX family.
     *
     * @param fontName logical font identifier, or {@code null} for Helvetica
     * @return PPTX family name
     */
    public static String familyFor(FontName fontName) {
        String replacement = standardReplacementFor(fontName);
        if (replacement != null) {
            return replacement;
        }
        return stripStyleSuffix((fontName == null ? FontName.HELVETICA : fontName).name());
    }

    /**
     * Returns the metric-compatible viewer family that replaces a standard-14
     * PostScript font in PPTX output, or {@code null} when the name is not a
     * standard-14 family (binary families keep their own name).
     *
     * @param fontName logical document font
     * @return replacement family name, or {@code null}
     */
    public static String standardReplacementFor(FontName fontName) {
        String lower = (fontName == null ? FontName.HELVETICA : fontName)
                .name().toLowerCase(Locale.ROOT);
        if (lower.startsWith("helvetica")) {
            return "Arial";
        }
        if (lower.startsWith("times")) {
            return "Times New Roman";
        }
        if (lower.startsWith("courier")) {
            return "Courier New";
        }
        return null;
    }

    /**
     * Returns the viewer ascent ratio for a standard-14 replacement font.
     *
     * @param fontName logical document font
     * @param fallback ratio to return for a non-standard family
     * @return viewer-font ascent divided by em size
     */
    public static double viewerAscentRatio(FontName fontName, double fallback) {
        String name = (fontName == null ? FontName.HELVETICA : fontName).name();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith("helvetica")) {
            return ARIAL_ASCENT_RATIO;
        }
        if (lower.startsWith("times")) {
            return TIMES_NEW_ROMAN_ASCENT_RATIO;
        }
        if (lower.startsWith("courier")) {
            return COURIER_NEW_ASCENT_RATIO;
        }
        return fallback;
    }

    /** Returns whether the run selects a bold face. */
    public static boolean isBold(TextStyle style) {
        TextDecoration decoration = style.decoration();
        if (decoration == TextDecoration.BOLD || decoration == TextDecoration.BOLD_ITALIC) {
            return true;
        }
        return style.fontName() != null
                && style.fontName().name().toLowerCase(Locale.ROOT).contains("bold");
    }

    /** Returns whether the run selects an italic face. */
    public static boolean isItalic(TextStyle style) {
        TextDecoration decoration = style.decoration();
        if (decoration == TextDecoration.ITALIC || decoration == TextDecoration.BOLD_ITALIC) {
            return true;
        }
        String name = style.fontName() == null ? "" : style.fontName().name().toLowerCase(Locale.ROOT);
        return name.contains("italic") || name.contains("oblique");
    }

    static boolean isUnderline(TextStyle style) {
        return style.decoration() == TextDecoration.UNDERLINE;
    }

    static boolean isStrikethrough(TextStyle style) {
        return style.decoration() == TextDecoration.STRIKETHROUGH;
    }

    private static String stripStyleSuffix(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String suffix : new String[]{
                "-bolditalic", "-boldoblique", "-bold", "-italic", "-oblique", "-regular"
        }) {
            if (lower.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }
}
