package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DOCX geometry-drop behaviour: overlay/positioned wrappers recurse so their
 * semantic children (text, images) survive the Word export, while pure
 * geometry leaves (paths, polygons) are dropped without throwing — the
 * fixed-layout PDF backend is the path for pixel-perfect geometry.
 */
@DisabledIfSystemProperty(named = "no.poi", matches = "true",
        disabledReason = "DocxSemanticBackend requires poi-ooxml")
class DocxGeometryDropTest {

    @Test
    void layerStackRecursesSoChildTextSurvives() throws Exception {
        List<String> texts = exportTexts(flow -> flow.addLayerStack(stack -> stack
                .name("Overlay")
                .layer(new com.demcha.compose.document.dsl.PathBuilder()
                        .name("Backdrop").size(120, 40)
                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath()
                        .fillColor(DocumentColor.rgb(20, 80, 95))
                        .build())
                .layer(new com.demcha.compose.document.dsl.ParagraphBuilder()
                        .text("Caption over the shape")
                        .build())));

        assertThat(texts).contains("Caption over the shape");
    }

    @Test
    void standaloneGeometryIsDroppedWithoutThrowing() throws Exception {
        // A bare path has no semantic Word analogue; the export completes and
        // simply carries no paragraph for it.
        List<String> texts = exportTexts(flow -> flow.addPath(path -> path
                .name("Wave").size(200, 40)
                .moveTo(0, 0.5).curveTo(0.25, 1, 0.75, 0, 1, 0.5)
                .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 2))));

        assertThat(texts).allMatch(String::isBlank);
    }

    private static List<String> exportTexts(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            var flow = session.dsl().pageFlow().name("Flow");
            author.accept(flow);
            flow.build();
            docxBytes = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .toList();
        }
    }
}
