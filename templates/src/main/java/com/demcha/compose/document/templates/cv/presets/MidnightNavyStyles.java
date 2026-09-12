package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * Design tokens for {@link MidnightNavy} — the palette, the type scale and the
 * geometry every part of the sheet measures against.
 *
 * <p>The design was drawn at 1054 px across an A4 page, so {@link #PX} converts
 * every length below from that raster to points: re-measuring the design at a
 * different resolution changes this one number rather than each constant that
 * came off it.</p>
 *
 * <p>Sizes are pinned by cap height rather than chosen, so a heading is as tall
 * as the design's heading whatever the face's own metrics say, and
 * {@link #leading} turns a measured line pitch into the additive value the
 * engine's {@code lineSpacing} actually is.</p>
 */
final class MidnightNavyStyles {

    private MidnightNavyStyles() {
    }

    static final DocumentPageSize PAGE = DocumentPageSize.A4;
    static final double PAGE_WIDTH = PAGE.width();

    /** The design's raster is 1054 px wide; every length below came off it. */
    static final double PX = PAGE_WIDTH / 1054.0;

    /** Measured: the navy plate runs from the left paper edge to x=327 inclusive. */
    static final double ASIDE_RATIO = 328.0 / 1054.0;
    static final double MAIN_RATIO = 1.0 - ASIDE_RATIO;

    static final double ASIDE_WIDTH = PAGE_WIDTH * ASIDE_RATIO;
    static final double MAIN_CELL_WIDTH = PAGE_WIDTH - ASIDE_WIDTH;

    static final double ASIDE_PAD_LEFT = px(52);
    static final double ASIDE_PAD_RIGHT = px(43);
    static final double ASIDE_PAD_TOP = px(54);
    static final double ASIDE_INNER_WIDTH = ASIDE_WIDTH - ASIDE_PAD_LEFT - ASIDE_PAD_RIGHT;

    /**
     * The inset every aside content block carries; the identity block does not.
     *
     * <p>The monogram is centred on the plate's full width rather than on the
     * text column, so the horizontal inset belongs to the four blocks under it
     * instead of to the column.</p>
     */
    static final DocumentInsets ASIDE_CONTENT_PAD =
            new DocumentInsets(0, ASIDE_PAD_RIGHT, 0, ASIDE_PAD_LEFT);

    static final double MAIN_PAD_LEFT = px(53);
    static final double MAIN_PAD_RIGHT = px(74);
    static final double MAIN_PAD_TOP = px(71.3);
    static final double MAIN_INNER_WIDTH = MAIN_CELL_WIDTH - MAIN_PAD_LEFT - MAIN_PAD_RIGHT;

    static final DocumentColor NAVY = DocumentColor.rgb(19, 34, 50);
    static final DocumentColor INK = DocumentColor.rgb(16, 27, 44);
    static final DocumentColor BODY = DocumentColor.rgb(51, 51, 51);
    static final DocumentColor MUTED = DocumentColor.rgb(63, 74, 90);
    static final DocumentColor RULE = DocumentColor.rgb(180, 186, 194);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(226, 227, 228);
    static final DocumentColor ASIDE_RULE = DocumentColor.rgb(107, 116, 128);
    static final DocumentColor ASIDE_DIVIDER = DocumentColor.rgb(195, 200, 206);
    static final DocumentColor ASIDE_DIM = DocumentColor.rgb(213, 219, 225);
    static final DocumentColor TRACK_DIM = DocumentColor.rgb(159, 166, 174);
    static final DocumentColor WHITE = DocumentColor.WHITE;

    static final FontName FACE = FontName.BARLOW;

    /** The face's cap band as a fraction of its type size, measured off a render. */
    private static final double FACE_CAP_RATIO = 0.700;

    /**
     * The gap between two language dots is spaces at the row's own type size,
     * so the count is derived from the measured pitch and the face's space
     * advance (0.200 em, read off the TTF) rather than tuned by eye. Setting the
     * space at a larger size instead would buy the same gap and a line box three
     * times too tall.
     */
    static final double FACE_SPACE_ADVANCE = 0.200;

    static final double NAME_SIZE = sizeForCap(23);
    static final double ROLE_SIZE = sizeForCap(10);
    static final double MONOGRAM_SIZE = sizeForCap(31);
    static final double ASIDE_HEAD_SIZE = sizeForCap(13);
    static final double ASIDE_TEXT_SIZE = sizeForCap(10);
    static final double ASIDE_SMALL_SIZE = sizeForCap(9);
    static final double EDUCATION_TITLE_SIZE = sizeForCap(11);
    static final double EDUCATION_YEARS_SIZE = sizeForCap(11);
    static final double MAIN_HEAD_SIZE = sizeForCap(14);
    static final double BODY_SIZE = sizeForCap(11);
    static final double JOB_TITLE_SIZE = sizeForCap(12);

    static final double MONOGRAM_DIAMETER = px(135);
    static final double MONOGRAM_STROKE = px(2.8);

    /** Measured: the first initial's cap top at 82 px, the second's at 129. */
    static final double MONOGRAM_PITCH = px(47);
    static final double NAME_DIVIDER_WIDTH = px(201);

    static final double ASIDE_RULE_THICKNESS = px(2);

    /** The contact rule is measurably shorter than the other three. */
    static final double CONTACT_RULE_WIDTH = px(100);

    static final double CONTACT_ICON_SIZE = 8.5;
    static final double CONTACT_ROW_GAP = px(13.5);

    static final double SKILL_TRACK_WIDTH = px(107);
    static final double SKILL_TRACK_THICKNESS = px(2);
    static final double SKILL_KNOB_WIDTH = px(9);
    static final double SKILL_KNOB_HEIGHT = px(6);
    static final double SKILL_ROW_GAP = px(11.3);

    static final int LANGUAGE_DOTS = 5;
    static final double LANGUAGE_DOT_DIAMETER = px(10);
    static final double LANGUAGE_DOT_PITCH = px(21.5);
    static final double LANGUAGE_GROUP_WIDTH =
            LANGUAGE_DOT_DIAMETER + (LANGUAGE_DOTS - 1) * LANGUAGE_DOT_PITCH;
    static final double LANGUAGE_ROW_GAP = px(19.9);
    static final double LANGUAGE_DOT_STROKE = px(1.4);

    static final double MAIN_RULE_THICKNESS = px(2);
    static final double ACHIEVEMENT_RULE_THICKNESS = px(1);
    static final double MAIN_SECTION_GAP = px(63);

    static final double RAIL_WIDTH = px(2.5);
    static final double RAIL_GUTTER = px(27);
    static final double MARKER_DIAMETER = px(9);
    static final double ENTRY_GAP = px(46);

    static final double ACHIEVEMENT_DISC_DIAMETER = px(54);
    static final double ACHIEVEMENT_ICON_SIZE = px(28);
    static final double ACHIEVEMENT_DISC_GAP = px(13);

    static final double CERT_DIVIDER_WIDTH = px(1.2);
    static final double CERT_DIVIDER_GAP = px(24);
    static final double CERT_MARKER_DIAMETER = px(6);

    /** The type size whose cap band is {@code capPixels} of the design tall. */
    static double sizeForCap(double capPixels) {
        return px(capPixels) / FACE_CAP_RATIO;
    }

    /**
     * A measured line pitch, as the additive value {@code lineSpacing} actually
     * is: {@code lineSpacing} adds points <em>between</em> lines and the line
     * box is 1.2 times the type size, so a measured leading converts as
     * {@code leading - 1.2 * size} — never as a ratio. It is deliberately not
     * clamped: this design sets three blocks tighter than their own line box,
     * and clamping would silently open them back up.
     */
    static double leading(double measuredPixels, double size) {
        return px(measuredPixels) - 1.2 * size;
    }

    static double px(double designPixels) {
        return designPixels * PX;
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

    static DocumentTextStyle asideText() {
        return style(ASIDE_TEXT_SIZE, WHITE, DocumentTextDecoration.DEFAULT);
    }
}
