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
 * the bold/italic axis travels as run flags, combining the span's
 * {@link TextDecoration} with a style suffix at the <em>end</em> of the name,
 * read off the one list of suffixes this class strips names by and from no
 * other reading of the name (see {@link #nameCarriesStyle}).</p>
 *
 * @since 2.1.0
 */
@Internal
public final class PptxFontMapping {

    /**
     * The style suffixes a font name can end in. No entry is a suffix of another — each begins with
     * the hyphen — so the order they are read in does not matter.
     *
     * <p>One list, read by the two places that must not disagree about what a style token is:
     * {@link #stripStyleSuffix}, which takes the suffix off a name no registration covers, and
     * {@link #nameCarriesStyle}, which turns it into a run flag. Adding a spelling here gives it to
     * both at once; keying either of them on anything else — a substring search, say — is what this
     * list exists to prevent.</p>
     */
    private static final String[] STYLE_SUFFIXES = {
            "-bolditalic", "-boldoblique", "-bold", "-italic", "-oblique", "-regular"
    };

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
        return nameCarriesStyle(style.fontName(), "bold");
    }

    /** Returns whether the run selects an italic face. */
    public static boolean isItalic(TextStyle style) {
        TextDecoration decoration = style.decoration();
        if (decoration == TextDecoration.ITALIC || decoration == TextDecoration.BOLD_ITALIC) {
            return true;
        }
        return nameCarriesStyle(style.fontName(), "italic")
                || nameCarriesStyle(style.fontName(), "oblique");
    }

    /**
     * Reports whether a font name's own style suffix should become a run flag.
     *
     * <p><b>Only a style suffix at the end of the name, read off the one list
     * {@link #STYLE_SUFFIXES} that {@link #familyFor} also reads.</b> A token anywhere else is part
     * of the family's own name — {@code Boldoni Text} and {@code Oswald SemiBold} name families,
     * not faces — and a flag taken off one asks the viewer for a face the layout never measured.
     * Measured in PowerPoint 16 on a 45-character line at 20pt: a family of two faces registered
     * under such a name had its <em>plain</em> paragraph drawn from the bold program, 12.90pt wider
     * than the frame the engine had sized for it, and a family of one regular face under the same
     * name had its line drawn 9.17pt short of it. The span's {@link TextDecoration} already says
     * which face such a family wants.</p>
     *
     * <p>Where a name does end in the suffix, it is the only signal in the one arrangement that
     * needs it: a registration <em>per face</em> under one {@code wordFamily} — {@code Acme-Sans}
     * holding the regular file beside {@code Acme-Sans-Bold} holding the bold one — where no
     * decoration distinguishes the two. Measured, that second paragraph is drawn from the bold
     * program as the page laid it out, and 12.88pt short of it, from the regular program, with the
     * flag taken away. What that costs is the same registration spelled with a space: it loses the
     * flag, and the spelling is the caller's.</p>
     *
     * <p>The standard-14 PostScript variants — {@code Helvetica-Bold}, {@code Times-Italic},
     * {@code Courier-BoldOblique} and their siblings — are excluded before any of this because they
     * are <em>family aliases</em>: the engine resolves each to its regular base family and then
     * picks the real face from the span's {@link TextDecoration}, so a span that names one of them
     * without a decoration is measured with the regular metrics.</p>
     *
     * <p><b>Two limits, both measured, neither reachable by a rule on the name.</b> A
     * <em>single</em> definition registered under a name ending in {@code -Bold} and holding both
     * a regular and a bold file still flags every run of itself: its plain paragraph was drawn from
     * the bold program at the same 12.90pt over the frame. And a name ending in {@code -Bold} over
     * a file that holds a regular program still contributes the flag. Both are the same gap — the
     * style token in the name and the face the file holds are never compared — and closing it means
     * keying the flag on the registration rather than on the name.</p>
     *
     * <p>What the deck <em>declares</em> is not this list's business: for a registered family it is
     * {@code wordFamily()}, which defaults to the registered name with the suffix left on. So a
     * definition under {@code Acme-Sans-Bold} that leaves {@code wordFamily} unset declares
     * {@code Acme-Sans-Bold} and asks for its bold face; the stripping below is reached only by a
     * name no registration covers.</p>
     *
     * @param fontName logical document font, may be {@code null}
     * @param token lowercase style token to look for
     * @return {@code true} when the name may contribute the flag
     */
    private static boolean nameCarriesStyle(FontName fontName, String token) {
        if (fontName == null || standardReplacementFor(fontName) != null) {
            return false;
        }
        String suffix = styleSuffixOf(fontName.name());
        return suffix != null && suffix.contains(token);
    }

    static boolean isUnderline(TextStyle style) {
        return style.decoration() == TextDecoration.UNDERLINE;
    }

    static boolean isStrikethrough(TextStyle style) {
        return style.decoration() == TextDecoration.STRIKETHROUGH;
    }

    /**
     * The style suffix this name ends with, lowercase, or {@code null} where it ends with none.
     *
     * @param name a font name as the caller spelled it
     * @return the matching entry of {@link #STYLE_SUFFIXES}, or {@code null}
     */
    private static String styleSuffixOf(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String suffix : STYLE_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return suffix;
            }
        }
        return null;
    }

    private static String stripStyleSuffix(String name) {
        String suffix = styleSuffixOf(name);
        return suffix == null ? name : name.substring(0, name.length() - suffix.length());
    }
}
