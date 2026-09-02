package com.demcha.compose.document.templates.cv.presets;

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
 * Design tokens for {@link OrangeOps} — the palette, the type scale, and the
 * geometry every part of the sheet measures against.
 *
 * <h2>One scale, not forty measurements</h2>
 *
 * <p>The design was drawn at 1055 px across an A4 page, so {@link #PX} converts
 * every length below from that raster to points. Re-measuring the design at a
 * different resolution changes this one number rather than each constant that
 * came off it.</p>
 *
 * <h2>Ink gaps and box gaps are different things</h2>
 *
 * <p>A vertical distance read off a design is between two pieces of
 * <em>ink</em> — the foot of one cap band to the top of the next. Every
 * vertical distance the engine accepts is between two <em>boxes</em>, and a
 * line box reaches above its cap and below its baseline. {@link Face} holds the
 * four fractions that separate the two, and {@link #boxGap} is the only place
 * the conversion happens.</p>
 */
final class OrangeOpsStyles {

    private OrangeOpsStyles() {
    }

    /**
     * What one line of a face occupies, as fractions of the type size.
     *
     * <p>The numbers are measured from rendered output rather than read out of
     * the font tables: the engine derives its line box from the face's ascent
     * and descent, which for a condensed display face is close to 1.5 times the
     * type size and not the 1.2 a body face gives.</p>
     *
     * @param lineFactor box height, as a multiple of the type size
     * @param capTop     box top to cap top
     * @param capHeight  the cap band itself
     * @param capBottom  baseline to box bottom
     */
    record Face(double lineFactor, double capTop, double capHeight, double capBottom) {

        double lineBox(double size) {
            return lineFactor * size;
        }

        double capTop(double size) {
            return capTop * size;
        }

        double capBottom(double size) {
            return capBottom * size;
        }

        /** The type size whose cap band is {@code capHeightPoints} tall. */
        double sizeForCap(double capHeightPoints) {
            return capHeightPoints / capHeight;
        }
    }

    static final DocumentPageSize PAGE = DocumentPageSize.A4;
    static final double PAGE_WIDTH = PAGE.width();

    /** The design's raster is 1055 px wide; every length below came off it. */
    static final double PX = PAGE_WIDTH / 1055.0;

    static final FontName BODY_FONT = FontName.LATO;

    /**
     * The display family, named but not carried.
     *
     * <p>Oswald is not one of the families {@code graph-compose-fonts} ships,
     * so a caller registers it on the session before composing — see the class
     * documentation on {@link OrangeOps}, which republishes this name as its
     * own public constant so a caller has something to register against.</p>
     */
    static final FontName DISPLAY_FONT = FontName.of("Oswald");

    static final Face LATO = new Face(1.200, 0.2705, 0.7165, 0.2130);
    static final Face DISPLAY = new Face(1.482, 0.3670, 0.8460, 0.2690);

    /* Sized so the display face's cap band matches the design's. */
    static final double NAME_SIZE = DISPLAY.sizeForCap(79 * PX);
    static final double HEADING_SIZE = DISPLAY.sizeForCap(17 * PX);
    static final double ROLE_SIZE = LATO.sizeForCap(14 * PX);

    /*
     * The body sizes are fitted to measured string widths, not to cap heights.
     *
     * A cap height pins the display face, whose job is to be as tall as the
     * design's. Running text has a different job: it has to break where the
     * design breaks. Sizing the body faces from cap heights instead put every
     * one of them about 7% too wide, wrapped an extra line in four places, and
     * pushed the body onto a second page.
     */
    static final double BODY_SIZE = 8.2;
    static final double HEADING_SUFFIX_SIZE = 8.8;
    static final double CONTACT_SIZE = 8.2;

    /*
     * An achievement's title is display face, not body face: measured against
     * the design it wants 13 px of cap and 224 px of width, and in a body face
     * those two ask for sizes 1.4 apart — which is what a condensed face looks
     * like when it is measured as though it were not one.
     */
    static final double ACHIEVEMENT_TITLE_SIZE = DISPLAY.sizeForCap(13 * PX);
    static final double ACHIEVEMENT_BODY_SIZE = 8.4;
    static final double EDUCATION_TITLE_SIZE = 8.4;
    static final double EDUCATION_LINE_SIZE = 8.0;
    static final double CERT_TITLE_SIZE = 8.2;
    static final double CERT_ISSUER_SIZE = 6.8;
    static final double JOB_TITLE_SIZE = 9.3;
    static final double JOB_META_SIZE = 8.5;
    static final double METRIC_VALUE_SIZE = LATO.sizeForCap(23 * PX);
    static final double METRIC_CAPTION_SIZE = 8.6;

    static final double MARGIN_X = 41 * PX;

    /**
     * Zero, and measured rather than chosen.
     *
     * <p>The design's name cap starts 30 px down the page. A paragraph's box
     * begins {@link Face#capTop} above its cap band, and for the display face at
     * {@link #NAME_SIZE} that alone is more than 30 px — so no positive top
     * margin puts the cap where the design has it, and zero is as close as the
     * page can get.</p>
     */
    static final double MARGIN_TOP = 0.0;

    /**
     * The design's last line of ink sits 34 px above the paper's edge; the box
     * that line lives in reaches lower than the ink does, so the margin is that
     * measurement less the descender it has to clear.
     */
    static final double MARGIN_BOTTOM = 34 * PX - LATO.capBottom(BODY_SIZE);

    static final double CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_X;
    static final double ASIDE_WIDTH = 309 * PX;
    static final double GUTTER = 63 * PX;
    static final double MAIN_WIDTH = CONTENT_WIDTH - ASIDE_WIDTH - GUTTER;

    /**
     * The body row spans the whole page, because the page margins are zero and
     * each column carries its own padding. The split falls in the middle of the
     * gutter, which is what puts the main cell's rule on the divider the design
     * shows.
     */
    static final double ASIDE_CELL_WEIGHT = (MARGIN_X + ASIDE_WIDTH + GUTTER / 2) / PAGE_WIDTH;
    static final double MAIN_CELL_WEIGHT = 1.0 - ASIDE_CELL_WEIGHT;

    static final double ROLE_BAR_HEIGHT = 39 * PX;
    static final double ROLE_BAR_WIDTH = 561 * PX;

    /**
     * The lean every edge on the role band shares.
     *
     * <p>Tracked row by row down the design, the plate's right edge and all
     * three slashes move left by the same 24 px across the 35 rows between the
     * band's second row and its second from last. One ratio, four edges — so
     * the plate's slant and each slash's foot are derived from it rather than
     * measured one at a time. Measuring them separately put four different
     * leans on the band and made the slashes visibly diverge from the plate.</p>
     */
    static final double SLANT_RATIO = 24.0 / 35.0;
    static final double ROLE_BAR_SLANT = SLANT_RATIO * ROLE_BAR_HEIGHT;

    /**
     * Each slash as {left edge of its top, width}, in design pixels relative to
     * {@link #SLASH_BLOCK_X}. The foot is not listed because it is not a
     * measurement: it is the top less {@link #ROLE_BAR_SLANT}.
     */
    static final double[][] SLASHES = {{26.7, 19}, {53.7, 19}, {80.7, 35}};
    static final double SLASH_BLOCK_X = 545 * PX;
    static final double SLASH_BLOCK_WIDTH = 116 * PX;

    /**
     * The one gap allowed to be negative.
     *
     * <p>The design leaves 18 px between the name's cap band and the top of the
     * role bar, and the display face's box reaches further below its cap band
     * than that — so the bar starts inside the name's box, and a clamped
     * conversion cannot say so. Clamped, it put the bar 11 px low and, with it,
     * everything down the page.</p>
     */
    static final double NAME_TO_BAR = 18 * PX - DISPLAY.capBottom(NAME_SIZE);

    static final double CONTACT_SEPARATOR_HEIGHT = 20 * PX;
    static final double ROLE_TEXT_INSET = MARGIN_X;

    static final double RULE_THICKNESS = 0.9;
    static final double ACCENT_RULE_THICKNESS = 3 * PX;
    static final double COLUMN_RULE_THICKNESS = 3 * PX;
    static final double MAIN_HEADING_RULE_WIDTH = 110 * PX;

    /*
     * Neither column spaces its joins the same way twice. The aside goes 26/20
     * px, then 22/15, then 18/13; the main column 28/21, then 24/16, then
     * 15/14. Both tighten as their column fills, which is what a designer does
     * to balance two columns of different weight, so each join is spaced as the
     * design spaces it rather than all of them from the first. Spacing them
     * uniformly put the last aside block 24 px low.
     *
     * Each row is {above, below} in design pixels, top join first. A column
     * with more blocks than the design's four reuses the last row, which is the
     * tightest.
     */
    static final double[][] ASIDE_JOIN_INK = {{26, 20}, {22, 15}, {18, 13}};
    static final double[][] MAIN_JOIN_INK = {{28, 21}, {24, 16}, {15, 14}};

    static final double SKILL_PITCH = 22 * PX;
    static final double ACHIEVEMENT_BODY_PITCH = 18 * PX;
    static final double EDUCATION_LINE_PITCH = 21 * PX;
    static final double CERT_PITCH = 46 * PX;
    static final double PROFILE_PITCH = 20.6 * PX;
    static final double BULLET_PITCH = 18 * PX;
    static final double BULLET_ITEM_PITCH = 24 * PX;
    static final double ADDITIONAL_PITCH = 24 * PX;

    static final double ACHIEVEMENT_CIRCLE = 58 * PX;
    static final double EDUCATION_CIRCLE = 52 * PX;
    static final double ACHIEVEMENT_ICON_COLUMN = 74 * PX;

    /**
     * The achievement cards stop short of the aside's right edge.
     *
     * <p>No body line in any of the design's four cards passes 341 px, and the
     * second line of the first card wraps at a word that would have taken it to
     * 345 — so the column ends there rather than at the aside's own 350. Given
     * the full width the cards fit one more word per line and every one of them
     * comes out a line short, which is where the whole of the lower aside's
     * drift came from.</p>
     */
    static final double ACHIEVEMENT_TEXT_INSET = 9 * PX;
    static final double EDUCATION_ICON_COLUMN = 69 * PX;
    static final double CERT_TEXT_INSET = 16 * PX;
    static final double BULLET_SIZE = 5 * PX;
    static final double DATE_COLUMN_RATIO = 0.30;
    static final double METRIC_SEPARATOR = 0.6;

    /** The design's glyph is a little under half the disc it sits in. */
    static final double BADGE_GLYPH_SHARE = 0.48;

    static final DocumentColor ACCENT = DocumentColor.rgb(0xEF, 0x5B, 0x03);
    static final DocumentColor INK = DocumentColor.rgb(0x1E, 0x24, 0x2D);
    static final DocumentColor BODY = DocumentColor.rgb(0x33, 0x3A, 0x42);
    static final DocumentColor MUTED = DocumentColor.rgb(0x55, 0x59, 0x5F);
    static final DocumentColor RULE = DocumentColor.rgb(0xC9, 0xCB, 0xCD);
    static final DocumentColor ON_DARK = DocumentColor.rgb(0xFF, 0xFF, 0xFF);

    /**
     * A table's rules are drawn per cell from that cell's own stroke, and the
     * default style has one. Every table on this page is a layout device rather
     * than a ruled grid, so every cell style starts by turning it off.
     */
    static final DocumentStroke NO_STROKE = DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 0), 0.0);

    /* Gaps shared by more than one part of the sheet. */

    static final double BAR_TO_CONTACT = boxGap(18, null, 0, LATO, CONTACT_SIZE);

    /** The design's last contact line descends, so its 29 px is measured from that. */
    static final double CONTACT_TO_BODY =
            boxGap(29, LATO, CONTACT_SIZE, true, DISPLAY, HEADING_SIZE);

    static final double HEADING_TO_RULE = boxGap(8, DISPLAY, HEADING_SIZE, null, 0);

    /**
     * How far a block's first line sits below its accent rule.
     *
     * <p>The aside's four blocks agree closely enough to share one value — 15,
     * 17, 11 and 13 px — but the main column's four do not, and the reason is
     * what sits on the first line. Where it is running text the design leaves
     * 12 px to the cap; where it is a job title, 16; where it is a row of icons,
     * 3, because an icon has no cap band to clear. One shared constant put the
     * metric strip and the closing block 8 px low each.</p>
     */
    static final double RULE_TO_BODY = boxGap(15, null, 0, LATO, BODY_SIZE);
    static final double PROFILE_RULE_TO_BODY = boxGap(12, null, 0, LATO, BODY_SIZE);
    static final double EXPERIENCE_RULE_TO_BODY = boxGap(16, null, 0, LATO, JOB_TITLE_SIZE);
    static final double METRIC_RULE_TO_BODY = boxGap(3, null, 0, null, 0);
    static final double ADDITIONAL_RULE_TO_BODY = boxGap(8, null, 0, null, 0);

    static final double ACHIEVEMENT_TITLE_TO_BODY =
            boxGap(9, DISPLAY, ACHIEVEMENT_TITLE_SIZE, LATO, ACHIEVEMENT_BODY_SIZE);

    /** A card's body ends on a full sentence, which descends either way. */
    static final double ACHIEVEMENT_GAP =
            boxGap(19, LATO, ACHIEVEMENT_BODY_SIZE, true, DISPLAY, ACHIEVEMENT_TITLE_SIZE);

    static final double CERT_TITLE_TO_ISSUER =
            boxGap(6, LATO, CERT_TITLE_SIZE, true, LATO, CERT_ISSUER_SIZE);

    /** What is left of a certification's measured pitch once its two lines are in it. */
    static final double CERT_GAP = Math.max(0.0, CERT_PITCH
            - LATO.lineBox(CERT_TITLE_SIZE)
            - CERT_TITLE_TO_ISSUER
            - LATO.lineBox(CERT_ISSUER_SIZE));

    /*
     * 7 px, not the 9 px average: the design's three instances are 7, 9 and 10,
     * but only the first job's title ends in a descender and the other two are
     * measured from a baseline. Converting the descender instance is the one
     * that gives the box gap all three share.
     */
    static final double JOB_TITLE_TO_COMPANY =
            boxGap(7, LATO, JOB_TITLE_SIZE, true, LATO, JOB_META_SIZE);
    static final double COMPANY_TO_BULLETS =
            boxGap(13, LATO, JOB_META_SIZE, true, LATO, BODY_SIZE);
    static final double JOB_GAP = boxGap(27, LATO, BODY_SIZE, true, LATO, JOB_TITLE_SIZE);

    static final double METRIC_ICON_TO_VALUE = boxGap(11, null, 0, LATO, METRIC_VALUE_SIZE);
    static final double METRIC_VALUE_TO_CAPTION =
            boxGap(9, LATO, METRIC_VALUE_SIZE, LATO, METRIC_CAPTION_SIZE);

    static DocumentTextStyle style(FontName font, double size, DocumentColor color, boolean bold) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .color(color)
                .decoration(bold ? DocumentTextDecoration.BOLD : DocumentTextDecoration.DEFAULT)
                .build();
    }

    static DocumentTableStyle cellStyle(DocumentInsets padding, DocumentTableTextAnchor anchor) {
        return DocumentTableStyle.builder()
                .padding(padding)
                .textAnchor(anchor)
                .stroke(NO_STROKE)
                .build();
    }

    /**
     * A gap the design shows between two pieces of ink, as the gap between the
     * two boxes that produce it.
     *
     * @param inkPixels the measured distance, in design pixels
     * @param above     the face of the line above, or {@code null} for a rule or a shape
     * @param sizeAbove its type size
     * @param below     the face of the line below, or {@code null} for a rule or a shape
     * @param sizeBelow its type size
     * @return the margin to declare, never negative
     */
    static double boxGap(double inkPixels, Face above, double sizeAbove,
                         Face below, double sizeBelow) {
        return boxGap(inkPixels, above, sizeAbove, false, below, sizeBelow);
    }

    /**
     * The same conversion, told whether the line above ends in a descender.
     *
     * <p>Subtracting the baseline-to-box depth of the line above assumes its ink
     * stops at the baseline. A line ending in a p or a g does not: its ink
     * already reaches the box, and subtracting that depth a second time takes
     * the gap 3 to 5 px too tight. Three such gaps inside one experience entry
     * cost 13 px, and three entries put the whole lower main column 42 px
     * high.</p>
     *
     * @param inkPixels      the measured distance, in design pixels
     * @param above          the face of the line above, or {@code null}
     * @param sizeAbove      that line's type size
     * @param descenderAbove whether the design's own text on the line above
     *                       descends past its baseline
     * @param below          the face of the line below, or {@code null}
     * @param sizeBelow      that line's type size
     * @return the margin to declare, never negative
     */
    static double boxGap(double inkPixels, Face above, double sizeAbove, boolean descenderAbove,
                         Face below, double sizeBelow) {
        double ink = inkPixels * PX;
        double descent = (above == null || descenderAbove) ? 0.0 : above.capBottom(sizeAbove);
        double ascent = below == null ? 0.0 : below.capTop(sizeBelow);
        return Math.max(0.0, ink - descent - ascent);
    }

    /** A measured pitch, less the content it has to clear, is the gap that produces it. */
    static double gap(double pitch, double contentHeight) {
        return Math.max(0.0, pitch - contentHeight);
    }

    /**
     * A measured leading, as the additive value {@code lineSpacing} actually is:
     * the engine's line box is the face's line factor times the type size and
     * {@code lineSpacing} adds points on top of it, so a CSS-style multiple
     * would be silently wrong.
     */
    static double leading(double measuredLeading, double size) {
        return Math.max(0.0, measuredLeading - LATO.lineBox(size));
    }

    /** Rounds a point value for an SVG user-space coordinate string. */
    static double px(double points) {
        return Math.round(points * 100.0) / 100.0;
    }
}
