package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * Every measurement, colour and type size of the Indigo proposal.
 *
 * <h2>Sizes are solved by width, not by cap height</h2>
 *
 * <p>The design's own face is not bundled. Gothic A1 matches it closely at
 * regular weight and sets 10–20% wide at bold, so a cap-height solve alone put
 * every bold block over its measure. Each size below is instead solved from the
 * measured ink width of the string it sets.</p>
 *
 * <h2>Positions are a map of cap tops</h2>
 *
 * <p>The whole sheet is one vertical map: every text block's cap top in design
 * pixels, in one place. {@link IndigoFlow} turns a cap top into the margin that
 * puts it there, given where the flow has already reached — so a block moves by
 * changing one number here rather than by re-deriving the gaps around it.</p>
 */
final class IndigoStyles {

    private IndigoStyles() {
    }

    // ------------------------------------------------------------------
    // The page, and the one scale everything derives from
    // ------------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    private static final double PAGE_W = PAGE.width();
    private static final double DESIGN_PX_WIDTH = 1054.0;
    private static final double PX = PAGE_W / DESIGN_PX_WIDTH;

    static double px(double designPixels) {
        return designPixels * PX;
    }

    /** Points back into design pixels, for the map's own arithmetic. */
    static double toPx(double points) {
        return points / PX;
    }

    /** Ink margins: the masthead and footer rules both run the same span. */
    private static final double MARGIN_T = px(46);
    static final double MARGIN_R = px(41);
    private static final double MARGIN_B = px(24);
    static final double MARGIN_L = px(49);

    static final DocumentInsets PAGE_MARGIN =
            new DocumentInsets(MARGIN_T, MARGIN_R, MARGIN_B, MARGIN_L);

    static final double CONTENT_W = PAGE_W - MARGIN_L - MARGIN_R;

    /** Where the flow's cursor starts, in design pixels. */
    static final double FLOW_ORIGIN_PX = 46;

    static final double HALF = 0.5;

    // ------------------------------------------------------------------
    // Columns
    // ------------------------------------------------------------------

    /**
     * The two halves of the sheet divide at different x, which is a fact about
     * the design and not a mistake: the upper hairline sits left of the lower
     * one.
     */
    private static final double DIVIDER_UPPER_X_PX = 402;
    static final double DIVIDER_UPPER_TOP_PX = 172;
    static final double DIVIDER_UPPER_H_PX = 646 - 172;
    private static final double DIVIDER_LOWER_X_PX = 503;
    static final double DIVIDER_LOWER_TOP_PX = 1020;
    static final double DIVIDER_LOWER_H_PX = 1361 - 1020;

    private static final double INTRO_SPLIT_PX = 441;
    private static final double LOWER_SPLIT_PX = 538;
    private static final double CONTENT_RIGHT_PX = 1013;

    static final double INTRO_LEFT_W = px(DIVIDER_UPPER_X_PX - 49);
    static final double INTRO_GAP_W = px(INTRO_SPLIT_PX - DIVIDER_UPPER_X_PX);
    static final double LOWER_LEFT_W = px(DIVIDER_LOWER_X_PX - 49);
    static final double LOWER_GAP_W = px(LOWER_SPLIT_PX - DIVIDER_LOWER_X_PX);
    static final double LOWER_RIGHT_INSET = px(CONTENT_RIGHT_PX - 996);
    static final double LOWER_RIGHT_W = px(996 - LOWER_SPLIT_PX);

    /** The hero's own measure, narrower than the column it sits in. */
    static final double HERO_W = CONTENT_W - px(INTRO_SPLIT_PX - 49);
    static final double SUBTITLE_MEASURE_PX = 410;
    static final double ABOUT_MEASURE_PX = 382;
    static final double OVERVIEW_MEASURE_PX = 420;

    // ------------------------------------------------------------------
    // The band
    // ------------------------------------------------------------------

    static final double BAND_TOP_PX = 675;
    static final double BAND_H_PX = 307;

    /**
     * The design fills the band's right with a product photograph. That is a
     * brand asset, and the templates artifact carries none, so the place it
     * occupies is kept — the copy keeps the measure the design solved it against
     * — and left as flat tint.
     */
    static final double BAND_ARTWORK_W = px(584);
    private static final double BAND_BLEED = MARGIN_R;
    static final double BAND_LEFT_W = CONTENT_W + BAND_BLEED - BAND_ARTWORK_W;

    // ------------------------------------------------------------------
    // Colour, sampled from the design
    // ------------------------------------------------------------------

    static final DocumentColor INK = DocumentColor.rgb(0, 0, 0);
    static final DocumentColor BODY = DocumentColor.rgb(124, 128, 138);
    static final DocumentColor ACCENT = DocumentColor.rgb(61, 37, 173);
    static final DocumentColor TINT = DocumentColor.rgb(241, 238, 251);
    static final DocumentColor TILE_DARK = DocumentColor.rgb(20, 26, 37);
    static final DocumentColor RULE_STRONG = DocumentColor.rgb(213, 216, 224);
    static final DocumentColor RULE_SOFT = DocumentColor.rgb(231, 233, 235);

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    /**
     * The family every size below was solved against. The templates artifact
     * carries no fonts: Gothic A1 arrives with {@code graph-compose-fonts} on
     * the classpath, or a caller registers a family of that name on the session
     * itself. With neither, the engine substitutes and every size is solved for
     * type that is not there.
     */
    static final FontName FACE = FontName.GOTHIC_A1;

    private static final double FACE_ASCENT = 0.7979;
    private static final double CAP_REGULAR = 0.7314;
    private static final double CAP_BOLD = 0.7725;
    private static final double TOP_BEARING_REGULAR = FACE_ASCENT - CAP_REGULAR;
    private static final double TOP_BEARING_BOLD = FACE_ASCENT - CAP_BOLD;

    static double topBearing(double size, boolean bold) {
        return (bold ? TOP_BEARING_BOLD : TOP_BEARING_REGULAR) * size;
    }

    /** The design y a box of this size and cap top reaches down to. */
    static double boxBottomPx(double capTopPx, double size, boolean bold) {
        return capTopPx - toPx(topBearing(size, bold)) + toPx(size);
    }

    static final double WORDMARK_SIZE = 28.3;
    static final double KICKER_SIZE = 13.9;
    static final double LABEL_SIZE = 8.1;
    static final double COMPANY_SIZE = 13.2;
    static final double BODY_SIZE = 9.7;
    static final double CONTACT_SIZE = 8.8;
    static final double ATTN_NAME_SIZE = 9.6;
    static final double ATTN_ROLE_SIZE = 8.9;
    static final double HEADLINE_SIZE = 25.5;
    static final double META_LABEL_SIZE = 6.8;
    static final double META_VALUE_SIZE = 7.8;
    static final double ABOUT_BODY_SIZE = 7.8;
    static final double FEATURE_SIZE = 7.9;
    static final double OVERVIEW_SIZE = 7.5;
    static final double STEP_TITLE_SIZE = 7.6;
    static final double STEP_SUB_SIZE = 7.4;
    static final double STEP_NUM_SIZE = 8.0;
    static final double INVEST_SIZE = 7.6;
    static final double TOTAL_LABEL_SIZE = 7.1;
    static final double TOTAL_FIGURE_SIZE = 13.2;
    static final double NOTE_SIZE = 7.4;
    static final double FOOTER_NAME_SIZE = 7.1;
    static final double FOOTER_BODY_SIZE = 7.0;

    /**
     * The foot's mark, solved from the design glyph's ink height rather than
     * from a measured string: what stands in its place is a monogram, and what
     * has to match is how large the mark reads beside the name.
     */
    static final double FOOTER_MARK_SIZE = 19.9;

    static DocumentTextStyle style(double size, DocumentColor color,
                                   DocumentTextDecoration decoration) {
        return new DocumentTextStyle(FACE, size, decoration, color);
    }

    static DocumentTextStyle plain(double size, DocumentColor color) {
        return style(size, color, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle bold(double size, DocumentColor color) {
        return style(size, color, DocumentTextDecoration.BOLD);
    }

    // ------------------------------------------------------------------
    // Rules and shapes
    // ------------------------------------------------------------------

    static final double RULE_THIN = px(1.5);
    static final double RULE_ACCENT = px(3);
    static final double LEFT_DIVIDER_W = px(322 - 49);
    static final double ACCENT_RULE_W = px(58);
    static final double KICKER_RULE_W = px(57);
    static final double META_CIRCLE = px(44);
    static final double META_GLYPH = px(20);
    static final double FEATURE_TILE = px(48);
    static final double FEATURE_RADIUS = px(12);
    static final double FEATURE_GLYPH = px(24);
    static final double FEATURE_PITCH_PX = 110.7;
    static final double STEP_CIRCLE = px(38);
    static final double STEP_CIRCLE_PX = 38;
    static final double STEP_PITCH_PX = 49.5;
    static final double STEP_COL_W = px(110 - 49);
    static final double STEP_CONNECTOR_W = px(2);
    static final double TOTAL_CARD_RADIUS = px(14);
    static final double TOTAL_CARD_H_PX = 60;
    static final double TOTAL_PAD_R = px(996 - 975);
    static final double TOTAL_PAD_L = px(557 - 538);
    static final double INVEST_CELL_PAD_R = px(996 - 979);
    static final double FOOTER_TEXT_COL_W = px(108 - 49);

    // ------------------------------------------------------------------
    // The vertical map, in design pixels: the cap top of every text block
    // ------------------------------------------------------------------

    static final double WORDMARK_CAP = 51;
    static final double KICKER_CAP = 50;
    static final double KICKER_RULE_AT = 90;
    static final double MASTHEAD_RULE_AT = 122;

    static final double PREPARED_LABEL_CAP = 174;
    static final double COMPANY_CAP = 204;
    static final double[] ADDRESS_CAP = {248, 270, 293};
    static final double ADDRESS_EXTRA_PITCH_PX = 23;
    static final double LEFT_DIVIDER_AT = 357;
    static final double ATTN_LABEL_CAP = 388;
    static final double ATTN_NAME_CAP = 419;
    static final double ATTN_ROLE_CAP = 448;
    static final double ATTN_EMAIL_CAP = 472;
    static final double ATTN_PHONE_CAP = 495;

    static final double[] HEADLINE_CAP = {179, 233, 286, 340};
    static final double HEADLINE_EXTRA_PITCH_PX = 53.7;
    static final double ACCENT_RULE_AT = 403;
    static final double SUBTITLE_CAP = 436;
    static final double SUBTITLE_PITCH_PX = 24;

    static final double[] META_PITCH_PX = {130, 141, 150};
    static final double META_CIRCLE_AT = 549;
    static final double META_CIRCLE_PX = 44;
    static final double META_LABEL_CAP = 614;
    static final double META_VALUE_CAP = 636;

    static final double ABOUT_HEADING_CAP = 711;
    static final double ABOUT_BODY_CAP = 743;
    static final double ABOUT_BODY_PITCH_PX = 20;
    static final double FEATURE_TILE_AT = 852;
    static final double FEATURE_TILE_PX = 48;
    static final double FEATURE_LINE1_CAP = 916;
    static final double FEATURE_LINE2_CAP = 935;

    static final double OVERVIEW_HEADING_CAP = 1020;
    static final double OVERVIEW_INTRO_CAP = 1052;
    static final double OVERVIEW_INTRO_PITCH_PX = 20;
    static final double STEP_FIRST_CIRCLE_AT = 1124;
    static final double STEP_TITLE_OFFSET_PX = 1133 - 1124;
    static final double STEP_SUB_OFFSET_PX = 1153 - 1124;

    static final double INVEST_HEADING_CAP = 1020;
    static final double INVEST_FIRST_ROW_CAP = 1061;
    static final double INVEST_ROW_PITCH_PX = 34.5;
    static final double INVEST_RULE_OFFSET_PX = 1084 - 1061;
    static final double TOTAL_CARD_AT = 1196;
    static final double TOTAL_FIGURE_CAP = 1218;
    static final double NOTES_HEADING_CAP = 1285;
    static final double NOTES_FIRST_CAP = 1312;
    static final double NOTES_PITCH_PX = 18.5;

    static final double FOOTER_RULE_AT = 1395;
    static final double FOOTER_MARK_AT = 1416;
    static final double FOOTER_NAME_CAP = 1416;
    static final double FOOTER_ADDRESS_CAP = 1435;
    static final double FOOTER_CONTACT_CAP = 1455;
}
