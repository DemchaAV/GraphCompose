package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;

import java.util.List;
import java.util.Objects;

/**
 * Positions a single fixed-size child horizontally within the full available
 * content width — the block-level {@code margin: auto} / {@code align(center)}
 * the flow does not provide for non-width-filling nodes. The wrapper fills the
 * width and its height equals the child's; the child seats {@link
 * HorizontalAlign#LEFT left}, {@link HorizontalAlign#CENTER centre}, or
 * {@link HorizontalAlign#RIGHT right} inside it.
 *
 * <p>Use it to centre an SVG icon, a path, an image, or a barcode on the page
 * without hand-computing the content width:
 * {@code flow.addAligned(HorizontalAlign.CENTER, icon.node(48))}.</p>
 *
 * @param name   node name used in snapshots and layout graph paths
 * @param child  the node to position
 * @param align  horizontal placement within the available width
 * @param margin outer margin around the wrapper
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record AlignNode(String name, DocumentNode child, HorizontalAlign align, DocumentInsets margin)
        implements DocumentNode {
    /**
     * Validates the child and alignment; normalizes name and margin.
     */
    public AlignNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(child, "child");
        Objects.requireNonNull(align, "align");
        margin = margin == null ? DocumentInsets.zero() : margin;
    }

    /**
     * Creates an align wrapper with no margin.
     *
     * @param child the node to position
     * @param align horizontal placement
     */
    public AlignNode(DocumentNode child, HorizontalAlign align) {
        this("", child, align, DocumentInsets.zero());
    }

    @Override
    public List<DocumentNode> children() {
        return List.of(child);
    }

    @Override
    public String nodeKind() {
        return "Align";
    }
}
