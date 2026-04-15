package com.demcha.compose.v2.backends;

import com.demcha.compose.v2.DocumentGraph;
import com.demcha.compose.v2.DocumentNode;
import com.demcha.compose.v2.SemanticBackend;
import com.demcha.compose.v2.SemanticExportContext;
import com.demcha.compose.v2.SemanticExportManifest;
import com.demcha.compose.v2.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.v2.nodes.ContainerNode;
import com.demcha.compose.v2.nodes.ImageNode;
import com.demcha.compose.v2.nodes.ParagraphNode;
import com.demcha.compose.v2.nodes.SectionNode;
import com.demcha.compose.v2.nodes.ShapeNode;
import com.demcha.compose.v2.nodes.TableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Skeleton semantic PPTX backend that validates slide-safe semantic nodes.
 */
public final class PptxSemanticBackend implements SemanticBackend<SemanticExportManifest> {
    @Override
    public String name() {
        return "pptx-semantic";
    }

    @Override
    public SemanticExportManifest export(DocumentGraph graph, SemanticExportContext context) {
        List<String> kinds = new ArrayList<>();
        for (DocumentNode root : graph.roots()) {
            collect(root, kinds);
        }
        return new SemanticExportManifest(name(), context.outputFile(), graph.roots().size(), kinds);
    }

    private void collect(DocumentNode node, List<String> kinds) {
        if (!(node instanceof ParagraphNode
                || node instanceof ImageNode
                || node instanceof ShapeNode
                || node instanceof TableNode
                || node instanceof ContainerNode
                || node instanceof SectionNode)) {
            throw new UnsupportedNodeCapabilityException("PPTX backend does not support node type: " + node.getClass().getName());
        }
        kinds.add(node.nodeKind());
        for (DocumentNode child : node.children()) {
            collect(child, kinds);
        }
    }
}
