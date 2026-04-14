package com.demcha.compose.layout_core.system.utils.page_breaker;

import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.BlockTextLineMetrics;
import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderingPosition;
import com.demcha.compose.layout_core.components.renderable.BlockText;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.BigSizeElementException;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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

        if (checkBlockCondition(e)) {
            return;
        }

        var position = e.getComponent(ComputedPosition.class).orElseThrow();
        InnerBoxSize innerBoxSize = InnerBoxSize.from(e).orElseThrow();
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
        BlockText.ValidatedTextData validateText = getValidatedTextData(e);
        BlockTextData blockTextComponent = validateText.textValue();
        var blockTextData = blockTextComponent.lines();

        List<TextMeasurementSystem.LineMetrics> lineMetrics = resolveLineMetrics(blockTextData, validateText.style());
        TextMeasurementSystem.LineMetrics baseMetrics = BlockTextLineMetrics.resolveStyleMetrics(
                pageLayoutCalculator.getEntityManager(),
                validateText.style());

        double spacing = e.getComponent(Align.class).orElseGet(() -> {
            log.warn("TextComponent has no Align; using default: {}", e);
            return Align.defaultAlign(2);
        }).spacing();

        if (log.isDebugEnabled()) {
            log.debug("Rendering textBlock '{}' at position ({}, {})", blockTextData, position.x(), position.y());
            log.debug("baseTextStyle={}", validateText.style());
        }

        double startX = position.x();
        double cursorTop = position.y() + padding.bottom() + innerBoxSize.height();
        List<LineTextData> assignPositionTextData = new ArrayList<>(blockTextData.size());

        for (int i = 0; i < blockTextData.size(); i++) {
            LineTextData ltd = blockTextData.get(i);
            TextMeasurementSystem.LineMetrics metrics = lineMetrics.get(i);
            double baselineOffset = baselineOffset(ltd, metrics);
            double currentX = ltd.x() + startX;
            double baselineY = cursorTop - metrics.lineHeight() + baselineOffset;

            if (log.isDebugEnabled()) {
                log.debug("line {} metrics={} baselineY={}", i, metrics, baselineY);
            }

            // Preserve cached measurement payload and only rewrite resolved placement.
            assignPositionTextData.add(new LineTextData(ltd, currentX, baselineY, 0));

            cursorTop -= metrics.lineHeight();
            if (i < lineMetrics.size() - 1) {
                cursorTop -= BlockTextLineMetrics.interLineGap(
                        metrics,
                        lineMetrics.get(i + 1),
                        baseMetrics,
                        spacing);
            }
        }

        e.addComponent(blockTextComponent.withLines(assignPositionTextData));
    }

    public Offset processPageBreakerBlockText(Entity entity,
                                              EntityManager entityManager,
                                              Canvas canvas,
                                              @NonNull Offset yOffset) throws IOException, BigSizeElementException {
        if (checkBlockCondition(entity)) {
            return new Offset();
        }

        BlockText.ValidatedTextData validatedTextData = getValidatedTextData(entity);
        BlockTextData blockTextComponent = validatedTextData.textValue();
        var blockTextData = blockTextComponent.lines();
        List<TextMeasurementSystem.LineMetrics> lineMetrics = resolveLineMetrics(blockTextData, validatedTextData.style());

        double spacing = entity.getComponent(Align.class).orElseGet(() -> {
            log.warn("TextComponent has no Align; using default: {}", entity);
            return Align.defaultAlign(2);
        }).spacing();

        final int currentPage = 0;
        Offset entityYOffset = new Offset();
        List<LineTextData> assignPositionTextData = new ArrayList<>(blockTextData.size());

        for (int i = 0; i < blockTextData.size(); i++) {
            LineTextData ltd = blockTextData.get(i);
            TextMeasurementSystem.LineMetrics metrics = lineMetrics.get(i);
            double baselineOffset = baselineOffset(ltd, metrics);
            double currentBottomY = ltd.y() - baselineOffset + yOffset.y() + entityYOffset.y();

            YPositionOnPage yPositionOnPage = definePositionOnPage(
                    currentBottomY,
                    metrics.lineHeight(),
                    currentPage,
                    canvas,
                    entityYOffset,
                    false,
                    entity);

            double baselineY = yPositionOnPage.yPosition() + baselineOffset;
            // Preserve cached measurement payload and only rewrite page-local placement.
            assignPositionTextData.add(new LineTextData(ltd, ltd.x(), baselineY, yPositionOnPage.startPage()));
        }

        finalizePageBreakingAndDefinition(entity, blockTextComponent, yOffset, assignPositionTextData, entityYOffset);
        return entityYOffset;
    }

    private void finalizePageBreakingAndDefinition(Entity entity,
                                                   BlockTextData blockTextData,
                                                   @NotNull Offset yOffset,
                                                   List<LineTextData> assignPositionTextData,
                                                   Offset entityYOffset) {
        BlockTextData newBlockTextData = blockTextData.withLines(assignPositionTextData);

        var size = entity.getComponent(ContentSize.class).orElseThrow();
        var newSize = new ContentSize(size.width(), size.height() + Math.abs(entityYOffset.y()));
        entity.addComponent(newSize);

        yOffset.incrementY(entityYOffset);
        log.debug("Returned Offset:  {} , {}", yOffset, entity);
        entity.addComponent(newBlockTextData);
    }

    private YPositionOnPage definePositionOnPage(double currentY,
                                                 double textHeight,
                                                 int currentPage,
                                                 Canvas canvas,
                                                 Offset entityYOffset,
                                                 boolean isBreakable,
                                                 Entity entity) {
        double canvasHeight = canvas.height();
        double canvasMarginTop = canvas.margin().top();
        double canvasMarginBottom = canvas.margin().bottom();
        try {
            return pageLayoutCalculator.calculatePageCoordinates(
                    currentY,
                    textHeight,
                    0.0,
                    0.0,
                    currentPage,
                    canvasHeight,
                    canvasMarginTop,
                    canvasMarginBottom,
                    entityYOffset,
                    isBreakable,
                    entity);
        } catch (BigSizeElementException ex) {
            log.error("BigSizeElementException {}, {}", ex, entity);
            throw new RuntimeException(String.format("BigSizeElementException %s, %s", ex, entity.printInfo()));
        }
    }

    private BlockText.ValidatedTextData getValidatedTextData(Entity e) {
        var textValue = e.getComponent(BlockTextData.class).orElse(BlockTextData.empty());
        TextStyle style = e.getComponent(TextStyle.class).orElse(TextStyle.DEFAULT_STYLE);
        return new BlockText.ValidatedTextData(style, textValue);
    }

    private List<TextMeasurementSystem.LineMetrics> resolveLineMetrics(List<LineTextData> lines, TextStyle fallbackStyle) {
        List<TextMeasurementSystem.LineMetrics> metrics = new ArrayList<>(lines.size());
        for (LineTextData line : lines) {
            metrics.add(line.hasCachedLineMetrics()
                    ? line.lineMetrics()
                    : BlockTextLineMetrics.resolveLineMetrics(
                            pageLayoutCalculator.getEntityManager(),
                            line,
                            fallbackStyle));
        }
        return metrics;
    }

    private double baselineOffset(LineTextData line, TextMeasurementSystem.LineMetrics metrics) {
        if (line.hasCachedBaselineOffset()) {
            return line.baselineOffset();
        }
        return metrics.baselineOffsetFromBottom();
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
        ) throws BigSizeElementException;
    }
}
