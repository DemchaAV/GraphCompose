package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.font.FontName;

/**
 * The measured geometry, palette and type scale of the Navy Sidebar CV.
 *
 * <p>The bands in this design are sized from the type they hold rather than
 * chosen: an entry head is as tall as its marker, and the title and the dates
 * set in it are taller than that, so each is pushed up by half the difference
 * to sit on the marker's axis. Those overflows are computed here so the two
 * ends of the band cannot drift apart.</p>
 */
final class NavySidebarStyles {

    private NavySidebarStyles() {
    }

    // -- page ------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;
    static final double PAGE_WIDTH = PAGE.width();

    static final double SIDEBAR_WEIGHT = 0.297;
    static final double MAIN_WEIGHT = 1.0 - SIDEBAR_WEIGHT;

    static final double SIDEBAR_PAD_X = 26.5;
    static final double PAGE_MARGIN = 36.5;

    /** The gutter between the navy plate and the main column's text. */
    static final double TROUGH = 25.4;

    static final double SIDEBAR_WIDTH = PAGE_WIDTH * SIDEBAR_WEIGHT;
    static final double MAIN_WIDTH = PAGE_WIDTH * MAIN_WEIGHT;
    static final double SIDEBAR_INNER_WIDTH = SIDEBAR_WIDTH - 2.0 * SIDEBAR_PAD_X;
    static final double MAIN_CONTENT_WIDTH = MAIN_WIDTH - TROUGH - PAGE_MARGIN;

    // -- ornaments -------------------------------------------------------

    /** The portrait fills the sidebar's text column exactly. */
    static final double AVATAR_DIAMETER = SIDEBAR_INNER_WIDTH;
    static final double AVATAR_RING_WIDTH = 1.6;

    static final double BADGE_DIAMETER = 22.6;
    static final double BADGE_GAP = 7.3;
    static final double SECTION_INDENT = BADGE_DIAMETER + BADGE_GAP;

    static final double MARKER_DIAMETER = 6.8;
    static final double RAIL_WIDTH = 0.8;
    static final double RAIL_X = BADGE_DIAMETER / 2.0;
    static final double RAIL_MARGIN_LEFT = RAIL_X - RAIL_WIDTH / 2.0;
    static final double ENTRY_TEXT_INSET = SECTION_INDENT - RAIL_MARGIN_LEFT;
    static final double ENTRY_WIDTH = MAIN_CONTENT_WIDTH - RAIL_MARGIN_LEFT;

    static final double SIDEBAR_RULE_THICKNESS = 0.7;
    static final double MAIN_RULE_THICKNESS = 0.7;
    static final double LIST_BULLET_DIAMETER = 2.4;

    // -- type ------------------------------------------------------------

    static final FontName HEADING_FONT = FontName.LATO;
    static final FontName BODY_FONT = FontName.LATO;

    static final double BODY_SIZE = 9.0;
    static final double SUMMARY_SIZE = 10.5;
    static final double NAME_SIZE = 30.0;
    static final double ROLE_SIZE = 13.5;
    static final double HEADING_SIZE = 12.0;
    static final double JOB_TITLE_SIZE = 11.5;
    static final double BODY_LEADING = 1.28;
    static final double BULLET_LEADING = 1.35;

    static final double TRACKING_EM = 0.17;

    /**
     * The advance of a space in Lato, as a fraction of type size.
     * {@link #tracked} sizes its spacer runs against this.
     */
    static final double SPACE_ADVANCE_EM = 0.25;

    // -- bands -----------------------------------------------------------

    static final double HEADING_BAND_HEIGHT = BADGE_DIAMETER;
    static final double JOB_TITLE_BAND_HEIGHT = JOB_TITLE_SIZE * BULLET_LEADING;
    static final double ENTRY_HEAD_BAND_HEIGHT = MARKER_DIAMETER;
    static final double DATE_BAND_HEIGHT = BODY_SIZE * BULLET_LEADING;
    static final double SIDEBAR_BAND_HEIGHT = BODY_SIZE * BULLET_LEADING;

    /** How far the job title rises above the marker band it is set on. */
    static final double TITLE_OVERFLOW =
            (JOB_TITLE_BAND_HEIGHT - ENTRY_HEAD_BAND_HEIGHT) / 2.0;

    /** The same, for the dates set at the other end of that band. */
    static final double DATE_OVERFLOW =
            (DATE_BAND_HEIGHT - ENTRY_HEAD_BAND_HEIGHT) / 2.0;

    static final double LANGUAGE_LABEL_WEIGHT = 0.41;
    static final double LANGUAGE_VALUE_X = SIDEBAR_INNER_WIDTH * LANGUAGE_LABEL_WEIGHT;

    // -- vertical rhythm -------------------------------------------------

    static final double SIDEBAR_PAD_TOP = 25.0;
    static final double AVATAR_TO_HEADING = 22.0;
    static final double SIDEBAR_BLOCK_GAP = 18.0;
    static final double HEADING_TO_RULE = 7.0;
    static final double RULE_TO_BODY = 10.0;
    static final double CONTACT_ROW_GAP = 10.5;
    static final double EDU_DEGREE_GAP = 4.0;
    static final double EDU_LINE_GAP = 1.5;
    static final double EDU_ENTRY_GAP = 13.0;
    static final double SKILL_ITEM_GAP = 2.4;
    static final double LANGUAGE_ROW_GAP = 6.0;

    static final double MAIN_PAD_TOP = 30.0;
    static final double NAME_TO_ROLE = 6.0;
    static final double ROLE_TO_SUMMARY = 16.0;
    static final double RULE_GAP_ABOVE = 16.0;
    static final double RULE_GAP_BELOW = 11.0;
    static final double SUMMARY_RULE_GAP_BELOW = 15.0;
    static final double HEADER_TO_BODY = 11.0;
    static final double ENTRY_TITLE_TO_EMPLOYER = 3.0;
    static final double ENTRY_EMPLOYER_TO_BULLETS = 7.0;
    static final double ENTRY_GAP = 22.0;
    static final double BULLET_ITEM_GAP = 6.1;
    static final double PLAIN_ITEM_GAP = 5.1;

    // -- palette ---------------------------------------------------------

    static final DocumentColor NAVY = DocumentColor.rgb(32, 44, 59);
    static final DocumentColor INK = DocumentColor.rgb(30, 40, 51);
    static final DocumentColor ACCENT = DocumentColor.rgb(43, 84, 149);
    static final DocumentColor BODY = DocumentColor.rgb(83, 87, 92);
    static final DocumentColor SIDEBAR_STRONG = DocumentColor.WHITE;
    static final DocumentColor SIDEBAR_TEXT = DocumentColor.rgb(233, 235, 238);
    static final DocumentColor SIDEBAR_RULE = DocumentColor.rgba(255, 255, 255, 115);
    static final DocumentColor MAIN_RULE = DocumentColor.rgb(228, 230, 233);
    static final DocumentColor RAIL = DocumentColor.rgb(110, 116, 123);
    static final DocumentColor AVATAR_RING = DocumentColor.rgb(226, 228, 231);

    // -- styles ----------------------------------------------------------

    static DocumentTextStyle body() {
        return style(BODY_SIZE, BODY, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle sidebarBody() {
        return style(BODY_SIZE, SIDEBAR_TEXT, DocumentTextDecoration.DEFAULT);
    }

    /**
     * Both faces are Lato, so the weight is what the decoration selects; the
     * split is kept because the design names a heading face and a body face,
     * and a document set in two families would want it back.
     */
    static DocumentTextStyle style(double size, DocumentColor color,
                                   DocumentTextDecoration decoration) {
        boolean bold = decoration == DocumentTextDecoration.BOLD
                || decoration == DocumentTextDecoration.BOLD_ITALIC;
        return TextStyles.of(bold ? HEADING_FONT : BODY_FONT, size, decoration, color);
    }

    /**
     * Writes text letter by letter with a sized space between each pair,
     * which is how this design gets its letter-spacing: a text style carries
     * no tracking, so the gap is a run of its own whose type size is chosen
     * to advance by {@link #TRACKING_EM} of the surrounding size.
     *
     * @param paragraph the paragraph being built
     * @param text      the text to space out
     * @param style     the style of the letters
     */
    static void tracked(ParagraphBuilder paragraph, String text, DocumentTextStyle style) {
        DocumentTextStyle spacer = style.withSize(TRACKING_EM * style.size() / SPACE_ADVANCE_EM);
        for (int i = 0; i < text.length(); i++) {
            paragraph.inlineText(String.valueOf(text.charAt(i)), style);
            if (i < text.length() - 1) {
                paragraph.inlineText(" ", spacer);
            }
        }
    }

    /** Strips a title down to the letters and digits a node name can carry. */
    static String compact(String text) {
        return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "");
    }
}
