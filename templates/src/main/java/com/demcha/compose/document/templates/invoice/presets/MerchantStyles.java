package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * Every measurement, colour and type size of the Merchant invoice.
 *
 * <h2>Two scales, and a third for squares</h2>
 *
 * <p>The design's aspect is not A4's, so one conversion constant cannot serve
 * both axes: {@link #px} is horizontal and carries widths and type sizes,
 * {@link #py} is vertical and carries heights and pitches, and {@link #sq} is
 * their mean for the marks and discs, which cannot match both.</p>
 *
 * <h2>Sizes are solved from ink, positions from cap tops</h2>
 *
 * <p>The design's own face is not bundled, so each size is solved from the
 * measured ink width of the string it sets and is named here by that string.
 * Vertical placement is by cap top rather than by box edge, because ink is what
 * a design can be measured on.</p>
 */
final class MerchantStyles {

    private MerchantStyles() {
    }

    // ------------------------------------------------------------------
    // The page and its scales
    // ------------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    static final double PAGE_W = PAGE.width();
    private static final double PAGE_H = PAGE.height();

    private static final double REF_W = 1054.0;
    private static final double REF_H = 1492.0;

    private static final double PX = PAGE_W / REF_W;
    private static final double PY = PAGE_H / REF_H;
    private static final double SQ = (PX + PY) / 2.0;

    static double px(double designPixels) {
        return designPixels * PX;
    }

    static double py(double designPixels) {
        return designPixels * PY;
    }

    static double sq(double designPixels) {
        return designPixels * SQ;
    }

    static final double MARGIN_L = px(35);
    static final double MARGIN_R = px(38);
    static final double MARGIN_B_FIRST = py(6);

    /**
     * The enumeration's band, and the reason a continuation page reserves more
     * than page one: page one ends in a footer band the design already leaves
     * room under, where a continuation page ends in table rows, and a table row
     * has no hole in its middle for a number to sit in.
     */
    static final double PAGE_NUMBER_BAND = 10.0;
    static final double MARGIN_B_LATER = PAGE_NUMBER_BAND + py(6);

    static final double CONTENT_W = PAGE_W - MARGIN_L - MARGIN_R;

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    /**
     * The design's own face is not bundled. Gothic A1 is the family every size
     * below was solved against; the templates artifact carries no fonts —
     * register it on the session, or the engine substitutes and every size is
     * solved for type that is not there.
     */
    static final FontName FACE = FontName.GOTHIC_A1;

    private static final double FACE_ASCENT = 0.7979;
    private static final double FACE_DESCENT = 0.2021;
    private static final double CAP_REGULAR = 0.7314;
    private static final double CAP_BOLD = 0.7725;

    static final double LINE_BOX = FACE_ASCENT + FACE_DESCENT;
    static final double TOP_BEARING_REGULAR = FACE_ASCENT - CAP_REGULAR;
    static final double TOP_BEARING_BOLD = FACE_ASCENT - CAP_BOLD;

    /** Gothic A1's space advance, for the two places the design sets a measured gap. */
    private static final double FACE_SPACE_ADVANCE = 0.2441;

    /**
     * A size solved from a string's measured ink <em>width</em>.
     *
     * <p>Width, not cap band, and deliberately: an ink width measured over 100 to
     * 350 design pixels is good to about 1%, where a cap band measured over 12 is
     * good to about 7%. Every size below names the string it was solved from.</p>
     */
    static double sizeForInk(double designPixelsPerEm) {
        return px(designPixelsPerEm);
    }

    static double topBearing(double size, boolean bold) {
        return (bold ? TOP_BEARING_BOLD : TOP_BEARING_REGULAR) * size;
    }

    /** The margin that puts a cap top at a measured design y. */
    static DocumentInsets capTop(double capPx, double size, boolean bold) {
        return new DocumentInsets(py(capPx) - topBearing(size, bold), 0, 0, 0);
    }

    /** The gap that puts the next cap top {@code deltaPx} below the previous one. */
    static double capGap(double deltaPx, double sizeAbove, boolean boldAbove,
                         double sizeBelow, boolean boldBelow) {
        return py(deltaPx) - LINE_BOX * sizeAbove + topBearing(sizeAbove, boldAbove)
                - topBearing(sizeBelow, boldBelow);
    }

    /**
     * The gap that puts an <em>image's</em> top a measured distance under the cap
     * top of the text above it.
     *
     * <p>{@link #capGap} removes a top bearing for the box below, because a text
     * box starts above its own first cap. An image does not — its box top is its
     * ink top — so that term is not there to remove.</p>
     */
    static double inkGapToImage(double deltaPx, double sizeAbove, boolean boldAbove) {
        return Math.max(0,
                py(deltaPx) - LINE_BOX * sizeAbove + topBearing(sizeAbove, boldAbove));
    }

    /** A run of spaces advancing a measured design width at a given size. */
    static String spaces(double widthPx, double size) {
        int count = (int) Math.round(px(widthPx) / (FACE_SPACE_ADVANCE * size));
        return " ".repeat(Math.max(1, count));
    }

    // Every size is named by the string it was solved from.
    static final double TITLE_SIZE = sizeForInk(64.92);
    static final double SUPPLIER_NAME_SIZE = sizeForInk(18.69);
    static final double BODY_SIZE = sizeForInk(15.69);
    static final double PARTY_LABEL_SIZE = sizeForInk(15.82);
    static final double CONTACT_SIZE = sizeForInk(14.74);
    static final double PARTY_NAME_SIZE = sizeForInk(17.39);
    static final double TABLE_HEAD_SIZE = sizeForInk(14.98);
    static final double ITEM_TITLE_SIZE = sizeForInk(16.11);
    static final double CELL_SIZE = sizeForInk(14.72);
    static final double TOTALS_LABEL_SIZE = sizeForInk(15.18);
    static final double TOTALS_VALUE_SIZE = sizeForInk(16.34);
    static final double TOTAL_DUE_LABEL_SIZE = sizeForInk(19.16);
    static final double TOTAL_DUE_VALUE_SIZE = sizeForInk(25.01);
    static final double PANEL_HEAD_SIZE = sizeForInk(15.27);
    static final double PANEL_ROW_SIZE = sizeForInk(13.51);
    static final double PANEL_NOTE_SIZE = sizeForInk(14.00);
    static final double DUE_LABEL_SIZE = sizeForInk(14.83);
    static final double DUE_VALUE_SIZE = sizeForInk(20.31);
    static final double CLOSING_BOLD_SIZE = sizeForInk(14.49);
    static final double CLOSING_REG_SIZE = sizeForInk(14.02);
    static final double FOOTER_NAME_SIZE = sizeForInk(13.57);
    static final double FOOTER_ADDR_SIZE = sizeForInk(13.72);
    static final double FOOTER_SITE_SIZE = sizeForInk(12.75);
    static final double PAGE_NUMBER_SIZE = sizeForInk(10.0);

    static DocumentTextStyle style(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FACE).size(size).color(color)
                .decoration(DocumentTextDecoration.DEFAULT).build();
    }

    static DocumentTextStyle bold(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FACE).size(size).color(color)
                .decoration(DocumentTextDecoration.BOLD).build();
    }

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    static final DocumentColor INK = DocumentColor.rgb(12, 27, 51);
    static final DocumentColor ACCENT = DocumentColor.rgb(44, 125, 47);
    static final DocumentColor SURFACE_SOFT = DocumentColor.rgb(247, 249, 246);
    static final DocumentColor BORDER_SOFT = DocumentColor.rgb(228, 231, 230);
    static final DocumentColor RULE_STRONG = DocumentColor.rgb(198, 200, 209);
    static final DocumentColor RULE_SOFT = DocumentColor.rgb(221, 223, 228);
    static final DocumentColor RULE_TOTALS = DocumentColor.rgb(145, 178, 130);
    static final DocumentColor ON_FILL = DocumentColor.WHITE;
    static final DocumentColor WHITE = DocumentColor.WHITE;

    // ------------------------------------------------------------------
    // The masthead's top, where two blocks disagree about where the page starts
    // ------------------------------------------------------------------

    /** The lockup's ink and the title's box start at different heights; the flow opens at the earlier. */
    static final double LOGO_INK_TOP = py(41);
    static final double TITLE_BOX_TOP = py(49) - TOP_BEARING_BOLD * TITLE_SIZE;
    static final double MARGIN_TOP = Math.min(LOGO_INK_TOP, TITLE_BOX_TOP);
    static final double LOGO_MARGIN_T = LOGO_INK_TOP - MARGIN_TOP;
    static final double TITLE_MARGIN_T = TITLE_BOX_TOP - MARGIN_TOP;

    static final DocumentInsets PAGE_MARGIN_FIRST =
            new DocumentInsets(MARGIN_TOP, MARGIN_R, MARGIN_B_FIRST, MARGIN_L);
    static final DocumentInsets PAGE_MARGIN_LATER =
            new DocumentInsets(MARGIN_TOP, MARGIN_R, MARGIN_B_LATER, MARGIN_L);

    // ------------------------------------------------------------------
    // Rules, discs and blocks
    // ------------------------------------------------------------------

    /** A rule asked for less than a point is laid out as one. */
    static final double RULE_BOX = 1.0;

    static final double RULE_THIN = py(1.6);
    static final double RULE_MEDIUM = py(2.2);
    static final double RULE_ROW = py(0.8);
    static final double DIVIDER_W = px(2);
    static final double PANEL_RADIUS = px(10);
    static final double TILE_RADIUS = sq(9);
    static final double PARTY_DISC_D = sq(44);
    static final double PARTY_DISC_GLYPH = sq(23);

    static final double LOGO_W = px(222);
    static final double LOGO_H = px(64);
    static final double TITLE_RULE_W = px(56);
    static final double LEFT_COLUMN_W = px(526 - 35);
    static final double CONTACT_ICON = sq(18);
    static final double CONTACT_GAP_PX = 19.0;
    static final double SUPPLIER_TAX_GAP_PX = 17.0;
    static final double PARTY_TAX_GAP_PX = 26.0;
    static final double META_PAD_L = px(593 - 528);
    static final double META_LABEL_W = px(773 - 593);
    static final double PARTY_GUTTER = px(97 - 35);
    static final double BILL_TO_PAD_L = px(1);
    static final double SHIP_TO_PAD_L = px(560 - 528);
    static final double PARTY_BLOCK_H = py(640 - 451);

    // ------------------------------------------------------------------
    // The line-item table
    // ------------------------------------------------------------------

    static final double TABLE_ROW_PITCH_PX = (974 - 707) / 4.0;
    static final double TABLE_HEADER_H = py(40);
    static final double TABLE_ROW_H = py(TABLE_ROW_PITCH_PX);
    static final double TABLE_PAD_HEAD = px(79);
    static final double TABLE_PAD_ICON = px(19);
    static final double TABLE_PAD_EDGE = px(8);
    static final double TABLE_PAD_CENTRED = px(4);
    static final double TABLE_ICON = sq(31);
    static final double TABLE_ICON_GUTTER = px(110 - 54);

    /** The five columns, as shares of the content width, solved from the design. */
    static final double[] COLUMN_SHARES = {0.308359, 0.200306, 0.135576, 0.183486, 0.172273};

    /** Only the description is left-aligned; the four columns after it are centred. */
    static final boolean[] COLUMN_CENTRED = {false, true, true, true, true};

    // ------------------------------------------------------------------
    // Settlement
    // ------------------------------------------------------------------

    static final double PANEL_TOP_Y = 996;
    static final double PANEL_ICON_Y = 1009;
    static final double PANEL_ROW1_CAP_Y = 1048;
    static final double PANEL_ROW_PITCH_Y = 22.5;
    static final double PANEL_LAST_ROW_CAP_Y = 1228;
    static final double PANEL_RULE_Y = 1254;
    static final double PANEL_NOTE_CAP_Y = 1268;
    static final double PANEL_NOTE_PITCH_Y = 26;
    static final double PANEL_BOTTOM_Y = 1323;

    static final double TOTALS_ROW1_CAP_Y = 1018;
    static final double TOTALS_ROW_PITCH_Y = 37;
    static final double TOTALS_RULE_Y = 1094;
    static final double TOTALS_DUE_CAP_Y = 1123;

    static final double CARD_TOP_Y = 1183;
    static final double CARD_BOTTOM_Y = 1287;
    static final double CARD_ICON_Y = 1212;
    static final double CARD_LABEL_CAP_Y = 1214;
    static final double CARD_VALUE_CAP_Y = 1240;

    static final double PANEL_W = px(435 - 35);
    static final double SUMMARY_GAP_W = px(555 - 435);
    static final double PANEL_PAD_L = px(54 - 35);
    static final double PANEL_PAD_R = px(20);
    static final double PANEL_ICON = sq(26);
    static final double PANEL_HEAD_GUTTER = px(101 - 55);
    static final double PANEL_ICON_PAD_L = px(55 - 35);
    static final double PANEL_LABEL_W = px(221 - 54);
    static final double PANEL_NOTE_ICON = sq(24);
    static final double PANEL_NOTE_GUTTER = px(88 - 54);
    static final double PANEL_RULE_W = px(418 - 46);
    static final double PANEL_RULE_PAD_L = px(46 - 35);

    static final double TOTALS_VALUE_PAD_R = px(1016 - 981);
    static final double TOTALS_RULE_W = px(986 - 555);
    static final double CARD_ICON = sq(45);
    static final double CARD_PAD_L = px(580 - 555);
    static final double CARD_GUTTER = px(644 - 580);

    // ------------------------------------------------------------------
    // Closing note and footer
    // ------------------------------------------------------------------

    static final double NOTE_TOP_Y = 1349;
    static final double NOTE_LINE1_CAP_Y = 1357;
    static final double NOTE_LINE2_CAP_Y = 1379;
    static final double NOTE_ICON = sq(30);
    static final double NOTE_GUTTER = px(80 - 35);

    static final double FOOTER_RULE_Y = 1412;
    static final double FOOTER_NAME_CAP_Y = 1426;
    static final double FOOTER_ADDR_CAP_Y = 1447;
    static final double FOOTER_SITE_CAP_Y = 1468;
    static final double FOOTER_TILE_W = px(105 - 34);
    static final double FOOTER_MARK_W = sq(47);
    static final double FOOTER_TEXT_PAD_L = px(131 - 105);
    /** Where the identity band's own top edge lands: immediately under its rule. */
    static final double FOOTER_ROW_TOP = py(FOOTER_RULE_Y) + RULE_BOX;

    /** A point of slack, so a block solved to the paper's edge still fits inside it. */
    private static final double FIT_TOLERANCE = 1.0;

    static final double FOOTER_TILE_H =
            PAGE_H - MARGIN_B_FIRST - FOOTER_ROW_TOP - FIT_TOLERANCE;

    // ------------------------------------------------------------------
    // Block heights, for the rules that are placed under whole blocks
    // ------------------------------------------------------------------

    /** The masthead is as tall as the taller of the lockup and the title. */
    static final double MASTHEAD_H = Math.max(
            LOGO_MARGIN_T + LOGO_H, TITLE_MARGIN_T + LINE_BOX * TITLE_SIZE);

    /**
     * The identity block's laid-out height, measured from its own top.
     *
     * <p>The supplier column ends with its tax line and the divider ends lower,
     * and the two are within half a point of each other — so both are computed
     * and the taller wins, rather than one being assumed to outlast the other.</p>
     */
    static final double IDENTITY_H = Math.max(
            py(391 - 144),
            py(377 - 144) - TOP_BEARING_REGULAR * BODY_SIZE + LINE_BOX * BODY_SIZE);

    /** The closing note's own height, from its top to the foot of its second line. */
    static final double CLOSING_NOTE_H =
            py(NOTE_LINE2_CAP_Y - NOTE_TOP_Y)
                    - TOP_BEARING_REGULAR * CLOSING_REG_SIZE
                    + LINE_BOX * CLOSING_REG_SIZE;
}
