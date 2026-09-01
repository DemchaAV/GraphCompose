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

import java.util.Map;

/**
 * Design tokens for {@link WorkspaceInvoice} — the palette, the type scale and
 * the geometry every part of the sheet measures against.
 *
 * <p>The design was drawn at 1055 px across an A4 page, so {@link #PX} converts
 * every length below from that raster to points.</p>
 *
 * <h2>Sizes carry two corrections, both measured</h2>
 *
 * <p>A cap measured on a screenshot includes about half a pixel of antialiasing
 * on each edge and the whole of it lands in the size, so {@link #sizeForCap}
 * takes it back off. And the body face sets uppercase about 6% wider than the
 * design's face at the same cap height — measured across three all-caps runs,
 * while mixed-case runs at the same sizes matched within 2% — so
 * {@link #sizeForCaps} carries that as a size correction. There is no
 * letter-spacing API to take it out with instead.</p>
 */
final class WorkspaceStyles {

    private WorkspaceStyles() {
    }

    static final DocumentPageSize PAGE = DocumentPageSize.A4;
    static final double PAGE_WIDTH = PAGE.width();

    /** The design's raster is 1055 px wide; every length below came off it. */
    static final double PX = PAGE_WIDTH / 1055.0;

    static final double MARGIN_T = px(46);
    static final double MARGIN_R = px(47);
    static final double MARGIN_L = px(45);

    /**
     * Page one's bottom margin is the page-number band: whatever else is on the
     * page, the band has to be reserved, or a paginated invoice runs its last
     * line straight through the number.
     */
    static final double MARGIN_B_FIRST = px(28);
    static final double PAGE_NUMBER_BAND = px(28);

    /**
     * How far the closing rule, the thank-you block and the brand band sit above
     * the design's own positions. It buys the page number a band to sit in;
     * without it there is nowhere on the page for one, because the design's
     * closing block runs to within 15 px of the paper.
     *
     * <p>Distributed rather than taken in one place, and the distribution is not
     * arbitrary: the closing rule sits 17 px under the payment card, so taking
     * the whole lift off its top margin would pull it into the card's bottom
     * border. Nine px come off the rule — leaving it 8 px clear of the card —
     * and the rest out of the two gaps below it. The three shares are not
     * independent: a gap inside the block moves everything below it as well, so
     * the band's total lift is their sum.</p>
     */
    static final double CLOSING_LIFT = px(18);
    static final double CLOSING_RULE_LIFT = px(9);
    static final double CLOSING_INNER_LIFT = (CLOSING_LIFT - CLOSING_RULE_LIFT) / 2.0;

    /**
     * A continuation page reserves more: its last row would otherwise end
     * against the paper rather than against a margin.
     */
    static final double MARGIN_B_LATER = px(58);

    static final DocumentInsets PAGE_MARGIN =
            new DocumentInsets(MARGIN_T, MARGIN_R, MARGIN_B_FIRST, MARGIN_L);
    static final DocumentInsets CONTINUATION_MARGIN =
            new DocumentInsets(MARGIN_T, MARGIN_R, MARGIN_B_LATER, MARGIN_L);

    /** Every width in this preset is a fraction of this. */
    static final double CONTENT_W = PAGE_WIDTH - MARGIN_L - MARGIN_R;

    /**
     * The issuer/metadata and bill-to/ship-to rows both split the content box in
     * half: measured, both vertical dividers land on the same x.
     */
    static final double HALF = 0.5;

    static final DocumentColor ACCENT = DocumentColor.rgb(85, 42, 164);
    static final DocumentColor ACCENT_SURFACE = DocumentColor.rgb(245, 242, 253);
    static final DocumentColor ACCENT_BORDER = DocumentColor.rgb(199, 184, 236);
    static final DocumentColor INK = DocumentColor.rgb(15, 16, 20);
    static final DocumentColor BODY = DocumentColor.rgb(45, 47, 53);
    static final DocumentColor MUTED = DocumentColor.rgb(100, 102, 108);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(232, 234, 238);
    static final DocumentColor HAIRLINE_STRONG = DocumentColor.rgb(214, 216, 221);
    static final DocumentColor SURFACE = DocumentColor.WHITE;
    static final DocumentColor TILE_BLUE = DocumentColor.rgb(45, 90, 242);
    static final DocumentColor TILE_GREEN = DocumentColor.rgb(31, 122, 85);

    static final FontName FACE = FontName.LATO;

    /** The face's cap height in ems, read off the TTF. */
    private static final double FACE_CAP_RATIO = 0.7165;

    /** The antialiasing a cap measured on a screenshot carries on each edge. */
    private static final double CAP_INK_BLEED = 0.5;

    /** How far below its own box top a line of text sits. */
    static final double TEXT_TOP_BEARING = 0.235;

    /**
     * {@code lineSpacing} is points added <em>between</em> lines over a line box
     * of 1.2× the type size, so a measured pitch converts as
     * {@code pitch - 1.2 * size}, never as a ratio.
     */
    static final double LINE_BOX = 1.2;

    /**
     * The one size not taken straight from its cap. At the size its 45 px cap
     * implies the title's ink came back 2.9% narrower than the design's with the
     * right edges aligned, so it is set 3% larger — which costs 1.4 px of cap
     * height to buy back 7 px of width.
     */
    static final double TITLE_SIZE = sizeForCap(45) * 1.03;

    /**
     * 3% under the size its 15 px cap implies. The two names set at it straddle
     * the design — one comes back 1.9% wide and the other 6.9% — so the
     * correction halves the larger error rather than making one exact and the
     * other worse.
     */
    static final double NAME_SIZE = sizeForCap(15) * 0.97;
    static final double BODY_SIZE = sizeForCap(12.5);
    static final double META_LABEL_SIZE = sizeForCap(12);

    /**
     * The party labels keep the uncorrected size: measured against the design
     * they come back 5% narrow plain and 11% narrow with the caps correction, so
     * they are the one all-caps run the correction does not help.
     */
    static final double SECTION_LABEL_SIZE = sizeForCap(12.5);
    static final double TABLE_HEAD_SIZE = sizeForCaps(11);
    static final double ITEM_TITLE_SIZE = sizeForCap(13);
    static final double ITEM_SUB_SIZE = sizeForCap(11);
    static final double CELL_SIZE = sizeForCaps(12);
    static final double TOTALS_SIZE = sizeForCap(12);
    static final double TOTAL_LABEL_SIZE = sizeForCaps(15);
    static final double TOTAL_VALUE_SIZE = sizeForCap(19);
    static final double CARD_TITLE_SIZE = sizeForCaps(12);
    static final double CARD_TEXT_SIZE = sizeForCap(11);
    static final double DUE_LABEL_SIZE = sizeForCaps(12);
    static final double DUE_VALUE_SIZE = sizeForCap(16);
    static final double THANKS_TITLE_SIZE = sizeForCap(12);
    static final double THANKS_BODY_SIZE = sizeForCap(10);
    static final double FOOTER_SIZE = sizeForCap(11);
    static final double WORDMARK_SIZE = sizeForCap(25);
    static final double PAGE_NUMBER_SIZE = sizeForCap(8);

    static final double RULE_THIN = px(1.4);
    static final double RULE_MED = px(1.6);
    static final double ACCENT_RULE_W = px(41);
    static final double ACCENT_RULE_T = px(4);

    /**
     * The lockup's box, measured off the design's own ink. A caller's logo is
     * sized by the width, which is the dimension the design fixes.
     */
    static final double LOCKUP_W = px(212);

    /** The title's ink stops short of the content box, not on it. */
    static final double TITLE_RIGHT_INSET = px(13);

    /**
     * The lockup sets the row's top edge, so the title's own line box hangs its
     * caps below where the design draws them. Nothing else in the row can absorb
     * that, so the title is lifted by it.
     */
    static final double TITLE_TOP_LIFT = px(7.3);

    static final double DISC_D = px(42);
    static final double DISC_GLYPH = px(20);
    static final double DISC_GAP = px(18);

    /** The party block's text sits on the label's axis, indented past the disc. */
    static final double PARTY_TEXT_INDENT = DISC_D + DISC_GAP;
    static final double ISSUER_TAX_LABEL_W = px(63);
    static final double PARTY_TAX_LABEL_W = px(79);

    /** Metadata labels start at the cell's edge and values 180 px along it. */
    static final double META_LABEL_W = px(180);
    static final double META_CELL_INSET = px(80);

    /**
     * Column shares of the content width, derived from the header labels'
     * measured centres rather than from boundaries the design never draws.
     */
    static final double[] COLUMN_SHARES = {0.383178, 0.176532, 0.118380, 0.172378, 0.149533};

    static final double HEAD_PAD_V = px(18.4);
    static final double HEAD_PAD_L = px(67);

    /**
     * Asymmetric, measured: the design's row content sits 19 px below its top
     * rule and 16 px above the next. Symmetric padding centres it and leaves the
     * text about 3 px low.
     */
    static final double CELL_PAD_T = px(17.0);
    static final double CELL_PAD_B = px(14.0);
    static final double CELL_PAD_L = px(15);
    static final double ITEM_TILE_D = px(33);
    static final double ITEM_TILE_RADIUS = px(8);
    static final double ITEM_GLYPH = px(19);
    static final double ITEM_TEXT_GAP = px(19);

    /**
     * Every rule in the table comes from one of these two styles. A cell strokes
     * all four of its own edges, and a one-column table has no interior vertical
     * edge — so the same stroke that draws the separator between two rows draws
     * the table's left and right sides, and nothing else.
     */
    static final DocumentTableStyle HEADER_CELL_STYLE = DocumentTableStyle.builder()
            .fillColor(ACCENT_SURFACE)
            .stroke(DocumentStroke.of(ACCENT_BORDER, RULE_THIN))
            .padding(new DocumentInsets(HEAD_PAD_V, 0, HEAD_PAD_V, 0))
            .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
            .build();

    static final DocumentTableStyle ITEM_CELL_STYLE = DocumentTableStyle.builder()
            .stroke(DocumentStroke.of(HAIRLINE, RULE_THIN))
            .padding(new DocumentInsets(CELL_PAD_T, 0, CELL_PAD_B, 0))
            .textAnchor(DocumentTableTextAnchor.CENTER_LEFT)
            .build();

    /**
     * The settlement row is NOT the half split the two rows above it use: its
     * left cell is 420 design px, its right 433, with a 110 px gutter. That
     * asymmetry is in the design, not in the measurement.
     */
    static final double SETTLEMENT_LEFT = px(420);
    static final double SETTLEMENT_GUTTER = px(110);
    static final double SETTLEMENT_RIGHT = px(433);
    static final double SETTLEMENT_RIGHT_INSET = px(29);

    static final double CARD_RADIUS = px(12);
    static final DocumentInsets CARD_PAD = new DocumentInsets(px(17), px(24), px(22), px(24));
    static final double CARD_LABEL_W = px(162);

    /** The card's inner width — what its interior rule spans. */
    static final double CARD_INNER_W = SETTLEMENT_LEFT - px(24) - px(24);
    static final double CARD_ICON = px(27);
    static final double CARD_ICON_GAP = px(21);
    static final double NOTE_ICON_COL = px(30);
    static final double NOTE_ICON = px(20);

    static final double TOTALS_PAD_L = px(6);
    static final double TOTALS_PAD_R = px(10);
    static final double TOTALS_RULE_W = px(397);

    static final double DUE_RADIUS = px(12);
    static final DocumentInsets DUE_PAD = new DocumentInsets(px(25), px(24), px(28), px(23));
    static final double DUE_TILE = px(38);
    static final double DUE_TILE_RADIUS = px(9);
    static final double DUE_GLYPH = px(22);
    static final double DUE_TILE_GAP = px(24);

    static final double THANKS_INDENT = px(6);
    static final double THANKS_ICON_COL = px(46);
    static final double THANKS_ICON = px(30);

    static final double FOOTER_MARK_COL = px(144);
    static final double FOOTER_LINK_COL = px(150);

    /**
     * Which surface a line's glyph is knocked out of. A token with no colour
     * here is drawn bare, which is how the design sets a mark that carries its
     * own.
     */
    static final Map<String, DocumentColor> TILE_COLOR = Map.of(
            "search", ACCENT,
            "grid", TILE_BLUE,
            "shield", TILE_GREEN);

    static double px(double designPixels) {
        return designPixels * PX;
    }

    /** A size named by the cap height it reproduces, in design px. */
    static double sizeForCap(double capPx) {
        return px(capPx - CAP_INK_BLEED) / FACE_CAP_RATIO;
    }

    /** The same for an all-caps run, which the face sets 6% wide. */
    static double sizeForCaps(double capPx) {
        return sizeForCap(capPx) * 0.94;
    }

    /**
     * Where to start a text box so its caps land {@code capPx} below the top of
     * the block it shares a row with — a label beside a glyph, whose design
     * position is its own baseline rather than the glyph's centre.
     */
    static double capOffset(double capPx, double size) {
        return Math.max(0, px(capPx) - TEXT_TOP_BEARING * size);
    }

    /** What a text box hangs below its last cap: the rest of the line box. */
    static double bottomBearing(double size) {
        return (LINE_BOX - TEXT_TOP_BEARING - FACE_CAP_RATIO) * size;
    }

    /**
     * A block gap read ink-to-ink on the design, turned into the margin the
     * engine wants. Pass 0 for a neighbour that is not text — a rule, a disc, a
     * table — which has no bearing to lose.
     */
    static double blockGap(double inkGapPx, double sizeAbove, double sizeBelow) {
        return Math.max(0, px(inkGapPx)
                - (sizeAbove > 0 ? bottomBearing(sizeAbove) : 0)
                - (sizeBelow > 0 ? TEXT_TOP_BEARING * sizeBelow : 0));
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
