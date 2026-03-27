package com.demcha.compose.loyaut_core.components.renderable;

import com.demcha.compose.loyaut_core.components.content.text.LineTextData;
import com.demcha.compose.loyaut_core.components.content.text.BlockTextData;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.InnerBoxSize;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.coordinator.Placement;
import com.demcha.compose.loyaut_core.components.layout.coordinator.RenderingPosition;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.exceptions.RenderGuideLinesException;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.loyaut_core.system.interfaces.Font;
import com.demcha.compose.loyaut_core.system.interfaces.guides.GuidesRenderer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.util.Matrix;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;

/**
 * {@code BlockText} is a renderable component responsible for drawing blocks of text onto a PDF document.
 * It handles text styling, positioning, and line-by-line rendering, including line spacing and alignment.
 */
@Slf4j
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChunkedBlockText extends Container {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING);

    /**
     * Retrieves and validates the {@link BlockTextData} and {@link TextStyle} components from an {@link Entity}.
     * If a component is missing, it logs a warning and provides a default or empty instance.
     *
     * @param e The {@link Entity} from which to extract text data and style.
     * @return A {@link ValidatedTextData} record containing the validated {@link TextStyle} and {@link BlockTextData}.
     */
    private static ValidatedTextData getValidatedTextData(Entity e) {
        var textValueOpt = e.getComponent(BlockTextData.class);
        var styleOpt = e.getComponent(TextStyle.class);

        BlockTextData textValue;
        TextStyle style;
        if (textValueOpt.isEmpty()) {
            log.info("TextComponent has no BlockTextData; skipping: {}", e);
            textValue = textValueOpt.orElse(BlockTextData.empty());
        } else {
            textValue = textValueOpt.get();
        }
        if (styleOpt.isEmpty()) {
            log.info("TextComponent has no TextStyle; skipping: {}", e);
            style = styleOpt.orElse(TextStyle.DEFAULT_STYLE);
        } else {
            style = styleOpt.get();
        }

        return new ValidatedTextData(style, textValue);
    }

    /**
     * Renders the block of text onto the PDF content stream.
     * This method retrieves text data, style, position, and size from the given {@link Entity}.
     * It then iterates through each line of text, setting the font, size, color, and position before drawing.
     * Optionally, it can render guide lines for debugging layout.
     *
     * @param e          The {@link Entity} containing the {@link BlockTextData}, {@link TextStyle}, {@link RenderingPosition}, and {@link InnerBoxSize}.
     * @param guideLines A boolean indicating whether to render layout guides.
     * @return {@code true} if the text was rendered, {@code false} otherwise (e.g., if required components are missing).
     * @throws IOException If an error occurs during PDF content stream operations.
     */
    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {


        if (!e.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", e);
            return false;
        }

        var placementOpt = e.getComponent(Placement.class);
        if (placementOpt.isEmpty()) {
            log.warn("TextComponent has no RenderingPosition; skipping: {}", e);
            return false;
        }

        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {

            var position = placementOpt.get();
            InnerBoxSize innerBoxSize = InnerBoxSize.from(e).orElseThrow();

            ValidatedTextData validateText = getValidatedTextData(e);


            var style = validateText.style();
            TextDataContainer textDataContainer = getTextDataContainer(validateText, manager);
            float fontSize = (float) style.size();
            PdfFont font = (PdfFont) textDataContainer.font();
            var pdfFont = font.fontType(style.decoration());
            Color color = validateText.style().color();
            var textHeight = (float) textDataContainer.font().getTextHeight(style);

            float scale = fontSize / 1000f;
            PDFontDescriptor fd = pdfFont.getFontDescriptor();

            float descentPx = Math.abs((fd != null ? fd.getDescent() : pdfFont.getBoundingBox().getLowerLeftY()) * scale);

            var blockTextData = validateText.textValue().lines();

            double spacing = e.getComponent(Align.class).orElseGet(() -> {
                log.warn("TextComponent has no Align; using default: {}", e);
                return Align.defaultAlign(2);
            }).spacing() * -1;

            log.debug("Rendering textBlock '{}' at position ({}, {}) fontSize={}  textStyle= ", blockTextData, position.x(), position.y());
            log.debug("fontSize={}  textStyle= {}", fontSize, style);


            cs.saveGraphicsState();
            cs.setFont(pdfFont, fontSize);
            cs.setNonStrokingColor(color);
            cs.beginText();

            // стартовая позиция (левый верх «абзаца»)
            float startX = (float) position.x() - descentPx;
            float startY = (float) (position.y() + innerBoxSize.height()) - textHeight + descentPx; // if spacing will be negative


            for (LineTextData ltd : blockTextData) {
                float currenPosition = (float) ltd.x() + startX;
                cs.setTextMatrix(new Matrix(1, 0, 0, 1, currenPosition, startY));
//                cs.showText(ltd.line());
                startY -= (float) (textHeight - spacing);
            }

            cs.endText();
            cs.restoreGraphicsState();


            if (guideLines) {
                renderingSystemECS.guidesRenderer().guidesRender(e, cs, DEFAULT_GUIDES);
            }
        } catch (RenderGuideLinesException ex) {
            throw new RenderGuideLinesException("Error in render in Guideline " + this.getClass().getSimpleName(), ex);
        } catch (IOException ioe) {
            throw new IOException(ioe);
        }

        return true;
    }

    private @NotNull TextDataContainer getTextDataContainer(ValidatedTextData validateText, EntityManager manager) {
        var style = validateText.style();
        var classFont = manager.getSystems().getSystem(LayoutSystem.class).orElseThrow().getRenderingSystem().fontClazz();
        com.demcha.compose.loyaut_core.system.interfaces.Font font = (Font) manager.getFonts().getFont(style.fontName(), classFont).orElseThrow();
        TextDataContainer result = new TextDataContainer(style, font);
        return result;
    }

    private record TextDataContainer(TextStyle style, Font font) {
    }

    /**
     * A record to hold validated text style and block text data.
     * This is used internally to pass the extracted and validated components.
     *
     * @param style     The {@link TextStyle} applied to the text block.
     * @param textValue The {@link BlockTextData} containing the lines of text.
     */
    public record ValidatedTextData(TextStyle style, BlockTextData textValue) {
    }


}

