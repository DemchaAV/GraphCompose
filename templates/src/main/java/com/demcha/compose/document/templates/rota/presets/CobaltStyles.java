package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.rota.ShiftStatus;
import com.demcha.compose.font.FontName;

/**
 * Every colour, size and measurement of the Cobalt rota.
 *
 * <h2>Two roots, and everything derives from them</h2>
 *
 * <p>Horizontal geometry derives from the page: the label column takes a
 * measured share of the content width and the day columns divide what is
 * left. Type derives from {@link #BODY_SIZE}: every face is a ratio of it. Two
 * numbers move the sheet, and the rest follows.</p>
 *
 * <h2>One rule width, many colours</h2>
 *
 * <p>Every cell edge in the table is drawn at {@link #GRID_LINE_WIDTH}; only the
 * colour changes. A cell's stroke is centred on its box and so reaches half its
 * width outside it — mix widths, or leave a neighbour unstroked, and the shared
 * edge lands in a different place on each side and the grid steps.</p>
 */
final class CobaltStyles {

    private CobaltStyles() {
    }

    // ------------------------------------------------------------------
    // The page
    // ------------------------------------------------------------------

    /** A rota is read across its days, not down them, so the page turns on its side. */
    static final DocumentPageSize PAGE = DocumentPageSize.A4.landscape();

    static final double MARGIN_X = PAGE.width() * 0.01475;
    static final double MARGIN_TOP = PAGE.height() * 0.0178;

    /**
     * Deep enough to hold the footer chrome. The separator draws at the top of
     * the footer zone and the text just beneath it, so the zone's depth is what
     * places the footer.
     */
    static final double MARGIN_BOTTOM = PAGE.height() * 0.0654;

    static final DocumentInsets PAGE_MARGIN =
            new DocumentInsets(MARGIN_TOP, MARGIN_X, MARGIN_BOTTOM, MARGIN_X);

    static final double CONTENT_WIDTH = PAGE.width() - 2 * MARGIN_X;

    // ------------------------------------------------------------------
    // The grid
    // ------------------------------------------------------------------

    /**
     * The share of the content width the staff-name column takes. Every other
     * horizontal dimension derives from it: the day columns are what is left,
     * divided evenly. Change this one number and the grid moves together.
     */
    static final double LABEL_COLUMN_WEIGHT = 0.1253;

    static final double LABEL_COLUMN_WIDTH = CONTENT_WIDTH * LABEL_COLUMN_WEIGHT;

    /**
     * The horizontal geometry of one sheet, which is a property of the document
     * rather than of the design: the day columns divide what the label column
     * leaves, so a rota of five days makes five wider columns and one of seven
     * makes seven narrower ones. Everything that sits inside a day column — its
     * padding, its chips, its legend swatch — is a fraction of that width, so
     * all of it moves together.
     *
     * @param days        how many day columns the sheet has
     * @param dayWidth    the width of one of them
     * @param cellPadX    a body cell's horizontal padding
     * @param chipInsetX  the gap between a chip and its column's edge
     * @param chipWidth   what is left for the chip itself
     * @param legendInset the same gap for a legend swatch, which sits closer
     * @param legendWidth what is left for the swatch
     */
    record Grid(int days, double dayWidth, double cellPadX, double chipInsetX,
                double chipWidth, double legendInset, double legendWidth) {

        /**
         * Rejects a grid whose parts do not belong to one another.
         *
         * <p>Six of the seven components derive from the first, so the only way
         * to build a grid is {@link #of(int)} — the check is here so that stays
         * true rather than depending on nobody calling the constructor.</p>
         */
        Grid {
            if (days < 0) {
                throw new IllegalArgumentException("A rota cannot have " + days + " days.");
            }
            if (dayWidth <= 0 || chipWidth <= 0 || legendWidth <= 0) {
                throw new IllegalArgumentException(
                        "Every width of a rota grid must be positive; got day " + dayWidth
                                + ", chip " + chipWidth + ", legend " + legendWidth + ".");
            }
        }

        /**
         * The grid a rota of this many days is drawn on.
         *
         * @param days the document's own day count
         * @return the geometry
         */
        static Grid of(int days) {
            // A rota with no days still has its label column, and a width is
            // still asked for: one column's worth keeps the arithmetic finite
            // and nothing is drawn in it.
            double width = CONTENT_WIDTH * (1 - LABEL_COLUMN_WEIGHT) / Math.max(1, days);
            double chipInset = width * 0.035;
            double legendInset = width * 0.012;
            return new Grid(days, width, width * 0.03, chipInset, width - 2 * chipInset,
                    legendInset, width - 2 * legendInset);
        }

        /** The label column plus one per day. */
        int columnCount() {
            return days + 1;
        }
    }

    /**
     * The masthead rule, the day names, the day notes and the rule that closes
     * the header — four rows, all repeating on a continuation page.
     */
    static final int HEADER_ROW_COUNT = 4;

    static final double STAFF_ROW_HEIGHT = PAGE.height() * 0.0417;
    static final double BAND_HEIGHT = PAGE.height() * 0.0332;
    static final double BAND_ICON_SIZE = BAND_HEIGHT * 0.52;

    // ------------------------------------------------------------------
    // Rules: one hierarchy, one base
    // ------------------------------------------------------------------

    /**
     * The hairline the body grid is drawn with, and the unit every heavier rule
     * is a multiple of.
     *
     * <p>Not half a point, which is what a design measures and what a rota
     * pinned to a wall cannot use: below about a point the grid falls under a
     * device pixel the moment the page is viewed at less than full size, and the
     * sheet reads as floating text with no table under it. Raising this one
     * number raises the whole hierarchy with it, so the rules keep their order
     * rather than flattening into each other.</p>
     */
    static final double GRID_LINE_WIDTH = 0.9;

    /** The outline chip's hairline: a foreground detail, just above the grid. */
    static final double CHIP_OUTLINE_WIDTH = GRID_LINE_WIDTH * 1.22;

    /** The rule closing the header, heavier so the two read as different things. */
    static final double BODY_TOP_RULE_THICKNESS = GRID_LINE_WIDTH * 1.78;

    /** The masthead's rule, the heaviest line on the sheet. */
    static final double MASTHEAD_RULE_THICKNESS = GRID_LINE_WIDTH * 2.1;

    /**
     * The footer's rule, which is chrome and not part of the table's grid, and
     * so is deliberately not tied to the hierarchy above.
     *
     * <p>{@code DocumentHeaderFooter} fixes the gap between that rule and the
     * footer text, so a heavier rule crowds the text against it.</p>
     */
    static final double FOOTER_RULE_WIDTH = 0.5;

    // ------------------------------------------------------------------
    // Chips
    // ------------------------------------------------------------------

    /** A lone entry fills its cell: about seven tenths of the row. */
    static final double CHIP_HEIGHT = STAFF_ROW_HEIGHT * 0.705;

    /** One of a stacked pair is shorter, so two and a gap fit the same block. */
    static final double CHIP_STACK_HEIGHT = STAFF_ROW_HEIGHT * 0.37;

    static final double CHIP_STACK_GAP = 1.2;
    static final double CHIP_CORNER_RADIUS = CHIP_HEIGHT * 0.23;

    /**
     * The height every day cell's content occupies, whatever it holds.
     *
     * <p>This is what makes a chip sit centred. A table cell places a node child
     * at its <em>top</em> — {@code textAnchor} seats text, not a node — so a
     * short chip in a row made tall by a neighbouring cell would hang with all
     * the slack beneath it. Giving every day cell the same content height leaves
     * no slack to hang in: a stacked pair fills this exactly, and a lone chip is
     * centred in it by {@link #SINGLE_SLOT_PAD_Y}, which is the difference
     * halved rather than a number nudged until it looked right.</p>
     */
    static final double CELL_BLOCK_HEIGHT = 2 * CHIP_STACK_HEIGHT + 2 * CHIP_STACK_GAP;

    /** Half the room a lone chip does not use, so it lands in the middle. */
    static final double SINGLE_SLOT_PAD_Y = (CELL_BLOCK_HEIGHT - CHIP_HEIGHT) / 2;

    /** The legend's swatches sit closer to their column's edges than a shift does. */
    static final double LEGEND_CHIP_HEIGHT = STAFF_ROW_HEIGHT * 0.62;

    // ------------------------------------------------------------------
    // Padding
    // ------------------------------------------------------------------

    static final double CELL_PAD_Y = STAFF_ROW_HEIGHT * 0.12;

    /**
     * The legend and covers rows are taller than a staff row and own their own
     * padding: one constant for two different rows would make shrinking the
     * staff rows shorten these with them.
     */
    static final double LEGEND_PAD_Y = STAFF_ROW_HEIGHT * 0.214;
    static final double COVERS_PAD_Y = STAFF_ROW_HEIGHT * 0.270;

    static final double DAY_NAME_PAD_TOP = PAGE.height() * 0.010;
    static final double DAY_NAME_PAD_BOTTOM = PAGE.height() * 0.018;
    static final double DAY_NOTE_PAD_TOP = PAGE.height() * 0.012;
    static final double DAY_NOTE_PAD_BOTTOM = PAGE.height() * 0.018;

    /**
     * Barely any vertical padding: this cell sets the header row's height, and
     * the day-name cell's generous insets here cost enough page to push a
     * twelve-person sheet onto a second one.
     */
    static final double LABEL_HEADER_PAD_Y = CELL_PAD_Y * 0.4;

    static final double BAND_PAD_X = CONTENT_WIDTH * 0.008;
    static final double BAND_PAD_Y = BAND_HEIGHT * 0.22;

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    /**
     * The face the sheet is set in. The templates artifact carries no fonts:
     * Carlito and Spectral arrive with {@code graph-compose-fonts} on the
     * classpath, or a caller registers families of those names on the session
     * itself. With neither, the engine substitutes and every size is solved for
     * type that is not there.
     */
    static final FontName SANS = FontName.CARLITO;

    /** The wordmark's face, and the only serif on the sheet. */
    static final FontName SERIF = FontName.SPECTRAL;

    /** The body face every other size is a ratio of. */
    static final double BODY_SIZE = 10.4;

    /**
     * The label column's own horizontal padding.
     *
     * <p>Taken once from a seven-day grid and then held there. The label
     * column's width does not change with the day count and the mark in it
     * should not either: derived live, a five-day rota would set its wordmark at
     * a different size from a seven-day one for no reason a reader could see.
     * The seven is the design's own, and the number it produces is the padding
     * the design gives this column.</p>
     */
    static final double LABEL_PAD_X = Grid.of(7).cellPadX();

    /**
     * The width the lockup gets: its column less the padding holding it off the
     * rule that boxes the cell. Stated on its own because it is the constraint
     * the wordmark is sized against.
     */
    static final double LOCKUP_WIDTH = LABEL_COLUMN_WIDTH - 2 * LABEL_PAD_X;

    /**
     * Sized by the width it has to fit rather than by the body scale: the lockup
     * sits in the label column beside the day names, and this fraction is what
     * keeps the wordmark on one line.
     */
    static final double WORDMARK_SIZE = LOCKUP_WIDTH * 0.22;
    static final double WORDMARK_SUB_SIZE = WORDMARK_SIZE * 0.4766;

    /**
     * The lockup's two lines, closed up.
     *
     * <p>Negative because it corrects a gap nothing asked for: the section
     * stacks its children with no spacing and the wordmark's own line box still
     * leaves about a third of its size between the lines. It would belong on the
     * section, which owns the distance between its children, but {@code spacing}
     * refuses a negative value — so it is a top margin on the one child it
     * applies to. It is a fraction of the wordmark because the gap it corrects is
     * that face's line box, so the two move together.</p>
     */
    static final double LOCKUP_LINE_SPACING = -WORDMARK_SIZE * 0.3076;

    static final double DAY_NAME_SIZE = BODY_SIZE * 0.87;
    static final double DAY_ORDINAL_SUFFIX_SIZE = DAY_NAME_SIZE * 0.68;
    static final double DAY_NOTE_SIZE = BODY_SIZE * 0.82;
    static final double GROUP_LABEL_SIZE = BODY_SIZE * 0.95;
    static final double STAFF_NAME_SIZE = BODY_SIZE * 1.03;
    static final double LEGEND_LABEL_SIZE = BODY_SIZE * 0.79;
    static final double COVERS_LABEL_SIZE = BODY_SIZE * 0.96;
    static final double COVERS_VALUE_SIZE = BODY_SIZE;
    static final double COVERS_TAG_SIZE = BODY_SIZE * 0.64;
    static final double FOOTER_SIZE = BODY_SIZE * 0.70;

    /**
     * The one face a box constrains rather than the scale: a chip in a stacked
     * pair has only {@link #CHIP_STACK_HEIGHT} to sit in, so its size is a
     * fraction of that slot. Tie it to the body scale and the pair stops fitting
     * the block a single chip fills.
     */
    static final double CHIP_SIZE = CHIP_STACK_HEIGHT * 0.89;

    /** A lone entry carries the body face; a stacked one keeps the smaller. */
    static final double CHIP_SINGLE_SIZE = BODY_SIZE;

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    static final DocumentColor NAVY = DocumentColor.rgb(16, 32, 80);
    static final DocumentColor GOLD = DocumentColor.rgb(166, 138, 66);
    static final DocumentColor PAPER = DocumentColor.rgb(255, 255, 255);
    static final DocumentColor GRID_LINE = DocumentColor.rgb(208, 208, 224);

    /**
     * The tint on alternate staff rows: pale enough that the grid drawn over it
     * stays the darker of the two, and that navy text and every status colour
     * keep their contrast. Its job is to give the eye a rail to follow across
     * the columns, not to be seen.
     */
    static final DocumentColor ZEBRA_TINT = DocumentColor.rgb(228, 235, 248);

    /** Navy at half strength: enough to read the tag, not enough to compete. */
    static final DocumentColor COVERS_TAG_INK = DocumentColor.rgb(120, 132, 168);

    static final DocumentColor STATUS_REQUEST = DocumentColor.rgb(176, 176, 192);
    static final DocumentColor STATUS_OFF = DocumentColor.rgb(208, 16, 16);
    static final DocumentColor STATUS_HOLIDAY = DocumentColor.rgb(255, 192, 32);
    static final DocumentColor STATUS_STOCK = DocumentColor.rgb(16, 160, 80);
    static final DocumentColor STATUS_STANDBY = DocumentColor.rgb(192, 144, 224);
    static final DocumentColor STATUS_TRAINING = DocumentColor.rgb(240, 80, 16);
    static final DocumentColor STATUS_SUPPORT = DocumentColor.rgb(208, 216, 240);

    static final DocumentStroke NAVY_RULE = DocumentStroke.of(NAVY, GRID_LINE_WIDTH);
    static final DocumentStroke GRID_RULE = DocumentStroke.of(GRID_LINE, GRID_LINE_WIDTH);

    // ------------------------------------------------------------------
    // Text styles
    // ------------------------------------------------------------------

    static DocumentTextStyle sans(double size, DocumentTextDecoration decoration,
                                  DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(SANS).size(size).decoration(decoration).color(color).build();
    }

    static DocumentTextStyle serif(double size, DocumentTextDecoration decoration,
                                   DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(SERIF).size(size).decoration(decoration).color(color).build();
    }

    static final DocumentTextStyle WORDMARK =
            serif(WORDMARK_SIZE, DocumentTextDecoration.DEFAULT, GOLD);
    static final DocumentTextStyle WORDMARK_SUB =
            sans(WORDMARK_SUB_SIZE, DocumentTextDecoration.DEFAULT, GOLD);
    static final DocumentTextStyle DAY_NAME =
            sans(DAY_NAME_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle DAY_ORDINAL_SUFFIX =
            sans(DAY_ORDINAL_SUFFIX_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle DAY_NOTE =
            sans(DAY_NOTE_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle GROUP_LABEL =
            sans(GROUP_LABEL_SIZE, DocumentTextDecoration.BOLD, PAPER);
    static final DocumentTextStyle STAFF_NAME =
            sans(STAFF_NAME_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle LEGEND_LABEL =
            sans(LEGEND_LABEL_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle COVERS_LABEL =
            sans(COVERS_LABEL_SIZE, DocumentTextDecoration.BOLD, PAPER);
    static final DocumentTextStyle COVERS_VALUE =
            sans(COVERS_VALUE_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle COVERS_TAG =
            sans(COVERS_TAG_SIZE, DocumentTextDecoration.BOLD, COVERS_TAG_INK);
    static final DocumentTextStyle CHIP_ON_LIGHT =
            sans(CHIP_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle CHIP_ON_DARK =
            sans(CHIP_SIZE, DocumentTextDecoration.BOLD, PAPER);
    static final DocumentTextStyle CHIP_SINGLE_ON_LIGHT =
            sans(CHIP_SINGLE_SIZE, DocumentTextDecoration.BOLD, NAVY);
    static final DocumentTextStyle CHIP_SINGLE_ON_DARK =
            sans(CHIP_SINGLE_SIZE, DocumentTextDecoration.BOLD, PAPER);

    /**
     * A rule row is an empty cell, and an empty cell is as tall as its own font,
     * so the rule's thickness is set as a type size.
     */
    static final DocumentTextStyle MASTHEAD_RULE_TEXT =
            sans(MASTHEAD_RULE_THICKNESS, DocumentTextDecoration.DEFAULT, NAVY);
    static final DocumentTextStyle BODY_TOP_RULE_TEXT =
            sans(BODY_TOP_RULE_THICKNESS, DocumentTextDecoration.DEFAULT, GRID_LINE);

    // ------------------------------------------------------------------
    // Cell styles
    // ------------------------------------------------------------------

    /** The colour a status is drawn in. */
    static DocumentColor colorFor(ShiftStatus status) {
        return switch (status) {
            case REQUEST -> STATUS_REQUEST;
            case OFF -> STATUS_OFF;
            case HOLIDAY -> STATUS_HOLIDAY;
            case STOCK -> STATUS_STOCK;
            case STANDBY -> STATUS_STANDBY;
            case TRAINING -> STATUS_TRAINING;
            case SUPPORT -> STATUS_SUPPORT;
            case NONE -> PAPER;
        };
    }

    /**
     * The label colour a filled chip needs, which is a property of the status
     * rather than of the chip: the pale fills carry navy labels and the
     * saturated ones carry white. One white constant would make every holiday
     * cell unreadable.
     *
     * @param status what the entry means
     * @param lone   whether it is the only entry in its cell, and so larger
     * @return the style for its label
     */
    static DocumentTextStyle labelOnFillFor(ShiftStatus status, boolean lone) {
        boolean onDark = switch (status) {
            case HOLIDAY, STANDBY, SUPPORT, NONE -> false;
            case REQUEST, OFF, STOCK, TRAINING -> true;
        };
        if (onDark) {
            return lone ? CHIP_SINGLE_ON_DARK : CHIP_ON_DARK;
        }
        return lone ? CHIP_SINGLE_ON_LIGHT : CHIP_ON_LIGHT;
    }

    /** One slot of a shift block; the padding is what seats the chip in it. */
    static DocumentTableStyle slotStyle(double padY, DocumentColor rowFill) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(padY, 0, padY, 0))
                .fillColor(rowFill)
                .stroke(DocumentStroke.of(rowFill, 0))
                .textStyle(CHIP_ON_LIGHT)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(0)
                .build();
    }

    /** A cell whose content is a chip: the inset is its horizontal padding. */
    static DocumentTableStyle chipCellStyle(double insetX, double padY,
                                            DocumentStroke rule, DocumentColor fill) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(padY, insetX, padY, insetX))
                .fillColor(fill)
                .stroke(rule)
                .textStyle(CHIP_ON_LIGHT)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(0)
                .build();
    }

    /** A plain text cell with the body padding and a named rule. */
    static DocumentTableStyle textCellStyle(DocumentTextStyle text,
                                            DocumentTableTextAnchor anchor, double padY,
                                            double padX, DocumentStroke rule,
                                            DocumentColor fill) {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(padY, padX, padY, padX))
                .fillColor(fill)
                .stroke(rule)
                .textStyle(text)
                .textAnchor(anchor)
                .lineSpacing(0)
                .build();
    }

    /**
     * The label column's own header cells — the lockup and the cell under it.
     *
     * <p>Stroked, so the column is closed into the same grid as the day
     * columns beside it. Left unstroked it reads as an open margin with a mark
     * floating in it rather than as the first column of the sheet.</p>
     */
    static DocumentTableStyle labelHeaderStyle() {
        return DocumentTableStyle.builder()
                .padding(new DocumentInsets(
                        LABEL_HEADER_PAD_Y, LABEL_PAD_X, LABEL_HEADER_PAD_Y, LABEL_PAD_X))
                .fillColor(PAPER)
                .stroke(GRID_RULE)
                .textStyle(DAY_NAME)
                .textAnchor(DocumentTableTextAnchor.CENTER)
                .lineSpacing(0)
                .build();
    }
}
