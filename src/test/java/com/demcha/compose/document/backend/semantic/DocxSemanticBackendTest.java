package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.ListMarker;
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
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DOCX semantic backend output. Skipped under the {@code -P no-poi}
 * profile, where {@code poi-ooxml} is intentionally absent from the
 * test classpath to validate that consumers who don't render DOCX can
 * still build and run the canonical suite without the optional POI
 * footprint (Track I1).
 */
@DisabledIfSystemProperty(named = "no.poi", matches = "true",
        disabledReason = "DocxSemanticBackend requires poi-ooxml; the no-poi profile validates the rest of the suite without it")
class DocxSemanticBackendTest {

    @Test
    void chartExportsAsItsDataTable() throws Exception {
        com.demcha.compose.document.chart.ChartData data =
                com.demcha.compose.document.chart.ChartData.builder()
                        .categories("Q1", "Q2")
                        .series("2024", 12.4, 15.1)
                        .series("2025", 14.0, 18.2)
                        .build();
        com.demcha.compose.document.chart.ChartSpec spec =
                com.demcha.compose.document.chart.ChartSpec.bar().data(data).build();

        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .chart(spec)
                    .build();
            docxBytes = session.export(new DocxSemanticBackend());
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<XWPFTable> tables = document.getTables();
            assertThat(tables).hasSize(1);
            XWPFTable table = tables.get(0);
            // Header (blank corner + series names) plus one row per category.
            assertThat(table.getRows()).hasSize(3);
            assertThat(table.getRow(0).getCell(1).getText()).isEqualTo("2024");
            assertThat(table.getRow(0).getCell(2).getText()).isEqualTo("2025");
            assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("Q1");
            assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("12.4");
            assertThat(table.getRow(2).getCell(2).getText()).isEqualTo("18.2");
        }
    }

    @Test
    void listsExportAsMarkerPrefixedParagraphs() throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addList("First", "Second")
                    .build();
            docxBytes = session.export(new DocxSemanticBackend());
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<String> texts = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText).toList();
            assertThat(texts).anyMatch(t -> t.endsWith("First") && t.length() > "First".length());
            assertThat(texts).anyMatch(t -> t.endsWith("Second"));
        }
    }

    @Test
    void nestedListItemsIndentTwoSpacesPerDepth() throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addList(list -> list
                            .name("Outline")
                            .addItem("Level zero", l1 -> l1
                                    .addItem("Level one", l2 -> l2
                                            .addItem("Level two"))))
                    .build();
            docxBytes = session.export(new DocxSemanticBackend());
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<String> texts = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText).toList();
            // Two spaces of indent per depth; without per-item markers the
            // semantic export falls back to the list's top-level bullet at
            // every level (the visual depth cascade is a layout-pass concern).
            assertThat(texts).contains(
                    "• Level zero",
                    "  • Level one",
                    "    • Level two");
        }
    }

    @Test
    void nestedListItemsKeepTheirCustomMarkers() throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addList(list -> list
                            .marker("→")
                            .markerFor(1, ListMarker.custom("‣"))
                            .addItem("Root", child -> child.addItem("Child")))
                    .build();
            docxBytes = session.export(new DocxSemanticBackend());
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<String> texts = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText).toList();
            // The top-level custom marker and the per-depth override both
            // survive the export.
            assertThat(texts).contains(
                    "→ Root",
                    "  ‣ Child");
        }
    }

    @Test
    void listInsideASectionIsExported() throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addSection("Wrapper", s -> s.addList("Inside section"))
                    .build();
            docxBytes = session.export(new DocxSemanticBackend());
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<String> texts = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText).toList();
            // writeNode recurses through section/container wrappers, so the
            // nested list is not dropped.
            assertThat(texts).contains("• Inside section");
        }
    }

    @Test
    void emptyListExportsNothingAndDoesNotFail() throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(paragraph -> paragraph.text("Before"))
                    .addList(list -> list.name("Empty"))
                    .build();
            docxBytes = session.export(new DocxSemanticBackend());
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<String> texts = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText).toList();
            assertThat(texts).contains("Before");
            assertThat(texts).noneMatch(t -> t.contains("•"));
        }
    }

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
