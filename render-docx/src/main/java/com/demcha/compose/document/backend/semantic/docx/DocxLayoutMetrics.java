package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
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
            new DocxLayoutMetrics(new IdentityHashMap<>(), Map.of(), Map.of());

    private final Map<DocumentNode, String> paths;
    private final Map<String, List<PlacedFragment>> fragments;
    private final Map<String, PlacedNode> placed;

    private DocxLayoutMetrics(Map<DocumentNode, String> paths,
                              Map<String, List<PlacedFragment>> fragments,
                              Map<String, PlacedNode> placed) {
        this.paths = paths;
        this.fragments = fragments;
        this.placed = placed;
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
        if (layout == null) {
            // No measurements, but the paths still name the nodes — which is what a
            // diagnostic note needs to say where in the document it came from.
            return new DocxLayoutMetrics(paths, Map.of(), Map.of());
        }
        Map<String, List<PlacedFragment>> fragments = new HashMap<>();
        for (PlacedFragment fragment : layout.fragments()) {
            fragments.computeIfAbsent(fragment.path(), key -> new ArrayList<>()).add(fragment);
        }
        Map<String, PlacedNode> placed = new HashMap<>();
        for (PlacedNode node : layout.nodes()) {
            placed.putIfAbsent(node.path(), node);
        }
        return new DocxLayoutMetrics(paths, fragments, placed);
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
     * How far a page zone's content sits from the page edge it belongs to, as laid out on
     * the first page.
     *
     * <p>Word places a footer by the distance from the page's bottom edge to the bottom of
     * the footer, and a header by the distance from the top edge to the top of the header.
     * The engine does not state either: a zone is a band of a given height against the
     * edge, and its content is laid out inside it from the top. So the distance is read
     * from where the content actually landed — the lowest edge of a footer's fragments, the
     * highest edge of a header's — rather than rebuilt from the band's parts.</p>
     *
     * <p>Zone fragments are spliced into the graph under {@code @page-zone[page][index]},
     * outside the node paths this index is built from, so they are found by that prefix.</p>
     *
     * @param zoneIndex  the zone's position in the session's zone list
     * @param header     whether it is a header, measured from the top edge
     * @param pageHeight the page's height in points
     * @return the distance in points, or empty when the layout carries no such zone
     */
    OptionalDouble zoneDistanceFromEdge(int zoneIndex, boolean header, double pageHeight) {
        String prefix = "@page-zone[0][" + zoneIndex + "]";
        double lowest = Double.POSITIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, List<PlacedFragment>> entry : fragments.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
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
     * <p>Read from the first fragment the node emitted: a paragraph split across a page
     * boundary emits one fragment per page and every one of them carries the same line
     * height, which is a property of the text style rather than of the split.</p>
     *
     * @param node any node that lays its text out as paragraph lines
     * @return the line height in points, or empty when the node laid out nothing
     */
    OptionalDouble lineHeight(DocumentNode node) {
        for (PlacedFragment fragment : fragmentsOf(node)) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph
                && paragraph.lineHeight() > 0) {
                return OptionalDouble.of(paragraph.lineHeight());
            }
        }
        return OptionalDouble.empty();
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
        double width = placedTable.placementWidth();

        TreeSet<Double> boundaries = new TreeSet<>();
        for (PlacedFragment fragment : fragmentsOf(node)) {
            if (!(fragment.payload() instanceof TableRowFragmentPayload row) || row.cells().isEmpty()) {
                continue;
            }
            // A table whose cell is built from another table emits that inner table's rows
            // under the *owner's* path, so the fragments at one path are not all one
            // table's. A row of this table spans this table; a nested one stops short, and
            // mixing the two produced a grid with more columns than the table has.
            double rowRight = 0;
            for (TableResolvedCell cell : row.cells()) {
                rowRight = Math.max(rowRight, cell.x() + cell.width());
            }
            if (Math.abs(rowRight - width) > 0.5) {
                continue;
            }
            for (TableResolvedCell cell : row.cells()) {
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

    private List<PlacedFragment> fragmentsOf(DocumentNode node) {
        String path = paths.get(node);
        return path == null ? List.of() : fragments.getOrDefault(path, List.of());
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
