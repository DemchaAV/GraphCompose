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
 * The measured geometry, palette and type scale of the Slate Orange CV.
 *
 * <h2>Everything horizontal derives from four numbers</h2>
 *
 * <p>{@link #PAGE_WIDTH}, {@link #PAGE_MARGIN}, {@link #SIDEBAR_WEIGHT} and
 * {@link #COLUMN_GAP}: the page-background bands, the body row's weights and
 * every table's column widths read the same four, so a fill and the content
 * sitting on it cannot drift apart. Only genuinely independent marks — a rule
 * thickness, a marker diameter — are written as points.</p>
 *
 * <h2>Vertical pitches are measured, then converted</h2>
 *
 * <p>The engine's line box is {@link #LINE_FACTOR} times the type size and
 * {@code lineSpacing} adds points on top of it, so a measured pitch minus the
 * line it has to clear is the gap that produces it — {@link #gap} and
 * {@link #leading} are that conversion. A CSS-style multiple would be
 * silently wrong.</p>
 */
final class SlateOrangeStyles {

    private SlateOrangeStyles() {
    }

    // -- type --------------------------------------------------------------

    /** The condensed display face: name, monogram and every section heading. */
    static final FontName DISPLAY_FONT = FontName.ASAP_CONDENSED;

    /**
     * The body face: everything the display face does not set.
     *
     * <p>Condensed, and chosen by measurement rather than by eye. Against the
     * design on three separate strings — a summary line, an experience bullet
     * and a competency label — this face averages within one per cent, where
     * the face its letterforms most resemble runs a tenth to a quarter too
     * wide on all three. That is the difference between a bullet on one line
     * and a bullet on two.</p>
     */
    static final FontName BODY_FONT = FontName.FIRA_SANS_CONDENSED;

    static final double MONOGRAM_SIZE = 50.0;
    static final double NAME_SIZE = 40.5;
    static final double ROLE_LINE_SIZE = 16.5;
    static final double TAGLINE_SIZE = 10.5;
    static final double CONTACT_SIZE = 9.6;
    static final double HEADING_SIZE = 12.5;

    /**
     * The body size, set from the design's line WIDTH rather than its ink
     * height, because the two disagree here: matched on height the summary ran
     * to six lines where the design has five and every line broke on a
     * different word. Matching the width sets the type a few per cent small
     * and puts every break back where the design has it.
     */
    static final double BODY_SIZE = 9.4;

    /**
     * The sidebar label size, set the same way and then backed off a quarter
     * point: the longest additional-information label needed all but four
     * points of its column and the engine broke it anyway. One size has to
     * hold both sidebar blocks, because the design sets both at one size.
     */
    static final double LABEL_SIZE = 8.65;

    static final double ACHIEVEMENT_TITLE_SIZE = 9.4;
    static final double ACHIEVEMENT_BODY_SIZE = 8.3;
    static final double LANGUAGE_SIZE = 8.8;
    static final double LEVEL_SIZE = 6.2;
    static final double ROLE_SIZE = 10.35;
    static final double EMPLOYER_SIZE = 9.1;
    static final double PERIOD_SIZE = 8.1;
    static final double HIGHLIGHT_SIZE = 9.05;
    static final double DEGREE_SIZE = 8.7;
    static final double EDUCATION_LINE_SIZE = 8.6;
    static final double CREDENTIAL_SIZE = 7.25;

    /**
     * The credential headings, which the design sets smaller than the ones
     * above them. Not an oversight to be tidied into {@link #HEADING_SIZE}:
     * the two credential columns are a footer rather than a third and fourth
     * section of the page, and the design's caps measure smaller to say so.
     */
    static final double CREDENTIAL_HEADING_SIZE = 10.0;

    /** The engine's line box as a multiple of the type size. */
    static final double LINE_FACTOR = 1.2;

    // -- palette -----------------------------------------------------------

    static final DocumentColor SLATE = DocumentColor.rgb(34, 46, 56);
    static final DocumentColor ACCENT = DocumentColor.rgb(194, 96, 72);
    static final DocumentColor INK = DocumentColor.rgb(46, 57, 68);
    static final DocumentColor MASTHEAD_INK = DocumentColor.WHITE;
    static final DocumentColor MASTHEAD_MUTED = DocumentColor.rgb(216, 220, 224);
    static final DocumentColor MUTED = DocumentColor.rgb(138, 144, 153);
    static final DocumentColor RULE = DocumentColor.rgb(200, 200, 202);
    static final DocumentColor RULE_FAINT = DocumentColor.rgb(227, 227, 227);
    static final DocumentColor RATING_EMPTY = DocumentColor.rgb(202, 205, 207);

    // -- the page ----------------------------------------------------------

    static final double PAGE_WIDTH = 595.276;
    static final double PAGE_HEIGHT = 841.89;
    static final DocumentPageSize PAGE = DocumentPageSize.A4;
    static final double PAGE_MARGIN = 22.0;
    static final double CONTENT_WIDTH = PAGE_WIDTH - 2.0 * PAGE_MARGIN;

    static final double MASTHEAD_HEIGHT = 136.6;
    static final double TILE_WIDTH = 85.8;
    static final double COLUMN_GAP = 45.7;
    static final double SIDEBAR_WEIGHT = 0.31514;
    static final double MAIN_WEIGHT = 1.0 - SIDEBAR_WEIGHT;
    static final double COLUMNS_WIDTH = CONTENT_WIDTH - COLUMN_GAP;
    static final double SIDEBAR_WIDTH = COLUMNS_WIDTH * SIDEBAR_WEIGHT;
    static final double MAIN_WIDTH = COLUMNS_WIDTH * MAIN_WEIGHT;

    /**
     * The body row carries no padding and no spacing of its own: each column
     * pads itself and half the gap goes to each side, so the page margin and
     * the gap live inside the cells and the cell weights are what is left once
     * they are added back on.
     */
    static final double HALF_GAP = COLUMN_GAP / 2.0;
    static final double SIDEBAR_CELL = SIDEBAR_WIDTH + PAGE_MARGIN + HALF_GAP;
    static final double MAIN_CELL = MAIN_WIDTH + PAGE_MARGIN + HALF_GAP;
    static final double SIDEBAR_CELL_WEIGHT = SIDEBAR_CELL / PAGE_WIDTH;
    static final double MAIN_CELL_WEIGHT = MAIN_CELL / PAGE_WIDTH;

    // -- rules and marks ---------------------------------------------------

    static final double RULE_THICKNESS = 0.6;
    static final double HAIRLINE_THICKNESS = 1.2;
    static final double MONOGRAM_RULE_THICKNESS = 1.7;
    static final double MARKER_DIAMETER = 5.6;
    static final double RATING_DOT_DIAMETER = 5.6;

    /**
     * A hundredth of a point of slack under every table. The engine compares a
     * table's declared width against its container's to the last bit, and a
     * sum of derived doubles lands a ten-thousandth of a point over — enough
     * to be refused. Taking it off the last column leaves every other column
     * exactly where the measurement put it.
     */
    static final double TABLE_SLACK = 0.01;

    /**
     * No cell borders.
     *
     * <p>A table draws its rules per cell from that cell's own style, and the
     * default style has one. The tables here are a layout mechanism rather
     * than a grid the reader should see, so every cell states the absence
     * explicitly: a zero-width, fully transparent stroke.</p>
     */
    static final DocumentStroke NO_BORDER =
            DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 0), 0.0);

    // -- the masthead ------------------------------------------------------

    static final double HAIRLINE_X = 383.7;
    static final double IDENTITY_WIDTH = HAIRLINE_X - TILE_WIDTH;
    static final double HAIRLINE_TOP = 34.4;
    static final double HAIRLINE_HEIGHT = 79.0;
    static final double HAIRLINE_BOTTOM = MASTHEAD_HEIGHT - HAIRLINE_TOP - HAIRLINE_HEIGHT;
    static final double MONOGRAM_TOP = 35.8;
    static final double MONOGRAM_RULE_WIDTH = 37.2;
    static final double INITIALS_TO_RULE = 1.7;
    static final double IDENTITY_TOP = 27.4;
    static final double NAME_TO_ROLE = 1.5;
    static final double ROLE_TO_TAGLINE = 7.5;
    static final double CONTACT_TOP = 36.8;
    static final double CONTACT_PAD_LEFT = 21.4;
    static final double CONTACT_PITCH = 21.1;

    /**
     * The specialism strip's separator.
     *
     * <p>Nine spaces a side rather than two: the design tracks that whole
     * line, a text style carries no letter-spacing, and padding the separator
     * recovers the width without putting spaces inside a word — which is what
     * the extracted text would then say the document contains.</p>
     */
    static final String TAGLINE_SEPARATOR = "         •         ";

    // -- the rhythm between blocks -----------------------------------------

    static final double BODY_TOP = 18.5;
    static final double HEADING_TO_RULE = 3.5;
    static final double RULE_TO_SIDEBAR_BODY = 9.5;
    static final double RULE_TO_MAIN_BODY = 13.0;

    /**
     * The three gaps between sidebar blocks are three separate measurements
     * off the design, not one constant applied three times. Deriving them from
     * each other would be inventing a rhythm the design does not have.
     */
    static final double ACHIEVEMENTS_TOP_GAP = 23.6;
    static final double LANGUAGES_TOP_GAP = 17.3;
    static final double ADDITIONAL_TOP_GAP = 23.9;

    static final double ICON_COLUMN = 24.8;

    /**
     * The closing block's icon column, which the design makes narrower than
     * the competencies'. Worth its own constant rather than rounding to the
     * shared one: the longest label in that block cleared the shared column by
     * less than the rounding and broke onto a second line.
     */
    static final double ADDITIONAL_ICON_COLUMN = 22.9;

    static final double SIDEBAR_TABLE_WIDTH = SIDEBAR_WIDTH - TABLE_SLACK;
    static final double COMPETENCY_PITCH = 23.15;
    static final double ACHIEVEMENT_BODY_LEADING = 11.3;

    /**
     * Half the gap the design shows between achievements, because the trophy's
     * own line box is taller than the title beside it and a table row is as
     * tall as its tallest cell — the mark has already paid for the rest.
     */
    static final double ACHIEVEMENT_GAP = 3.5;

    static final double LANGUAGE_PITCH = 16.4;
    static final double ADDITIONAL_PITCH = 25.1;
    static final double LANGUAGE_NAME_COLUMN = SIDEBAR_WIDTH * 0.230;
    static final double LANGUAGE_LEVEL_COLUMN = SIDEBAR_WIDTH * 0.407;
    static final double LANGUAGE_RATING_COLUMN =
            SIDEBAR_TABLE_WIDTH - LANGUAGE_NAME_COLUMN - LANGUAGE_LEVEL_COLUMN;

    static final double PROFILE_LEADING = 13.83;
    static final double PROFILE_TO_EXPERIENCE = 23.0;
    static final double ENTRY_INDENT = 13.0;
    static final double ENTRY_WIDTH = MAIN_WIDTH - ENTRY_INDENT - TABLE_SLACK;

    /**
     * The design's three inter-entry gaps differ, and this is the one value
     * that has to serve all three. It sits at the low end of that range rather
     * than the middle: the body row is atomic and the page has about a point
     * of slack, so a gap that split the difference would cost a second page.
     */
    static final double ENTRY_GAP = 18.4;

    static final double ROLE_TO_EMPLOYER = 2.0;
    static final double EMPLOYER_TO_HIGHLIGHTS = 6.6;
    static final double HIGHLIGHT_PITCH = 14.96;
    static final double HIGHLIGHT_LEADING = 12.99;
    static final double PERIOD_COLUMN = ENTRY_WIDTH * 0.30;
    static final double ROLE_COLUMN = ENTRY_WIDTH - PERIOD_COLUMN;

    static final double EXPERIENCE_TO_RULE = 12.0;
    static final double RULE_TO_CREDENTIALS = 12.1;
    static final double CREDENTIAL_LEFT_COLUMN = MAIN_WIDTH * 0.400;
    static final double CREDENTIAL_GUTTER = MAIN_WIDTH * 0.0585;
    static final double CREDENTIAL_TABLE_WIDTH = MAIN_WIDTH - TABLE_SLACK;
    static final double CREDENTIAL_RIGHT_COLUMN =
            CREDENTIAL_TABLE_WIDTH - CREDENTIAL_LEFT_COLUMN - CREDENTIAL_GUTTER;

    /**
     * The credential headings sit closer to their rules than the headings
     * above them do. A table row's padding is not a paragraph's margin — it
     * has no line box to clear — so the same visual gap is a smaller number
     * here.
     */
    static final double CREDENTIAL_HEADING_TO_RULE = 2.5;
    static final double CREDENTIAL_RULE_TO_BODY = 6.5;
    static final double EDUCATION_LINE_PITCH = 12.4;
    static final double CREDENTIAL_LINE_PITCH = 11.9;

    /** A heading rule inside a credential column, as a filled hairline row. */
    static final double CREDENTIAL_RULE_SIZE = 0.5;

    /**
     * The credentials divider is the one mark whose extent is measured rather
     * than derived: a table cell's stroke is a box with no per-edge control,
     * so the divider cannot be the right column's own border and is a page
     * background like the three above it.
     */
    static final double CREDENTIALS_DIVIDER_X = 0.61422;
    static final double CREDENTIALS_DIVIDER_Y = 0.89671;
    static final double CREDENTIALS_DIVIDER_HEIGHT = 0.07713;

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

    static DocumentTextStyle italic(FontName font, double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .color(color)
                .decoration(DocumentTextDecoration.ITALIC)
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
     * A measured leading, as the additive value {@code lineSpacing} actually
     * is: the line box is {@link #LINE_FACTOR} times the type size and the
     * setting adds points on top of it.
     */
    static double leading(double measuredLeading, double size) {
        return Math.max(0.0, measuredLeading - LINE_FACTOR * size);
    }

    /** A node name built from a heading, with the spacing taken out of it. */
    static String compact(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "");
    }
}
