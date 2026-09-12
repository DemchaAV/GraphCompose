package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.List;

/**
 * Every measurement, colour and type size of the Subscription invoice.
 *
 * <p>Geometry is relational: every dimension comes from three base numbers —
 * the page, {@link #px(double)} (the design's own pixel scale in points), and
 * {@link #CONTENT_W}. A measurement taken off the design is written as
 * {@code px(n)} so it stays readable as the thing that was measured while still
 * rescaling with the page, and every type size is {@link #sizeForCap(double)} so
 * it stays readable as the cap height it reproduces.</p>
 */
final class SubscriptionStyles {

    private SubscriptionStyles() {
    }

    // ------------------------------------------------------------------
    // The page
    // ------------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    static final double PAGE_W = PAGE.width();

    /** The design image's pixel width — the scale every measurement below is in. */
    private static final double DESIGN_PX_WIDTH = 1055.0;

    /** Points per design pixel. The one conversion the whole preset goes through. */
    private static final double PX = PAGE_W / DESIGN_PX_WIDTH;

    static double px(double designPixels) {
        return designPixels * PX;
    }

    /** Measured: ink runs x=48..1006 on a 1055 px page, symmetric within a pixel. */
    static final double MARGIN_X = px(48);

    /**
     * The flow's top.
     *
     * <p>The mark's first ink row is at 46, but a paragraph's box starts above
     * its cap band and the title is the tallest thing in the masthead. The flow
     * therefore opens at the title's <em>box</em> top — 14 px above the mark —
     * which is what puts the mark back at 46 once the masthead row aligns its
     * cells against each other.</p>
     */
    static final double MARGIN_T = px(31);

    /**
     * The room the body is held off the paper's bottom edge — roughly the
     * padding the design already leaves below the last line of its closing band,
     * so the band's fill still runs to the edge and only the text moves up.
     */
    static final double FOOTER_RESERVE = px(20);

    /**
     * The chrome band the enumeration is seated in, deliberately taller than the
     * reserve.
     *
     * <p>The two answer different questions. The reserve is how much page the
     * <em>body</em> gives up, and it decides whether the closing band still fits
     * on one page; the band height is where inside that foot the number
     * <em>sits</em>. Seating it in a taller band lifts it off the paper edge
     * without taking another point from the body.</p>
     */
    static final double FOOTER_H = px(24);

    static final double CONTENT_W = PAGE_W - 2 * MARGIN_X;

    /** The horizontal inset every block carries except the two that reach the paper edges. */
    static final DocumentInsets CONTENT_PAD = new DocumentInsets(0, MARGIN_X, 0, MARGIN_X);

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    static final DocumentColor INK = DocumentColor.rgb(0, 4, 14);
    static final DocumentColor TITLE_GREY = DocumentColor.rgb(71, 71, 70);
    static final DocumentColor ACCENT = DocumentColor.rgb(0, 104, 218);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(234, 234, 235);
    static final DocumentColor BAND_FILL = DocumentColor.rgb(245, 246, 247);
    static final DocumentColor CHROME_GREY = DocumentColor.rgb(154, 160, 166);

    private static final DocumentColor CYCLE_RED = DocumentColor.rgb(242, 69, 34);
    private static final DocumentColor CYCLE_GREEN = DocumentColor.rgb(114, 176, 22);
    private static final DocumentColor CYCLE_BLUE = DocumentColor.rgb(20, 118, 212);
    static final DocumentColor CYCLE_AMBER = DocumentColor.rgb(255, 180, 0);

    /**
     * The cycle the metadata bars, the party underlines and the closing strip all
     * draw from. Position, not meaning: the fourth row is amber because it is
     * fourth, and nothing in the document ever names a colour.
     */
    static final List<DocumentColor> CYCLE =
            List.of(CYCLE_RED, CYCLE_GREEN, CYCLE_BLUE, CYCLE_AMBER);

    /** The colour at a position in the cycle. */
    static DocumentColor cycle(int index) {
        return CYCLE.get(Math.floorMod(index, CYCLE.size()));
    }

    /**
     * A cell's stroke draws all four edges and there is no per-edge control, so
     * every cell in this document carries none. The rules the design does show
     * are drawn as accents and as rule rows instead.
     */
    static final DocumentStroke NO_RULE = DocumentStroke.of(DocumentColor.rgba(255, 255, 255, 0), 0);

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    /**
     * The design is set in a humanist grotesque that is not bundled. Lato is the
     * usual substitute and matches the design's mixed-case widths to within 4%,
     * but its uppercase is about 20% wider per unit of cap height, which shows up
     * immediately as headings and their coloured rules overshooting. Measured
     * across twenty strings, Fira Sans Condensed is the bundled family that
     * reproduces both: all-caps within 5% and mixed-case within 1% at the
     * measured cap heights.
     *
     * <p>The templates artifact carries no fonts — register the family on the
     * session, or the engine substitutes and the cap arithmetic below no longer
     * describes the type sitting in it.</p>
     */
    static final FontName FACE = FontName.FIRA_SANS_CONDENSED;

    /** Fira Sans Condensed's cap heights, read off the bundled TTFs, on a 1000 unit em. */
    private static final double CAP_REGULAR = 0.688;
    private static final double CAP_BOLD = 0.693;

    /** Its ascender and descender, same source. They sum to the 1.2 em line box. */
    private static final double ASCENT = 0.935;
    private static final double DESCENT = 0.265;

    static double sizeForCap(double capPx) {
        return px(capPx) / CAP_REGULAR;
    }

    static double sizeForCapBold(double capPx) {
        return px(capPx) / CAP_BOLD;
    }

    /**
     * The margin that puts the next block's first baseline {@code pitchPx} below
     * the previous block's last baseline. Baselines are what the design can be
     * measured on unambiguously; box edges are not.
     */
    static double baselineGap(double pitchPx, double sizeAbove, double sizeBelow) {
        return px(pitchPx) - DESCENT * sizeAbove - ASCENT * sizeBelow;
    }

    /**
     * Line spacing is points added <em>between</em> lines and the line box is
     * 1.2× the type size, so a measured line pitch converts as
     * {@code pitch - 1.2 * size}, never as a ratio.
     */
    static double leading(double pitchPx, double size) {
        return px(pitchPx) - 1.2 * size;
    }

    static final double TITLE_SIZE = sizeForCapBold(52);
    static final double WORDMARK_SIZE = sizeForCap(32);
    static final double SUPPLIER_NAME_SIZE = sizeForCapBold(15);
    static final double BODY_SIZE = sizeForCap(13);
    static final double META_LABEL_SIZE = sizeForCapBold(13.5);
    static final double META_VALUE_SIZE = sizeForCap(13.5);
    static final double HEADING_SIZE = sizeForCapBold(15.5);
    static final double TABLE_HEAD_SIZE = sizeForCapBold(13.5);
    static final double TABLE_BODY_SIZE = sizeForCap(12.8);
    static final double TOTALS_ROW_SIZE = sizeForCapBold(13);
    static final double TOTAL_DUE_LABEL_SIZE = sizeForCapBold(16);
    static final double TOTAL_DUE_VALUE_SIZE = sizeForCapBold(20.5);
    static final double NOTES_BODY_SIZE = sizeForCap(13.5);
    static final double PAY_LABEL_SIZE = sizeForCapBold(13);
    static final double PAY_VALUE_SIZE = sizeForCap(13);
    static final double PAY_NOTE_SIZE = sizeForCap(12.8);
    static final double CLOSE_HEAD_SIZE = sizeForCapBold(12.5);
    static final double CLOSE_BODY_SIZE = sizeForCap(12);
    static final double CHROME_SIZE = 7.0;

    static DocumentTextStyle style(double size, DocumentColor colour,
                                   DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(FACE)
                .size(size)
                .color(colour)
                .decoration(decoration)
                .build();
    }

    static DocumentTextStyle plain(double size, DocumentColor colour) {
        return style(size, colour, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle bold(double size, DocumentColor colour) {
        return style(size, colour, DocumentTextDecoration.BOLD);
    }

    // ------------------------------------------------------------------
    // Masthead
    // ------------------------------------------------------------------

    /** The mark is 54 px square and the wordmark's ink opens 16 px to its right. */
    static final double MARK_W = px(54);
    static final double MARK_GUTTER = px(16);

    /**
     * The mark's top against the masthead row's top. The design lines the title's
     * <em>cap</em> band up with the mark rather than the title's line box, and
     * the box is half as tall again as the cap — so the row is top-aligned and
     * the mark carries the difference instead of the row centring the two.
     */
    static final double MARK_TOP = px(15);

    /** The wordmark's cap sits below the mark's top by this much. */
    static final double WORDMARK_TOP = px(17.5);

    // ------------------------------------------------------------------
    // Identity band
    // ------------------------------------------------------------------

    /** The metadata panel's left edge, as a share of the content box. */
    static final double IDENTITY_LEFT_RATIO = (633.0 - 48.0) / 959.0;

    static final double SUPPLIER_TOP_OFFSET = px(2.7);
    static final double META_BAR_W = px(4);
    static final double META_BAR_H = px(39);
    static final double META_LABEL_INDENT = px(17);
    static final double META_RIGHT_INSET = px(15);
    static final double META_RULE_W = px(368);
    static final double META_RULE_INDENT = px(6);
    static final double META_RULE_THICK = px(2);

    /** Bar bottom 179 → rule top 182, and rule bottom 184 → next bar top 189. */
    static final double META_RULE_GAP = px(3);
    static final double META_ROW_GAP = px(5);

    /**
     * The contact label gutter. The design puts two of the values at x=116 and
     * the third at 132, which is a minimum width plus a gap; one fixed column is
     * the honest reading of it, and it has to clear the longest label.
     */
    static final double CONTACT_LABEL_W = px(70);

    // ------------------------------------------------------------------
    // Parties and headings
    // ------------------------------------------------------------------

    /** The second party column opens at x=539 of the content box. */
    static final double PARTY_SPLIT = (539.0 - 48.0) / 959.0;

    /** Heading rule bottom 449 → first address baseline 487. */
    static final double PARTY_BODY_GAP = px(22.7);

    /** Heading baseline 434 → rule 444..448: ten points of pad, then a five-pixel rule. */
    static final double HEADING_RULE_PAD = px(7);
    static final double HEADING_RULE_THICK = px(4.2);

    static final double PARTY_LINE_PITCH = px(27);

    // ------------------------------------------------------------------
    // The line-item table
    // ------------------------------------------------------------------

    /**
     * The six columns, solved from the ink: the number and description are left
     * aligned, the money headings centred, the amounts right aligned. The widths
     * sum to the content width by construction.
     */
    static final double[] COLUMN_PX = {67, 413, 96, 116, 151, 116};
    static final double COLUMN_TOTAL_PX = 959.0;

    static final double CELL_PAD_L = px(17);
    static final double CELL_PAD_R = px(25);

    /** The data row band is 47.5 px, of which the rule row below it takes one. */
    static final double CELL_PAD_V = px(12.2);
    static final double HEAD_PAD_V = px(11.6);

    /**
     * A centred caption needs room, not alignment: the widest one is 94 px inside
     * a 116 px column, which leaves it seven px a side. A left-aligned caption is
     * positioned by its padding instead, so those cells keep the data cells'
     * measured inset.
     */
    static final double HEAD_PAD_H = px(7);

    static final double TABLE_RULE = px(1);
    static final double TABLE_TOP_RULE = px(5);

    // ------------------------------------------------------------------
    // Totals and notes
    // ------------------------------------------------------------------

    /** The totals block opens on the unit-price boundary, so it is derived, not measured. */
    static final double TOTALS_LEFT_RATIO =
            (COLUMN_PX[0] + COLUMN_PX[1] + COLUMN_PX[2]) / COLUMN_TOTAL_PX;
    static final double TOTALS_LABEL_SHARE = 0.50;
    static final double TOTALS_PAD_H = px(25);
    static final double TOTALS_PAD_V = px(12.6);
    static final double TOTALS_DUE_PAD_V = px(8.45);
    static final double TOTALS_RULE = px(3);

    /** The notes paragraph wraps well before its cell ends, so the width is the cell's. */
    static final double NOTES_TEXT_W = px(400);
    static final double NOTES_BODY_GAP = px(20.7);
    static final double NOTES_LINE_PITCH = 27;

    /**
     * What makes the notes/totals row not top-flush: the design starts the notes
     * heading a heading-height below the first totals line beside it.
     */
    static final double NOTES_TOP_OFFSET = px(50.5);

    // ------------------------------------------------------------------
    // Payment band, strip and closing band
    // ------------------------------------------------------------------

    /** The five payment columns, from the dividers' measured positions. */
    static final double[] PAYMENT_PX = {207.5, 178, 184, 185, 204.5};

    static final double PAY_ICON_W = px(42);
    static final double PAY_DIVIDER = px(2);
    static final double PAY_COLUMNS_GAP = px(18.9);
    static final double PAY_LABEL_GAP = px(15.8);
    static final double PAY_VALUE_GAP = px(6.6);

    /**
     * The design's outer two payment columns are not centred on the space the
     * dividers give them: their content sits about ten px inboard. The middle
     * three are centred to within three px, so the nudge is on the outer pair.
     */
    static final double PAY_OUTER_NUDGE = px(20);

    /** The closing strip: four segments across the full paper width. */
    static final double[] STRIP_PX = {272, 255, 252, 276};
    static final double STRIP_TOTAL_PX = DESIGN_PX_WIDTH;
    static final double STRIP_H = px(6);

    /**
     * The band's left inset is short of the page's, because the mark's SVG
     * carries its own padding: the design puts the glyph's <em>ink</em> at the
     * content edge, so the icon box has to start left of it to land there.
     */
    static final double CLOSE_PAD_L = px(42);
    static final double CLOSE_TEXT_INDENT = px(6);

    /** Lifts the mark off the text block's centre line, as the design does. */
    static final double CLOSE_ICON_LIFT = px(5);
    static final double CLOSE_ICON_COLUMN = px(79);
    static final double CLOSE_ICON_W = px(56);
    static final double CLOSE_PAD_T = px(24.8);
    static final double CLOSE_PAD_B = px(2);
    static final double CLOSE_LINE_PITCH = 24;

    // Inter-block gaps, each named by the two measured ink edges it reproduces.
    static final double GAP_MASTHEAD_TO_IDENTITY = px(19.9);
    static final double GAP_IDENTITY_TO_PARTIES = px(37.7);
    static final double GAP_PARTIES_TO_TABLE = px(43.8);
    static final double GAP_TABLE_TO_TOTALS = px(2.1);
    static final double GAP_TABLE_TO_PAYMENT = px(36.1);
    static final double GAP_PAYMENT_TO_NOTE = px(21.3);
    static final double GAP_NOTE_TO_STRIP = px(32.1);
    static final double GAP_STRIP_TO_BAND = px(1);
}
