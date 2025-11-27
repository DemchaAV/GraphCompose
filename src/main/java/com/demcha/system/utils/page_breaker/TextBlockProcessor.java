package com.demcha.system.utils.page_breaker;

import com.demcha.components.LineTextData;
import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.text.BlockTextData;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.renderable.BlockText;
import com.demcha.core.EntityManager;
import com.demcha.exceptions.BigSizeElementException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TextBlockProcessor {
    private final EntityManager entityManager;
    private PageLayoutCalculator pageLayoutCalculator;
    private PageLayoutCalculator layoutCalculator;

    public TextBlockProcessor(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.pageLayoutCalculator = new PageLayoutCalculator(entityManager);
        this.layoutCalculator = new PageLayoutCalculator(entityManager);

    }

    /**
     * Calculates and assigns the position for each line of text within a {@link BlockText}.
     * This method iterates through the lines, determines their position on the page, handles
     * page breaks, and updates the entity's {@link BlockTextData} component with the new,
     * positioned data.
     *
     * @param e       The entity containing the {@link BlockText}.
     * @param canvas  The canvas object for retrieving page dimension information.
     * @param yOffset The total Y-axis offset, which will be updated based on
     *                offsets caused by page breaks within the text block.
     * @throws IOException           if an error occurs while working with fonts.
     * @throws IllegalStateException if the entity is missing required components.
     */
    public void processTextLines(Entity e, Canvas canvas, @NonNull Offset yOffset) throws IOException, BigSizeElementException {

        Offset entityYOffset = new Offset();

        if (!e.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", e);
            return;
        }

        var positionOpt = RenderingPosition.from(e);
        if (positionOpt.isEmpty()) {
            log.warn("TextComponent has no RenderingPosition; skipping: {}", e);
            return;
        }


        var placement = e.getComponent(Placement.class).orElseThrow();
        InnerBoxSize innerBoxSize = InnerBoxSize.from(e).orElseThrow();

        BlockText.ValidatedTextData validateText = getValidatedTextData(e);


        var style = validateText.style();
        float fontSize = (float) style.size();
        PDFont font = style.font();
        var textHeight = (float) style.getLineHeight();

        float scale = fontSize / 1000f;
        PDFontDescriptor fd = font.getFontDescriptor();

        float descentPx = Math.abs((fd != null ? fd.getDescent() : font.getBoundingBox().getLowerLeftY()) * scale);

        var blockTextData = validateText.textValue().lines();

        double spacing = e.getComponent(Align.class).orElseGet(() -> {
            log.warn("TextComponent has no Align; using default: {}", e);
            return Align.defaultAlign(2);
        }).spacing() * -1;

        log.debug("Rendering textBlock '{}' at placement ({}, {}) fontSize={}  textStyle= ", blockTextData, placement.x(), placement.y());
        log.debug("fontSize={}  textStyle= {}", fontSize, style);

        int currentPage = placement.startPage();


        // стартовая позиция (левый верх «абзаца»)
        float startX = (float) placement.x() - descentPx;
        float startY = (float) (placement.y() + innerBoxSize.height()) - textHeight + descentPx; // if spacing will be negative
        boolean isStarted = false;
        BlockTextData newBlockTextData;
        List<LineTextData> assignPositionTextData = new ArrayList<>();


        for (LineTextData ltd : blockTextData) {

            if (!isStarted) {
                log.debug("Started print a block text, Position Y is {}", startY);
                isStarted = true;
                YPositionOnPage yPositionOnPage = null;
                try {
                    yPositionOnPage = pageLayoutCalculator.definePositionOnPage(startY, textHeight, 0.0, 0.0, currentPage, canvas, yOffset, false, e);
                } catch (BigSizeElementException | PageOutOfBoundException ex) {
                    log.error("BigSizeElementException {}, {}", ex, e);
                    throw new RuntimeException(String.format("BigSizeElementException %s, %s", ex, e.printInfo()));
                }
                startY = (float) yPositionOnPage.yPosition();
                currentPage = yPositionOnPage.startPage();
            }

            float currenPosition = (float) ltd.x() + startX;


            LineTextData nltd = new LineTextData(ltd, currenPosition, startY, currentPage);
            assignPositionTextData.add(nltd);
            startY -= (float) (textHeight - spacing);
            YPositionOnPage yPositionOnPage = null;
            try {
                yPositionOnPage = pageLayoutCalculator.definePositionOnPage(startY, textHeight, 0.0, 0.0, currentPage, canvas, yOffset, false, e);
            } catch (BigSizeElementException | PageOutOfBoundException ex) {
                log.error("Failed to define position on page: page={}, yOffset={} {}", currentPage, yOffset, e.printInfo(), ex);

                throw new RuntimeException(String.format("Error processing page position"), ex);
            }

            if (log.isDebugEnabled()) {
                log.debug(ltd.toString());
                log.debug("Line placement {}, Current page: {}", ltd.x(), currentPage);
                log.debug(yPositionOnPage.toString());
            }


            startY = (float) yPositionOnPage.yPosition();

            double shift = layoutCalculator.shift(e, startY, canvas);
            if (shift != 0) {
                double currentShift;
                currentShift = shift;
                startY += (float) currentShift;
                entityYOffset.incrementY(currentShift);
                log.debug("Current Entity Offset {} {}", entityYOffset, e);
            }
            if (currentPage != yPositionOnPage.startPage()) {
                currentPage = yPositionOnPage.startPage();

            }
        }


        newBlockTextData = new BlockTextData(assignPositionTextData, (float) spacing);
        e.verticalOffsetAndCorrectionSize(entityYOffset);
        yOffset.incrementY(entityYOffset);
        log.debug("Returned Offset:  {} , {}", yOffset, e);
        e.addComponent(newBlockTextData);

    }

    private BlockText.ValidatedTextData getValidatedTextData(Entity e) {
        var textValue = e.getComponent(BlockTextData.class).orElse(BlockTextData.empty());
        var style = e.getComponent(TextStyle.class).orElse(TextStyle.defaultStyle());
        return new BlockText.ValidatedTextData(style, textValue);
    }

    /**
     * Functional interface to decouple TextProcessor from the main PageBreaker logic.
     */
    @FunctionalInterface
    public interface PagePositionCalculator {
        YPositionOnPage calculate(
                double currentY,
                double height,
                double marginTop,
                double marginBottom,
                int currentPage,
                Canvas canvas,
                Offset yOffset,
                boolean isBreakable,
                Entity entity
        ) throws BigSizeElementException; // Add other exceptions if necessary
    }

    // Helper record for internal data
    private record ValidatedData(TextStyle style, BlockTextData textValue) {
    }
}