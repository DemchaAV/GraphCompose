package com.demcha.compose.engine.components.core;

import com.demcha.compose.engine.components.geometry.EntityBounds;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.Render;
import com.demcha.compose.engine.pagination.Offset;
import com.demcha.compose.engine.pagination.ParentContainerUpdater;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Core runtime node in the GraphCompose entity-component model.
 * <p>
 * An {@code Entity} is a lightweight container for components such as text,
 * style, size, layout metadata, and render markers. Builders create entities
 * and
 * attach those components; systems later read and enrich the same entity as the
 * document moves through layout, pagination, and rendering.
 * </p>
 *
 * <p>
 * In other words, builders describe intent on the entity, while systems turn
 * that intent into resolved geometry and final output.
 * </p>
 */
@Slf4j
@EqualsAndHashCode
public final class Entity {
    @Getter
    private final UUID uuid;
    private final Map<Class<? extends Component>, Component> comps = new LinkedHashMap<>();
    @Getter
    private final List<UUID> children = new ArrayList<>();
    private EntityName name;
    @Getter
    private Render render;
    @Setter
    @Getter
    private boolean guideLines;

    /**
     * Creates a new entity with a fresh runtime UUID.
     */
    public Entity() {
        UUID uuid = UUID.randomUUID();
        log.debug("Creating entity {}", uuid);
        this.uuid = uuid;
    }

    /**
     * Creates a shallow component copy of another entity.
     *
     * <p>
     * The new entity gets its own UUID while reusing the source entity's current
     * component references as the initial snapshot.
     * </p>
     *
     * @param entity source entity to clone from
     * @return new entity with copied component references
     */
    public static Entity createFrom(Entity entity) {
        log.debug("Creating a copy of entity {}", entity);
        var newEntity = new Entity();
        var values = new HashSet<>(entity.comps.values());
        newEntity.populate(values);
        return newEntity;
    }

    /**
     * Returns the current entity name.
     *
     * @return configured name value
     */
    public String name() {
        return name.value();
    }

    /**
     * Adds a component only when the same concrete component type is not already
     * present.
     *
     * @param c component to add if absent
     * @param <T> component type
     * @return this entity for fluent chaining
     */
    public <T extends Component> Entity addComponentIfAbsent(T c) {
        if (c == null) {
            log.warn("addComponentIfAbsent: component is null");
            return this;
        }
        Class<? extends Component> key = c.getClass().asSubclass(Component.class);
        Component prev = comps.putIfAbsent(key, c);
        if (prev != null) {
            if (log.isDebugEnabled()) {
                log.debug("addComponentIfAbsent: component {} already exists on {}", key.getName(), this);
            }
            return this;
        }
        if (c instanceof EntityName en)
            this.name = en;
        if (log.isDebugEnabled()) {
            log.debug("Added absent component {} to {}", key.getSimpleName(), this);
        }
        return this;
    }

    /**
     * Adds or replaces a component by its concrete runtime class.
     *
     * <p>
     * When the component is an {@link EntityName}, the cached entity name is
     * updated. When it implements {@link Render}, the entity also caches it for
     * fast render checks and dispatch.
     * </p>
     *
     * @param c component to add
     * @param <T> component type
     * @return this entity for fluent chaining
     * @throws IllegalStateException if a second render component is attached
     *                               through the strict add path
     */
    public <T extends Component> Entity addComponent(@NonNull T c) {
        Class<? extends Component> key = c.getClass().asSubclass(Component.class);
        if (log.isDebugEnabled()) {
            log.debug("Adding component {} to {}", c, this);
        }
        Component prev = comps.put(key, c);
        if (c instanceof EntityName en)
            this.name = en;
        if (c instanceof Render) {
            if (render == null) {
                this.render = (Render) c;
            } else {
                log.warn("Render Component Already signed in {}", this);
                throw new IllegalStateException("%s Render component already assigned in %s".formatted(c, this.render));
            }

        }
        debugPutMethod(c, prev, "addComponent");
        return this;
    }

    private <T extends Component> void debugPutMethod(@NotNull T c, Component prev, String methodName) {
        if (log.isDebugEnabled()) {
            if (prev == null) {
                log.debug("Added component {} to {} through method {}", c, this, methodName);
            } else {
                log.debug("Replaced component {} on {} through method {}", c, this, methodName);
            }
        }
    }

    /**
     * Adds or replaces a component by its concrete runtime class, including
     * overwriting an existing cached render component when necessary.
     *
     * @param c component to add
     * @param <T> component type
     * @return this entity for fluent chaining
     */
    public <T extends Component> Entity forceAddComponent(@NonNull T c) {
        Class<? extends Component> key = c.getClass().asSubclass(Component.class);
        Component prev = comps.put(key, c);
        if (c instanceof EntityName en)
            this.name = en;
        if (c instanceof Render) {
            if (render == null) {
                this.render = (Render) c;
            } else {
                log.warn("Rendering Component Already signed in {}", this);
                this.render = (Render) c;
            }

        }
        debugPutMethod(c, prev, "forceAddComponent");
        return this;
    }

    /**
     * Looks up a component by its exact concrete class key.
     *
     * @param type concrete component class
     * @param <T> component type
     * @return the component wrapped in an {@link Optional}, or empty when absent
     */
    public <T extends Component> Optional<T> getComponent(Class<T> type) {
        T value = type.cast(comps.get(type));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * Looks up a component and fails fast when the entity does not have it.
     *
     * @param type concrete component class
     * @param <T> component type
     * @return resolved component
     * @throws NoSuchElementException when the component is missing
     */
    public <T extends Component> T require(Class<T> type) {
        T value = type.cast(comps.get(type));
        if (value != null) {
            return value;
        }
        log.error("No component found for type {} for entity [{}]", type.getName(), uuid);
        throw new NoSuchElementException("Missing " + type.getName() + " for " + uuid);
    }

    /**
     * Checks whether any attached component type is assignable to the given base
     * class.
     *
     * @param baseClass base class or marker interface to test
     * @return {@code true} when at least one attached component matches by
     *         assignability
     */
    public boolean hasAssignable(Class<?> baseClass) {
        return comps.keySet().stream()
                .anyMatch(c -> baseClass.isAssignableFrom(c));
    }

    /**
     * Checks whether the entity contains a component of the exact given concrete
     * type.
     *
     * @param type component class to check
     * @return {@code true} when the component is present
     */
    public boolean has(Class<? extends Component> type) {
        if (log.isDebugEnabled()) {
            log.debug("Checking component {} {}", type, this);
        }
        return comps.containsKey(type);
    }

    /**
     * Convenience overload that checks the runtime type of the provided component
     * instance.
     *
     * @param component component whose runtime type should be checked
     * @param <T> component type
     * @return {@code true} when a component of that concrete type is attached
     */
    public <T extends Component> boolean has(T component) {
        var type = component.getClass();
        return has(type);
    }

    /**
     * Returns whether the entity currently has a cached render marker.
     *
     * <p>
     * This is intentionally O(1) and does not rescan the component map.
     * </p>
     *
     * @return {@code true} when a render component is attached
     */
    public boolean hasRender() {
        return render != null;
    }

    /**
     * Compatibility wrapper for entity top-bound calculation.
     *
     * @return top outer line including top margin
     * @deprecated use {@link EntityBounds#topLine(Entity)} directly in new code
     */
    @Deprecated
    public double boundingTopLine() {
        return EntityBounds.topLine(this);
    }

    /**
     * Compatibility wrapper for entity bottom-bound calculation.
     *
     * @return bottom outer line including bottom margin
     * @deprecated use {@link EntityBounds#bottomLine(Entity)} directly in new code
     */
    @Deprecated
    public double boundingBottomLine() {
        return EntityBounds.bottomLine(this);
    }

    /**
     * Compatibility wrapper for entity right-bound calculation.
     *
     * @return right outer line including right margin
     * @deprecated use {@link EntityBounds#rightLine(Entity)} directly in new code
     */
    @Deprecated
    public double boundingRightLine() {
        return EntityBounds.rightLine(this);
    }

    /**
     * Compatibility wrapper for entity left-bound calculation.
     *
     * @return left outer line including left margin
     * @deprecated use {@link EntityBounds#leftLine(Entity)} directly in new code
     */
    @Deprecated
    public double boundingLeftLine() {
        return EntityBounds.leftLine(this);
    }

    /**
     * Populates the entity with the provided component set.
     *
     * @param components components to attach
     * @param <T> component type
     * @return this entity for fluent chaining
     */
    public <T extends Component> Entity populate(Set<? extends Component> components) {
        if (log.isDebugEnabled()) {
            log.debug("Creating and populating entity");
            log.debug("Populating entity UUID [{}] with\nComponents: {}", this, components);
        }
        for (Component component : components) {
            addComponent(component);
        }
        if (log.isDebugEnabled()) {
            log.debug("Created and populated entity {}", this);
        }
        return this;

    }

    /**
     * Returns a read-only view of all components currently attached to the entity.
     *
     * @return an unmodifiable component map keyed by concrete component class
     */
    public Map<Class<? extends Component>, Component> view() {
        if (log.isDebugEnabled()) {
            log.debug("Viewing component {} {}", uuid, this);
        }
        return Collections.unmodifiableMap(comps);
    }

    /**
     * Removes a component by its exact concrete class key.
     *
     * @param type component class to remove
     * @return {@code true} when a component was removed
     */
    public boolean remove(Class<? extends Component> type) {
        Component removed = comps.remove(type);
        if (removed == null) {
            return false;
        }
        if (removed == render) {
            render = null;
        }
        if (removed == name) {
            name = null;
        }
        return true;
    }

    private String printChildren(EntityManager manager) {
        StringBuilder childrenInfo = new StringBuilder();
        manager.getSetEntitiesFromUuids(new HashSet<>(children)).forEach(entity -> {
            childrenInfo.append(entity.toString()).append("\n");
        });
        return childrenInfo.toString();

    }

    /**
     * Compatibility wrapper that propagates a child offset to the parent container.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offset propagated offset
     * @return {@code true} when a parent update was applied
     * @deprecated use {@link ParentContainerUpdater#updateParentContainer(Entity, EntityManager, Offset)}
     *             directly in new code
     */
    @Deprecated
    public boolean updateParentContainer(EntityManager manager, Offset offset) {
        return ParentContainerUpdater.updateParentContainer(this, manager, offset);
    }

    /**
     * Compatibility wrapper that propagates a vertical delta to the parent
     * container chain.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offsetY vertical delta to propagate
     * @return {@code true} when a parent update was applied
     * @deprecated use
     *             {@link ParentContainerUpdater#updateParentContainer(Entity, EntityManager, double)}
     *             directly in new code
     */
    @Deprecated
    public boolean updateParentContainer(EntityManager manager, double offsetY) {
        return ParentContainerUpdater.updateParentContainer(this, manager, offsetY);
    }

    /**
     * Compatibility wrapper that propagates a size-only delta to the parent
     * container chain.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta to propagate
     * @return {@code true} when a parent size update was applied
     * @deprecated use
     *             {@link ParentContainerUpdater#updateParentContainerSize(Entity, EntityManager, double)}
     *             directly in new code
     */
    @Deprecated
    public boolean updateParentContainerSize(EntityManager manager, double offsetY) {
        return ParentContainerUpdater.updateParentContainerSize(this, manager, offsetY);
    }

    /**
     * Compatibility wrapper that resizes and, when needed, repositions the target
     * entity before propagating the change upward.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta or upward shift delta
     * @param entity entity to mutate
     * @return {@code true} when an update was applied
     * @deprecated use
     *             {@link ParentContainerUpdater#updateEntitySizeAndPosition(EntityManager, double, Entity)}
     *             directly in new code
     */
    @Deprecated
    public boolean updateEntitySizeAndPosition(EntityManager manager, double offsetY, @NonNull Entity entity) {
        return ParentContainerUpdater.updateEntitySizeAndPosition(manager, offsetY, entity);
    }

    /**
     * Compatibility wrapper that resizes the target entity and propagates the size
     * change upward.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta to apply
     * @param entity entity to resize
     * @return {@code true} when an update was applied
     * @deprecated use {@link ParentContainerUpdater#updateEntitySize(EntityManager, double, Entity)}
     *             directly in new code
     */
    @Deprecated
    public boolean updateEntitySize(EntityManager manager, double offsetY, @NonNull Entity entity) {
        return ParentContainerUpdater.updateEntitySize(manager, offsetY, entity);
    }

    /**
     * Compatibility wrapper that resizes the current entity and propagates the
     * size-only change upward.
     *
     * @param manager entity manager used to resolve parent entities
     * @param offsetY height delta to apply
     * @return {@code true} when an update was applied
     * @deprecated use
     *             {@link ParentContainerUpdater#updateCurrentEntitySize(Entity, EntityManager, double)}
     *             directly in new code
     */
    @Deprecated
    public boolean updateEntitySize(EntityManager manager, double offsetY) {
        return ParentContainerUpdater.updateCurrentEntitySize(this, manager, offsetY);
    }

    /**
     * Returns a multi-line debug representation of the entity and its attached
     * components.
     *
     * @return debug text for this entity
     */
    public String printInfo() {
        StringBuilder info = new StringBuilder(this + "\n");

        comps.forEach((k, e) -> {
            info.append(e.toString()).append("\n");
        });
        return info.toString();
    }

    /**
     * Returns debug text for this entity plus its currently resolved child
     * entities.
     *
     * @param entityManager entity manager used to resolve child UUIDs
     * @return debug text for this entity and its children
     */
    public String printInfoWithChildren(EntityManager entityManager) {
        return printInfo() + printChildren(entityManager);
    }

    @Override
    public String toString() {
        return "Entity[" + name +
                " id: " + uuid +
                " renderType: " + (render != null ? render.getClass().getSimpleName() : "null") +
                ']';
    }

}
