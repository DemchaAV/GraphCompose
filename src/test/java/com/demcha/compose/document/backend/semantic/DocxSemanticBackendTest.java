package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocxSemanticBackendTest {

    @Test
    void exportProducesDocxWithParagraphAndTableContent() throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .name("Title")
                            .text("Hello Word")
                            .textStyle(DocumentTextStyle.builder().size(20).build()))
                    .addTable(table -> table
                            .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                            .row("R1C1", "R1C2")
                            .row("R2C1", "R2C2"))
                    .build();

            docxBytes = session.export(new DocxSemanticBackend());
        }

        assertThat(docxBytes).isNotEmpty();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            assertThat(paragraphs).isNotEmpty();
            assertThat(paragraphs.stream().map(XWPFParagraph::getText))
                    .anyMatch(text -> text.contains("Hello Word"));

            List<XWPFTable> tables = document.getTables();
            assertThat(tables).hasSize(1);
            XWPFTable table = tables.get(0);
            assertThat(table.getRows()).hasSize(2);
            assertThat(table.getRow(0).getCell(0).getText().trim()).isEqualTo("R1C1");
            assertThat(table.getRow(1).getCell(1).getText().trim()).isEqualTo("R2C2");
        }
    }

    @Test
    void exportPropagatesSessionLevelMetadataIntoCoreProperties() throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            session.metadata(DocumentMetadata.builder()
                    .title("Q1 Report")
                    .author("Engineering")
                    .subject("Quarterly summary")
                    .keywords("docx, metadata, report")
                    .build());

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph("Body content", DocumentTextStyle.DEFAULT)
                    .build();

            docxBytes = session.export(new DocxSemanticBackend());
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            var props = document.getProperties().getCoreProperties();
            assertThat(props.getTitle()).isEqualTo("Q1 Report");
            assertThat(props.getCreator()).isEqualTo("Engineering");
            assertThat(props.getKeywords()).isEqualTo("docx, metadata, report");
        }
    }

    @Test
    void shapeContainerExportsLayersInlineWithoutOutline() throws Exception {
        // POI/DOCX has no portable equivalent of a graphics-state path
        // clip, so the canonical contract (canonical-legacy-parity.md)
        // is for DocxSemanticBackend to render a ShapeContainer's layers
        // inline — the inner paragraph survives, but the circle outline
        // is not drawn. The single capability warning is logged once per
        // export pass.
        ParagraphNode label = new ParagraphBuilder()
                .text("Featured")
                .build();
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            session.add(new ShapeContainerBuilder()
                    .name("Highlight")
                    .circle(80.0)
                    .fillColor(DocumentColor.rgb(180, 40, 40))
                    .center(label)
                    .build());

            docxBytes = session.export(new DocxSemanticBackend());
        }

        assertThat(docxBytes).isNotEmpty();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            assertThat(document.getParagraphs().stream().map(XWPFParagraph::getText))
                    .as("layer paragraph text survives the ShapeContainer fallback")
                    .anyMatch(text -> text.contains("Featured"));
        }
    }
}
