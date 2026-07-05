package com.demcha.compose.document.backend.semantic.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticExportManifest;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@code DocumentSession.export(new PptxSemanticBackend())} from the
 * graph-compose-render-pptx module. The semantic PPTX backend validates slide-safe
 * nodes and returns a manifest describing the document graph. Split out of
 * render-docx's {@code SessionSemanticExportTest} when the PPTX backend became its
 * own module.
 */
class PptxSemanticExportTest {

    @Test
    void pptxBackendShouldExportManifestFromDocumentGraph() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.add(new ContainerNode(
                    "PptxRoot",
                    List.of(new ShapeNode("Box", 40, 20, Color.BLUE, DocumentStroke.of(DocumentColor.BLACK, 1), DocumentInsets.zero(), DocumentInsets.zero())),
                    8,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null,
                    null));

            SemanticExportManifest pptx = session.export(new PptxSemanticBackend());
            assertThat(pptx.backendName()).isEqualTo("pptx-semantic");
            assertThat(pptx.nodeKinds()).contains("ContainerNode", "ShapeNode");
        }
    }
}
