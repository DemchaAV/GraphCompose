package com.demcha.system;

import com.demcha.components.content.Box;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.content.text.Text;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.BoundingBox;
import com.demcha.components.geometry.BoxSize;
import com.demcha.components.geometry.ContentBox;
import com.demcha.components.geometry.Size;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.ComputedPosition;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import lombok.extern.slf4j.Slf4j;

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
 *   <li>BoxSize = content size (inner size, excludes padding/margin)</li>
 *   <li>BoundingBox = final rendered box (content + padding + margin)</li>
 *   <li>ContentBox (computed) = parent's content area minus parent's padding</li>
 * </ul>
 * </p>
 */
@Slf4j
public class LayoutSystem implements System {

    // ------------------------------------------------------------
    // Main process
    // ------------------------------------------------------------

    private static void updateBoxSize(Entity parent, ContentBox contentBox, double requireContextWidth, double requireContextHigh) {
        log.debug("Check if require size bigger than parent content size");
        if (requireContextWidth > contentBox.innerW() || requireContextHigh > contentBox.innerH()) {
            double wDifference = requireContextWidth - contentBox.innerW();
            double hDifference = requireContextHigh - contentBox.innerH();

            var boxOpt = parent.getComponent(BoxSize.class);
            boxOpt.ifPresent(boxSize -> {
                parent.addComponent(new BoxSize(boxSize.width() + wDifference, boxSize.height() + hDifference));
                log.debug("Expend {} {} to {}", parent, boxSize, parent.getComponent(BoxSize.class).get());
            });

            var sizeOpt = parent.getComponent(Size.class);
            sizeOpt.ifPresent(size -> {
                parent.addComponent(new Size(size.width() + wDifference, size.height() + hDifference));
                log.debug("Expend {} {} to {}", parent, size, parent.getComponent(Size.class).get());
            });
        } else {
            log.debug("{} BoxSize hasn't been changed", parent);
        }
    }

    // ------------------------------------------------------------

    private static OptionalDouble rectWidthOf(Entity e) {
        return e.getComponent(Rectangle.class)
                .map(Rectangle::width)
                .map(OptionalDouble::of)
                .orElse(OptionalDouble.empty());
    }

    private static OptionalDouble rectHeightOf(Entity e) {
        return e.getComponent(Rectangle.class)
                .map(Rectangle::high)
                .map(OptionalDouble::of)
                .orElse(OptionalDouble.empty());
    }

    /**
     * Content width priority: BoxSize → Rectangle → (BoundingBox − margin).
     */
    private static OptionalDouble contentWidthOf(Entity e) {
        var box = e.getComponent(BoxSize.class);
        if (box.isPresent()) return OptionalDouble.of(box.get().width());

        var rw = rectWidthOf(e);
        if (rw.isPresent()) return rw;

        var bb = e.getComponent(BoundingBox.class);
        if (bb.isPresent()) {
            Margin m = e.getComponent(Margin.class).orElse(Margin.zero());
            return OptionalDouble.of(Math.max(0, bb.get().width() - m.horizontal()));
        }
        return OptionalDouble.empty();
    }

    /**
     * Content height priority: BoxSize → Rectangle → (BoundingBox − margin).
     */
    private static OptionalDouble contentHeightOf(Entity e) {
        var box = e.getComponent(BoxSize.class);
        if (box.isPresent()) return OptionalDouble.of(box.get().height());

        var rh = rectHeightOf(e);
        if (rh.isPresent()) return rh;

        var bb = e.getComponent(BoundingBox.class);
        if (bb.isPresent()) {
            Margin m = e.getComponent(Margin.class).orElse(Margin.zero());
            return OptionalDouble.of(Math.max(0, bb.get().height() - m.vertical()));
        }
        return OptionalDouble.empty();
    }

    /**
     * Total width (rendered): BoundingBox → (Rectangle + margin) → (BoxSize + margin).
     */
    private static OptionalDouble totalWidthOf(Entity e) {
        var bb = e.getComponent(BoundingBox.class);
        if (bb.isPresent()) return OptionalDouble.of(bb.get().width());

        Margin m = e.getComponent(Margin.class).orElse(Margin.zero());
        var rw = rectWidthOf(e);
        if (rw.isPresent()) return OptionalDouble.of(rw.getAsDouble() + m.horizontal());

        var box = e.getComponent(BoxSize.class);
        if (box.isPresent()) return OptionalDouble.of(box.get().width() + m.horizontal());

        return OptionalDouble.empty();
    }

    // ------------------------------------------------------------

    /**
     * Total height (rendered): BoundingBox → (Rectangle + margin) → (BoxSize + margin).
     */
    private static OptionalDouble totalHeightOf(Entity e) {
        var bb = e.getComponent(BoundingBox.class);
        if (bb.isPresent()) return OptionalDouble.of(bb.get().height());

        Margin m = e.getComponent(Margin.class).orElse(Margin.zero());
        var rh = rectHeightOf(e);
        if (rh.isPresent()) return OptionalDouble.of(rh.getAsDouble() + m.vertical()); // FIX: vertical()

        var box = e.getComponent(BoxSize.class);
        if (box.isPresent()) return OptionalDouble.of(box.get().height() + m.vertical());

        return OptionalDouble.empty();
    }

    // ------------------------------------------------------------
    // Content box (parent inner area minus padding)
    private static void extracted(Map<UUID, Entity> entities, Map<UUID, List<UUID>> childrenByParent, Set<UUID> allChildren) {
        for (Map.Entry<UUID, Entity> en : entities.entrySet()) {
            UUID id = en.getKey();
            var pc = en.getValue().getComponent(ParentComponent.class);
            if (pc.isPresent()) {
                UUID pid = pc.get().uuid();
                if (pid != null) {
                    childrenByParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(id);
                    allChildren.add(id);
                }
            }
        }
    }

    @Override
    public void process(PdfDocument pdfDocument) {
        log.info("LayoutSystem: processing");

        Map<UUID, Entity> entities = pdfDocument.getEntities();
        if (entities == null || entities.isEmpty()) {
            log.info("LayoutSystem: no entities to layout");
            return;
        }

        // 1) Definition BoxSize and ContentSize
        log.debug("Starting BoxSize and ContentBox normalization for entities");
        for (Entity e : entities.values()) {

            Optional<BoxSize> boxSizeOpt = calculateBoxSize(e);
            boxSizeOpt.ifPresent(e::addComponent);

            Optional<ContentBox> contentBoxOpt = calculateContentBox(e);
            contentBoxOpt.ifPresent(e::addComponent);
        }
        log.debug("🏁 Finished size normalization for entities");

        // 2) Build parent → children map
        Optional<Set<UUID>> allChildrenOpt = pdfDocument.getEntitiesWithComponent(ParentComponent.class);
        Optional<Map<UUID, Set<UUID>>> childrenByParentOpt;
        if (allChildrenOpt.isPresent()) {
            childrenByParentOpt = pdfDocument.childrenByParent(allChildrenOpt.get());
        } else {
            log.info("LayoutSystem: no children to layout");
            return;
        }

        var childrenByParents = childrenByParentOpt.get();
        var allChildren = allChildrenOpt.get();

        //change a box size or size if childe bigger then parent

        expandParentsBoxSize(childrenByParents, pdfDocument);


        // 3) Roots (no parent, or parent is missing)
        Set<UUID> roots = new LinkedHashSet<>(entities.keySet()); // preserve insertion order
        roots.removeAll(allChildren); // nodes that are never children are roots

        // If you intended to prefetch/cache, store the result; otherwise remove this line.
        pdfDocument.getEntitiesWithComponent(ParentComponent.class)
                .ifPresent(parentEntities -> {
                    for (UUID id : parentEntities) {
                        var eOpt = pdfDocument.getEntity(id);
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
        for (UUID root : roots) dfsLayout(root, null, entities, childrenByParents, visit);

        log.info("LayoutSystem: layout complete (nodes: {})", entities.size());
    }

    // ------------------------------------------------------------

    /**
     * Calculates and sets the final {@link BoxSize} and {@link ContentBox} for a given entity
     * based on its {@link Size}, {@link Margin}, and {@link Padding} components.
     * <p>
     * This method enforces a box model similar to the CSS Box Model, ensuring that every
     * entity has a well-defined outer boundary and an inner content area.
     *
     * <h3>Logic:</h3>
     * <ol>
     * <li><b>Requirement Check</b>: It first ensures the entity has a base {@link Size} component.
     * If not, it throws a {@link SizeNotFoundException} as size is mandatory for calculations.</li>
     * <li><b>BoxSize Calculation</b>:
     * <ul>
     * <li>It calculates the required space by adding the horizontal and vertical {@link Margin}
     * to the entity's base {@link Size}.</li>
     * <li>If the entity already has a {@link BoxSize}, the new BoxSize will be the maximum of
     * the existing and the required sizes. This ensures the box is large enough to
     * accommodate its content while respecting any predefined minimum size.</li>
     * <li>If no {@link BoxSize} exists, a new one is created from the required size.</li>
     * </ul>
     * </li>
     * <li><b>ContentBox Calculation</b>: It calculates the available space for content by subtracting
     * the horizontal and vertical {@link Padding} from the entity's base {@link Size}.</li>
     * <li><b>Component Update</b>: It adds/updates the calculated {@link BoxSize} and {@link ContentBox}
     * components on the entity.</li>
     * </ol>
     *
     * @param e The entity to process. It must not be null.
     * @throws SizeNotFoundException if the entity does not have a {@link Size} component,
     *                               which is essential for the normalization calculations.
     * @see BoxSize
     * @see ContentBox
     * @see Size
     * @see Margin
     * @see Padding
     */

    // ------------------------------------------------------------
    public Optional<ContentBox> calculateContentBox(Entity e) {
        log.debug("Starting calculation of ContentBox: {}", e);
        Size size = null;
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());

        if (e.has(Size.class)) {
            // Directly use Size (already margin-free)
            size = e.getComponent(Size.class).get();
        } else if (e.has(BoxSize.class)) {
            // Convert BoxSize -> Size by removing margins
            size = calculateSizeFromBoxSize(e);
        } else {
            return Optional.empty();
        }

        // Now subtract padding only once
        double cWidth = size.width() - padding.horizontal();
        double cHeight = size.height() - padding.vertical();

        ContentBox contentBox = new ContentBox(cWidth, cHeight);
        log.debug("{} {}", e, contentBox);

        return Optional.of(contentBox);
    }

    public Optional<BoxSize> calculateBoxSize(Entity e) {
        log.debug("Starting calculation of box size for entity: {}", e);
        Size size;
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());

        if (e.has(Size.class)) {
            // Directly use Size (already margin-free)
            size = e.getComponent(Size.class).get();
        } else if (e.has(BoxSize.class)) {
            // Convert BoxSize -> Size by removing margins
            size = calculateSizeFromBoxSize(e);
        } else {
            return Optional.empty();
        }

        Margin margin = e.getComponent(Margin.class).orElse(Margin.zero());
        log.debug("{} {}", e, margin);

        double requiredWidth = size.width() + margin.horizontal();
        double requiredHeight = size.height() + margin.vertical();
        log.debug("{}  requiredWidth {}, requiredHeight {}", e, requiredWidth, requiredHeight);

        BoxSize box = e.getComponent(BoxSize.class).orElse(new BoxSize(requiredWidth, requiredHeight));

        double finalWidth = Math.max(requiredWidth, box.width());
        double finalHeight = Math.max(requiredHeight, box.height());

        var boxSize = new BoxSize(finalWidth, finalHeight);
        log.debug("{} {}", e, boxSize);
        return Optional.of(boxSize);
    }

    private Size calculateSizeFromBoxSize(Entity e) {
        if (!e.has(BoxSize.class) && !e.has(Size.class)) {
            log.error("❌ Critical error: Size component is missing for entity {}. Cannot proceed.", e);
            throw new SizeNotFoundException("All objects must have a Size. Object %s doesn't have one.".formatted(e));
        } else if (e.has(Size.class)) {
            log.debug("{} component already defined {}", e, e.getComponent(Size.class));
            return e.getComponent(Size.class).get();
        }
        var boxSize = e.getComponent(BoxSize.class).get();
        var margin = e.getComponent(Margin.class).orElse(Margin.zero());
        double w = boxSize.width() - margin.horizontal();
        double h = boxSize.height() - margin.vertical();
        log.debug("The Size has benn initialized from BoxSize ");
        return new Size(w, h);
    }

    public Optional<ComputedPosition> calculatePositionFromParent(Entity child, Entity parent, PdfDocument pdfDocument) {
        log.debug("Starting calculation of computed position for  {} from parent {}", child, parent);
        var computedX = 0.0d;
        var computedY = 0.0d;
        ComputedPosition computedPosition;
        //если нету родителя, значит мы принимаем даного ребенка как рут и калькулируем относитьльно его позиции и позиции страницы
        if (parent == null) {

            var anchor = child.getComponent(Anchor.class).orElse(Anchor.bottomLeft());
            var pageSize = pdfDocument.pageSize();


            computedPosition = alinePositionWithAnchor(child, anchor, new ContentBox(pageSize.width(), pageSize.height()));
            log.debug("Computed position has been created: {}", computedPosition);
            BoundingBox boundingBox = calculateBoundingBox(child, computedPosition);
            child.addComponent(boundingBox);
        } else {
            ContentBox contentBox = parent.getComponent(ContentBox.class).or(() ->
                    calculateContentBox(parent)
            ).orElseThrow(() -> new IllegalStateException("No ContentBox available for " + parent));

//            var boundinBox = parent.getComponent(BoundingBox.class).
            return Optional.empty();
        }

        return Optional.of(computedPosition);


    }

    private BoundingBox calculateBoundingBox(Entity entity, ComputedPosition positionWithMargin) {
        if (entity.has(BoundingBox.class)) {
            log.debug("{}  hase already {}", entity, entity.getComponent(BoundingBox.class));
            return entity.getComponent(BoundingBox.class).get();
        }
        var boundingBox = entity.getComponent(BoxSize.class)
                .or(() -> calculateBoxSize(entity))
                .orElseThrow(() -> new IllegalStateException("No BoxSize available for " + entity));
        entity.addComponentIfAbsent(boundingBox);
        return new BoundingBox(positionWithMargin.x(), positionWithMargin.y(), boundingBox.width(), boundingBox.height());
    }

    private ComputedPosition alinePositionWithAnchor(Entity child, Anchor anchor, ContentBox contentBox) {
        double computedY;
        double computedX;
        var position = child.getComponent(Position.class).orElse(Position.zero());
        var margin = child.getComponent(Margin.class).orElse(Margin.zero());
        var boxSize = child.getComponent(BoxSize.class)
                .or(() -> calculateBoxSize(child))
                .orElseThrow(() -> new IllegalStateException("No BoxSize available for " + child));
        child.addComponentIfAbsent(boxSize);
        computedX = switch (anchor.h()) {
            case LEFT -> position.x() + margin.left();
            case CENTER -> position.x() + (contentBox.innerW() - boxSize.width()) / 2.0;
            case RIGHT -> position.x() + contentBox.innerW() - boxSize.width();
        };
        computedY = switch (anchor.v()) {
            case TOP -> contentBox.innerH() - boxSize.height() - position.y();
            case MIDDLE -> ((contentBox.innerH() - boxSize.height()) / 2.0) - position.y();
            case BOTTOM -> position.y() + margin.bottom();

        };
        return new ComputedPosition(computedX, computedY);
    }

    /**
     * The method normalizeBoxSize adjusts the size of parent entities. For each parent, it inspects the dimensions (BoxSize) of all its direct children.
     * It then ensures the parent's BoxSize is large enough to encompass its own original size and the size of its largest child by updating it to the maximum width and maximum height found.
     * In simple terms: It makes a parent container at least as big as its biggest child.
     *
     * @param childrenByParents map already sorted by parent
     * @param pdfDocument       original document with entities
     */
    private void expandParentsBoxSize(Map<UUID, Set<UUID>> childrenByParents, PdfDocument pdfDocument) {
        log.info("LayoutSystem: normalizing box size");
        //TODO нужно имплементировать  другой алгоритм для имерения сонтент бокса если родитель это VBox или HBox должен учитываться то что спайсинг

        for (Map.Entry<UUID, Set<UUID>> parentUuid : childrenByParents.entrySet()) {

            var entityParentOpt = pdfDocument.getEntity(parentUuid.getKey());
            if (entityParentOpt.isEmpty()) {
                log.warn("LayoutSystem: hasn't find a Parent Entity by id {}", parentUuid.getKey());
                continue;
            }
            var entityParent = entityParentOpt.get();

            if (dontExpandIf(entityParent)) {
                log.debug("{} is restricted", entityParent);
                continue;
            }

            log.debug("Definition {} all child entities by given Set children uuid", entityParent);
            var entities = parentUuid.getValue()
                    .stream()
                    .map(pdfDocument::getEntity)
                    .map(Optional::get)
                    .collect(Collectors.toSet());

            expendBoxSizeByChildren(entityParent, entities);

        }
    }

    public void expendBoxSizeByChildren(Entity parent, Set<Entity> children) {
        parent.getComponent(ContentBox.class).ifPresent(contentBox -> {
            log.debug("{} {}", parent, contentBox);
            double requireContextWidth = contentBox.innerW();
            double requireContextHigth = contentBox.innerH();
            for (Entity childEntity : children) {
                Optional<BoxSize> childBoxSizeComponentOpt = childEntity.getComponent(BoxSize.class);
                if (childBoxSizeComponentOpt.isPresent()) {
                    var childBoxSize = childBoxSizeComponentOpt.get();
                    log.debug("{} {}", childEntity, childBoxSize);
                    requireContextWidth = Math.max(requireContextWidth, childBoxSize.width());
                    requireContextHigth = Math.max(requireContextHigth, childBoxSize.height());
                }

            }
            //update BoxSize if size is bigger then previous
            updateBoxSize(parent, contentBox, requireContextWidth, requireContextHigth);

        });
    }

    private boolean dontExpandIf(Entity entity) {
        //TODO Restrictions if we need to skip a some entitie and deny expend execution
        if (entity.has(Text.class)) {
            // Forbidden expend text Entitie
            return true;
        }
        if (entity.has(Box.class)) {
            return false;
        }
        return true;
    }

    private void dfsLayout(
            UUID id,
            UUID parentId,
            Map<UUID, Entity> entities,
            Map<UUID, Set<UUID>> childrenByParent,
            Map<UUID, Visit> visit
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

        double x = 0.0, y = 0.0;
        if (parentId != null) {
            Entity parent = entities.get(parentId);
            if (parent != null) {
//                x = calculateChildX(childEntity, parent);
//                y = calculateChildY(childEntity, parent);
            } else {
                // Родитель указан, но отсутствует → ведём себя как для корня
                log.warn("LayoutSystem: parent {} of {} not found — using root positioning (Position + Margin)", parentId, id);
                double localX = childEntity.getComponent(Position.class).map(Position::x).orElse(0.0);
                double localY = childEntity.getComponent(Position.class).map(Position::y).orElse(0.0);
                Margin cm = childEntity.getComponent(Margin.class).orElse(Margin.zero());
                x = localX + cm.left();
                y = localY + cm.top();
            }
        } else {
            // Корневой элемент: уважаем локальные Position и Margin
            double localX = childEntity.getComponent(Position.class).map(Position::x).orElse(0.0);
            double localY = childEntity.getComponent(Position.class).map(Position::y).orElse(0.0);
            Margin cm = childEntity.getComponent(Margin.class).orElse(Margin.zero());
            x = localX + cm.left();
            y = localY + cm.top();
        }

        childEntity.addComponent(new ComputedPosition(x, y));

        var tw = totalWidthOf(childEntity);
        var th = totalHeightOf(childEntity);
        if (tw.isPresent() && th.isPresent()) {
            // keep size; set BB at final position
            //TODO закментил добавление boundinBox
//            childEntity.addComponent(new BoundingBox(x, y, tw.getAsDouble(), th.getAsDouble()));
        }

        if (log.isDebugEnabled()) {
            String name = childEntity.getComponent(EntityName.class).map(EntityName::value).orElse(id.toString());
            log.debug("LayoutSystem: {} positioned at ({}, {})", name, x, y);
        }

        for (UUID childId : childrenByParent.getOrDefault(id, Collections.emptySet())) {
            dfsLayout(childId, id, entities, childrenByParent, visit);
        }

        visit.put(id, Visit.DONE);
    }

//    private double calculateChildX(Entity child, Entity parent) {
//        double parentX = parent.getComponent(ComputedPosition.class).map(ComputedPosition::x).orElse(0.0);
//        Padding pp = parent.getComponent(Padding.class).orElse(Padding.zero());
//        Margin cm = child.getComponent(Margin.class).orElse(Margin.zero());
//        double contentStartX = parentX + pp.left();
//
//        double localX = child.getComponent(Position.class).map(Position::x).orElse(0.0);
//        HAnchor h = child.getComponent(Anchor.class).map(Anchor::h)
//                .orElse(child.getComponent(Align.class).map(Align::h).orElse(HAnchor.LEFT));
//
//        double alignedX = contentStartX;
//        var cb = calculateContentBox(parent, child);
//        if (cb.isPresent() && h != HAnchor.LEFT) {
//            double availableW = cb.get().parentInnerW();
//            double childW = contentWidthOf(child).orElse(0.0);
//            alignedX = switch (h) {
//                case CENTER -> contentStartX + (availableW - childW) / 2.0;
//                case RIGHT -> contentStartX + Math.max(0, availableW - childW);
//                default -> contentStartX;
//            };
//        }
//        return alignedX + localX + cm.left();
//    }
//    // ------------------------------------------------------------
//    // Normalization
//
//    // ------------------------------------------------------------
//
//    private double calculateChildY(Entity child, Entity parent) {
//        double parentY = parent.getComponent(ComputedPosition.class).map(ComputedPosition::y).orElse(0.0);
//        Padding pp = parent.getComponent(Padding.class).orElse(Padding.zero());
//        Margin cm = child.getComponent(Margin.class).orElse(Margin.zero());
//        double contentStartY = parentY + pp.top();
//
//        double localY = child.getComponent(Position.class).map(Position::y).orElse(0.0);
//        VAnchor v = child.getComponent(Anchor.class).map(Anchor::v)
//                .orElse(child.getComponent(Align.class).map(Align::v).orElse(VAnchor.TOP));
//
//        double alignedY = contentStartY;
//        var cb = calculateContentBox(parent, child);
//        if (cb.isPresent() && v != VAnchor.TOP) {
//            double availableH = cb.get().parentInnerH();
//            double childH = contentHeightOf(child).orElse(0.0);
//            alignedY = switch (v) {
//                case MIDDLE -> contentStartY + (availableH - childH) / 2.0;
//                case BOTTOM -> contentStartY + Math.max(0, availableH - childH);
//                default -> contentStartY;
//            };
//        }
//        return alignedY + localY + cm.top();
//    }

    @Override
    public String toString() {
        return "LayoutSystem";
    }

    private enum Visit {UNSEEN, ACTIVE, DONE;}

}
