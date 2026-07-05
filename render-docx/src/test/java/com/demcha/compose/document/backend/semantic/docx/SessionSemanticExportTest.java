package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@code DocumentSession.export(new DocxSemanticBackend())} from its new
 * home. Moved out of the engine's {@code DocumentSessionTest} when the DOCX backend
 * was extracted to graph-compose-render-docx (the engine test scope can no longer
 * see it). POI is always present in this module, so the old no-poi guard is dropped.
 * The PPTX backend moved to graph-compose-render-pptx and is covered there.
 */
class SessionSemanticExportTest {

    @Test
    void docxBackendShouldExportZipPackageFromDocumentGraph() throws Exception {
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
        }
    }
}
