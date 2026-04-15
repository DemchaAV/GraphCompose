package com.demcha.compose;

import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.table.TableResolvedCell;
import com.demcha.compose.layout_core.components.content.table.TableRowData;
import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.components.content.text.TextDataBody;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.core.EntityName;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.AbstractDocumentComposer;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.compose.layout_core.debug.LayoutNodeSnapshot;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.v2.BuiltInNodeDefinitions;
import com.demcha.compose.v2.DocumentSession;
import com.demcha.compose.v2.LayoutGraph;
import com.demcha.compose.v2.PlacedFragment;
import com.demcha.compose.v2.nodes.ContainerNode;
import com.demcha.compose.v2.nodes.ParagraphNode;
import com.demcha.compose.v2.nodes.ShapeNode;
import com.demcha.compose.v2.nodes.TableNode;
import com.demcha.compose.v2.nodes.TextAlign;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArchitectureComparisonParityTest {

    private static final PDRectangle SIMPLE_PAGE_SIZE = new PDRectangle(320, 260);
    private static final PDRectangle FLOW_PAGE_SIZE = new PDRectangle(220, 180);
    private static final PDRectangle TABLE_PAGE_SIZE = new PDRectangle(240, 220);
    private static final Margin SIMPLE_MARGIN = Margin.of(18);
    private static final Margin FLOW_MARGIN = Margin.of(12);
    private static final Margin TABLE_MARGIN = Margin.of(12);
    private static final double GEOMETRY_TOLERANCE = 0.05;
    private static final String LONG_PARAGRAPH = ("GraphCompose keeps pagination in the engine so element extensions only " +
            "need to explain measurement, legal split points, and rendering. ").repeat(55);

    private static final TextStyle TITLE_STYLE = TextStyle.builder()
            .size(18)
            .decoration(TextDecoration.BOLD)
            .color(new Color(18, 40, 74))
            .build();
    private static final TextStyle BODY_STYLE = TextStyle.builder()
            .size(9.5)
            .decoration(TextDecoration.DEFAULT)
            .color(new Color(58, 69, 84))
            .build();
    private static final TableCellStyle TABLE_STYLE = TableCellStyle.builder()
            .padding(Padding.of(4))
            .fillColor(ComponentColor.WHITE)
            .stroke(new Stroke(ComponentColor.BLACK, 1.0))
            .textStyle(BODY_STYLE)
            .textAnchor(Anchor.centerLeft())
            .build();

    @Test
    void simpleFlowShouldMatchSnapshotGeometryAndPdfTextBetweenLegacyAndV2() throws Exception {
        SnapshotRender legacy = renderLegacySimple();
        SnapshotRender v2 = renderV2Simple();

        assertPageCountsMatch(legacy.snapshot(), legacy.pdfBytes());
        assertPageCountsMatch(v2.snapshot(), v2.pdfBytes());
        assertThat(v2.snapshot().totalPages()).isEqualTo(legacy.snapshot().totalPages());
        assertThat(normalizedPdfText(v2.pdfBytes())).isEqualTo(normalizedPdfText(legacy.pdfBytes()));

        assertNodeEquivalent(legacy.snapshot(), v2.snapshot(), "SimpleTitle");
        assertNodeEquivalent(legacy.snapshot(), v2.snapshot(), "SimpleBody");
    }

    @Test
    void paragraphFlowShouldMatchWrappingPaginationAndPdfTextBetweenLegacyAndV2() throws Exception {
        ParagraphOutcome legacy = renderLegacyParagraph();
        ParagraphOutcome v2 = renderV2Paragraph();

        assertPageCountsMatch(legacy.snapshot(), legacy.pdfBytes());
        assertPageCountsMatch(v2.snapshot(), v2.pdfBytes());
        assertThat(v2.snapshot().totalPages()).isEqualTo(legacy.snapshot().totalPages());
        assertThat(normalizedPdfText(v2.pdfBytes())).isEqualTo(normalizedPdfText(legacy.pdfBytes()));

        assertNormalizedPageSpan(legacy.snapshot(), v2.snapshot(), "LongParagraph");
        assertThat(v2.lines()).extracting(ParagraphWrappedLine::text)
                .containsExactlyElementsOf(legacy.lines().stream().map(ParagraphWrappedLine::text).toList());
        assertThat(v2.lines()).extracting(ParagraphWrappedLine::page)
                .containsExactlyElementsOf(legacy.lines().stream().map(ParagraphWrappedLine::page).toList());
        assertThat(pageLineCounts(v2.lines())).isEqualTo(pageLineCounts(legacy.lines()));
    }

    @Test
    void tableFlowShouldMatchAtomicRowsCellGeometryAndPdfTextBetweenLegacyAndV2() throws Exception {
        TableOutcome legacy = renderLegacyTable();
        TableOutcome v2 = renderV2Table();

        assertPageCountsMatch(legacy.snapshot(), legacy.pdfBytes());
        assertPageCountsMatch(v2.snapshot(), v2.pdfBytes());
        assertThat(v2.snapshot().totalPages()).isEqualTo(legacy.snapshot().totalPages());
        assertThat(normalizedPdfText(v2.pdfBytes())).isEqualTo(normalizedPdfText(legacy.pdfBytes()));

        assertNormalizedPageSpan(legacy.snapshot(), v2.snapshot(), "BenchmarkTable");
        assertThat(v2.rows()).extracting(TableRowSummary::rowName)
                .containsExactlyElementsOf(legacy.rows().stream().map(TableRowSummary::rowName).toList());

        for (int rowIndex = 0; rowIndex < legacy.rows().size(); rowIndex++) {
            TableRowSummary legacyRow = legacy.rows().get(rowIndex);
            TableRowSummary v2Row = v2.rows().get(rowIndex);

            assertThat(v2Row.page()).isEqualTo(legacyRow.page());
            assertThat(v2Row.cells()).hasSameSizeAs(legacyRow.cells());

            for (int cellIndex = 0; cellIndex < legacyRow.cells().size(); cellIndex++) {
                TableCellSummary legacyCell = legacyRow.cells().get(cellIndex);
                TableCellSummary v2Cell = v2Row.cells().get(cellIndex);

                assertThat(v2Cell.name()).isEqualTo(legacyCell.name());
                assertThat(v2Cell.x()).isCloseTo(legacyCell.x(), within(GEOMETRY_TOLERANCE));
                assertThat(v2Cell.width()).isCloseTo(legacyCell.width(), within(GEOMETRY_TOLERANCE));
                assertThat(v2Cell.height()).isCloseTo(legacyCell.height(), within(GEOMETRY_TOLERANCE));
                assertThat(v2Cell.lines()).containsExactlyElementsOf(legacyCell.lines());
                assertThat(v2Cell.borderSides()).isEqualTo(legacyCell.borderSides());
            }
        }
    }

    private SnapshotRender renderLegacySimple() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacySimpleDocument(composer);
            LayoutSnapshot snapshot = composer.layoutSnapshot();
            byte[] pdfBytes = composer.toBytes();
            return new SnapshotRender(snapshot, pdfBytes);
        }
    }

    private SnapshotRender renderV2Simple() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .create()) {
            composeV2SimpleDocument(session);
            LayoutSnapshot snapshot = session.layoutSnapshot();
            byte[] pdfBytes = session.toPdfBytes();
            return new SnapshotRender(snapshot, pdfBytes);
        }
    }

    private ParagraphOutcome renderLegacyParagraph() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyParagraphDocument(composer);
            LayoutSnapshot snapshot = composer.layoutSnapshot();
            List<ParagraphWrappedLine> lines = legacyParagraphLines(composer, "LongParagraph");
            byte[] pdfBytes = composer.toBytes();
            return new ParagraphOutcome(snapshot, pdfBytes, lines);
        }
    }

    private ParagraphOutcome renderV2Paragraph() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .create()) {
            composeV2ParagraphDocument(session);
            LayoutSnapshot snapshot = session.layoutSnapshot();
            List<ParagraphWrappedLine> lines = v2ParagraphLines(session);
            byte[] pdfBytes = session.toPdfBytes();
            return new ParagraphOutcome(snapshot, pdfBytes, lines);
        }
    }

    private TableOutcome renderLegacyTable() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyTableDocument(composer);
            LayoutSnapshot snapshot = composer.layoutSnapshot();
            List<TableRowSummary> rows = legacyTableRows(composer);
            byte[] pdfBytes = composer.toBytes();
            return new TableOutcome(snapshot, pdfBytes, rows);
        }
    }

    private TableOutcome renderV2Table() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .create()) {
            composeV2TableDocument(session);
            LayoutSnapshot snapshot = session.layoutSnapshot();
            List<TableRowSummary> rows = v2TableRows(session);
            byte[] pdfBytes = session.toPdfBytes();
            return new TableOutcome(snapshot, pdfBytes, rows);
        }
    }

    private void composeLegacySimpleDocument(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();

        cb.vContainer(Align.middle(8))
                .entityName("SimpleRoot")
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .addChild(cb.text()
                        .entityName("SimpleTitle")
                        .textWithAutoSize("GraphCompose Speed Check")
                        .textStyle(TITLE_STYLE)
                        .anchor(Anchor.topLeft())
                        .build())
                .addChild(cb.blockText(Align.left(2), BODY_STYLE)
                        .entityName("SimpleBody")
                        .size(width, 2)
                        .anchor(Anchor.topLeft())
                        .padding(Padding.of(4))
                        .text(List.of(
                                        "This scenario compares the smallest realistic document flow: heading text, a wrapped paragraph, and a divider."),
                                BODY_STYLE,
                                Padding.zero(),
                                Margin.zero())
                        .build())
                .addChild(cb.divider()
                        .entityName("SimpleDivider")
                        .width(width)
                        .thickness(1)
                        .color(ComponentColor.LIGHT_GRAY)
                        .build())
                .build();
    }

    private void composeV2SimpleDocument(DocumentSession session) {
        double width = session.canvas().innerWidth();
        session.add(new ContainerNode(
                "SimpleRoot",
                List.of(
                        new ParagraphNode(
                                "SimpleTitle",
                                "GraphCompose Speed Check",
                                TITLE_STYLE,
                                TextAlign.LEFT,
                                0.0,
                                Padding.zero(),
                                Margin.zero()),
                        new ParagraphNode(
                                "SimpleBody",
                                "This scenario compares the smallest realistic document flow: heading text, a wrapped paragraph, and a divider.",
                                BODY_STYLE,
                                TextAlign.LEFT,
                                2.0,
                                Padding.of(4),
                                Margin.zero()),
                        new ShapeNode(
                                "SimpleDivider",
                                width,
                                1,
                                ComponentColor.LIGHT_GRAY,
                                null,
                                Padding.zero(),
                                Margin.zero())),
                8,
                Padding.zero(),
                Margin.zero(),
                null,
                null));
    }

    private void composeLegacyParagraphDocument(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();

        cb.blockText(Align.left(2), BODY_STYLE)
                .entityName("LongParagraph")
                .size(width, 2)
                .anchor(Anchor.topLeft())
                .padding(Padding.of(4))
                .text(List.of(LONG_PARAGRAPH), BODY_STYLE, Padding.zero(), Margin.zero())
                .build();
    }

    private void composeV2ParagraphDocument(DocumentSession session) {
        session.add(new ParagraphNode(
                "LongParagraph",
                LONG_PARAGRAPH,
                BODY_STYLE,
                TextAlign.LEFT,
                2.0,
                Padding.of(4),
                Margin.zero()));
    }

    private void composeLegacyTableDocument(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();
        var table = cb.table()
                .entityName("BenchmarkTable")
                .columns(TableColumnSpec.fixed(52), TableColumnSpec.auto())
                .width(width)
                .defaultCellStyle(TABLE_STYLE);
        for (List<TableCellSpec> row : tableRows()) {
            table.row(row.toArray(TableCellSpec[]::new));
        }
        var tableEntity = table.build();

        cb.vContainer(Align.middle(0))
                .entityName("BenchmarkTableRoot")
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .addChild(tableEntity)
                .build();
    }

    private void composeV2TableDocument(DocumentSession session) {
        session.add(new TableNode(
                "BenchmarkTable",
                List.of(TableColumnSpec.fixed(52), TableColumnSpec.auto()),
                tableRows(),
                TABLE_STYLE,
                session.canvas().innerWidth(),
                Padding.zero(),
                Margin.zero()));
    }

    private List<ParagraphWrappedLine> legacyParagraphLines(PdfComposer composer, String entityName) throws Exception {
        EntityManager entityManager = entityManager(composer);
        return entityManager.getEntities().values().stream()
                .filter(entity -> entityName(entity).filter(entityName::equals).isPresent())
                .map(entity -> entity.getComponent(BlockTextData.class))
                .flatMap(Optional::stream)
                .flatMap(data -> data.lines().stream())
                .sorted(Comparator.comparingInt(LineTextData::page)
                        .thenComparing((LineTextData line) -> -line.y())
                        .thenComparingDouble(LineTextData::x))
                .map(line -> new ParagraphWrappedLine(
                        normalizeLineText(line.bodies().stream().map(TextDataBody::text).collect(Collectors.joining())),
                        line.page(),
                        line.lineWidth()))
                .toList();
    }

    private List<ParagraphWrappedLine> v2ParagraphLines(DocumentSession session) {
        LayoutGraph graph = session.layoutGraph();
        List<PlacedFragment> fragments = graph.fragments().stream()
                .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload)
                .sorted(Comparator.comparingInt(PlacedFragment::pageIndex)
                        .thenComparing((PlacedFragment fragment) -> -fragment.y()))
                .toList();

        List<ParagraphWrappedLine> lines = new ArrayList<>();
        for (PlacedFragment fragment : fragments) {
            BuiltInNodeDefinitions.ParagraphFragmentPayload payload =
                    (BuiltInNodeDefinitions.ParagraphFragmentPayload) fragment.payload();
            for (BuiltInNodeDefinitions.ParagraphLine line : payload.lines()) {
                lines.add(new ParagraphWrappedLine(normalizeLineText(line.text()), fragment.pageIndex(), line.width()));
            }
        }
        return List.copyOf(lines);
    }

    private List<TableRowSummary> legacyTableRows(PdfComposer composer) throws Exception {
        EntityManager entityManager = entityManager(composer);
        return entityManager.getEntities().values().stream()
                .filter(entity -> entity.getComponent(TableRowData.class).isPresent())
                .sorted(Comparator.comparingInt(entity -> rowIndex(entityName(entity).orElseThrow())))
                .map(entity -> {
                    TableRowData rowData = entity.getComponent(TableRowData.class).orElseThrow();
                    Placement placement = entity.getComponent(Placement.class).orElseThrow();
                    return new TableRowSummary(
                            entityName(entity).orElseThrow(),
                            placement.startPage(),
                            rowData.cells().stream().map(this::cellSummary).toList());
                })
                .toList();
    }

    private List<TableRowSummary> v2TableRows(DocumentSession session) {
        LayoutGraph graph = session.layoutGraph();
        return graph.fragments().stream()
                .filter(fragment -> fragment.payload() instanceof BuiltInNodeDefinitions.TableRowFragmentPayload)
                .map(fragment -> {
                    BuiltInNodeDefinitions.TableRowFragmentPayload payload =
                            (BuiltInNodeDefinitions.TableRowFragmentPayload) fragment.payload();
                    String rowName = rowNameFromCell(payload.cells().getFirst().name());
                    return new TableRowSummary(
                            rowName,
                            fragment.pageIndex(),
                            payload.cells().stream().map(this::cellSummary).toList());
                })
                .sorted(Comparator.comparingInt(row -> rowIndex(row.rowName())))
                .toList();
    }

    private TableCellSummary cellSummary(TableResolvedCell cell) {
        return new TableCellSummary(
                cell.name(),
                cell.x(),
                cell.width(),
                cell.height(),
                cell.lines(),
                cell.borderSides());
    }

    private void assertNodeEquivalent(LayoutSnapshot legacy, LayoutSnapshot v2, String entityName) {
        LayoutNodeSnapshot legacyNode = nodeByName(legacy, entityName);
        LayoutNodeSnapshot v2Node = nodeByName(v2, entityName);

        assertNormalizedPageSpan(legacyNode, v2Node);
        assertThat(v2Node.computedX()).isCloseTo(legacyNode.computedX(), within(GEOMETRY_TOLERANCE));
        assertThat(v2Node.computedY()).isCloseTo(legacyNode.computedY(), within(GEOMETRY_TOLERANCE));
        assertThat(v2Node.placementX()).isCloseTo(legacyNode.placementX(), within(GEOMETRY_TOLERANCE));
        assertThat(v2Node.placementY()).isCloseTo(legacyNode.placementY(), within(GEOMETRY_TOLERANCE));
        assertThat(v2Node.placementHeight()).isCloseTo(legacyNode.placementHeight(), within(GEOMETRY_TOLERANCE));
        assertThat(v2Node.contentHeight()).isCloseTo(legacyNode.contentHeight(), within(GEOMETRY_TOLERANCE));
        assertThat(v2Node.margin()).isEqualTo(legacyNode.margin());
        assertThat(v2Node.padding()).isEqualTo(legacyNode.padding());
    }

    private LayoutNodeSnapshot nodeByName(LayoutSnapshot snapshot, String entityName) {
        return snapshot.nodes().stream()
                .filter(node -> entityName.equals(node.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing snapshot node " + entityName));
    }

    private void assertNormalizedPageSpan(LayoutSnapshot legacy, LayoutSnapshot v2, String entityName) {
        assertNormalizedPageSpan(nodeByName(legacy, entityName), nodeByName(v2, entityName));
    }

    private void assertNormalizedPageSpan(LayoutNodeSnapshot legacyNode, LayoutNodeSnapshot v2Node) {
        assertThat(Math.min(v2Node.startPage(), v2Node.endPage()))
                .isEqualTo(Math.min(legacyNode.startPage(), legacyNode.endPage()));
        assertThat(Math.max(v2Node.startPage(), v2Node.endPage()))
                .isEqualTo(Math.max(legacyNode.startPage(), legacyNode.endPage()));
    }

    private void assertPageCountsMatch(LayoutSnapshot snapshot, byte[] pdfBytes) throws Exception {
        assertThat(pdfPageCount(pdfBytes)).isEqualTo(snapshot.totalPages());
    }

    private int pdfPageCount(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        }
    }

    private String normalizedPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return normalizeWhitespace(stripper.getText(document));
        }
    }

    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private String normalizeLineText(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private Map<Integer, Long> pageLineCounts(List<ParagraphWrappedLine> lines) {
        return lines.stream()
                .collect(Collectors.groupingBy(
                        ParagraphWrappedLine::page,
                        TreeMap::new,
                        Collectors.counting()));
    }

    private Optional<String> entityName(Entity entity) {
        return entity.getComponent(EntityName.class).map(EntityName::value);
    }

    private EntityManager entityManager(PdfComposer composer) throws Exception {
        Field entityManagerField = AbstractDocumentComposer.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        return (EntityManager) entityManagerField.get(composer);
    }

    private int rowIndex(String rowName) {
        int marker = rowName.lastIndexOf("__row_");
        return Integer.parseInt(rowName.substring(marker + "__row_".length()));
    }

    private String rowNameFromCell(String cellName) {
        int marker = cellName.lastIndexOf("__cell_");
        return marker >= 0 ? cellName.substring(0, marker) : cellName;
    }

    private List<List<TableCellSpec>> tableRows() {
        List<List<TableCellSpec>> rows = new ArrayList<>();
        rows.add(List.of(
                TableCellSpec.text("ID"),
                TableCellSpec.text("Summary")));
        for (int index = 1; index <= 20; index++) {
            TableCellSpec summary = index % 3 == 0
                    ? TableCellSpec.lines("Benchmark item " + index, "Second line")
                    : TableCellSpec.text("Benchmark item " + index);
            rows.add(List.of(
                    TableCellSpec.text("R" + index),
                    summary));
        }
        return List.copyOf(rows);
    }

    private record SnapshotRender(LayoutSnapshot snapshot, byte[] pdfBytes) {
    }

    private record ParagraphOutcome(LayoutSnapshot snapshot, byte[] pdfBytes, List<ParagraphWrappedLine> lines) {
    }

    private record ParagraphWrappedLine(String text, int page, double width) {
    }

    private record TableOutcome(LayoutSnapshot snapshot, byte[] pdfBytes, List<TableRowSummary> rows) {
    }

    private record TableRowSummary(String rowName, int page, List<TableCellSummary> cells) {
    }

    private record TableCellSummary(
            String name,
            double x,
            double width,
            double height,
            List<String> lines,
            Set<Side> borderSides) {
        private TableCellSummary {
            lines = List.copyOf(lines);
            borderSides = Set.copyOf(borderSides);
        }
    }
}
