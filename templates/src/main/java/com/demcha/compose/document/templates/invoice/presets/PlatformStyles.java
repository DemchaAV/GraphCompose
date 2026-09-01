package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * Every measurement, colour and type size of the Platform invoice.
 *
 * <h2>Two scales, on purpose</h2>
 *
 * <p>The design is 1.50 aspect and A4 is 1.414, so one conversion constant
 * cannot serve both axes. {@link #px(double)} is horizontal and every column
 * width, x-offset and font size goes through it, because that is the axis where
 * advance widths have to fit. {@link #py(double)} is vertical, 6.3% tighter, and
 * every block height, line pitch and gap goes through that. {@link #sq(double)}
 * is their mean, for the two square things on the sheet — marks and discs —
 * which cannot match both axes and are therefore about 3% out in each rather
 * than 6% out in one.</p>
 *
 * <p>Writing a vertical measurement as {@code px(n)} would stretch the sheet by
 * 6.3% without anything failing. The split exists to make that visible.</p>
 */
final class PlatformStyles {

    private PlatformStyles() {
    }

    // ------------------------------------------------------------------
    // The page and its two scales
    // ------------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    private static final double PAGE_W = PAGE.width();
    private static final double PAGE_H = PAGE.height();

    /** The design image's own pixel size — the units every measurement below is in. */
    private static final double REF_W = 1024.0;
    private static final double REF_H = 1536.0;

    /** Points per design pixel, horizontal. */
    private static final double PX = PAGE_W / REF_W;

    /**
     * Points per design pixel, vertical.
     *
     * <p>The design's full height maps onto the page's full height, which is what
     * a comparison against it does: the image is resampled to the render's width
     * <em>and</em> height, so a design pixel at y is compared against the page at
     * y / 1536 of its height and nothing else. Anchoring the scale on anything
     * narrower — the last ink, say — tilts every measurement below the masthead
     * by the difference, growing to a visible offset by the foot of the page.</p>
     */
    private static final double PY = PAGE_H / REF_H;

    /** Points per design pixel for anything square. */
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

    // ------------------------------------------------------------------
    // Type, and the arithmetic the design's pitches need
    // ------------------------------------------------------------------

    /**
     * The sheet is set in Barlow throughout. The templates artifact carries no
     * fonts — register the family on the session, or the engine substitutes and
     * the cap arithmetic below no longer describes the type sitting in it.
     */
    static final FontName FACE = FontName.BARLOW;

    /** Barlow's capHeight / unitsPerEm, identical in Regular and Bold. */
    private static final double FACE_CAP_RATIO = 0.700;

    /** Barlow's ascender, as a share of the em. */
    private static final double FACE_ASCENT = 1.000;

    /** The line box the engine lays a paragraph out in, as a multiple of the size. */
    static final double LINE_BOX = 1.200;

    /** What a line box carries above its own cap. */
    static final double CAP_INSET = FACE_ASCENT - FACE_CAP_RATIO;

    /** Barlow's space advance, as a share of the em. */
    private static final double FACE_SPACE_ADVANCE = 0.200;

    /** A size whose caps measure {@code capPx} on the design. */
    static double sizeForCap(double capPx) {
        return px(capPx) / FACE_CAP_RATIO;
    }

    /**
     * The gap to put between two stacked text boxes so their cap tops land
     * {@code pitchPx} design pixels apart.
     *
     * <p>Line spacing and section spacing are additive terms in <em>points</em>
     * between boxes rather than multiples of the type size, and a box reaches
     * the full ascent above its baseline where the cap only reaches
     * {@code FACE_CAP_RATIO}. So the gap is the measured pitch minus what the
     * previous box hangs below its own cap and what the next box carries above
     * its own.</p>
     *
     * <p>The result is clamped at zero, and on this sheet that clamp fires. Type
     * is sized on the horizontal scale because advance widths have to fit the
     * columns, while pitches are measured on the vertical one, which is 6.3%
     * tighter — so a line box can be marginally taller than the pitch the design
     * sets it at. The closing note is the case: 22 design pixels of pitch against
     * a line box that occupies 22.1. Zero spacing lands it a tenth of a pixel
     * out, which is the closest the compressed page can come, and a negative gap
     * is not something the engine accepts.</p>
     *
     * @param pitchPx  the cap-to-cap distance the design sets
     * @param prevSize the size of the box above
     * @param nextSize the size of the box below
     * @return the gap between the two boxes
     */
    static double capPitch(double pitchPx, double prevSize, double nextSize) {
        return Math.max(0, py(pitchPx) - (LINE_BOX - CAP_INSET) * prevSize - CAP_INSET * nextSize);
    }

    /** The same, for two lines of one size. */
    static double capPitch(double pitchPx, double size) {
        return capPitch(pitchPx, size, size);
    }

    static final double TITLE_SIZE = sizeForCap(39);
    static final double SUPPLIER_NAME_SIZE = sizeForCap(16);
    static final double PARTY_NAME_SIZE = sizeForCap(13);
    static final double BODY_SIZE = sizeForCap(12);
    static final double SMALL_SIZE = sizeForCap(11);
    static final double TINY_SIZE = sizeForCap(10);
    static final double LABEL_SIZE = sizeForCap(12);
    static final double TABLE_HEAD_SIZE = sizeForCap(10);
    static final double TOTAL_LABEL_SIZE = sizeForCap(15);
    static final double TOTAL_VALUE_SIZE = sizeForCap(20);
    static final double DUE_VALUE_SIZE = sizeForCap(15);
    static final double NOTE_SIZE = sizeForCap(11);
    static final double FOOTER_SIZE = sizeForCap(8);

    static DocumentTextStyle style(double size, DocumentColor color) {
        return type(size, color, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle bold(double size, DocumentColor color) {
        return type(size, color, DocumentTextDecoration.BOLD);
    }

    private static DocumentTextStyle type(double size, DocumentColor color,
                                          DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(FACE)
                .size(size)
                .color(color)
                .decoration(decoration)
                .build();
    }

    // ------------------------------------------------------------------
    // The page box
    // ------------------------------------------------------------------

    /**
     * Points reserved at the foot of every page for the enumeration, and the
     * bottom margin that keeps the body out of it.
     *
     * <p>The band's height is also what positions the number inside it: the
     * engine seats the text's line box at the band's top and the band's foot is
     * the paper's, so the number rises point for point with this constant. At
     * nine points the descender came within a point of the paper edge, which
     * reads as a printing accident rather than a margin.</p>
     *
     * <p>The margin is the band, and not the band plus a chosen number.
     * Reserving less is what lets a continuation page's last row run into the
     * enumeration — a defect page one is structurally unable to show, because its
     * content ends well above the fold. Reserving more is a second gap doing the
     * job the band's own top leading already does.</p>
     */
    static final double FOOTER_BAND_H = 12.0;

    static final double MARGIN_TOP = py(45) - CAP_INSET * TITLE_SIZE;
    static final double MARGIN_X = px(44);
    static final double MARGIN_BOTTOM = FOOTER_BAND_H;

    static final DocumentInsets PAGE_MARGIN =
            new DocumentInsets(MARGIN_TOP, MARGIN_X, MARGIN_BOTTOM, MARGIN_X);

    static final double CONTENT_W = PAGE_W - 2 * MARGIN_X;

    /** Flow y of a design y, for the few places a gap is measured from the page. */
    static double flowY(double designY) {
        return py(designY) - MARGIN_TOP;
    }

    // ------------------------------------------------------------------
    // Columns and blocks
    // ------------------------------------------------------------------

    /** Every rule on the sheet. */
    static final double RULE_THICK = py(1.2);

    static final double PANEL_RADIUS = px(8);
    static final double CARD_RADIUS = px(8);

    static final double LOCKUP_W = px(292);
    static final double FOOTER_LOCKUP_W = px(180);

    static final double LEFT_COLUMN_W = px(463);
    static final double DIVIDER_W = px(1);
    static final double META_PAD_L = px(65);
    static final double META_LABEL_W = px(175);

    static final double BILL_TO_GUTTER = px(56);
    static final double BILL_TO_PAD_L = px(5);
    static final double SHIP_TO_GUTTER = px(53);
    static final double SHIP_TO_PAD_L = px(32);

    static final double DISC_D = sq(33);
    static final double DISC_ICON = sq(18);
    static final double PIN_W = sq(29);
    static final double CONTACT_ICON = sq(18);
    static final double CONTACT_GAP_PX = 20.0;

    // ------------------------------------------------------------------
    // The usage table
    // ------------------------------------------------------------------

    /** The pitch of the design's five body rows, measured across all of them. */
    private static final double TABLE_ROW_PITCH_Y = (1034 - 696) / 5.0;

    /** A separator asked for less than a point is laid out as one. */
    static final double TABLE_RULE_LAID_OUT = 1.0;

    static final double TABLE_HEADER_H = py(41);
    static final double TABLE_ROW_H = py(TABLE_ROW_PITCH_Y) - TABLE_RULE_LAID_OUT;

    static final double TABLE_PAD_FIRST = px(29);
    static final double TABLE_PAD_X = px(19);
    static final double TABLE_PAD_RIGHT = px(8);

    /**
     * A centred column carries far less side padding than a left-aligned one:
     * nineteen design pixels on both sides does not leave room for the widest
     * heading inside its measured column.
     */
    static final double TABLE_PAD_CENTRED = px(6);

    static final double TABLE_PAD_ICON = px(18);
    static final double TABLE_ICON = sq(36);
    static final double TABLE_ICON_GUTTER = px(36);
    static final double TABLE_ICON_GAP = px(22);

    // ------------------------------------------------------------------
    // Settlement
    // ------------------------------------------------------------------

    static final double PANEL_W = px(415);
    static final double PANEL_PAD_X = px(24);
    static final double PANEL_HEAD_GUTTER = px(50);
    static final double PANEL_NOTE_GUTTER = px(39);
    static final double PANEL_LABEL_W = px(180);
    static final double PANEL_ICON = sq(24);
    static final double PANEL_NOTE_ICON = sq(23);
    static final double PANEL_PAD_ROWS_L = px(25);

    static final double SUMMARY_GAP_W = px(81);
    static final double RIGHT_COLUMN_W = px(405);
    static final double TOTALS_PAD_L = px(10);
    static final double TOTALS_PAD_R = px(13);
    static final double TOTALS_RULE_W = px(395);

    static final double CARD_DISC_D = sq(50);
    static final double CARD_DISC_ICON = sq(26);
    static final double CARD_PAD_L = px(23);
    static final double CARD_GUTTER = px(69);

    static final double NOTE_ICON = sq(26);
    static final double NOTE_GUTTER = px(45);

    // The design's measured y positions inside the settlement band.
    static final double PANEL_TOP_Y = 1046;
    static final double PANEL_ICON_Y = 1064;
    static final double PANEL_ROW1_CAP_Y = 1103;
    static final double PANEL_ROW_PITCH_Y = 21.7;
    static final double PANEL_LAST_ROW_CAP_Y = 1254;
    static final double PANEL_RULE_Y = 1286;
    static final double PANEL_NOTE_CAP_Y = 1302;
    static final double PANEL_NOTE_LAST_CAP_Y = 1324;
    static final double PANEL_BOTTOM_Y = 1350;

    /** Design y of the last ink on the sheet: the foot of the identity band. */
    static final double REF_CONTENT_BOTTOM = 1505.0;

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    static final DocumentColor ACCENT = DocumentColor.rgb(26, 115, 232);
    static final DocumentColor INK = DocumentColor.rgb(32, 33, 36);
    static final DocumentColor BODY = DocumentColor.rgb(60, 64, 67);
    static final DocumentColor MUTED = DocumentColor.rgb(95, 99, 104);
    static final DocumentColor BAND = DocumentColor.rgb(239, 244, 254);
    static final DocumentColor CARD = DocumentColor.rgb(244, 248, 254);
    static final DocumentColor PANEL_BORDER = DocumentColor.rgb(226, 236, 253);
    static final DocumentColor RULE = DocumentColor.rgb(224, 224, 225);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(229, 229, 230);
    static final DocumentColor WHITE = DocumentColor.WHITE;

    /**
     * A run of spaces that advances {@code widthPx} design pixels at
     * {@code size}, for the two places the design sets a measured gap inside one
     * paragraph rather than between two.
     *
     * @param widthPx the gap the design measures
     * @param size    the type the gap is set in
     * @return the spaces to write
     */
    static String spaces(double widthPx, double size) {
        int count = (int) Math.round(px(widthPx) / (FACE_SPACE_ADVANCE * size));
        return " ".repeat(Math.max(1, count));
    }
}
