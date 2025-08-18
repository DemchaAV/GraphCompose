package com.demcha.system;

import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.BoundingBox;
import com.demcha.components.geometry.BoxSize;
import com.demcha.components.layout.ComputedPosition;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * <h1>LayoutSystem</h1>
 *
 * <p>
 * A top-down, deterministic DFS (depth-first search) layout system for an ECS-style PDF document model.
 * It computes absolute coordinates for each entity based on its local {@link Position}, {@link Margin},
 * and the parent's {@link ComputedPosition} and {@link Padding}. If an entity also has {@link BoxSize},
 * the system emits a corresponding {@link BoundingBox}.
 * </p>
 *
 * <h2>Coordinate Model</h2>
 * <p>
 * For an entity <em>child</em> with local position (x, y) and margin (ml, mt) inside a <em>parent</em>
 * with absolute position (PX, PY) and padding (pl, pt), the absolute coordinates are:
 * </p>
 * <pre>
 * absX = PX + pl + x + ml
 * absY = PY + pt + y + mt
 * </pre>
 * <p>
 * Note: PDFBox uses a bottom-left origin. If your logical coordinates are top-left based, ensure the conversion
 * is handled consistently at render time or with a dedicated component.
 * </p>
 *
 * <h2>Algorithm Overview</h2>
 * <ol>
 *   <li>Build a {@code parent -> children} index via {@link ParentComponent}.</li>
 *   <li>Detect <strong>roots</strong> (entities that are not children). Orphans (missing parents) are treated as roots with a warning.</li>
 *   <li>Sort roots and each children list (UUID order) for deterministic traversal.</li>
 *   <li>Run DFS from each root with <em>cycle detection</em> and <em>memoization</em> to compute {@link ComputedPosition} and optional {@link BoundingBox}.</li>
 * </ol>
 *
 * <h2>Complexity</h2>
 * <p>
 * Linear in the number of entities and relationships: O(N + E).
 * </p>
 *
 * <h2>Thread-safety</h2>
 * <p>
 * This system is not thread-safe by itself. Confinement to a single thread or external synchronization is required
 * if the underlying {@code PdfDocument} and entity maps are shared across threads.
 * </p>
 *
 * <h2>Required / Optional Components</h2>
 * <ul>
 *   <li><strong>Optional</strong>: {@link Position} (defaults to 0,0 if absent)</li>
 *   <li><strong>Optional</strong>: {@link Margin} (defaults to {@code Margin.zero()})</li>
 *   <li><strong>Optional</strong>: {@link Padding} on parent (defaults to 0 if absent)</li>
 *   <li><strong>Optional</strong>: {@link BoxSize} (emits {@link BoundingBox} when present)</li>
 *   <li><strong>Optional</strong>: {@link EntityName} (used for friendlier debug logs)</li>
 *   <li><strong>Optional</strong>: {@link ParentComponent} (absence implies the entity is a root)</li>
 * </ul>
 *
 * <h2>Failure Modes</h2>
 * <ul>
 *   <li>Cycle in the parent chain triggers {@link IllegalStateException} during DFS.</li>
 *   <li>Missing parent entity is tolerated: the child is treated as a root and a warning is logged.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * PdfDocument doc = new PdfDocument();
 * doc.addSystem(new LayoutSystem());
 *
 * // Create a root with padding & margin
 * UUID root = doc.createEntity();
 * doc.addComponent(root, new EntityName("Box"));
 * doc.addComponent(root, new BoxSize(500, 500));
 * doc.addComponent(root, new Padding(8, 8, 8, 8));
 * doc.addComponent(root, new Margin(5, 9, 8, 0));
 *
 * // Create a child positioned inside the root
 * UUID child = doc.createEntity();
 * doc.addComponent(child, new EntityName("Rectangle"));
 * doc.addComponent(child, new ParentComponent(root));
 * doc.addComponent(child, new Position(24, 24));
 * doc.addComponent(child, new Margin(5.5, 3, 0, 0));
 * doc.addComponent(child, new BoxSize(300, 90));
 *
 * // Run systems
 * doc.process();
 *
 * // Read computed layout
 * ComputedPosition cp = doc.getEntities().get(child)
 *     .get(ComputedPosition.class)
 *     .orElseThrow();
 * System.out.println(cp.x() + ", " + cp.y());
 * }
 * </pre>
 */
@Slf4j
public class LayoutSystem implements System {

    @Override
    public void process(PdfDocument pdfDocument) {
        log.info("LayoutSystem: processing");

        Map<UUID, Entity> entities = pdfDocument.getEntities();
        if (entities == null || entities.isEmpty()) {
            log.info("LayoutSystem: no entities to layout");
            return;
        }

        // 1) Build children index and collect all child IDs
        Map<UUID, List<UUID>> childrenByParent = new HashMap<>();
        Set<UUID> allChildren = new HashSet<>();

        for (Map.Entry<UUID, Entity> e : entities.entrySet()) {
            UUID id = e.getKey();
            Optional<ParentComponent> parentOpt = e.getValue().getComponent(ParentComponent.class);
            if (parentOpt.isPresent()) {
                UUID p = parentOpt.get().uuid();
                if (p != null) {
                    childrenByParent.computeIfAbsent(p, k -> new ArrayList<>()).add(id);
                    allChildren.add(id);
                }
            }
        }

        // 2) Roots = not children. Also include nodes referencing a missing parent (treat as roots, warn).
        List<UUID> roots = new ArrayList<>();
        for (UUID id : entities.keySet()) {
            boolean isChild = allChildren.contains(id);
            if (!isChild) {
                roots.add(id);
            }
        }

        // Add orphans (has parent not present in entities) as roots and warn once.
        for (Map.Entry<UUID, Entity> e : entities.entrySet()) {
            UUID id = e.getKey();
            Optional<ParentComponent> parentOpt = e.getValue().getComponent(ParentComponent.class);
            if (parentOpt.isPresent()) {
                UUID p = parentOpt.get().uuid();
                if (p != null && !entities.containsKey(p)) {
                    log.warn("LayoutSystem: entity {} references missing parent {} — treating as root", id, p);
                    if (!roots.contains(id)) roots.add(id);
                }
            }
        }

        // Deterministic order
        roots.sort(Comparator.naturalOrder());
        for (List<UUID> list : childrenByParent.values()) list.sort(Comparator.naturalOrder());

        // 3) DFS with cycle detection & memoization
        Map<UUID, Visit> visit = new HashMap<>();
        for (UUID id : entities.keySet()) visit.put(id, Visit.UNSEEN);

        for (UUID root : roots) {
            dfsLayout(root, null, entities, childrenByParent, visit);
        }

        log.info("LayoutSystem: layout complete (nodes: {})", entities.size());
    }

    private void dfsLayout(
            UUID id,
            UUID parentId,
            Map<UUID, Entity> entities,
            Map<UUID, List<UUID>> childrenByParent,
            Map<UUID, Visit> visit
    ) {
        Visit st = visit.getOrDefault(id, Visit.UNSEEN);
        if (st == Visit.DONE) return; // memoized
        if (st == Visit.ACTIVE) {
            throw new IllegalStateException("LayoutSystem: cycle detected at entity " + id);
        }
        visit.put(id, Visit.ACTIVE);

        Entity comps = entities.get(id);
        if (comps == null) {
            log.warn("LayoutSystem: entity {} has no component map — skipping", id);
            visit.put(id, Visit.DONE);
            return;
        }

        // Parent computed position and padding
        double parentAbsX = 0.0, parentAbsY = 0.0;
        double parentPadLeft = 0.0, parentPadTop = 0.0;
        if (parentId != null) {
            Entity pComps = entities.get(parentId);
            if (pComps == null) {
                log.warn("LayoutSystem: parent {} of {} missing — using (0,0)", parentId, id);
            } else {
                var pcp = pComps.getComponent(ComputedPosition.class);
                if (pcp.isPresent()) {
                    parentAbsX = pcp.get().x();
                    parentAbsY = pcp.get().y();
                }
                var pad = pComps.getComponent(Padding.class);
                if (pad.isPresent()) {
                    parentPadLeft = pad.get().left();
                    parentPadTop = pad.get().top();
                } else {
                    log.trace("LayoutSystem: {} has no Padding", pComps);
                }
            }
        }

        // Local components
        var posOpt = comps.getComponent(Position.class);
        double localX = posOpt.map(Position::x).orElse(0.0);
        double localY = posOpt.map(Position::y).orElse(0.0);
        Margin margin = comps.getComponent(Margin.class).orElse(Margin.zero());



        double absX = parentAbsX + parentPadLeft + localX + margin.left();
        double absY = parentAbsY + parentPadTop + localY + margin.top();

        // Write back computed results
        comps.addComponent(new ComputedPosition(absX, absY));
        comps.getComponent(BoxSize.class).ifPresent(sz ->
                comps.addComponent(new BoundingBox(absX, absY, sz.width(), sz.height()))
        );

        if (log.isDebugEnabled()) {
            comps.getComponent(EntityName.class).ifPresentOrElse(
                    name -> log.debug("LayoutSystem: {} → (x={}, y={})", comps, absX, absY),
                    () -> log.debug("LayoutSystem: {} → (x={}, y={})", id, absX, absY)
            );
        }

        // Recurse into children
        for (UUID child : childrenByParent.getOrDefault(id, Collections.emptyList())) {
            dfsLayout(child, id, entities, childrenByParent, visit);
        }

        visit.put(id, Visit.DONE);
    }

    @Override
    public String toString() {
        return "LayoutSystem";
    }

    private enum Visit {UNSEEN, ACTIVE, DONE}
}