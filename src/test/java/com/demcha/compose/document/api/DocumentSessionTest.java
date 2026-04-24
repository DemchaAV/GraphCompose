package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.semantic.DocxSemanticBackend;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.semantic.PptxSemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportManifest;
import com.demcha.compose.document.exceptions.AtomicNodeTooLargeException;
import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.MeasureContext;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.NodeRegistry;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.PreparedSplitResult;
import com.demcha.compose.document.layout.SplitRequest;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentSessionTest {
    private static final float RENDER_DPI = 144.0f;
    private static final double RENDER_SCALE = RENDER_DPI / 72.0;

    @Test
    void atomicNodeShouldMoveWholeToNextPage() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(200, 200))
                .margin(Margin.of(10))
                .create()) {

            session.add(new ShapeNode("First", 80, 130, Color.LIGHT_GRAY, DocumentStroke.of(DocumentColor.BLACK, 1), DocumentInsets.zero(), DocumentInsets.zero()));
            session.add(new ShapeNode("Second", 80, 60, Color.GRAY, DocumentStroke.of(DocumentColor.BLACK, 1), DocumentInsets.zero(), DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.totalPages()).isEqualTo(2);
            assertThat(graph.nodes()).extracting(PlacedNode::path)
                    .containsExactly("First[0]", "Second[1]");
            assertThat(graph.nodes()).extracting(PlacedNode::startPage)
                    .containsExactly(0, 1);
            assertThat(graph.nodes()).extracting(PlacedNode::endPage)
                    .containsExactly(0, 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void pageFlowShortcutShouldAttachRootWithoutDslFacade() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(Margin.of(12))
                .create()) {

            ContainerNode root = session.pageFlow()
                    .name("ShortcutRoot")
                    .spacing(8)
                    .addParagraph("Shortcut paragraph", DocumentTextStyle.DEFAULT)
                    .build();

            assertThat(session.roots()).containsExactly(root);
            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(ParagraphNode.class);
            assertThat(((ParagraphNode) root.children().getFirst()).text()).isEqualTo("Shortcut paragraph");
            assertThat(session.layoutGraph().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void composeShortcutShouldBatchDslCallsWithoutManualBuilderLifecycle() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(Margin.of(12))
                .create()) {

            session.compose(dsl -> dsl.pageFlow(flow -> flow
                    .name("ComposeShortcut")
                    .spacing(6)
                    .addText("Composed paragraph")));

            assertThat(session.roots()).hasSize(1);
            assertThat(session.roots().getFirst()).isInstanceOf(ContainerNode.class);

            ContainerNode root = (ContainerNode) session.roots().getFirst();
            assertThat(root.name()).isEqualTo("ComposeShortcut");
            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(ParagraphNode.class);
            assertThat(((ParagraphNode) root.children().getFirst()).text()).isEqualTo("Composed paragraph");
            assertThat(session.layoutGraph().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void namedSectionShortcutShouldAvoidRepeatingSectionNameInsideNestedBuilder() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(Margin.of(12))
                .create()) {

            ContainerNode root = session.pageFlow()
                    .name("SectionShortcut")
                    .spacing(8)
                    .addSection("Profile", section -> section
                            .spacing(6)
                            .addText("Senior platform engineer"))
                    .build();

            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(SectionNode.class);

            SectionNode section = (SectionNode) root.children().getFirst();
            assertThat(section.name()).isEqualTo("Profile");
            assertThat(section.children()).hasSize(1);
            assertThat(section.children().getFirst()).isInstanceOf(ParagraphNode.class);
            assertThat(((ParagraphNode) section.children().getFirst()).text()).isEqualTo("Senior platform engineer");
            assertThat(session.layoutGraph().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void moduleShortcutShouldBuildTitledSemanticBlocks() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(320, 240))
                .margin(Margin.of(12))
                .create()) {

            ContainerNode root = session.pageFlow()
                    .name("ModuleShortcut")
                    .spacing(8)
                    .module("Technical Skills", module -> module
                            .titleMargin(DocumentInsets.bottom(4))
                            .spacing(2)
                            .paragraph("Practical backend engineering with clean document APIs.")
                            .bullets(List.of("Java", "SQL"))
                            .dashList("Spring Boot", "Docker")
                            .rows("Portfolio API - production backend", "GraphCompose - canonical engine"))
                    .build();

            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(SectionNode.class);

            SectionNode module = (SectionNode) root.children().getFirst();
            assertThat(module.name()).isEqualTo("TechnicalSkills");
            assertThat(module.children()).hasSize(5);
            assertThat(module.children().getFirst()).isInstanceOf(ParagraphNode.class);
            assertThat(((ParagraphNode) module.children().getFirst()).text()).isEqualTo("Technical Skills");
            assertThat(module.children().get(1)).isInstanceOf(ParagraphNode.class);
            assertThat(module.children().get(2)).isInstanceOf(ListNode.class);
            assertThat(module.children().get(3)).isInstanceOf(ListNode.class);
            assertThat(module.children().get(4)).isInstanceOf(ListNode.class);

            ListNode bullets = (ListNode) module.children().get(2);
            assertThat(bullets.items()).containsExactly("Java", "SQL");
            assertThat(bullets.marker().prefix()).isEqualTo("• ");

            ListNode rows = (ListNode) module.children().get(4);
            assertThat(rows.marker().isVisible()).isFalse();
            assertThat(rows.continuationIndent()).isEqualTo("  ");
            assertThat(session.layoutGraph().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void moduleShortcutShouldPaginateLongBodyThroughCanonicalCompositePath() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow(flow -> flow
                    .name("PagedModuleRoot")
                    .module("Long Module", module -> {
                        module.spacing(3);
                        for (int index = 0; index < 28; index++) {
                            module.paragraph("Module paragraph " + index + " keeps flowing through the canonical layout compiler.");
                        }
                    }));

            LayoutGraph graph = session.layoutGraph();
            PlacedNode moduleNode = graph.nodes().stream()
                    .filter(node -> "LongModule".equals(node.semanticName()))
                    .findFirst()
                    .orElseThrow();

            assertThat(graph.totalPages()).isGreaterThan(1);
            assertThat(moduleNode.startPage()).isEqualTo(0);
            assertThat(moduleNode.endPage()).isGreaterThan(0);
        }
    }

    @Test
    void tableConvenienceMethodsShouldBuildHeaderAndBulkRows() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(320, 220))
                .margin(Margin.of(12))
                .create()) {

            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .padding(DocumentInsets.of(6))
                    .build();

            ContainerNode root = session.pageFlow()
                    .name("TableShortcut")
                    .addTable(table -> table
                            .name("StatusTable")
                            .autoColumns(3)
                            .defaultCellStyle(DocumentTableStyle.builder()
                                    .padding(DocumentInsets.of(4))
                                    .build())
                            .headerStyle(headerStyle)
                            .header("Role", "Owner", "Status")
                            .rows(
                                    new String[]{"Engine", "GraphCompose", "Stable"},
                                    new String[]{"Feature", "Table Builder", "Canonical"}))
                    .build();

            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(TableNode.class);

            TableNode table = (TableNode) root.children().getFirst();
            assertThat(table.rows()).hasSize(3);
            assertThat(table.rows().getFirst())
                    .extracting(DocumentTableCell::lines)
                    .containsExactly(List.of("Role"), List.of("Owner"), List.of("Status"));
            assertThat(table.rows().get(1))
                    .extracting(DocumentTableCell::lines)
                    .containsExactly(List.of("Engine"), List.of("GraphCompose"), List.of("Stable"));
            assertThat(table.rowStyles().get(0)).isSameAs(headerStyle);
            assertThat(session.layoutGraph().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void atomicNodeTooLargeShouldFailDeterministically() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(200, 200))
                .margin(Margin.of(10))
                .create()) {

            session.add(new ShapeNode("TooBig", 80, 181, Color.RED, DocumentStroke.of(DocumentColor.BLACK, 1), DocumentInsets.zero(), DocumentInsets.zero()));

            assertThatThrownBy(session::layoutGraph)
                    .isInstanceOf(AtomicNodeTooLargeException.class)
                    .hasMessageContaining("TooBig[0]");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void paragraphShouldSplitAcrossPagesAndPdfShouldMatchPageCount() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(Margin.of(12))
                .create()) {

            session.add(new ParagraphNode(
                    "LongParagraph",
                    "GraphCompose keeps pagination in the engine. ".repeat(60),
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isGreaterThan(1);
            assertThat(graph.nodes()).hasSize(1);
            assertThat(graph.nodes().getFirst().startPage()).isEqualTo(0);
            assertThat(graph.nodes().getFirst().endPage()).isGreaterThan(0);
            assertThat(graph.fragments()).extracting(PlacedFragment::pageIndex).doesNotHaveDuplicates();
            assertThat(graph.fragments()).allSatisfy(fragment ->
                    assertThat(fragment.payload()).isInstanceOf(BuiltInNodeDefinitions.ParagraphFragmentPayload.class));

            BuiltInNodeDefinitions.ParagraphFragmentPayload firstPayload =
                    (BuiltInNodeDefinitions.ParagraphFragmentPayload) graph.fragments().getFirst().payload();
            BuiltInNodeDefinitions.ParagraphFragmentPayload lastPayload =
                    (BuiltInNodeDefinitions.ParagraphFragmentPayload) graph.fragments().getLast().payload();
            assertThat(firstPayload.padding().top()).isEqualTo(4.0);
            assertThat(firstPayload.padding().bottom()).isEqualTo(0.0);
            assertThat(lastPayload.padding().top()).isEqualTo(0.0);
            assertThat(lastPayload.padding().bottom()).isEqualTo(4.0);

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isEqualTo(graph.totalPages());
            }
        }
    }

    @Test
    void writePdfShouldStreamValidPdfWithoutClosingCallerStream() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow(page -> page
                    .module("Summary", module -> module.paragraph("Streaming PDF output stays request-local.")));

            TrackingOutputStream output = new TrackingOutputStream();

            session.writePdf(output);

            assertThat(output.closed).isFalse();
            assertThat(output.size()).isGreaterThan(0);
            try (PDDocument document = Loader.loadPDF(output.toByteArray())) {
                assertThat(document.getNumberOfPages()).isEqualTo(1);
                String extracted = normalizeWhitespace(new PDFTextStripper().getText(document));
                assertThat(extracted).contains("Streaming PDF output stays request-local.");
            }
        }
    }

    @Test
    void toPdfBytesShouldNotKeepSessionByteCache() throws Exception {
        List<String> fieldNames = Arrays.stream(DocumentSession.class.getDeclaredFields())
                .map(field -> field.getName())
                .toList();

        assertThat(fieldNames).doesNotContain("cachedPdfBytes", "cachedPdfRevision");

        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(Margin.of(12))
                .create()) {

            session.pageFlow(page -> page
                    .module("Summary", module -> module.paragraph("Byte output is convenience-only.")));

            byte[] pdfBytes = session.toPdfBytes();

            assertThat(pdfBytes).isNotEmpty();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isEqualTo(1);
            }
        }
    }

    @Test
    void paragraphShouldPreserveExplicitNewlinesInPreparedFragments() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 220))
                .margin(Margin.of(12))
                .create()) {

            session.add(new ParagraphNode(
                    "Paragraph",
                    "First line\n\nSecond line",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.fragments()).hasSize(1);

            BuiltInNodeDefinitions.ParagraphFragmentPayload payload =
                    (BuiltInNodeDefinitions.ParagraphFragmentPayload) graph.fragments().getFirst().payload();
            assertThat(payload.lines())
                    .extracting(BuiltInNodeDefinitions.ParagraphLine::text)
                    .containsExactly("First line", "", "Second line");
        }
    }

    @Test
    void paragraphPrepareShouldReuseMeasuredLayoutAcrossSplitAndEmit() throws Exception {
        NodeRegistry registry = BuiltInNodeDefinitions.registerDefaults(new NodeRegistry());
        ParagraphNode paragraph = new ParagraphNode(
                "LongParagraph",
                "Prepared paragraph layout should be reused across split and emit. ".repeat(35),
                DocumentTextStyle.DEFAULT,
                TextAlign.LEFT,
                2.0,
                DocumentInsets.of(4),
                DocumentInsets.zero());

        @SuppressWarnings("unchecked")
        NodeDefinition<ParagraphNode> definition = (NodeDefinition<ParagraphNode>) registry.definitionFor(paragraph);

        try (CountingPrepareContext context = new CountingPrepareContext(registry, new PDRectangle(220, 180), Margin.of(12))) {
            PreparedNode<ParagraphNode> prepared = definition.prepare(paragraph, context, new BoxConstraints(160, 1_000));

            int textWidthCallsAfterPrepare = context.textMeasurement().textWidthCalls();
            int lineMetricsCallsAfterPrepare = context.textMeasurement().lineMetricsCalls();

            PreparedSplitResult<ParagraphNode> split = definition.split(prepared, new SplitRequest(
                    new BoxConstraints(160, 44),
                    44,
                    156,
                    context));

            assertThat(split.head()).isNotNull();
            assertThat(split.tail()).isNotNull();

            emitParagraph(definition, split.head(), context, 0);
            emitParagraph(definition, split.tail(), context, 1);

            assertThat(context.textMeasurement().textWidthCalls()).isEqualTo(textWidthCallsAfterPrepare);
            assertThat(context.textMeasurement().lineMetricsCalls()).isEqualTo(lineMetricsCallsAfterPrepare);
        }
    }

    @Test
    void tableShouldSplitByAtomicRows() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(Margin.of(12))
                .create()) {

            TableNode table = new TableNode(
                    "Table",
                    List.of(DocumentTableColumn.auto()),
                    List.of(
                            List.of(DocumentTableCell.text("row 1")),
                            List.of(DocumentTableCell.text("row 2")),
                            List.of(DocumentTableCell.text("row 3")),
                            List.of(DocumentTableCell.text("row 4")),
                            List.of(DocumentTableCell.text("row 5")),
                            List.of(DocumentTableCell.text("row 6")),
                            List.of(DocumentTableCell.text("row 7")),
                            List.of(DocumentTableCell.text("row 8"))),
                    DocumentTableStyle.empty(),
                    160.0,
                    DocumentInsets.zero(),
                    DocumentInsets.zero());

            session.add(table);
            LayoutGraph graph = session.layoutGraph();

            List<PlacedFragment> rowFragments = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.TableRowFragmentPayload)
                    .toList();

            assertThat(rowFragments).hasSize(8);
            assertThat(rowFragments).extracting(PlacedFragment::pageIndex).contains(0, 1);

            int previousPage = -1;
            for (PlacedFragment fragment : rowFragments) {
                BuiltInNodeDefinitions.TableRowFragmentPayload payload =
                        (BuiltInNodeDefinitions.TableRowFragmentPayload) fragment.payload();
                if (fragment.pageIndex() != previousPage) {
                    assertThat(payload.startsPageFragment()).isTrue();
                    previousPage = fragment.pageIndex();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void customNodeShouldWorkViaRegisteredDefinitionAndBackend() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(200, 160))
                .margin(Margin.of(10))
                .create()) {

            session.registerNodeDefinition(new BadgeNodeDefinition());
            session.add(new BadgeNode("Badge", "alpha", DocumentInsets.zero(), DocumentInsets.zero()));

            FixedLayoutBackend<List<String>> backend = new FixedLayoutBackend<>() {
                @Override
                public String name() {
                    return "badge-backend";
                }

                @Override
                public List<String> render(LayoutGraph graph, FixedLayoutRenderContext context) {
                    return graph.fragments().stream()
                            .map(fragment -> (String) fragment.payload())
                            .toList();
                }
            };

            List<String> rendered = session.render(backend);
            assertThat(rendered).containsExactly("alpha");
        }
    }

    @Test
    void semanticBackendsShouldExportManifestsFromDocumentGraph() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(200, 200))
                .margin(Margin.of(10))
                .create()) {

            session.add(new ContainerNode(
                    "DocxRoot",
                    List.of(new ParagraphNode("P", "Hello world", DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0, DocumentInsets.zero(), DocumentInsets.zero())),
                    8,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null,
                    null));

            SemanticExportManifest docx = session.export(new DocxSemanticBackend());

            assertThat(docx.backendName()).isEqualTo("docx-semantic");
            assertThat(docx.nodeKinds()).contains("ContainerNode", "ParagraphNode");

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

    @Test
    void documentLevelPdfOptionsShouldReuseCompiledLayout() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(Margin.of(12))
                .create()) {

            session.add(new ParagraphNode(
                    "Paragraph",
                    "Layout should stay cached while only PDF chrome changes.",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            LayoutGraph graph = session.layoutGraph();

            session.metadata(PdfMetadataOptions.builder().title("Chrome test").build())
                    .watermark(PdfWatermarkOptions.builder().text("DRAFT").build())
                    .header(PdfHeaderFooterOptions.builder().centerText("Header").build())
                    .guideLines(true);

            assertThat(session.layoutGraph()).isSameAs(graph);
        }
    }

    @Test
    void paragraphShouldRenderMarkdownStylesByDefault() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(Margin.of(12))
                .create()) {

            session.add(new ParagraphNode(
                    "Markdown",
                    "Plain **bold** and *italic* text",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                String extracted = normalizeWhitespace(new PDFTextStripper().getText(document));
                assertThat(extracted).contains("Plain bold and italic text");
                assertThat(extracted).doesNotContain("**bold**");
                assertThat(extracted).doesNotContain("*italic*");
                assertThat(pdfUsesFont(document, "Helvetica-Bold")).isTrue();
                assertThat(pdfUsesFont(document, "Helvetica-Oblique")).isTrue();
            }
        }
    }

    @Test
    void paragraphShouldKeepMarkdownMarkersWhenDisabled() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(Margin.of(12))
                .markdown(false)
                .create()) {

            session.add(new ParagraphNode(
                    "MarkdownDisabled",
                    "Plain **bold** and *italic* text",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(4),
                    DocumentInsets.zero()));

            byte[] pdfBytes = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                String extracted = normalizeWhitespace(new PDFTextStripper().getText(document));
                assertThat(extracted).contains("Plain **bold** and *italic* text");
                assertThat(pdfUsesFont(document, "Helvetica-Bold")).isFalse();
                assertThat(pdfUsesFont(document, "Helvetica-Oblique")).isFalse();
            }
        }
    }

    @Test
    void guideLinesShouldRenderLegacyMarginPaddingAndBoxColors() throws Exception {
        Color marginColor = new Color(0, 110, 255);
        Color paddingColor = new Color(255, 140, 0);
        Color boxColor = new Color(150, 150, 150);

        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(260, 200))
                .margin(Margin.of(12))
                .guideLines(true)
                .create()) {

            session.add(new ParagraphNode(
                    "GuidedParagraph",
                    "Guide colors should expose margin and padding overlays.",
                    DocumentTextStyle.DEFAULT,
                    TextAlign.LEFT,
                    2.0,
                    DocumentInsets.of(10),
                    DocumentInsets.of(14)));

            LayoutGraph graph = session.layoutGraph();
            byte[] pdfBytes = session.toPdfBytes();

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, RENDER_DPI);
                PlacedFragment fragment = graph.fragments().getFirst();

                assertThat(hasColorNear(
                        image,
                        fragment.x() - fragment.margin().left(),
                        fragment.y() - fragment.margin().bottom(),
                        marginColor,
                        10)).isTrue();
                assertThat(hasColorNear(
                        image,
                        fragment.x() + fragment.padding().left(),
                        fragment.y() + fragment.padding().bottom(),
                        paddingColor,
                        10)).isTrue();
                assertThat(hasColorNear(
                        image,
                        fragment.x(),
                        fragment.y(),
                        boxColor,
                        6)).isTrue();
            }
        }
    }

    private static final class TrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    private record BadgeNode(String name, String label, DocumentInsets margin, DocumentInsets padding) implements DocumentNode {
    }

    private static final class BadgeNodeDefinition implements NodeDefinition<BadgeNode> {
        @Override
        public Class<BadgeNode> nodeType() {
            return BadgeNode.class;
        }

        @Override
        public PreparedNode<BadgeNode> prepare(BadgeNode node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(40, 18));
        }

        @Override
        public PaginationPolicy paginationPolicy(BadgeNode node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<BadgeNode> prepared, FragmentContext ctx, FragmentPlacement placement) {
            return List.of(new LayoutFragment(
                    placement.path(),
                    0,
                    0,
                    0,
                    placement.width(),
                    placement.height(),
                    prepared.node().label()));
        }
    }

    private void emitParagraph(NodeDefinition<ParagraphNode> definition,
                               PreparedNode<ParagraphNode> prepared,
                               CountingPrepareContext context,
                               int pageIndex) {
        definition.emitFragments(prepared, context, new FragmentPlacement(
                prepared.node().name() + "[" + pageIndex + "]",
                null,
                0,
                1,
                pageIndex,
                0,
                0,
                prepared.measureResult().width(),
                prepared.measureResult().height(),
                pageIndex,
                pageIndex,
                toMargin(prepared.node().margin()),
                toPadding(prepared.node().padding())));
    }

    private static Margin toMargin(DocumentInsets insets) {
        return new Margin(insets.top(), insets.right(), insets.bottom(), insets.left());
    }

    private static Padding toPadding(DocumentInsets insets) {
        return new Padding(insets.top(), insets.right(), insets.bottom(), insets.left());
    }

    private static final class CountingPrepareContext implements PrepareContext, FragmentContext, AutoCloseable {
        private final NodeRegistry registry;
        private final LayoutCanvas canvas;
        private final PDDocument measurementDocument;
        private final FontLibrary fonts;
        private final CountingTextMeasurementSystem textMeasurement;

        private CountingPrepareContext(NodeRegistry registry, PDRectangle pageSize, Margin margin) throws Exception {
            this.registry = registry;
            this.canvas = LayoutCanvas.from(pageSize, margin);
            this.measurementDocument = new PDDocument();
            this.fonts = DefaultFonts.library(measurementDocument);
            this.textMeasurement = new CountingTextMeasurementSystem(
                    new FontLibraryTextMeasurementSystem(fonts, PdfFont.class));
        }

        @Override
        public <E extends DocumentNode> PreparedNode<E> prepare(E node, BoxConstraints constraints) {
            @SuppressWarnings("unchecked")
            NodeDefinition<E> definition = (NodeDefinition<E>) registry.definitionFor(node);
            return definition.prepare(node, this, constraints);
        }

        @Override
        public FontLibrary fonts() {
            return fonts;
        }

        @Override
        public CountingTextMeasurementSystem textMeasurement() {
            return textMeasurement;
        }

        @Override
        public LayoutCanvas canvas() {
            return canvas;
        }

        @Override
        public boolean markdownEnabled() {
            return true;
        }

        @Override
        public void close() throws Exception {
            measurementDocument.close();
        }
    }

    private static final class CountingTextMeasurementSystem implements TextMeasurementSystem {
        private final TextMeasurementSystem delegate;
        private int textWidthCalls;
        private int lineMetricsCalls;

        private CountingTextMeasurementSystem(TextMeasurementSystem delegate) {
            this.delegate = delegate;
        }

        @Override
        public ContentSize measure(TextStyle style, String text) {
            return delegate.measure(style, text);
        }

        @Override
        public double textWidth(TextStyle style, String text) {
            textWidthCalls++;
            return delegate.textWidth(style, text);
        }

        @Override
        public LineMetrics lineMetrics(TextStyle style) {
            lineMetricsCalls++;
            return delegate.lineMetrics(style);
        }

        int textWidthCalls() {
            return textWidthCalls;
        }

        int lineMetricsCalls() {
            return lineMetricsCalls;
        }
    }

    private boolean pdfUsesFont(PDDocument document, String expectedNameFragment) throws Exception {
        for (PDPage page : document.getPages()) {
            for (var resourceFontName : page.getResources().getFontNames()) {
                var font = page.getResources().getFont(resourceFontName);
                if (font != null && normalizeWhitespace(font.getName()).contains(normalizeWhitespace(expectedNameFragment))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private boolean hasColorNear(BufferedImage image,
                                 double pdfX,
                                 double pdfY,
                                 Color expected,
                                 int radius) {
        int centerX = (int) Math.round(pdfX * RENDER_SCALE);
        int centerY = image.getHeight() - 1 - (int) Math.round(pdfY * RENDER_SCALE);

        for (int y = Math.max(0, centerY - radius); y <= Math.min(image.getHeight() - 1, centerY + radius); y++) {
            for (int x = Math.max(0, centerX - radius); x <= Math.min(image.getWidth() - 1, centerX + radius); x++) {
                if (isCloseColor(new Color(image.getRGB(x, y), true), expected, 35)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCloseColor(Color actual, Color expected, int tolerancePerChannel) {
        return Math.abs(actual.getRed() - expected.getRed()) <= tolerancePerChannel
                && Math.abs(actual.getGreen() - expected.getGreen()) <= tolerancePerChannel
                && Math.abs(actual.getBlue() - expected.getBlue()) <= tolerancePerChannel;
    }
}

