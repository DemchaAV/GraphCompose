package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.payloads.BarcodeFragmentPayload;
import com.demcha.compose.document.node.BarcodeNode;

import java.util.List;

import static com.demcha.compose.document.layout.NodeDefinitionSupport.EPS;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.toBarcodeData;
import static com.demcha.compose.document.layout.NodeDefinitionSupport.wrapAtomicWithTransform;

/**
 * Layout definition for {@link BarcodeNode}: a fixed-size atomic barcode or QR
 * fragment.
 *
 * @author Artem Demchyshyn
 */
public final class BarcodeDefinition implements NodeDefinition<BarcodeNode> {

    /**
     * Creates the barcode layout definition.
     */
    public BarcodeDefinition() {
    }

    @Override
    public Class<BarcodeNode> nodeType() {
        return BarcodeNode.class;
    }

    @Override
    public PreparedNode<BarcodeNode> prepare(BarcodeNode node, PrepareContext ctx, BoxConstraints constraints) {
        return PreparedNode.leaf(node, new MeasureResult(
                node.width() + node.padding().horizontal(),
                node.height() + node.padding().vertical()));
    }

    @Override
    public PaginationPolicy paginationPolicy(BarcodeNode node) {
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<BarcodeNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        BarcodeNode node = prepared.node();
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
                new BarcodeFragmentPayload(
                        toBarcodeData(node.barcodeOptions()),
                        node.linkOptions(),
                        node.bookmarkOptions()));
        return wrapAtomicWithTransform(leaf, placement, node.transform());
    }
}
