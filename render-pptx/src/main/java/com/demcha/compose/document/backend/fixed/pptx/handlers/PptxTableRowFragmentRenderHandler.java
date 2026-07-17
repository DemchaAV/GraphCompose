package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.text.TextControlSanitizer;
import com.demcha.compose.font.FontLibrary;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Renders atomic table-row fragments as positioned rectangles, edge lines,
 * and text frames — never native PowerPoint tables, which re-lay-out their
 * content and cannot hold the engine's exact row geometry.
 *
 * <p>The backend batches {@link #renderFills} for all contiguous row
 * fragments of a table before any {@link #renderBordersAndText} call, exactly
 * like the PDF backend, so fills land beneath every border stroke. Border
 * lines reuse the PDF handler's half-stroke-width extension on internal
 * horizontal joins — without it, adjacent cells' perpendicular borders leave
 * a corner notch after rasterization; outer table edges stay clamped to the
 * cell box. Spanning cells arrive with a negative {@code yOffset} that grows
 * the rectangle downward through the merged rows.</p>
 *
 * @since 2.1.0
 */
public final class PptxTableRowFragmentRenderHandler
        implements PptxFragmentRenderHandler<TableRowFragmentPayload> {

    private static final double EPS = 1e-6;

    /**
     * Creates the table-row fragment renderer.
     */
    public PptxTableRowFragmentRenderHandler() {
    }

    @Override
    public Class<TableRowFragmentPayload> payloadType() {
        return TableRowFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       TableRowFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        renderFills(fragment, payload, environment);
        renderBordersAndText(fragment, payload, environment);
    }

    /**
     * Paints the cell backgrounds of one row fragment. The backend calls this
     * for every row of a contiguous table group before any borders or text.
     *
     * @param fragment    placed row fragment to render
     * @param payload     table-row payload carrying the resolved cells
     * @param environment active PPTX render environment
     */
    public void renderFills(PlacedFragment fragment,
                            TableRowFragmentPayload payload,
                            PptxRenderEnvironment environment) {
        XSLFSlide slide = environment.slide(fragment.pageIndex());
        for (TableResolvedCell cell : payload.cells()) {
            Color fill = cell.style().fillColor();
            if (fill == null || cell.width() <= 0 || cell.height() <= 0) {
                continue;
            }
            double cellX = fragment.x() + cell.x();
            double cellY = fragment.y() + cell.yOffset();
            XSLFAutoShape rectangle = slide.createAutoShape();
            rectangle.setShapeType(ShapeType.RECT);
            rectangle.setAnchor(new Rectangle2D.Double(
                    cellX,
                    PptxCoordinates.topY(environment.canvasHeight(), cellY, cell.height()),
                    cell.width(),
                    cell.height()));
            rectangle.setFillColor(fill);
            rectangle.setLineColor(null);
            PptxTextFrames.setShapeName(rectangle, "GraphCompose Table Cell Fill");
        }
    }

    /**
     * Paints the cell borders and text of one row fragment, after every fill
     * of the table group has landed.
     *
     * @param fragment    placed row fragment to render
     * @param payload     table-row payload carrying the resolved cells
     * @param environment active PPTX render environment
     */
    public void renderBordersAndText(PlacedFragment fragment,
                                     TableRowFragmentPayload payload,
                                     PptxRenderEnvironment environment) {
        XSLFSlide slide = environment.slide(fragment.pageIndex());
        FontLibrary fonts = environment.fonts();
        for (TableResolvedCell cell : payload.cells()) {
            double cellX = fragment.x() + cell.x();
            // yOffset is 0 for single-row cells and negative for spanning
            // cells, shifting the bottom edge downward through the merged
            // rows — same semantics as the PDF handler.
            double cellY = fragment.y() + cell.yOffset();
            renderCellBorders(slide, cell, cellX, cellY,
                    fragment.width(), payload.startsPageFragment(), environment);
            renderCellText(slide, fonts, cell, cellX, cellY, environment);
        }
    }

    private static void renderCellBorders(XSLFSlide slide,
                                          TableResolvedCell cell,
                                          double cellX,
                                          double cellY,
                                          double rowWidth,
                                          boolean startsPageFragment,
                                          PptxRenderEnvironment environment) {
        if (cell.style().stroke() == null || cell.style().stroke().width() <= 0) {
            return;
        }
        Set<Side> sides = effectiveBorderSides(cell, startsPageFragment);
        if (sides.isEmpty()) {
            return;
        }
        double strokeWidth = cell.style().stroke().width();
        Color strokeColor = cell.style().stroke().strokeColor().color();
        double canvasHeight = environment.canvasHeight();
        // Butt-capped edge lines meet exactly at cell corners, leaving a
        // notch after rasterization; horizontal lines extend half a stroke
        // width past internal joins to cover the corner pixel, while outer
        // table edges stay clamped to the real cell box.
        double cap = strokeWidth / 2.0;
        double leftCap = cell.x() <= EPS ? 0.0 : cap;
        double rightCap = Math.abs((cell.x() + cell.width()) - rowWidth) <= EPS ? 0.0 : cap;
        double topY = PptxCoordinates.flipPoint(canvasHeight, cellY + cell.height());
        double bottomY = PptxCoordinates.flipPoint(canvasHeight, cellY);
        if (sides.contains(Side.TOP)) {
            edgeLine(slide, strokeColor, strokeWidth,
                    cellX - leftCap, topY, cellX + cell.width() + rightCap, topY);
        }
        if (sides.contains(Side.BOTTOM)) {
            edgeLine(slide, strokeColor, strokeWidth,
                    cellX - leftCap, bottomY, cellX + cell.width() + rightCap, bottomY);
        }
        if (sides.contains(Side.LEFT)) {
            edgeLine(slide, strokeColor, strokeWidth, cellX, topY, cellX, bottomY);
        }
        if (sides.contains(Side.RIGHT)) {
            edgeLine(slide, strokeColor, strokeWidth,
                    cellX + cell.width(), topY, cellX + cell.width(), bottomY);
        }
    }

    private static void edgeLine(XSLFSlide slide,
                                 Color color,
                                 double width,
                                 double x1, double y1, double x2, double y2) {
        XSLFConnectorShape line = slide.createConnector();
        line.setShapeType(ShapeType.LINE);
        line.setAnchor(new Rectangle2D.Double(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.abs(x2 - x1),
                Math.abs(y2 - y1)));
        line.setLineColor(color);
        line.setLineWidth(width);
        PptxTextFrames.setShapeName(line, "GraphCompose Table Cell Border");
    }

    private static void renderCellText(XSLFSlide slide,
                                       FontLibrary fonts,
                                       TableResolvedCell cell,
                                       double cellX,
                                       double cellY,
                                       PptxRenderEnvironment environment) {
        PdfFont font = fonts.getFont(cell.style().textStyle().fontName(), PdfFont.class).orElseThrow();
        double lineHeight = font.getLineHeight(cell.style().textStyle());
        Padding padding = cell.style().padding() == null ? Padding.zero() : cell.style().padding();
        Anchor anchor = cell.style().textAnchor() == null ? Anchor.centerLeft() : cell.style().textAnchor();

        double innerX = cellX + padding.left();
        double innerY = cellY + padding.bottom();
        double innerWidth = Math.max(0, cell.width() - padding.horizontal());
        double innerHeight = Math.max(0, cell.height() - padding.vertical());
        List<String> safeLines = sanitizeLines(cell.lines());
        double blockHeight = lineHeight * Math.max(1, safeLines.size());

        double blockY = switch (anchor.v()) {
            case TOP -> innerY + innerHeight - blockHeight;
            case MIDDLE -> innerY + (innerHeight - blockHeight) / 2.0;
            case BOTTOM, DEFAULT -> innerY;
        };

        PdfFont.VerticalMetrics metrics = font.verticalMetrics(cell.style().textStyle());
        double viewerAscent = environment.viewerAscent(cell.style().textStyle(), metrics.ascent());
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
            String safeText = font.sanitizeForRender(cell.style().textStyle(), line);
            PptxTextFrames.singleRunBox(slide,
                    "GraphCompose Table Cell Text",
                    new Rectangle2D.Double(
                            lineX,
                            environment.canvasHeight() - baselineY - viewerAscent,
                            Math.max(PptxTextFrames.FRAME_EPSILON,
                                    lineWidth + PptxTextFrames.FRAME_EPSILON),
                            Math.max(PptxTextFrames.FRAME_EPSILON, metrics.lineHeight())),
                    safeText,
                    cell.style().textStyle(),
                    environment);
        }
    }

    private static Set<Side> effectiveBorderSides(TableResolvedCell cell, boolean startsPageFragment) {
        EnumSet<Side> sides = cell.borderSides().isEmpty()
                ? EnumSet.noneOf(Side.class)
                : EnumSet.copyOf(cell.borderSides());
        if (startsPageFragment) {
            sides.add(Side.TOP);
        }
        return sides;
    }

    private static List<String> sanitizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of("");
        }
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(TextControlSanitizer.replace(line, " ").trim());
        }
        return List.copyOf(result);
    }
}
