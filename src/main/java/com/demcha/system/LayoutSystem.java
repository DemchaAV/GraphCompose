package com.demcha.system;

import com.demcha.components.LineTextData;
import com.demcha.components.content.text.BlockTextData;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.geometry.Placement;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.PaddingCoordinate;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.renderable.TextComponent;
import com.demcha.components.style.Padding;
import com.demcha.core.CanvasSize;
import com.demcha.core.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>LayoutSystem</h1>
 * <p>
 * Top-down DFS layout for ECS-model with a CSS-like box model.
 * </p>
 * <p>
 * Box Model:
 * <ul>
 *   <li>OuterBoxSize = content size (inner size, excludes padding/margin)</li>
 *   <li>Placement = final rendered box (content + padding + margin)</li>
 *   <li>InnerBoxSize (computed) = parent's content area minus parent's padding</li>
 * </ul>
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class LayoutSystem implements System {
    private final CanvasSize canvasSize;
    private EntityManager entityManager;

    public LayoutSystem(PDPage page) {
        this.canvasSize = generateCanvasSizeFromPage(page);
    }

    private boolean updateContentSizeInEntity(Entity parent, double requireInnerBoxWidth, double requireInnerBoxHigh) {
        log.debug("Check if require size bigger than parent InnerBoxSize");
        var innerBoxSize = InnerBoxSize.from(parent).orElseThrow();

        if (requireInnerBoxWidth > innerBoxSize.innerW() || requireInnerBoxHigh > innerBoxSize.innerH()) {
            double wDifference = requireInnerBoxWidth - innerBoxSize.innerW();
            double hDifference = requireInnerBoxHigh - innerBoxSize.innerH();


            var contentSize = parent
                    .getComponent(ContentSize.class)
                    .orElseThrow(() -> {
                        log.error("All objects must have a ContentSize. Object {} doesn't have one.", parent);
                        return new ContentSizeNotFoundException(parent);
                    });

            log.debug("{} has been changed to", contentSize);
            ContentSize newSize = new ContentSize(contentSize.width() + wDifference, contentSize.height() + hDifference);
            parent.addComponent(newSize);

            log.debug("{} size has been changed", newSize);
            return true;
        } else {
            log.debug("{} size hasn't been changed", parent);
            return false;
        }
    }

    /**
     * log.error("❌ Critical error: {} Cannot proceed. Can not compute {} Cause: {}.", e,returnClassName);
     *
     * @param e
     * @param returnClass
     * @param cause
     */
    private void errorCalculation(Entity e, Class<? extends Component> returnClass, String cause) {
        var returnClassName = returnClass.getName();
        log.error("❌ Critical error: {} Cannot proceed. Can not compute {} Cause: {}.", e, returnClassName, cause == null ? "" : cause);
    }

    /**
     * Calculates a childEntity's {@link ComputedPosition} relative to its parentEntity entity.
     *
     * <p>If the parentEntity is {@code null}, the childEntity is treated as the root and aligned
     * to the page using {@link #positionWithAnchor(Entity, InnerBoxSize, PaddingCoordinate)}.</p>
     *
     * <p>If a parentEntity is present, its {@link InnerBoxSize} is used as the reference
     * area for alignment (delegating to {@code alinePositionWithAnchor}).</p>
     *
     * @param childEntity   the entity to position
     * @param parentEntity  the parentEntity entity, or {@code null} if the childEntity is the root
     * @param entityManager the entityManager providing page size information
     * @return an {@link Optional} with the computed position
     */

    private Optional<ComputedPosition> calculatePositionFromParent(Entity childEntity,
                                                                   Entity parentEntity,
                                                                   EntityManager entityManager) {
        log.debug("Starting calculation of computed position for {} from parentEntity {}", childEntity, parentEntity);

        // 0) Handle page-level (no parent)
        if (parentEntity == null) {
            InnerBoxSize pageArea = new InnerBoxSize(this.canvasSize.width(), this.canvasSize.height());

            PaddingCoordinate paddingPercentCoordinate = new PaddingCoordinate(this.canvasSize.x(), this.canvasSize.y());
            ComputedPosition local = positionWithAnchor(childEntity, pageArea, paddingPercentCoordinate);
            log.debug("Final computed absolute position (page-level): {}", local);
            return Optional.of(local);
        }

        // 1) Build ancestor chain: [root ... parent]
        List<Entity> chain = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        Entity cur = parentEntity;

        while (cur != null) {
            if (!seen.add(cur.getId())) {
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
                    InnerBoxSize refArea = new InnerBoxSize(this.canvasSize.width(), this.canvasSize.height());
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

        // ✅ Final summary log
        log.debug("Position calculation summary: nodesTraversed={}, depth={}, finalPosition={}",
                chain.size(), depth, childAbs);

        return Optional.of(childAbs);
    }

    /**
     * Aligns a child {@link Entity} inside a parent area using its {@link Anchor}.
     *
     * <p>The method computes a {@link ComputedPosition} based on:
     * <ul>
     *   <li>{@link OuterBoxSize} – full size including content, padding, and margin</li>
     *   <li>{@link InnerBoxSize} – parent’s available area</li>
     *   <li>{@link Anchor} – alignment point (LEFT/CENTER/RIGHT × TOP/MIDDLE/BOTTOM)</li>
     *   <li>{@link Position} – offset from the anchor</li>
     * </ul>
     *
     * @param child               entity to align
     * @param perrentInnerBoxSize parent’s content area
     * @return the computed position, also added to the entity
     *
     * <p><b>Note:</b> OuterBoxSize already includes margin, so do not add it again in formulas.</p>
     */

    private ComputedPosition positionWithAnchor(Entity child, InnerBoxSize perrentInnerBoxSize, PaddingCoordinate paddingCoordinate) {

        var computed = ComputedPosition.from(child, perrentInnerBoxSize, paddingCoordinate);
        child.addComponent(computed);
        log.debug("Computed position with Anchor has been created: {}", computed);
        log.debug("{} has been created in: {}", computed, child);
        return computed;
    }

    public void expendBoxSizeByChildren(Entity parent, Set<Entity> children) {
        var parentInner = InnerBoxSize.from(parent).orElseThrow();
        log.debug("{} {}", parent, parentInner);
        double requireContextWidth = parentInner.innerW();
        double requireContextHigth = parentInner.innerH();
        log.debug("");
        for (Entity childEntity : children) {
            Optional<OuterBoxSize> childBoxSizeComponentOpt = OuterBoxSize.from(childEntity);
            if (childBoxSizeComponentOpt.isPresent()) {
                var childBoxSize = childBoxSizeComponentOpt.get();
                log.debug("{} {}", childEntity, childBoxSize);
                requireContextWidth = Math.max(requireContextWidth, childBoxSize.width());
                requireContextHigth = Math.max(requireContextHigth, childBoxSize.height());
            }

        }
        //update OuterBoxSize if size is bigger then previous
        if (updateContentSizeInEntity(parent, requireContextWidth, requireContextHigth)) {
            log.debug("{} has been expended", parent);
        } else {
            log.debug("{} has not been expended", parent);
        }

    }


    @Override
    public void process(EntityManager entityManager) {
        this.entityManager = entityManager;
        log.info("LayoutSystem: processing");

        Map<UUID, Entity> entities = entityManager.getEntities();
        if (entities == null || entities.isEmpty()) {
            log.info("LayoutSystem: no entities to layout");
            return;
        }


        // 2) Build parent → children map
        Optional<Set<UUID>> allChildrenOpt = entityManager.getEntitiesWithComponent(ParentComponent.class);
        Optional<Map<UUID, Set<UUID>>> childrenByParentOpt;
        if (allChildrenOpt.isPresent()) {
            childrenByParentOpt = entityManager.childrenByParent(allChildrenOpt.get());
        } else {
            log.info("LayoutSystem: no children to layout");
            entities.forEach((key, value)->{
                log.info("check align");
                if (value.has(Align.class)) {
                    alignRearrange(value);

                }

            });
            return;
        }

        var childrenByParents = childrenByParentOpt.get();
        var allChildren = allChildrenOpt.get();

        //change a box size or size if child bigger then parent
        expandParentsBox(childrenByParents, entityManager);


        // 3) Roots (no parent, or parent is missing)
        Set<UUID> roots = new LinkedHashSet<>(entities.keySet()); // preserve insertion order
        roots.removeAll(allChildren); // nodes that are never children are roots

        // If you intended to prefetch/cache, store the result; otherwise remove this line.
        entityManager.getEntitiesWithComponent(ParentComponent.class)
                .ifPresent(parentEntities -> {
                    for (UUID id : parentEntities) {
                        var eOpt = entityManager.getEntity(id);
                        if (eOpt.isEmpty()) continue;

                        Entity e = eOpt.get();
                        UUID pid = e.getComponent(ParentComponent.class).get().uuid();

                        if (pid == null || !entities.containsKey(pid)) {
                            if (roots.add(id)) {
                                log.warn("LayoutSystem: entity {} references missing parent {} — treating as root", id, pid);
                            }
                        }
                    }
                });


        // 4) DFS with cycle detection
        Map<UUID, Visit> visit = new HashMap<>();
        //Definition all entities as Visit.UNSEEN
        entities.keySet().forEach(id -> visit.put(id, Visit.UNSEEN));
        var layers = entityManager.getLayers();    // NEW: layer → ordered ids
        Map<UUID, Integer> depthById = entityManager.getDepthById();
        int depth = 0;
        for (UUID root : roots) dfsLayout(root, null, entities, childrenByParents, visit, layers, depthById, depth + 1);

        log.info("LayoutSystem: layout complete (nodes: {})", entities.size());
    }

    private void dfsLayout(
            UUID id,
            UUID parentId,
            Map<UUID, Entity> entities,
            Map<UUID, Set<UUID>> childrenByParent,
            Map<UUID, Visit> visit,
            Map<Integer, List<UUID>> layers,     // NEW: layer → ordered ids
            Map<UUID, Integer> depthById,         // NEW: id → depth
            int depth                              // NEW: current depth
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
                computedPosition = ComputedPosition.from(childEntity, this.canvasSize);
            }
        } else {
            if (childEntity.has(Align.class)){
                alignRearrange(childEntity);
            }
            computedPosition = ComputedPosition.from(childEntity, canvasSize);
        }

        // IMPORTANT: store the computed position, not (0,0)
        childEntity.addComponent(computedPosition);

        OuterBoxSize outerBoxSize = OuterBoxSize.from(childEntity).orElseThrow();
        var boundingBox = new Placement(
                computedPosition.x(),
                computedPosition.y(),
                outerBoxSize.width(),
                outerBoxSize.height()
        );
        childEntity.addComponent(boundingBox);
        if (childEntity.has(Align.class)){
            alignRearrange(childEntity);
        }

        if (log.isDebugEnabled()) {
            String name = childEntity.getComponent(EntityName.class)
                    .map(EntityName::value)
                    .orElse(id.toString());
            log.debug("LayoutSystem: {} [depth={}] positioned at ({}, {})",
                    name, depth, computedPosition.x(), computedPosition.y());
        }

        // DFS to children with depth+1
        for (UUID childId : childrenByParent.getOrDefault(id, Collections.emptySet())) {
            dfsLayout(childId, id, entities, childrenByParent, visit, layers, depthById, depth + 1);
        }

        visit.put(id, Visit.DONE);
    }

    /**
     * The method normalizeBoxSize adjusts the size of parent entities. For each parent, it inspects the dimensions (OuterBoxSize) of all its direct children.
     * It then ensures the parent's OuterBoxSize is large enough to encompass its own original size and the size of its largest child by updating it to the maximum width and maximum height found.
     * In simple terms: It makes a parent container at least as big as its biggest child.
     *
     * @param childrenByParents map already sorted by parent
     */
    private void expandParentsBox(Map<UUID, Set<UUID>> childrenByParents, EntityManager entityManager) {
        log.info("LayoutSystem: normalizing box size");


        for (Map.Entry<UUID, Set<UUID>> parentUuid : childrenByParents.entrySet()) {

            var entityParentOpt = entityManager.getEntity(parentUuid.getKey());
            if (entityParentOpt.isEmpty()) {
                log.warn("LayoutSystem: hasn't find a Parent Entity by id {}", parentUuid.getKey());
                continue;
            }
            var parentEntity = entityParentOpt.get();
            if (!isExpandable(parentEntity)) {
                log.debug("{} is restricted", parentEntity);
                continue;
            }
            var childrenEntities = parentUuid.getValue()
                    .stream()
                    .map(entityManager::getEntity)
                    .map(Optional::get)
                    .collect(Collectors.toSet());


            if (parentEntity.has(Align.class)) {

                log.debug("It is a container with component Align");


            }

            log.debug("Definition {} all child entities by given Set children uuid", parentEntity);

            expendBoxSizeByChildren(parentEntity, childrenEntities);

        }
    }

    private Entity alignBlockText(Entity blockTextBox) {
        //TODO нужно имплементировать  другой алгоритм для имерения сонтент бокса если родитель это VBox или HBox должен учитываться то что спайсинг
        Align align = blockTextBox
                .getComponent(Align.class).orElse(Align.defaultAlign(2));
        var component = blockTextBox.getComponent(BlockTextData.class).orElseThrow();
        var size = blockTextBox.getComponent(ContentSize.class).orElseThrow();
        var padding = blockTextBox.getComponent(Padding.class).orElse(Padding.zero());

        var lines = component.lines();
        for (LineTextData line : lines) {
            switch (align.h()) {
                case LEFT -> {
                    double x = line.getX() + padding.left();
                    line.setX(x);
                }
                case RIGHT -> {
                    double x = size.width() - line.getWidth() + line.getX() + padding.right();
                    line.setX(x);
                }
                default -> {
                    continue;
                }
            }

        }


        return blockTextBox;
    }
    public Entity alignRearrange(Entity entity){

        if (entity.has(BlockTextData.class)) {
            return  alignBlockText(entity);
        }else {
            log.info("Has to be implemented align for entity {}", entity);
            return entity;
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

    private boolean isExpandable(Entity entity) {
        //TODO Restrictions if we need to skip a some entitie and deny expend execution
        if (entity.has(TextComponent.class)) {
            // Forbidden expend text Entitie
            return false;
        }

        return true;
    }

    private CanvasSize generateCanvasSizeFromPage(PDPage page) {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        float x = page.getMediaBox().getLowerLeftX();
        float y = page.getMediaBox().getLowerLeftY();
        return new CanvasSize(width, height, x, y);
    }

    @Override
    public String toString() {
        return "LayoutSystem";
    }

    private enum Visit {UNSEEN, ACTIVE, DONE;}
}
