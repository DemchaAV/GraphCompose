package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.font.FontName;

/**
 * The measured geometry, palette and type scale of the Charcoal Gold CV.
 *
 * <p>Vertical rhythm here is authored as a <em>pitch</em> — the distance
 * from one line's top to the next — rather than as a margin, which is how
 * the design was drawn. {@link #gap} turns a pitch into the margin to set,
 * by subtracting the line box the type already occupies.</p>
 */
final class CharcoalGoldStyles {

    private CharcoalGoldStyles() {
    }

    // -- page ------------------------------------------------------------

    static final double PAGE_WIDTH = 595.276;

    static final double SIDEBAR_WEIGHT = 0.3197;
    static final double MAIN_WEIGHT = 1.0 - SIDEBAR_WEIGHT;

    static final double SIDEBAR_PAD = 17.0;
    static final double SIDEBAR_PAD_TOP = 24.3;
    static final double SIDEBAR_CONTENT_WIDTH =
            PAGE_WIDTH * SIDEBAR_WEIGHT - 2.0 * SIDEBAR_PAD;

    static final double MAIN_PAD_LEFT = 23.7;
    static final double MAIN_PAD_RIGHT = 28.8;
    static final double MAIN_PAD_TOP = 25.7;
    static final double MAIN_CONTENT_WIDTH =
            PAGE_WIDTH * MAIN_WEIGHT - MAIN_PAD_LEFT - MAIN_PAD_RIGHT;

    // -- type ------------------------------------------------------------

    static final FontName FONT = FontName.LATO;

    static final double BODY_SIZE = 9.1;
    static final double NAME_GIVEN_SIZE = 35.7;
    static final double NAME_FAMILY_SIZE = 45.5;
    static final double JOB_TITLE_SIZE = 11.6;
    static final double MAIN_HEADING_SIZE = 11.0;
    static final double SIDEBAR_HEADING_SIZE = 10.0;
    static final double ROLE_SIZE = 10.5;
    static final double EMPLOYER_SIZE = 6.9;
    static final double DETAIL_SIZE = 7.8;
    static final double SMALL_SIZE = 7.9;

    /** The share of its type size a line box occupies at the default leading. */
    static final double LINE_FACTOR = 1.2;

    // -- palette ---------------------------------------------------------

    static final DocumentColor SIDEBAR = DocumentColor.rgb(39, 45, 50);
    static final DocumentColor PAPER = DocumentColor.rgb(254, 254, 254);
    static final DocumentColor ACCENT = DocumentColor.rgb(186, 148, 88);
    static final DocumentColor INK = DocumentColor.rgb(39, 45, 50);
    static final DocumentColor SIDEBAR_INK = DocumentColor.rgb(251, 251, 251);
    static final DocumentColor RULE = DocumentColor.rgb(218, 218, 219);
    static final DocumentColor SIDEBAR_RULE = DocumentColor.rgb(61, 67, 69);
    static final DocumentColor RATING_EMPTY = DocumentColor.rgb(119, 122, 125);

    // -- ornaments -------------------------------------------------------

    static final double ACCENT_BAR_WIDTH = 1.7;
    static final double HEADING_INDENT = 9.0;
    static final double RULE_THICKNESS = 0.6;
    static final double MARKER_DIAMETER = 6.2;
    static final double RATING_DOT_DIAMETER = 4.5;
    static final double SKILL_BULLET_DIAMETER = 2.3;

    /**
     * The bullet gets a column of its own rather than an inline run plus
     * counted spaces: its width is the design's own name indent, so neither
     * the mark nor the name depends on how wide a space happens to be.
     */
    static final double SKILL_BULLET_COLUMN = 9.5;

    static final double PHOTO_RING_WIDTH = 1.2;
    static final double PHOTO_DIAMETER = SIDEBAR_CONTENT_WIDTH * 0.802;

    // -- sidebar rhythm --------------------------------------------------

    static final double PHOTO_TO_CONTACT = 29.4;
    static final double HEADING_TO_BODY = 12.0;
    static final double BLOCK_TO_DIVIDER = 19.0;
    static final double DIVIDER_TO_HEADING = 17.0;
    static final double CONTACT_PITCH = 19.2;
    static final double SKILL_PITCH = 16.6;
    static final double LANGUAGE_PITCH = 16.9;
    static final double EDUCATION_LINE_PITCH = 12.4;
    static final double EDUCATION_ENTRY_GAP = 13.7;
    static final double EDUCATION_INDENT = 12.4;

    static final double LANGUAGE_NAME_WEIGHT = 0.36;
    static final double SKILL_NAME_WEIGHT = 0.72;

    // -- main rhythm -----------------------------------------------------

    static final double MASTHEAD_TO_TITLE = 0.0;
    static final double TITLE_TO_RULE = 12.0;
    static final double MASTHEAD_RULE_WIDTH = MAIN_CONTENT_WIDTH * 0.093;
    static final double MASTHEAD_RULE_THICKNESS = 1.4;
    static final double RULE_TO_SUMMARY = 17.8;
    static final double SUMMARY_LEADING = 1.38;
    static final double SUMMARY_TO_EXPERIENCE = 28.4;
    static final double EXPERIENCE_TO_ENTRIES = 19.0;
    static final double ENTRY_GAP = 19.6;
    static final double ENTRIES_TO_RULE = 18.0;
    static final double CREDENTIALS_TO_TOOLS = 13.7;

    static final double DATE_WEIGHT = 0.194;
    static final double MARKER_WEIGHT = MARKER_DIAMETER / MAIN_CONTENT_WIDTH;
    static final double ENTRY_WEIGHT = 1.0 - DATE_WEIGHT - MARKER_WEIGHT;
    static final double ENTRY_INDENT = 16.0;
    static final double ROLE_TO_EMPLOYER = 3.0;
    static final double EMPLOYER_TO_HIGHLIGHTS = 7.0;
    static final double HIGHLIGHT_ITEM_GAP = 4.0;
    static final double HIGHLIGHT_LEADING = 1.3;

    // -- credentials -----------------------------------------------------

    static final double CREDENTIAL_LEFT_WEIGHT = 0.402;
    static final double CREDENTIAL_GUTTER_WEIGHT = 0.124;
    static final double CREDENTIAL_RIGHT_WEIGHT =
            1.0 - CREDENTIAL_LEFT_WEIGHT - CREDENTIAL_GUTTER_WEIGHT;
    static final double CREDENTIAL_ICON_WEIGHT = 0.14;
    static final double CREDENTIAL_HALF_GUTTER =
            MAIN_CONTENT_WIDTH * CREDENTIAL_GUTTER_WEIGHT / 2.0;
    static final double RULE_TO_CREDENTIALS = 22.0;
    static final double CREDENTIAL_ENTRY_GAP = 6.5;
    static final double CREDENTIAL_HEADING_TO_BODY = 17.3;
    static final double CREDENTIAL_LINE_GAP = 2.0;

    /** The separator between two tools on the closing strip. */
    static final String TOOL_SEPARATOR = "     |     ";

    // -- helpers ---------------------------------------------------------

    static DocumentTextStyle textStyle(double size, DocumentColor color, boolean bold) {
        return TextStyles.of(FONT, size,
                bold ? DocumentTextDecoration.BOLD : DocumentTextDecoration.DEFAULT, color);
    }

    /**
     * The margin that realises an authored pitch: the distance between two
     * line tops, less the line box the type above already fills.
     *
     * @param pitch the distance the design draws between line tops
     * @param size  the type size of the line above
     * @return the margin to set, never negative
     */
    static double gap(double pitch, double size) {
        return Math.max(0.0, pitch - size * LINE_FACTOR);
    }

    /**
     * Letter-spacing by spaces rather than by sized runs: a space between
     * letters and three between words.
     *
     * <p>This design tracks only one line — the job title — and does it
     * coarsely enough that real spaces reach it, which is simpler than the
     * per-letter spacer runs the other ported sheets need.</p>
     *
     * @param text the text to space out
     * @return the spaced text
     */
    static String tracked(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() * 2);
        boolean startOfWord = true;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                out.append("   ");
                startOfWord = true;
                continue;
            }
            if (!startOfWord) {
                out.append(' ');
            }
            out.append(ch);
            startOfWord = false;
        }
        return out.toString();
    }

    /** Strips a title down to the letters and digits a node name can carry. */
    static String compact(String text) {
        return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "");
    }
}
