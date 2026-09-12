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
 * Every measurement, colour and type size of the Metered invoice.
 *
 * <p>Geometry is relational. Every width is a fraction of {@link #CONTENT_W} and
 * every length taken off the design is written as {@code px(n)} — the design's
 * own pixel — so it still reads as the thing that was measured while scaling
 * with the page. Two lengths are independent of the grid: the icon tile and the
 * hairline weight.</p>
 */
final class MeteredStyles {

    private MeteredStyles() {
    }

    // ------------------------------------------------------------------
    // The page and its grid
    // ------------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    private static final double PAGE_W = PAGE.width();
    private static final double PAGE_H = PAGE.height();

    /** The design image's pixel width — the scale every measurement below is in. */
    private static final double DESIGN_PX_WIDTH = 1055.0;

    /** Points per design pixel. The one conversion the whole preset goes through. */
    private static final double PX = PAGE_W / DESIGN_PX_WIDTH;

    static double px(double designPixels) {
        return designPixels * PX;
    }

    /**
     * The side margins are not equal: every full-width element runs from x=35.5
     * to x=1011.5 of a 1055 px sheet, a 35.5 px left margin against a 43.5 px
     * right one. Built symmetric at the mean it costs 4 px of horizontal error
     * on every glyph in the document to save 8 px on one edge, so the asymmetry
     * is kept as measured.
     */
    static final double MARGIN_LEFT = px(35.5);
    static final double MARGIN_RIGHT = px(43.5);
    static final double TOP_MARGIN = px(44);

    /** The dark band's share of the page height; passed straight to the background. */
    static final double FOOTER_BAND_RATIO = 72.0 / 1491.0;
    static final double FOOTER_BAND_H = PAGE_H * FOOTER_BAND_RATIO;

    /** The band plus clearance, so no continuation page runs its last row into the chrome. */
    static final double BOTTOM_MARGIN = FOOTER_BAND_H + px(8);

    /** Every column width on the page is a fraction of this. */
    static final double CONTENT_W = PAGE_W - MARGIN_LEFT - MARGIN_RIGHT;

    static final DocumentInsets PAGE_MARGIN =
            new DocumentInsets(TOP_MARGIN, MARGIN_RIGHT, BOTTOM_MARGIN, MARGIN_LEFT);

    /**
     * Every rule, border and table stroke on the page.
     *
     * <p>The design's hairlines are one pixel of a 1055 px image, which is
     * 0.6 pt — and a 0.6 pt stroke rasterises to less than a pixel at preview
     * resolution, so half the table's row rules come out at partial opacity and
     * the two panel outlines do not register at all. They are a pixel of ink,
     * not a pixel of geometry; this is the weight that reproduces them.</p>
     */
    static final double HAIRLINE = px(1.7);

    // ------------------------------------------------------------------
    // Seams between the stacked bands
    // ------------------------------------------------------------------

    /** The base seam. The ones that differ carry the difference as their own margin. */
    static final double BAND_GAP = px(22);

    static final DocumentInsets SEAM_IDENTITY_TO_RULE = seam(14);
    static final DocumentInsets SEAM_RULE_TO_PARTIES = seam(9);
    static final DocumentInsets SEAM_PARTIES_TO_TABLE = seam(12);
    static final DocumentInsets SEAM_TABLE_TO_CLOSING = seam(0);
    static final DocumentInsets SEAM_CLOSING_TO_NOTE = seam(3);

    /** The two columns of the identity row start at different heights. */
    static final DocumentInsets SUPPLIER_BLOCK_INSET = seam(6);
    static final DocumentInsets META_BLOCK_INSET = seam(11);

    private static DocumentInsets seam(double extraPixels) {
        return new DocumentInsets(px(extraPixels), 0, 0, 0);
    }

    // ------------------------------------------------------------------
    // Column splits
    // ------------------------------------------------------------------

    /** Identity row: supplier | hairline | invoice metadata. */
    static final double IDENTITY_SPLIT = 0.431;
    static final double SEAM_CLEARANCE = px(20);
    static final double META_COLUMN_INSET = px(141);

    /** Parties row: bill-to | hairline | ship-to. */
    static final double PARTIES_SPLIT = 0.494;
    static final double SHIP_COLUMN_INSET = px(74);

    /** Closing row: payment card | gutter | totals over the due-by card. */
    static final double CLOSING_LEFT_W = CONTENT_W * (431.0 / 976.0);
    static final double CLOSING_RIGHT_W = CONTENT_W * (422.0 / 976.0);
    static final double CLOSING_GUTTER = px(113);

    /** The totals rows sit inside their column, not flush with the panel below them. */
    static final double SUMMARY_INSET_L = px(6);
    static final double SUMMARY_INSET_R = px(7.5);
    static final double SUMMARY_W = CLOSING_RIGHT_W - SUMMARY_INSET_L - SUMMARY_INSET_R;
    static final double TOTALS_RULE_GAP = px(3.5);

    /** The five table columns, solved from the header-label centres. */
    static final double COL_DESCRIPTION = CONTENT_W * 0.2945;
    static final double COL_SERVICE = CONTENT_W * 0.2352;
    static final double COL_QUANTITY = CONTENT_W * 0.1288;
    static final double COL_UNIT_PRICE = CONTENT_W * 0.1759;
    static final double COL_AMOUNT = CONTENT_W * 0.1656;

    static final double ICON_TILE = px(44);
    static final double ICON_TILE_RADIUS = px(10);
    static final double LINE_ICON = px(25);
    static final double TABLE_PAD_L = px(17.5);
    static final double TILE_GAP = px(24);

    /** Splits the description column into the tile's lane and the text's lane. */
    static final double DESC_TILE_COL = TABLE_PAD_L + ICON_TILE + TILE_GAP;
    static final double DESC_TEXT_COL = COL_DESCRIPTION - DESC_TILE_COL;

    static final double ROW_PAD_Y = px(14.3);
    static final double HEADER_PAD_Y = px(14.5);
    static final double LINE_TEXT_GAP = px(1);

    // ------------------------------------------------------------------
    // Cards and marks
    // ------------------------------------------------------------------

    static final double PANEL_RADIUS = px(10);
    static final double PANEL_PADDING = px(21);
    static final double DUE_PADDING = px(20);

    static final double SECTION_ICON = px(24);
    static final double SECTION_ICON_GAP = px(15);
    static final double PANEL_ICON = px(26);
    static final double PANEL_ICON_GAP = px(5);
    static final double DUE_ICON = px(36);
    static final double DUE_ICON_GAP = px(22);
    static final double NOTE_ICON = px(24);
    static final double NOTE_ICON_GAP = px(19);

    /** Bill-to's mark and the note's mark hang inside the content edge, not on it. */
    static final double PARTY_INSET = px(7.5);
    static final double NOTE_INSET = px(5.5);

    /** The masthead lockup's measured width. */
    static final double LOGO_W = px(222);

    /** Label columns: four pair lists, four widths, each stated once. */
    static final double SUPPLIER_LABEL_W = px(85);
    static final double META_LABEL_W = px(153);
    static final double PANEL_LABEL_W = px(172);
    static final double PARTY_LABEL_W = px(71);

    // Line pitches, as the gap a section puts between its children.
    static final double SUPPLIER_LINE_GAP = px(4.5);
    static final double SUPPLIER_NAME_GAP = px(6);
    static final double SUPPLIER_GROUP_GAP = px(24);
    static final double SUPPLIER_PAIR_GAP = px(6.2);
    static final double META_PAIR_GAP = px(16);
    static final double PARTY_HEAD_GAP = px(9);
    static final double PARTY_LINE_GAP = px(5);
    static final double PARTY_EXTRA_GAP = px(14);
    static final double TOTALS_GAP = px(15);
    static final double TOTALS_RULE_T = px(2);
    static final double SUMMARY_GAP = px(29);
    static final double PANEL_HEAD_GAP = px(4);
    static final double PANEL_ROW_GAP = px(4.7);
    static final double DUE_TEXT_GAP = px(0);
    static final double NOTE_RULE_GAP = px(22.5);
    static final double NOTE_LINE_GAP = px(5);
    static final double TITLE_RULE_GAP = px(17);
    static final double TITLE_RULE_W = px(71);
    static final double TITLE_RULE_T = px(4);

    // ------------------------------------------------------------------
    // Page chrome
    // ------------------------------------------------------------------

    /**
     * Repeating chrome is positioned by its distance from the paper's bottom
     * edge, so the two legal lines are two footer entries at two heights and the
     * enumeration is a third on the band's own centre line.
     */
    static final double FOOTER_LINE_ONE_H = 32.7;
    static final double FOOTER_LINE_TWO_H = 21.5;
    static final double FOOTER_PAGE_H = 27;
    static final double FOOTER_SIZE = 7;

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    static final DocumentColor INK = DocumentColor.rgb(11, 18, 28);
    static final DocumentColor ACCENT = DocumentColor.rgb(251, 122, 0);
    static final DocumentColor ACCENT_BRIGHT = DocumentColor.rgb(255, 140, 5);
    static final DocumentColor BAND_DARK = DocumentColor.rgb(17, 26, 41);
    static final DocumentColor FOOTER_DARK = DocumentColor.rgb(7, 18, 27);
    static final DocumentColor INVERSE = DocumentColor.rgb(255, 255, 255);
    static final DocumentColor PANEL_FILL = DocumentColor.rgb(248, 248, 248);
    static final DocumentColor PANEL_BORDER = DocumentColor.rgb(235, 235, 235);
    static final DocumentColor RULE = DocumentColor.rgb(222, 223, 226);
    static final DocumentColor TABLE_RULE = DocumentColor.rgb(224, 225, 227);
    static final DocumentColor TABLE_BORDER = DocumentColor.rgb(237, 237, 237);
    static final DocumentColor WHITE = DocumentColor.WHITE;

    /**
     * The sheet is set in Lato throughout, chosen by measurement against the
     * design's own grotesque. The templates artifact carries no fonts —
     * register the family on the session, or the engine substitutes and the
     * measured geometry no longer matches the type sitting in it.
     */
    static final FontName FACE = FontName.LATO;

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    static final DocumentTextStyle TITLE = bold(29, INK);
    static final DocumentTextStyle SUPPLIER_NAME = bold(10.5, INK);
    static final DocumentTextStyle BODY = plain(9.1, INK);
    static final DocumentTextStyle META_LABEL = plain(8.6, INK);
    static final DocumentTextStyle META_VALUE = plain(9.1, INK);
    static final DocumentTextStyle META_ACCENT = bold(9.7, ACCENT);
    static final DocumentTextStyle SECTION_LABEL = bold(7.9, ACCENT);
    static final DocumentTextStyle PARTY_NAME = bold(10.5, INK);
    static final DocumentTextStyle TABLE_HEAD = bold(7.5, INVERSE);
    static final DocumentTextStyle LINE_TITLE = bold(10, INK);
    static final DocumentTextStyle LINE_SUB = plain(8.6, INK);
    static final DocumentTextStyle CELL = plain(8.2, INK);
    static final DocumentTextStyle TOTALS_LABEL = plain(8.6, INK);
    static final DocumentTextStyle TOTALS_VALUE = plain(8.6, INK);
    static final DocumentTextStyle TOTAL_LABEL = bold(11, INK);
    static final DocumentTextStyle TOTAL_VALUE = bold(14.3, ACCENT);
    static final DocumentTextStyle PANEL_TITLE = bold(8.75, ACCENT);
    static final DocumentTextStyle PANEL_TEXT = plain(8.1, INK);
    static final DocumentTextStyle DUE_LABEL = bold(8, INK);
    static final DocumentTextStyle DUE_VALUE = bold(11.4, INK);
    static final DocumentTextStyle NOTE_LEAD = bold(9.1, INK);
    static final DocumentTextStyle NOTE_BODY = plain(8.1, INK);

    static DocumentTextStyle plain(double size, DocumentColor color) {
        return style(size, color, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle bold(double size, DocumentColor color) {
        return style(size, color, DocumentTextDecoration.BOLD);
    }

    private static DocumentTextStyle style(double size, DocumentColor color,
                                           DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(FACE)
                .size(size)
                .color(color)
                .decoration(decoration)
                .build();
    }

    // ------------------------------------------------------------------
    // Table cells
    // ------------------------------------------------------------------

    /**
     * A table's rules are drawn per cell and cover all four of that cell's
     * edges. A five-column stroked table therefore draws four internal
     * verticals the design does not have, so the table is one column wide and
     * each cell carries a composed row of six fixed sub-columns. That leaves
     * exactly the topology the design has: an outer box and a rule between
     * rows.
     */
    static final DocumentTableStyle HEADER_CELL = DocumentTableStyle.builder()
            .padding(new DocumentInsets(HEADER_PAD_Y, 0, HEADER_PAD_Y, 0))
            .fillColor(BAND_DARK)
            .stroke(DocumentStroke.of(TABLE_BORDER, HAIRLINE))
            .build();

    static final DocumentTableStyle BODY_CELL = DocumentTableStyle.builder()
            .padding(new DocumentInsets(ROW_PAD_Y, 0, ROW_PAD_Y, 0))
            .stroke(DocumentStroke.of(TABLE_RULE, HAIRLINE))
            .build();
}
