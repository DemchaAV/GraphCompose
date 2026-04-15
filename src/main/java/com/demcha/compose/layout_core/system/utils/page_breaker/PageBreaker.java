package com.demcha.compose.layout_core.system.utils.page_breaker;

import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.Layer;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.renderable.BlockText;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.core.LayoutTraversalContext;
import com.demcha.compose.layout_core.exceptions.BigSizeElementException;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.SystemECS;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;


/**
 * A utility class responsible for the logic of breaking content across pages.
 * <p>
 * The main responsibilities of this class are:
 * <ol>
 *     <li>building a child-first pagination order from the resolved hierarchy</li>
 *     <li>Calculating and assigning a page number and in-page coordinates for each entity.</li>
 *     <li>Handling "breakable" entities, such as {@link BlockText}, whose content can
 *     flow onto the next page.</li>
 * </ol>
 * The class operates in a coordinate system where the Y-axis points downwards. It does not create new pages
 * but rather calculates and assigns {@link Placement} components to entities.
 * <p>
 * A critical invariant of this class is that descendants must be processed before their ancestor containers.
 * The current implementation enforces that rule with a priority-based topological walk over the resolved
 * hierarchy: only nodes whose children are already processed can enter the ready queue, and unrelated nodes
 * are then ordered by layout position and depth. This keeps the algorithm renderer-agnostic while avoiding
 * repeated ancestor-chain walks inside a comparator.
 */
@Slf4j
@Data
@Accessors(chain = true)
public class PageBreaker {
    private final EntityManager entityManager;
    private PageLayoutCalculator pageLayoutCalculator;
    private TextBlockProcessor textBlockProcessor;

    /**
     * Creates a page breaker bound to one entity manager and its pagination
     * helpers.
     *
     * @param entityManager entity registry for the current document
     */
    public PageBreaker(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.pageLayoutCalculator = new PageLayoutCalculator(entityManager);
        this.textBlockProcessor = new TextBlockProcessor(entityManager);
    }


    // === Paging math =========================================================

    /**
     * Builds a {@link Placement} for the entity using a replacement Y coordinate
     * and explicit page span.
     *
     * @param entity entity being placed
     * @param yPosition resolved Y coordinate inside the target page flow
     * @param startPage first page touched by the entity
     * @param endPage last page touched by the entity
     * @return placement preserving the entity's resolved X position and size
     */
    private static Placement setYInPlacement(Entity entity, double yPosition, int startPage, int endPage) {
        ComputedPosition computedPosition = entity.require(ComputedPosition.class);
        var size = entity.require(ContentSize.class);
        return new Placement(computedPosition.x(), yPosition, size.width(), size.height(), startPage, endPage);
    }

    /**
     * Convenience overload that converts a precomputed page-position result into a
     * {@link Placement}.
     *
     * @param entity entity being placed
     * @param position resolved position/page-span tuple
     * @return placement preserving the entity's resolved X position and size
     */
    private static Placement setYInPlacement(Entity entity, YPositionOnPage position) {
        return setYInPlacement(entity, position.yPosition(), position.startPage(), position.endPage());
    }

    /**
     * Runs page-breaking for the current entity set.
     * <p>
     * The ordering step guarantees that descendants are processed before ancestors while still preferring
     * top-to-bottom layout order between unrelated subtrees.
     */
    public void process(@NonNull Map<UUID, Entity> entities, Canvas canvas) {
        LayoutTraversalContext traversalContext = LayoutTraversalContext.from(entityManager);
        process(entities, canvas, traversalContext, entityManager.getDepthById());
    }

    /**
     * Runs page-breaking with a caller-supplied traversal snapshot and depth map.
     *
     * <p>
     * This overload is used when layout has already materialized the canonical tree
     * snapshot for the pass and pagination should reuse that exact hierarchy view.
     * </p>
     *
     * @param entities entity map for the current document
     * @param canvas page canvas configuration
     * @param traversalContext canonical parent/child snapshot for this pass
     * @param depthById depth map from layout traversal
     */
    public void process(@NonNull Map<UUID, Entity> entities,
                        Canvas canvas,
                        @NonNull LayoutTraversalContext traversalContext,
                        @NonNull Map<UUID, Integer> depthById) {
        Offset yOffset = new Offset();
        List<Map.Entry<UUID, Entity>> orderedEntities = orderedForPagination(
                entities,
                traversalContext.parentById(),
                traversalContext.childrenByParent(),
                depthById);

        orderedEntities.forEach(e -> {
            Entity entity = e.getValue();

            if (Breakable.class.isAssignableFrom(entity.getRender().getClass())) {
                if (entity.hasAssignable(BlockText.class)) {
                    definePlacementForBlockText(canvas, entity, yOffset);
                } else {
                    definePlacement(canvas, entity, yOffset, true);
                }

            } else {
                definePlacement(canvas, entity, yOffset, false);
            }
        });
    }

    /**
     * Builds one pagination order for the whole pass without repeated pairwise ancestry checks.
     *
     * <p>The queue only admits nodes whose descendants are already processed, which preserves
     * the child-before-parent contract. Between unrelated ready nodes we still use layout
     * geometry so the order remains visually intuitive and deterministic.</p>
     */
    private List<Map.Entry<UUID, Entity>> orderedForPagination(Map<UUID, Entity> entities,
                                                               Map<UUID, UUID> parentById,
                                                               Map<UUID, List<UUID>> childrenByParent,
                                                               Map<UUID, Integer> depthById) {
        Map<UUID, Integer> remainingChildren = new LinkedHashMap<>();
        PriorityQueue<UUID> ready = new PriorityQueue<>(paginationPriority(entities, depthById));
        List<Map.Entry<UUID, Entity>> ordered = new ArrayList<>(entities.size());

        for (UUID entityId : entities.keySet()) {
            int childCount = 0;
            for (UUID childId : childrenByParent.getOrDefault(entityId, List.of())) {
                if (entities.containsKey(childId)) {
                    childCount++;
                }
            }
            remainingChildren.put(entityId, childCount);
            if (childCount == 0) {
                ready.add(entityId);
            }
        }

        while (!ready.isEmpty()) {
            UUID entityId = ready.poll();
            Entity entity = entities.get(entityId);
            if (entity == null) {
                continue;
            }

            ordered.add(Map.entry(entityId, entity));

            UUID parentId = parentById.get(entityId);
            if (parentId == null || !remainingChildren.containsKey(parentId)) {
                continue;
            }

            int unresolvedChildren = remainingChildren.merge(parentId, -1, Integer::sum);
            if (unresolvedChildren == 0) {
                ready.add(parentId);
            }
        }

        if (ordered.size() != entities.size()) {
            throw new IllegalStateException("Cycle detected while computing pagination order.");
        }

        return ordered;
    }

    /**
     * Returns the resolved layout Y-coordinate used for pagination ordering.
     * <p>
     * We use {@link ComputedPosition} rather than renderer-specific coordinates because pagination operates on
     * the layout result, before the final rendering pass writes output streams.
     */
    private double positionY(Entity entity) {
        return entity.require(ComputedPosition.class).y();
    }

    /**
     * Ready-queue ordering for unrelated nodes in the pagination walk.
     *
     * <p>The priority keys are chosen so that visually higher entities and deeper
     * subtree leaves are processed first:</p>
     * <ol>
     *   <li>Y-position descending — top-of-page entities come first in engine coordinates</li>
     *   <li>Depth descending — deeper children break ties in favour of leaves</li>
     *   <li>UUID string — deterministic fallback for bit-identical layouts</li>
     * </ol>
     *
     * @param entities  entity map used during this pagination pass
     * @param depthById precomputed depth map from the layout traversal
     * @return comparator for the {@link java.util.PriorityQueue} used in
     *         {@link #orderedForPagination}
     */
    private Comparator<UUID> paginationPriority(Map<UUID, Entity> entities, Map<UUID, Integer> depthById) {
        return Comparator
                .comparingDouble((UUID entityId) -> positionY(requireEntity(entities, entityId)))
                .reversed()
                .thenComparing(Comparator.comparingInt((UUID entityId) -> depthOf(entityId, requireEntity(entities, entityId), depthById)).reversed())
                .thenComparing(UUID::toString);
    }

    /**
     * Retrieves an entity from the pagination map, throwing if absent.
     *
     * @param entities entity map active during the current pagination pass
     * @param entityId id to look up
     * @return the entity, never {@code null}
     * @throws IllegalStateException if the entity is missing
     */
    private Entity requireEntity(Map<UUID, Entity> entities, UUID entityId) {
        Entity entity = entities.get(entityId);
        if (entity == null) {
            throw new IllegalStateException("Entity not found for pagination: " + entityId);
        }
        return entity;
    }

    /**
     * Returns the layout depth of an entity, falling back to its {@link Layer}
     * value when the depth map has no entry (e.g. for dynamically added entities).
     *
     * @param entityId entity id to look up
     * @param entity   the resolved entity instance
     * @param depthById precomputed depth map from the layout traversal
     * @return depth value, zero if neither source has a value
     */
    private int depthOf(UUID entityId, Entity entity, Map<UUID, Integer> depthById) {
        return depthById.getOrDefault(entityId, entity.getComponent(Layer.class)
                .map(Layer::value)
                .orElse(0));
    }

    /**
     * Determines and adds a {@link Placement} component to an entity.
     * This method updates the entity's vertical position based on the total {@code yOffset},
     * calculates its position on the page, and stores the result in a new {@code Placement} component.
     * <p>
     * For non-breakable entities, the calculator may shift the whole object to the next page. That shift can
     * propagate upward into parent container sizing before this method writes the final placement.
     *
     * @param canvas      The canvas configuration.
     * @param entity      The entity to process.
     * @param yOffset     The total Y-axis offset accumulated from previous elements.
     * @param isBreakable A flag indicating whether the element can be broken across pages.
     */
    private void definePlacement(Canvas canvas, Entity entity, Offset yOffset, boolean isBreakable) {
        var computedPosition = entity.require(ComputedPosition.class);
        entity.require(ContentSize.class);
        log.debug("Defining position for {}", entity);
        YPositionOnPage position;
        try {
            position = pageLayoutCalculator.definePositionOnPage(computedPosition.y() + yOffset.y(), entity, 0, canvas, yOffset, isBreakable);
        } catch (Exception e) {
            log.error("Error while defining position for {}", entity.printInfo(), e);
            throw new RuntimeException(entity.printInfo(), e);
        }

        Placement placement = setYInPlacement(entity, position);
        entity.addComponent(placement);
    }

    /**
     * Handles pagination for block-text entities, including line-level splitting
     * before the container-level {@link Placement} is written.
     *
     * @param canvas page canvas configuration
     * @param entity block-text entity
     * @param yOffset running pagination offset
     */
    private void definePlacementForBlockText(Canvas canvas, Entity entity, Offset yOffset) {
        try {
            //definition a blockText
            textBlockProcessor.processPageBreakerBlockText(entity, entityManager, canvas, yOffset);
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
        log.debug("Breaking pages");
        RenderingSystemECS renderingSystemECS = null;
        log.debug("Definition a RenderingSystemECS");
        for (SystemECS system : entityManager.getSystems().getStream().toList()) {
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

