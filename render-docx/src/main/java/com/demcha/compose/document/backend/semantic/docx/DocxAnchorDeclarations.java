package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.table.DocumentTableCell;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The anchors a document's nodes declare, counted node by node.
 *
 * <p>The layout registers an anchor where it draws the node, and that is not once a node: every
 * cell of a table built from a node registers its anchor under the table's path, and a header row
 * the page repeats registers its anchor again on each page it is drawn on. Counted off the layout,
 * two cells of one table declaring an anchor were one node, and the file keeping one of their
 * bookmarks went unsaid. Counted here, off the document, each node declaring an anchor is one,
 * a table's composed cells and a header row included.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxAnchorDeclarations {

    private DocxAnchorDeclarations() {
    }

    /**
     * Adds what a section's nodes declare to what the sections before it declared.
     *
     * @param graph             the section's document
     * @param nodes             how many nodes declare each anchor, added to
     * @param inARepeatedHeader the anchors a node in a table's repeated header row declares,
     *                          added to
     */
    static void count(DocumentGraph graph, Map<String, Integer> nodes, Set<String> inARepeatedHeader) {
        for (DocumentNode root : graph.roots()) {
            count(root, false, nodes, inARepeatedHeader);
        }
    }

    private static void count(DocumentNode node, boolean inAHeader, Map<String, Integer> nodes,
                              Set<String> inARepeatedHeader) {
        String anchor = declaredBy(node);
        if (anchor != null) {
            nodes.merge(anchor, 1, Integer::sum);
            if (inAHeader) {
                inARepeatedHeader.add(anchor);
            }
        }
        Set<DocumentNode> repeated = repeatedHeaderContent(node);
        for (DocumentNode child : DocxWrittenTree.under(node)) {
            count(child, inAHeader || repeated.contains(child), nodes, inARepeatedHeader);
        }
    }

    /** What the cells of a table's repeated header rows are built from; empty for any other node. */
    private static Set<DocumentNode> repeatedHeaderContent(DocumentNode node) {
        if (!(node instanceof TableNode table) || table.repeatedHeaderRowCount() <= 0) {
            return Set.of();
        }
        Set<DocumentNode> content = Collections.newSetFromMap(new IdentityHashMap<>());
        List<List<DocumentTableCell>> rows = table.rows();
        for (int row = 0; row < Math.min(table.repeatedHeaderRowCount(), rows.size()); row++) {
            for (DocumentTableCell cell : rows.get(row)) {
                if (cell != null && cell.content() != null) {
                    content.add(cell.content());
                }
            }
        }
        return content;
    }

    /**
     * The anchor each node kind that declares one holds, by its exact class: node records are
     * final. A page reference names an anchor and declares none, so it is not here.
     */
    static final Map<Class<? extends DocumentNode>, Function<DocumentNode, String>> DECLARING = Map.ofEntries(
            declaring(ParagraphNode.class, ParagraphNode::anchor),
            declaring(SectionNode.class, SectionNode::anchor),
            declaring(ContainerNode.class, ContainerNode::anchor),
            declaring(TableNode.class, TableNode::anchor),
            declaring(ImageNode.class, ImageNode::anchor),
            declaring(BarcodeNode.class, BarcodeNode::anchor),
            declaring(LineNode.class, LineNode::anchor),
            declaring(ShapeNode.class, ShapeNode::anchor),
            declaring(EllipseNode.class, EllipseNode::anchor));

    private static <T extends DocumentNode> Map.Entry<Class<? extends DocumentNode>, Function<DocumentNode, String>>
    declaring(Class<T> kind, Function<T, String> anchor) {
        return Map.entry(kind, node -> anchor.apply(kind.cast(node)));
    }

    /**
     * The anchor a node declares — the place a link and a page reference go to — or null.
     *
     * @param node a node of the document
     * @return its anchor, or null when it declares none
     */
    static String declaredBy(DocumentNode node) {
        Function<DocumentNode, String> declared = DECLARING.get(node.getClass());
        String anchor = declared == null ? null : declared.apply(node);
        return anchor == null || anchor.isBlank() ? null : anchor;
    }
}
