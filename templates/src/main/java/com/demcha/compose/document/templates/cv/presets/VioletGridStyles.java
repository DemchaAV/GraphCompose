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
 * The measured geometry, palette and type scale of the Violet Grid CV.
 *
 * <h2>Every length is a measurement, converted once</h2>
 *
 * <p>The design was drawn on a 1055-pixel-wide raster, so each length here is
 * written as {@link #px} of a pixel measured off it. That keeps the source
 * auditable — a reader can put a ruler on the drawing and find the number —
 * and keeps one conversion factor rather than a page of pre-multiplied
 * decimals. Widths that are shares of the page derive from
 * {@link #CONTENT_WIDTH}; only genuinely independent marks (a disc's
 * diameter, a rule's thickness) are their own constants.</p>
 *
 * <h2>The type sizes are solved, not estimated</h2>
 *
 * <p>Each is the drawing's measured ink width for one specific string divided
 * by that string's width in ems in the face that sets it. An estimate from an
 * average advance came out several per cent small everywhere, which is enough
 * to move a line break — and a moved break is the difference a pixel gate can
 * see.</p>
 */
final class VioletGridStyles {

    private VioletGridStyles() {
    }

    // -- the grid ----------------------------------------------------------

    /** The drawing's raster width against A4's, in points per pixel. */
    static final double PX_TO_PT = 595.276 / 1055.0;

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    /**
     * One measurement off the drawing, in points.
     *
     * @param pixels the pixel measured on the 1055-wide raster
     * @return the same distance on the page
     */
    static double px(double pixels) {
        return pixels * PX_TO_PT;
    }

    /** The engine's line box as a multiple of the type size. */
    static final double LINE_FACTOR = 1.2;

    /**
     * A hundredth of a point of slack under every table. The engine compares a
     * table's declared width against its container's to the last bit, and a
     * sum of derived doubles lands a ten-thousandth of a point over — enough
     * to be refused.
     */
    static final double TABLE_SLACK = 0.01;

    /**
     * No cell borders. A table draws its rules per cell from that cell's own
     * style, and the default style has one; every table here is a layout
     * mechanism rather than a grid the reader should see, so each states the
     * absence explicitly.
     */
    static final DocumentStroke NO_BORDER =
            DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 0), 0.0);

    // -- palette -----------------------------------------------------------

    /** The violet everything but the family name is set in. */
    static final DocumentColor ACCENT = DocumentColor.rgb(118, 78, 175);

    /**
     * The lighter violet the family name takes. Two tones, not one: the
     * drawing sets them measurably apart, and antialiasing only lightens — so
     * the difference is a decision rather than noise.
     */
    static final DocumentColor ACCENT_LIGHT = DocumentColor.rgb(141, 105, 193);

    /** The hairline that follows every section heading — the accent at half. */
    static final DocumentColor ACCENT_RULE = DocumentColor.rgb(190, 172, 220);

    /** The tint behind the project tiles, the education badge and the band. */
    static final DocumentColor TINT = DocumentColor.rgb(237, 231, 245);

    static final DocumentColor INK = DocumentColor.rgb(28, 26, 32);
    static final DocumentColor BODY = DocumentColor.rgb(58, 56, 72);
    static final DocumentColor MUTED = DocumentColor.rgb(96, 94, 110);
    static final DocumentColor RULE = DocumentColor.rgb(219, 219, 219);
    static final DocumentColor RATING_EMPTY = DocumentColor.rgb(226, 226, 226);

    // -- type --------------------------------------------------------------

    /**
     * The display face: the name, the discipline line, the section headings
     * and the skill labels. Chosen by measurement: the drawing sets the name
     * at a width-to-cap ratio this face matches within a few per cent, where
     * the nearest-looking alternative is a quarter too wide.
     */
    static final FontName DISPLAY_FONT = FontName.ASAP_CONDENSED;

    /** The body face: everything the display face does not set. */
    static final FontName BODY_FONT = FontName.LATO;

    static final double NAME_SIZE = 46.55;
    static final double DISCIPLINE_SIZE = 17.73;
    static final double HEADING_SIZE = 12.90;

    /**
     * The skill labels, sized so the longest one clears its column's gutter.
     *
     * <p>Solved from the drawing's own ink widths the size comes out larger —
     * but at that size the longest label is wider than its column less the
     * gutter, and the binding constraint is the gutter rather than the ink. All
     * six are set at one size even though only one is constrained: six labels
     * of a set at five sizes is a worse document than six one step small.</p>
     */
    static final double LABEL_SIZE = 6.8;

    static final double CONTACT_SIZE = 8.93;
    static final double SUMMARY_SIZE = 9.31;
    static final double SKILL_DESC_SIZE = 6.35;
    static final double TOOL_SIZE = 8.11;
    static final double PERIOD_SIZE = 8.67;
    static final double ROLE_SIZE = 9.42;
    static final double EMPLOYER_SIZE = 8.09;
    static final double LOCATION_SIZE = 8.76;
    static final double HIGHLIGHT_SIZE = 8.02;
    static final double PROJECT_TITLE_SIZE = 9.62;
    static final double PROJECT_SUB_SIZE = 8.35;
    static final double PROJECT_YEAR_SIZE = 8.2;
    static final double PROJECT_BODY_SIZE = 8.47;
    static final double DEGREE_SIZE = 8.93;
    static final double INSTITUTION_SIZE = 8.96;
    static final double EDU_DETAIL_SIZE = 8.55;
    static final double LANGUAGE_SIZE = 8.49;
    static final double LEVEL_SIZE = 7.85;
    static final double QUOTE_SIZE = 8.64;

    // -- tracking ----------------------------------------------------------

    /**
     * A space's advance in the body face, as a fraction of the em.
     *
     * <p>Deliberately the BODY face's number even though tracking is only ever
     * applied to display text: the display face's space is narrow enough that
     * a gap of the wanted width would need a space run taller than the type it
     * separates, and an inline run sets the line box. The body face's space is
     * nearly twice as wide, so the same gap keeps the line box the type's — and
     * a space has no glyph to give the substitution away.</p>
     */
    static final double SPACE_RATIO = 0.256;

    /**
     * Tracking in points between one letter and the next. The headings' figure
     * is an average over all six: solved one at a time they range too widely
     * for any single typeface setting, because the drawing is a raster whose
     * glyph advances are not internally consistent — six per-heading constants
     * would be fitting noise rather than reproducing a design.
     */
    static final double DISCIPLINE_TRACKING = 3.15;
    static final double HEADING_TRACKING = 0.77;

    /** Gaps measured as constants whatever the words are, written as spaces. */
    static final String CONTACT_ICON_GAP = "     ";
    static final String TOOL_GAP = "      ";
    static final String PIPE = "   |   ";
    static final String RATING_GAP = "  ";
    static final String QUOTE_GAP = "      ";

    // -- the page ----------------------------------------------------------

    static final double PAGE_MARGIN_X = px(53);

    /**
     * Back-calculated rather than measured: the first ink on the page is a cap,
     * and a line box starts above its caps, so the margin is where the name's
     * box has to begin for its caps to land where the drawing puts them.
     */
    static final double PAGE_MARGIN_TOP = px(24);

    /**
     * Measured under the quote band, then taken in by three pixels. Nothing on
     * this page sits at the bottom margin except the closing band, and the
     * drawing puts its lower edge exactly ON the margin — so at the measured
     * value the flow has no slack and the band's last points spill onto a
     * second page. Three pixels of headroom is the difference between a
     * one-page CV and a two-page one.
     */
    static final double PAGE_MARGIN_BOTTOM = px(18);

    static final double CONTENT_WIDTH = 595.276 - 2.0 * PAGE_MARGIN_X;

    // -- the masthead ------------------------------------------------------

    /** Where the contact marks begin, which is where the masthead row splits. */
    static final double CONTACT_COLUMN_X = px(653);
    static final double CONTACT_TOP = px(8);
    static final double CONTACT_PITCH = px(34.3);
    static final double DISCIPLINE_TO_RULE = px(23.3);
    static final double IDENTITY_RULE_WIDTH = px(70);
    static final double IDENTITY_RULE_THICKNESS = px(4);

    /**
     * Measured ink to ink, then corrected by two pixels against the render:
     * the measurement is a cap-to-cap distance and what the engine wants is a
     * margin under a line BOX, which grew when the name's size was solved.
     */
    static final double HEADER_TO_SUMMARY = px(26);
    static final double SUMMARY_PITCH = px(22);

    // -- headings ----------------------------------------------------------

    static final double HEADING_TO_RULE = px(20);
    static final double HEADING_RULE_THICKNESS = px(1.5);

    /**
     * How far below the heading's line top the rule sits. The drawing centres
     * it on the cap band rather than on the line box, and the two differ by
     * the leading above the caps.
     */
    static final double HEADING_RULE_OFFSET = px(13.5);

    // -- the skills grid ---------------------------------------------------

    static final double SUMMARY_TO_SKILLS = px(33.8);
    static final double HEADING_TO_SKILLS = px(19.5);
    static final double ICON_TO_LABEL = px(17.4);
    static final double LABEL_TO_DESC = px(6);
    static final double SKILL_DESC_PITCH = px(18);
    static final double SKILL_DIVIDER_THICKNESS = px(2);

    /**
     * The gutter between a column's content and the separators either side.
     *
     * <p>The drawing gives its widest column more room than six equal columns
     * leave, so the label that just fits there is flush here. This is that
     * column's own narrowest gutter, applied to every column so the grid keeps
     * one rhythm.</p>
     */
    static final double SKILL_COLUMN_INSET = px(11);

    /**
     * The measure the descriptions wrap to, which is narrower than the column.
     *
     * <p>Solved from the drawing's own line breaks rather than measured off
     * it: greedy-wrapping all six descriptions, each column's breaks are
     * reproduced by an interval of measures, and this is the value inside the
     * most of them. No single measure reproduces all six — the drawing's own
     * metrics disagree with any one face — so five of six is the best
     * available.</p>
     */
    static final double SKILL_DESC_MEASURE = 72.5;

    /**
     * The dotted separator's height — the one length on this page that is not
     * derived from what it sits beside, and a known limit: a skill whose
     * description ran to four lines would outgrow its separator. The
     * alternative primitive stretches to the content but cannot be dashed, and
     * the dotted rule is a visible feature of the design.
     */
    static final double SKILL_RAIL_HEIGHT = px(136);
    static final double SKILL_DASH_ON = px(2);
    static final double SKILL_DASH_OFF = px(3.15);

    // -- the tools strip ---------------------------------------------------

    static final double SKILLS_TO_TOOLS = px(27.3);
    static final double HEADING_TO_TOOLS = px(18.7);
    static final double TOOL_DOT_DIAMETER = px(5.2);

    // -- the timeline ------------------------------------------------------

    static final double TOOLS_TO_EXPERIENCE = px(35.7);
    static final double HEADING_TO_ENTRIES = px(19.3);

    /** Where the rail stands, as an indent from the page margin. */
    static final double RAIL_INDENT = px(134.5);

    /** The gap between the rail and an entry's own content. */
    static final double ENTRY_INDENT = px(35.5);
    static final double RAIL_THICKNESS = px(2);
    static final double MARKER_DIAMETER = px(13.5);
    static final double ENTRY_WIDTH =
            CONTENT_WIDTH - RAIL_INDENT - ENTRY_INDENT - TABLE_SLACK;
    static final double LOCATION_COLUMN = px(200);
    static final double ROLE_COLUMN = ENTRY_WIDTH - LOCATION_COLUMN;

    /** The dates sit back at the page margin, outside the rail. */
    static final double DATE_OFFSET = -(RAIL_INDENT + ENTRY_INDENT);

    /** Half the disc, plus the indent, puts its centre on the border. */
    static final double MARKER_OFFSET = -(ENTRY_INDENT + MARKER_DIAMETER / 2.0);

    static final double BULLET_COLUMN = px(16);
    static final double BULLET_DOT_DIAMETER = px(6);
    static final double BULLET_PITCH = px(20.5);
    static final double TITLE_TO_BULLETS = px(6.1);
    static final double ENTRY_GAP = px(24);

    // -- the projects ------------------------------------------------------

    static final double EXPERIENCE_TO_PROJECTS = px(27.6);
    static final double HEADING_TO_PROJECTS = px(15.1);
    static final double PROJECT_GAP = px(17.4);
    static final double THUMB_COLUMN = px(138.5);
    static final double THUMB_PAD_LEFT = px(21);
    static final double THUMB_WIDTH = px(80);
    static final double THUMB_HEIGHT = px(76);
    static final double THUMB_RADIUS = px(10);
    static final double PROJECT_COPY_INDENT = px(34.5);
    static final double PROJECT_RULE_THICKNESS = px(2);
    static final double PROJECT_COPY_WIDTH =
            CONTENT_WIDTH - THUMB_COLUMN - PROJECT_COPY_INDENT - TABLE_SLACK;
    static final double PROJECT_YEAR_COLUMN = px(120);
    static final double PROJECT_TITLE_COLUMN = PROJECT_COPY_WIDTH - PROJECT_YEAR_COLUMN;
    static final double TITLE_TO_BODY = px(3.6);
    static final double PROJECT_BODY_PITCH = px(19.5);

    // -- the credentials band ----------------------------------------------

    static final double PROJECTS_TO_CREDENTIALS = px(15.7);
    static final double EDUCATION_COLUMN = px(430);
    static final double CREDENTIAL_GUTTER = px(70);
    static final double LANGUAGES_COLUMN =
            CONTENT_WIDTH - EDUCATION_COLUMN - CREDENTIAL_GUTTER - TABLE_SLACK;

    /** How the two heading rules split what the two heading texts leave. */
    static final double EDUCATION_RULE_WEIGHT = 335.0 / 680.0;
    static final double LANGUAGES_RULE_WEIGHT = 345.0 / 680.0;

    static final double HEADING_TO_EDUCATION = px(13.5);
    static final double BADGE_DIAMETER = px(72);
    static final double BADGE_COLUMN = px(91);
    static final double EDUCATION_TEXT_TOP = px(4.1);
    static final double DEGREE_TO_INSTITUTION = px(3);
    static final double INSTITUTION_TO_DETAIL = px(8.7);

    /**
     * The languages rows start a shade below where the education badge does.
     * The bodies row already carries the heading gap; this is only the
     * difference between the two halves.
     */
    static final double LANGUAGES_BODY_OFFSET = px(15.8 - 13.5);

    static final double LANGUAGE_NAME_COLUMN = px(162);
    static final double LANGUAGE_LEVEL_COLUMN = px(194);
    static final double LANGUAGE_RATING_COLUMN =
            LANGUAGES_COLUMN - LANGUAGE_NAME_COLUMN - LANGUAGE_LEVEL_COLUMN;
    static final double LANGUAGE_PITCH = px(27.5);
    static final double RATING_DOT_DIAMETER = px(10);

    /** How many discs a rating is drawn out of. */
    static final int RATING_SCALE = 5;

    // -- the closing band --------------------------------------------------

    static final double CREDENTIALS_TO_QUOTE = px(24);
    static final double QUOTE_RADIUS = px(6);
    static final double QUOTE_PAD_H = px(40);

    /**
     * The line box the band has to hold. The quotation mark is taller than the
     * type beside it, and an inline run taller than its paragraph's line box is
     * drawn clipped — so the box is the mark's height.
     */
    static final double QUOTE_LINE_BOX =
            Math.max(QUOTE_SIZE * LINE_FACTOR, VioletGridIcons.size(VioletGridIcons.QUOTE));

    /** The band's measured height less what it holds, halved. */
    static final double QUOTE_PAD_V = (px(48) - QUOTE_LINE_BOX) / 2.0;

    // -- helpers -----------------------------------------------------------

    static DocumentTextStyle style(FontName font, double size, DocumentColor color,
                                   boolean bold) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .color(color)
                .decoration(bold ? DocumentTextDecoration.BOLD : DocumentTextDecoration.DEFAULT)
                .build();
    }

    /** A cell's padding and how its content sits in it. */
    static DocumentTableStyle cellStyle(DocumentInsets padding,
                                        DocumentTableTextAnchor anchor) {
        return DocumentTableStyle.builder()
                .padding(padding)
                .textAnchor(anchor)
                .stroke(NO_BORDER)
                .build();
    }

    /** A measured pitch, minus the content it has to clear, is the gap that produces it. */
    static double gap(double pitch, double contentHeight) {
        return Math.max(0.0, pitch - contentHeight);
    }

    /**
     * A measured pitch as the additive value {@code lineSpacing} actually is:
     * the line box is {@link #LINE_FACTOR} times the type size and the setting
     * adds points on top of it.
     */
    static double leading(double pitch, double size) {
        return Math.max(0.0, pitch - LINE_FACTOR * size);
    }

    /** How far to drop a mark of this height to centre it on a title's line box. */
    static double centred(double markHeight) {
        return Math.max(0.0, (ROLE_SIZE * LINE_FACTOR - markHeight) / 2.0);
    }
}
