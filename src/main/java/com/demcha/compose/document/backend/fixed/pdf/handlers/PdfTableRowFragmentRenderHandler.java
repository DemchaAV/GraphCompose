package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.text.TextControlSanitizer;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Renders atomic table-row fragments emitted by the semantic table node.
 */
public final class PdfTableRowFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.TableRowFragmentPayload> {

    @Override
    public Class<BuiltInNodeDefinitions.TableRowFragmentPayload> payloadType() {
        return BuiltInNodeDefinitions.TableRowFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.TableRowFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        FontLibrary fonts = environment.fonts();

        for (TableResolvedCell cell : payload.cells()) {
            double cellX = fragment.x() + cell.x();
            double cellY = fragment.y();
            renderCellBox(stream, cell, cellX, cellY, payload.startsPageFragment());
            renderCellText(stream, fonts, cell, cellX, cellY);
        }
    }

    private void renderCellBox(PDPageContentStream stream,
                               TableResolvedCell cell,
                               double cellX,
                               double cellY,
                               boolean startsPageFragment) throws IOException {
        Padding fillInsets = effectiveFillInsets(cell, startsPageFragment);
        if (cell.style().fillColor() != null) {
            double fillX = cellX + fillInsets.left();
            double fillY = cellY + fillInsets.bottom();
            double fillWidth = Math.max(0.0, cell.width() - fillInsets.horizontal());
            double fillHeight = Math.max(0.0, cell.height() - fillInsets.vertical());
            if (fillWidth > 0 && fillHeight > 0) {
                stream.saveGraphicsState();
                try {
                    stream.setNonStrokingColor(cell.style().fillColor());
                    stream.addRect((float) fillX, (float) fillY, (float) fillWidth, (float) fillHeight);
                    stream.fill();
                } finally {
                    stream.restoreGraphicsState();
                }
            }
        }

        if (cell.style().stroke() == null || cell.style().stroke().width() <= 0) {
            return;
        }

        Set<Side> sides = effectiveBorderSides(cell, startsPageFragment);
        if (sides.isEmpty()) {
            return;
        }

        stream.saveGraphicsState();
        try {
            stream.setStrokingColor(cell.style().stroke().strokeColor().color());
            stream.setLineWidth((float) cell.style().stroke().width());
            if (sides.contains(Side.TOP)) {
                line(stream, cellX, cellY + cell.height(), cellX + cell.width(), cellY + cell.height());
            }
            if (sides.contains(Side.BOTTOM)) {
                line(stream, cellX, cellY, cellX + cell.width(), cellY);
            }
            if (sides.contains(Side.LEFT)) {
                line(stream, cellX, cellY, cellX, cellY + cell.height());
            }
            if (sides.contains(Side.RIGHT)) {
                line(stream, cellX + cell.width(), cellY, cellX + cell.width(), cellY + cell.height());
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private void renderCellText(PDPageContentStream stream,
                                FontLibrary fonts,
                                TableResolvedCell cell,
                                double cellX,
                                double cellY) throws IOException {
        PdfFont font = fonts.getFont(cell.style().textStyle().fontName(), PdfFont.class).orElseThrow();
        List<ResolvedTextLine> lines = resolveTextLines(font, cell, cellX, cellY);

        stream.saveGraphicsState();
        try {
            stream.setFont(font.fontType(cell.style().textStyle().decoration()), (float) cell.style().textStyle().size());
            stream.setNonStrokingColor(cell.style().textStyle().color());
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

    private List<ResolvedTextLine> resolveTextLines(PdfFont font,
                                                    TableResolvedCell cell,
                                                    double cellX,
                                                    double cellY) {
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
        List<ResolvedTextLine> resolved = new ArrayList<>(safeLines.size());

        for (int lineIndex = 0; lineIndex < safeLines.size(); lineIndex++) {
            String line = safeLines.get(lineIndex);
            double lineWidth = font.getTextWidth(cell.style().textStyle(), line);
            double lineX = switch (anchor.h()) {
                case RIGHT -> innerX + innerWidth - lineWidth;
                case CENTER -> innerX + (innerWidth - lineWidth) / 2.0;
                case LEFT, DEFAULT -> innerX;
            };
            double lineBoxY = blockY + lineHeight * (safeLines.size() - lineIndex - 1);
            double baselineY = lineBoxY + metrics.baselineOffsetFromBottom();
            resolved.add(new ResolvedTextLine(line, lineX, baselineY));
        }

        return List.copyOf(resolved);
    }

    private Set<Side> effectiveBorderSides(TableResolvedCell cell, boolean startsPageFragment) {
        EnumSet<Side> sides = cell.borderSides().isEmpty()
                ? EnumSet.noneOf(Side.class)
                : EnumSet.copyOf(cell.borderSides());
        if (startsPageFragment) {
            sides.add(Side.TOP);
        }
        return sides;
    }

    private Padding effectiveFillInsets(TableResolvedCell cell, boolean startsPageFragment) {
        if (!startsPageFragment) {
            return cell.fillInsets();
        }
        double topInset = cell.style().stroke() == null ? 0.0 : cell.style().stroke().width() / 2.0;
        return new Padding(topInset, cell.fillInsets().right(), cell.fillInsets().bottom(), cell.fillInsets().left());
    }

    private void line(PDPageContentStream stream, double x1, double y1, double x2, double y2) throws IOException {
        stream.moveTo((float) x1, (float) y1);
        stream.lineTo((float) x2, (float) y2);
        stream.stroke();
    }

    private List<String> sanitizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of("");
        }

        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(TextControlSanitizer.replace(line, " ").trim());
        }
        return List.copyOf(result);
    }

    private record ResolvedTextLine(String text, double x, double baselineY) {
    }
}
