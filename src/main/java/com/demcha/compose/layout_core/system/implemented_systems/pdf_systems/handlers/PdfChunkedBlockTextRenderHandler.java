package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.renderable.ChunkedBlockText;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.RenderGuideLinesException;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;

@Slf4j
public final class PdfChunkedBlockTextRenderHandler implements RenderHandler<ChunkedBlockText, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);

    @Override
    public Class<ChunkedBlockText> renderType() {
        return ChunkedBlockText.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          ChunkedBlockText renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        if (!entity.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", entity);
            return false;
        }

        var placementOpt = entity.getComponent(Placement.class);
        if (placementOpt.isEmpty()) {
            log.warn("TextComponent has no RenderingPosition; skipping: {}", entity);
            return false;
        }

        try (PDPageContentStream contentStream = renderingSystem.stream().openContentStream(entity)) {
            var placement = placementOpt.get();
            InnerBoxSize innerBoxSize = InnerBoxSize.from(entity).orElseThrow();
            BlockTextData textValue = entity.getComponent(BlockTextData.class).orElse(BlockTextData.empty());
            TextStyle style = entity.getComponent(TextStyle.class).orElse(TextStyle.DEFAULT_STYLE);

            PdfFont font = (PdfFont) manager.getFonts().getFont(style.fontName(), PdfFont.class).orElseThrow();
            float fontSize = (float) style.size();
            var pdfFont = font.fontType(style.decoration());
            Color color = style.color();
            float textHeight = (float) font.getTextHeight(style);

            float scale = fontSize / 1000f;
            PDFontDescriptor descriptor = pdfFont.getFontDescriptor();
            float descentPx = Math.abs((descriptor != null ? descriptor.getDescent() : pdfFont.getBoundingBox().getLowerLeftY()) * scale);

            var blockTextData = textValue.lines();
            double spacing = entity.getComponent(Align.class).orElseGet(() -> {
                log.warn("TextComponent has no Align; using default: {}", entity);
                return Align.defaultAlign(2);
            }).spacing() * -1;

            contentStream.saveGraphicsState();
            contentStream.setFont(pdfFont, fontSize);
            contentStream.setNonStrokingColor(color);
            contentStream.beginText();

            float startX = (float) placement.x() - descentPx;
            float startY = (float) (placement.y() + innerBoxSize.height()) - textHeight + descentPx;

            for (LineTextData line : blockTextData) {
                float currentPosition = (float) line.x() + startX;
                contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, currentPosition, startY));
                startY -= (float) (textHeight - spacing);
            }

            contentStream.endText();
            contentStream.restoreGraphicsState();

            if (guideLines) {
                renderingSystem.guidesRenderer().guidesRender(entity, contentStream, DEFAULT_GUIDES);
            }
        } catch (RenderGuideLinesException ex) {
            throw new RenderGuideLinesException("Error in render in Guideline " + this.getClass().getSimpleName(), ex);
        }

        return true;
    }
}
