package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.font.FontName;

/**
 * The measured geometry, palette and type scale of the Serif Headline CV.
 *
 * <p>Unlike the other ported sheets, almost nothing here is a literal. The
 * design was drawn on a 1024 x 1536 pixel grid, so every distance is that
 * grid's number put through {@link #h} or {@link #v}, and every vertical gap
 * is stated as the <em>ink</em> distance the drawing shows — the white
 * between a descender and the next cap — with {@link #gap} subtracting the
 * blank a line box carries above and below its letters. Copying the pixel
 * numbers straight in would have set every block a few points too far apart,
 * because a line box is taller than the letters in it.</p>
 *
 * <p>The vertical scale carries a compression factor of its own: the drawing
 * is proportionally taller than A4, so heights are scaled by
 * {@link #V_COMPRESS} on top of the width scale to bring the sheet back onto
 * the page.</p>
 */
final class SerifHeadlineStyles {

    private SerifHeadlineStyles() {
    }

    // -- the reference grid ----------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    /** The width of the grid the design was drawn on, in pixels. */
    static final double REFERENCE_WIDTH_PX = 1024.0;

    static final double H_SCALE = PAGE.width() / REFERENCE_WIDTH_PX;

    /** How much shorter the page is than the drawing, proportionally. */
    static final double V_COMPRESS = 0.8450;
    static final double V_SCALE = H_SCALE * V_COMPRESS;

    /** A horizontal distance from the drawing, in points. */
    static double h(double px) {
        return px * H_SCALE;
    }

    /** A vertical distance from the drawing, in points. */
    static double v(double px) {
        return px * V_SCALE;
    }

    // -- type metrics ----------------------------------------------------

    /** Cap height as a fraction of type size, for the text face. */
    static final double CAP_EM = 0.72;
    static final double ASCENDER_EM = 0.80;
    static final double DESCENDER_EM = 0.21;

    /** Cap height as a fraction of type size, for the display face. */
    static final double DISPLAY_CAP_EM = 0.766;

    /** How much narrower the small face runs than the body face. */
    static final double ADVANCE_MATCH = 0.95;

    /**
     * The blank a line box leaves under its letters: the descender space,
     * which no glyph in a line of capitals or of ordinary lowercase reaches.
     */
    static double blankBelow(double size) {
        return size * DESCENDER_EM;
    }

    /**
     * The blank a line box leaves above its letters: the leading it adds,
     * plus the ascender space above the cap line.
     */
    static double blankAbove(double size, double leading) {
        return size * ((leading - 1.0) + (ASCENDER_EM - CAP_EM));
    }

    /**
     * A vertical gap authored as the ink distance in the drawing, less the
     * blank the two line boxes on either side already contribute.
     *
     * @param inkPx            the white space the drawing shows, in grid pixels
     * @param blankAboveTheGap blank the box above already leaves below itself
     * @param blankBelowTheGap blank the box below already leaves above itself
     * @return the margin to author, never negative
     */
    static double gap(double inkPx, double blankAboveTheGap, double blankBelowTheGap) {
        return Math.max(0.0, v(inkPx) - blankAboveTheGap - blankBelowTheGap);
    }

    // -- the column grid -------------------------------------------------

    static final double MARGIN = h(54.5);
    static final double CONTENT_WIDTH = PAGE.width() - 2.0 * MARGIN;

    static final double MAIN_WEIGHT = 0.6448;
    static final double GUTTER_WEIGHT = 0.0721;
    static final double ASIDE_WEIGHT = 1.0 - MAIN_WEIGHT - GUTTER_WEIGHT;

    /** The row splits on the gutter's midline, where the divider runs. */
    static final double MAIN_CELL_WEIGHT = MAIN_WEIGHT + GUTTER_WEIGHT / 2.0;
    static final double ASIDE_CELL_WEIGHT = 1.0 - MAIN_CELL_WEIGHT;

    static final double MAIN_WIDTH = CONTENT_WIDTH * MAIN_WEIGHT;
    static final double GUTTER = CONTENT_WIDTH * GUTTER_WEIGHT;
    static final double ASIDE_WIDTH = CONTENT_WIDTH * ASIDE_WEIGHT;
    static final double HALF_GUTTER = GUTTER / 2.0;

    static final double IDENTITY_WEIGHT = 0.7803;
    static final double CONTACT_WEIGHT = 1.0 - IDENTITY_WEIGHT;
    static final double CONTACT_WIDTH = CONTENT_WIDTH * CONTACT_WEIGHT;

    /** The summary stops at the divider rather than running the page width. */
    static final double SUMMARY_WIDTH = CONTENT_WIDTH * (MAIN_WEIGHT + GUTTER_WEIGHT);
    static final double SUMMARY_RIGHT_INSET = CONTENT_WIDTH - SUMMARY_WIDTH;

    // -- type ------------------------------------------------------------

    static final FontName DISPLAY_FONT = FontName.VOLKHOV;
    static final FontName TEXT_FONT = FontName.LATO;

    static final double BODY_SIZE = h(10.0) / CAP_EM;
    static final double SMALL_SIZE = h(9.0) / CAP_EM * ADVANCE_MATCH;
    static final double HEADING_SIZE = h(12.0) / CAP_EM;
    static final double JOB_TITLE_SIZE = HEADING_SIZE;
    static final double ITEM_TITLE_SIZE = BODY_SIZE;
    static final double ROLE_SIZE = h(16.0) / CAP_EM;
    static final double NAME_SIZE = h(44.0) / DISPLAY_CAP_EM;

    static final double BODY_LEADING = 1.34;
    static final double SUMMARY_LEADING = 1.37;
    static final double TIGHT_LEADING = 1.30;
    static final double DISPLAY_LEADING = 1.0;

    static final double TRACKING_EM = 0.17;
    static final double SPACE_ADVANCE_EM = 0.25;

    /** The marker string: the dot and the space the design leaves after it. */
    static final String BULLET_MARKER = "•  ";

    static final double BLANK_BODY = blankBelow(BODY_SIZE);
    static final double BLANK_SMALL = blankBelow(SMALL_SIZE);
    static final double BLANK_TITLE = blankBelow(ITEM_TITLE_SIZE);
    static final double BLANK_CAPS_BELOW_HEADING = blankBelow(HEADING_SIZE);
    static final double BLANK_CAPS_ABOVE_HEADING = blankAbove(HEADING_SIZE, TIGHT_LEADING);

    // -- palette ---------------------------------------------------------

    static final DocumentColor INK = DocumentColor.rgb(26, 38, 62);
    static final DocumentColor ACCENT = DocumentColor.rgb(61, 81, 147);
    static final DocumentColor BODY = DocumentColor.rgb(72, 76, 89);
    static final DocumentColor GOLD = DocumentColor.rgb(165, 117, 42);
    static final DocumentColor RULE = DocumentColor.rgb(229, 230, 233);
    static final DocumentColor DIVIDER = DocumentColor.rgb(222, 224, 228);
    static final DocumentColor TRACK = DocumentColor.rgb(224, 226, 231);
    static final DocumentColor PLATE = DocumentColor.rgb(243, 244, 246);
    static final DocumentColor STUB_RULE = DocumentColor.rgb(181, 185, 196);

    // -- masthead --------------------------------------------------------

    static final double NAME_TO_ROLE = gap(25,
            blankBelow(NAME_SIZE), blankAbove(ROLE_SIZE, TIGHT_LEADING));
    static final double ROLE_TO_GOLD_RULE = gap(23, blankBelow(ROLE_SIZE), 0.0);
    static final double GOLD_RULE_WIDTH = h(79);
    static final double GOLD_RULE_THICKNESS = h(3);

    static final double CONTACT_ROW_HEIGHT = BODY_SIZE * BODY_LEADING;
    static final double CONTACT_ROW_GAP = Math.max(0.0, v(29.25) - CONTACT_ROW_HEIGHT);
    static final double CONTACT_TEXT_INSET = h(32);
    static final double CONTACT_GLYPH_BOX = h(16);

    static final double MASTHEAD_TO_SUMMARY =
            gap(31, 0.0, blankAbove(BODY_SIZE, SUMMARY_LEADING));
    static final double SUMMARY_TO_RULE = gap(30, BLANK_BODY, 0.0);
    static final double RULE_TO_BODY = gap(35, 0.0, BLANK_CAPS_ABOVE_HEADING);
    static final double RULE_THICKNESS = h(1.5);
    static final double HAIRLINE_THICKNESS = h(1.2);

    // -- section headings ------------------------------------------------

    static final double DASH_CLEARANCE = h(25);
    static final double DASH_WIDTH = h(22);
    static final double DASH_THICKNESS = h(3);
    static final double DASH_TO_TITLE = h(16);
    static final double TAIL_GAP = h(19);

    /**
     * Where the heading dash starts, measured from the left edge of whatever
     * surface it is drawn on — the page for a full-width band, the column for
     * one inside the grid.
     */
    static double dashOffset(double leftSurfaceDistance) {
        return DASH_CLEARANCE - leftSurfaceDistance;
    }

    /** Where the heading's letters start, on the same measure. */
    static double titleOffset(double leftSurfaceDistance) {
        return dashOffset(leftSurfaceDistance) + DASH_WIDTH + DASH_TO_TITLE;
    }

    static final double SECTION_TEXT_INSET = titleOffset(MARGIN);

    // -- experience ------------------------------------------------------

    static final double RAIL_THICKNESS = h(1.2);
    static final double RAIL_MARGIN_LEFT = SECTION_TEXT_INSET - RAIL_THICKNESS / 2.0;
    static final double ENTRY_WIDTH = MAIN_WIDTH - RAIL_MARGIN_LEFT;
    static final double ENTRY_TEXT_INSET = h(104 - 54) - RAIL_MARGIN_LEFT;
    static final double MARKER_DIAMETER = h(11);
    static final double ENTRY_HEAD_BAND_HEIGHT = MARKER_DIAMETER;

    /** How far the job title rises above the marker band it is set on. */
    static final double TITLE_OVERFLOW =
            (JOB_TITLE_SIZE * TIGHT_LEADING - ENTRY_HEAD_BAND_HEIGHT) / 2.0;

    /** The same, for the dates set at the other end of that band. */
    static final double DATE_OVERFLOW =
            (BODY_SIZE * TIGHT_LEADING - ENTRY_HEAD_BAND_HEIGHT) / 2.0;

    static final double HEADING_TO_ENTRY = gap(36, BLANK_CAPS_BELOW_HEADING, 0.0);
    static final double TITLE_TO_EMPLOYER = gap(17, 0.0, BLANK_SMALL);
    static final double EMPLOYER_TO_BULLETS = gap(23, BLANK_SMALL, BLANK_BODY);
    static final double BULLET_LEADING = v(20) / BODY_SIZE;
    static final double BULLET_ITEM_GAP =
            Math.max(0.0, v(25) - BODY_SIZE * BULLET_LEADING);
    static final double BULLETS_TO_SEPARATOR = gap(35, BLANK_BODY, 0.0);
    static final double SEPARATOR_TO_ENTRY = gap(32, 0.0, 0.0);

    // -- projects --------------------------------------------------------

    static final double EXPERIENCE_TO_PROJECTS =
            gap(59, BLANK_BODY, BLANK_CAPS_ABOVE_HEADING);
    static final double HEADING_TO_CARDS = gap(33, BLANK_CAPS_BELOW_HEADING, 0.0);
    static final double PROJECT_PLATE_DIAMETER = h(47);
    static final double PROJECT_GLYPH_TO_TEXT = h(61);

    /** The plate hangs a little left of its column, into the gutter. */
    static final double PLATE_HANG = h(-4);
    static final double GLYPH_CLEARANCE = h(10);

    /**
     * The space a band column leaves at its right edge, so its text stops
     * short of the hairline rather than running into it.
     *
     * <p>This is the one measure on the sheet that is not the published
     * design's: there, a column's text runs to the separator, and a line
     * that happens to fill the column touches it. The gutter matches
     * {@link #GLYPH_CLEARANCE}, the space the next column's mark already
     * keeps on the other side, so the rule sits centred in white.</p>
     */
    static final double COLUMN_TAIL_GUTTER = GLYPH_CLEARANCE;
    static final double CARD_TITLE_TO_TECH = gap(15, BLANK_TITLE, BLANK_SMALL);
    static final double CARD_TECH_TO_BODY = gap(12, BLANK_SMALL, BLANK_SMALL);
    static final double CARD_BODY_LEADING = v(18.5) / SMALL_SIZE;
    static final double PROJECT_SEPARATOR_HEIGHT = v(98);

    static final double BODY_BAND_TO_CERTIFICATIONS =
            gap(22, BLANK_BODY, BLANK_CAPS_ABOVE_HEADING);

    // -- education -------------------------------------------------------

    static final double HEADING_TO_EDUCATION = gap(34, BLANK_CAPS_BELOW_HEADING, 0.0);
    static final double DEGREE_TO_INSTITUTION = gap(14, BLANK_TITLE, BLANK_SMALL);
    static final double INSTITUTION_TO_YEARS = gap(14, BLANK_SMALL, BLANK_SMALL);
    static final double YEARS_TO_STUB = gap(20, BLANK_SMALL, 0.0);
    static final double STUB_TO_DEGREE = gap(20, 0.0, 0.0);
    static final double STUB_RULE_WIDTH = h(27);
    static final double STUB_RULE_THICKNESS = h(2);

    // -- skills ----------------------------------------------------------

    static final double EDUCATION_TO_SKILLS =
            gap(45, BLANK_SMALL, BLANK_CAPS_ABOVE_HEADING);
    static final double HEADING_TO_CAPTION = gap(32, BLANK_CAPS_BELOW_HEADING,
            blankAbove(SMALL_SIZE, TIGHT_LEADING));
    static final double CAPTION_TO_ROWS = gap(17, blankBelow(SMALL_SIZE), 0.0);
    static final double SKILL_ROW_HEIGHT = SMALL_SIZE * TIGHT_LEADING;
    static final double SKILL_ROW_GAP = Math.max(0.0, v(21.5) - SKILL_ROW_HEIGHT);
    static final double GROUP_GAP =
            gap(31, 0.0, blankAbove(SMALL_SIZE, TIGHT_LEADING));
    static final double TRACK_WIDTH = h(116);
    static final double TRACK_THICKNESS = h(3);
    static final double CAPTION_TO_SOFT_SKILLS =
            gap(14, blankBelow(SMALL_SIZE), BLANK_BODY);
    static final double SOFT_SKILL_LEADING = v(19) / BODY_SIZE;

    // -- certifications and achievements ---------------------------------

    static final double HEADING_TO_CERTIFICATIONS =
            gap(33, BLANK_CAPS_BELOW_HEADING, 0.0);
    static final double CERT_PLATE_DIAMETER = h(48);
    static final double CERT_GLYPH_TO_TEXT = h(58);
    static final double CERT_LINE_LEADING = v(19) / SMALL_SIZE;
    static final double CERT_SEPARATOR_HEIGHT = v(46);
    static final double CERTIFICATIONS_TO_ACHIEVEMENTS =
            gap(50, BLANK_SMALL, BLANK_CAPS_ABOVE_HEADING);

    static final double HEADING_TO_ACHIEVEMENTS =
            gap(39, BLANK_CAPS_BELOW_HEADING, 0.0);
    static final double ACHIEVEMENT_GLYPH_BOX = h(40);
    static final double ACHIEVEMENT_GLYPH_TO_TEXT = h(59);
    static final double ACHIEVEMENT_GLYPH_HANG = h(-2);
    static final double ACHIEVEMENT_TEXT_DROP = v(8);
    static final double ACHIEVEMENT_TITLE_TO_BODY = gap(16, BLANK_TITLE, BLANK_SMALL);
    static final double ACHIEVEMENT_BODY_LEADING = v(18) / SMALL_SIZE;
    static final double ACHIEVEMENT_SEPARATOR_HEIGHT = v(62);

    // -- tracked capitals ------------------------------------------------

    static final double CAP_ADVANCE_NARROW = 0.30;
    static final double CAP_ADVANCE_WIDE = 0.85;
    static final double CAP_ADVANCE_EM = 0.62;

    /** Headroom on the estimate, so a tail never runs under its heading. */
    static final double TRACKED_WIDTH_SAFETY = 1.03;

    /**
     * Writes text letter by letter with a sized space between each pair,
     * which is how this design gets its letter-spacing: a text style carries
     * no tracking, so the gap is a run of its own whose type size is chosen
     * to advance by {@link #TRACKING_EM} of the surrounding size.
     *
     * @param paragraph the paragraph being built
     * @param text      the text to space out
     * @param style     the style of the letters
     */
    static void tracked(ParagraphBuilder paragraph, String text, DocumentTextStyle style) {
        DocumentTextStyle spacer = style.withSize(TRACKING_EM * style.size() / SPACE_ADVANCE_EM);
        for (int i = 0; i < text.length(); i++) {
            paragraph.inlineText(String.valueOf(text.charAt(i)), style);
            if (i < text.length() - 1) {
                paragraph.inlineText(" ", spacer);
            }
        }
    }

    /**
     * How wide a run of tracked capitals will be, estimated from per-letter
     * advances rather than measured.
     *
     * <p>The heading rule that trails a title has to stop before the letters
     * start, and its width is authored — the engine offers no measurement at
     * compose time. Three advance classes are enough for capitals, and the
     * result is padded by {@link #TRACKED_WIDTH_SAFETY} so the estimate errs
     * towards a shorter rule.</p>
     *
     * @param text the capitals to measure
     * @param size the type size
     * @return the estimated width in points
     */
    static double trackedWidth(String text, double size) {
        if (text.isEmpty()) {
            return 0.0;
        }
        double glyphs = 0.0;
        for (int i = 0; i < text.length(); i++) {
            glyphs += capAdvanceEm(text.charAt(i));
        }
        return (glyphs * size + (text.length() - 1) * size * TRACKING_EM)
                * TRACKED_WIDTH_SAFETY;
    }

    private static double capAdvanceEm(char glyph) {
        return switch (glyph) {
            case 'I', 'J' -> CAP_ADVANCE_NARROW;
            case 'M', 'W' -> CAP_ADVANCE_WIDE;
            default -> CAP_ADVANCE_EM;
        };
    }

    // -- bands -----------------------------------------------------------

    /**
     * The margins that place one column of an N-column band, as insets on a
     * layer of the band's stack.
     *
     * @param bandWidth    the band's full width
     * @param columnWidth  one column's share of it
     * @param index        which column, from zero
     * @param dx           an extra offset for a mark that hangs left
     * @param naturalWidth the child's own width when it has one, else zero
     * @return the insets to set on that layer
     */
    static DocumentInsets columnInsets(double bandWidth, double columnWidth,
                                       int index, double dx, double naturalWidth) {
        return columnInsets(bandWidth, columnWidth, index, dx, naturalWidth, 0.0);
    }

    /**
     * The same, with a gutter kept at the column's right edge.
     *
     * @param bandWidth    the band's full width
     * @param columnWidth  one column's share of it
     * @param index        which column, from zero
     * @param dx           an extra offset for a mark that hangs left
     * @param naturalWidth the child's own width when it has one, else zero
     * @param tailGutter   space to leave at the right edge
     * @return the insets to set on that layer
     */
    static DocumentInsets columnInsets(double bandWidth, double columnWidth,
                                       int index, double dx, double naturalWidth,
                                       double tailGutter) {
        double left = index * columnWidth + dx;
        double right = naturalWidth > 0.0
                ? Math.max(0.0, bandWidth - left - naturalWidth)
                : Math.max(0.0, bandWidth - left - (columnWidth - dx) + tailGutter);
        return new DocumentInsets(0, right, 0, left);
    }

    // -- styles ----------------------------------------------------------

    static DocumentTextStyle body() {
        return style(BODY_SIZE, BODY, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle small() {
        return style(SMALL_SIZE, BODY, DocumentTextDecoration.DEFAULT);
    }

    static DocumentTextStyle style(double size, DocumentColor color,
                                   DocumentTextDecoration decoration) {
        return TextStyles.of(TEXT_FONT, size, decoration, color);
    }

    /** Strips a title down to the letters and digits a node name can carry. */
    static String compact(String text) {
        return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "");
    }
}
