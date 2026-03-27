package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.content.shape.FillColor;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.content.table.TableResolvedCell;
import com.demcha.compose.layout_core.components.content.table.TableRowData;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.ParentComponent;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Atomic row renderable that draws all cell boxes and their single-line text on one page.
 */
public class TableRow implements PdfRender {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    private final TableCellBox cellRenderer = new TableCellBox();

    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystem, boolean guideLines) throws IOException {
        Placement placement = e.getComponent(Placement.class).orElseThrow();
        TableRowData rowData = e.getComponent(TableRowData.class).orElseThrow();
        boolean startsNewPageFragment = startsPageFragment(manager, e);

        try (PDPageContentStream stream = renderingSystem.stream().openContentStream(e)) {
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
                renderingSystem.guidesRenderer().guidesRender(e, stream, DEFAULT_GUIDES);
            }
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

    private void renderCellText(PDPageContentStream stream,
                                EntityManager manager,
                                TableResolvedCell cell,
                                double cellX,
                                double cellY) throws IOException {
        TableCellStyle style = cell.style();
        PdfFont font = manager.getFonts()
                .getFont(style.textStyle().fontName(), PdfFont.class)
                .orElseThrow();

        String safeText = BlockText.sanitizeText(cell.text())
                .replace('\r', ' ')
                .replace('\n', ' ');

        double textWidth = font.getTextWidth(style.textStyle(), safeText);
        double textHeight = font.getLineHeight(style.textStyle());
        Padding padding = style.padding() == null ? Padding.zero() : style.padding();
        Anchor anchor = style.textAnchor() == null ? Anchor.centerLeft() : style.textAnchor();

        double innerX = cellX + padding.left();
        double innerY = cellY + padding.bottom();
        double innerWidth = Math.max(0, cell.width() - padding.horizontal());
        double innerHeight = Math.max(0, cell.height() - padding.vertical());

        double textBoxX = switch (anchor.h()) {
            case RIGHT -> innerX + innerWidth - textWidth;
            case CENTER -> innerX + (innerWidth - textWidth) / 2.0;
            case LEFT, DEFAULT -> innerX;
        };
        double textBoxY = switch (anchor.v()) {
            case TOP -> innerY + innerHeight - textHeight;
            case MIDDLE -> innerY + (innerHeight - textHeight) / 2.0;
            case BOTTOM, DEFAULT -> innerY;
        };

        PdfFont.VerticalMetrics metrics = font.verticalMetrics(style.textStyle());
        double baselineY = textBoxY + metrics.baselineOffsetFromBottom();

        stream.saveGraphicsState();
        try {
            stream.setFont(font.fontType(style.textStyle().decoration()), (float) style.textStyle().size());
            stream.setNonStrokingColor(style.textStyle().color());
            stream.beginText();
            stream.newLineAtOffset((float) textBoxX, (float) baselineY);
            stream.showText(safeText);
            stream.endText();
        } finally {
            stream.restoreGraphicsState();
        }
    }
}
