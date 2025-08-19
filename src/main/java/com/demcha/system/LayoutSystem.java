package com.demcha.system;

import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.BoundingBox;
import com.demcha.components.geometry.BoxSize;
import com.demcha.components.layout.*;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

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
    // Size helpers (NO reflection; relies on Rectangle API)
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

    /** Content width priority: BoxSize → Rectangle → (BoundingBox − margin). */
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

    /** Content height priority: BoxSize → Rectangle → (BoundingBox − margin). */
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

    /** Total width (rendered): BoundingBox → (Rectangle + margin) → (BoxSize + margin). */
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

    /** Total height (rendered): BoundingBox → (Rectangle + margin) → (BoxSize + margin). */
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
    // Normalization
    // ------------------------------------------------------------

    /**
     * Ensures each entity has a consistent BoxSize and BoundingBox.
     * Preference order: BoxSize (truthy) → Rectangle → BoundingBox.
     * If both BoxSize and BoundingBox exist but disagree, we keep BoxSize as source of truth and recompute BoundingBox.
     */
    private void normalizeEntitySizes(Entity e) {
        Margin m = e.getComponent(Margin.class).orElse(Margin.zero());
        var boxOpt = e.getComponent(BoxSize.class);
        var bbOpt = e.getComponent(BoundingBox.class);

        // Fill from Rectangle when BoxSize is missing
        if (boxOpt.isEmpty()) {
            var rw = rectWidthOf(e);
            var rh = rectHeightOf(e);
            if (rw.isPresent() && rh.isPresent()) {
                e.addComponent(new BoxSize(rw.getAsDouble(), rh.getAsDouble()));
                boxOpt = e.getComponent(BoxSize.class);
            }
        }

        // If BoundingBox is missing but we have BoxSize → derive BB = content + margin (padding accounted at parent level)
        if (bbOpt.isEmpty() && boxOpt.isPresent()) {
            var box = boxOpt.get();
            e.addComponent(new BoundingBox(0, 0, box.width() + m.horizontal(), box.height() + m.vertical()));
            bbOpt = e.getComponent(BoundingBox.class);
        }

        // If BoxSize is missing but BB present → derive content = BB − margin
        if (boxOpt.isEmpty() && bbOpt.isPresent()) {
            var bb = bbOpt.get();
            e.addComponent(new BoxSize(Math.max(0, bb.width() - m.horizontal()), Math.max(0, bb.height() - m.vertical())));
            boxOpt = e.getComponent(BoxSize.class);
        }

        // Reconcile mismatch (prefer BoxSize as truthy; recompute BB accordingly)
        if (boxOpt.isPresent() && bbOpt.isPresent()) {
            var box = boxOpt.get();
            var bb = bbOpt.get();
            double expectedW = box.width() + m.horizontal();
            double expectedH = box.height() + m.vertical();
            if (Math.abs(bb.width() - expectedW) > 1e-6 || Math.abs(bb.height() - expectedH) > 1e-6) {
                log.debug("LayoutSystem: reconciling BoundingBox to match BoxSize for entity {}", e);
                e.addComponent(new BoundingBox(bb.x(), bb.y(), expectedW, expectedH));
            }
        }
    }

    // ------------------------------------------------------------
    // Content box (parent inner area minus padding)
    // ------------------------------------------------------------

    private Optional<ContentBox> calculateContentBox(Entity parent, Entity child) {
        Padding p = parent.getComponent(Padding.class).orElse(Padding.zero());
        var pw = contentWidthOf(parent);
        var ph = contentHeightOf(parent);

        if (pw.isEmpty() || ph.isEmpty()) {
            boolean wantsAlign = child.getComponent(Anchor.class).isPresent() || child.getComponent(Align.class).isPresent();
            if (wantsAlign) {
                String childName = child.getComponent(EntityName.class).map(EntityName::value).orElse(child.getId().toString());
                String parentName = parent.getComponent(EntityName.class).map(EntityName::value).orElse(parent.getId().toString());
                log.warn("LayoutSystem: alignment requested for child [{}] but parent [{}] has no content size; alignment skipped.", childName, parentName);
            }
            return Optional.empty();
        }

        double innerW = Math.max(0, pw.getAsDouble() - p.horizontal());
        double innerH = Math.max(0, ph.getAsDouble() - p.vertical());
        return Optional.of(new ContentBox(innerW, innerH));
    }

    // ------------------------------------------------------------
    // Main process
    // ------------------------------------------------------------

    @Override
    public void process(PdfDocument pdfDocument) {
        log.info("LayoutSystem: processing");

        Map<UUID, Entity> entities = pdfDocument.getEntities();
        if (entities == null || entities.isEmpty()) {
            log.info("LayoutSystem: no entities to layout");
            return;
        }

        // 1) Normalize sizes
        for (Entity e : entities.values()) normalizeEntitySizes(e);

        // 2) Build parent → children map
        Map<UUID, List<UUID>> childrenByParent = new HashMap<>();
        Set<UUID> allChildren = new HashSet<>();
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

        // 3) Roots (no parent or parent missing)
        List<UUID> roots = new ArrayList<>();
        for (UUID id : entities.keySet()) if (!allChildren.contains(id)) roots.add(id);
        for (Map.Entry<UUID, Entity> en : entities.entrySet()) {
            UUID id = en.getKey();
            var pc = en.getValue().getComponent(ParentComponent.class);
            if (pc.isPresent()) {
                UUID pid = pc.get().uuid();
                if (pid != null && !entities.containsKey(pid) && !roots.contains(id)) {
                    log.warn("LayoutSystem: entity {} references missing parent {} — treating as root", id, pid);
                    roots.add(id);
                }
            }
        }

        // 4) Deterministic order: by EntityName (if present), then UUID
        roots.sort((a, b) -> compareByNameThenId(entities.get(a), entities.get(b)));
        childrenByParent.values().forEach(list -> list.sort((a, b) -> compareByNameThenId(entities.get(a), entities.get(b))));

        // 5) DFS with cycle detection
        Map<UUID, Visit> visit = new HashMap<>();
        entities.keySet().forEach(id -> visit.put(id, Visit.UNSEEN));
        for (UUID root : roots) dfsLayout(root, null, entities, childrenByParent, visit);

        log.info("LayoutSystem: layout complete (nodes: {})", entities.size());
    }

    private static int compareByNameThenId(Entity a, Entity b) {
        String an = a.getComponent(EntityName.class).map(EntityName::value).orElse("");
        String bn = b.getComponent(EntityName.class).map(EntityName::value).orElse("");
        int byName = an.compareToIgnoreCase(bn);
        if (byName != 0) return byName;
        return a.getId().compareTo(b.getId());
    }

    private void dfsLayout(
            UUID id,
            UUID parentId,
            Map<UUID, Entity> entities,
            Map<UUID, List<UUID>> childrenByParent,
            Map<UUID, Visit> visit
    ) {
        Visit st = visit.getOrDefault(id, Visit.UNSEEN);
        if (st == Visit.DONE) return;
        if (st == Visit.ACTIVE) throw new IllegalStateException("LayoutSystem: cycle detected at entity " + id);
        visit.put(id, Visit.ACTIVE);

        Entity e = entities.get(id);
        if (e == null) {
            log.warn("LayoutSystem: entity {} not found — skipping", id);
            visit.put(id, Visit.DONE);
            return;
        }

        double x = 0.0, y = 0.0;
        if (parentId != null) {
            Entity parent = entities.get(parentId);
            if (parent != null) {
                x = calculateChildX(e, parent);
                y = calculateChildY(e, parent);
            } else {
                log.warn("LayoutSystem: parent {} of {} not found — using (0,0)", parentId, id);
            }
        }

        e.addComponent(new ComputedPosition(x, y));

        var tw = totalWidthOf(e);
        var th = totalHeightOf(e);
        if (tw.isPresent() && th.isPresent()) {
            // keep size; set BB at final position
            e.addComponent(new BoundingBox(x, y, tw.getAsDouble(), th.getAsDouble()));
        }

        if (log.isDebugEnabled()) {
            String name = e.getComponent(EntityName.class).map(EntityName::value).orElse(id.toString());
            log.debug("LayoutSystem: {} positioned at ({}, {})", name, x, y);
        }

        for (UUID childId : childrenByParent.getOrDefault(id, Collections.emptyList())) {
            dfsLayout(childId, id, entities, childrenByParent, visit);
        }

        visit.put(id, Visit.DONE);
    }

    private double calculateChildX(Entity child, Entity parent) {
        double parentX = parent.getComponent(ComputedPosition.class).map(ComputedPosition::x).orElse(0.0);
        Padding pp = parent.getComponent(Padding.class).orElse(Padding.zero());
        Margin cm = child.getComponent(Margin.class).orElse(Margin.zero());
        double contentStartX = parentX + pp.left();

        double localX = child.getComponent(Position.class).map(Position::x).orElse(0.0);
        HAnchor h = child.getComponent(Anchor.class).map(Anchor::h)
                .orElse(child.getComponent(Align.class).map(Align::h).orElse(HAnchor.LEFT));

        double alignedX = contentStartX;
        var cb = calculateContentBox(parent, child);
        if (cb.isPresent() && h != HAnchor.LEFT) {
            double availableW = cb.get().parentInnerW();
            double childW = contentWidthOf(child).orElse(0.0);
            alignedX = switch (h) {
                case CENTER -> contentStartX + (availableW - childW) / 2.0;
                case RIGHT -> contentStartX + Math.max(0, availableW - childW);
                default -> contentStartX;
            };
        }
        return alignedX + localX + cm.left();
    }

    private double calculateChildY(Entity child, Entity parent) {
        double parentY = parent.getComponent(ComputedPosition.class).map(ComputedPosition::y).orElse(0.0);
        Padding pp = parent.getComponent(Padding.class).orElse(Padding.zero());
        Margin cm = child.getComponent(Margin.class).orElse(Margin.zero());
        double contentStartY = parentY + pp.top();

        double localY = child.getComponent(Position.class).map(Position::y).orElse(0.0);
        VAnchor v = child.getComponent(Anchor.class).map(Anchor::v)
                .orElse(child.getComponent(Align.class).map(Align::v).orElse(VAnchor.TOP));

        double alignedY = contentStartY;
        var cb = calculateContentBox(parent, child);
        if (cb.isPresent() && v != VAnchor.TOP) {
            double availableH = cb.get().parentInnerH();
            double childH = contentHeightOf(child).orElse(0.0);
            alignedY = switch (v) {
                case MIDDLE -> contentStartY + (availableH - childH) / 2.0;
                case BOTTOM -> contentStartY + Math.max(0, availableH - childH);
                default -> contentStartY;
            };
        }
        return alignedY + localY + cm.top();
    }

    @Override
    public String toString() { return "LayoutSystem"; }

    private enum Visit { UNSEEN, ACTIVE, DONE }

    private record ContentBox(double parentInnerW, double parentInnerH) {}
}
