package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.components.content.text.TextDataBody;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.renderable.BlockText;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfStream;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.util.EnumSet;
import java.util.List;

@Slf4j
public final class PdfBlockTextRenderHandler implements RenderHandler<BlockText, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    @Override
    public Class<BlockText> renderType() {
        return BlockText.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          BlockText renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        var placementOpt = BlockText.validatedPlacement(entity);
        if (placementOpt.isEmpty()) {
            return false;
        }

        var validatedTextOpt = BlockText.validatedTextData(entity);
        if (validatedTextOpt.isEmpty()) {
            return false;
        }

        var validatedText = validatedTextOpt.get();
        var style = validatedText.style();
        var font = (PdfFont) manager.getFonts().getFont(style.fontName(), PdfFont.class).orElseThrow();

        float fontSize = (float) style.size();
        PDFont pdfFont = font.fontType(style.decoration());
        Color color = style.color();
        List<LineTextData> blockTextData = validatedText.textValue().lines();
        int currentPage = placementOpt.get().startPage();

        if (log.isDebugEnabled()) {
            log.debug("Rendering textBlock '{}' at {}", blockTextData, placementOpt.get());
            log.debug("fontSize={} textStyle={}", fontSize, style);
        }

        return renderBlock(entity, manager, renderingSystem, guideLines, currentPage, pdfFont, fontSize, color, blockTextData);
    }

    private boolean renderBlock(Entity entity,
                                EntityManager entityManager,
                                PdfRenderingSystemECS renderingSystem,
                                boolean guideLines,
                                int currentPage,
                                PDFont font,
                                float fontSize,
                                Color color,
                                List<LineTextData> blockTextData) throws IOException {
        boolean result = false;
        PDPageContentStream contentStream = null;
        Placement placement = entity.getComponent(Placement.class).orElseThrow();
        boolean singlePlacement = placement.startPage() == placement.endPage();

        try {
            PdfStream stream = (PdfStream) renderingSystem.stream();
            contentStream = stream.openContentSteamForTextData(currentPage, font, fontSize, color);

            for (LineTextData line : blockTextData) {
                if (currentPage != line.page()) {
                    currentPage = line.page();
                    contentStream = stream.reopenContentStreamForTextData(contentStream, currentPage, font, fontSize, color);
                }

                contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, (float) line.x(), (float) line.y()));
                for (TextDataBody body : line.bodies()) {
                    setFont(entityManager, body, contentStream);
                    try {
                        String text = BlockText.sanitizeText(body.text());
                        if (!text.isEmpty()) {
                            contentStream.showText(text);
                        }
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalCharsetNameException("Exception in rendering char " + body.text() + "\n " + line.bodies() + "\n" + ex);
                    }
                }
            }

            contentStream.endText();
            contentStream.restoreGraphicsState();
            if (guideLines && singlePlacement) {
                renderingSystem.guidesRenderer().guidesRender(entity, contentStream, DEFAULT_GUIDES);
            }
            result = true;
        } finally {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        if (guideLines && !singlePlacement) {
            renderingSystem.guidesRenderer().guidesRender(entity, DEFAULT_GUIDES);
        }

        return result;
    }

    private void setFont(EntityManager entityManager, TextDataBody body, PDPageContentStream contentStream) throws IOException {
        var style = body.textStyle();
        PdfFont pdfFont = entityManager.getFonts().getFont(style.fontName(), PdfFont.class).orElseThrow();
        contentStream.setFont(pdfFont.fontType(style.decoration()), (float) style.size());
    }
}
