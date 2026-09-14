package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Side-by-side page columns drawn in the order a reader should meet them.
 *
 * <p>A row draws its columns left to right, and the content stream follows that order. A resume
 * parser that reads the stream rather than the page meets whatever the left column opens with — a
 * monogram, a contact heading — before the name at the top of the right column, and takes that for
 * the name. The same columns laid as the layers of one stack can be drawn in any order: each layer
 * spans the stack and is inset to the band its column held in the row, so nothing moves on the
 * page, and a stack is atomic exactly as the row was.</p>
 *
 * <p>Drawing the whole right column first trades one field for another. The parsers read the
 * contact details only from the lines above the first section heading, so a right column drawn
 * first closes that block at its own first heading, before the left column's contact lines are
 * reached. A preset draws the name alone first instead, then the left column, then the rest of the
 * right column under a stand-in as tall as the name — {@link #measure} lays the name out on its own
 * to find that height.</p>
 */
final class ReadingOrderColumns {

    private ReadingOrderColumns() {
    }

    /**
     * The size a node was laid out at.
     *
     * @param width  placed width in points
     * @param height placed height in points, margins excluded
     */
    record Box(double width, double height) {
    }

    /**
     * One column as a layer: a section as wide as the stack, inset on either side so the section
     * inside it gets the band a weighted row would have given the column.
     *
     * @param name        the name of the full-width layer
     * @param insetLeft   the width taken by the columns to its left
     * @param insetRight  the width taken by the columns to its right
     * @param contentName the name of the section that carries the column's content
     * @param content     composes that section exactly as the row's column did
     * @return the layer's node
     */
    static DocumentNode column(String name, double insetLeft, double insetRight,
                               String contentName, Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft));
        layer.addSection(contentName, content);
        return layer.build();
    }

    /**
     * Lays {@code probe} out on its own and returns the size of each named node in it.
     *
     * <p>The session's roots are set aside while the probe is laid out and put back afterwards, so
     * a preset can measure whatever the caller composed before it. The page size, the margin and the
     * page backgrounds are left as they are, which is why a probe inset the way its nodes will be on
     * the page lays them out at the width they will have there.</p>
     *
     * <p>The sizes are read from the layout snapshot, which rounds to a thousandth of a point, so a
     * stand-in built from one can differ from the node it replaces by up to half a thousandth.</p>
     *
     * @param document the session the preset composes into, with its page already set
     * @param probe    composes the nodes to measure, inset as they will be on the page
     * @param names    the names of the nodes whose sizes are wanted
     * @return each named node's size, by name
     * @throws IllegalStateException if the probe lays out no node of one of the names
     */
    static Map<String, Box> measure(DocumentSession document, Consumer<PageFlowBuilder> probe,
                                    String... names) {
        List<DocumentNode> roots = document.roots();
        document.clear();
        try {
            document.pageFlow(probe);
            Map<String, Box> sizes = new HashMap<>();
            for (LayoutNodeSnapshot node : document.layoutSnapshot().nodes()) {
                for (String name : names) {
                    if (name.equals(node.entityName())) {
                        sizes.putIfAbsent(name,
                                new Box(node.placementWidth(), node.placementHeight()));
                    }
                }
            }
            for (String name : names) {
                if (!sizes.containsKey(name)) {
                    throw new IllegalStateException("The probe laid out no node named " + name);
                }
            }
            return sizes;
        } finally {
            document.clear();
            document.addAll(roots);
        }
    }

    /**
     * An invisible stand-in the size of {@code box}: it keeps the place of a node drawn in another
     * layer, so everything after it in the flow lands where it did.
     *
     * @param flow   the flow the node stood in
     * @param name   the stand-in's name
     * @param box    the size of the node it stands in for
     * @param margin that node's margin
     */
    static void holdPlace(SectionBuilder flow, String name, Box box, DocumentInsets margin) {
        flow.addSpacer(spacer -> spacer
                .name(name)
                .width(box.width())
                .height(box.height())
                .margin(margin));
    }
}
