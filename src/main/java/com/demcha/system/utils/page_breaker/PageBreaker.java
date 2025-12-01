package com.demcha.system.utils.page_breaker;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.renderable.BlockText;
import com.demcha.core.EntityManager;
import com.demcha.exceptions.BigSizeElementException;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.interfaces.SystemECS;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;


/**
 * A utility class responsible for the logic of breaking content across pages.
 * <p>
 * The main responsibilities of this class are:
 * <ol>
 *     <li>Sorting entities ({@link Entity}) by their vertical position.</li>
 *     <li>Calculating and assigning a page number and in-page coordinates for each entity.</li>
 *     <li>Handling "breakable" entities, such as {@link BlockText}, whose content can
 *     flow onto the next page.</li>
 * </ol>
 * The class operates in a coordinate system where the Y-axis points downwards. It does not create new pages
 * but rather calculates and assigns {@link Placement} components to entities.
 */
@Slf4j
@Data
@Accessors(chain = true)
public class PageBreaker {
    private final EntityManager entityManager;
    private PageLayoutCalculator pageLayoutCalculator;
    private TextBlockProcessor textBlockProcessor;

    public PageBreaker(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.pageLayoutCalculator = new PageLayoutCalculator(entityManager);
        this.textBlockProcessor = new TextBlockProcessor(entityManager);
    }


    // === Paging math =========================================================

    private static Placement setYInPlacement(Entity entity, double yPosition, int startPage, int endPage) {
        ComputedPosition computedPosition = entity.getComponent(ComputedPosition.class).orElseThrow();
        var size = entity.getComponent(ContentSize.class).orElseThrow();
        return new Placement(computedPosition.x(), yPosition, size.width(), size.height(), startPage, endPage);
    }

    private static Placement setYInPlacement(Entity entity, YPositionOnPage position) {
        return setYInPlacement(entity, position.yPosition(), position.startPage(), position.endPage());
    }

    public void process(@NonNull Map<UUID, Entity> entities, Canvas canvas) {
        Offset yOffset = new Offset();

        entities.entrySet().stream()
                // Sorting our Entitles in to  by Rendering position to  make Shifting lees accessible
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) ->
                                RenderingPosition.from(e.getValue())
                                        .orElseThrow(
                                                () -> new IllegalStateException("Entity " + e + " has no RenderingPosition"))
                                        .y())
                        .reversed())
                //Starting process of breaking Pages
                .forEach(e -> {
                    Entity entity = e.getValue();
                    log.info("Work with Element {} ", entity);

                    if (Breakable.class.isAssignableFrom(entity.getRender().getClass())) {
                        if (entity.hasAssignable(BlockText.class)) {
                            definePlacementForBlockText(canvas, entity, yOffset);
                        } else {
                            definePlacement(canvas, entity, yOffset, true);
                        }

                    } else {
                        log.info("{} -> {}", entity, Breakable.class);
                        definePlacement(canvas, entity, yOffset, false);
                    }
                });
    }

    /**
     * Determines and adds a {@link Placement} component to an entity.
     * This method updates the entity's vertical position based on the total {@code yOffset},
     * calculates its position on the page, and stores the result in a new {@code Placement} component.
     *
     * @param canvas      The canvas configuration.
     * @param entity      The entity to process.
     * @param yOffset     The total Y-axis offset accumulated from previous elements.
     * @param isBreakable A flag indicating whether the element can be broken across pages.
     */
    private void definePlacement(Canvas canvas, Entity entity, Offset yOffset, boolean isBreakable) {
        var computedPosition = entity.getComponent(ComputedPosition.class).orElseThrow();
        ContentSize contentSize = entity.getComponent(ContentSize.class)
                .orElseThrow(() -> new IllegalStateException("Entity " + entity + " has no ContentSize"));
        log.debug("Defining position for {}", entity);
        PageLayoutCalculator layoutCalculator = new PageLayoutCalculator(entityManager);
        YPositionOnPage position;
        try {
            position = layoutCalculator.definePositionOnPage(computedPosition.y() + yOffset.y(), entity, 0, canvas, yOffset, isBreakable);
        } catch (Exception e) {
            log.error("Error while defining position for {}", entity.printInfo(), e);
            throw new RuntimeException(entity.printInfo(), e);
        }

        Placement placement = setYInPlacement(entity, position);
        entity.addComponent(placement);
    }

    private void definePlacementForBlockText(Canvas canvas, Entity entity, Offset yOffset) {
        try {
            //definition a blockText
            textBlockProcessor.breakBlockTextInToPages(entity, canvas, yOffset);
            //definition a placementForBlockTextContainer
            definePlacement(canvas, entity, yOffset, true);
        } catch (BigSizeElementException | IOException ex) {
            log.error("Error while defining position for block text {}", entity.printInfo(), ex);
            throw new RuntimeException(String.format("Error while defining position for block text %s, %s", entity.printInfo(), ex));
        }

    }


    /**
     * Initiates the page-breaking process for all entities managed by the {@link EntityManager}.
     * This method automatically finds the active rendering system ({@link RenderingSystemECS}) to retrieve
     * canvas information ({@link Canvas}).
     *
     */
    public void process() {
        log.info("Breaking pages");
        RenderingSystemECS renderingSystemECS = null;
        log.info("Definition a RenderingSystemECS");
        for (SystemECS system : entityManager.getSystems()) {
            if (RenderingSystemECS.class.isAssignableFrom(system.getClass())) {
                renderingSystemECS = (RenderingSystemECS) system;
                break;
            }

        }
        if (renderingSystemECS == null) {
            log.error("No RenderingSystemECS found");
            throw new IllegalStateException("No RenderingSystemECS found");
        }
        process(entityManager.getEntities(), renderingSystemECS.canvas());
    }
}
