package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

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
 * time. A spacer the layout placed level with content of another layer of the same band is such
 * a stand-in, and is left out.</p>
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
     *                 first block, in points; below zero where the block's own edge gives some
     *                 back ({@link #resume})
     * @param over     for each layer the page starts above where the content of the layers before
     *                 it in its cell ends, by how much, in points (see {@link #over(DocumentNode)})
     * @param moves    what is written in place of a stand-in instead (see {@link Moves})
     * @param drawn    the layers holding only what is drawn where the page puts it — a column's
     *                 mark, the rule between two columns — which belong to no column
     * @param closing  where the blocks a resume is measured from are placed: the space below each
     *                 is the page's, its own margin and padding below it included
     */
    record Plan(double width, List<Column> columns, Set<DocumentNode> standIns,
                Map<DocumentNode, Double> resumes, Map<DocumentNode, Double> over, Moves moves,
                List<DocumentNode> drawn, Set<PlacedNode> closing) {

        /**
         * The space above a layer's first block when it follows another layer in its cell.
         *
         * @param layer a layer of the stack
         * @return the space in points, below zero where the block's own edge gives some back
         *         ({@link DocxLayerColumns#resume}), or {@code NaN} when the layer opens its cell
         *         or the layout placed too little of it to tell
         */
        double resume(DocumentNode layer) {
            Double points = resumes.get(layer);
            return points == null ? Double.NaN : points;
        }

        /**
         * How far above where the content of the layers before it in its cell ends — a panel
         * among them where its box does — the page starts a layer's highest block: the page sets
         * it over that content, beside it or above it, and Word writes one block under another,
         * so what the layer writes in the flow is written under them.
         *
         * @param layer a layer of the stack
         * @return the points, or 0 where the page starts it under them — or above them by no
         *         more than {@link #EDGE}, a layer touching the one above it — and where it opens
         *         its cell, or it or the layers before it write nothing
         */
        double over(DocumentNode layer) {
            return over.getOrDefault(layer, 0.0);
        }
    }

    /**
     * The columns a stack's layers form, or {@code null} when they do not form any.
     *
     * @param stack  the stack
     * @param layout where the layout placed the stack and its layers
     * @param plain   whether a node is a container that draws nothing of its own
     * @param painted whether a node is a container that paints a panel, which a later layer's
     *                content written after it would stand outside of (see {@link Moves})
     * @param drawn   whether a layer holds only what is drawn where the page puts it: it forms
     *                no column, and may stand over one — a card grid lays each card's mark
     *                over the edge of its text and a rule between two cards, and was written
     *                one card under the other, each a column lower and further right
     * @param inset   how far below the top of its box the page starts a leaf's content, which the
     *                leaf writes above that content itself: its padding, a contained picture's
     *                room in its box
     * @return the plan, or {@code null} when the stack is not a set of side-by-side columns
     */
    static Plan of(LayerStackNode stack, DocxLayoutMetrics layout, Predicate<DocumentNode> plain,
                   Predicate<DocumentNode> painted, Predicate<DocumentNode> drawn, ToDoubleFunction<DocumentNode> inset) {
        // Every layer a column first: a stack that already forms columns — a portrait's band
        // beside a text band — is written as it always was. Only one whose drawn layers stand
        // across the columns is tried again without them.
        Plan whole = columnsOf(stack, layout, plain, painted, node -> false, inset);
        if (whole != null) {
            return whole;
        }
        Plan withoutDrawing = columnsOf(stack, layout, plain, painted, drawn, inset);
        return withoutDrawing == null || withoutDrawing.drawn().isEmpty() ? null : withoutDrawing;
    }

    private static Plan columnsOf(LayerStackNode stack, DocxLayoutMetrics layout, Predicate<DocumentNode> plain,
                                  Predicate<DocumentNode> painted, Predicate<DocumentNode> drawn,
                                  ToDoubleFunction<DocumentNode> inset) {
        PlacedNode box = layout.placement(stack);
        if (box == null || stack.layers().size() < 2) {
            return null;
        }
        double contentLeft = box.placementX() + box.padding().left();
        double width = box.placementWidth() - box.padding().left() - box.padding().right();
        List<Column> columns = new ArrayList<>();
        List<DocumentNode> drawnLayers = new ArrayList<>();
        for (LayerStackNode.Layer layer : stack.layers()) {
            if (drawn.test(layer.node())) {
                drawnLayers.add(layer.node());
                continue;
            }
            PlacedNode placed = layout.placement(layer.node());
            if (placed == null || layer.align() != LayerAlign.TOP_LEFT
                || layer.offsetX() != 0 || layer.offsetY() != 0
                || !plain.test(layer.node())) {
                return null;
            }
            // A layer at the top-left corner is offered the stack's whole width, and may come
            // out narrower when its content is: the band is what it was offered, less its
            // margin and padding, not what it filled. Its box starts inside its left margin.
            double left = placed.placementX() + placed.padding().left() - contentLeft;
            double right = width - placed.padding().right() - layer.node().margin().right();
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
        Map<DocumentNode, Double> resumes = new IdentityHashMap<>();
        Map<DocumentNode, Double> over = new IdentityHashMap<>();
        Set<PlacedNode> closing = Collections.newSetFromMap(new IdentityHashMap<>());
        Moves moves = new Moves();
        for (Column column : columns) {
            flow(column.layers(), layout, node -> false, painted, painted, inset, standIns, resumes, over, closing,
                    moves);
        }
        return new Plan(width, List.copyOf(columns), standIns, resumes, over, moves, List.copyOf(drawnLayers),
                closing);
    }

    /**
     * Content a later layer lays exactly where an earlier layer holds its place, written in
     * that place instead.
     *
     * <p>A template that wants its reading order to differ from its drawing order draws a
     * block in two layers: a hero strip carries its fill and the name, with a stand-in where
     * the subtitle goes, and a later layer carries a stand-in for the name and then the
     * subtitle. On the page they lie over each other as one strip. Written one layer after the
     * other, the strip lost its stand-in and the subtitle came after it, under the strip
     * instead of in it. The outermost block of a later layer whose box lies inside a stand-in
     * of an earlier one, in a painted panel, is written in the stand-in's place, and not again
     * in its own layer.</p>
     */
    static final class Moves {
        private final Map<DocumentNode, List<DocumentNode>> into = new IdentityHashMap<>();
        private final Set<DocumentNode> moved = Collections.newSetFromMap(new IdentityHashMap<>());

        /** What is written in place of a stand-in, empty when nothing is. */
        List<DocumentNode> into(DocumentNode standIn) {
            return into.getOrDefault(standIn, List.of());
        }

        /** Whether a node is written in an earlier layer's stand-in rather than in its own. */
        boolean moved(DocumentNode node) {
            return moved.contains(node);
        }

        private void move(DocumentNode node, DocumentNode standIn) {
            into.computeIfAbsent(standIn, key -> new ArrayList<>()).add(node);
            moved.add(node);
        }

        private boolean filled(DocumentNode standIn) {
            return into.containsKey(standIn);
        }
    }

    /**
     * A stack whose layers overlap, written as one band: layer after layer, as the page
     * places their content.
     *
     * @param layers   the stack's layers, in its order
     * @param standIns the spacers that hold another layer's place, to be left out
     * @param resumes  for each layer after the first, the space above its first block, below zero
     *                 where the block's own edge gives some back ({@link #resume})
     * @param over     for each layer the page starts above where the content of the layers before
     *                 it ends, by how much, in points (see {@link Plan#over(DocumentNode)})
     * @param above    from the stack's top to its first written block, that block's own margin
     *                 aside
     * @param below    from the text of its lowest written block to the stack's bottom; below zero
     *                 when that text runs past the bottom, by as much as it hangs below it
     * @param closing  where the blocks {@code below} and a resume are measured from are placed: the
     *                 space below each is the page's, its own margin and padding below it included
     */
    record Band(List<DocumentNode> layers, Set<DocumentNode> standIns, Map<DocumentNode, Double> resumes,
                Map<DocumentNode, Double> over, double above, double below, Set<PlacedNode> closing) {

        /** @see Plan#resume(DocumentNode) */
        double resume(DocumentNode layer) {
            Double points = resumes.get(layer);
            return points == null ? Double.NaN : points;
        }

        /** @see Plan#over(DocumentNode) */
        double over(DocumentNode layer) {
            return over.getOrDefault(layer, 0.0);
        }
    }

    /**
     * A stack of overlapping layers as one band, or {@code null} when it has fewer than two
     * layers or nothing written in it.
     *
     * <p>Word has no layers, so an overlay's layers are written one after the other, and each
     * one starts again at the top: what one layer holds the place of with a spacer — a badge's
     * circle kept in the flow while the badge is drawn over it — came out twice as tall, the
     * spacer's height and then the badge's text below it. As a band, a spacer level with
     * another layer's content is a stand-in and is left out, the space above the first block
     * and below the last is the page's, and a later layer resumes the page's distance below
     * the blocks above it. Drawing writes no paragraph and is not content here: a circle's box
     * would otherwise decide where the text in it starts.</p>
     *
     * @param stack   the stack: a layer stack, or a shape container, whose layers are its children
     * @param layout  where the layout placed the stack and its layers
     * @param drawing whether a leaf is drawing this export does not write
     * @param painted whether a node is a container that paints a panel, where the layers before a
     *                layer end when it is the lowest among them (see {@link Band#over})
     * @param inset   how far below its box's top a leaf's content starts (see {@link #of})
     * @return the band, or {@code null}
     */
    static Band band(DocumentNode stack, DocxLayoutMetrics layout, Predicate<DocumentNode> drawing,
                     Predicate<DocumentNode> painted, ToDoubleFunction<DocumentNode> inset) {
        PlacedNode box = layout.placement(stack);
        if (box == null || stack.children().size() < 2) {
            return null;
        }
        List<DocumentNode> layers = stack.children();
        Set<DocumentNode> standIns = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<DocumentNode, Double> resumes = new IdentityHashMap<>();
        Map<DocumentNode, Double> over = new IdentityHashMap<>();
        Set<PlacedNode> closing = Collections.newSetFromMap(new IdentityHashMap<>());
        // A band's layers are written over each other in one run, where a stand-in's place
        // is where the later layer writes its content anyway: nothing moves. Its resumes are
        // measured from text, a panel's too.
        Moves none = new Moves();
        flow(layers, layout, drawing, node -> false, painted, inset, standIns, resumes, over, closing, none);
        // A stack nested in a layer is a band of its own, and its stand-ins are not written
        // either: the band's first and lowest blocks are measured past them too, or a badge's
        // place-holding spacer would set where the initials over it start.
        Set<DocumentNode> measured = Collections.newSetFromMap(new IdentityHashMap<>());
        measured.addAll(standIns);
        for (DocumentNode layer : layers) {
            nestedStandIns(layer, layout, drawing, measured);
        }
        DocumentNode first = null;
        PlacedNode lowest = null;
        for (DocumentNode layer : layers) {
            if (first == null) {
                first = firstLeaf(layer, layout, written(measured, none, false), drawing);
            }
            lowest = lowestLeaf(layer, layout, written(measured, none, true), drawing, lowest);
        }
        if (first == null || lowest == null) {
            return null;
        }
        PlacedNode top = layout.placement(first);
        double above = box.placementY() + box.placementHeight()
                       - (top.placementY() + top.placementHeight()) - first.margin().top();
        double below = lowest.placementY() + lowest.padding().bottom() - box.placementY();
        closing.add(lowest);
        return new Band(layers, standIns, resumes, over, Math.max(0, above), below, closing);
    }

    /**
     * Which leaves are written where they stand: not a stand-in, unless something is written
     * in its place and {@code filledCount} says that counts, and not a leaf written in another
     * layer's stand-in.
     */
    private static Predicate<DocumentNode> written(Set<DocumentNode> standIns, Moves moves, boolean filledCount) {
        return node -> !moves.moved(node)
                       && (!standIns.contains(node) || filledCount && moves.filled(node));
    }

    /** Adds the stand-ins of every layer stack under {@code node}, each as a band of its own. */
    private static void nestedStandIns(DocumentNode node, DocxLayoutMetrics layout,
                                       Predicate<DocumentNode> drawing, Set<DocumentNode> standIns) {
        if (node instanceof LayerStackNode nested) {
            flow(nested.children(), layout, drawing, candidate -> false, candidate -> false, candidate -> 0,
                    standIns, new IdentityHashMap<>(), new IdentityHashMap<>(),
                    Collections.newSetFromMap(new IdentityHashMap<>()), new Moves());
        }
        for (DocumentNode child : node.children()) {
            nestedStandIns(child, layout, drawing, standIns);
        }
    }

    /**
     * The stand-ins among layers that share a band, each later layer's resume, how far above
     * where the layers before it end the page starts a later layer, and where the block each
     * resume is measured from is placed.
     *
     * @param painted the panels content moves into and resumes are measured from
     * @param panels  the panels where the layers before a layer end, when one is the lowest
     * @param inset   how far below its box's top a leaf's content starts (see {@link #of})
     */
    private static void flow(List<DocumentNode> layers, DocxLayoutMetrics layout,
                             Predicate<DocumentNode> drawing, Predicate<DocumentNode> painted,
                             Predicate<DocumentNode> panels, ToDoubleFunction<DocumentNode> inset,
                             Set<DocumentNode> standIns, Map<DocumentNode, Double> resumes,
                             Map<DocumentNode, Double> over, Set<PlacedNode> closing, Moves moves) {
        if (layers.size() < 2) {
            return;
        }
        Map<DocumentNode, List<PlacedNode>> content = new IdentityHashMap<>();
        Map<DocumentNode, List<DocumentNode>> leaves = new IdentityHashMap<>();
        for (DocumentNode layer : layers) {
            List<DocumentNode> nodes = new ArrayList<>();
            collectContent(layer, layout, drawing, nodes);
            leaves.put(layer, nodes);
            List<PlacedNode> placed = new ArrayList<>(nodes.size());
            for (DocumentNode node : nodes) {
                placed.add(layout.placement(node));
            }
            content.put(layer, placed);
        }
        Map<DocumentNode, List<DocumentNode>> layerStandIns = new IdentityHashMap<>();
        for (DocumentNode layer : layers) {
            // In the layer's order, so a block inside two stand-ins goes to the first.
            List<DocumentNode> own = new ArrayList<>();
            collectStandIns(layer, layer, content, layout, own);
            standIns.addAll(own);
            layerStandIns.put(layer, new ArrayList<>(own));
        }
        for (int index = 0; index < layers.size(); index++) {
            Set<DocumentNode> inPanels = Collections.newSetFromMap(new IdentityHashMap<>());
            underPaint(layers.get(index), painted, false, inPanels);
            for (DocumentNode standIn : layerStandIns.get(layers.get(index))) {
                if (!inPanels.contains(standIn)) {
                    continue;
                }
                PlacedNode place = layout.placement(standIn);
                double[] across = layout.parentContent(standIn);
                if (place == null || across == null) {
                    continue;
                }
                for (DocumentNode later : layers.subList(index + 1, layers.size())) {
                    Set<DocumentNode> laterLeaves = Collections.newSetFromMap(new IdentityHashMap<>());
                    laterLeaves.addAll(leaves.get(later));
                    for (DocumentNode child : later.children()) {
                        moveInto(child, place, across, standIn, laterLeaves, layout, moves);
                    }
                }
            }
        }
        for (int index = 1; index < layers.size(); index++) {
            PlacedNode[] from = new PlacedNode[1];
            DocumentNode[] first = new DocumentNode[1];
            double space = spaceAbove(layers.subList(0, index), layers.get(index), layout, standIns, drawing,
                    painted, moves, from, first);
            if (!Double.isNaN(space)) {
                resumes.put(layers.get(index), resume(space, layers.get(index), first[0], inset, panels));
                if (from[0] != null) {
                    closing.add(from[0]);
                }
            }
            // A panel among them ends where its box does, below its text; the layer starts at its
            // highest content, not its first where its blocks stand side by side, as a row's do.
            double end = endOf(layers.subList(0, index), layout, standIns, drawing, panels, moves, new PlacedNode[1]);
            double highest = highestContent(layers.get(index), layout, written(standIns, moves, true), drawing, inset,
                    Double.NaN);
            if (!Double.isNaN(highest) && highest - end > EDGE) {
                over.put(layers.get(index), highest - end);
            }
        }
    }

    /**
     * A later layer's resume: the space Word writes above its first block, from the last block of
     * the layers above, before the block writes what it holds above its own content — its margin,
     * and inside its box its padding and a contained picture's room.
     *
     * <p>That is the page's gap to the block's content less what the block writes, and below zero
     * where the block holds more above its content than the gap: a caption whose 20pt margin
     * starts it 7pt under the line above, or a picture contained in a box that starts level with
     * that line. The block's own edge then gives that much back. Kept at zero, it was written
     * whole after the layers before it, and the caption stood 12.7pt low in Word and LibreOffice,
     * the picture 12.9pt (measured). Where the page starts the content itself above where they
     * end, it is written under them, no further up than its own edge gives, and named
     * ({@link Band#over}). An edge below zero is not written in a resumed layer, and gives
     * nothing.</p>
     *
     * <p>Only a paragraph, a picture, a list or a table written in its layer's own flow — under
     * plain containers alone — writes its edge where the space is given back. Any other first
     * block keeps the gap less its margin, never below zero: one in a row, a panel, a nested stack
     * or a shape writes its edge inside them, and a spacer writes neither its margin nor its
     * padding, though its margin comes off the gap, and the report names it.</p>
     *
     * @param space from the end of the layers above to the top of the block's box, below zero
     *              where the box starts above that end ({@link #spaceAbove})
     */
    private static double resume(double space, DocumentNode layer, DocumentNode block,
                                 ToDoubleFunction<DocumentNode> inset, Predicate<DocumentNode> painted) {
        boolean givesItsEdgeBack = (block instanceof ParagraphNode || block instanceof ImageNode
                                    || block instanceof ListNode || block instanceof TableNode)
                                   && inThePlainFlowOf(layer, block, painted);
        if (!givesItsEdgeBack) {
            return Math.max(0, space - block.margin().top());
        }
        double inside = Math.max(0, inset.applyAsDouble(block));
        double written = Math.max(0, block.margin().top() + Math.min(0, block.padding().top()) + inside);
        return Math.max(0, space + inside) - written;
    }

    /** Whether {@code block} is {@code node} or under it through plain, unpainted containers alone. */
    private static boolean inThePlainFlowOf(DocumentNode node, DocumentNode block, Predicate<DocumentNode> painted) {
        if (node == block) {
            return true;
        }
        if (!(node instanceof SectionNode || node instanceof ContainerNode) || painted.test(node)) {
            return false;
        }
        for (DocumentNode child : node.children()) {
            if (inThePlainFlowOf(child, block, painted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves into a stand-in the outermost blocks under {@code node} that lie inside its place
     * and hold content: a subtitle set in a chip moves with its chip.
     */
    private static void moveInto(DocumentNode node, PlacedNode place, double[] across, DocumentNode standIn,
                                 Set<DocumentNode> content, DocxLayoutMetrics layout, Moves moves) {
        if (moves.moved(node)) {
            return;
        }
        if (inside(layout.placement(node), place, across) && holdsAny(node, content)) {
            moves.move(node, standIn);
            return;
        }
        for (DocumentNode child : node.children()) {
            moveInto(child, place, across, standIn, content, layout, moves);
        }
    }

    private static boolean holdsAny(DocumentNode node, Set<DocumentNode> content) {
        if (content.contains(node)) {
            return true;
        }
        for (DocumentNode child : node.children()) {
            if (holdsAny(child, content)) {
                return true;
            }
        }
        return false;
    }

    /** Collects the spacers under {@code node} that sit inside a painted panel. */
    private static void underPaint(DocumentNode node, Predicate<DocumentNode> painted, boolean inPanel,
                                   Set<DocumentNode> spacers) {
        boolean here = inPanel || painted.test(node);
        if (node instanceof SpacerNode && here) {
            spacers.add(node);
        }
        for (DocumentNode child : node.children()) {
            underPaint(child, painted, here, spacers);
        }
    }

    /**
     * Whether a box lies in a stand-in's place, edges within {@link #EDGE}: between its top and
     * its foot, and across within the edges the stand-in is written between — a spacer stands
     * at the left of its panel, while what it holds the place of can stand anywhere across it.
     */
    private static boolean inside(PlacedNode box, PlacedNode place, double[] across) {
        return box != null && place != null && box.startPage() == place.startPage()
               && box.placementX() >= across[0] - EDGE
               && box.placementX() + box.placementWidth() <= across[1] + EDGE
               && box.placementY() >= place.placementY() - EDGE
               && box.placementY() + box.placementHeight() <= place.placementY() + place.placementHeight() + EDGE;
    }

    /**
     * The space between the last block of the layers above and a layer's first block, as the
     * page has it; below zero where the page starts that block above where they end.
     *
     * <p>A later layer starts at the top of the band, as the earlier ones do, and whatever it
     * holds above its first block — its own padding, the stand-ins — is space the earlier
     * layers' content already takes. Written after them, it is space above that content a
     * second time. What the cell needs instead is the gap the page shows between the two:
     * from the bottom of the lowest block above, its own padding included, to the top of the
     * layer's first block — less what that block writes above its content itself, it is the
     * layer's resume ({@link #resume}). A block inside a painted panel ends where the panel does: the panel is a
     * table that writes its own padding below the block, so that padding is not part of the gap.</p>
     *
     * @param first set to the layer's first block
     */
    private static double spaceAbove(List<DocumentNode> above, DocumentNode layer,
                                     DocxLayoutMetrics layout, Set<DocumentNode> standIns,
                                     Predicate<DocumentNode> drawing, Predicate<DocumentNode> painted, Moves moves,
                                     PlacedNode[] from, DocumentNode[] first) {
        double bottom = endOf(above, layout, standIns, drawing, painted, moves, from);
        // A filled stand-in is where what fills it is written, so it counts as a first block.
        first[0] = firstLeaf(layer, layout, written(standIns, moves, true), drawing);
        if (Double.isNaN(bottom) || first[0] == null) {
            return Double.NaN;
        }
        PlacedNode top = layout.placement(first[0]);
        return bottom - (top.placementY() + top.placementHeight());
    }

    /**
     * Where on the page the written content of some layers ends: the lowest edge of any of them
     * (see {@link #lowestEdge}); {@code NaN} where they write nothing.
     */
    private static double endOf(List<DocumentNode> layers, DocxLayoutMetrics layout, Set<DocumentNode> standIns,
                                Predicate<DocumentNode> drawing, Predicate<DocumentNode> painted, Moves moves,
                                PlacedNode[] from) {
        double bottom = Double.NaN;
        for (DocumentNode layer : layers) {
            bottom = lowestEdge(layer, layout, written(standIns, moves, true), drawing, painted, null, bottom, from);
        }
        return bottom;
    }

    /**
     * The lowest edge on the page a written leaf under {@code node} ends at: its text's bottom,
     * or the bottom of the outermost painted panel it sits in. Measured up from the page's foot,
     * so lower is smaller; {@code NaN} while none is found.
     *
     * @param from set to where the leaf whose text ends at the edge is placed, or to null where
     *             the edge is a panel's
     */
    private static double lowestEdge(DocumentNode node, DocxLayoutMetrics layout,
                                     Predicate<DocumentNode> written, Predicate<DocumentNode> drawing,
                                     Predicate<DocumentNode> painted, PlacedNode panel, double lowest,
                                     PlacedNode[] from) {
        if (!written.test(node)) {
            return lowest;
        }
        PlacedNode outer = panel == null && painted.test(node) ? layout.placement(node) : panel;
        if (node.children().isEmpty()) {
            PlacedNode placed = drawing.test(node) ? null : layout.placement(node);
            if (placed == null) {
                return lowest;
            }
            // A panel's foot is the leaf's only while both are on one page.
            boolean onePage = outer != null && outer.startPage() == outer.endPage()
                              && outer.startPage() == placed.startPage();
            double edge = onePage ? outer.placementY() : placed.placementY() + placed.padding().bottom();
            if (Double.isNaN(lowest) || edge < lowest) {
                from[0] = onePage ? null : placed;
                return edge;
            }
            return lowest;
        }
        for (DocumentNode child : node.children()) {
            lowest = lowestEdge(child, layout, written, drawing, painted, outer, lowest, from);
        }
        return lowest;
    }

    /**
     * The placed leaf under {@code node} whose bottom is lowest on the page, stand-ins aside:
     * a stand-in at the foot of a layer holds the place of what the next layer writes, and is
     * not written itself.
     */
    private static PlacedNode lowestLeaf(DocumentNode node, DocxLayoutMetrics layout,
                                         Predicate<DocumentNode> written, Predicate<DocumentNode> drawing,
                                         PlacedNode lowest) {
        if (!written.test(node)) {
            return lowest;
        }
        if (node.children().isEmpty()) {
            PlacedNode placed = drawing.test(node) ? null : layout.placement(node);
            return placed != null && (lowest == null || placed.placementY() < lowest.placementY())
                    ? placed : lowest;
        }
        for (DocumentNode child : node.children()) {
            lowest = lowestLeaf(child, layout, written, drawing, lowest);
        }
        return lowest;
    }

    /**
     * Where on the page the highest written content under {@code node} starts, as
     * {@link #lowestLeaf} reads its leaves: a leaf's box's top less what the leaf writes above its
     * content inside it ({@code inset}). Measured up from the page's foot, so higher is larger;
     * {@code NaN} while none is found.
     */
    private static double highestContent(DocumentNode node, DocxLayoutMetrics layout,
                                         Predicate<DocumentNode> written, Predicate<DocumentNode> drawing,
                                         ToDoubleFunction<DocumentNode> inset, double highest) {
        if (!written.test(node)) {
            return highest;
        }
        if (node.children().isEmpty()) {
            PlacedNode placed = drawing.test(node) ? null : layout.placement(node);
            if (placed == null) {
                return highest;
            }
            double content = top(placed) - inset.applyAsDouble(node);
            return Double.isNaN(highest) || content > highest ? content : highest;
        }
        for (DocumentNode child : node.children()) {
            highest = highestContent(child, layout, written, drawing, inset, highest);
        }
        return highest;
    }

    private static double top(PlacedNode placed) {
        return placed.placementY() + placed.placementHeight();
    }

    /** The first placed leaf under {@code node}, in reading order, that is written. */
    private static DocumentNode firstLeaf(DocumentNode node, DocxLayoutMetrics layout,
                                          Predicate<DocumentNode> written, Predicate<DocumentNode> drawing) {
        if (!written.test(node)) {
            return null;
        }
        if (node.children().isEmpty()) {
            return drawing.test(node) || layout.placement(node) == null ? null : node;
        }
        for (DocumentNode child : node.children()) {
            DocumentNode first = firstLeaf(child, layout, written, drawing);
            if (first != null) {
                return first;
            }
        }
        return null;
    }

    /** Collects the placed leaves under {@code node} that are content: every one but a spacer or drawing. */
    private static void collectContent(DocumentNode node, DocxLayoutMetrics layout,
                                       Predicate<DocumentNode> drawing, List<DocumentNode> leaves) {
        if (node.children().isEmpty()) {
            PlacedNode placed = layout.placement(node);
            if (placed != null && !(node instanceof SpacerNode) && !drawing.test(node)) {
                leaves.add(node);
            }
            return;
        }
        for (DocumentNode child : node.children()) {
            collectContent(child, layout, drawing, leaves);
        }
    }

    /**
     * Adds the spacers under {@code node} that sit level with content of another layer of the
     * band. Measured against that content rather than the layer's box: a long layer's box
     * covers the whole band, and a spacer of a short layer beside it holds real space.
     */
    private static void collectStandIns(DocumentNode node, DocumentNode own,
                                        Map<DocumentNode, List<PlacedNode>> content,
                                        DocxLayoutMetrics layout, java.util.Collection<DocumentNode> standIns) {
        if (node instanceof SpacerNode spacer) {
            PlacedNode placed = layout.placement(spacer);
            if (placed != null && levelWithAnother(placed, own, content)) {
                standIns.add(spacer);
            }
            return;
        }
        for (DocumentNode child : node.children()) {
            collectStandIns(child, own, content, layout, standIns);
        }
    }

    /** Whether a box sits level with content of any layer but {@code own}. */
    private static boolean levelWithAnother(PlacedNode box, DocumentNode own,
                                            Map<DocumentNode, List<PlacedNode>> content) {
        for (Map.Entry<DocumentNode, List<PlacedNode>> layer : content.entrySet()) {
            if (layer.getKey() == own) {
                continue;
            }
            for (PlacedNode leaf : layer.getValue()) {
                if (level(box, leaf)) {
                    return true;
                }
            }
        }
        return false;
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
