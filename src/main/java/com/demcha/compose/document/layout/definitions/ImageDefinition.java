package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.*;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import com.demcha.compose.document.node.ImageNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.*;

/**
 * Layout definition for {@link ImageNode}: resolves image dimensions from
 * intrinsic metadata, requested size, scale, and available width, then emits a
 * fixed atomic image fragment.
 *
 * @author Artem Demchyshyn
 */
public final class ImageDefinition implements NodeDefinition<ImageNode> {

    /**
     * Creates the image layout definition.
     */
    public ImageDefinition() {
    }

    @Override
    public Class<ImageNode> nodeType() {
        return ImageNode.class;
    }

    @Override
    public PreparedNode<ImageNode> prepare(ImageNode node, PrepareContext ctx, BoxConstraints constraints) {
        ImageDimensions dimensions = resolveImageDimensions(node, constraints.availableWidth());
        return PreparedNode.leaf(node, new MeasureResult(
                dimensions.width() + node.padding().horizontal(),
                dimensions.height() + node.padding().vertical()));
    }

    @Override
    public PaginationPolicy paginationPolicy(ImageNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ImageNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        ImageNode node = prepared.node();
        double width = Math.max(0.0, placement.width() - node.padding().horizontal());
        double height = Math.max(0.0, placement.height() - node.padding().vertical());
        if (width <= EPS || height <= EPS) {
            return List.of();
        }
        LayoutFragment leaf = new LayoutFragment(
                placement.path(),
                0,
                node.padding().left(),
                node.padding().bottom(),
                width,
                height,
                new ImageFragmentPayload(
                        toImageData(node.imageData()),
                        node.fitMode(),
                        node.linkOptions(),
                        node.bookmarkOptions()));
        return wrapAtomicWithTransform(leaf, placement, node.transform());
    }
}
