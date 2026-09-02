package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * The Northline look: colour roles, the Spectral/Lato type scale, and the
 * measured geometry constants shared by the preset's bands.
 *
 * <p>Preset-local rather than a {@code BrandTheme}: the teal-and-navy
 * proposal look is not shared with any other family today, and every value
 * below was measured against the ported template's reference render — a
 * theme slot would add indirection without a second consumer. The Northline
 * orange variant parameterises the colours when it lands.</p>
 *
 * <p>Geometry notes carried over from the ported template: every width is a
 * fraction of {@link #CONTENT_WIDTH}; the only hardcoded points are
 * dimensions nothing else depends on (icon sizes, badge diameter, logo
 * square, footer band height, card padding), each named so a revision
 * changes one constant. {@link #BODY_LEADING} is EXTRA leading in points,
 * not a multiplier — the engine adds it to the font's own line height.</p>
 */
final class NorthlineStyles {

    private NorthlineStyles() {
    }

    // --- page ---------------------------------------------------------------
    static final double PAGE_WIDTH = 595.276;   // A4 portrait, points
    static final double PAGE_HEIGHT = 841.890;
    static final double MARGIN_SIDE = 36.0;
    static final double MARGIN_TOP = 8.0;
    /** The denominator of every derived width below. */
    static final double CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_SIDE;

    // --- band splits, measured from the reference ---------------------------
    static final double SUMMARY_WEIGHT = 0.62;
    static final double GLANCE_WEIGHT = 0.35;
    static final double SUMMARY_GAP = CONTENT_WIDTH * 0.03;
    static final double INVESTMENT_WEIGHT = 0.59;
    static final double TERMS_WEIGHT = 0.35;
    static final double MONEY_GAP = CONTENT_WIDTH * 0.06;
    static final double DELIVERABLES_LEFT_WEIGHT = 0.48;
    static final double DELIVERABLES_RIGHT_WEIGHT = 0.52;
    /** PHASE / FOCUS / DURATION / OUTPUT, from the four measured vertical rules. */
    static final double[] TIMELINE_WEIGHTS = {0.232, 0.298, 0.179, 0.291};
    /** Where the scope description column starts. */
    static final double SCOPE_TITLE_SPLIT = 0.448;

    static final double SUMMARY_WIDTH = CONTENT_WIDTH * SUMMARY_WEIGHT;
    static final double GLANCE_WIDTH = CONTENT_WIDTH * GLANCE_WEIGHT;
    static final double INVESTMENT_WIDTH = CONTENT_WIDTH * INVESTMENT_WEIGHT;
    static final double TERMS_WIDTH = CONTENT_WIDTH * TERMS_WEIGHT;

    // --- independent dimensions ---------------------------------------------
    static final double BADGE_DIAMETER = 27.0;
    static final double BADGE_GLYPH = BADGE_DIAMETER * 0.52;
    static final double BADGE_GAP = BADGE_DIAMETER * 0.42;
    static final double GOAL_ICON = 20.0;
    static final double GOAL_GAP = 7.0;
    /** Lines a goal statement wraps to; sets the separator height. */
    static final double GOAL_TEXT_LINES = 3;
    static final double FACT_ICON = 25.0;
    static final double FACT_GAP = 12.0;
    static final double LOGO_SIZE = 50.0;
    static final double FOOTER_HEIGHT = 48.0;
    static final double PANEL_PADDING = 16.0;
    static final double PANEL_RADIUS = 7.0;
    static final double SCOPE_BADGE_W = 26.0;
    static final double SCOPE_BADGE_H = 15.0;
    static final double SCOPE_GAP = 12.0;
    static final double TITLE_RULE_WIDTH = CONTENT_WIDTH * 0.086;
    static final double PAGE_BLOCK_WIDTH = 62.0;
    static final double HAIRLINE = 0.7;
    /** Helvetica cap height as a fraction of the type size. */
    static final double HELVETICA_CAP = 0.717;
    /** Footer chrome text size (the backend draws footer chrome in Standard-14). */
    static final double FOOTER_TEXT_SIZE = 9.5;
    /**
     * Not the band height: the backend places the footer baseline at
     * (height - fontSize) above the page edge, so this value seats the cap
     * band in the middle of a {@link #FOOTER_HEIGHT} band.
     */
    static final double FOOTER_ZONE_HEIGHT =
            (FOOTER_HEIGHT - FOOTER_TEXT_SIZE * HELVETICA_CAP) / 2 + FOOTER_TEXT_SIZE;
    static final double WORDMARK_WIDTH = CONTENT_WIDTH * 0.19;
    static final double DOC_LABEL_WIDTH = CONTENT_WIDTH * 0.16;
    static final double SIGNATURE_LABEL_WIDTH = CONTENT_WIDTH * 0.115;

    // --- vertical rhythm -----------------------------------------------------
    static final double GAP_SECTION = 18.0;
    /**
     * The gap above a band, carried by the band rather than by the page
     * flow: flow spacing survives a page break and would indent the
     * page-two header, so the flow spacing stays zero and the rhythm lives
     * on the bands — a header carries no leading space on any page.
     */
    static final float BAND_TOP = (float) GAP_SECTION;
    static final double GAP_HEADING = 7.0;
    static final double GAP_ROW = 5.0;
    /** Inner padding of the acceptance card; the glance card keeps PANEL_PADDING. */
    static final double ACCEPTANCE_PADDING = 11.0;
    /** Gap between the acceptance statement and the signature fields. */
    static final double ACCEPTANCE_FIELD_GAP = 13.0;
    /** Vertical padding inside a scope row, each side (measured). */
    static final double SCOPE_ROW_PAD = 5.0;
    /** Vertical padding in an investment cell, each side (measured). */
    static final double INVESTMENT_ROW_PAD = 4.0;
    /** Gap above and below a divider between glance facts. */
    static final double FACT_DIVIDER_GAP = 7.0;
    /** Glance label / value line heights, as multiples of the type size. */
    static final double FACT_LINE = 1.4;
    static final double FACT_VALUE_LINE = 1.3;

    // --- type scale ----------------------------------------------------------
    static final double BODY_SIZE = 10.0;
    /** EXTRA leading in points (added to the font's own line height). */
    static final double BODY_LEADING = 1.45;
    /** Lato line height as a multiple of the type size (measured). */
    static final double LATO_LINE = 1.213;
    static final double TITLE_SIZE = 46.0;
    /** The reference's title pitch between stacked title lines. */
    static final double TITLE_PITCH = 48.0;
    /** Height reserved for the last title line inside the stacked-title container. */
    static final double TITLE_LAST_LINE_HEIGHT = TITLE_SIZE * 0.95;
    static final double HEADING_SIZE = 13.5;
    static final double TABLE_SIZE = 9.0;
    static final double LABEL_SIZE = 8.0;
    static final double FACT_VALUE_SIZE = 14.0;

    // --- theme tokens: role names, not colour names ---------------------------
    static final DocumentColor INK = DocumentColor.rgb(1, 25, 52);
    static final DocumentColor ACCENT = DocumentColor.rgb(20, 111, 120);
    static final DocumentColor PAGE_BACKGROUND = DocumentColor.rgb(252, 252, 252);
    static final DocumentColor PANEL_BACKGROUND = DocumentColor.rgb(245, 246, 247);
    static final DocumentColor RULE = DocumentColor.rgb(203, 208, 213);
    static final DocumentColor RULE_QUIET = DocumentColor.rgb(228, 231, 234);
    static final DocumentColor BODY_TEXT = DocumentColor.rgb(39, 51, 75);
    static final DocumentColor MUTED_TEXT = DocumentColor.rgb(85, 93, 109);
    static final DocumentColor ON_DARK = DocumentColor.rgb(255, 255, 255);

    static final FontName TITLE_FONT = FontName.SPECTRAL;
    static final FontName BODY_FONT = FontName.LATO;

    // --- text roles -----------------------------------------------------------
    static final DocumentTextStyle TITLE_INK =
            style(TITLE_FONT, TITLE_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle TITLE_ACCENT =
            style(TITLE_FONT, TITLE_SIZE, DocumentTextDecoration.BOLD, ACCENT);
    static final DocumentTextStyle SECTION_HEADING =
            style(BODY_FONT, HEADING_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle PANEL_HEADING =
            style(BODY_FONT, BODY_SIZE + 0.5, DocumentTextDecoration.BOLD, ACCENT);
    static final DocumentTextStyle BODY =
            style(BODY_FONT, BODY_SIZE, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle META =
            style(BODY_FONT, LABEL_SIZE, DocumentTextDecoration.BOLD, MUTED_TEXT);
    static final DocumentTextStyle META_SEPARATOR =
            style(BODY_FONT, LABEL_SIZE, DocumentTextDecoration.DEFAULT, RULE);
    static final DocumentTextStyle FACT_LABEL =
            style(BODY_FONT, LABEL_SIZE, DocumentTextDecoration.BOLD, MUTED_TEXT);
    static final DocumentTextStyle FACT_VALUE =
            style(BODY_FONT, FACT_VALUE_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle FACT_NOTE =
            style(BODY_FONT, LABEL_SIZE + 0.5, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle GOAL_TEXT =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle SCOPE_TITLE =
            style(BODY_FONT, TABLE_SIZE + 0.5, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle SCOPE_BODY =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.DEFAULT, MUTED_TEXT);
    static final DocumentTextStyle SCOPE_NUMBER =
            style(BODY_FONT, LABEL_SIZE + 0.5, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle TABLE_HEADER =
            style(BODY_FONT, TABLE_SIZE - 0.5, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle TABLE_BODY =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.DEFAULT, BODY_TEXT);
    static final DocumentTextStyle TABLE_BODY_BOLD =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle TOTAL_TEXT =
            style(BODY_FONT, TABLE_SIZE + 1.5, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle WORDMARK =
            style(BODY_FONT, 16.0, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle MONOGRAM =
            style(BODY_FONT, 30.0, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle DOC_LABEL =
            style(BODY_FONT, LABEL_SIZE + 1, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle SIGNATURE_LABEL =
            style(BODY_FONT, TABLE_SIZE, DocumentTextDecoration.DEFAULT, INK);

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
