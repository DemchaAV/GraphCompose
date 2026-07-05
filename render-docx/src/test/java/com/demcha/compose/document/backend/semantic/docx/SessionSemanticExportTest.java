package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticExportManifest;
import com.demcha.compose.document.backend.semantic.pptx.PptxSemanticBackend;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@code DocumentSession.export(...)} with the semantic office backends
 * from their new home. Moved out of the engine's {@code DocumentSessionTest} when
 * the DOCX/PPTX backends were extracted to graph-compose-render-docx (the engine
 * test scope can no longer see them). POI is always present in this module, so the
 * old no-poi guard is dropped.
 */
class SessionSemanticExportTest {

    @Test
    void semanticBackendsShouldExportManifestsFromDocumentGraph() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.add(new ContainerNode(
                    "DocxRoot",
                    List.of(new ParagraphNode("P", "Hello world", DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0, DocumentInsets.zero(), DocumentInsets.zero())),
                    8,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null,
                    null));

            byte[] docx = session.export(new DocxSemanticBackend());

            assertThat(docx).isNotEmpty();
            // DOCX files are ZIP-archived OOXML packages; the first two bytes
            // of any ZIP container are the local-file-header signature.
            assertThat(docx[0]).isEqualTo((byte) 'P');
            assertThat(docx[1]).isEqualTo((byte) 'K');

            session.clear();
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
