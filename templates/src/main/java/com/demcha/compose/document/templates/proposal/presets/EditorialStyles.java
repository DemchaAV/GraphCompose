package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * The Editorial Proposal look: page geometry, the colour roles, the
 * Spectral/Lato scale, and the measured widths every band works against.
 *
 * <p>Preset-local for the same reason as its sibling's tokens: every value
 * was measured against the ported template's reference render, and no other
 * family shares this look.</p>
 */
final class EditorialStyles {

    private EditorialStyles() {
    }

    // --- page ---------------------------------------------------------------
    static final double PAGE_WIDTH = 595.276;   // A4 portrait, points
    static final double PAGE_HEIGHT = 841.890;
    static final double MARGIN_SIDE = 30.0;
    static final double MARGIN_TOP = 19.7;
    static final double CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_SIDE;
    static final double FOOTER_BAND = 49.0;

    // --- band splits, measured from the reference ---------------------------
    static final double SUMMARY_WEIGHT = 0.60;
    static final double GLANCE_WEIGHT = 0.338;
    static final double SUMMARY_GAP = CONTENT_WIDTH * (1 - SUMMARY_WEIGHT - GLANCE_WEIGHT);
    static final double INVESTMENT_WEIGHT = 0.596;
    static final double TERMS_WEIGHT = 0.330;
    static final double MONEY_GAP = CONTENT_WIDTH * (1 - INVESTMENT_WEIGHT - TERMS_WEIGHT);
    static final double DELIVERABLES_LEFT_WEIGHT = 0.491;
    static final double DELIVERABLES_COLUMN_WEIGHT = 0.362;
    /** PHASE / FOCUS / DURATION / OUTPUT. */
    static final double[] TIMELINE_WEIGHTS = {0.222, 0.307, 0.179, 0.291};
    static final double SCOPE_TITLE_SPLIT = 0.430;
    static final double SCOPE_DESC_WEIGHT = 0.532;
    static final double INVESTMENT_LABEL_SPLIT = 0.64;

    static final double GLANCE_WIDTH = CONTENT_WIDTH * GLANCE_WEIGHT;
    static final double INVESTMENT_WIDTH = CONTENT_WIDTH * INVESTMENT_WEIGHT;

    // --- independent dimensions ---------------------------------------------
    static final double LOGO_WIDTH = 30.0;
    static final double LOGO_HEIGHT = 26.0;
    static final double LOGO_GAP = 9.0;
    /** Wordmark line pitch, as a fraction of its type size. */
    static final double WORDMARK_LINE = 0.82;
    static final double WORDMARK_WIDTH = CONTENT_WIDTH * 0.19;
    static final double DOC_LABEL_WIDTH = CONTENT_WIDTH * 0.13;
    static final double DOC_LABEL_RULE_WIDTH = CONTENT_WIDTH * 0.115;
    static final double DOC_LABEL_INSET = 8.5;
    static final double FACT_ICON = 20.0;
    static final double FACT_GAP = 14.0;
    static final double GOAL_ICON = 16.0;
    static final double GOAL_GAP = 8.0;
    static final int GOAL_TEXT_LINES = 3;
    static final double PANEL_PADDING = 17.0;
    static final double PANEL_RADIUS = 5.0;
    static final double ACCEPTANCE_PADDING = 9.0;
    static final double ACCEPTANCE_SIDE_PADDING = 18.0;
    static final double SIGNATURE_LABEL_GAP = 4.0;
    static final double SIGNATURE_FIELD_GAP = 15.5;
    static final double SIGNATURE_SPACER = SIGNATURE_FIELD_GAP - 2 * SIGNATURE_LABEL_GAP;
    /** Rule widths of the three signature fields the reference sets. */
    static final double[] SIGNATURE_RULE_WEIGHTS = {0.374, 0.385, 0.241};
    static final double SCOPE_NUMBER_WIDTH = 22.0;
    static final double SCOPE_GAP = 12.0;
    static final double HAIRLINE = 0.7;
    static final double MASTHEAD_RULE = 1.1;
    static final double HEADING_RULE_WIDTH = CONTENT_WIDTH * 0.048;
    static final double HEADING_RULE_THICKNESS = 1.6;

    // --- vertical rhythm -----------------------------------------------------
    /**
     * The gap above a band, carried by the band rather than by the page flow:
     * flow spacing survives a page break, so a band opening a page would
     * inherit a leading gap the first band never had.
     */
    static final float BAND_TOP = 17.8f;
    static final double GAP_MASTHEAD_RULE = 16.1;
    static final float GAP_TITLE = 15.5f;
    static final float GAP_META = 16.0f;
    static final float GAP_PAGE_TWO_OPENING = 19.0f;
    static final double GAP_HEADING = 1.0;
    static final float GAP_AFTER_RULE = 9.5f;
    static final float GAP_BEFORE_TABLE = 13.7f;
    static final float GAP_PARAGRAPH = 9.0f;
    static final double GAP_BEFORE_GOALS_HEADING = 26.5;
    static final double FACT_DIVIDER_GAP = 7.1;
    static final double SCOPE_ROW_PAD = 2.9;
    static final double INVESTMENT_ROW_PAD = 4.0;
    static final double INVESTMENT_EMPHASIS_PAD = 7.0;
    static final double INVESTMENT_TOTAL_PAD = 5.4;
    static final double TIMELINE_ROW_PAD = 7.0;
    static final double TABLE_CELL_INSET = 10.0;
    static final double ACCEPTANCE_FIELD_GAP = 9.0;
    static final float GAP_BULLET = 7.6f;
    static final double LIST_LEADING = 0.87;

    // --- type scale ----------------------------------------------------------
    static final double BODY_SIZE = 10.0;
    /** EXTRA leading in points (the engine adds it to the font's line height). */
    static final double BODY_LEADING = 3.05;
    /** Lato line height as a multiple of the type size (measured). */
    static final double LATO_LINE = 1.213;
    static final double TITLE_SIZE = 41.8;
    static final double TITLE_PITCH = 41.9;
    static final double HEADING_SIZE = 16.3;
    static final double WORDMARK_SIZE = 16.1;
    static final double WORDMARK_SUB_SIZE = 14.0;
    static final double TABLE_SIZE = 9.0;
    static final double LABEL_SIZE = 7.5;
    static final double DOC_LABEL_SIZE = 8.3;
    static final double META_SIZE = 10.5;
    static final double FACT_VALUE_SIZE = 14.0;
    static final double SCOPE_BODY_SIZE = 9.9;
    static final double SCOPE_LEADING = 0.6;
    static final double FOOTER_TEXT_SIZE = 8.5;
    /** Helvetica cap height as a fraction of the type size. */
    static final double HELVETICA_CAP = 0.717;
    /**
     * Not the band height: the backend places the footer baseline at
     * (height - fontSize) above the page edge, so this seats the cap band in
     * the middle of the footer band.
     */
    static final double FOOTER_ZONE_HEIGHT =
            (FOOTER_BAND - FOOTER_TEXT_SIZE * HELVETICA_CAP) / 2 + FOOTER_TEXT_SIZE;
    static final double FOOTER_RULE_Y = PAGE_HEIGHT - FOOTER_BAND;
    /** How far a heading's line box starts above its cap top, as a fraction of the size. */
    static final double HEADING_CAP_LEAD = 0.42;

    // --- colour roles: named for the job, not the hue ------------------------
    static final DocumentColor ACCENT = DocumentColor.rgb(237, 76, 15);
    static final DocumentColor INK = DocumentColor.rgb(35, 35, 35);
    static final DocumentColor BODY_TEXT = DocumentColor.rgb(46, 46, 46);
    static final DocumentColor MUTED_TEXT = DocumentColor.rgb(90, 90, 90);
    static final DocumentColor PAGE_BACKGROUND = DocumentColor.rgb(254, 254, 254);
    static final DocumentColor PANEL_BACKGROUND = DocumentColor.rgb(250, 246, 243);
    static final DocumentColor SUBTOTAL_FILL = DocumentColor.rgb(243, 241, 239);
    static final DocumentColor RULE = DocumentColor.rgb(216, 213, 210);
    static final DocumentColor RULE_QUIET = DocumentColor.rgb(231, 227, 223);
    static final DocumentColor ON_DARK = DocumentColor.rgb(255, 255, 255);

    static final FontName TITLE_FONT = FontName.SPECTRAL;
    static final FontName BODY_FONT = FontName.LATO;

    // --- text roles -----------------------------------------------------------
    static final DocumentTextStyle TITLE_INK =
            style(TITLE_FONT, TITLE_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle TITLE_ACCENT =
            style(TITLE_FONT, TITLE_SIZE, DocumentTextDecoration.BOLD, ACCENT);
    static final DocumentTextStyle SECTION_HEADING =
            style(TITLE_FONT, HEADING_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle BODY =
            style(BODY_FONT, BODY_SIZE, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle META =
            style(BODY_FONT, META_SIZE, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle META_SEPARATOR =
            style(BODY_FONT, META_SIZE, DocumentTextDecoration.DEFAULT, ACCENT);
    static final DocumentTextStyle FACT_LABEL =
            style(BODY_FONT, LABEL_SIZE, DocumentTextDecoration.BOLD, MUTED_TEXT);
    static final DocumentTextStyle FACT_VALUE =
            style(BODY_FONT, FACT_VALUE_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle FACT_NOTE =
            style(BODY_FONT, LABEL_SIZE + 0.5, DocumentTextDecoration.DEFAULT, MUTED_TEXT);
    static final DocumentTextStyle GOAL_TEXT =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle SCOPE_NUMBER =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.BOLD, ACCENT);
    static final DocumentTextStyle SCOPE_TITLE =
            style(BODY_FONT, TABLE_SIZE + 0.5, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle SCOPE_BODY =
            style(BODY_FONT, SCOPE_BODY_SIZE, DocumentTextDecoration.DEFAULT, MUTED_TEXT);
    static final DocumentTextStyle TABLE_HEADER =
            style(BODY_FONT, TABLE_SIZE - 0.5, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle TABLE_BODY =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle TABLE_BODY_BOLD =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle TOTAL_TEXT =
            style(BODY_FONT, TABLE_SIZE + 1.5, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle WORDMARK =
            style(BODY_FONT, WORDMARK_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle WORDMARK_SUB =
            style(BODY_FONT, WORDMARK_SUB_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle DOC_LABEL =
            style(BODY_FONT, DOC_LABEL_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle SIGNATURE_LABEL =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.DEFAULT, INK);

    /**
     * The height one line of body-scale text occupies: the font's own line
     * height plus the additive leading. Multiplying the size by the leading
     * is the wrong model and makes derived heights half again too tall.
     *
     * @param size the type size
     * @return the line height in points
     */
    static double lineHeight(double size) {
        return size * LATO_LINE + BODY_LEADING;
    }

    private static DocumentTextStyle style(FontName font,
                                           double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
