package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A layer stack whose layers are side-by-side columns, as the columns of one table row.
 *
 * <p>A two-column page can be composed as the layers of one stack rather than the columns of a
 * row, so it can be drawn in the order a reader should meet it: each layer spans the stack and
 * is inset to the band its column holds. On the page nothing overlaps. Written as layers, one
 * after the other, the export put the whole of the second column below the whole of the first,
 * and a one-page CV ran to three pages in Word.</p>
 *
 * <p>A stack is taken as columns when every layer is a plain container at the stack's top-left
 * corner, and the bands its layers leave for their content are, pair by pair, either the same
 * band or apart. Each band is one column. Layers that share a band follow one another in its
 * cell, in the stack's order.</p>
 *
 * <p>Layers sharing a band overlap on the page, so a later one keeps the place of what an earlier
 * one draws with an invisible spacer — a name drawn first for the reading order, and a stand-in
 * as tall as the name at the top of the column it belongs to. In the cell the name is written
 * before the column, and the stand-in would push the column down by the name's height a second
 * time. A spacer the layout placed level with another layer of the same band is such a stand-in,
 * and is left out.</p>
 */
final class DocxLayerColumns {

    /** How far apart two edges may be and still be the same edge, in points. */
    private static final double EDGE = 0.5;

    private DocxLayerColumns() {
    }

    /**
     * One column: the band it holds across the stack, and the layers drawn in it.
     *
     * @param left   the band's left edge, from the stack's content box, in points
     * @param right  the band's right edge, from the stack's content box, in points
     * @param layers the layers in this band, in the stack's order
     */
    record Column(double left, double right, List<DocumentNode> layers) {
    }

    /**
     * A stack written as columns.
     *
     * @param width    the stack's content width in points
     * @param columns  the columns, left to right
     * @param standIns the spacers that hold another layer's place, to be left out
     * @param resumes  for each layer written after another in its cell, the space above its
     *                 first block, in points
     */
    record Plan(double width, List<Column> columns, Set<DocumentNode> standIns,
                Map<DocumentNode, Double> resumes) {

        /**
         * The space above a layer's first block when it follows another layer in its cell.
         *
         * @param layer a layer of the stack
         * @return the space in points, or {@code NaN} when the layer opens its cell or the
         *         layout placed too little of it to tell
         */
        double resume(DocumentNode layer) {
            Double points = resumes.get(layer);
            return points == null ? Double.NaN : points;
        }
    }

    /**
     * The columns a stack's layers form, or {@code null} when they do not form any.
     *
     * @param stack  the stack
     * @param layout where the layout placed the stack and its layers
     * @param plain  whether a node is a container that draws nothing of its own
     * @return the plan, or {@code null} when the stack is not a set of side-by-side columns
     */
    static Plan of(LayerStackNode stack, DocxLayoutMetrics layout, Predicate<DocumentNode> plain) {
        PlacedNode box = layout.placement(stack);
        if (box == null || stack.layers().size() < 2) {
            return null;
        }
        double contentLeft = box.placementX() + box.padding().left();
        double width = box.placementWidth() - box.padding().left() - box.padding().right();
        List<Column> columns = new ArrayList<>();
        for (LayerStackNode.Layer layer : stack.layers()) {
            PlacedNode placed = layout.placement(layer.node());
            if (placed == null || layer.align() != LayerAlign.TOP_LEFT
                || layer.offsetX() != 0 || layer.offsetY() != 0
                || !plain.test(layer.node())) {
                return null;
            }
            // A layer at the top-left corner is offered the stack's whole width, and may come
            // out narrower when its content is: the band is what it was offered, less its
            // padding, not what it filled.
            double left = placed.placementX() + placed.padding().left() - contentLeft;
            double right = width - placed.padding().right();
            if (right - left <= EDGE) {
                return null;
            }
            Column home = null;
            for (Column column : columns) {
                boolean same = Math.abs(column.left() - left) <= EDGE && Math.abs(column.right() - right) <= EDGE;
                boolean apart = right <= column.left() + EDGE || left >= column.right() - EDGE;
                if (same) {
                    home = column;
                } else if (!apart) {
                    return null;
                }
            }
            if (home == null) {
                columns.add(new Column(left, right, new ArrayList<>(List.of(layer.node()))));
            } else {
                home.layers().add(layer.node());
            }
        }
        if (columns.size() < 2) {
            return null;
        }
        columns.sort(Comparator.comparingDouble(Column::left));
        Set<DocumentNode> standIns = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Column column : columns) {
            for (DocumentNode layer : column.layers()) {
                collectStandIns(layer, column.layers(), layer, layout, standIns);
            }
        }
        Map<DocumentNode, Double> resumes = new IdentityHashMap<>();
        for (Column column : columns) {
            List<DocumentNode> layers = column.layers();
            for (int index = 1; index < layers.size(); index++) {
                double resume = resume(layers.subList(0, index), layers.get(index), layout, standIns);
                if (!Double.isNaN(resume)) {
                    resumes.put(layers.get(index), resume);
                }
            }
        }
        return new Plan(width, List.copyOf(columns), standIns, resumes);
    }

    /**
     * The space between the last block of the layers above and a layer's first block, as the
     * page has it.
     *
     * <p>A later layer starts at the top of the band, as the earlier ones do, and whatever it
     * holds above its first block — its own padding, the stand-ins — is space the earlier
     * layers' content already takes. Written after them, it is space above that content a
     * second time. What the cell needs instead is the gap the page shows between the two:
     * from the bottom of the lowest block above, its own padding included, to the top of the
     * layer's first block, less that block's margin, which the block writes itself.</p>
     */
    private static double resume(List<DocumentNode> above, DocumentNode layer,
                                 DocxLayoutMetrics layout, Set<DocumentNode> standIns) {
        PlacedNode lowest = null;
        for (DocumentNode earlier : above) {
            lowest = lowestLeaf(earlier, layout, lowest);
        }
        DocumentNode first = firstLeaf(layer, layout, standIns);
        if (lowest == null || first == null) {
            return Double.NaN;
        }
        PlacedNode top = layout.placement(first);
        double gap = lowest.placementY() + lowest.padding().bottom()
                     - (top.placementY() + top.placementHeight()) - first.margin().top();
        return Math.max(0, gap);
    }

    /** The placed leaf under {@code node} whose bottom is lowest on the page. */
    private static PlacedNode lowestLeaf(DocumentNode node, DocxLayoutMetrics layout, PlacedNode lowest) {
        if (node.children().isEmpty()) {
            PlacedNode placed = layout.placement(node);
            return placed != null && (lowest == null || placed.placementY() < lowest.placementY())
                    ? placed : lowest;
        }
        for (DocumentNode child : node.children()) {
            lowest = lowestLeaf(child, layout, lowest);
        }
        return lowest;
    }

    /** The first placed leaf under {@code node}, in reading order, that is not a stand-in. */
    private static DocumentNode firstLeaf(DocumentNode node, DocxLayoutMetrics layout,
                                          Set<DocumentNode> standIns) {
        if (node.children().isEmpty()) {
            return standIns.contains(node) || layout.placement(node) == null ? null : node;
        }
        for (DocumentNode child : node.children()) {
            DocumentNode first = firstLeaf(child, layout, standIns);
            if (first != null) {
                return first;
            }
        }
        return null;
    }

    /** Adds the spacers under {@code node} that sit level with another layer of the band. */
    private static void collectStandIns(DocumentNode node, List<DocumentNode> band, DocumentNode own,
                                        DocxLayoutMetrics layout, Set<DocumentNode> standIns) {
        if (node instanceof SpacerNode spacer) {
            PlacedNode placed = layout.placement(spacer);
            if (placed != null) {
                for (DocumentNode other : band) {
                    if (other != own && level(placed, layout.placement(other))) {
                        standIns.add(spacer);
                        break;
                    }
                }
            }
            return;
        }
        for (DocumentNode child : node.children()) {
            collectStandIns(child, band, own, layout, standIns);
        }
    }

    /** Whether two boxes share a page and some of their height on it. */
    private static boolean level(PlacedNode a, PlacedNode b) {
        if (b == null || a.startPage() != b.startPage()) {
            return false;
        }
        double overlap = Math.min(a.placementY() + a.placementHeight(), b.placementY() + b.placementHeight())
                - Math.max(a.placementY(), b.placementY());
        return overlap > EDGE;
    }
}
