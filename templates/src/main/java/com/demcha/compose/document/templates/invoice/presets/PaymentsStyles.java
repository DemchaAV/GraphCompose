package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.font.FontName;

/**
 * Design tokens for {@link PaymentsInvoice} — the palette, the type scale and
 * the geometry every part of the sheet measures against.
 *
 * <h2>One scale, and one content box</h2>
 *
 * <p>The design was drawn at 1055 px across an A4 page, so {@link #PX} converts
 * every length below from that raster to points. The content box is the
 * <em>table's</em> box, not the text's: body copy sits a further
 * {@link #TEXT_INSET} inside it, while the table, the payment card and the
 * total-due panel sit on it exactly, which is what makes it the page's real
 * edge.</p>
 *
 * <h2>Ink measurements against box measurements</h2>
 *
 * <p>Every vertical distance read off the design is between two pieces of ink;
 * every one the engine accepts is between two boxes. Text sits about
 * {@link #TEXT_TOP_BEARING} of its size below its own box top — the half-leading
 * of a 1.2× line box plus the run from the ascender down to the cap — so a gap
 * read ink-to-ink loses that before it can be a margin. {@link #gap} and
 * {@link #capOffset} are the two places that conversion happens.</p>
 *
 * <h2>Sizes come from widths, not heights</h2>
 *
 * <p>Each size below is a measured ink <em>width</em> divided by the face's own
 * advance for the same string at unit cap. The two disagree for all-caps runs,
 * where the body face sets about 6% wider than the design's at the same cap
 * height, and width is the one to match: it keeps every glyph after the first in
 * place, where matching the height would leave the run's right edge short by 6%
 * of its length.</p>
 */
final class PaymentsStyles {

    private PaymentsStyles() {
    }

    static final DocumentPageSize PAGE = DocumentPageSize.A4;
    static final double PAGE_WIDTH = PAGE.width();

    /** The design's raster is 1055 px wide; every length below came off it. */
    static final double PX = PAGE_WIDTH / 1055.0;

    static final double MARGIN_L = px(51);
    static final double MARGIN_R = px(52);
    static final double MARGIN_T = px(28);

    /**
     * The band the page number sits in, measured up from the paper's edge, and
     * page one's bottom margin, which is the same thing.
     *
     * <p>The design is a one-page sheet and has no page number, but this preset
     * flows, and a financial record that paginates has to make a missing page
     * detectable. The band is reserved rather than merely drawn into: on a
     * paginated invoice page one is all table, and without the margin the last
     * row runs down to where the number is.</p>
     */
    static final double PAGE_NUMBER_BAND = px(32);
    static final double MARGIN_B_FIRST = PAGE_NUMBER_BAND;

    /**
     * A continuation page reserves more. Its last table row would otherwise end
     * against the paper rather than against a margin, which page one never shows
     * because its content stops well above the fold.
     */
    static final double MARGIN_B_LATER = px(58);

    static final DocumentInsets PAGE_MARGIN =
            new DocumentInsets(MARGIN_T, MARGIN_R, MARGIN_B_FIRST, MARGIN_L);
    static final DocumentInsets CONTINUATION_MARGIN =
            new DocumentInsets(MARGIN_T, MARGIN_R, MARGIN_B_LATER, MARGIN_L);

    /** Every width in this preset is a fraction of this: 952 design px. */
    static final double CONTENT_W = PAGE_WIDTH - MARGIN_L - MARGIN_R;

    /** How far body copy sits inside the content box the table defines. */
    static final double TEXT_INSET = px(11);

    static final DocumentColor ACCENT = DocumentColor.rgb(99, 91, 255);
    static final DocumentColor INK = DocumentColor.rgb(14, 24, 72);
    static final DocumentColor BODY = DocumentColor.rgb(46, 54, 92);
    static final DocumentColor MUTED = DocumentColor.rgb(70, 76, 110);
    static final DocumentColor SURFACE = DocumentColor.WHITE;

    /**
     * Three lavender surfaces, not one. Sampled as rings inside each disc, clear
     * of the glyph: the party and line item discs agree, the notes disc is two
     * steps paler and the payment card's is two steps deeper. They look like one
     * colour and measure as three.
     */
    static final DocumentColor ACCENT_SURFACE = DocumentColor.rgb(224, 218, 253);
    static final DocumentColor NOTES_SURFACE = DocumentColor.rgb(226, 222, 253);
    static final DocumentColor CARD_DISC_SURFACE = DocumentColor.rgb(216, 208, 252);
    static final DocumentColor CARD_SURFACE = DocumentColor.rgb(248, 247, 254);
    static final DocumentColor TABLE_HEAD_FILL = DocumentColor.rgb(240, 238, 254);
    static final DocumentColor BAND_LAVENDER = DocumentColor.rgb(205, 194, 248);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(227, 227, 238);
    static final DocumentColor RULE_SOFT = DocumentColor.rgb(213, 208, 239);
    static final DocumentColor RULE_STRONG = DocumentColor.rgb(159, 145, 222);
    static final DocumentColor DIVIDER = DocumentColor.rgb(199, 191, 236);

    static final FontName FACE = FontName.LATO;

    /**
     * The face's cap heights in ems. Regular and bold differ, and the difference
     * matters: every size below is a measured cap divided by the ratio for the
     * weight it is set in.
     */
    private static final double CAP_RATIO_REGULAR = 0.7165;
    private static final double CAP_RATIO_BOLD = 0.723;

    /** How far below its own box top a line of text sits. */
    static final double TEXT_TOP_BEARING = 0.235;

    /**
     * {@code lineSpacing} is points added <em>between</em> lines over a line box
     * of 1.2× the type size, so a measured pitch converts as
     * {@code pitch - 1.2 * size}, never as a ratio. Section spacing works the
     * same way: it is the gap between sibling boxes, so a measured top-to-top
     * pitch has the first box's height taken off it.
     */
    static final double LINE_BOX = 1.2;

    static final double TITLE_SIZE = sizeB(46.53);
    static final double ISSUER_NAME_SIZE = sizeB(14.65);
    static final double PARTY_NAME_SIZE = sizeB(13.11);
    static final double PARTY_LABEL_SIZE = sizeB(10.91);
    static final double META_LABEL_SIZE = sizeB(11.59);
    static final double META_VALUE_SIZE = sizeR(10.35);
    static final double BODY_SIZE = sizeR(11.08);
    static final double TABLE_HEAD_SIZE = sizeB(10.22);

    /** The currency qualifier after the amount label is a smaller run of it. */
    static final double TABLE_HEAD_SMALL_SIZE = sizeB(8.0);
    static final double ITEM_TITLE_SIZE = sizeB(12.18);
    static final double ITEM_SUB_SIZE = sizeR(10.17);
    static final double CELL_SIZE = sizeR(11.19);
    static final double CELL_AMOUNT_SIZE = sizeB(12.25);
    static final double CARD_HEAD_SIZE = sizeB(11.30);
    static final double CARD_LABEL_SIZE = sizeB(9.44);
    static final double CARD_VALUE_SIZE = sizeR(9.33);
    static final double TOTALS_LABEL_SIZE = sizeB(12.07);
    static final double TOTALS_VALUE_SIZE = sizeR(11.72);
    static final double TOTAL_DUE_LABEL_SIZE = sizeB(13.04);
    static final double TOTAL_DUE_SUB_SIZE = sizeR(10.79);
    static final double TOTAL_DUE_VALUE_SIZE = sizeB(22.18);
    static final double NOTES_LABEL_SIZE = sizeB(10.29);
    static final double NOTES_BODY_SIZE = sizeR(10.07);
    static final double FOOTER_DUE_TITLE_SIZE = sizeB(10.16);
    static final double FOOTER_SUPPORT_TITLE_SIZE = sizeB(11.12);
    static final double FOOTER_SUB_SIZE = sizeR(10.05);
    static final double PAGE_NUMBER_SIZE = sizeR(7.5);

    /*
     * The diagonal band: two slanted quadrilaterals in the gutter between the
     * lockup and the title, the lavender running from the paper's top edge and
     * the navy drawn over it from a third of the way down. Corners are given in
     * PAGE design pixels — the coordinates that were actually measured — and
     * normalised in one place by nx/ny, because a path's own coordinates run
     * 0..1 inside its box with the origin at the BOTTOM-LEFT, which is the
     * opposite of the direction every measurement here was taken in.
     */
    static final double BAND_LEFT_PX = 385.4;
    static final double BAND_RIGHT_PX = 556.0;
    static final double BAND_BOTTOM_PX = 360.0;
    static final double BAND_W = px(BAND_RIGHT_PX - BAND_LEFT_PX);
    static final double BAND_H = px(BAND_BOTTOM_PX);

    /** Top-left, top-right, bottom-right, bottom-left, in page design pixels. */
    static final double[][] BAND_LAVENDER_QUAD = {
        {495.0, 0.0}, {539.6, 0.0}, {422.4, 360.0}, {385.4, 360.0}
    };
    static final double[][] BAND_NAVY_QUAD = {
        {507.7, 98.0}, {554.7, 98.0}, {469.3, 360.0}, {422.3, 360.0}
    };

    /**
     * How tall the masthead's layer stack comes out. The band is the tallest
     * layer in it, so the stack's own height is the band's — and the header rule
     * that follows the stack has to be pulled back up by the difference between
     * the band's bottom and where the stack ends. The band is offset up by
     * {@link #MARGIN_T} to reach the paper, and the stack is sized to its layers
     * without accounting for that offset, so the stack ends that much below the
     * band's visible bottom.
     */
    static final double HEADER_STACK_BOTTOM_PX = BAND_BOTTOM_PX + 28.0;

    /**
     * Where the metadata column starts, as an offset into the content box. The
     * title shares it: the title is a left-anchored member of the metadata
     * column, not a right-anchored member of the page, and its measured left
     * edge is the metadata labels' to the pixel.
     */
    static final double META_COL_X = px(639 - 51);

    /**
     * The lockup's box, measured off the design's own ink: 136 design px wide
     * and 55.3 tall, starting at {@link #BRAND_LOCKUP_INSET} into the content
     * box. A caller's logo is sized by the height, so a wider or narrower mark
     * keeps the design's optical weight and stays on the same top edge.
     */
    static final double BRAND_LOCKUP_W = px(136);
    static final double BRAND_LOCKUP_H = px(55.3);
    static final double BRAND_LOCKUP_INSET = px(63 - 51);

    /**
     * The size a wordmark is set at when a document brings a name rather than a
     * logo. Not the lockup's own height: a name is usually longer than a
     * drawn mark and set at that height would run out of the column, so it is
     * sized to read as a lockup beside the title rather than to fill the box.
     */
    static final double WORDMARK_SIZE = sizeB(28);

    /**
     * The lockup sets the row's top edge, so the title's own line box needs
     * pushing down to its measured cap. The 1.4 px correction is measured, not
     * derived: at 46 design px of cap the top bearing runs slightly deeper than
     * the fraction that holds at body sizes.
     */
    static final double TITLE_DROP = px(65 - 46 - 1.4) - TEXT_TOP_BEARING * TITLE_SIZE;

    static final double ACCENT_BAR_W = px(2);
    static final double ACCENT_BAR_H = px(310);

    /** The bar is anchored to the PAGE, 22 design px left of the content box. */
    static final double ACCENT_BAR_OFFSET = px(-22);

    static final double META_LABEL_W = px(781.5 - 639);
    static final double META_VALUE_INSET = px(805 - 781.5);
    static final double META_PITCH = 30.17;

    static final double HEADER_RULE_W = px(1018 - 30);

    /** How far the rule reaches past the content box: 21 design px left, 15 right. */
    static final double HEADER_RULE_OVERHANG_L = px(51 - 30);
    static final double HEADER_RULE_OVERHANG_R = px(1018 - 1003);
    static final double RULE_THIN = px(1.4);
    static final double RULE_MED = px(2.0);

    static final double DISC_D = px(40);

    /**
     * <b>Box, not ink.</b> An icon's size is the box the artwork is fitted into,
     * and every glyph here carries about two units of padding on each side of
     * its 24-unit viewBox — so the ink comes back at roughly this fraction of
     * what is asked for. Each glyph size below is therefore the design's
     * measured ink divided by that ratio, which is why they read larger than the
     * marks they produce.
     */
    static final double GLYPH_INK_RATIO = 0.75;
    static final double DISC_GLYPH = glyph(21);
    static final double PARTY_DISC_INSET = px(60 - 51);
    static final double PARTY_DISC_GAP = px(118 - 100);
    static final double PARTY_TEXT_INDENT = DISC_D + PARTY_DISC_GAP;

    /** The second cell's own left padding, past the divider that is its left border. */
    static final double PARTY_CELL_INSET = px(567 - 527);
    static final double PARTY_ADDRESS_PITCH = 20.7;

    /**
     * Column shares of the content width. Derived from each column's measured
     * <em>anchor</em> rather than from boundaries the design never draws: the
     * description text's left edge, the quantity digits' centre, the unit
     * price's right edge, the tax centre and the amount's right edge.
     */
    static final double[] COLUMN_SHARES = {
        379.0 / 952.0, 127.0 / 952.0, 143.0 / 952.0, 139.0 / 952.0, 164.0 / 952.0
    };

    static final double HEAD_PAD_T = px(11.7);
    static final double HEAD_PAD_B = px(12.3);
    static final double CELL_PAD_V = px(8.5);

    /** The description cell's own inset; the disc sits on it and the text past it. */
    static final double ITEM_CELL_INSET = px(62 - 51);
    static final double ITEM_DISC_GAP = px(118 - 102);

    /**
     * What a line's description wraps at. Not a free choice: the design breaks
     * one description before its parenthetical and keeps the next whole, which
     * puts the width between 248 and 256 design px. Everything about the table's
     * row heights follows from it.
     */
    static final double ITEM_TEXT_W = px(250);
    static final double ITEM_SUB_PITCH = 18.0;
    static final double CELL_PAD_R = px(22);
    static final double AMOUNT_PAD_R = px(23);
    static final double DESC_PAD_L = px(118 - 51);

    static final DocumentTableStyle HEADER_CELL_STYLE = DocumentTableStyle.builder()
            .fillColor(TABLE_HEAD_FILL)
            .stroke(DocumentStroke.of(HAIRLINE, RULE_THIN))
            .padding(new DocumentInsets(HEAD_PAD_T, 0, HEAD_PAD_B, 0))
            .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
            .build();

    /**
     * Every rule in the table comes from this one stroke. A cell strokes all
     * four of its own edges and a one-column table has no interior vertical edge
     * — so the same stroke that draws the separator between two rows draws the
     * table's left and right sides, and nothing else. That is exactly the
     * topology the design shows.
     */
    static final DocumentTableStyle ITEM_CELL_STYLE = DocumentTableStyle.builder()
            .stroke(DocumentStroke.of(HAIRLINE, RULE_THIN))
            .padding(new DocumentInsets(CELL_PAD_V, 0, CELL_PAD_V, 0))
            .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
            .build();

    /**
     * The settlement split is NOT the even split the two rows above it use: the
     * card is 440 design px, the totals cell 455, and the divider sits inside
     * the gutter rather than on either cell's edge. That asymmetry is in the
     * design, not in the measurement.
     */
    static final double CARD_W = px(440);
    static final double SETTLEMENT_GUTTER = px(527.5 - 491);
    static final double SETTLEMENT_RIGHT_INSET = px(547 - 527.5);

    static final double CARD_RADIUS = px(10);

    /**
     * The card's own disc is smaller than the party and line item discs — 38
     * design px against 40 — and it sits 5 px below the card's top edge, not 12.
     * Both were measured off the card rather than assumed from the other three,
     * and both matter: the disc sets the head row's height, and that height is
     * what the field block's margin is measured from.
     */
    static final double CARD_DISC_D = px(38);
    static final double CARD_GLYPH = glyph(21);
    static final DocumentInsets CARD_PAD =
            new DocumentInsets(px(1065 - 1060), px(20), px(1252 - 1238.5), 0);
    static final double CARD_DISC_INSET = px(63.5 - 51);
    static final double CARD_HEAD_GAP = px(118 - 101);
    static final double CARD_LABEL_W = px(256 - 116);
    static final double CARD_TEXT_INDENT = px(116 - 51);
    static final double CARD_FIELD_PITCH = 19.3;

    /** The disc's bottom edge, measured down from the card's content top. */
    static final double CARD_HEAD_BOTTOM_PX = 1065 + 38;

    static final double TOTALS_TEXT_INSET = px(566 - 547);
    static final double TOTALS_PAD_R = px(1003 - 987);
    static final double TOTALS_PITCH = 38.0;
    static final double TOTALS_RULE_W = px(1002 - 547);

    static final double DUE_RADIUS = px(10);

    /**
     * The top inset is the distance to the label's <em>box</em>, not to its cap:
     * the measured cap gap less what the text hangs below its own box top.
     * Taking the cap gap as a padding makes the panel 4 px too tall and pushes
     * its bottom past the card's, which the design has ending on the same line.
     */
    static final DocumentInsets DUE_PAD = new DocumentInsets(
            px(1194 - 1173) - TEXT_TOP_BEARING * TOTAL_DUE_LABEL_SIZE,
            px(1002 - 984),
            px(1251 - 1235),
            px(566 - 547));
    static final double DUE_SUB_PITCH = 28.0;

    static final double NOTES_DISC_D = px(39);
    static final double NOTES_DISC_INSET = px(53 - 51);
    static final double NOTES_TEXT_INDENT = px(106 - 53);
    static final double NOTES_GLYPH = glyph(22);
    static final double NOTES_PITCH = 22.0;

    static final double FOOTER_RULE_W = px(1005 - 50);

    /**
     * The rule's own gaps. The 1.2 px correction is measured and is taken off
     * the gap below as well as added to the gap above: the footer's text already
     * sits on the design's line, so the rule has to move without taking the
     * footer with it.
     */
    static final double FOOTER_RULE_NUDGE = px(1.2);
    static final double FOOTER_RULE_GAP_ABOVE = px(1373 - 1343) + FOOTER_RULE_NUDGE;
    static final double FOOTER_RULE_GAP_BELOW = px(1397 - 1374) - FOOTER_RULE_NUDGE;

    /** One design px past the content box on the left, two on the right. Measured. */
    static final double FOOTER_RULE_OVERHANG_L = px(51 - 50);
    static final double FOOTER_RULE_OVERHANG_R = px(1005 - 1003);
    static final double FOOTER_SPLIT_X = px(541 - 51);
    static final double FOOTER_DUE_ICON_COL = px(112 - 55);
    static final double FOOTER_DUE_ICON_INSET = px(55 - 51);
    static final double FOOTER_DUE_ICON = glyph(37);
    static final double FOOTER_SUPPORT_CELL_INSET = px(594 - 541);
    static final double FOOTER_SUPPORT_ICON_COL = px(659 - 594);
    static final double FOOTER_SUPPORT_ICON = glyph(39);

    static double px(double designPixels) {
        return designPixels * PX;
    }

    /**
     * An icon box sized so its ink comes out at {@code inkPx} design pixels —
     * see {@link #GLYPH_INK_RATIO}. Every glyph goes through here, so the one
     * measured ratio lives in one place.
     */
    static double glyph(double inkPx) {
        return px(inkPx) / GLYPH_INK_RATIO;
    }

    /** Half the padding {@link #glyph} adds — what a left-anchored glyph gives back. */
    static double glyphPad(double inkPx) {
        return (glyph(inkPx) - px(inkPx)) / 2.0;
    }

    /** A regular-weight size named by the cap height it reproduces, in design px. */
    static double sizeR(double capPx) {
        return px(capPx) / CAP_RATIO_REGULAR;
    }

    /** The same for bold, whose cap ratio differs. */
    static double sizeB(double capPx) {
        return px(capPx) / CAP_RATIO_BOLD;
    }

    /**
     * Where to start a text box so its caps land {@code capPx} below the top of
     * the block it shares a row with — a label beside a glyph, whose design
     * position is its own cap top rather than the glyph's centre.
     */
    static double capOffset(double capPx, double size) {
        return Math.max(0, px(capPx) - TEXT_TOP_BEARING * size);
    }

    /**
     * A measured top-to-top pitch turned into the gap the engine wants: the
     * first box's own height comes off it, because spacing is between boxes.
     */
    static double gap(double pitchPx, double sizeOfFirst) {
        return Math.max(0, px(pitchPx) - LINE_BOX * sizeOfFirst);
    }

    static DocumentTextStyle style(double size, DocumentColor color,
                                   DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(FACE)
                .size(size)
                .color(color)
                .decoration(decoration)
                .build();
    }
}
