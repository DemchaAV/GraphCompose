package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

/**
 * Every measurement, colour and type size of the Obsidian invoice.
 *
 * <h2>Sizes are solved from ink, not from cap height</h2>
 *
 * <p>The other presets in this family size their type from a measured cap
 * height. This design's own face is not bundled, so a cap-height match would
 * still set every string to the wrong width; here each size is solved from the
 * measured <em>ink width</em> of the string it sets, and the substitute face's
 * own width ratio is applied once. Every size below is named by the string it
 * was solved from.</p>
 *
 * <h2>Positions are cap tops, not box edges</h2>
 *
 * <p>The design can be measured on where ink starts, and a text box begins above
 * its cap band by the face's top bearing. {@link #capTop} and {@link #capGap}
 * convert a measured cap position into the margin that puts it there, so the
 * numbers in the preset stay the numbers that were measured.</p>
 */
final class ObsidianStyles {

    private ObsidianStyles() {
    }

    // ------------------------------------------------------------------
    // The page
    // ------------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    private static final double PAGE_W = PAGE.width();
    private static final double DESIGN_PX_WIDTH = 1055.0;
    private static final double PX = PAGE_W / DESIGN_PX_WIDTH;

    static double px(double designPixels) {
        return designPixels * PX;
    }

    static final double MARGIN_T = px(46);
    static final double MARGIN_L = px(46);
    static final double MARGIN_R = px(48);
    static final double MARGIN_B_FIRST = px(24);
    static final double MARGIN_B_LATER = px(58);
    static final double PAGE_NUMBER_BAND = px(20);

    static final DocumentInsets PAGE_MARGIN =
            new DocumentInsets(MARGIN_T, MARGIN_R, MARGIN_B_FIRST, MARGIN_L);

    /**
     * A continuation page reserves more at the foot than the first, because the
     * first page's last block is the closing band and a continuation page's is a
     * table row, which would otherwise sit on the enumeration.
     */
    static final DocumentInsets CONTINUATION_MARGIN =
            new DocumentInsets(MARGIN_T, MARGIN_R, MARGIN_B_LATER, MARGIN_L);

    static final double CONTENT_W = PAGE_W - MARGIN_L - MARGIN_R;
    static final double CARD_GUTTER = px(38);
    static final double HALF = 0.5;

    // ------------------------------------------------------------------
    // Colour — a dark sheet
    // ------------------------------------------------------------------

    static final DocumentColor PAGE_BG = DocumentColor.rgb(22, 23, 26);
    static final DocumentColor SURFACE = DocumentColor.rgb(27, 28, 32);
    static final DocumentColor SURFACE_BORDER = DocumentColor.rgb(38, 39, 43);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(42, 43, 48);
    static final DocumentColor HAIRLINE_STRONG = DocumentColor.rgb(88, 94, 102);
    static final DocumentColor INK = DocumentColor.WHITE;
    static final DocumentColor MUTED = DocumentColor.rgb(174, 178, 185);
    static final DocumentColor ACCENT = DocumentColor.rgb(127, 140, 255);
    static final DocumentColor DISC_SUPPLIER = DocumentColor.rgb(114, 80, 226);
    static final DocumentColor DISC_CLIENT = DocumentColor.rgb(79, 105, 216);
    static final DocumentColor DISC_CLOSING = DocumentColor.rgb(110, 120, 250);

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    /**
     * The design's own face is not bundled. Gothic A1 is the family whose widths
     * the sizes below are solved against; the templates artifact carries no fonts
     * — register it on the session, or the engine substitutes and every size is
     * solved for type that is not there.
     */
    static final FontName FACE = FontName.GOTHIC_A1;

    /** The substitute's ink width against the design's, measured across the sheet. */
    private static final double FACE_WIDTH_SPLIT = 0.98;

    /** Gothic A1's line box, ascender, descender and cap heights, read off the TTFs. */
    static final double LINE_BOX = 1.0;
    private static final double FACE_ASCENT = 0.7979;
    private static final double FACE_DESCENT = 0.2021;
    private static final double CAP_REGULAR = 0.7314;
    private static final double CAP_BOLD = 0.7725;
    private static final double TOP_BEARING_REGULAR = FACE_ASCENT - CAP_REGULAR;
    private static final double TOP_BEARING_BOLD = FACE_ASCENT - CAP_BOLD;

    /** A size whose ink reproduces a string measured at {@code solvedDesignPx}. */
    static double sizeForInk(double solvedDesignPx) {
        return px(solvedDesignPx * FACE_WIDTH_SPLIT);
    }

    static double topBearing(double size, boolean bold) {
        return (bold ? TOP_BEARING_BOLD : TOP_BEARING_REGULAR) * size;
    }

    static double bottomBearing(double size) {
        return FACE_DESCENT * size;
    }

    /** The margin that puts a cap top at a measured design y. */
    static DocumentInsets capTop(double capPx, double size, boolean bold) {
        return capTop(capPx, size, bold, 0);
    }

    /** The same, under a box of a stated height. */
    static DocumentInsets capTop(double capPx, double size, boolean bold,
                                 double precedingBoxHeight) {
        return new DocumentInsets(
                px(capPx) - topBearing(size, bold) - precedingBoxHeight, 0, 0, 0);
    }

    /** The gap that puts the next cap top {@code deltaPx} below the previous one. */
    static double capGap(double deltaPx, double sizeAbove, boolean boldAbove,
                         double sizeBelow, boolean boldBelow) {
        return px(deltaPx) - LINE_BOX * sizeAbove + topBearing(sizeAbove, boldAbove)
                - topBearing(sizeBelow, boldBelow);
    }

    /** The gap between two blocks, measured between their ink rather than their boxes. */
    static double blockGap(double inkGapPx, double sizeAbove, double sizeBelow) {
        return px(inkGapPx)
                - (sizeAbove > 0 ? bottomBearing(sizeAbove) : 0)
                - (sizeBelow > 0 ? topBearing(sizeBelow, false) : 0);
    }

    // Every size is named by the string it was solved from.
    static final double WORDMARK_SIZE = sizeForInk(47.5);
    static final double TITLE_SIZE = sizeForInk(74.7);
    static final double META_LABEL_SIZE = sizeForInk(14.7);
    static final double META_NUMBER_SIZE = px(19.9);
    static final double META_VALUE_SIZE = sizeForInk(16.6);
    static final double PARTY_LABEL_SIZE = sizeForInk(16.1);
    static final double PARTY_NAME_SIZE = sizeForInk(17.4);
    static final double BODY_SIZE = sizeForInk(17.2);
    static final double TABLE_HEAD_SIZE = sizeForInk(16.0);
    static final double ITEM_TITLE_SIZE = sizeForInk(16.7);
    static final double ITEM_SUB_SIZE = sizeForInk(15.7);
    static final double FIGURE_SIZE = sizeForInk(16.9);
    static final double TOTALS_LABEL_SIZE = sizeForInk(16.0);

    /** The totals figures are measurably wider than the table's. */
    static final double TOTALS_VALUE_SIZE = sizeForInk(18.3);

    static final double TOTAL_DUE_LABEL_SIZE = sizeForInk(17.5);
    static final double TOTAL_DUE_VALUE_SIZE = sizeForInk(32.2);
    static final double CARD_HEAD_SIZE = sizeForInk(15.6);
    static final double CARD_BODY_SIZE = sizeForInk(15.2);
    static final double PAY_LABEL_SIZE = sizeForInk(14.8);
    static final double PAY_VALUE_SIZE = sizeForInk(15.5);
    static final double CLOSING_SIZE = sizeForInk(15.2);
    static final double CLOSING_RIGHT_SIZE = sizeForInk(16.1);
    static final double PAGE_NUMBER_SIZE = px(12);

    static DocumentTextStyle style(double size, DocumentColor color,
                                   DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(FACE)
                .size(size)
                .color(color)
                .decoration(decoration)
                .build();
    }

    static DocumentTextStyle plain(double size, DocumentColor color) {
        return style(size, color, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle bold(double size, DocumentColor color) {
        return style(size, color, DocumentTextDecoration.BOLD);
    }

    // ------------------------------------------------------------------
    // Rules, cards and discs
    // ------------------------------------------------------------------

    static final double RULE_THIN = px(1.2);

    /** A rule asked for less than a point is laid out as one. */
    static final double RULE_BOX = 1.0;

    static final double CARD_RADIUS = px(12);
    static final double CARD_PAD_H = px(24);
    static final double CARD_BORDER = px(1.1);
    static final DocumentStroke CARD_STROKE = DocumentStroke.of(SURFACE_BORDER, CARD_BORDER);

    static final double DISC_D = px(63);
    static final double DISC_GLYPH = px(29);
    static final double PARTY_TEXT_INDENT = px(111);
    static final double META_COL_W = px(385);

    static final double TOTALS_CARD_TOP = 996;
    static final double TOTALS_CARD_BOTTOM = 1142;
    static final double[] TOTALS_ROW_AT = {1014, 1048};
    static final double TOTALS_RULE_AT = 1078.5;
    static final double TOTALS_DUE_AT = 1097;

    /** The totals card is padded to its text, and the rule reaches back out past it. */
    static final double TOTALS_RULE_OVERHANG = px(7);
    static final double TOTALS_PAD_L = px(17) + TOTALS_RULE_OVERHANG;
    static final double TOTALS_PAD_R = px(23) + TOTALS_RULE_OVERHANG;

    static final double CARD_ICON_COL = px(117 - 46);
    static final double CARD_ICON = px(25);

    // ------------------------------------------------------------------
    // The line-item table
    // ------------------------------------------------------------------

    static final double TABLE_INSET = px(22);
    static final double TABLE_W = CONTENT_W - 2 * TABLE_INSET;
    static final double[] COLUMN_SHARES = {0.0578, 0.4177, 0.0763, 0.1527, 0.1527, 0.1428};
    static final double CELL_PAD_L = px(2);
    static final double CELL_PAD_R = px(10);

    /**
     * The table's cells fill and stroke in the card's own surface colour, so the
     * table reads as part of the card rather than as a grid drawn on it. The rules
     * the design does show are drawn inside each row's own content.
     */
    static final DocumentTableStyle CELL_STYLE = DocumentTableStyle.builder()
            .fillColor(SURFACE)
            .stroke(DocumentStroke.of(SURFACE, RULE_THIN))
            .padding(DocumentInsets.zero())
            .build();
}
