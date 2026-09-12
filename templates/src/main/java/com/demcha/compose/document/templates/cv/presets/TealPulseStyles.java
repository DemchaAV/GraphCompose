package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * The measured geometry, palette and type scale of the Teal Pulse CV.
 *
 * <h2>Every length is a measurement off one grid</h2>
 *
 * <p>The design was drawn on a 1103×1426 raster, so each length here is
 * written as {@link #px} of a pixel measured off it and {@code px} is the one
 * conversion into points. Re-measuring the drawing therefore changes one
 * number per length and never a chain of them — and the page itself takes
 * that grid's proportion rather than A4's, which is why this preset sets its
 * own page.</p>
 *
 * <h2>Leading is not a multiple of the type size</h2>
 *
 * <p>A paragraph's line pitch on this engine is about
 * {@code 1.216 × size + 0.872 × lineSpacing}, so the leading constants below
 * are the values that produce the drawn pitch rather than ratios of it. A
 * single-line paragraph ignores leading entirely and takes {@code 1.216 ×
 * size}, which is why the bullet rhythm is section spacing instead.</p>
 */
final class TealPulseStyles {

    private TealPulseStyles() {
    }

    // -- the grid ----------------------------------------------------------

    /** The raster the design was drawn on. */
    static final double REFERENCE_WIDTH_PX = 1103.0;
    static final double REFERENCE_HEIGHT_PX = 1426.0;

    static final double PAGE_WIDTH = 595.28;
    static final double PAGE_HEIGHT = PAGE_WIDTH * REFERENCE_HEIGHT_PX / REFERENCE_WIDTH_PX;
    static final DocumentPageSize PAGE = DocumentPageSize.of(PAGE_WIDTH, PAGE_HEIGHT);

    /**
     * One measurement off the drawing, in points.
     *
     * @param referencePixels the pixel measured on the 1103-wide raster
     * @return the same distance on the page
     */
    static double px(double referencePixels) {
        return referencePixels * PAGE_WIDTH / REFERENCE_WIDTH_PX;
    }

    static final double MARGIN_X = px(53);
    static final double PAD_TOP = px(34);
    static final double PAD_BOTTOM = px(14);
    static final double CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_X;

    /** The sidebar runs from the left margin to the column rule. */
    static final double SIDEBAR_WEIGHT = 0.26533;
    static final double MAIN_WEIGHT = 1.0 - SIDEBAR_WEIGHT;
    static final double MAIN_PAD_LEFT = px(32);
    static final double MAIN_CONTENT_WIDTH = CONTENT_WIDTH * MAIN_WEIGHT - MAIN_PAD_LEFT;

    /**
     * The closing band's first divider is collinear with the body's column
     * rule, so its first weight is the sidebar's — that is the shared grid,
     * not a coincidence to be measured twice.
     */
    static final double BAND_WEIGHT_1 = SIDEBAR_WEIGHT;
    static final double BAND_WEIGHT_2 = 0.37789;
    static final double BAND_WEIGHT_3 = 1.0 - BAND_WEIGHT_1 - BAND_WEIGHT_2;

    static final double BAND_PAD_LEFT = px(36);

    /**
     * Two right insets, because the design uses two: a heading's underline
     * stops well short of the divider while the body under it runs on almost
     * to the rule. One inset for both would either wrap the longest line or
     * overrun every underline.
     */
    static final double BAND_RULE_INSET = px(40);
    static final double BAND_COL1_WIDTH = CONTENT_WIDTH * BAND_WEIGHT_1 - BAND_RULE_INSET;
    static final double BAND_COL2_WIDTH =
            CONTENT_WIDTH * BAND_WEIGHT_2 - BAND_PAD_LEFT - BAND_RULE_INSET;
    static final double BAND_COL3_WIDTH = CONTENT_WIDTH * BAND_WEIGHT_3 - BAND_PAD_LEFT;

    /**
     * Two blocks wrap inside a measure narrower than the column holding them,
     * and the design is unambiguous about both: the summary breaks with a
     * finger's width of its column still free, and so does the first fact.
     * Reproducing the line breaks means reproducing the measure, not only the
     * type size.
     */
    static final double SUMMARY_RIGHT_INSET = px(105);
    static final double BAND_COL3_TEXT_INSET = px(79);

    // -- type --------------------------------------------------------------

    static final FontName DISPLAY_FONT = FontName.POPPINS;

    /**
     * The drawing's heading face sets about a fifth narrower per cap height
     * than the body's bold, so the headings take a condensed face and keep
     * both dimensions rather than shrinking to fit and losing the cap height.
     */
    static final FontName HEADING_FONT = FontName.BARLOW_CONDENSED;
    static final FontName BODY_FONT = FontName.LATO;

    static final double NAME_SIZE = 39.5;
    static final double ROLE_SIZE = 13.84;
    static final double CONTACT_SIZE = 7.86;
    static final double SIDEBAR_HEADING_SIZE = 10.4;
    static final double COMPETENCY_SIZE = 9.09;
    static final double MAIN_HEADING_SIZE = 12.7;
    static final double SUMMARY_SIZE = 9.91;
    static final double ENTRY_HEADLINE_SIZE = 8.90;
    static final double BULLET_SIZE = 8.16;
    static final double BAND_HEADING_SIZE = 9.6;
    static final double BAND_BODY_SIZE = 8.6;
    static final double BAND_DEGREE_SIZE = 10.55;
    static final double TAGLINE_SIZE = 8.09;

    static final double SUMMARY_LEADING = 5.37;
    static final double BULLET_LEADING = 1.0;
    static final double BAND_LEADING = 4.71;

    // -- tracking ----------------------------------------------------------

    /**
     * A space's advance as a fraction of the type size. Tracking is written as
     * spacer runs between the letters because a text style carries no
     * letter-spacing, and this is what turns a wanted em fraction into a
     * number of spaces.
     */
    static final double SPACE_ADVANCE_EM = 0.25;

    static final double NAME_TRACKING_EM = 0.045;
    static final double ROLE_TRACKING_EM = 0.43;
    static final double MAIN_HEADING_TRACKING_EM = 0.124;
    static final double SIDEBAR_HEADING_TRACKING_EM = 0.251;
    static final double BAND_HEADING_TRACKING_EM = 0.215;
    static final double TAGLINE_TRACKING_EM = 0.239;

    // -- palette -----------------------------------------------------------

    static final DocumentColor ACCENT = DocumentColor.rgb(6, 100, 112);
    static final DocumentColor ACCENT_DEEP = DocumentColor.rgb(0, 80, 95);
    static final DocumentColor RULE_STRONG = DocumentColor.rgb(0, 88, 104);
    static final DocumentColor RULE_SOFT = DocumentColor.rgb(119, 174, 182);
    static final DocumentColor RULE_PALE = DocumentColor.rgb(177, 204, 207);
    static final DocumentColor SEPARATOR = DocumentColor.rgb(54, 125, 135);
    static final DocumentColor DISPLAY_TEXT = DocumentColor.rgb(34, 36, 50);
    static final DocumentColor BODY_TEXT = DocumentColor.rgb(17, 17, 17);
    static final DocumentColor PAPER = DocumentColor.WHITE;

    // -- the mark ----------------------------------------------------------

    static final double LOGO_WIDTH = px(132);
    static final double LOGO_HEIGHT = px(105);
    static final double LOGO_TO_IDENTITY = px(39);
    static final double MARK_STROKE = px(3);

    /** The pulse crosses the heart below its centre, not through it. */
    static final double PULSE_DROP = px(6.5);

    /**
     * The pulse's box is its INK box: a path is mapped onto the size given by
     * its own bounds rather than its viewBox. Its height is the spike's, and
     * that is why the pulse is a path and not an icon — the drawing's line is
     * flatter than any heartbeat glyph, and an icon keeps its aspect.
     */
    static final double PULSE_BOX_WIDTH = px(172);
    static final double PULSE_BOX_HEIGHT = px(82);

    /**
     * A shape container clamps an over-wide child to the left exactly as it
     * top-clamps an over-tall one, so the pulse is pulled back by half its own
     * overflow.
     */
    static final double PULSE_SHIFT = -(PULSE_BOX_WIDTH - LOGO_WIDTH) / 2.0;

    // -- rules, badges and dots --------------------------------------------

    static final double DIVIDER_WIDTH = px(2);
    static final double FULL_RULE_THICKNESS = px(2.5);
    static final double HEADER_RULE_THICKNESS = px(2.5);
    static final double BAND_RULE_THICKNESS = px(3);
    static final double SIDEBAR_DASH_WIDTH = px(54);
    static final double SIDEBAR_DASH_THICKNESS = px(3);
    static final double SIDEBAR_CLOSING_RULE_WIDTH = px(216);
    static final double SIDEBAR_CLOSING_RULE_THICKNESS = px(3);
    static final double TAGLINE_RULE_HEIGHT = px(21);

    static final double BADGE_MAIN = px(58);
    static final double BADGE_BAND = px(51);
    static final double SECTION_GAP = px(17);
    static final double BAND_HEADER_GAP = px(18);
    static final double DOT_DIAMETER = px(6);
    static final double DOT_GAP = px(17);

    static final double SEPARATOR_HEIGHT = px(38);
    static final double SEPARATOR_THICKNESS = px(1.5);

    /**
     * A contact icon is twice the height of the value beside it, so it is the
     * icon that sets the line box: centring puts the icon on that box's middle
     * while the value still sits near its foot, and the pair reads as standing
     * at two levels. These two corrections put both back where the drawing has
     * them — the first raises the pair, the second returns the icon to the
     * value's own axis.
     */
    static final double CONTACT_ICON_DROP = -px(4);
    static final double CONTACT_PAIR_LIFT = px(4);

    /**
     * The gap asked of the type, not the gap that appears: an icon's box
     * carries its own bearing, so about half the drawn gap is requested and
     * the bearing makes up the rest.
     */
    static final double CONTACT_ICON_GAP = px(7.5);

    // -- the rhythm between blocks -----------------------------------------

    static final double MASTHEAD_TO_CONTACT = px(17);
    static final double CONTACT_TO_RULE = px(23);
    static final double RULE_TO_BODY = px(29);
    static final double NAME_TO_ROLE = px(0);
    static final double SIDEBAR_PAD_TOP = px(9);
    static final double DASH_TO_HEADING = px(14);
    static final double HEADING_TO_ITEMS = px(35);
    static final double COMPETENCY_GAP = px(29.2);
    static final double ITEMS_TO_CLOSING_RULE = px(50);
    static final double HEADER_TO_BODY = px(20);
    static final double SUMMARY_TO_EXPERIENCE = px(29);
    static final double HEADER_TO_ENTRY = px(14);
    static final double ENTRY_TO_BULLETS = px(13);
    static final double ENTRY_GAP = px(34);
    static final double BODY_TO_CLOSING_RULE = px(30);
    static final double CLOSING_RULE_TO_BAND = px(27);
    static final double BAND_HEADING_TO_UNDERLINE = px(9.5);
    static final double BAND_HEADER_TO_BODY = px(22);

    /**
     * The closing-band heading does not sit on its badge's centre line: the
     * stack is seated below it so the underline clears the badge's lower half
     * rather than cutting through it.
     */
    static final double BAND_HEADING_DROP = px(12);
    static final double BAND_LINE_GAP = px(10.5);
    static final double BAND_FACT_GAP = px(14.6);
    static final double CERT_ITEM_GAP = px(12.9);
    static final double BAND_TO_TAGLINE_RULE = px(22);
    static final double TAGLINE_RULE_TO_TEXT = px(1.6);

    /**
     * Bullet pitch is section spacing, not line spacing: a single-line
     * paragraph takes the font's own line height whatever leading it is given,
     * so the drawn pitch has to be the gap between paragraphs.
     */
    static final double BULLET_GAP = px(7.9);

    // -- helpers -----------------------------------------------------------

    static DocumentTextStyle style(FontName font, double size, DocumentColor color,
                                   DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .color(color)
                .decoration(decoration)
                .build();
    }

    /**
     * The size a spacer run is set at so that {@code runs} of them advance the
     * wanted fraction of an em.
     */
    static DocumentTextStyle spacerStyle(DocumentTextStyle textStyle, double trackingEm,
                                         int runs) {
        return textStyle.withSize(trackingEm * textStyle.size() / (runs * SPACE_ADVANCE_EM));
    }

    /**
     * How many spacer characters carry one gap.
     *
     * <p>A run's size sets the line's height as well as its advance, so a
     * single space wide enough for heavy tracking would make the line taller
     * than its own type. Splitting the gap across several smaller spaces keeps
     * every spacer below the text size.</p>
     */
    static int spacerRuns(double trackingEm) {
        return Math.max(1, (int) Math.ceil(trackingEm / SPACE_ADVANCE_EM));
    }

    /**
     * Spaces that advance about {@code width} points at {@code size}.
     *
     * <p>Deliberately not one resized space: a run's size sets the line's
     * height as well as its advance, so a single space wide enough for the gap
     * would make its line several times too tall. The count follows the size,
     * so changing the type scale moves the gap with it.</p>
     */
    static String gapRun(double width, double size) {
        int spaces = (int) Math.max(1, Math.round(width / (SPACE_ADVANCE_EM * size)));
        return " ".repeat(spaces);
    }

    /** A node name built from a heading, with the spacing taken out of it. */
    static String compact(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "");
    }
}
