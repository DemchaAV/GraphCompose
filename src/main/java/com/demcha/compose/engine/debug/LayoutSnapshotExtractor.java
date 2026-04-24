package com.demcha.compose.engine.debug;

import com.demcha.compose.engine.components.core.Component;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.core.EntityName;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.Layer;
import com.demcha.compose.engine.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.Canvas;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.core.LayoutTraversalContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Extracts deterministic, renderer-agnostic layout snapshots from resolved ECS state.
 *
 * <p>The extractor walks the entity tree in deterministic depth-first order,
 * reads post-layout components such as {@link ComputedPosition} and
 * {@link Placement}, and projects them into stable snapshot records that are
 * suitable for regression testing and debugging.</p>
 *
 * <p>The extractor is intentionally strict: if required post-layout components
 * are missing after the layout pass, extraction fails fast with a descriptive
 * error rather than silently producing an incomplete snapshot.</p>
 */
public final class LayoutSnapshotExtractor {
    public static final String FORMAT_VERSION = "1.0";
    private static final String PATH_SEPARATOR = "/";
    private static final Pattern GENERATED_ENTITY_NAME = Pattern.compile("^[A-Za-z0-9$]+Builder__[0-9a-fA-F]{5}$");

    private LayoutSnapshotExtractor() {
    }

    /**
     * Builds a deterministic snapshot from the current entity manager state.
     *
     * <p>This method expects layout resolution and pagination to have already
     * happened. It does not run rendering and does not mutate renderer-owned
     * output state.</p>
     *
     * @param entityManager entity graph and resolved post-layout components
     * @param canvas resolved canvas used during layout
     * @return snapshot of the current resolved layout tree
     */
    public static LayoutSnapshot extract(EntityManager entityManager, Canvas canvas) {
        Objects.requireNonNull(entityManager, "entityManager");
        Objects.requireNonNull(canvas, "canvas");

        Map<UUID, Entity> entities = entityManager.getEntities();
        if (entities == null || entities.isEmpty()) {
            return new LayoutSnapshot(
                    FORMAT_VERSION,
                    snapshotCanvas(canvas),
                    0,
                    List.of());
        }

        // Resolved-order features must always traverse through LayoutTraversalContext
        // so snapshots, pagination, and renderer ordering agree on the same tree.
        LayoutTraversalContext traversalContext = LayoutTraversalContext.from(entityManager);
        List<Entity> roots = resolveRoots(entities, traversalContext);
        List<LayoutNodeSnapshot> nodes = new ArrayList<>(entities.size());

        for (int rootIndex = 0; rootIndex < roots.size(); rootIndex++) {
            visitNode(
                    entityManager,
                    entities,
                    traversalContext,
                    roots.get(rootIndex),
                    null,
                    rootIndex,
                    nodes,
                    new HashSet<>());
        }

        int totalPages = nodes.stream()
                .mapToInt(node -> Math.max(node.startPage(), node.endPage()) + 1)
                .max()
                .orElse(0);

        return new LayoutSnapshot(
                FORMAT_VERSION,
                snapshotCanvas(canvas),
                totalPages,
                List.copyOf(nodes));
    }

    static double normalize(double value) {
        if (Math.abs(value) < 0.0005d) {
            return 0.0d;
        }
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static void visitNode(EntityManager entityManager,
                                  Map<UUID, Entity> entities,
                                  LayoutTraversalContext traversalContext,
                                  Entity entity,
                                  String parentPath,
                                  int childIndex,
                                  List<LayoutNodeSnapshot> snapshots,
                                  Set<UUID> activePath) {
        if (!activePath.add(entity.getUuid())) {
            throw new IllegalStateException("Cycle detected while extracting layout snapshot for entity " + describeEntity(entity));
        }

        String segment = nodeSegment(entity, childIndex);
        String path = parentPath == null ? segment : parentPath + PATH_SEPARATOR + segment;

        snapshots.add(toNodeSnapshot(entityManager, entity, parentPath, path, childIndex));

        List<UUID> childIds = traversalContext.childrenByParent().getOrDefault(entity.getUuid(), List.of());
        for (int index = 0; index < childIds.size(); index++) {
            UUID childId = childIds.get(index);
            Entity child = entities.get(childId);
            if (child == null) {
                throw new IllegalStateException("Missing child entity " + childId + " referenced from " + describeEntity(entity));
            }
            visitNode(entityManager, entities, traversalContext, child, path, index, snapshots, activePath);
        }

        activePath.remove(entity.getUuid());
    }

    private static LayoutNodeSnapshot toNodeSnapshot(EntityManager entityManager,
                                                     Entity entity,
                                                     String parentPath,
                                                     String path,
                                                     int childIndex) {
        String label = path + " (" + describeEntity(entity) + ")";
        ComputedPosition computedPosition = requireComponent(entity, ComputedPosition.class, label);
        Placement placement = requireComponent(entity, Placement.class, label);
        ContentSize contentSize = requireComponent(entity, ContentSize.class, label);
        int layer = requireComponent(entity, Layer.class, label).value();
        int depth = entityManager.getDepthById()
                .getOrDefault(entity.getUuid(), layer);

        return new LayoutNodeSnapshot(
                path,
                semanticEntityName(entity),
                entityKind(entity),
                parentPath,
                childIndex,
                depth,
                layer,
                normalize(computedPosition.x()),
                normalize(computedPosition.y()),
                normalize(placement.x()),
                normalize(placement.y()),
                normalize(placement.width()),
                normalize(placement.height()),
                placement.startPage(),
                placement.endPage(),
                normalize(contentSize.width()),
                normalize(contentSize.height()),
                LayoutInsetsSnapshot.from(entity.getComponent(Margin.class).orElse(Margin.zero())),
                LayoutInsetsSnapshot.from(entity.getComponent(Padding.class).orElse(Padding.zero())));
    }

    private static LayoutCanvasSnapshot snapshotCanvas(Canvas canvas) {
        return new LayoutCanvasSnapshot(
                normalize(canvas.width()),
                normalize(canvas.height()),
                normalize(canvas.innerWidth()),
                normalize(canvas.innerHeigh()),
                LayoutInsetsSnapshot.from(canvas.margin()));
    }

    private static List<Entity> resolveRoots(Map<UUID, Entity> entities, LayoutTraversalContext traversalContext) {
        Comparator<Entity> rootComparator = Comparator
                .comparingInt(LayoutSnapshotExtractor::startPage)
                .thenComparing(LayoutSnapshotExtractor::topToBottomY)
                .thenComparing(LayoutSnapshotExtractor::leftToRightX)
                .thenComparing(LayoutSnapshotExtractor::entitySortKey);

        return traversalContext.roots().stream()
                .map(entities::get)
                .filter(Objects::nonNull)
                .sorted(rootComparator)
                .collect(Collectors.toList());
    }

    private static int startPage(Entity entity) {
        return requireComponent(entity, Placement.class, describeEntity(entity)).startPage();
    }

    private static double topToBottomY(Entity entity) {
        return -requireComponent(entity, Placement.class, describeEntity(entity)).y();
    }

    private static double leftToRightX(Entity entity) {
        return requireComponent(entity, Placement.class, describeEntity(entity)).x();
    }

    private static String entitySortKey(Entity entity) {
        String semanticName = semanticEntityName(entity);
        return semanticName == null ? entityKind(entity) : semanticName;
    }

    private static String nodeSegment(Entity entity, int childIndex) {
        String base = semanticEntityName(entity);
        if (base != null) {
            base = sanitizeSegment(base);
        }
        if (base == null || base.isBlank()) {
            base = entityKind(entity);
        }

        return base + "[" + childIndex + "]";
    }

    private static String sanitizeSegment(String value) {
        return value.trim()
                .replace('\\', '_')
                .replace('/', '_');
    }

    private static String entityKind(Entity entity) {
        if (entity.getRender() != null) {
            return entity.getRender().getClass().getSimpleName();
        }
        return entity.getClass().getSimpleName();
    }

    private static String semanticEntityName(Entity entity) {
        return entity.getComponent(EntityName.class)
                .map(EntityName::value)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> !GENERATED_ENTITY_NAME.matcher(value).matches())
                .orElse(null);
    }

    private static String describeEntity(Entity entity) {
        String semanticName = semanticEntityName(entity);
        return semanticName != null
                ? semanticName
                : entityKind(entity) + "#" + entity.getUuid().toString().substring(0, 8);
    }

    private static <T extends Component> T requireComponent(Entity entity, Class<T> componentType, String label) {
        return entity.getComponent(componentType)
                .orElseThrow(() -> new NoSuchElementException("Layout snapshot requires " + componentType.getSimpleName()
                        + " for " + label));
    }
}
