package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Carries a CV whose columns are one row onto as many pages as it needs, one row per page.
 *
 * <p>A row is atomic, so a CV taller than the page raises {@code AtomicNodeTooLargeException}
 * rather than flowing on. This lets a preset keep its row for a CV that fits and pick its own page
 * boundaries for one that does not. Each column is a list of blocks — a role, a project, a sidebar
 * list — that are never split; every block is laid out on its own at its column's width, and the
 * blocks are packed onto pages in order. {@link TimelineMinimal} does the same from estimated line
 * counts; measuring is what lets a column carry meters, rails and marks.</p>
 *
 * <p>Every row after the first opens with a page break. A page closes when its next block does not
 * fit what the page has left, but that block leaves its lead behind when it opens the next page, so
 * the row it starts can be short enough to settle under the one before: a hairline taller than the
 * page's top padding is all it takes.</p>
 */
final class ColumnPages {

    /**
     * How far past the page the paginator lets a block that cannot be split run, as rounding noise.
     * Columns that fit the first page within it stay on that page, as the single row they are
     * composed as always did.
     */
    private static final double OVERRUN = 0.5;

    /** Room left unspent at the foot of a page, for the rounding in the measured heights. */
    private static final double SLACK = 0.1;

    private ColumnPages() {
    }

    /**
     * A piece of a column that moves to another page whole.
     *
     * @param name    the block's name, unique across the columns of one plan
     * @param lead    what separates the block from the one above it on the same page — a hairline
     *                — and is left out when the block opens a page; {@code null} for nothing
     * @param content the block itself
     */
    record Block(String name, Consumer<SectionBuilder> lead, Consumer<SectionBuilder> content) {

        /**
         * A block with nothing between it and the block above.
         *
         * @param name    the block's name
         * @param content the block itself
         * @return the block
         */
        static Block of(String name, Consumer<SectionBuilder> content) {
            return new Block(name, null, content);
        }
    }

    /**
     * One column of the row: where it sits, how much of a page its blocks may fill, and the blocks.
     *
     * @param insetLeft  the width taken by the columns to its left
     * @param insetRight the width taken by the columns to its right
     * @param padLeft    the column's own left padding
     * @param padRight   the column's own right padding
     * @param firstPage  the height its blocks may fill on the first page
     * @param laterPages the height they may fill on every later page
     * @param blocks     the blocks, in reading order
     */
    record Column(double insetLeft, double insetRight, double padLeft, double padRight,
                  double firstPage, double laterPages, List<Block> blocks) {
    }

    /**
     * Which of each column's blocks land on each page.
     *
     * @param columns per column, per page, the indices of that column's blocks
     */
    record Plan(List<List<List<Integer>>> columns) {

        /**
         * How many pages the fullest column needs.
         *
         * @return at least one
         */
        int pages() {
            int pages = 1;
            for (List<List<Integer>> column : columns) {
                pages = Math.max(pages, column.size());
            }
            return pages;
        }

        /**
         * The blocks a column puts on a page.
         *
         * @param column the column's index
         * @param page   the page's index
         * @return the indices of the column's blocks on that page; empty past its last page
         */
        List<Integer> blocks(int column, int page) {
            List<List<Integer>> pages = columns.get(column);
            return page < pages.size() ? pages.get(page) : List.of();
        }
    }

    /** Composes the row of one page of a plan. */
    @FunctionalInterface
    interface PageRow {

        /**
         * Composes the row.
         *
         * @param row  the page's row
         * @param page the page's index, from zero
         */
        void compose(RowBuilder row, int page);
    }

    /**
     * Measures every block and cuts each column into pages.
     *
     * <p>Columns that fit the first page stay on it whole. Otherwise each column is packed page by
     * page, a block moving on whole when it and its lead do not fit what the page has left.</p>
     *
     * @param document the session the preset composes into, with its page already set
     * @param columns  the row's columns, left to right
     * @return the pages each column's blocks land on
     * @throws AtomicNodeTooLargeException if a block is taller than the page it opens — raised while
     *                                     measuring when the block cannot be split even there
     * @throws IllegalArgumentException    if two blocks share a name
     */
    static Plan plan(DocumentSession document, List<Column> columns) {
        List<String> names = new ArrayList<>();
        Set<String> blocks = new HashSet<>();
        for (Column column : columns) {
            for (Block block : column.blocks()) {
                if (!blocks.add(block.name())) {
                    throw new IllegalArgumentException(
                            "Two blocks of one plan are named " + block.name());
                }
                names.add(contentName(block));
                if (block.lead() != null) {
                    names.add(leadName(block));
                }
            }
        }
        Map<String, ReadingOrderColumns.Box> sizes = ReadingOrderColumns.measure(document, page -> {
            page.spacing(0);
            for (Column column : columns) {
                for (Block block : column.blocks()) {
                    page.add(probe(column, contentName(block), block.content()));
                    if (block.lead() != null) {
                        page.add(probe(column, leadName(block), block.lead()));
                    }
                }
            }
        }, names.toArray(String[]::new));

        boolean onePage = true;
        for (Column column : columns) {
            if (height(column, sizes) > column.firstPage() + OVERRUN - SLACK) {
                onePage = false;
            }
        }
        List<List<List<Integer>>> packed = new ArrayList<>();
        for (Column column : columns) {
            packed.add(onePage ? List.of(all(column)) : pack(column, sizes));
        }
        return new Plan(List.copyOf(packed));
    }

    /**
     * Adds a plan's rows to the page flow: one row per page, every row after the first behind a
     * page break.
     *
     * @param flow the page flow, which must add no spacing of its own: a gap after a full page
     *             would move on to the next page before the break does, and the break would
     *             then leave that page blank
     * @param plan the plan the rows follow
     * @param name the first row's name; a later row's name adds its page's index
     * @param row  composes each page's row
     */
    static void addRows(PageFlowBuilder flow, Plan plan, String name, PageRow row) {
        for (int index = 0; index < plan.pages(); index++) {
            int page = index;
            if (page > 0) {
                flow.addPageBreak(pageBreak -> pageBreak.name(name + "Break_" + page));
            }
            flow.addRow(page == 0 ? name : name + "_" + page, builder -> row.compose(builder, page));
        }
    }

    /**
     * Composes the blocks a plan puts on one page, leaving out the lead of the block that opens it.
     *
     * @param section the column's section on that page
     * @param blocks  all of the column's blocks
     * @param page    the indices of the blocks on that page
     */
    static void compose(SectionBuilder section, List<Block> blocks, List<Integer> page) {
        for (int k = 0; k < page.size(); k++) {
            Block block = blocks.get(page.get(k));
            if (k > 0 && block.lead() != null) {
                block.lead().accept(section);
            }
            block.content().accept(section);
        }
    }

    /** A column's height on a single page: every block, and every lead but the first. */
    private static double height(Column column, Map<String, ReadingOrderColumns.Box> sizes) {
        double height = 0;
        for (int index = 0; index < column.blocks().size(); index++) {
            Block block = column.blocks().get(index);
            height += sizes.get(contentName(block)).height();
            if (index > 0 && block.lead() != null) {
                height += sizes.get(leadName(block)).height();
            }
        }
        return height;
    }

    /** Every block of a column, in order. */
    private static List<Integer> all(Column column) {
        List<Integer> indices = new ArrayList<>(column.blocks().size());
        for (int index = 0; index < column.blocks().size(); index++) {
            indices.add(index);
        }
        return List.copyOf(indices);
    }

    /** A column packed page by page, each page holding the blocks that fit it. */
    private static List<List<Integer>> pack(Column column,
                                            Map<String, ReadingOrderColumns.Box> sizes) {
        List<List<Integer>> pages = new ArrayList<>();
        List<Integer> page = new ArrayList<>();
        double room = column.firstPage() - SLACK;
        double used = 0;
        for (int index = 0; index < column.blocks().size(); index++) {
            Block block = column.blocks().get(index);
            double content = sizes.get(contentName(block)).height();
            double lead = block.lead() == null ? 0 : sizes.get(leadName(block)).height();
            if (!page.isEmpty() && used + lead + content > room) {
                pages.add(List.copyOf(page));
                page = new ArrayList<>();
                used = 0;
                room = column.laterPages() - SLACK;
            }
            if (page.isEmpty() && content > room) {
                throw new AtomicNodeTooLargeException(String.format(Locale.ROOT,
                        "Block '%s' is %.1fpt tall and a page of its column holds %.1fpt: a block"
                                + " moves to another page whole and is never split. Shorten it,"
                                + " or break it into smaller entries.",
                        block.name(), content, room + SLACK));
            }
            used += (page.isEmpty() ? 0 : lead) + content;
            page.add(index);
        }
        pages.add(List.copyOf(page));
        return List.copyOf(pages);
    }

    /** A block on its own, at its column's width, in a section of its own name to be measured. */
    private static DocumentNode probe(Column column, String name, Consumer<SectionBuilder> content) {
        return ReadingOrderColumns.column(name + "Band", column.insetLeft(), column.insetRight(),
                name, section -> {
                    section.spacing(0)
                            .padding(new DocumentInsets(0, column.padRight(), 0, column.padLeft()));
                    content.accept(section);
                });
    }

    private static String contentName(Block block) {
        return block.name() + "Block";
    }

    private static String leadName(Block block) {
        return block.name() + "Lead";
    }
}
