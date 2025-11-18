package com.demcha.components.renderable;

import com.demcha.components.LineTextData;
import com.demcha.components.content.text.BlockTextData;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.system.GuidesRenderer;
import com.demcha.system.pdf_systems.PdfRender;
import com.demcha.system.pdf_systems.PdfRenderingSystemECS;
import com.demcha.utils.page_brecker.Breakable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * {@code BlockText} is a renderable component responsible for drawing blocks of text onto a PDF document.
 * It handles text styling, positioning, and line-by-line rendering, including line spacing and alignment.
 */
@Slf4j
@Builder
@EqualsAndHashCode
@NoArgsConstructor
public class BlockText implements PdfRender, Breakable {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    /**
     * Retrieves and validates the {@link BlockTextData} and {@link TextStyle} components from an {@link Entity}.
     * If a component is missing, it logs a warning and provides a default or empty instance.
     *
     * @param e The {@link Entity} from which to extract text data and style.
     * @return A {@link ValidatedTextData} record containing the validated {@link TextStyle} and {@link BlockTextData}.
     */
    private static Optional<ValidatedTextData> getValidatedTextData(@NonNull Entity e) {

        var blockTextDataOpt = e.getComponent(BlockTextData.class);
        var styleOpt = e.getComponent(TextStyle.class);

        BlockTextData textValue;
        TextStyle style;

        if (blockTextDataOpt.isEmpty()) {
            log.info("TextComponent has no BlockTextData.class; skipping: {}", e);
            return Optional.empty();

        } else {
            textValue = blockTextDataOpt.get();
        }
        if (styleOpt.isEmpty()) {
            log.info("TextComponent has no TextStyle; skipping: {}", e);
            style = styleOpt.orElse(TextStyle.defaultStyle());
        } else {
            style = styleOpt.get();
        }

        return Optional.of(new ValidatedTextData(style, textValue));
    }

    private static Optional<Placement> blockEntityValidate(Entity e) {
        if (!e.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", e);
            return Optional.empty();
        }

        var placementOpt = e.getComponent(Placement.class);
        if (placementOpt.isEmpty()) {
            log.warn("TextComponent has no Placement.class Component, skipping: {}", e);
            return Optional.empty();
        }
        return placementOpt;
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
    public boolean pdf(Entity e, PdfRenderingSystemECS renderingSystem, boolean guideLines) throws IOException {

        var placementOpt = blockEntityValidate(e);
        if (placementOpt.isEmpty()) return false;
        var placement = placementOpt.get();

        var validateTextOpt = getValidatedTextData(e);
        if (validateTextOpt.isEmpty()) return false;
        ValidatedTextData validateText = getValidatedTextData(e).get();


        var style = validateText.style();

        //Font Settings info
        float fontSize = (float) style.size();
        PDFont font = style.font();
        Color color = validateText.style().color();


        var blockTextData = validateText.textValue().lines();

        if (log.isDebugEnabled()) {
            log.debug("Rendering textBlock '{}' at {}", blockTextData, placement);
            log.debug("fontSize={}  textStyle= {}", fontSize, style);
        }


        int currentPage = e.getComponent(Placement.class).orElseThrow().startPage();

        // стартовая позиция (левый верх «абзаца»)


        return pdfRenderBlock(e, renderingSystem, guideLines, currentPage, font, fontSize, color, blockTextData);
//        return true;
    }

    private boolean pdfRenderBlock(Entity e, PdfRenderingSystemECS renderingSystem, boolean guideLines, int currentPage, PDFont font, float fontSize, Color color, List<LineTextData> blockTextData) throws IOException {
        boolean result = false;
        PDPageContentStream cs = null;
        try {
            cs = renderingSystem.stream() .openContentSteamForTextData(currentPage, font, fontSize, color);
            boolean isStarted = false;

            for (LineTextData ltd : blockTextData) {

                if (!isStarted) {
                    log.debug("Started print a block text, Position Y is {}", ltd.page());
                    isStarted = true;
                }

                if (currentPage != ltd.page()) {
                    currentPage = ltd.page();
                    cs = renderingSystem.stream().reopenContentStreamForTextData(cs, currentPage, font, fontSize, color);
                }

                cs.setTextMatrix(new Matrix(1, 0, 0, 1, (float) ltd.x(), (float) ltd.y()));
                cs.showText(ltd.line());
            }

            cs.endText();
            cs.restoreGraphicsState();
            if (guideLines) {
                renderingSystem.guideRenderer().guidesRender(e, DEFAULT_GUIDES);
            }

            result = true;

        } finally {
            if (cs != null) {
                cs.close();
            }
        }

        return result;
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
