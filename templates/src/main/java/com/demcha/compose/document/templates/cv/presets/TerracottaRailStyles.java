package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * The measured geometry, palette and type scale of the Terracotta Rail CV.
 *
 * <p>Every size here is derived from one body size and every width from the
 * page's own, so the sheet is one edit rather than a walk through the
 * regions that draw it.</p>
 */
final class TerracottaRailStyles {

    private TerracottaRailStyles() {
    }

    // -- the two columns --------------------------------------------------

    /**
     * The width every horizontal measure here is a share of: A4's, as the
     * design drew it. It is a constant rather than the session's own page,
     * because the sheet is a fixed composition — the columns and the bands
     * inside them are shares of this number, and a caller who sets another
     * page gets the same sheet on it rather than a rescaled one.
     */
    static final double PAGE_WIDTH = 595.276;

    static final double SIDEBAR_WEIGHT = 0.308;
    static final double MAIN_WEIGHT = 1.0 - SIDEBAR_WEIGHT;

    static final double SIDEBAR_PAD_LEFT = 25.4;
    static final double SIDEBAR_PAD_RIGHT = 11.4;
    static final double SIDEBAR_PAD_TOP = 20.0;
    static final double COLUMN_PAD_BOTTOM = 5.0;
    static final double MAIN_PAD_LEFT = 22.0;
    static final double MAIN_PAD_RIGHT = 24.0;
    static final double MAIN_PAD_TOP = 20.0;

    static final double MAIN_CONTENT_WIDTH =
            PAGE_WIDTH * MAIN_WEIGHT - MAIN_PAD_LEFT - MAIN_PAD_RIGHT;

    // -- type -------------------------------------------------------------

    static final FontName SANS = FontName.LATO;
    static final FontName SERIF = FontName.PT_SERIF;

    static final double BODY_SIZE = 8.6;
    static final double DETAIL_SIZE = BODY_SIZE * 0.98;
    static final double ITEM_TITLE_SIZE = BODY_SIZE * 1.14;
    static final double SECTION_HEADING_SIZE = BODY_SIZE * 1.02;
    static final double MONOGRAM_SIZE = 36.0;
    static final double NAME_SIZE = 28.0;
    static final double SUBTITLE_SIZE = 13.0;

    /**
     * How much smaller a link is set than the channels above it. A URL is one
     * long token that cannot be broken, so it is the line that outgrows this
     * sidebar first.
     */
    static final double LINK_SCALE = 0.85;

    // -- palette ----------------------------------------------------------

    static final DocumentColor ACCENT = DocumentColor.rgb(201, 74, 41);
    static final DocumentColor INK = DocumentColor.rgb(26, 29, 32);
    static final DocumentColor MUTED = DocumentColor.rgb(85, 90, 96);
    static final DocumentColor RULE = DocumentColor.rgb(216, 216, 216);
    static final DocumentColor PAPER = DocumentColor.WHITE;

    static final DocumentStroke NO_BORDER = DocumentStroke.of(PAPER, 0.0);

    // -- rules, rails and the rhythm between blocks ------------------------

    static final double RULE_THICKNESS = 0.6;
    static final double ACCENT_RULE_THICKNESS = 1.4;
    static final double MARKER_DIAMETER = 6.2;

    /** How far an entry sits from the rail its marker rides. */
    static final double ENTRY_INDENT = 13.0;
    static final double ENTRY_GAP = 9.0;

    static final double HEADING_TO_BODY_GAP = 9.0;
    static final double HEADING_TO_DASH_GAP = 3.5;
    static final double SIDEBAR_DASH_WIDTH = 20.0;
    static final double MAIN_DASH_WIDTH = 22.0;

    static final double SIDEBAR_DIVIDER_GAP = 8.0;
    static final double MAIN_DIVIDER_TOP = 6.0;
    static final double MAIN_DIVIDER_BOTTOM = 6.5;
    static final double MASTHEAD_DIVIDER_TOP = 4.0;
    static final double MASTHEAD_DIVIDER_BOTTOM = 6.0;

    static final double MONOGRAM_RULE_WIDTH = 48.0;
    static final double MONOGRAM_TO_RULE_GAP = 8.0;
    static final double MONOGRAM_RULE_TO_CONTACT_GAP = 10.0;
    static final double NAME_TO_SUBTITLE_GAP = 8.0;
    static final double SUBTITLE_TO_RULE_GAP = 5.0;

    static final double CONTACT_ROW_GAP = 4.5;
    static final double BULLET_ROW_GAP = 3.0;
    static final double INFO_BLOCK_GAP = 7.0;
    static final double INFO_LINE_GAP = 0.5;
    static final double PROJECT_ROW_GAP = 2.0;
    static final double PROJECT_DIVIDER_GAP = 2.0;
    static final double SUMMARY_LINE_GAP = 2.5;
    static final double SUMMARY_LINE_SPACING = 1.30;
    static final double EMPLOYER_GAP = 2.5;
    static final double HIGHLIGHT_ITEM_SPACING = 1.2;
    static final double HIGHLIGHT_LINE_SPACING = 1.15;

    // -- the bands the entries are laid on ---------------------------------

    /**
     * The slack a fixed table needs inside the cell it spans. Without it a
     * table exactly as wide as its cell is one rounding step too wide.
     */
    static final double TABLE_SLACK = 0.01;

    static final double ENTRY_WIDTH = MAIN_CONTENT_WIDTH - ENTRY_INDENT - TABLE_SLACK;
    static final double ROLE_PERIOD_SHARE = 0.30;
    static final double EDUCATION_PERIOD_SHARE = 0.34;

    /** Education stops short of the column and closes on a hairline. */
    static final double EDUCATION_BAND_WEIGHT = 0.70;
    static final double EDUCATION_BAND_PAD_RIGHT = 8.0;
    static final double EDUCATION_ENTRY_WIDTH =
            MAIN_CONTENT_WIDTH * EDUCATION_BAND_WEIGHT - EDUCATION_BAND_PAD_RIGHT
                    - ENTRY_INDENT - TABLE_SLACK;

    static final double[] PROJECT_WEIGHTS = {0.08, 0.36, 0.56};
    static final double PROJECT_META_PAD_RIGHT = 8.0;
    static final double PROJECT_META_PAD_LEFT = 4.0;
    static final double PROJECT_DESCRIPTION_PAD_LEFT = 10.0;
    static final double PROJECT_LINE_SPACING = 1.24;
    static final double[] INFO_WEIGHTS = {0.18, 0.82};

    // -- tracking ----------------------------------------------------------

    /**
     * The gaps a heading is letter-spaced with. A text style carries no
     * tracking, so a tracked line is written with a space character between
     * the letters, and which space is the whole choice: the sidebar takes a
     * hair space (a tenth of an em) because a full one would set ADDITIONAL
     * INFORMATION wider than the column and wrap it, and the main column,
     * being wider, takes a thin space (a fifth).
     */
    static final char SIDEBAR_HEADING_SPACER = '\u200A';
    static final char MAIN_HEADING_SPACER = '\u2009';

    // -- helpers ------------------------------------------------------------

    static DocumentTextStyle text(double size, DocumentColor color, boolean bold) {
        return DocumentTextStyle.builder()
                .fontName(SANS)
                .size(size)
                .color(color)
                .decoration(bold ? DocumentTextDecoration.BOLD : DocumentTextDecoration.DEFAULT)
                .build();
    }

    static DocumentTextStyle italic(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(SANS)
                .size(size)
                .color(color)
                .decoration(DocumentTextDecoration.ITALIC)
                .build();
    }

    static DocumentTextStyle serif(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(SERIF)
                .size(size)
                .color(color)
                .decoration(DocumentTextDecoration.DEFAULT)
                .build();
    }
}
