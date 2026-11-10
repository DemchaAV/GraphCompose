package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A path in the Word export: drawn as custom geometry whether it stands in the flow on its own
 * or as a layer of a stack, and a stack that holds one still writes its text.
 */
class DocxPathGeometryTest {

    @Test
    void aLayerStackKeepsItsTextAndDrawsItsPath() throws Exception {
        Exported exported = export(flow -> flow.addLayerStack(stack -> stack
                .name("Overlay")
                .layer(new com.demcha.compose.document.dsl.PathBuilder()
                        .name("Backdrop").size(120, 40)
                        .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath()
                        .fillColor(DocumentColor.rgb(20, 80, 95))
                        .build())
                .layer(new com.demcha.compose.document.dsl.ParagraphBuilder()
                        .text("Caption over the shape")
                        .build())));

        assertThat(exported.texts()).contains("Caption over the shape");
        assertThat(exported.customGeometries()).as("the backdrop, drawn").isEqualTo(1);
        assertThat(exported.report().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    @Test
    void aPathOnItsOwnIsDrawnAsCustomGeometry() throws Exception {
        Exported exported = export(flow -> flow.addPath(path -> path
                .name("Wave").size(200, 40)
                .moveTo(0, 0.5).curveTo(0.25, 1, 0.75, 0, 1, 0.5)
                .stroke(DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 2))));

        assertThat(exported.customGeometries()).isEqualTo(1);
        assertThat(exported.xml()).contains("<a:cubicBezTo>");
        assertThat(exported.texts()).as("a drawing, not text").allMatch(String::isBlank);
        assertThat(exported.report().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    private record Exported(List<String> texts, String xml, DocxExportReport report) {

        long customGeometries() {
            return xml.split("<a:custGeom>", -1).length - 1L;
        }
    }

    private static Exported export(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            var flow = session.dsl().pageFlow().name("Flow");
            author.accept(flow);
            flow.build();
            docxBytes = session.export(new DocxSemanticBackend(report::set));
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            return new Exported(
                    document.getParagraphs().stream().map(XWPFParagraph::getText).toList(),
                    document.getDocument().xmlText(),
                    report.get());
        }
    }
}
