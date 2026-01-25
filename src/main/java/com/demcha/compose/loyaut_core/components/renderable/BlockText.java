package com.demcha.compose.loyaut_core.components.renderable;

import com.demcha.compose.loyaut_core.components.LineTextData;
import com.demcha.compose.loyaut_core.components.content.text.BlockTextData;
import com.demcha.compose.loyaut_core.components.content.text.TextDataBody;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.InnerBoxSize;
import com.demcha.compose.loyaut_core.components.layout.coordinator.Placement;
import com.demcha.compose.loyaut_core.components.layout.coordinator.RenderingPosition;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfStream;
import com.demcha.compose.loyaut_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.loyaut_core.system.utils.page_breaker.Breakable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
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
            style = styleOpt.orElse(TextStyle.DEFAULT_STYLE);
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

    public static String sanitizeText(String rawText) {
        if (rawText == null) {
            return "";
        }
        // \p{C} matches invisible control characters (including \0)
        return rawText.replaceAll("\\p{C}", "");
    }

    private static void setFont(EntityManager entityManager, TextDataBody textDataBody, PDPageContentStream cs) throws IOException {
        PdfFont pdfFont = entityManager.getFonts().getFont(textDataBody.textStyle().fontName(), PdfFont.class).orElseThrow();
        PDFont pdFont = pdfFont.fontType(textDataBody.textStyle().decoration());
        float size = (float) textDataBody.textStyle().size();
        cs.setFont(pdFont, size);
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
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystem, boolean guideLines) throws IOException {

        var placementOpt = blockEntityValidate(e);
        if (placementOpt.isEmpty()) return false;
        var placement = placementOpt.get();

        var validateTextOpt = getValidatedTextData(e);
        if (validateTextOpt.isEmpty()) return false;
        ValidatedTextData validateText = getValidatedTextData(e).get();


        var style = validateText.style();

        var aClass = manager.getSystems().getSystem(LayoutSystem.class).orElseThrow().getRenderingSystem().fontClazz();
        var font = (PdfFont) manager.getFonts().getFont(style.fontName(), aClass).orElseThrow();

        //Font Settings info
        float fontSize = (float) style.size();
        PDFont pdfFont = font.fontType(style.decoration());
        Color color = validateText.style().color();


        var blockTextData = validateText.textValue().lines();

        if (log.isDebugEnabled()) {
            log.debug("Rendering textBlock '{}' at {}", blockTextData, placement);
            log.debug("fontSize={}  textStyle= {}", fontSize, style);
        }


        int currentPage = e.getComponent(Placement.class).orElseThrow().startPage();

        // стартовая позиция (левый верх «абзаца»)


        return pdfRenderBlock(e, manager, renderingSystem, guideLines, currentPage, pdfFont, fontSize, color, blockTextData);
//        return true;
    }

    private boolean pdfRenderBlock(Entity e, EntityManager entityManager, PdfRenderingSystemECS renderingSystem, boolean guideLines, int currentPage, PDFont font, float fontSize, Color color, List<LineTextData> blockTextData) throws IOException {
        boolean result = false;
        PDPageContentStream cs = null;
        Placement placement = e.getComponent(Placement.class).orElseThrow();
        boolean singlePlacement = placement.startPage() == placement.endPage();
        try {
            PdfStream stream = (PdfStream) renderingSystem.stream();
            cs = stream.openContentSteamForTextData(currentPage, font, fontSize, color);
            boolean isStarted = false;

            for (LineTextData ltd : blockTextData) {

                if (!isStarted) {
                    log.debug("Started print a block text, Position Y is {}", ltd.page());
                    isStarted = true;
                }


                if (currentPage != ltd.page()) {
                    currentPage = ltd.page();
                    cs = stream.reopenContentStreamForTextData(cs, currentPage, font, fontSize, color);
                }

                cs.setTextMatrix(new Matrix(1, 0, 0, 1, (float) ltd.x(), (float) ltd.y()));
                List<TextDataBody> textDataBodies = ltd.bodies();
                for (TextDataBody textDataBody : textDataBodies) {
                    setFont(entityManager, textDataBody, cs);
                    try {
                        cs.showText(textDataBody.text());
                    } catch (IllegalArgumentException il) {
                        throw new IllegalCharsetNameException("Exception in rendering char  " + textDataBody.text() + " " + il);
                    }
                }
            }

            cs.endText();
            cs.restoreGraphicsState();
            if (guideLines) {
                if (singlePlacement) {
                    renderingSystem.guidesRenderer().guidesRender(e, cs, DEFAULT_GUIDES);
                }

            }

            result = true;

        } finally {
            if (cs != null) {
                cs.close();
            }
        }

        if (guideLines) {
            if (!singlePlacement) {
                renderingSystem.guidesRenderer().guidesRender(e, DEFAULT_GUIDES);
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
