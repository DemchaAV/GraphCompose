package com.demcha.compose.v2.backends;

import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.components.content.shape.Side;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.v2.BuiltInNodeDefinitions;
import com.demcha.compose.v2.FixedLayoutBackend;
import com.demcha.compose.v2.FixedLayoutRenderContext;
import com.demcha.compose.v2.LayoutGraph;
import com.demcha.compose.v2.PlacedFragment;
import com.demcha.compose.v2.exceptions.UnsupportedNodeCapabilityException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * First fixed-layout PDF backend for the v2 fragment graph.
 */
public final class PdfFixedLayoutBackend implements FixedLayoutBackend<byte[]> {
    @Override
    public String name() {
        return "pdf-fixed-layout";
    }

    @Override
    public byte[] render(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        try (PDDocument document = new PDDocument()) {
            FontLibrary fonts = DefaultFonts.library(document, context.customFontFamilies());
            RenderState renderState = new RenderState(document, fonts);

            List<PDPage> pages = createPages(document, graph);
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                try (PDPageContentStream stream = new PDPageContentStream(document, pages.get(pageIndex))) {
                    for (PlacedFragment fragment : fragmentsForPage(graph.fragments(), pageIndex)) {
                        renderFragment(stream, fragment, renderState);
                    }
                }
            }

            if (context.outputFile() != null) {
                document.save(context.outputFile().toFile());
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        }
    }

    private List<PDPage> createPages(PDDocument document, LayoutGraph graph) {
        int pageCount = Math.max(graph.totalPages(), 1);
        PDRectangle pageSize = new PDRectangle((float) graph.canvas().width(), (float) graph.canvas().height());
        List<PDPage> pages = new ArrayList<>(pageCount);
        for (int index = 0; index < pageCount; index++) {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            pages.add(page);
        }
        return List.copyOf(pages);
    }

    private List<PlacedFragment> fragmentsForPage(List<PlacedFragment> fragments, int pageIndex) {
        return fragments.stream()
                .filter(fragment -> fragment.pageIndex() == pageIndex)
                .toList();
    }

    private void renderFragment(PDPageContentStream stream, PlacedFragment fragment, RenderState renderState) throws IOException {
        Object payload = fragment.payload();
        if (payload instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload paragraph) {
            renderParagraph(stream, fragment, paragraph, renderState.fonts());
            return;
        }
        if (payload instanceof BuiltInNodeDefinitions.ShapeFragmentPayload shape) {
            renderShape(stream, fragment, shape);
            return;
        }
        if (payload instanceof BuiltInNodeDefinitions.ImageFragmentPayload image) {
            renderImage(stream, fragment, image, renderState);
            return;
        }
        if (payload instanceof BuiltInNodeDefinitions.TableRowFragmentPayload row) {
            renderTableRow(stream, fragment, row, renderState.fonts());
            return;
        }
        throw new UnsupportedNodeCapabilityException("PDF backend does not support fragment payload: " + payload);
    }

    private void renderParagraph(PDPageContentStream stream,
                                 PlacedFragment fragment,
                                 BuiltInNodeDefinitions.ParagraphFragmentPayload payload,
                                 FontLibrary fonts) throws IOException {
        PdfFont font = fonts.getFont(payload.textStyle().fontName(), PdfFont.class).orElseThrow();
        double innerX = fragment.x() + payload.padding().left();
        double innerWidth = Math.max(0.0, fragment.width() - payload.padding().horizontal());
        double contentTop = fragment.y() + fragment.height() - payload.padding().top();

        stream.saveGraphicsState();
        try {
            stream.setFont(font.fontType(payload.textStyle().decoration()), (float) payload.textStyle().size());
            stream.setNonStrokingColor(payload.textStyle().color());

            for (int lineIndex = 0; lineIndex < payload.lines().size(); lineIndex++) {
                BuiltInNodeDefinitions.ParagraphLine line = payload.lines().get(lineIndex);
                String text = sanitize(line.text());
                double lineTop = contentTop - lineIndex * (payload.lineHeight() + payload.lineGap());
                double baselineY = lineTop - payload.lineHeight() + payload.baselineOffset();
                double lineX = switch (payload.align()) {
                    case RIGHT -> innerX + innerWidth - line.width();
                    case CENTER -> innerX + (innerWidth - line.width()) / 2.0;
                    case LEFT -> innerX;
                };

                if (text.isEmpty()) {
                    continue;
                }
                stream.beginText();
                stream.newLineAtOffset((float) lineX, (float) baselineY);
                stream.showText(text);
                stream.endText();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private void renderShape(PDPageContentStream stream,
                             PlacedFragment fragment,
                             BuiltInNodeDefinitions.ShapeFragmentPayload payload) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        boolean hasFill = payload.fillColor() != null;
        boolean hasStroke = payload.stroke() != null
                && payload.stroke().strokeColor() != null
                && payload.stroke().width() > 0;
        if (!hasFill && !hasStroke) {
            return;
        }

        stream.saveGraphicsState();
        try {
            if (hasStroke) {
                stream.setStrokingColor(payload.stroke().strokeColor().color());
                stream.setLineWidth((float) payload.stroke().width());
            }
            if (hasFill) {
                stream.setNonStrokingColor(payload.fillColor());
            }
            stream.addRect((float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
            if (hasFill && hasStroke) {
                stream.fillAndStroke();
            } else if (hasFill) {
                stream.fill();
            } else {
                stream.stroke();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private void renderImage(PDPageContentStream stream,
                             PlacedFragment fragment,
                             BuiltInNodeDefinitions.ImageFragmentPayload payload,
                             RenderState renderState) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }
        PDImageXObject image = renderState.imageCache.computeIfAbsent(
                payload.imageData().getFingerprint(),
                key -> createImage(renderState.document(), payload.imageData()));
        stream.drawImage(image, (float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
    }

    private PDImageXObject createImage(PDDocument document, com.demcha.compose.layout_core.components.content.ImageData imageData) {
        try {
            return PDImageXObject.createFromByteArray(document, imageData.getBytes(), imageData.getSourceKey());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode image '" + imageData.getSourceKey() + "'", e);
        }
    }

    private void renderTableRow(PDPageContentStream stream,
                                PlacedFragment fragment,
                                BuiltInNodeDefinitions.TableRowFragmentPayload payload,
                                FontLibrary fonts) throws IOException {
        for (com.demcha.compose.layout_core.components.content.table.TableResolvedCell cell : payload.cells()) {
            double cellX = fragment.x() + cell.x();
            double cellY = fragment.y();
            renderCellBox(stream, cell, cellX, cellY, payload.startsPageFragment());
            renderCellText(stream, fonts, cell, cellX, cellY);
        }
    }

    private void renderCellBox(PDPageContentStream stream,
                               com.demcha.compose.layout_core.components.content.table.TableResolvedCell cell,
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
                                com.demcha.compose.layout_core.components.content.table.TableResolvedCell cell,
                                double cellX,
                                double cellY) throws IOException {
        PdfFont font = fonts.getFont(cell.style().textStyle().fontName(), PdfFont.class).orElseThrow();
        double lineHeight = font.getLineHeight(cell.style().textStyle());
        Padding padding = cell.style().padding() == null ? Padding.zero() : cell.style().padding();
        Anchor anchor = cell.style().textAnchor() == null ? Anchor.centerLeft() : cell.style().textAnchor();

        double innerX = cellX + padding.left();
        double innerY = cellY + padding.bottom();
        double innerWidth = Math.max(0, cell.width() - padding.horizontal());
        double innerHeight = Math.max(0, cell.height() - padding.vertical());
        double blockHeight = lineHeight * Math.max(1, cell.lines().size());

        double blockY = switch (anchor.v()) {
            case TOP -> innerY + innerHeight - blockHeight;
            case MIDDLE -> innerY + (innerHeight - blockHeight) / 2.0;
            case BOTTOM, DEFAULT -> innerY;
        };

        PdfFont.VerticalMetrics metrics = font.verticalMetrics(cell.style().textStyle());

        stream.saveGraphicsState();
        try {
            stream.setFont(font.fontType(cell.style().textStyle().decoration()), (float) cell.style().textStyle().size());
            stream.setNonStrokingColor(cell.style().textStyle().color());
            for (int lineIndex = 0; lineIndex < cell.lines().size(); lineIndex++) {
                String text = sanitize(cell.lines().get(lineIndex));
                if (text.isEmpty()) {
                    continue;
                }
                double lineWidth = font.getTextWidth(cell.style().textStyle(), text);
                double lineX = switch (anchor.h()) {
                    case RIGHT -> innerX + innerWidth - lineWidth;
                    case CENTER -> innerX + (innerWidth - lineWidth) / 2.0;
                    case LEFT, DEFAULT -> innerX;
                };
                double lineBoxY = blockY + lineHeight * (cell.lines().size() - lineIndex - 1);
                double baselineY = lineBoxY + metrics.baselineOffsetFromBottom();
                stream.beginText();
                stream.newLineAtOffset((float) lineX, (float) baselineY);
                stream.showText(text);
                stream.endText();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private Set<Side> effectiveBorderSides(com.demcha.compose.layout_core.components.content.table.TableResolvedCell cell,
                                           boolean startsPageFragment) {
        EnumSet<Side> sides = cell.borderSides().isEmpty()
                ? EnumSet.noneOf(Side.class)
                : EnumSet.copyOf(cell.borderSides());
        if (startsPageFragment) {
            sides.add(Side.TOP);
        }
        return sides;
    }

    private Padding effectiveFillInsets(com.demcha.compose.layout_core.components.content.table.TableResolvedCell cell,
                                        boolean startsPageFragment) {
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

    private String sanitize(String text) {
        return text == null ? "" : text.replaceAll("\\p{C}", "");
    }

    private record RenderState(
            PDDocument document,
            FontLibrary fonts,
            Map<String, PDImageXObject> imageCache
    ) {
        private RenderState(PDDocument document, FontLibrary fonts) {
            this(document, fonts, new HashMap<>());
        }
    }
}
