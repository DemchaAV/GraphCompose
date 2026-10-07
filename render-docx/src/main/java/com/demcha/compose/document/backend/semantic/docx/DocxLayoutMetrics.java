package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphLineGeometry;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeSet;

/**
 * The numbers the compiled layout already worked out, addressed by the node that owns them.
 *
 * <p>The semantic export writes what the document says and leaves what it does not know to
 * Word. Two of the things it does not know matter most to how the file looks: how tall a
 * line of text is, and how wide a table's columns came out. Both are measurements over the
 * font, which this backend has no runtime for — but the engine made them already, and a
 * backend that asks for {@code requiresResolvedLayout()} is handed them.</p>
 *
 * <p>A {@link LayoutGraph} addresses everything by a path built from each node's name (or
 * its kind, when it has none) and its index among its siblings. That path is rebuilt here
 * by walking the same tree the compiler walked, so a node can be handed over and its
 * geometry looked up — no signature in the writers has to carry a path around, and nothing
 * depends on the writers visiting nodes in the compiler's order.</p>
 *
 * <p>Every accessor answers "I do not know" rather than guessing: an empty index — which is
 * what an export without a compiled layout gets — makes the whole class inert and the
 * writers fall back to what they can work out themselves.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxLayoutMetrics {

    /** What an export with no compiled layout uses: every question answers "unknown". */
    static final DocxLayoutMetrics EMPTY =
            new DocxLayoutMetrics(new IdentityHashMap<>(), java.util.Set.of(), Map.of(), Map.of(), List.of(), 0);

    private final Map<DocumentNode, String> paths;
    // Nodes one instance of which stands at more than one place — see placedMoreThanOnce.
    private final java.util.Set<DocumentNode> repeated;
    private final Map<String, List<PlacedFragment>> fragments;
    private final Map<String, PlacedNode> placed;
    // Every fragment, in the order the page paints them.
    private final List<PlacedFragment> painted;
    private final int pageCount;
    // A table's measured cells by name, filled on first use — see cellLineHeightsOf.
    private final Map<DocumentNode, Map<String, Double>> cellLineHeights = new IdentityHashMap<>();
    private final Map<DocumentNode, Map<String, List<CellBox>>> cellBoxes = new IdentityHashMap<>();
    // The rows of a table the layout placed, by index, filled on first use — see placedRowsOf.
    private final Map<DocumentNode, Map<String, Integer>> placedRows = new IdentityHashMap<>();
    // The heights of a table's placed rows, by index, filled on first use — see rowHeightsOf.
    private final Map<DocumentNode, Map<String, Double>> rowHeights = new IdentityHashMap<>();
    // Paragraphs composed in table cells, paired with their fragments on first use.
    private Map<DocumentNode, PlacedFragment> composedText;
    // The text and pictures of each page, indexed on first use — see textOnPage.
    private Map<Integer, List<PlacedFragment>> textByPage;
    // Each page's fragments in paint order, indexed on first use — see clipsOf.
    private Map<Integer, List<PlacedFragment>> paintedByPage;
    // A table's first own row on each page, filled on first use — see colourUnderCell.
    private final Map<DocumentNode, Map<Integer, PlacedFragment>> firstRows = new IdentityHashMap<>();
    // The node at each path, indexed on first use — see aRowsOwnFill.
    private Map<String, DocumentNode> nodesByPath;

    private DocxLayoutMetrics(Map<DocumentNode, String> paths,
                              java.util.Set<DocumentNode> repeated,
                              Map<String, List<PlacedFragment>> fragments,
                              Map<String, PlacedNode> placed,
                              List<PlacedFragment> painted,
                              int pageCount) {
        this.paths = paths;
        this.repeated = repeated;
        this.fragments = fragments;
        this.placed = placed;
        this.painted = painted;
        this.pageCount = pageCount;
    }

    /**
     * Indexes a compiled layout against the tree it was compiled from.
     *
     * @param graph  the document being exported
     * @param layout the layout compiled from it, or {@code null}
     * @return an index, or {@link #EMPTY} when there is no layout to index
     */
    static DocxLayoutMetrics of(DocumentGraph graph, LayoutGraph layout) {
        if (graph == null) {
            return EMPTY;
        }
        Map<DocumentNode, String> paths = new IdentityHashMap<>();
        for (int index = 0; index < graph.roots().size(); index++) {
            indexPaths(graph.roots().get(index), null, index, paths);
        }
        java.util.Set<DocumentNode> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        java.util.Set<DocumentNode> repeated = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (DocumentNode root : graph.roots()) {
            countPlaces(root, seen, repeated);
        }
        if (layout == null) {
            // No measurements, but the paths still name the nodes — which is what a
            // diagnostic note needs to say where in the document it came from.
            return new DocxLayoutMetrics(paths, repeated, Map.of(), Map.of(), List.of(), 0);
        }
        Map<String, List<PlacedFragment>> fragments = new HashMap<>();
        for (PlacedFragment fragment : layout.fragments()) {
            fragments.computeIfAbsent(fragment.path(), key -> new ArrayList<>()).add(fragment);
        }
        Map<String, PlacedNode> placed = new HashMap<>();
        for (PlacedNode node : layout.nodes()) {
            placed.putIfAbsent(node.path(), node);
        }
        return new DocxLayoutMetrics(paths, repeated, fragments, placed, layout.fragments(), layout.totalPages());
    }

    /** Walks every place a node stands — in the flow, and in a table's composed cells — adding those seen twice. */
    private static void countPlaces(DocumentNode node, java.util.Set<DocumentNode> seen,
                                    java.util.Set<DocumentNode> repeated) {
        if (node == null) {
            return;
        }
        if (!seen.add(node)) {
            repeated.add(node);
        }
        if (node instanceof TableNode table) {
            for (List<DocumentTableCell> row : table.rows()) {
                for (DocumentTableCell cell : row) {
                    if (cell != null) {
                        countPlaces(cell.content(), seen, repeated);
                    }
                }
            }
        }
        for (DocumentNode child : node.children()) {
            countPlaces(child, seen, repeated);
        }
    }

    /**
     * Whether one instance of a node stands at more than one place: added twice to the flow, or
     * in the flow and in a table's composed cell. The layout lays out each place apart, at a width
     * of its own, and this index, keyed by the node, finds the lines of only one of them.
     */
    boolean placedMoreThanOnce(DocumentNode node) {
        return repeated.contains(node);
    }

    /**
     * How many pages the layout ran to.
     *
     * @return the page count, or 0 when there is no layout behind this index
     */
    int pageCount() {
        return pageCount;
    }

    /**
     * Rebuilds {@code LayoutCompiler.pathFor} over the authored tree.
     *
     * <p>The rule is the compiler's: a node is named by its own name when it has one and by
     * its kind when it does not, suffixed with its index among its siblings, and joined to
     * its parent's path with a slash. Separators in a name are replaced the same way, so a
     * name carrying one cannot collide with the structure.</p>
     */
    private static void indexPaths(DocumentNode node,
                                   String parentPath,
                                   int childIndex,
                                   Map<DocumentNode, String> into) {
        if (node == null) {
            return;
        }
        String base = node.name() == null || node.name().isBlank() ? node.nodeKind() : node.name().trim();
        String segment = base.replace('\\', '_').replace('/', '_') + "[" + childIndex + "]";
        String path = parentPath == null ? segment : parentPath + "/" + segment;
        into.put(node, path);
        List<DocumentNode> children = node.children();
        for (int index = 0; index < children.size(); index++) {
            indexPaths(children.get(index), path, index, into);
        }
    }

    /** @return true when there is no layout behind this index */
    boolean isEmpty() {
        return fragments.isEmpty();
    }

    /**
     * The height of one line of a table cell's text, as the layout measured it.
     *
     * <p>A paragraph's line height is on its own fragment; a table cell's is on the resolved
     * cell inside its row's fragment. Cells are found by the name the layout gives them —
     * the table's name, or its node kind when it has none, then the row and the column —
     * because a composed cell's nested table emits its rows under the <em>owner's</em> path,
     * so position alone would find the inner table's cells as readily as the outer's.</p>
     *
     * @param table  the table node
     * @param row    the cell's logical row
     * @param column the cell's first column
     * @return the measured height, or empty when the layout does not carry one
     */
    OptionalDouble cellLineHeight(DocumentNode table, int row, int column) {
        String owner = table.name() == null || table.name().isBlank() ? table.nodeKind() : table.name();
        Double measured = cellLineHeightsOf(table).get(owner + "__row_" + row + "__cell_" + column);
        return measured == null ? OptionalDouble.empty() : OptionalDouble.of(measured);
    }

    /**
     * Every measured cell of one table, by name — built the first time a cell of that table is
     * asked about.
     *
     * <p>Scanning the table's rows once per cell made a table's export quadratic in its size:
     * measured, doubling a 1000-row table's rows took its export from 545ms to 1463ms. One
     * pass per table and a lookup per cell keeps it linear.</p>
     */
    private Map<String, Double> cellLineHeightsOf(DocumentNode table) {
        return cellLineHeights.computeIfAbsent(table, node -> {
            Map<String, Double> byName = new HashMap<>();
            for (PlacedFragment fragment : fragmentsOf(node)) {
                if (fragment.payload() instanceof TableRowFragmentPayload payload) {
                    for (TableResolvedCell cell : payload.cells()) {
                        if (cell.hasMeasuredLineHeight()) {
                            byName.putIfAbsent(cell.name(), cell.lineHeight());
                        }
                    }
                }
            }
            return byName;
        });
    }

    /**
     * Where the layout placed one of a table's cells, found by its name among the table's own
     * rows (see {@link #ownRows}): the page and the box, in the page's points, y up.
     *
     * @param page   the page, counted within the section
     * @param left   the cell's left edge
     * @param bottom the cell's bottom edge
     * @param right  the cell's right edge
     * @param top    the cell's top edge
     */
    record CellBox(int page, double left, double bottom, double right, double top) {

        /** Whether a fragment lies in the box on its page, a point either side allowed. */
        boolean holds(PlacedFragment fragment) {
            double slack = 1;
            return fragment.pageIndex() == page
                   && fragment.x() >= left - slack && fragment.x() + fragment.width() <= right + slack
                   && fragment.y() >= bottom - slack && fragment.y() + fragment.height() <= top + slack;
        }
    }

    /**
     * Every box the layout placed one of a table's cells in, in the order it placed them: one
     * for a row placed once, one a page for a header repeated on every page, as many as the
     * layout placed it otherwise; empty when it placed none by that name.
     *
     * @param table  the table node
     * @param row    the cell's logical row
     * @param column the cell's first column
     */
    List<CellBox> cellBoxes(DocumentNode table, int row, int column) {
        String owner = table.name() == null || table.name().isBlank() ? table.nodeKind() : table.name();
        return cellBoxes.computeIfAbsent(table, node -> {
            Map<String, List<CellBox>> byName = new HashMap<>();
            // Only the table's own rows: a table composed in one of its cells, unnamed as it may
            // be, emits rows under the owner's path whose cells carry the owner's names.
            for (PlacedFragment fragment : ownRows(node)) {
                if (fragment.payload() instanceof TableRowFragmentPayload payload) {
                    for (TableResolvedCell cell : payload.cells()) {
                        double bottom = fragment.y() + cell.yOffset();
                        double left = fragment.x() + cell.x();
                        byName.computeIfAbsent(cell.name(), name -> new ArrayList<>()).add(
                                new CellBox(fragment.pageIndex(), left, bottom, left + cell.width(), bottom + cell.height()));
                    }
                }
            }
            return byName;
        }).getOrDefault(owner + "__row_" + row + "__cell_" + column, List.of());
    }

    /**
     * Whether the layout placed one of a table's rows.
     *
     * <p>A row is found by the name its cells carry — the table's name, or its node kind when
     * it has none, then {@code __row_} and the row's index in the table — and only among the
     * table's own rows (see {@link #ownRows}), because a nested table with no name of its own
     * carries the same kind as the owner.</p>
     *
     * @param table the table node
     * @param row   the row's index in the table
     * @return true when the layout carries a placement for that row
     */
    boolean placedRow(DocumentNode table, int row) {
        return placedRowsOf(table).containsKey(String.valueOf(row));
    }

    /**
     * The page the layout placed one of a table's rows on, found as {@link #placedRow} finds it.
     *
     * @param table the table node
     * @param row   the row's index in the table
     * @return the page, counted within the section, or -1 when the row was not placed
     */
    int rowPage(DocumentNode table, int row) {
        return placedRowsOf(table).getOrDefault(String.valueOf(row), -1);
    }

    /**
     * How tall the layout made one of a table's rows, found as {@link #placedRow} finds it.
     *
     * <p>A row repeated on every page — a header — is the same height each time. A row whose
     * placements disagree is answered as unknown rather than with one of them.</p>
     *
     * @param table the table node
     * @param row   the row's index in the table
     * @return the height in points, or empty when the row was not placed or its height is unclear
     */
    OptionalDouble rowHeight(DocumentNode table, int row) {
        Double height = rowHeightsOf(table).get(String.valueOf(row));
        return height == null || height.isNaN() ? OptionalDouble.empty() : OptionalDouble.of(height);
    }

    private Map<String, Double> rowHeightsOf(DocumentNode table) {
        return rowHeights.computeIfAbsent(table, node -> {
            Map<String, Double> heights = new HashMap<>();
            String prefix = (node.name() == null || node.name().isBlank()
                    ? node.nodeKind()
                    : node.name()) + "__row_";
            for (PlacedFragment fragment : ownRows(node)) {
                String name = ((TableRowFragmentPayload) fragment.payload()).cells().get(0).name();
                int end = name == null || !name.startsWith(prefix)
                        ? -1
                        : name.indexOf("__cell_", prefix.length());
                if (end >= 0) {
                    heights.merge(name.substring(prefix.length(), end), fragment.height(),
                            (first, next) -> Math.abs(first - next) <= 0.01 ? first : Double.NaN);
                }
            }
            return heights;
        });
    }

    /**
     * The index part of each placed row's name, kept as the text the layout wrote rather than
     * parsed: a row is looked up by writing its index the same way, so a name that only
     * resembles the pattern matches nothing instead of failing the export. Each is kept with
     * the page it was placed on.
     */
    private Map<String, Integer> placedRowsOf(DocumentNode table) {
        return placedRows.computeIfAbsent(table, node -> {
            Map<String, Integer> rows = new HashMap<>();
            String prefix = (node.name() == null || node.name().isBlank()
                    ? node.nodeKind()
                    : node.name()) + "__row_";
            for (PlacedFragment fragment : ownRows(node)) {
                String name = ((TableRowFragmentPayload) fragment.payload()).cells().get(0).name();
                int end = name == null || !name.startsWith(prefix)
                        ? -1
                        : name.indexOf("__cell_", prefix.length());
                if (end >= 0) {
                    rows.putIfAbsent(name.substring(prefix.length(), end), fragment.pageIndex());
                }
            }
            return rows;
        });
    }

    /**
     * Whether the layout placed a node at all.
     *
     * @param node any authored node
     * @return true when the layout carries a placement for it
     */
    boolean placed(DocumentNode node) {
        return placedFor(node) != null;
    }

    /**
     * How wide the layout placed a node's box.
     *
     * @param node any authored node
     * @return the width in points, or empty when the layout placed nothing for it
     */
    OptionalDouble placedWidth(DocumentNode node) {
        PlacedNode placedNode = placedFor(node);
        return placedNode == null || placedNode.placementWidth() <= 0
                ? OptionalDouble.empty()
                : OptionalDouble.of(placedNode.placementWidth());
    }

    /**
     * The space the layout puts between two lines of a node's text, beyond the lines' own
     * height — a paragraph's or a list's {@code lineSpacing}.
     *
     * @param node any node that lays its text out as paragraph lines
     * @return the gap in points, 0 when there is none or nothing was laid out
     */
    double lineGap(DocumentNode node) {
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                return Math.max(0, paragraph.lineGap());
            }
        }
        return 0;
    }

    /**
     * The lines each item of a list was laid out on, in the order the items were placed.
     *
     * <p>A list lays each item out as a fragment of its own. A list whose markers stand in a
     * column of their own lays the marker out as one more fragment, level with its item's
     * text and as tall: the two are one item, its text's lines the item's, and the marker's
     * single line does not make the item a wrapped one. The text is the fragment to the right,
     * or the later where the two start together — a marker that draws nothing, with no gap.</p>
     *
     * @param list a list node
     * @return every item's text, empty when the list laid out nothing
     */
    List<ItemText> itemLines(DocumentNode list) {
        List<ItemText> items = new ArrayList<>();
        PlacedFragment previous = null;
        for (PlacedFragment fragment : fragmentsOf(list)) {
            if (!(fragment.payload() instanceof ParagraphFragmentPayload paragraph)) {
                continue;
            }
            boolean sameItem = previous != null
                               && previous.pageIndex() == fragment.pageIndex()
                               && Math.abs(previous.y() - fragment.y()) < 0.01
                               && Math.abs(previous.height() - fragment.height()) < 0.01;
            ItemText text = new ItemText(paragraph.lines(), fragment.x() + paragraph.padding().left());
            if (!sameItem) {
                items.add(text);
            } else if (text.x() >= items.get(items.size() - 1).x()) {
                items.set(items.size() - 1, text);
            }
            previous = fragment;
        }
        return items;
    }

    /**
     * An item's text as a list laid it out.
     *
     * @param lines its lines
     * @param x     where its text starts across the page, in points
     */
    record ItemText(List<ParagraphLine> lines, double x) {
    }

    /**
     * How many lines the layout set a node's text on, over every page it reached.
     *
     * @param node any node that lays its text out as paragraph lines
     * @return the number of lines, 0 when it laid out none
     */
    int lineCount(DocumentNode node) {
        int lines = 0;
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                lines += paragraph.lines().size();
            }
        }
        return lines;
    }

    /**
     * Where the layout placed a node's box.
     *
     * @param node any authored node
     * @return the placement, or {@code null} when the layout placed nothing for it
     */
    PlacedNode placement(DocumentNode node) {
        return placedFor(node);
    }

    /**
     * Whether the layout placed a node, and placed all of it on one page.
     *
     * @param node any authored node
     * @return true when the node starts and ends on the same page
     */
    boolean onOnePage(DocumentNode node) {
        PlacedNode placedNode = placedFor(node);
        return placedNode != null && placedNode.startPage() == placedNode.endPage();
    }

    /**
     * How far a page zone's content sits from the page edge it belongs to, as laid out on
     * the pages it is drawn on.
     *
     * <p>Word places a footer by the distance from the page's bottom edge to the bottom of
     * the footer, and a header by the distance from the top edge to the top of the header.
     * The engine does not state either: a zone is a band of a given height against the
     * edge, and its content is laid out inside it from the top. So the distance is read
     * from where the content actually landed — the lowest edge of a footer's fragments, the
     * highest edge of a header's — rather than rebuilt from the band's parts.</p>
     *
     * <p>Zone fragments are spliced into the graph under {@code @page-zone[page][index]},
     * outside the node paths this index is built from, so they are found by that prefix — on
     * any page, because a zone that skips the first page has nothing on it. A band sits at the
     * same place on every page it is drawn on, so every page gives the same distance.</p>
     *
     * @param zoneIndex  the zone's position in the session's zone list
     * @param header     whether it is a header, measured from the top edge
     * @param pageHeight the page's height in points
     * @return the distance in points, or empty when the layout carries no such zone
     */
    OptionalDouble zoneDistanceFromEdge(int zoneIndex, boolean header, double pageHeight) {
        java.util.regex.Pattern zone =
                java.util.regex.Pattern.compile("^@page-zone\\[\\d+]\\[" + zoneIndex + "]");
        double lowest = Double.POSITIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, List<PlacedFragment>> entry : fragments.entrySet()) {
            if (!zone.matcher(entry.getKey()).find()) {
                continue;
            }
            for (PlacedFragment fragment : entry.getValue()) {
                lowest = Math.min(lowest, fragment.y());
                highest = Math.max(highest, fragment.y() + fragment.height());
            }
        }
        if (lowest == Double.POSITIVE_INFINITY) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(header ? pageHeight - highest : lowest);
    }

    /**
     * The text a page zone's content laid out, on the first page the zone is drawn on: each
     * node's first fragment of paragraph lines, by its path within the content.
     *
     * <p>A zone's content is compiled on its own, its paths as a document's of that one root,
     * and spliced in under {@code @page-zone[page][index]} (see {@link #zoneDistanceFromEdge}).
     * The export builds the content again, so it finds its nodes here by those paths
     * ({@link #pathsWithin}).</p>
     *
     * @param zoneIndex the zone's position in the section's zone list
     * @return the fragments by path, empty when the layout carries no such zone
     */
    Map<String, PlacedFragment> zoneText(int zoneIndex) {
        int first = zoneFirstPage(zoneIndex);
        if (first < 0) {
            return Map.of();
        }
        String prefix = "@page-zone[" + first + "][" + zoneIndex + "]";
        Map<String, PlacedFragment> text = new HashMap<>();
        for (Map.Entry<String, List<PlacedFragment>> entry : fragments.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            for (PlacedFragment fragment : entry.getValue()) {
                if (fragment.payload() instanceof ParagraphFragmentPayload paragraph && !paragraph.lines().isEmpty()) {
                    text.putIfAbsent(entry.getKey().substring(prefix.length()), fragment);
                    break;
                }
            }
        }
        return text;
    }

    /**
     * The first page a page zone is drawn on, which {@link #zoneText} reads it from.
     *
     * @param zoneIndex the zone's position in the section's zone list
     * @return the page's index, counted from 0, or -1 when the layout carries no such zone
     */
    int zoneFirstPage(int zoneIndex) {
        java.util.regex.Pattern zone =
                java.util.regex.Pattern.compile("^@page-zone\\[\\d+]\\[" + zoneIndex + "]");
        // The page a zone's fragment is drawn on is the one its path names.
        int first = Integer.MAX_VALUE;
        for (Map.Entry<String, List<PlacedFragment>> entry : fragments.entrySet()) {
            if (zone.matcher(entry.getKey()).find()) {
                for (PlacedFragment fragment : entry.getValue()) {
                    first = Math.min(first, fragment.pageIndex());
                }
            }
        }
        return first == Integer.MAX_VALUE ? -1 : first;
    }

    /**
     * The paths the layout gives a tree compiled as a document of its own root: a page zone's
     * content, which the layout lays out apart from the body.
     *
     * @param root the tree's root
     * @return each node's path
     */
    static Map<DocumentNode, String> pathsWithin(DocumentNode root) {
        Map<DocumentNode, String> paths = new IdentityHashMap<>();
        indexPaths(root, null, 0, paths);
        return paths;
    }

    /**
     * The path the layout graph addresses a node by, for a note that has to say where in
     * the document it came from.
     *
     * @param node any authored node
     * @return its path, or null when this index was built from no graph at all
     */
    String pathOf(DocumentNode node) {
        return node == null ? null : paths.get(node);
    }

    /**
     * The height of one line of the paragraph's text, as the engine measured it.
     *
     * <p>The height of the text on the laid-out lines — the tallest of them, since Word has
     * one height for a paragraph and a shorter one would clip the rest — rather than the
     * paragraph's own style's. The two are the same while every run is set in the
     * paragraph's style. They part when the runs carry a style of their own: a skill rating
     * of dots with a 7.8pt space between each, in a paragraph left at the default size, is a
     * 9.4pt line on the page, and the style's 13pt made every skill row of a CV sidebar
     * 3.6pt taller in Word. Runs larger than the style make the line taller the same way,
     * where the style's height clipped them. An inline picture does not count; the lines
     * that hold one are made room for separately ({@code makeRoomForPictures}). A line with
     * no text at all — an empty line, one holding only a picture — has the style's height,
     * and counts with it. In a paragraph of more than one line, a blank {@code bulletOffset},
     * which the export writes as an indent, does not count towards its line
     * ({@link #writtenTextHeight}).</p>
     *
     * <p>Where the page sets the lines further apart than the tallest of them and the gap —
     * lines of different heights, each set its own height — the page's mean distance between
     * their baselines, less the gap, is the height instead: Word sets every line that far apart,
     * and the first and last lines' baselines stand that far apart on both.</p>
     *
     * <p>Read from every fragment the node emitted: a paragraph split across a page boundary
     * emits one fragment per page, and its tallest line may be on any of them.</p>
     *
     * @param node any node that lays its text out as paragraph lines
     * @return the line height in points, or empty when the node laid out nothing
     */
    OptionalDouble lineHeight(DocumentNode node) {
        return lineHeight(node, true, true);
    }

    /**
     * Whether a node's line height is other than its tallest laid-out line ({@link #lineHeight}):
     * the page's distance between its lines, or a line without its prefix. The paragraph, set at
     * that one height, can then come out shorter than the page's; at the tallest line it cannot.
     *
     * @param node any node that lays its text out as paragraph lines
     * @return whether the height is not the tallest line's
     */
    boolean lineIsNotTheTallest(DocumentNode node) {
        OptionalDouble written = lineHeight(node, true, true);
        OptionalDouble tallest = lineHeight(node, false, false);
        return written.isPresent() && tallest.isPresent()
               && Math.abs(written.getAsDouble() - tallest.getAsDouble()) > 0.01;
    }

    private OptionalDouble lineHeight(DocumentNode node, boolean byPitch, boolean withoutPrefix) {
        double text = 0;
        double style = 0;
        double pitches = 0;
        int pairs = 0;
        double heights = 0;
        int textLines = 0;
        // Only lines on one fragment, of more than one line, are written other than at their
        // tallest: what they then fall short of the page is owed below them (linesHeight), and a
        // line of its own is set at its laid-out height where nothing would make up a shorter one
        // (riseIntoItsLine, holdPicturesInTheLine).
        boolean ownWay = textFragmentsOf(node).size() == 1 && lineCount(node) > 1;
        // The layout sets the prefix before the lines its strategy names, and only there.
        com.demcha.compose.document.style.DocumentTextIndent strategy =
                withoutPrefix && ownWay && node instanceof ParagraphNode paragraphNode
                && !paragraphNode.bulletOffset().isEmpty()
                        ? paragraphNode.indentStrategy()
                        : com.demcha.compose.document.style.DocumentTextIndent.NONE;
        boolean firstPrefixed = strategy == com.demcha.compose.document.style.DocumentTextIndent.FIRST_LINE
                                || strategy == com.demcha.compose.document.style.DocumentTextIndent.ALL_LINES;
        boolean wrappedPrefixed = strategy == com.demcha.compose.document.style.DocumentTextIndent.FROM_SECOND_LINE
                                  || strategy == com.demcha.compose.document.style.DocumentTextIndent.ALL_LINES;
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                ParagraphLine above = null;
                for (ParagraphLine line : paragraph.lines()) {
                    boolean prefixed = above == null ? firstPrefixed : wrappedPrefixed;
                    text = Math.max(text, prefixed ? writtenTextHeight(line) : line.textLineHeight());
                    if (setByText(line)) {
                        heights += line.lineHeight();
                        textLines++;
                    }
                    // A line a picture makes taller is made room for on its own (makeRoomForPictures).
                    if (above != null && setByText(above) && setByText(line)) {
                        pitches += above.baselineOffsetFromBottom() + Math.max(0, paragraph.lineGap())
                                   + line.lineHeight() - line.baselineOffsetFromBottom();
                        pairs++;
                    }
                    above = line;
                }
                if (style <= 0) {
                    style = paragraph.lineHeight();
                }
            }
        }
        if (text > 0) {
            // Word sets every line of a paragraph one height apart; the page sets each pair of
            // lines its own distance apart. Where those differ, the page's mean distance — less
            // the gap Word is given on top (applyLineGap) — keeps the first and last lines where
            // the page has them, never below the tallest written line, which would clip it. Nor
            // above the lines' mean height: the paragraph would come out taller than the page,
            // and a surplus cannot be owed back.
            if (byPitch && ownWay && pairs > 0 && textLines > 0) {
                double pitch = pitches / pairs - Math.max(0, lineGap(node));
                text = Math.max(text, Math.min(pitch, heights / textLines));
            }
            return OptionalDouble.of(text);
        }
        return style > 0 ? OptionalDouble.of(style) : OptionalDouble.empty();
    }

    /**
     * How tall the page sets a node's lines together: each line's own height and the gap
     * between each two. Empty when the lines are not on one fragment, or a picture makes one
     * of them taller than its text — those are made room for on their own.
     *
     * @param node any node that lays its text out as paragraph lines
     * @return the height in points, or empty
     */
    OptionalDouble linesHeight(DocumentNode node) {
        List<PlacedFragment> fragments = textFragmentsOf(node);
        if (fragments.size() != 1 || !(fragments.get(0).payload() instanceof ParagraphFragmentPayload paragraph)
            || paragraph.lines().isEmpty()) {
            return OptionalDouble.empty();
        }
        double height = Math.max(0, paragraph.lineGap()) * (paragraph.lines().size() - 1);
        for (ParagraphLine line : paragraph.lines()) {
            if (!setByText(line)) {
                return OptionalDouble.empty();
            }
            height += line.lineHeight();
        }
        return OptionalDouble.of(height);
    }

    /**
     * The page's mean distance between the baselines of a node's lines, the gap included, over
     * each two neighbouring lines as tall as their text. Empty when the lines are not on one
     * fragment, or no two neighbours are.
     *
     * @param node any node that lays its text out as paragraph lines
     * @return the distance in points, or empty
     */
    OptionalDouble linePitch(DocumentNode node) {
        List<PlacedFragment> fragments = textFragmentsOf(node);
        if (fragments.size() != 1 || !(fragments.get(0).payload() instanceof ParagraphFragmentPayload paragraph)) {
            return OptionalDouble.empty();
        }
        double pitches = 0;
        int pairs = 0;
        ParagraphLine above = null;
        for (ParagraphLine line : paragraph.lines()) {
            if (above != null && setByText(above) && setByText(line)) {
                pitches += above.baselineOffsetFromBottom() + Math.max(0, paragraph.lineGap())
                           + line.lineHeight() - line.baselineOffsetFromBottom();
                pairs++;
            }
            above = line;
        }
        return pairs == 0 ? OptionalDouble.empty() : OptionalDouble.of(pitches / pairs);
    }

    /** Whether a line is as tall as its text, nothing inline making it taller. */
    private static boolean setByText(ParagraphLine line) {
        return Math.abs(line.lineHeight() - line.textLineHeight()) < 0.01;
    }

    /**
     * The height of a line's text the export writes: the line's own, less a leading span of
     * blank prefix the page sets in the paragraph's style ({@code bulletOffset}), which the
     * export writes as an indent and not as text. The page measures that span with the rest,
     * so a prefix in a face taller than the runs made a wrapped line taller than its text.
     */
    private static double writtenTextHeight(ParagraphLine line) {
        List<com.demcha.compose.document.layout.payloads.ParagraphSpan> spans = line.spans();
        if (spans.size() < 2
            || !(spans.get(0) instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan prefix)
            || !prefix.text().isBlank()) {
            return line.textLineHeight();
        }
        double height = 0;
        for (int i = 1; i < spans.size(); i++) {
            if (spans.get(i) instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan span) {
                height = Math.max(height, span.height());
            }
        }
        return height > 0 ? Math.min(height, line.textLineHeight()) : line.textLineHeight();
    }

    /**
     * The first line a node laid out as paragraph lines: its height, its text's ascent and
     * how far its baseline sits above its bottom.
     *
     * @param node any node that lays out as paragraph lines
     * @return the line, or empty when the node laid out nothing
     */
    java.util.Optional<ParagraphLine> firstLine(DocumentNode node) {
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph && !paragraph.lines().isEmpty()) {
                return java.util.Optional.of(paragraph.lines().get(0));
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * How far right of its marker a list's first item's text starts, as the page sets it: the
     * marker's width and the gap after it. A list whose marker the page draws is laid out as
     * a fragment for the marker and one for the item's text beside it.
     *
     * @param list the list
     * @return the distance in points, or empty when the list laid out no marker beside its text
     */
    OptionalDouble markerToText(DocumentNode list) {
        return markerToText(list, null);
    }

    /**
     * How far right of its marker a list's first item's text starts, the marker drawn or, when
     * {@code markerText} is given, a marker of that text laid out as a fragment of its own.
     *
     * @param list       the list
     * @param markerText the marker's letters, or {@code null} for a drawn marker
     * @return the distance in points, or empty when the list laid out no such marker beside its text
     */
    OptionalDouble markerToText(DocumentNode list, String markerText) {
        List<PlacedFragment> own = fragmentsOf(list);
        int index = markerFragment(own, markerText);
        return index < 0 ? OptionalDouble.empty() : OptionalDouble.of(own.get(index + 1).x() - own.get(index).x());
    }

    /**
     * How wide a list's first item's marker is as the page sets it, the marker drawn or of the
     * text given (see {@link #markerToText(DocumentNode, String)}).
     */
    OptionalDouble markerWidth(DocumentNode list, String markerText) {
        List<PlacedFragment> own = fragmentsOf(list);
        int index = markerFragment(own, markerText);
        if (index < 0 || !(own.get(index).payload() instanceof ParagraphFragmentPayload marker)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(marker.lines().stream().mapToDouble(ParagraphLine::width).max().orElse(0));
    }

    /** Where among a list's fragments its first marker beside its text is, or -1. */
    private static int markerFragment(List<PlacedFragment> own, String markerText) {
        for (int index = 0; index + 1 < own.size(); index++) {
            if (own.get(index).payload() instanceof ParagraphFragmentPayload marker
                && own.get(index + 1).payload() instanceof ParagraphFragmentPayload text
                && isMarker(marker, markerText) && !isMarker(text, markerText) && !drawsOnly(text)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isMarker(ParagraphFragmentPayload paragraph, String markerText) {
        if (markerText == null) {
            return drawsOnly(paragraph);
        }
        String letters = markerText.strip();
        return !letters.isEmpty() && paragraph.lines().size() == 1
               && paragraph.lines().get(0).text().strip().equals(letters);
    }

    /** Whether a paragraph fragment holds nothing but drawings — a list's drawn marker. */
    private static boolean drawsOnly(ParagraphFragmentPayload paragraph) {
        return !paragraph.lines().isEmpty() && paragraph.lines().stream().allMatch(line -> !line.spans().isEmpty()
                && line.spans().stream().noneMatch(span -> span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan));
    }

    /**
     * Where a node's first line of text starts, measured up from the foot of its page: the top
     * of the first fragment holding its lines, inside the paragraph's padding, where the page
     * starts setting them.
     *
     * @param node any node that lays out as paragraph lines
     * @return the top in points, or empty when the node laid out nothing
     */
    OptionalDouble firstLineTop(DocumentNode node) {
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph && !paragraph.lines().isEmpty()) {
                return OptionalDouble.of(ParagraphLineGeometry.contentTop(fragment.y(), fragment.height(),
                        paragraph.padding().top()));
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * Where the page sets a node's first lines of text: the content box of the first fragment
     * holding them, inside the paragraph's padding.
     *
     * @param node any node that lays out as paragraph lines
     * @return the box, or empty when the node laid out nothing
     */
    java.util.Optional<TextBox> firstTextBox(DocumentNode node) {
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph && !paragraph.lines().isEmpty()) {
                double top = ParagraphLineGeometry.contentTop(fragment.y(), fragment.height(), paragraph.padding().top());
                ParagraphLine first = paragraph.lines().get(0);
                return java.util.Optional.of(new TextBox(fragment.pageIndex(),
                        fragment.x() + paragraph.padding().left(),
                        fragment.x() + fragment.width() - paragraph.padding().right(),
                        top, top - first.lineHeight() + first.baselineOffsetFromBottom()));
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * The content box of a node's first fragment of text, and its first line's baseline.
     *
     * @param page     the page, counted within the section
     * @param left     its left edge, from the page's left edge
     * @param right    its right edge, from the page's left edge
     * @param top      its top edge, measured up from the foot of the page
     * @param baseline its first line's baseline, measured up from the foot of the page
     */
    record TextBox(int page, double left, double right, double top, double baseline) {
    }

    /**
     * Every line a node laid out, page after page, in order.
     *
     * @param node any node that lays out as paragraph lines
     * @return the lines, empty when the node laid out none
     */
    List<ParagraphLine> lines(DocumentNode node) {
        List<ParagraphLine> lines = new ArrayList<>();
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                lines.addAll(paragraph.lines());
            }
        }
        return lines;
    }

    /**
     * The width of a node's widest laid-out line when every line of it is one word: a word with
     * nowhere to break — an address, a link — longer than the width it is given is set whole,
     * wider than that width. A node with a line of several words has none: the page broke it
     * where it fits, and the room a long word would take would let those lines take more words.
     *
     * @param node any node that lays out as paragraph lines
     * @return the width in points, 0 when any line holds more than one word or none is laid out
     */
    double unbrokenWidth(DocumentNode node) {
        double widest = 0;
        for (PlacedFragment fragment : textFragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                for (ParagraphLine line : paragraph.lines()) {
                    String text = line.text().strip();
                    if (text.chars().anyMatch(Character::isWhitespace)) {
                        return 0;
                    }
                    if (!text.isEmpty()) {
                        widest = Math.max(widest, line.width());
                    }
                }
            }
        }
        return widest;
    }

    /**
     * The text a node laid out as paragraph lines, as the layout wrote it.
     *
     * <p>A page reference's number is known only once the document is paginated, so the
     * layout resolves it and lays out the number as text; this reads that text back rather
     * than resolving the page a second time.</p>
     *
     * @param node any node that lays out as paragraph lines
     * @return the laid-out text, or empty when the node laid out nothing
     */
    java.util.Optional<String> laidOutText(DocumentNode node) {
        for (PlacedFragment fragment : fragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph
                && !paragraph.lines().isEmpty()) {
                StringBuilder text = new StringBuilder();
                paragraph.lines().forEach(line -> text.append(line.text()));
                return java.util.Optional.of(text.toString());
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * The resolved width of every column of a table.
     *
     * <p>Derived from the cells rather than from the column specs, because that is where
     * the answer is: an {@code auto} column's width is its content's, and the resolved
     * cells carry the width it came out at. The boundaries are collected across every row
     * so a row whose cells span columns contributes its edges without hiding the ones a
     * plainer row shows.</p>
     *
     * @param node the table being written
     * @return one width per column in order, or {@code null} when the table laid out nothing
     */
    double[] tableColumns(DocumentNode node, int columnCount) {
        PlacedNode placedTable = placedFor(node);
        if (placedTable == null || placedTable.placementWidth() <= 0) {
            return null;
        }
        double width = rowsWidth(placedTable);

        TreeSet<Double> boundaries = new TreeSet<>();
        for (PlacedFragment fragment : ownRows(node)) {
            for (TableResolvedCell cell : ((TableRowFragmentPayload) fragment.payload()).cells()) {
                boundaries.add(round(cell.x()));
            }
        }
        if (boundaries.isEmpty()) {
            return null;
        }
        boundaries.add(round(width));
        double[] widths = widthsBetween(new ArrayList<>(boundaries));
        // The grid has to be the table's own. A column count that disagrees with the one
        // the table resolves means something else contributed a boundary, and a wrong grid
        // is worse than none: Word would place every column edge where it was told.
        return widths != null && widths.length == columnCount ? widths : null;
    }

    /**
     * Where each of a row's children starts, and how wide the row is.
     *
     * <p>A row divides its width by weights, by an even split, by explicit columns or by
     * the flex path, and two of those measure their children. The layout resolved whichever
     * applied, so this reads the answer instead of reproducing the rule.</p>
     *
     * <p>Only the <em>starts</em> are read, because only they are the slots'. A placed
     * child is as wide as its own content — a paragraph reading "Left" measures a few
     * points whatever slot it was given — so its width says nothing about where its column
     * ends. Where the next one begins does, and the gap between them is a number the row
     * itself states.</p>
     *
     * @param row the row being carried as a one-row table
     * @return {@code {rowWidth, x0, x1, ...}}, the starts relative to the row's left edge,
     *         or {@code null} when the row or one of its children laid out nothing
     */
    double[] rowChildStarts(DocumentNode row) {
        PlacedNode rowNode = placedFor(row);
        if (rowNode == null || rowNode.placementWidth() <= 0) {
            return null;
        }
        List<DocumentNode> children = row.children();
        if (children.isEmpty()) {
            return null;
        }
        double[] starts = new double[1 + children.size()];
        starts[0] = rowNode.placementWidth();
        for (int index = 0; index < children.size(); index++) {
            PlacedNode child = placedFor(children.get(index));
            if (child == null) {
                return null;
            }
            starts[1 + index] = child.placementX() - rowNode.placementX();
        }
        return starts;
    }

    /**
     * The row fragments that belong to the table itself, each carrying at least one cell.
     *
     * <p>A table whose cell is built from another table emits that inner table's rows under
     * the <em>owner's</em> path, so the fragments at one path are not all one table's. A row
     * of this table spans this table; a nested one stops short, and mixing the two produced a
     * grid with more columns than the table has.</p>
     */
    private List<PlacedFragment> ownRows(DocumentNode table) {
        PlacedNode placedTable = placedFor(table);
        if (placedTable == null || placedTable.placementWidth() <= 0) {
            return List.of();
        }
        double width = rowsWidth(placedTable);
        List<PlacedFragment> rows = new ArrayList<>();
        for (PlacedFragment fragment : fragmentsOf(table)) {
            if (!(fragment.payload() instanceof TableRowFragmentPayload row) || row.cells().isEmpty()) {
                continue;
            }
            double rowRight = 0;
            for (TableResolvedCell cell : row.cells()) {
                rowRight = Math.max(rowRight, cell.x() + cell.width());
            }
            if (Math.abs(rowRight - width) <= 0.5) {
                rows.add(fragment);
            }
        }
        return rows;
    }

    /**
     * How wide a table's rows are: its placement less its padding on either side, which the
     * page draws the rows inside. Measured against the placement itself, a table with side
     * padding matched none of its rows, and lost its grid, its row heights and its unbroken
     * rows to Word's own.
     */
    private static double rowsWidth(PlacedNode table) {
        return table.placementWidth() - table.padding().left() - table.padding().right();
    }

    private List<PlacedFragment> fragmentsOf(DocumentNode node) {
        String path = paths.get(node);
        return path == null ? List.of() : fragments.getOrDefault(path, List.of());
    }

    /**
     * The fragments that hold a node's lines of text: its own, or — for a paragraph composed
     * in a table cell, which has no path of its own — the one its table laid out for it.
     */
    private List<PlacedFragment> textFragmentsOf(DocumentNode node) {
        List<PlacedFragment> own = fragmentsOf(node);
        if (!own.isEmpty() || !(node instanceof ParagraphNode) || fragments.isEmpty()) {
            return own;
        }
        if (composedText == null) {
            composedText = matchComposedText();
        }
        PlacedFragment matched = composedText.get(node);
        return matched == null ? List.of() : List.of(matched);
    }

    /**
     * Pairs every paragraph composed in a table cell with the fragment its table laid out for it.
     *
     * <p>A composed cell's content is laid out under the table's path, so its paragraphs' lines
     * sit among the table's own fragments and not at a path of their own. Without them a
     * paragraph in a cell was written at the height Word gives the face — 13.4pt for a 9pt
     * Gothic A1 line the page sets at 9.1 — and every row of such a table ran taller than the
     * page's. Cells are laid out in order, and so is what each holds, so the paragraphs are
     * walked in that order and each takes the first fragment not yet taken whose text is
     * its own as authored; those left then take the first whose lines set their markdown as the
     * page reads it, marks dropped, in its faces and sizes. A paragraph whose text no fragment
     * carries so is left without one, as before.</p>
     */
    private Map<DocumentNode, PlacedFragment> matchComposedText() {
        Map<DocumentNode, PlacedFragment> matched = new IdentityHashMap<>();
        for (DocumentNode table : paths.keySet()) {
            if (!(table instanceof TableNode tableNode)) {
                continue;
            }
            List<ParagraphNode> paragraphs = new ArrayList<>();
            collectComposedParagraphs(tableNode, paragraphs);
            if (paragraphs.isEmpty()) {
                continue;
            }
            // One queue per text, in layout order: each paragraph takes the first of its own text
            // still waiting, so pairing a table is linear in its cells.
            Map<String, java.util.ArrayDeque<PlacedFragment>> byText = new HashMap<>();
            for (PlacedFragment fragment : fragmentsOf(table)) {
                if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                    StringBuilder text = new StringBuilder();
                    paragraph.lines().forEach(line -> text.append(line.text()));
                    byText.computeIfAbsent(comparable(text.toString()), key -> new java.util.ArrayDeque<>())
                            .add(fragment);
                }
            }
            List<ParagraphNode> unmatched = new ArrayList<>();
            for (ParagraphNode paragraph : paragraphs) {
                java.util.ArrayDeque<PlacedFragment> waiting = waitingFor(byText, authoredText(paragraph));
                if (waiting != null) {
                    matched.put(paragraph, waiting.poll());
                } else {
                    unmatched.add(paragraph);
                }
            }
            // Then by its text as the page reads its markdown, where its session does: marks
            // dropped, and only a fragment whose lines set the pieces as they are read. A paragraph
            // of the same text as authored may have taken this one's own fragment: the one left
            // is in another face or size, and taken, its lines would cut this one's letters.
            for (ParagraphNode paragraph : unmatched) {
                if (DocxMarkdown.mayRead(paragraph) && paragraph.textStyle() != null) {
                    List<DocxMarkdown.Piece> pieces = DocxMarkdown.read(paragraph.text(), paragraph.textStyle());
                    java.util.ArrayDeque<PlacedFragment> waiting = waitingFor(byText, DocxMarkdown.text(pieces));
                    PlacedFragment fragment = waiting == null ? null : waiting.stream()
                            .filter(candidate -> setsThePieces(paragraph, pieces, candidate)).findFirst().orElse(null);
                    if (fragment != null) {
                        waiting.remove(fragment);
                        matched.put(paragraph, fragment);
                    }
                }
            }
        }
        return matched;
    }

    /**
     * Whether a fragment's lines set a paragraph's markdown pieces as they are read
     * ({@link DocxMarkdown#laidOutIn}) — an auto-sized paragraph's at sizes in proportion
     * ({@link DocxMarkdown#scaleIn}), the size the page fits it to not known before its lines are
     * found. A fragment whose lines lead with a prefix's letters carries other text than the
     * pieces, and is never offered.
     */
    private static boolean setsThePieces(ParagraphNode paragraph, List<DocxMarkdown.Piece> pieces, PlacedFragment fragment) {
        List<ParagraphLine> lines = ((ParagraphFragmentPayload) fragment.payload()).lines();
        return paragraph.autoSize() != null
                ? !Double.isNaN(DocxMarkdown.scaleIn(pieces, lines, ""))
                : DocxMarkdown.laidOutIn(pieces, lines, "");
    }

    /** The fragments of a text still waiting for a paragraph, or {@code null} where none is. */
    private static java.util.ArrayDeque<PlacedFragment> waitingFor(Map<String, java.util.ArrayDeque<PlacedFragment>> byText,
                                                                  String text) {
        String key = comparable(text);
        java.util.ArrayDeque<PlacedFragment> waiting = key.isEmpty() ? null : byText.get(key);
        return waiting == null || waiting.isEmpty() ? null : waiting;
    }

    /** The paragraphs a table's composed cells hold, nested tables' included, in layout order. */
    private static void collectComposedParagraphs(TableNode table, List<ParagraphNode> into) {
        for (List<DocumentTableCell> row : table.rows()) {
            for (DocumentTableCell cell : row) {
                if (cell != null && cell.content() != null) {
                    collectParagraphs(cell.content(), into);
                }
            }
        }
    }

    private static void collectParagraphs(DocumentNode node, List<ParagraphNode> into) {
        if (node instanceof ParagraphNode paragraph) {
            into.add(paragraph);
            return;
        }
        if (node instanceof TableNode nested) {
            collectComposedParagraphs(nested, into);
        }
        for (DocumentNode child : node.children()) {
            collectParagraphs(child, into);
        }
    }

    private static String authoredText(ParagraphNode paragraph) {
        if (paragraph.inlineRuns() == null || paragraph.inlineRuns().isEmpty()) {
            return paragraph.text() == null ? "" : paragraph.text();
        }
        // As the layout writes a line's text: a run's text and a chip's, and nothing for a
        // picture, an icon or a shape. Leaving the paragraph out instead left its fragment for a
        // later paragraph of the same text to take, with this one's line.
        StringBuilder text = new StringBuilder();
        for (InlineRun run : paragraph.inlineRuns()) {
            if (run instanceof InlineTextRun textRun && textRun.text() != null) {
                text.append(textRun.text());
            } else if (run instanceof com.demcha.compose.document.node.InlineHighlightRun chip && chip.text() != null) {
                text.append(chip.text());
            }
        }
        return text.toString();
    }

    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");

    /** Text as both sides can agree on it: wrapping drops spaces, and a style may set capitals. */
    private static String comparable(String text) {
        return WHITESPACE.matcher(text).replaceAll("").toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Every line of text and every picture the layout set on one page, whatever node set it.
     *
     * @param page the page, as a fragment counts it
     * @return those fragments, empty when there are none or there is no layout
     */
    List<PlacedFragment> textOnPage(int page) {
        if (textByPage == null) {
            textByPage = new HashMap<>();
            for (List<PlacedFragment> atPath : fragments.values()) {
                for (PlacedFragment fragment : atPath) {
                    if (fragment.payload() instanceof ParagraphFragmentPayload
                        || fragment.payload() instanceof com.demcha.compose.document.layout.payloads.ImageFragmentPayload) {
                        textByPage.computeIfAbsent(fragment.pageIndex(), key -> new ArrayList<>()).add(fragment);
                    }
                }
            }
        }
        return textByPage.getOrDefault(page, List.of());
    }

    /**
     * The fragments a node painted itself, not its children's.
     *
     * @param node a node of the graph
     * @return its fragments in paint order, empty when it painted none or there is no layout
     */
    List<PlacedFragment> ownFragments(DocumentNode node) {
        return fragmentsOf(node);
    }

    /**
     * The colour the page shows under a node: what it painted before the node's first fragment,
     * at that fragment's centre, each fill laid over the one before on the page's white.
     *
     * <p>For a translucent colour Word can only hold opaque, flattened against it, so the file
     * shows on first opening the colour the page shows — a white rule at half strength over a
     * page's navy sidebar is a pale navy, not white.</p>
     *
     * @param node a node of the graph
     * @return that colour, or empty where nothing tells: no layout, no fragment of the node's
     *         own — content composed in a table cell has none — or something under it whose colour
     *         at that point the layout does not carry: a picture, a barcode, a gradient, a fill
     *         under a transform
     */
    java.util.Optional<java.awt.Color> colourUnder(DocumentNode node) {
        return colourUnder(paths.get(node));
    }

    /**
     * The colour the page shows under the node at a path, as {@link #colourUnder(DocumentNode)}.
     *
     * @param path a node's path, or {@code null}
     * @return that colour, or empty where nothing tells
     */
    java.util.Optional<java.awt.Color> colourUnder(String path) {
        List<PlacedFragment> own = path == null ? List.of() : fragments.getOrDefault(path, List.of());
        if (own.isEmpty()) {
            return java.util.Optional.empty();
        }
        PlacedFragment first = own.get(0);
        return colourUnder(first, first.x() + first.width() / 2, first.y() + first.height() / 2);
    }

    /**
     * The colour the page shows under one of a table's cells, at the cell's centre, painted
     * before the table's first row on the page where the cell first stands.
     *
     * @param table  the table node
     * @param row    the cell's logical row
     * @param column the cell's first column
     * @return that colour, or empty where nothing tells, as {@link #colourUnder(DocumentNode)}
     */
    java.util.Optional<java.awt.Color> colourUnderCell(DocumentNode table, int row, int column) {
        if (isEmpty()) {
            // Nothing to tell, and the index with no layout is shared: its caches stay empty.
            return java.util.Optional.empty();
        }
        List<CellBox> boxes = cellBoxes(table, row, column);
        if (boxes.isEmpty()) {
            return java.util.Optional.empty();
        }
        CellBox box = boxes.get(0);
        PlacedFragment firstRow = firstRows.computeIfAbsent(table, node -> {
            Map<Integer, PlacedFragment> byPage = new HashMap<>();
            for (PlacedFragment rowFragment : ownRows(node)) {
                byPage.putIfAbsent(rowFragment.pageIndex(), rowFragment);
            }
            return byPage;
        }).get(box.page());
        return firstRow == null ? java.util.Optional.empty()
                : colourUnder(firstRow, (box.left() + box.right()) / 2, (box.bottom() + box.top()) / 2);
    }

    /**
     * What the page painted at a point before a fragment, over its white, as Word shows it:
     * rectangles, ellipses, polygons, paths and table cells in their fill colours, a row's own fill
     * left out, as the export does not write it ({@code row paint}). A picture, a barcode, a
     * gradient, a fill drawn under a transform, or what the layout paints in a payload this does
     * not know, covering the point, leaves the colour unknown.
     */
    private java.util.Optional<java.awt.Color> colourUnder(PlacedFragment above, double x, double y) {
        List<PlacedFragment> page = paintedOn(above.pageIndex());
        int until = indexOf(page, above);
        if (until < 0) {
            return java.util.Optional.empty();
        }
        java.awt.Color colour = java.awt.Color.WHITE;
        int turned = 0;
        for (int index = 0; index < until; index++) {
            PlacedFragment fragment = page.get(index);
            Object payload = fragment.payload();
            if (payload instanceof com.demcha.compose.document.layout.payloads.TransformBeginPayload) {
                turned++;
                continue;
            }
            if (payload instanceof com.demcha.compose.document.layout.payloads.TransformEndPayload) {
                turned = Math.max(0, turned - 1);
                continue;
            }
            if (payload == null || PAINTS_NO_FILL.contains(payload.getClass()) || aRowsOwnFill(fragment)) {
                continue;
            }
            java.awt.Color fill = solidFillAt(fragment, x, y);
            if (!colourKnownAt(fragment, x, y) || (fill != null && turned > 0)) {
                return java.util.Optional.empty();
            }
            if (fill != null) {
                colour = DocxTranslucency.flatten(fill, colour);
            }
        }
        return java.util.Optional.of(colour);
    }

    /**
     * The payloads that paint no fill: text and strokes are ink over what is under them, and the
     * rest mark a place or open and close a clip.
     */
    private static final java.util.Set<Class<?>> PAINTS_NO_FILL = java.util.Set.of(
            ParagraphFragmentPayload.class,
            com.demcha.compose.document.layout.payloads.LineFragmentPayload.class,
            ShapeClipBeginPayload.class,
            ShapeClipEndPayload.class,
            com.demcha.compose.document.layout.payloads.AnchorMarkerPayload.class,
            com.demcha.compose.document.layout.payloads.BookmarkMarkerPayload.class,
            com.demcha.compose.document.layout.payloads.LayoutAnchorPayload.class);

    /** Whether a fragment is a row's own fill, which the export does not write. */
    private boolean aRowsOwnFill(PlacedFragment fragment) {
        if (!(fragment.payload() instanceof com.demcha.compose.document.layout.payloads.ShapeFragmentPayload)) {
            return false;
        }
        return nodesByPath().get(fragment.path()) instanceof com.demcha.compose.document.node.RowNode;
    }

    /** The nodes this index knows, by path, built when first asked. */
    private Map<String, DocumentNode> nodesByPath() {
        if (nodesByPath == null) {
            nodesByPath = new HashMap<>();
            paths.forEach((node, path) -> nodesByPath.putIfAbsent(path, node));
        }
        return nodesByPath;
    }

    /**
     * The solid colour a fragment fills a point with, or null where it fills none there — a
     * gradient included, which has no one colour ({@link #colourKnownAt} says so).
     */
    private static java.awt.Color solidFillAt(PlacedFragment fragment, double x, double y) {
        Object payload = fragment.payload();
        if (payload instanceof TableRowFragmentPayload row) {
            java.awt.Color fill = null;
            for (TableResolvedCell cell : row.cells()) {
                double left = fragment.x() + cell.x();
                double bottom = fragment.y() + cell.yOffset();
                if (cell.style() != null && cell.style().fillColor() != null
                    && x >= left && x <= left + cell.width() && y >= bottom && y <= bottom + cell.height()) {
                    fill = cell.style().fillColor();
                }
            }
            return fill;
        }
        if (payload instanceof com.demcha.compose.document.layout.payloads.ShapeFragmentPayload shape) {
            java.awt.Color fill = shape.fillPaint() == null ? shape.fillColor() : solidColourOf(shape.fillPaint());
            return fill != null && DocxInkOutline.box(fragment, shape.cornerRadius()).contains(x, y) ? fill : null;
        }
        if (payload instanceof com.demcha.compose.document.layout.payloads.EllipseFragmentPayload ellipse) {
            return ellipse.fillColor() != null && DocxInkOutline.ellipse(fragment.x(), fragment.y(),
                    fragment.width(), fragment.height()).contains(x, y) ? ellipse.fillColor() : null;
        }
        if (payload instanceof com.demcha.compose.document.layout.payloads.PolygonFragmentPayload polygon) {
            return polygon.fillColor() != null
                   && DocxInkOutline.polygon(polygon.points(), fragment).contains(x, y) ? polygon.fillColor() : null;
        }
        if (payload instanceof com.demcha.compose.document.layout.payloads.PathFragmentPayload path) {
            java.awt.Color fill = path.fillPaint() == null ? path.fillColor() : solidColourOf(path.fillPaint());
            return fill != null && DocxInkOutline.path(path.segments(), fragment).contains(x, y) ? fill : null;
        }
        return null;
    }

    /**
     * Whether the colour a fragment paints at a point is one the layout carries: false for a
     * gradient over the point, and for a picture, a barcode or a payload this does not know whose
     * box holds it.
     */
    private static boolean colourKnownAt(PlacedFragment fragment, double x, double y) {
        Object payload = fragment.payload();
        if (payload instanceof TableRowFragmentPayload
            || payload instanceof com.demcha.compose.document.layout.payloads.EllipseFragmentPayload
            || payload instanceof com.demcha.compose.document.layout.payloads.PolygonFragmentPayload) {
            return true;
        }
        if (payload instanceof com.demcha.compose.document.layout.payloads.ShapeFragmentPayload shape) {
            return shape.fillPaint() == null || solidColourOf(shape.fillPaint()) != null
                   || !DocxInkOutline.box(fragment, shape.cornerRadius()).contains(x, y);
        }
        if (payload instanceof com.demcha.compose.document.layout.payloads.PathFragmentPayload path) {
            return path.fillPaint() == null || solidColourOf(path.fillPaint()) != null
                   || !DocxInkOutline.path(path.segments(), fragment).contains(x, y);
        }
        return x < fragment.x() || x > fragment.x() + fragment.width()
               || y < fragment.y() || y > fragment.y() + fragment.height();
    }

    /** A paint's one colour, or null for a gradient, which has none. */
    private static java.awt.Color solidColourOf(com.demcha.compose.document.style.DocumentPaint paint) {
        return paint instanceof com.demcha.compose.document.style.DocumentPaint.Solid solid ? solid.color().color() : null;
    }

    /**
     * What a node clips, on each page it opens a clip: the fragment opening the clip, and what
     * the page paints after it until the clip closes — the node's layers, and anything they
     * clip in turn.
     *
     * @param node a node of the graph
     * @return its clips, empty when it opens none or there is no layout
     */
    List<Clip> clipsOf(DocumentNode node) {
        List<Clip> clips = new ArrayList<>();
        for (PlacedFragment opening : fragmentsOf(node)) {
            if (!(opening.payload() instanceof ShapeClipBeginPayload begin)) {
                continue;
            }
            List<PlacedFragment> page = paintedOn(opening.pageIndex());
            int from = indexOf(page, opening);
            if (from < 0) {
                continue;
            }
            int to = from + 1;
            // Up to the close of this clip; a clip the page never closes runs to the page's end.
            while (to < page.size() && !(page.get(to).payload() instanceof ShapeClipEndPayload end
                                         && end.ownerPath().equals(begin.ownerPath()))) {
                to++;
            }
            clips.add(new Clip(opening, page.subList(from + 1, to)));
        }
        return clips;
    }

    /**
     * A clip a node opens on a page.
     *
     * @param opening the fragment opening it, whose box the clip's outline is set in
     * @param painted what the page paints inside it, in paint order
     */
    record Clip(PlacedFragment opening, List<PlacedFragment> painted) {
    }

    private List<PlacedFragment> paintedOn(int page) {
        if (paintedByPage == null) {
            paintedByPage = new HashMap<>();
            for (PlacedFragment fragment : painted) {
                paintedByPage.computeIfAbsent(fragment.pageIndex(), key -> new ArrayList<>()).add(fragment);
            }
        }
        return paintedByPage.getOrDefault(page, List.of());
    }

    /** The position of a fragment in a page's list, by identity: two fragments may be equal. */
    private static int indexOf(List<PlacedFragment> page, PlacedFragment fragment) {
        for (int index = 0; index < page.size(); index++) {
            if (page.get(index) == fragment) {
                return index;
            }
        }
        return -1;
    }

    /**
     * The drawing the layout paints in a pass of its own rather than for a node of the tree: a
     * timeline's rail. The other passes are written as what they are — page backgrounds as page
     * fills, page zones as headers and footers, page fields as fields — so only the rail is
     * named; drawn again here, a header's band would stand in the body over the header's text.
     *
     * @return those fragments, by page
     */
    List<PlacedFragment> passFragments() {
        List<PlacedFragment> passes = new ArrayList<>();
        for (Map.Entry<String, List<PlacedFragment>> entry : fragments.entrySet()) {
            String path = entry.getKey();
            if (path != null && path.startsWith("@timeline-rail")) {
                passes.addAll(entry.getValue());
            }
        }
        // The index is a hash map: order by page, then path and index, so the output does not
        // depend on hash order.
        passes.sort(java.util.Comparator.comparingInt(PlacedFragment::pageIndex)
                .thenComparing(PlacedFragment::path)
                .thenComparingInt(PlacedFragment::fragmentIndex));
        return passes;
    }

    /**
     * How far inside its parent's content box the layout placed a node, on the left and on
     * the right, in points.
     *
     * <p>For content laid out in a column another node resolved — a timeline entry's body in
     * the header row's content column — whose place across the page is the layout's and not
     * the tree's.</p>
     *
     * @param node a placed node
     * @return {@code {left, right}}, never negative, or {@code null} when the node or its
     *         parent was not placed
     */
    double[] insideParent(DocumentNode node) {
        double[] content = parentContent(node);
        if (content == null) {
            return null;
        }
        PlacedNode box = placedFor(node);
        return new double[]{
                Math.max(0, box.placementX() - content[0]),
                Math.max(0, content[1] - (box.placementX() + box.placementWidth()))};
    }

    /**
     * The left and right edges of the content box of the node a node was placed in, as page
     * positions in points.
     *
     * @param node a placed node
     * @return {@code {left, right}}, or {@code null} when the node or its parent was not placed
     */
    double[] parentContent(DocumentNode node) {
        PlacedNode box = placedFor(node);
        if (box == null || box.parentPath() == null) {
            return null;
        }
        PlacedNode parent = placed.get(box.parentPath());
        if (parent == null) {
            return null;
        }
        return new double[]{parent.placementX() + parent.padding().left(),
                parent.placementX() + parent.placementWidth() - parent.padding().right()};
    }

    /**
     * Whether the layout placed something after a node in the same parent — a block under it
     * in a section.
     *
     * @param node a placed node
     * @return true when a later sibling was placed
     */
    boolean followedInItsParent(DocumentNode node) {
        PlacedNode box = placedFor(node);
        if (box == null || box.parentPath() == null) {
            return false;
        }
        if (lastChildIndex == null) {
            lastChildIndex = new HashMap<>();
            for (PlacedNode other : placed.values()) {
                if (other.parentPath() != null) {
                    lastChildIndex.merge(other.parentPath(), other.childIndex(), Math::max);
                }
            }
        }
        return lastChildIndex.getOrDefault(box.parentPath(), -1) > box.childIndex();
    }

    /** The highest child index the layout placed under each parent path, built when first asked. */
    private Map<String, Integer> lastChildIndex;

    /**
     * The node the layout placed a node in.
     *
     * @param node a placed node
     * @return its parent, or {@code null} when the node was not placed or its parent is not a
     *         node this index knows
     */
    DocumentNode parentOf(DocumentNode node) {
        PlacedNode box = placedFor(node);
        return box == null || box.parentPath() == null ? null : nodesByPath().get(box.parentPath());
    }

    /** The placed node for a semantic node, or null when this index knows neither. */
    private PlacedNode placedFor(DocumentNode node) {
        String path = paths.get(node);
        return path == null ? null : placed.get(path);
    }

    /** Consecutive differences of an ascending boundary list. */
    private static double[] widthsBetween(List<Double> boundaries) {
        if (boundaries.size() < 2) {
            return null;
        }
        double[] widths = new double[boundaries.size() - 1];
        for (int index = 0; index < widths.length; index++) {
            widths[index] = boundaries.get(index + 1) - boundaries.get(index);
        }
        return widths;
    }

    /**
     * Rounds to a hundredth of a point before two edges are compared.
     *
     * <p>Column boundaries are arrived at by addition, so the same edge reached along two
     * rows can differ in the last bits and split one column into two of nearly zero width.
     * A hundredth of a point is a twentieth of a twip: far below anything Word can write,
     * and far above the noise.</p>
     */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
