package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Skeleton semantic PPTX backend that validates slide-safe semantic nodes.
 */
public final class PptxSemanticBackend implements SemanticBackend<SemanticExportManifest> {
    /**
     * Creates a semantic PPTX manifest backend.
     */
    public PptxSemanticBackend() {
    }

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

