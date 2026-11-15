package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;

import java.util.Locale;
import java.util.Set;

/**
 * Maps the engine's {@link FontName} identifiers onto PPTX font families and
 * style flags.
 *
 * <p>PPTX references fonts by family name and lets the viewer resolve them,
 * so the standard-14 PostScript families map to their metric-compatible
 * system fonts (Helvetica → Arial, Times → Times New Roman, Courier →
 * Courier New) — page geometry is safe either way because every line is
 * pre-wrapped and placed by the engine, but metric-compatible glyphs keep
 * intra-line rendering close. Binary families keep their real family name —
 * stripped of a style suffix ({@code Roboto-Bold} → {@code Roboto}) where no
 * registration covers the name, while a registered family declares the
 * {@code wordFamily()} it was given, suffix and all;
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
     * The style suffixes a font name can end in, ordered so that an entry is tried before any
     * shorter entry it itself ends with: {@code -bold-italic} before {@code -italic}, and
     * {@code -bold-oblique} before {@code -oblique}. Read first to last, so the order is part of
     * the rule rather than a tidiness.
     *
     * <p>Read by {@link #nameCarriesStyle} and by nothing else: a style suffix is the only thing a
     * font name is read for here, and the one place it is read decides a run flag. Searching the
     * <em>whole name</em> for a token instead — which is what this class did until the measurements
     * below — is what the list exists to prevent. {@code -Regular} is in it so that a name ending
     * in it is answered "this is a style suffix, and it names no face".</p>
     */
    private static final String[] STYLE_SUFFIXES = {
            "-bolditalic", "-bold-italic", "-boldoblique", "-bold-oblique",
            "-bold", "-italic", "-oblique", "-regular"
    };

    private static final double ARIAL_ASCENT_RATIO = 1854.0 / 2048.0;
    private static final double TIMES_NEW_ROMAN_ASCENT_RATIO = 1825.0 / 2048.0;
    private static final double COURIER_NEW_ASCENT_RATIO = 1705.0 / 2048.0;

    private PptxFontMapping() {
    }

    /**
     * Maps one logical engine font identifier to a viewer-facing PPTX family.
     *
     * <p>The name itself, for anything but a standard-14 alias. It used to come back with a style
     * suffix taken off it, which was a dead branch: a <em>registered</em> family declares its
     * {@code wordFamily()} and never reaches here, a bundled name carries no suffix, and a name
     * nothing registers has to resolve in the font library to be laid out at all — one that does
     * not fails the layout outright ("Font not found for style") before a deck is written. So no
     * reachable name was ever stripped, and the one caller left — the substitution warning's
     * deduplication — now counts such names apart rather than together.</p>
     *
     * @param fontName logical font identifier, or {@code null} for Helvetica
     * @return PPTX family name
     */
    public static String familyFor(FontName fontName) {
        String replacement = standardReplacementFor(fontName);
        if (replacement != null) {
            return replacement;
        }
        return (fontName == null ? FontName.HELVETICA : fontName).name();
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

    /**
     * What a family's own faces leave for its name to say: every token, where the registration
     * cannot name a face at all.
     *
     * <p>Passed by a caller that knows nothing about the family's faces — a bundled or
     * standard-14 name, which no registration covers, and the geometry mirror in the tests. A
     * caller that does know passes what {@code PptxRenderEnvironment} works out from the faces
     * the family carries.</p>
     */
    public static final Set<String> ANY_TOKEN = Set.of("bold", "italic", "oblique");

    /**
     * Returns whether the run selects a bold face.
     *
     * @param style       the span's style
     * @param nameMaySpeak the tokens this family's name is allowed to contribute — see
     *                     {@link #ANY_TOKEN} and {@link #nameCarriesStyle}
     * @return whether the run asks a viewer for a bold face
     */
    public static boolean isBold(TextStyle style, Set<String> nameMaySpeak) {
        TextDecoration decoration = style.decoration();
        if (decoration == TextDecoration.BOLD || decoration == TextDecoration.BOLD_ITALIC) {
            return true;
        }
        return nameMaySpeak.contains("bold") && nameCarriesStyle(style.fontName(), "bold");
    }

    /**
     * Returns whether the run selects an italic face.
     *
     * @param style       the span's style
     * @param nameMaySpeak the tokens this family's name is allowed to contribute
     * @return whether the run asks a viewer for an italic face
     */
    public static boolean isItalic(TextStyle style, Set<String> nameMaySpeak) {
        TextDecoration decoration = style.decoration();
        if (decoration == TextDecoration.ITALIC || decoration == TextDecoration.BOLD_ITALIC) {
            return true;
        }
        return (nameMaySpeak.contains("italic") && nameCarriesStyle(style.fontName(), "italic"))
                || (nameMaySpeak.contains("oblique")
                        && nameCarriesStyle(style.fontName(), "oblique"));
    }

    /**
     * Reports whether a font name's own style suffix should become a run flag.
     *
     * <p><b>Only one of the suffixes {@link #STYLE_SUFFIXES} lists, at the end of the name.</b> A
     * token the name carries any other way is part of the family's own name — {@code Boldoni Text}
     * carries it inside a word, {@code Acme Sans Bold} and {@code Oswald SemiBold} at the end but
     * not behind the hyphen a face name uses — and a flag taken off one of those asks the viewer
     * for a face the layout never measured. A name beginning with a standard-14 family word is
     * excluded before any of this (see below), whatever it ends in.
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
     * without a decoration is measured with the regular metrics. That exclusion is by the name's
     * <em>beginning</em> ({@link #standardReplacementFor}), so a caller's own
     * {@code Courier Prime-Bold} is caught by it too and its suffix never read.</p>
     *
     * <p>Three things take this flag, not one: the run's {@code b} or {@code i} attribute, the
     * record of which faces a family was drawn in — which decides what a bundled family is
     * embedded with — and the viewer ascent that seats the frame
     * ({@code PptxViewerMetrics.ascentPoints}). The first is what the measurements above read; the
     * ascent arm was not demonstrated, the line top being identical in every deck measured.</p>
     *
     * <p><b>And only where the family's own faces cannot say it.</b> {@code nameMaySpeak} is that
     * permission, worked out by {@code PptxRenderEnvironment} from the faces a registered family
     * carries: a family carrying two or more programs distinguishes its faces by the slot a run
     * asks for, so its name says nothing and the decoration decides; a family carrying one program
     * may be named only in the style that program itself is; a family carrying none has nothing but
     * its name. Without that permission a definition under {@code Acme-Sans-Bold} holding a regular
     * file and a bold one flagged every run of itself, and its plain paragraph was drawn from the
     * bold program at 12.90pt over the frame — measured, and now closed, as is the same name over a
     * regular file alone (9.17pt under the frame).</p>
     *
     * <p>What the deck <em>declares</em> is not this reading's business: for a registered family it
     * is {@code wordFamily()}, which defaults to the registered name with the suffix left on. So a
     * definition under {@code Acme-Sans-Bold} that leaves {@code wordFamily} unset declares
     * {@code Acme-Sans-Bold} and, carrying that one bold file, asks for its bold face.</p>
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

}
