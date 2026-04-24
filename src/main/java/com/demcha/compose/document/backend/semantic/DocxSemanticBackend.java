package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Skeleton semantic DOCX backend that validates semantic support and produces a
 * manifest for the future native writer layer.
 */
public final class DocxSemanticBackend implements SemanticBackend<SemanticExportManifest> {
    /**
     * Creates a semantic DOCX manifest backend.
     */
    public DocxSemanticBackend() {
    }

    @Override
    public String name() {
        return "docx-semantic";
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
                || node instanceof TableNode
                || node instanceof ContainerNode
                || node instanceof SectionNode)) {
            throw new UnsupportedNodeCapabilityException("DOCX backend does not support node type: " + node.getClass().getName());
        }
        kinds.add(node.nodeKind());
        for (DocumentNode child : node.children()) {
            collect(child, kinds);
        }
    }
}

