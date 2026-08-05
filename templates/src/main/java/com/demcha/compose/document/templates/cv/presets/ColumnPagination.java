package com.demcha.compose.document.templates.cv.presets;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a column of titled text blocks across pages.
 *
 * <p>A preset whose body is a multi-column {@code addRow} cannot lean on the
 * paginator: a row is atomic, so a row taller than the page raises
 * {@code AtomicNodeTooLargeException} rather than flowing. The preset has to
 * pick the page boundary itself, and to pick it, it has to guess how tall its
 * own content will be before the engine measures anything.</p>
 *
 * <p>The guess models one thing — line wrapping. A block's line is a logical
 * line; in a 148pt sidebar at 7.5pt it renders as three. Counting logical
 * lines and calling that a page budget is what overflows: the same count of
 * lines is twice the height in the narrow column. So the estimate turns a
 * character count into wrapped lines using the column's width and the font's
 * mean advance, and packs blocks until the page height is spent.</p>
 *
 * <p>It is an estimate, and deliberately a conservative one: a page that
 * breaks a block early costs some whitespace, while a page that breaks late
 * costs an exception. A font far from the assumed mean advance, or a single
 * unbreakable token wider than the column, can still push a page over.</p>
 *
 * <p>What it never does is lose a line. Everything handed to
 * {@link #paginate} comes back out on some page.</p>
 */
final class ColumnPagination {

    /**
     * Mean glyph advance as a fraction of font size, for the humanist sans
     * faces these presets use.
     *
     * <p>Taken across the body text of the repository's CV fixtures in Lato
     * at 7.5–7.9pt. Being a mean it is wrong for any individual line; over a
     * block of prose it converges, which is the scale it is used at.</p>
     */
    private static final double MEAN_GLYPH_ADVANCE_RATIO = 0.48;

    /**
     * Rendered line height as a ratio of font size, before extra spacing.
     *
     * <p>Package-visible so a preset sizing its own header uses the same
     * ratio this uses to size its columns — two copies of the number would
     * drift and only show up as a page that overflows.</p>
     */
    static final double FONT_LEADING_RATIO = 1.2;

    private final double charsPerLine;
    private final double lineHeight;
    private final double headingHeight;
    private final double blockGap;

    private ColumnPagination(double charsPerLine, double lineHeight,
                             double headingHeight, double blockGap) {
        this.charsPerLine = charsPerLine;
        this.lineHeight = lineHeight;
        this.headingHeight = headingHeight;
        this.blockGap = blockGap;
    }

    /**
     * One titled run of already-flattened content.
     *
     * @param title the heading to print
     * @param lines the block's logical lines, complete and unclipped
     * @param prose {@code true} to render as running text, {@code false} as bullets
     */
    record Block(String title, List<String> lines, boolean prose) {
    }

    /**
     * Builds an estimator for one column.
     *
     * @param columnWidth      the column's inner width in points
     * @param bodyFontSize     font size of the column's body text
     * @param extraLineSpacing additive gap between lines, in points
     * @param headingFontSize  font size of a block's heading
     * @param blockGap         space a block adds beyond its lines — heading
     *                         gap, trailing rule, spacing to the next block
     * @return an estimator for that column
     * @throws IllegalArgumentException if a font size or the width is not positive
     */
    static ColumnPagination forColumn(double columnWidth, double bodyFontSize,
                                      double extraLineSpacing,
                                      double headingFontSize, double blockGap) {
        if (columnWidth <= 0 || bodyFontSize <= 0 || headingFontSize <= 0) {
            throw new IllegalArgumentException(
                    "columnWidth, bodyFontSize and headingFontSize must be positive, got "
                    + columnWidth + ", " + bodyFontSize + ", " + headingFontSize);
        }
        return new ColumnPagination(
                Math.max(1.0, columnWidth / (bodyFontSize * MEAN_GLYPH_ADVANCE_RATIO)),
                bodyFontSize * FONT_LEADING_RATIO + Math.max(0.0, extraLineSpacing),
                headingFontSize * FONT_LEADING_RATIO,
                Math.max(0.0, blockGap));
    }

    /**
     * Splits blocks into per-page groups that fit {@code budget} points.
     *
     * <p>A block that does not fit the <em>remainder</em> of a page starts the
     * next one intact. A block too tall for a whole page is carried across as
     * many pages as it needs, repeating its heading — dropping the heading
     * would leave the continuation reading as part of what came before it.</p>
     *
     * @param blocks the column's blocks in render order
     * @param budget height available on one page, in points; must be positive
     * @return one list per page; at least one, possibly empty
     */
    List<List<Block>> paginate(List<Block> blocks, double budget) {
        List<List<Block>> pages = new ArrayList<>();
        List<Block> current = new ArrayList<>();
        double used = 0;
        for (Block block : blocks) {
            List<String> pending = block.lines();
            while (!pending.isEmpty()) {
                int fits = linesThatFit(pending, budget - used);
                if (fits == pending.size()) {
                    current.add(new Block(block.title(), pending, block.prose()));
                    used += blockHeight(pending);
                    pending = List.of();
                } else if (fits > 0 && current.isEmpty()) {
                    // First block on the page and it still overflows: take what
                    // fits so the remainder makes progress on the next page.
                    current.add(new Block(block.title(),
                            List.copyOf(pending.subList(0, fits)), block.prose()));
                    pending = List.copyOf(pending.subList(fits, pending.size()));
                    used = budget;
                } else if (current.isEmpty()) {
                    // Not one line fits an empty page — the budget is smaller
                    // than a heading. Emit a line anyway: looping here would
                    // never terminate, and dropping it is the bug being fixed.
                    current.add(new Block(block.title(),
                            List.of(pending.get(0)), block.prose()));
                    pending = List.copyOf(pending.subList(1, pending.size()));
                    used = budget;
                } else {
                    // Push the whole block to the next page rather than
                    // orphaning its opening lines under a heading at the foot.
                    used = budget;
                }
                if (used >= budget && !pending.isEmpty()) {
                    pages.add(List.copyOf(current));
                    current = new ArrayList<>();
                    used = 0;
                }
            }
        }
        pages.add(List.copyOf(current));
        return List.copyOf(pages);
    }

    /**
     * Estimated height of a whole page's worth of blocks.
     *
     * @param page the blocks placed on one page
     * @return height in points
     */
    double pageHeight(List<Block> page) {
        double height = 0;
        for (Block block : page) {
            height += blockHeight(block.lines());
        }
        return height;
    }

    /**
     * How many rendered lines a logical line takes in this column.
     *
     * @param line the logical line; {@code null} or empty counts as one
     * @return at least one
     */
    int wrappedLines(String line) {
        if (line == null || line.isEmpty()) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(line.length() / charsPerLine));
    }

    /** Height a run of lines occupies under its own heading. */
    double blockHeight(List<String> lines) {
        double height = headingHeight + blockGap;
        for (String line : lines) {
            height += wrappedLines(line) * lineHeight;
        }
        return height;
    }

    /**
     * The largest prefix of {@code lines} that fits {@code available} points
     * once the heading is paid for.
     *
     * @return how many lines fit; {@code 0} when not even one does
     */
    int linesThatFit(List<String> lines, double available) {
        double used = headingHeight + blockGap;
        int fitted = 0;
        for (String line : lines) {
            double next = used + wrappedLines(line) * lineHeight;
            if (next > available) {
                break;
            }
            used = next;
            fitted++;
        }
        return fitted;
    }
}
