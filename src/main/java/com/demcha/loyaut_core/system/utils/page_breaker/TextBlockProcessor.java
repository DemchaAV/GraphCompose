package com.demcha.loyaut_core.system.utils.page_breaker;

import com.demcha.loyaut_core.components.LineTextData;
import com.demcha.loyaut_core.components.components_builders.Canvas;
import com.demcha.loyaut_core.components.content.text.BlockTextData;
import com.demcha.loyaut_core.components.content.text.TextStyle;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.geometry.ContentSize;
import com.demcha.loyaut_core.components.geometry.InnerBoxSize;
import com.demcha.loyaut_core.components.layout.Align;
import com.demcha.loyaut_core.components.layout.coordinator.ComputedPosition;
import com.demcha.loyaut_core.components.layout.coordinator.RenderingPosition;
import com.demcha.loyaut_core.components.renderable.BlockText;
import com.demcha.loyaut_core.core.EntityManager;
import com.demcha.loyaut_core.exceptions.BigSizeElementException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TextBlockProcessor {
    private final PageLayoutCalculator pageLayoutCalculator;

    public TextBlockProcessor(EntityManager entityManager) {
        this.pageLayoutCalculator = new PageLayoutCalculator(entityManager);
    }

    private static boolean checkBlockCondition(Entity e) {
        if (!e.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", e);
            return true;
        }

        var positionOpt = RenderingPosition.from(e);
        if (positionOpt.isEmpty()) {
            log.warn("TextComponent has no RenderingPosition; skipping: {}", e);
            return true;
        }
        return false;
    }

    /**
     * Calculates and assigns the position for each line of text within a {@link BlockText}.
     * This method iterates through the lines, determines their position on the page, handles
     * page breaks, and updates the entity's {@link BlockTextData} component with the new,
     * positioned data.
     *
     * @param e The entity containing the {@link BlockText}.
     *          offsets caused by page breaks within the text block.
     * @throws IOException           if an error occurs while working with fonts.
     * @throws IllegalStateException if the entity is missing required components.
     */
    public void processLayoutSystemTextLines(Entity e) throws IOException, BigSizeElementException {

        //check blockTextCondition

        if (checkBlockCondition(e)) return;


        var position = e.getComponent(ComputedPosition.class).orElseThrow();
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
        }).spacing();

        if (log.isDebugEnabled()) {
            log.debug("Rendering textBlock '{}' at position ({}, {})", blockTextData, position.x(), position.y());
            log.debug("fontSize={}  textStyle= {}", fontSize, style);
        }


        int currentPage = 0;


        // стартовая позиция (левый верх «абзаца»)
        float startX = (float) position.x() - descentPx;
        float startY = (float) (position.y() + innerBoxSize.height()) - textHeight + descentPx; // if spacing will be negative
        BlockTextData newBlockTextData;
        List<LineTextData> assignPositionTextData = new ArrayList<>();


        float currentX = startX;
        float currentY = startY;
        for (LineTextData ltd : blockTextData) {
            log.trace(ltd.toString());

            currentX = (float) ltd.x() + startX;
            if (log.isDebugEnabled()) {
                log.debug(ltd.toString());
                log.debug("Line position {}, Current page: {}", ltd.x(), currentPage);
                log.debug("currentY {}", currentY);
            }

            LineTextData nltd = new LineTextData(ltd, currentX, currentY, 0);
            currentY = nextLine(currentY, textHeight, spacing);
            assignPositionTextData.add(nltd);

        }


        newBlockTextData = new BlockTextData(assignPositionTextData, (float) spacing);
        e.addComponent(newBlockTextData);

    }


    public Offset processPageBreakerBlockText(Entity entity, EntityManager entityManager, Canvas canvas, @NonNull Offset yOffset) throws IOException, BigSizeElementException {
        //check blockTextCondition

        if (checkBlockCondition(entity)) return new Offset();
        BlockText.ValidatedTextData validatedTextData = getValidatedTextData(entity);
        var blockTextData = validatedTextData.textValue().lines();
        var textHeight = validatedTextData.style().getLineHeight();
        double spacing = entity.getComponent(Align.class).orElseGet(() -> {
            log.warn("TextComponent has no Align; using default: {}", entity);
            return Align.defaultAlign(2);
        }).spacing();


        final int currentPage = 0;
        Offset entityYOffset = new Offset();

        List<LineTextData> assignPositionTextData = new ArrayList<>();
        for (LineTextData ltd : blockTextData) {
            log.trace(ltd.toString());
            double currentY = ltd.y() + yOffset.y() + entityYOffset.y();
            YPositionOnPage yPositionOnPage = definePositionOnPage(currentY, textHeight, currentPage, canvas, entityYOffset, false, entity);
            LineTextData newLtd = new LineTextData(ltd, ltd.x(), yPositionOnPage.yPosition(), yPositionOnPage.startPage());
            assignPositionTextData.add(newLtd);
        }
        finalizePageBreakingAndDefinition(entity, entityManager, yOffset, assignPositionTextData, (float) spacing, entityYOffset);
        return entityYOffset;

    }

    private void finalizePageBreakingAndDefinition(Entity entity, EntityManager entityManager, @NotNull Offset yOffset, List<LineTextData> assignPositionTextData, float spacing, Offset entityYOffset) {
        BlockTextData newBlockTextData;
        newBlockTextData = new BlockTextData(assignPositionTextData, spacing);

        //Updating ContainerSize
        var size = entity.getComponent(ContentSize.class).orElseThrow();
        var newSize = new ContentSize(size.width(), size.height() + Math.abs(entityYOffset.y()));
        entity.addComponent(newSize);


        yOffset.incrementY(entityYOffset);
        log.debug("Returned Offset:  {} , {}", yOffset, entity);
        entity.addComponent(newBlockTextData);
    }


    private YPositionOnPage definePositionOnPage(double currentY, double textHeight, int currentPage, Canvas canvas, Offset entityYOffset, boolean isBreakable, Entity entity) {
        //canvas
        double canvasHeight = canvas.height();
        double canvasMarginTop = canvas.margin().top();
        double canvasMarginBottom = canvas.margin().bottom();
        try {
            return pageLayoutCalculator.calculatePageCoordinates(currentY, textHeight, 0.0, 0.0,
                    currentPage, canvasHeight, canvasMarginTop, canvasMarginBottom,
                    entityYOffset, isBreakable, entity);
        } catch (BigSizeElementException e) {
            log.error("BigSizeElementException {}, {}", e, entity);
            throw new RuntimeException(String.format("BigSizeElementException %s, %s", e, entity.printInfo()));
        }
    }


    private float nextLine(double positionY, float height, double spacing) {
        return (float) (positionY - (height + spacing));
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

}