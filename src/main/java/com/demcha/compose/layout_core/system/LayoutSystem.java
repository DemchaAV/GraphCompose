package com.demcha.compose.layout_core.system;

import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.core.EntityName;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.geometry.OuterBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.Layer;
import com.demcha.compose.layout_core.components.layout.ParentComponent;
import com.demcha.compose.layout_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.layout_core.components.layout.coordinator.PaddingCoordinate;
import com.demcha.compose.layout_core.components.layout.coordinator.Position;
import com.demcha.compose.layout_core.components.renderable.BlockText;
import com.demcha.compose.layout_core.components.renderable.TextComponent;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.BigSizeElementException;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.SystemECS;
import com.demcha.compose.layout_core.system.utils.containerUtils.ContainerExpander;
import com.demcha.compose.layout_core.system.utils.containerUtils.ContainerLayoutManager;
import com.demcha.compose.layout_core.system.utils.containerUtils.ModuleWidthResolver;
import com.demcha.compose.layout_core.system.utils.page_breaker.PageBreaker;
import com.demcha.compose.layout_core.system.utils.page_breaker.TextBlockProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * Main layout pass for GraphCompose entities.
 * <p>
 * This system turns builder-produced entities into resolved geometry. It walks
 * the entity hierarchy, computes container relationships, expands parent boxes
 * when needed, resolves per-entity positions, and finally delegates to the
 * pagination stage so entities receive final {@code Placement} metadata.
 * </p>
 *
 * <p>Conceptually this is the "builder intent to geometry" phase of the engine.</p>
 *
 * <p>Box model summary:</p>
 * <ul>
 *   <li>{@code ContentSize}: declared content dimensions</li>
 *   <li>{@code InnerBoxSize}: available child area after padding</li>
 *   <li>{@code OuterBoxSize}: content plus padding and margin</li>
 *   <li>{@code Placement}: final bounding box and page span</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class LayoutSystem<T extends RenderingSystemECS<?>> implements SystemECS {
    private final Canvas canvas;
    @Getter
    private final T renderingSystem;
    private TextBlockProcessor textBlockProcessor;

    /**
     * Resolves an entity position from its parent context, walking the ancestor
     * chain when intermediate computed positions are still missing.
     *
     * @param childEntity the entity to position
     * @param parentEntity the parent entity, or {@code null} for a root entity
     * @param entityManager entity registry providing parent lookups
     * @return the resolved position when computation succeeds
     */

    private Optional<ComputedPosition> calculatePositionFromParent(Entity childEntity,
                                                                   Entity parentEntity,
                                                                   EntityManager entityManager) {
        log.debug("Starting calculation of computed position for {} from parentEntity {}", childEntity, parentEntity);

        // 0) Handle page-level (no parent)
        if (parentEntity == null) {
            InnerBoxSize pageArea = new InnerBoxSize(this.canvas.width(), this.canvas.height());

            PaddingCoordinate paddingPercentCoordinate = new PaddingCoordinate(this.canvas.x(), this.canvas.y());
            ComputedPosition local = positionWithAnchor(childEntity, pageArea, paddingPercentCoordinate);
            log.debug("Final computed absolute position (page-level): {}", local);
            return Optional.of(local);
        }

        // 1) Build ancestor chain: [root ... parent]
        List<Entity> chain = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        Entity cur = parentEntity;

        while (cur != null) {
            if (!seen.add(cur.getUuid())) {
                log.error("Cycle detected in ParentComponent chain at entity {}", cur);
                throw new IllegalStateException("Cycle detected in parent chain");
            }
            chain.add(cur);

            var parentCompOpt = cur.getComponent(ParentComponent.class);
            if (parentCompOpt.isEmpty()) {
                break; // cur is root
            }
            UUID gpId = parentCompOpt.get().uuid();
            cur = entityManager.getEntity(gpId).orElse(null);
        }

        int depth = chain.size(); // глубина

        // 2) Ensure each ancestor has InnerBoxSize and ComputedPosition
        for (int i = 0; i < depth; i++) {
            Entity e = chain.get(i); // root -> ... -> parent

            var inner = InnerBoxSize.from(e).orElseThrow();

            if (e.getComponent(ComputedPosition.class).isEmpty()) {
                var parentComp = e.getComponent(ParentComponent.class);
                if (parentComp.isPresent()) {
                    Entity p = entityManager.getEntity(parentComp.get().uuid())
                            .orElseThrow(() -> new IllegalStateException("Parent not found for " + e));

                    var parentInner = InnerBoxSize.from(p).orElseThrow();
                    var pagingCoordinate = PaddingCoordinate.from(parentEntity);
                    ComputedPosition local = positionWithAnchor(e, parentInner, pagingCoordinate);

                    ComputedPosition parentAbs = p.getComponent(ComputedPosition.class)
                            .orElse(new ComputedPosition(0, 0));

                    e.addComponent(new ComputedPosition(local.x() + parentAbs.x(),
                            local.y() + parentAbs.y()));
                } else {
                    InnerBoxSize refArea = new InnerBoxSize(this.canvas.width(), this.canvas.height());
                    var pagingCoordinate = PaddingCoordinate.from(parentEntity);
                    ComputedPosition local = positionWithAnchor(e, refArea, pagingCoordinate);
                    e.addComponent(local);
                }
            }
        }

        // 3) Compute child's final position
        InnerBoxSize parentInner = InnerBoxSize.from(parentEntity).orElseThrow();
        var pagingCoordinate = PaddingCoordinate.from(parentEntity);

        ComputedPosition childLocal = positionWithAnchor(childEntity, parentInner, pagingCoordinate);
        ComputedPosition parentAbs = parentEntity.getComponent(ComputedPosition.class)
                .orElse(new ComputedPosition(0, 0));

        ComputedPosition childAbs = new ComputedPosition(
                childLocal.x() + parentAbs.x(),
                childLocal.y() + parentAbs.y()
        );

        //  Final summary log
        log.debug("Position calculation summary: nodesTraversed={}, depth={}, finalPosition={}",
                chain.size(), depth, childAbs);

        return Optional.of(childAbs);
    }

    /**
     * Computes a child's anchored position inside the supplied parent area and
     * stores the result back on the entity.
     *
     * @param child entity being positioned
     * @param parentInnerBoxSize available parent area
     * @param paddingCoordinate parent padding origin
     * @return the computed position, also attached to the entity
     */

    private ComputedPosition positionWithAnchor(Entity child, InnerBoxSize parentInnerBoxSize, PaddingCoordinate paddingCoordinate) {

        var computed = ComputedPosition.from(child, parentInnerBoxSize, paddingCoordinate);
        child.addComponent(computed);
        log.debug("Computed position with Anchor has been created: {}", computed);
        log.debug("{} has been created in: {}", computed, child);
        return computed;
    }


    @Override
    public void process(EntityManager entityManager) {
        log.info("LayoutSystem: processing...");
        textBlockProcessor = new TextBlockProcessor(entityManager);

        final var entities = entityManager.getEntities();
        if (entities == null || entities.isEmpty()) {
            log.info("LayoutSystem: no entities to lay out");
            return;
        }

        // 1) Collect all nodes that have a ParentComponent (i.e., are children)
        final Set<UUID> childIds = entityManager
                .getEntitiesWithComponent(ParentComponent.class)
                .orElseGet(Set::of);

        // 2) Build parent -> children map
        final Map<UUID, Set<UUID>> childrenByParent = entityManager
                .childrenByParent(childIds)
                .orElseGet(Map::of);
//TODO currently en testing stage
        ModuleWidthResolver.process(entityManager, canvas);
        ContainerLayoutManager.process(childrenByParent, entityManager);

        // 3) Expand parent boxes if needed (any child larger than parent)
        ContainerExpander.process(childrenByParent, entityManager);

        // 4) Compute roots
        final Set<UUID> roots = computeRoots(entities, childIds, entityManager);

        // 5) DFS with cycle detection
        final Map<UUID, Visit> visit = new HashMap<>(entities.size());
        entities.keySet().forEach(id -> visit.put(id, Visit.UNSEEN));

        final var layers = entityManager.getLayers();     // layer → ordered ids (assumed provided)
        final var depthById = entityManager.getDepthById();

        for (UUID root : roots) {
            dfsLayout(
                    root,
                    null,
                    entities,
                    childrenByParent,
                    visit,
                    layers,
                    depthById,
                    1
                    , entityManager
            );
        }

        log.info("LayoutSystem: layout complete (nodes: {})", entities.size());

//         Pagination

        boolean withPagination = true;

        if (withPagination) {
            var pageBreaker = new PageBreaker(entityManager);
            pageBreaker.process();
        } else {
            PaginationLayoutSystem paginationLayoutSystem = new PaginationLayoutSystem();
            paginationLayoutSystem.process(entityManager);
        }


    }

    /**
     * Computes the root entities that should start independent layout traversals.
     *
     * <p>An entity is treated as a root when it is not referenced as a child or
     * when its declared parent cannot be resolved.</p>
     */
    private Set<UUID> computeRoots(Map<UUID, Entity> entities,
                                   Set<UUID> childIds,
                                   EntityManager entityManager) {
        final Set<UUID> roots = new LinkedHashSet<>(entities.keySet());
        // Nodes that are never children are roots
        roots.removeAll(childIds);

        // Nodes with missing/invalid parent are also roots
        entityManager.getEntitiesWithComponent(ParentComponent.class).ifPresent(parentEntities -> {
            for (UUID id : parentEntities) {
                final var eOpt = entityManager.getEntity(id);
                if (eOpt.isEmpty()) continue;

                final UUID pid = eOpt.get()
                        .getComponent(ParentComponent.class)
                        .map(ParentComponent::uuid)
                        .orElse(null);

                if (pid == null || !entities.containsKey(pid)) {
                    if (roots.add(id)) {
                        log.warn("LayoutSystem: entity {} references missing parent {} — treating as root", id, pid);
                    }
                }
            }
        });

        if (roots.isEmpty()) {
            // Defensive: if everything is wired as a child forming a closed cycle, fall back to all nodes
            log.warn("LayoutSystem: computed empty root set; falling back to all entities as roots");
            roots.addAll(entities.keySet());
        }
        return roots;
    }

    /**
     * Performs the recursive layout traversal for one hierarchy branch.
     *
     * <p>Parents are processed before children so child layout can depend on
     * parent inner size, padding coordinates, and container ordering metadata.</p>
     */

    private void dfsLayout(
            UUID id,
            UUID parentId,
            Map<UUID, Entity> entities,
            Map<UUID, Set<UUID>> childrenByParent,
            Map<UUID, Visit> visit,
            Map<Integer, List<UUID>> layers,     // NEW: layer → ordered ids
            Map<UUID, Integer> depthById,         // NEW: id → depth
            int depth,                              // NEW: current depth
            EntityManager entityManager

    ) {
        Visit st = visit.getOrDefault(id, Visit.UNSEEN);
        if (st == Visit.DONE) return;
        if (st == Visit.ACTIVE) throw new IllegalStateException("LayoutSystem: cycle detected at entity " + id);
        visit.put(id, Visit.ACTIVE);

        Entity childEntity = entities.get(id);
        if (childEntity == null) {
            log.warn("LayoutSystem: entity {} not found — skipping", id);
            visit.put(id, Visit.DONE);
            return;
        }

        // --- LAYERS COLLECTION ---
        layers.computeIfAbsent(depth, k -> new ArrayList<>()).add(id);
        depthById.put(id, depth);

        // --- POSITION CALC ---
        ComputedPosition computedPosition;
        if (parentId != null) {
            Entity parent = entities.get(parentId);
            if (parent != null) {
                computedPosition = ComputedPosition.from(childEntity, parent);
            } else {
                log.warn("LayoutSystem: parent {} of {} not found — using root positioning (Position + Margin)", parentId, id);
                computedPosition = ComputedPosition.from(childEntity, this.canvas);
            }
        } else {
            computedPosition = ComputedPosition.from(childEntity, canvas);
        }

        // IMPORTANT: store the computed position, not (0,0)
        childEntity.addComponent(computedPosition);
        childEntity.addComponent(new Layer(depth));

        if (childEntity.has(Align.class)) {
            alignRearrangeBlockText(childEntity, entityManager);
        }

        if (log.isDebugEnabled()) {
            String name = childEntity.getComponent(EntityName.class)
                    .map(EntityName::value)
                    .orElse(id.toString());
            log.debug("LayoutSystem: {} [depth={}] positioned at ({}, {})",
                    name, depth, computedPosition.x(), computedPosition.y());
        }
        if (BlockText.class.isAssignableFrom(childEntity.getRender().getClass())) {
            try {
                textBlockProcessor.processLayoutSystemTextLines(childEntity);
            } catch (IOException e) {
                log.error("Error during processing block text entity {}", childEntity);
                throw new RuntimeException(String.format("Error during processing block text entity %s", childEntity), e);
            } catch (BigSizeElementException e) {
                log.error(String.format("To big size line in block text, Error during processing block text entity %s", childEntity), e);

                throw new RuntimeException(e);
            }
        }

        // DFS to children with depth+1
        for (UUID childId : childrenByParent.getOrDefault(id, Collections.emptySet())) {
            dfsLayout(childId, id, entities, childrenByParent, visit, layers, depthById, depth + 1, entityManager);
        }

        visit.put(id, Visit.DONE);
    }

    private Entity alignBlockText(Entity blockTextBox, EntityManager entityManager) {
        Align align = blockTextBox
                .getComponent(Align.class).orElse(Align.defaultAlign(2));
        var component = blockTextBox.getComponent(BlockTextData.class).orElseThrow();
        var size = blockTextBox.getComponent(ContentSize.class).orElseThrow();
        var padding = blockTextBox.getComponent(Padding.class).orElse(Padding.zero());
        TextStyle style = blockTextBox.getComponent(TextStyle.class).orElseThrow();

        var font = entityManager.getFonts().getFont(style.fontName(), renderingSystem.fontClazz()).orElseThrow();

        var lines = component.lines();
        for (LineTextData line : lines) {
            switch (align.h()) {
                case LEFT -> {
                    double x = line.x() + padding.left();
                    line.x(x);
                }
                case RIGHT -> {
                    double x = size.width() - line.width(font) - padding.right();
                    line.x(x);
                }
                case CENTER -> {
                    double x = (size.width() - line.width(font) + padding.left()) / 2;
                    line.x(x);
                }

            }

        }


        return blockTextBox;
    }

    public Optional<Entity> alignRearrangeBlockText(Entity entity, EntityManager entityManager) {

        if (entity.has(BlockTextData.class)) {
            return Optional.of(alignBlockText(entity, entityManager));
        } else {
            log.info("Entity  is not a BlockTextData");
            return Optional.empty();
        }
    }


    /**
     * Returns whether the given entity must NOT be expanded.
     *
     * <p>Rules:
     * <ul>
     *   <li>Has {@code TextComponent}  → return {@code true}  (do not expand)</li>
     *   <li>Has {@code Box}   → return {@code false} (allow expansion)</li>
     *   <li>Otherwise         → return {@code true}  (do not expand)</li>
     * </ul>
     *
     * @param entity the entity to check
     * @return {@code true} to skip expansion; {@code false} to allow it
     */

    @Deprecated
    private boolean isExpandable(Entity entity) {
        if (entity.has(TextComponent.class)) {
            // Forbidden expend text Entitie
            return false;
        }

        return true;
    }


    @Override
    public String toString() {
        return "LayoutSystem";
    }

    public enum Visit {UNSEEN, ACTIVE, DONE;}
}


