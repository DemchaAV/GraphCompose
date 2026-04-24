package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.components_builders.TableCellStyle;
import com.demcha.compose.engine.components.content.shape.FillColor;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.content.table.TableRowData;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.ParentComponent;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.BlockText;
import com.demcha.compose.engine.components.renderable.TableRow;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.pdf.helpers.TableCellBox;
import com.demcha.compose.engine.render.guides.GuidesRenderer;
import com.demcha.compose.engine.render.RenderHandler;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PDF renderer-side handler for atomic table rows.
 */
public final class PdfTableRowRenderHandler implements RenderHandler<TableRow, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    private final TableCellBox cellRenderer = new TableCellBox();

    @Override
    public Class<TableRow> renderType() {
        return TableRow.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          TableRow renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        Placement placement = entity.getComponent(Placement.class).orElseThrow();
        TableRowData rowData = entity.getComponent(TableRowData.class).orElseThrow();
        boolean startsNewPageFragment = startsPageFragment(manager, entity);

        PDPageContentStream stream = renderingSystem.pageSurface(entity);
        for (TableResolvedCell cell : rowData.cells()) {
            double cellX = placement.x() + cell.x();
            double cellY = placement.y();
            TableCellStyle style = cell.style();

            cellRenderer.render(
                    stream,
                    renderingSystem,
                    cellX,
                    cellY,
                    cell.width(),
                    cell.height(),
                    style.fillColor() == null ? null : new FillColor(style.fillColor()),
                    style.stroke(),
                    effectiveFillInsets(cell, startsNewPageFragment),
                    effectiveBorderSides(cell, startsNewPageFragment)
            );
            renderCellText(stream, manager, cell, cellX, cellY);
        }

        if (guideLines) {
            renderingSystem.guidesRenderer().guidesRender(entity, stream, DEFAULT_GUIDES);
        }

        return true;
    }

    boolean startsPageFragment(EntityManager manager, Entity rowEntity) {
        Placement placement = rowEntity.getComponent(Placement.class).orElseThrow();
        ParentComponent parentComponent = rowEntity.getComponent(ParentComponent.class).orElse(null);
        if (parentComponent == null) {
            return true;
        }

        Entity parent = manager.getEntity(parentComponent.uuid()).orElse(null);
        if (parent == null) {
            return true;
        }

        int currentIndex = parent.getChildren().indexOf(rowEntity.getUuid());
        if (currentIndex <= 0) {
            return true;
        }

        UUID previousRowId = parent.getChildren().get(currentIndex - 1);
        Entity previousRow = manager.getEntity(previousRowId).orElse(null);
        if (previousRow == null) {
            return true;
        }

        Placement previousPlacement = previousRow.getComponent(Placement.class).orElse(null);
        if (previousPlacement == null) {
            return true;
        }

        return previousPlacement.endPage() != placement.startPage();
    }

    Set<Side> effectiveBorderSides(TableResolvedCell cell, boolean startsNewPageFragment) {
        EnumSet<Side> sides = cell.borderSides().isEmpty()
                ? EnumSet.noneOf(Side.class)
                : EnumSet.copyOf(cell.borderSides());

        if (startsNewPageFragment) {
            sides.add(Side.TOP);
        }

        return sides;
    }

    Padding effectiveFillInsets(TableResolvedCell cell, boolean startsNewPageFragment) {
        if (!startsNewPageFragment) {
            return cell.fillInsets();
        }

        double topInset = 0.0;
        if (cell.style().stroke() != null) {
            topInset = cell.style().stroke().width() / 2.0;
        }

        Padding fillInsets = cell.fillInsets();
        return new Padding(topInset, fillInsets.right(), fillInsets.bottom(), fillInsets.left());
    }

    List<ResolvedTextLine> resolveTextLines(PdfFont font,
                                            TableResolvedCell cell,
                                            double cellX,
                                            double cellY) {
        TableCellStyle style = cell.style();
        List<String> safeLines = sanitizeLines(cell.lines());
        double lineHeight = font.getLineHeight(style.textStyle());
        double lineSpacing = style.lineSpacing() == null ? 0.0 : style.lineSpacing();
        double linePitch = lineHeight + lineSpacing;
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        Anchor anchor = style.textAnchor() == null ? Anchor.centerLeft() : style.textAnchor();

        double innerX = cellX + padding.left();
        double innerY = cellY + padding.bottom();
        double innerWidth = Math.max(0, cell.width() - padding.horizontal());
        double innerHeight = Math.max(0, cell.height() - padding.vertical());
        int lineCount = Math.max(1, safeLines.size());
        double blockHeight = (lineHeight * lineCount) + (lineSpacing * (lineCount - 1));

        double blockY = switch (anchor.v()) {
            case TOP -> innerY + innerHeight - blockHeight;
            case MIDDLE -> innerY + (innerHeight - blockHeight) / 2.0;
            case BOTTOM, DEFAULT -> innerY;
        };

        PdfFont.VerticalMetrics metrics = font.verticalMetrics(style.textStyle());
        List<ResolvedTextLine> resolved = new ArrayList<>(safeLines.size());

        for (int lineIndex = 0; lineIndex < safeLines.size(); lineIndex++) {
            String line = safeLines.get(lineIndex);
            double lineWidth = font.getTextWidth(style.textStyle(), line);
            double lineX = switch (anchor.h()) {
                case RIGHT -> innerX + innerWidth - lineWidth;
                case CENTER -> innerX + (innerWidth - lineWidth) / 2.0;
                case LEFT, DEFAULT -> innerX;
            };

            double lineBoxY = blockY + linePitch * (safeLines.size() - lineIndex - 1);
            double baselineY = lineBoxY + metrics.baselineOffsetFromBottom();
            resolved.add(new ResolvedTextLine(line, lineX, baselineY));
        }

        return List.copyOf(resolved);
    }

    private void renderCellText(PDPageContentStream stream,
                                EntityManager manager,
                                TableResolvedCell cell,
                                double cellX,
                                double cellY) throws IOException {
        TableCellStyle style = cell.style();
        PdfFont font = manager.getFonts()
                .getFont(style.textStyle().fontName(), PdfFont.class)
                .orElseThrow();

        List<ResolvedTextLine> lines = resolveTextLines(font, cell, cellX, cellY);

        stream.saveGraphicsState();
        try {
            stream.setFont(font.fontType(style.textStyle().decoration()), (float) style.textStyle().size());
            stream.setNonStrokingColor(style.textStyle().color());
            for (ResolvedTextLine line : lines) {
                if (line.text().isEmpty()) {
                    continue;
                }
                stream.beginText();
                stream.newLineAtOffset((float) line.x(), (float) line.baselineY());
                stream.showText(line.text());
                stream.endText();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private List<String> sanitizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of("");
        }

        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(BlockText.sanitizeText(line == null ? "" : line)
                    .replace('\r', ' ')
                    .replace('\n', ' '));
        }
        return List.copyOf(result);
    }

    record ResolvedTextLine(String text, double x, double baselineY) {
    }
}
