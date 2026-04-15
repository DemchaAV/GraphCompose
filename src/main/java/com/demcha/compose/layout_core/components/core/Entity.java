package com.demcha.compose.layout_core.components.core;

import com.demcha.compose.layout_core.components.geometry.EntityBounds;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.Render;
import com.demcha.compose.layout_core.system.utils.page_breaker.Offset;
import com.demcha.compose.layout_core.system.utils.page_breaker.ParentContainerUpdater;
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

    public Entity() {
        UUID uuid = UUID.randomUUID();
        log.debug("Creating entity {}", uuid);
        this.uuid = uuid;
    }

    public static Entity createFrom(Entity entity) {
        log.debug("Creating a copy of entity {}", entity);
        var newEntity = new Entity();
        var values = new HashSet<>(entity.comps.values());
        newEntity.populate(values);
        return newEntity;
    }

    public String name() {
        return name.value();
    }

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

    public <T extends Component> Optional<T> getComponent(Class<T> type) {
        if (log.isDebugEnabled()) {
            log.debug("Getting component {} from  {}", type, this);
        }
        T value = type.cast(comps.get(type));
        if (value == null) {
            if (log.isDebugEnabled()) {
                log.debug("Component {} from {} not found!", type, this);
            }
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public <T extends Component> T require(Class<T> type) {
        if (log.isDebugEnabled()) {
            log.debug("Require component {} {}", type, this);
        }
        return getComponent(type).orElseThrow(() -> {
            log.error("No component found for type {} for entity [{}]", type.getName(), uuid);
            return new NoSuchElementException("Missing " + type.getName() + " for " + uuid);
        });
    }

    public boolean hasAssignable(Class<?> baseClass) {
        return comps.keySet().stream()
                .anyMatch(c -> baseClass.isAssignableFrom(c));
    }

    public boolean has(Class<? extends Component> type) {
        if (log.isDebugEnabled()) {
            log.debug("Checking component {} {}", type, this);
        }
        return comps.containsKey(type);
    }

    public <T extends Component> boolean has(T component) {
        var type = component.getClass();
        return has(type);
    }

    public boolean hasRender() {
        return render != null;
    }

    @Deprecated
    public double boundingTopLine() {
        return EntityBounds.topLine(this);
    }

    @Deprecated
    public double boundingBottomLine() {
        return EntityBounds.bottomLine(this);
    }

    @Deprecated
    public double boundingRightLine() {
        return EntityBounds.rightLine(this);
    }

    @Deprecated
    public double boundingLeftLine() {
        return EntityBounds.leftLine(this);
    }

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

    @Deprecated
    public boolean updateParentContainer(EntityManager manager, Offset offset) {
        return ParentContainerUpdater.updateParentContainer(this, manager, offset);
    }

    @Deprecated
    public boolean updateParentContainer(EntityManager manager, double offsetY) {
        return ParentContainerUpdater.updateParentContainer(this, manager, offsetY);
    }

    @Deprecated
    public boolean updateParentContainerSize(EntityManager manager, double offsetY) {
        return ParentContainerUpdater.updateParentContainerSize(this, manager, offsetY);
    }

    @Deprecated
    public boolean updateEntitySizeAndPosition(EntityManager manager, double offsetY, @NonNull Entity entity) {
        return ParentContainerUpdater.updateEntitySizeAndPosition(manager, offsetY, entity);
    }

    @Deprecated
    public boolean updateEntitySize(EntityManager manager, double offsetY, @NonNull Entity entity) {
        return ParentContainerUpdater.updateEntitySize(manager, offsetY, entity);
    }

    @Deprecated
    public boolean updateEntitySize(EntityManager manager, double offsetY) {
        return ParentContainerUpdater.updateCurrentEntitySize(this, manager, offsetY);
    }

    public String printInfo() {
        StringBuilder info = new StringBuilder(this + "\n");

        comps.forEach((k, e) -> {
            info.append(e.toString()).append("\n");
        });
        return info.toString();
    }

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
