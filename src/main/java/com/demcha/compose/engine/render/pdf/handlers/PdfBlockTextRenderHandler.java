package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.content.text.LineTextData;
import com.demcha.compose.engine.components.content.text.TextDataBody;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.BlockText;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.guides.GuidesRenderer;
import com.demcha.compose.engine.render.RenderHandler;
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
        Placement placement = entity.getComponent(Placement.class).orElseThrow();
        boolean singlePlacement = placement.startPage() == placement.endPage();
        PDPageContentStream contentStream = null;
        Integer openTextPage = null;

        try {
            for (LineTextData line : blockTextData) {
                if (openTextPage == null || openTextPage != line.page()) {
                    closeTextState(contentStream);
                    contentStream = null;
                    currentPage = line.page();
                    contentStream = openTextState(renderingSystem, currentPage, font, fontSize, color);
                    openTextPage = currentPage;
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

            closeTextState(contentStream);
            contentStream = null;
            if (guideLines && singlePlacement) {
                renderingSystem.guidesRenderer().guidesRender(entity, renderingSystem.pageSurface(currentPage), DEFAULT_GUIDES);
            }
        } finally {
            closeTextState(contentStream);
        }

        if (guideLines && !singlePlacement) {
            renderingSystem.guidesRenderer().guidesRender(entity, DEFAULT_GUIDES);
        }

        return true;
    }

    private void setFont(EntityManager entityManager, TextDataBody body, PDPageContentStream contentStream) throws IOException {
        var style = body.textStyle();
        PdfFont pdfFont = entityManager.getFonts().getFont(style.fontName(), PdfFont.class).orElseThrow();
        contentStream.setFont(pdfFont.fontType(style.decoration()), (float) style.size());
    }

    /**
     * Opens a text-drawing state on the session-owned page surface: saves graphics
     * state, sets font/size/color, and begins a text block.
     *
     * <p>The caller must pair this with {@link #closeTextState} before switching
     * pages or returning from the handler.</p>
     *
     * @param renderingSystem rendering system holding the active render session
     * @param currentPage     zero-based page index
     * @param font            resolved PDFBox font
     * @param fontSize        font size in points
     * @param color           non-stroking text color
     * @return the page surface with an active text block
     * @throws IOException if the content stream operation fails
     */
    private PDPageContentStream openTextState(PdfRenderingSystemECS renderingSystem,
                                              int currentPage,
                                              PDFont font,
                                              float fontSize,
                                              Color color) throws IOException {
        PDPageContentStream contentStream = renderingSystem.pageSurface(currentPage);
        contentStream.saveGraphicsState();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(color);
        contentStream.beginText();
        return contentStream;
    }

    /**
     * Ends the text block and restores graphics state opened by {@link #openTextState}.
     * Safe to call with a {@code null} stream (no-op).
     *
     * @param contentStream the page surface with an active text block, or {@code null}
     * @throws IOException if the content stream operation fails
     */
    private void closeTextState(PDPageContentStream contentStream) throws IOException {
        if (contentStream == null) {
            return;
        }
        contentStream.endText();
        contentStream.restoreGraphicsState();
    }
}
