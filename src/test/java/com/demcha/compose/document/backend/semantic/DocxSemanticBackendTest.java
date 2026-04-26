package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
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
}
