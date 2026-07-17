package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.text.TextControlSanitizer;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Geometry identity for table rows: cell fills, border edge lines (including
 * the half-stroke internal join extension and the page-start top border), and
 * cell text frames re-derived from the captured {@link LayoutGraph} against
 * the reopened .pptx — across a page break with a repeated header and a
 * row-spanning cell, with every fill of the table landing before any border
 * or text shape.
 */
class PptxTableGeometryTest {

    private static final double EPS_PT = 0.5;
    private static final double JOIN_EPS = 1e-6;

    private static DocumentSession composeTable() {
        DocumentSession session = GraphCompose.document()
                .pageSize(420, 240)
                .margin(DocumentInsets.of(20))
                .create();
        DocumentTableStyle plain = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(DocumentColor.GRAY, 1))
                .padding(3)
                .build();
        session.pageFlow(page -> page.module("data", module -> module.table(table -> {
            table.autoColumns(3)
                    .headerRow("Alpha", "Beta", "Gamma")
                    .repeatHeader()
                    .headerStyle(DocumentTableStyle.builder()
                            .fillColor(DocumentColor.ROYAL_BLUE)
                            .textStyle(DocumentTextStyle.builder()
                                    .color(DocumentColor.WHITE).size(10).build())
                            .stroke(DocumentStroke.of(DocumentColor.DARK_GRAY, 1))
                            .padding(3)
                            .build())
                    .defaultCellStyle(plain)
                    .zebra(DocumentColor.WHITE, DocumentColor.LIGHT_GRAY);
            table.rowCells(
                    new DocumentTableCell(List.of("merged"), plain).rowSpan(2),
                    new DocumentTableCell(List.of("b1"), plain),
                    new DocumentTableCell(List.of("c1"), plain));
            table.rowCells(
                    new DocumentTableCell(List.of("b2"), plain),
                    new DocumentTableCell(List.of("c2"), plain));
            for (int row = 1; row <= 16; row++) {
                table.row("row " + row, "value " + row, "note " + row);
            }
        })));
        return session;
    }

    @Test
    void tableRowsRenderFillsBordersAndTextAtTheGraphCoordinates() throws Exception {
        try (DocumentSession session = composeTable()) {
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            assertThat(graph.totalPages()).as("the table must cross a page break").isGreaterThan(1);
            List<TableRowFragmentPayload> rowPayloads = graph.fragments().stream()
                    .map(PlacedFragment::payload)
                    .filter(TableRowFragmentPayload.class::isInstance)
                    .map(TableRowFragmentPayload.class::cast)
                    .toList();
            assertThat(rowPayloads)
                    .as("the row-spanning cell must resolve to a negative yOffset")
                    .anyMatch(payload -> payload.cells().stream().anyMatch(cell -> cell.yOffset() < 0));
            assertThat(rowPayloads)
                    .as("a page-start row fragment must exist")
                    .anyMatch(TableRowFragmentPayload::startsPageFragment);

            List<ExpectedRect> expectedFills = new ArrayList<>();
            List<ExpectedRect> expectedBorders = new ArrayList<>();
            List<ExpectedText> expectedTexts = new ArrayList<>();
            double canvasHeight = graph.canvas().height();
            try (PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
                for (PlacedFragment fragment : graph.fragments()) {
                    if (!(fragment.payload() instanceof TableRowFragmentPayload payload)) {
                        continue;
                    }
                    for (TableResolvedCell cell : payload.cells()) {
                        deriveCellExpectations(fragment, payload, cell, canvasHeight,
                                measurement, expectedFills, expectedBorders, expectedTexts);
                    }
                }
            }
            assertThat(expectedFills).isNotEmpty();
            assertThat(expectedBorders).isNotEmpty();
            assertThat(expectedTexts).isNotEmpty();

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSize(graph.totalPages());
                List<ActualShape> fills = collect(show, "GraphCompose Table Cell Fill");
                List<ActualShape> borders = collect(show, "GraphCompose Table Cell Border");
                List<ActualShape> texts = collect(show, "GraphCompose Table Cell Text");

                assertRects(fills, expectedFills, "fill");
                assertRects(borders, expectedBorders, "border");

                assertThat(texts).hasSameSizeAs(expectedTexts);
                for (int i = 0; i < expectedTexts.size(); i++) {
                    ExpectedText want = expectedTexts.get(i);
                    ActualShape got = texts.get(i);
                    assertThat(got.pageIndex).as("text %d page", i).isEqualTo(want.pageIndex);
                    assertThat(((XSLFTextBox) got.shape).getText())
                            .as("text %d content", i).isEqualTo(want.text);
                    assertThat(got.shape.getAnchor().getX())
                            .as("text %d x", i)
                            .isCloseTo(want.x, org.assertj.core.data.Offset.offset(EPS_PT));
                    assertThat(got.shape.getAnchor().getY())
                            .as("text %d top", i)
                            .isCloseTo(want.top, org.assertj.core.data.Offset.offset(EPS_PT));
                    assertThat(got.shape.getAnchor().getWidth())
                            .as("text %d width", i).isPositive();
                    assertThat(got.shape.getAnchor().getHeight())
                            .as("text %d height", i).isPositive();
                }

                for (int page = 0; page < show.getSlides().size(); page++) {
                    int lastFill = -1;
                    int firstInk = Integer.MAX_VALUE;
                    List<XSLFShape> shapes = show.getSlides().get(page).getShapes();
                    for (int i = 0; i < shapes.size(); i++) {
                        String name = shapes.get(i).getShapeName();
                        if ("GraphCompose Table Cell Fill".equals(name)) {
                            lastFill = Math.max(lastFill, i);
                        } else if ("GraphCompose Table Cell Border".equals(name)
                                || "GraphCompose Table Cell Text".equals(name)) {
                            firstInk = Math.min(firstInk, i);
                        }
                    }
                    if (lastFill >= 0 && firstInk < Integer.MAX_VALUE) {
                        assertThat(lastFill)
                                .as("slide %d: every table fill must land before borders and text", page)
                                .isLessThan(firstInk);
                    }
                }
            }
        }
    }

    private static void deriveCellExpectations(PlacedFragment fragment,
                                               TableRowFragmentPayload payload,
                                               TableResolvedCell cell,
                                               double canvasHeight,
                                               PdfMeasurementResources measurement,
                                               List<ExpectedRect> fills,
                                               List<ExpectedRect> borders,
                                               List<ExpectedText> texts) {
        double cellX = fragment.x() + cell.x();
        double cellY = fragment.y() + cell.yOffset();
        if (cell.style().fillColor() != null && cell.width() > 0 && cell.height() > 0) {
            fills.add(new ExpectedRect(fragment.pageIndex(), new Rectangle2D.Double(
                    cellX, canvasHeight - cellY - cell.height(), cell.width(), cell.height())));
        }

        if (cell.style().stroke() != null && cell.style().stroke().width() > 0) {
            EnumSet<Side> sides = cell.borderSides().isEmpty()
                    ? EnumSet.noneOf(Side.class)
                    : EnumSet.copyOf(cell.borderSides());
            if (payload.startsPageFragment()) {
                sides.add(Side.TOP);
            }
            double cap = cell.style().stroke().width() / 2.0;
            double leftCap = cell.x() <= JOIN_EPS ? 0.0 : cap;
            double rightCap =
                    Math.abs((cell.x() + cell.width()) - fragment.width()) <= JOIN_EPS ? 0.0 : cap;
            double topY = canvasHeight - (cellY + cell.height());
            double bottomY = canvasHeight - cellY;
            if (sides.contains(Side.TOP)) {
                borders.add(segment(fragment.pageIndex(),
                        cellX - leftCap, topY, cellX + cell.width() + rightCap, topY));
            }
            if (sides.contains(Side.BOTTOM)) {
                borders.add(segment(fragment.pageIndex(),
                        cellX - leftCap, bottomY, cellX + cell.width() + rightCap, bottomY));
            }
            if (sides.contains(Side.LEFT)) {
                borders.add(segment(fragment.pageIndex(), cellX, topY, cellX, bottomY));
            }
            if (sides.contains(Side.RIGHT)) {
                borders.add(segment(fragment.pageIndex(),
                        cellX + cell.width(), topY, cellX + cell.width(), bottomY));
            }
        }

        PdfFont font = measurement.fontLibrary()
                .getFont(cell.style().textStyle().fontName(), PdfFont.class).orElseThrow();
        double lineHeight = font.getLineHeight(cell.style().textStyle());
        var padding = cell.style().padding() == null
                ? com.demcha.compose.engine.components.style.Padding.zero()
                : cell.style().padding();
        var anchor = cell.style().textAnchor() == null
                ? com.demcha.compose.engine.components.layout.Anchor.centerLeft()
                : cell.style().textAnchor();
        double innerX = cellX + padding.left();
        double innerY = cellY + padding.bottom();
        double innerWidth = Math.max(0, cell.width() - padding.horizontal());
        double innerHeight = Math.max(0, cell.height() - padding.vertical());
        List<String> safeLines = new ArrayList<>();
        for (String line : cell.lines().isEmpty() ? List.of("") : cell.lines()) {
            safeLines.add(TextControlSanitizer.replace(line, " ").trim());
        }
        double blockHeight = lineHeight * Math.max(1, safeLines.size());
        double blockY = switch (anchor.v()) {
            case TOP -> innerY + innerHeight - blockHeight;
            case MIDDLE -> innerY + (innerHeight - blockHeight) / 2.0;
            case BOTTOM, DEFAULT -> innerY;
        };
        PdfFont.VerticalMetrics metrics = font.verticalMetrics(cell.style().textStyle());
        double viewerAscent = PptxViewerMetrics.ascentPoints(cell.style().textStyle(), font, null);
        for (int lineIndex = 0; lineIndex < safeLines.size(); lineIndex++) {
            String line = safeLines.get(lineIndex);
            if (line.isEmpty()) {
                continue;
            }
            double lineWidth = font.getTextWidth(cell.style().textStyle(), line);
            double lineX = switch (anchor.h()) {
                case RIGHT -> innerX + innerWidth - lineWidth;
                case CENTER -> innerX + (innerWidth - lineWidth) / 2.0;
                case LEFT, DEFAULT -> innerX;
            };
            double lineBoxY = blockY + lineHeight * (safeLines.size() - lineIndex - 1);
            double baselineY = lineBoxY + metrics.baselineOffsetFromBottom();
            texts.add(new ExpectedText(fragment.pageIndex(),
                    font.sanitizeForRender(cell.style().textStyle(), line),
                    lineX,
                    canvasHeight - baselineY - viewerAscent));
        }
    }

    private static ExpectedRect segment(int pageIndex, double x1, double y1, double x2, double y2) {
        return new ExpectedRect(pageIndex, new Rectangle2D.Double(
                Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)));
    }

    private static List<ActualShape> collect(XMLSlideShow show, String shapeName) {
        List<ActualShape> shapes = new ArrayList<>();
        for (int page = 0; page < show.getSlides().size(); page++) {
            for (XSLFShape shape : show.getSlides().get(page).getShapes()) {
                if (shapeName.equals(shape.getShapeName())) {
                    shapes.add(new ActualShape(page, shape));
                }
            }
        }
        return shapes;
    }

    private static void assertRects(List<ActualShape> actual,
                                    List<ExpectedRect> expected,
                                    String label) {
        assertThat(actual).as(label + " count").hasSameSizeAs(expected);
        for (int i = 0; i < expected.size(); i++) {
            ExpectedRect want = expected.get(i);
            ActualShape got = actual.get(i);
            assertThat(got.pageIndex).as(label + " %d page", i).isEqualTo(want.pageIndex);
            Rectangle2D anchor = got.shape.getAnchor();
            assertThat(anchor.getX()).as(label + " %d x", i)
                    .isCloseTo(want.rect.x, org.assertj.core.data.Offset.offset(EPS_PT));
            assertThat(anchor.getY()).as(label + " %d y", i)
                    .isCloseTo(want.rect.y, org.assertj.core.data.Offset.offset(EPS_PT));
            assertThat(anchor.getWidth()).as(label + " %d width", i)
                    .isCloseTo(want.rect.width, org.assertj.core.data.Offset.offset(EPS_PT));
            assertThat(anchor.getHeight()).as(label + " %d height", i)
                    .isCloseTo(want.rect.height, org.assertj.core.data.Offset.offset(EPS_PT));
        }
    }

    /**
     * Pins the page-start branch at the handler level, independent of what
     * border sides the layout engine happens to emit: a continuation-row cell
     * whose sides carry only BOTTOM must still receive a TOP border line when
     * its fragment starts a page.
     */
    @Test
    void pageStartFragmentForcesATopBorderLine() throws Exception {
        try (org.apache.poi.xslf.usermodel.XMLSlideShow show =
                     new org.apache.poi.xslf.usermodel.XMLSlideShow();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
            PptxRenderSession session = new PptxRenderSession(show, 300, 200, 1);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, 200, measurement.fontLibrary(), List.of());
            TableResolvedCell cell = new TableResolvedCell(
                    "cell", 0, 120, 30, 0, List.of(),
                    com.demcha.compose.engine.components.content.table.TableCellLayoutStyle.builder()
                            .stroke(new com.demcha.compose.engine.components.content.shape.Stroke(
                                    java.awt.Color.BLACK, 1.0))
                            .textStyle(com.demcha.compose.engine.components.content.text.TextStyle.DEFAULT_STYLE)
                            .build(),
                    com.demcha.compose.engine.components.style.Padding.zero(),
                    Set.of(Side.BOTTOM));
            TableRowFragmentPayload payload = new TableRowFragmentPayload(
                    List.of(cell), true, null, null);
            PlacedFragment fragment = new PlacedFragment(
                    "root/table", 0, 0, 40, 100, 120, 30, null, null, payload);

            new com.demcha.compose.document.backend.fixed.pptx.handlers
                    .PptxTableRowFragmentRenderHandler()
                    .renderBordersAndText(fragment, payload, environment);

            List<XSLFShape> borders = show.getSlides().get(0).getShapes().stream()
                    .filter(shape -> "GraphCompose Table Cell Border".equals(shape.getShapeName()))
                    .toList();
            assertThat(borders).as("BOTTOM plus the forced page-start TOP").hasSize(2);
            assertThat(borders)
                    .anyMatch(shape -> Math.abs(shape.getAnchor().getY() - (200 - 130)) < EPS_PT)
                    .as("one border must sit on the cell's top edge (y = 200 − (100+30))");
        }
    }

    private record ExpectedRect(int pageIndex, Rectangle2D.Double rect) {
    }

    private record ExpectedText(int pageIndex, String text, double x, double top) {
    }

    private record ActualShape(int pageIndex, XSLFShape shape) {
    }
}
