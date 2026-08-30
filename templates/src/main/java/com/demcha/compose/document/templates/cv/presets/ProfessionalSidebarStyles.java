package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.font.FontName;

/**
 * The measured geometry, palette and type scale of the Professional Sidebar
 * CV.
 *
 * <p>Every value here is read off the design rather than derived, which is
 * why so many carry two decimals: the preset reproduces a specific sheet,
 * and rounding them to tidier numbers moves type off its baseline.</p>
 */
final class ProfessionalSidebarStyles {

    private ProfessionalSidebarStyles() {
    }

    // -- page ------------------------------------------------------------

    /**
     * The page: 1024 x 1536 pixels at 150 dpi, which is the frame the design
     * was drawn in rather than a paper size.
     */
    static final DocumentPageSize PAGE = DocumentPageSize.of(491.60, 737.28);

    static final double PAGE_WIDTH = PAGE.width();
    static final double PAGE_HEIGHT = PAGE.height();

    static final double SIDEBAR_WEIGHT = 0.291;
    static final double MAIN_WEIGHT = 1.0 - SIDEBAR_WEIGHT;

    static final double SIDEBAR_WIDTH = PAGE_WIDTH * SIDEBAR_WEIGHT;
    static final double MAIN_WIDTH = PAGE_WIDTH * MAIN_WEIGHT;

    /** The navy plate behind the monogram, as a fraction of page height. */
    static final double HEADER_PLATE_RATIO = 0.134;
    static final double HEADER_PLATE_HEIGHT = PAGE_HEIGHT * HEADER_PLATE_RATIO;

    static final double SIDEBAR_PAD_X = 17.8;
    static final double SIDEBAR_INNER_WIDTH = SIDEBAR_WIDTH - 2.0 * SIDEBAR_PAD_X;

    static final double MAIN_PAD_LEFT = 27.0;
    static final double MAIN_PAD_RIGHT = 20.0;
    static final double MAIN_CONTENT_WIDTH = MAIN_WIDTH - MAIN_PAD_LEFT - MAIN_PAD_RIGHT;

    // -- ornaments -------------------------------------------------------

    static final double MONOGRAM_DIAMETER = 55.2;
    static final double MONOGRAM_STROKE = 0.8;
    static final double MONOGRAM_SIZE = 16.0;
    static final double MONOGRAM_TRACKING_EM = 0.24;

    static final double SECTION_ACCENT_WIDTH = 19.0;
    static final double SECTION_ACCENT_HEIGHT = 1.05;
    static final double IDENTITY_ACCENT_WIDTH = 27.0;

    static final double RULE_THICKNESS = 0.55;

    static final double SKILL_TRACK_WIDTH = SIDEBAR_INNER_WIDTH * 0.44;
    static final double SKILL_TRACK_HEIGHT = 2.8;
    static final double SKILL_ROW_HEIGHT = 8.5;

    static final double EDUCATION_RAIL_X = 2.8;
    static final double EDUCATION_TEXT_X = 13.0;
    static final double EDUCATION_MARKER_DIAMETER = 4.3;
    static final double EDUCATION_RAIL_WIDTH = 0.55;
    static final double EDUCATION_DEGREE_SIZE = 7.3;

    static final int LANGUAGE_DOTS = 5;
    static final double LANGUAGE_RATING_X = SIDEBAR_INNER_WIDTH * 0.49;
    static final double LANGUAGE_DOT_DIAMETER = 5.0;
    static final double LANGUAGE_DOT_GAP = 5.1;
    static final double LANGUAGE_DOT_PITCH = LANGUAGE_DOT_DIAMETER + LANGUAGE_DOT_GAP;
    static final double LANGUAGE_RATING_WIDTH =
            LANGUAGE_DOTS * LANGUAGE_DOT_DIAMETER + (LANGUAGE_DOTS - 1) * LANGUAGE_DOT_GAP;
    static final double LANGUAGE_ROW_HEIGHT = 8.5;

    static final double ENTRY_HEAD_HEIGHT = 9.3;

    // -- type ------------------------------------------------------------

    static final FontName DISPLAY_FONT = FontName.BARLOW_CONDENSED;
    static final FontName BODY_FONT = FontName.LATO;

    static final double BODY_SIZE = 7.1;
    static final double SIDEBAR_HEADING_SIZE = 8.5;
    static final double MAIN_HEADING_SIZE = 8.7;
    static final double NAME_SIZE = 36.7;
    static final double PROFESSIONAL_TITLE_SIZE = 9.5;
    static final double ENTRY_TITLE_SIZE = 7.5;
    static final double META_SIZE = 7.2;
    static final double REFERENCES_SIZE = 7.4;
    static final double BODY_LEADING = 1.18;

    static final double NAME_TRACKING_EM = 0.114;
    static final double HEADING_TRACKING_EM = 0.14;
    static final double ROLE_TRACKING_EM = 0.18;

    /**
     * The advance of a space in the body face, as a fraction of type size.
     * {@link #tracked} sizes its spacer runs against this to hit a tracking
     * measured in ems.
     */
    static final double SPACE_ADVANCE_EM = 0.25;

    // -- vertical rhythm -------------------------------------------------

    static final double SIDEBAR_BODY_TOP = 22.0;
    static final double SIDEBAR_HEADING_TO_RULE = 4.0;
    static final double CONTACT_HEADING_TO_BODY = 16.0;
    static final double STANDARD_HEADING_TO_BODY = 13.5;
    static final double CONTACT_ROW_GAP = 11.8;
    static final double SKILL_ROW_GAP = 9.3;
    static final double EDUCATION_HEADING_TO_BODY = 10.0;
    static final double EDUCATION_LINE_GAP = 2.5;
    static final double EDUCATION_ENTRY_GAP = 11.0;
    static final double LANGUAGE_ROW_GAP = 7.3;

    static final double CONTACT_TO_SKILLS_ABOVE = 24.0;
    static final double CONTACT_TO_SKILLS_BELOW = 18.0;
    static final double SKILLS_TO_EDUCATION_ABOVE = 15.0;
    static final double SKILLS_TO_EDUCATION_BELOW = 18.0;
    static final double EDUCATION_TO_LANGUAGES_ABOVE = 8.0;
    static final double EDUCATION_TO_LANGUAGES_BELOW = 16.0;

    static final double MAIN_PAD_TOP = 21.9;
    static final double NAME_TO_ROLE = 6.3;
    static final double ROLE_TO_RULE = 14.9;
    static final double IDENTITY_TO_PROFILE = 20.5;
    static final double MAIN_HEADING_TO_RULE = 4.0;
    static final double MAIN_HEADING_TO_BODY = 14.0;
    static final double EXPERIENCE_HEADING_TO_BODY = 18.3;
    static final double PROJECT_HEADING_TO_BODY = 8.0;
    static final double REFERENCES_HEADING_TO_BODY = 8.0;
    static final double PROFILE_TO_EXPERIENCE = 24.5;
    static final double ENTRY_HEAD_TO_META = 2.0;
    static final double ENTRY_META_TO_HIGHLIGHTS = 14.3;
    static final double HIGHLIGHT_ITEM_GAP = 4.8;
    static final double ENTRY_TO_DIVIDER = 14.0;
    static final double DIVIDER_TO_ENTRY = 13.0;
    static final double EXPERIENCE_TO_PROJECTS = 22.0;
    static final double PROJECT_HEAD_TO_BODY = 5.0;
    static final double PROJECT_TO_DIVIDER = 6.0;
    static final double DIVIDER_TO_PROJECT = 7.0;
    static final double PROJECTS_TO_REFERENCES = 18.0;

    // -- palette ---------------------------------------------------------

    static final DocumentColor PAGE_BACKGROUND = DocumentColor.WHITE;
    static final DocumentColor SIDEBAR_BACKGROUND = DocumentColor.rgb(247, 248, 250);
    static final DocumentColor PLATE_BACKGROUND = DocumentColor.rgb(11, 39, 77);
    static final DocumentColor TEXT_PRIMARY = DocumentColor.rgb(7, 17, 30);
    static final DocumentColor TEXT_MUTED = DocumentColor.rgb(47, 74, 115);
    static final DocumentColor ACCENT_PRIMARY = DocumentColor.rgb(11, 103, 200);
    static final DocumentColor RULE_MUTED = DocumentColor.rgb(209, 211, 214);
    static final DocumentColor RATING_MUTED = DocumentColor.rgb(194, 196, 199);

    // -- styles ----------------------------------------------------------

    static DocumentTextStyle body() {
        return style(BODY_FONT, BODY_SIZE, TEXT_PRIMARY, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle metaItalic() {
        return style(BODY_FONT, META_SIZE, TEXT_MUTED, DocumentTextDecoration.ITALIC);
    }

    static DocumentTextStyle style(FontName font, double size, DocumentColor color,
                                   DocumentTextDecoration decoration) {
        return TextStyles.of(font, size, decoration, color);
    }

    /**
     * Writes text letter by letter with a sized space between each pair,
     * which is how this design gets its letter-spacing: a text style carries
     * no tracking, so the gap is a run of its own whose type size is chosen
     * to advance by {@code trackingEm} of the surrounding size.
     *
     * @param paragraph  the paragraph being built
     * @param text       the text to space out
     * @param style      the style of the letters
     * @param trackingEm the gap between letters, in ems of {@code style}
     */
    static void tracked(ParagraphBuilder paragraph, String text,
                        DocumentTextStyle style, double trackingEm) {
        DocumentTextStyle spacer = style.withSize(trackingEm * style.size() / SPACE_ADVANCE_EM);
        for (int i = 0; i < text.length(); i++) {
            paragraph.inlineText(String.valueOf(text.charAt(i)), style);
            if (i + 1 < text.length()) {
                paragraph.inlineText(" ", spacer);
            }
        }
    }

    /** Strips a title down to the letters and digits a node name can carry. */
    static String compact(String text) {
        return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "");
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
