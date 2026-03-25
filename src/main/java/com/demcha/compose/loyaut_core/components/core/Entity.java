package com.demcha.compose.loyaut_core.components.core;

import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.components.layout.ParentComponent;
import com.demcha.compose.loyaut_core.components.layout.coordinator.ComputedPosition;
import com.demcha.compose.loyaut_core.components.layout.coordinator.Placement;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.interfaces.Render;
import com.demcha.compose.loyaut_core.system.utils.page_breaker.Offset;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
            log.debug("addComponentIfAbsent: component {} already exists on {}", key.getName(), this);
            return this;
        }
        if (c instanceof EntityName en) this.name = en;
        log.debug("Added absent component {} to {}", key.getSimpleName(), this);
        return this;
    }

    public <T extends Component> Entity addComponent(@NonNull T c) {
        Class<? extends Component> key = c.getClass().asSubclass(Component.class);
        log.debug("Adding component {} to {}", c, this);
        Component prev = comps.put(key, c);
        if (c instanceof EntityName en) this.name = en;
        if (c instanceof Render) {
            if (render == null) {
                this.render = (Render) c;
            } else {
                log.warn("Render Component Already signed in {}", this);
                throw new IllegalStateException("%sPdfRender Component Already signed in %s".formatted(c, this.render));
            }

        }
        debugPutMethod(c, prev, "addComponent");
        return this;
    }

    private <T extends Component> void debugPutMethod(@NotNull T c, Component prev, String methodName) {
        if (prev == null) {
            log.debug("Added component {} to {} through method {}", c, this, methodName);
        } else {
            log.debug("Replaced component {} on {} through method {}", c, this, methodName);
        }
    }

    public <T extends Component> Entity forceAddComponent(@NonNull T c) {
        Class<? extends Component> key = c.getClass().asSubclass(Component.class);
        Component prev = comps.put(key, c);
        if (c instanceof EntityName en) this.name = en;
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
        log.debug("Getting component {} from  {}", type, this);
        T value = type.cast(comps.get(type));
        if (value == null) {
            log.debug("Component {} from {} not found!", type, this);
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public <T extends Component> T require(Class<T> type) {
        log.debug("Require component {} {}", type, this);
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
        log.debug("Checking component {} {}", type, this);
        return comps.containsKey(type);
    }

    public <T extends Component> boolean has(T component) {
        var type = component.getClass();
        return has(type);
    }

    public boolean hasRender() {
        return comps.values().stream()
                .anyMatch(comp -> comp instanceof Render);
    }


    public double boundingTopLine() {
        var placement = getComponent(Placement.class).orElseThrow(() -> {
            // Лямбда здесь позволяет выполнить логирование *только* в случае ошибки
            log.error("No component Placement.class found for entity [{}] boundingTopLine() aborted", this);
            return new NoSuchElementException("Missing component Placement.class");
        });
        var margin = getComponent(Margin.class).orElse(Margin.zero());
        var size = getComponent(ContentSize.class).orElseThrow(() -> new NoSuchElementException("Missing component ContentSize.class"));

        return placement.y() + size.height() + margin.top();
    }

    public double boundingBottomLine() {
        var placement = getComponent(Placement.class).orElseThrow(() -> {
            // Лямбда здесь позволяет выполнить логирование *только* в случае ошибки
            log.error("No component Placement.class found for entity [{}] boundingBottomLine() aborted", this);
            return new NoSuchElementException("Missing component Placement.class");
        });
        var margin = getComponent(Margin.class).orElse(Margin.zero());

        return placement.y() - margin.bottom();
    }

    public double boundingRightLine() {
        var placement = getComponent(Placement.class).orElseThrow(() -> {
            // Лямбда здесь позволяет выполнить логирование *только* в случае ошибки
            log.error("No component Placement.class found for entity [{}] boundingRightLine() aborted", this);
            return new NoSuchElementException("Missing component Placement.class");
        });
        var margin = getComponent(Margin.class).orElse(Margin.zero());
        var size = getComponent(ContentSize.class).orElseThrow(() -> new NoSuchElementException("Missing component ContentSize.class"));

        return placement.x() + size.width() + margin.right();
    }

    public double boundingLeftLine() {
        var placement = getComponent(Placement.class).orElseThrow(() -> {
            // Лямбда здесь позволяет выполнить логирование *только* в случае ошибки
            log.error("No component Placement.class found for entity [{}] boundingLeftLine() aborted", this);
            return new NoSuchElementException("Missing component Placement.class");
        });
        var margin = getComponent(Margin.class).orElse(Margin.zero());

        return placement.x() - margin.left();
    }


    public <T extends Component> Entity populate(Set<? extends Component> components) {
        log.info("Creating and populating entity");

        log.info("Populating entity UUID [{}] with\nComponents: {}", this, components);
        for (Component component : components) {
            addComponent(component);
        }
        log.info("Created and populated entity {}", this);
        return this;

    }


    /**
     * That method will return a copy of hashMap Entities as unmodifiableMap
     *
     * @return <p>Map<Class<? extends Component>, Component></p>
     */
    public Map<Class<? extends Component>, Component> view() {
        log.debug("Viewing component {} {}", uuid, this);
        return Collections.unmodifiableMap(comps);
    }

    public boolean remove(Class<? extends Component> type) {
        if (has(type)) {
            comps.remove(type);
            return true;
        } else {
            return false;
        }
    }

    private String printChildren(EntityManager manager) {
        StringBuilder childrenInfo = new StringBuilder();
        manager.getSetEntitiesFromUuids(new HashSet<>(children)).forEach(entity -> {
            childrenInfo.append(entity.toString()).append("\n");
        });
        return childrenInfo.toString();

    }

    public boolean updateParentContainer(EntityManager manager, Offset offset) {
        if (offset == null) {
            log.error("Offset cannot be null");
            return false;
        }
        return updateParentContainer(manager, offset.y());
    }


    public boolean updateParentContainer(EntityManager manager, double offsetY) {

        // 1. Guard Clause: No offset, no work needed.
        if (offsetY == 0) {
            log.info("Update aborted: Offset is zero");
            return false;
        }

        // 2. Get Parent Component Info
        // Note: 'var' infers the type automatically (Java 10+)
        var parentComponent = this.getComponent(ParentComponent.class).orElse(null);

        if (parentComponent == null) {
            // Warning is often better than Error if it's a root element (has no parent)
            log.warn("Parent component missing for entity [{}]. updateParent() stopped.", this);
            return false;
        }

        // 3. Fetch Parent Entity
        var parent = manager.getEntity(parentComponent.uuid()).orElse(null);
        if (parent == null) {
            log.error("Parent Entity not found in Manager for UUID: {}", parentComponent.uuid());
            return false;
        }
        return updateEntitySizeAndPosition(manager, offsetY, parent);
    }

    public boolean updateParentContainerSize(EntityManager manager, double offsetY) {

        // 1. Guard Clause: No offset, no work needed.
        if (offsetY == 0) {
            log.info("Update aborted: Offset is zero");
            return false;
        }

        // 2. Get Parent Component Info
        // Note: 'var' infers the type automatically (Java 10+)
        var parentComponent = this.getComponent(ParentComponent.class).orElse(null);

        if (parentComponent == null) {
            // Warning is often better than Error if it's a root element (has no parent)
            log.warn("Parent component missing for entity [{}]. updateParent() stopped.", this);
            return false;
        }

        // 3. Fetch Parent Entity
        var parent = manager.getEntity(parentComponent.uuid()).orElse(null);
        if (parent == null) {
            log.error("Parent Entity not found in Manager for UUID: {}", parentComponent.uuid());
            return false;
        }
        return updateEntitySize(manager, offsetY, parent);
    }

    public boolean updateEntitySizeAndPosition(EntityManager manager, double offsetY, @NonNull Entity entity) {


        // 4. Fetch Parent State (Throw if state is corrupt/missing)
        // Using orElseThrow checks data integrity.
        var computedPos = entity.getComponent(ComputedPosition.class)
                .orElseThrow(() -> new IllegalStateException("Parent missing Position"));
        var size = entity.getComponent(ContentSize.class)
                .orElseThrow(() -> new IllegalStateException("Parent missing Size"));

        // 5. Calculate New Dimensions
        // We treat records/components as immutable, creating new instances.
        if (offsetY < 0) {
            // EXPAND UPWARDS: Move Y up, Increase Height
            double newY = computedPos.y() + offsetY;
            double newHeight = size.height() + Math.abs(offsetY);

            entity.addComponent(new ComputedPosition(computedPos.x(), newY));
            entity.addComponent(new ContentSize(size.width(), newHeight));
        } else {
            // EXPAND DOWNWARDS: Y stays same, Increase Height
            double newHeight = size.height() + offsetY;

            entity.addComponent(new ContentSize(size.width(), newHeight));
        }

        // 6. Recursion: Bubble the change up the tree
        return entity.updateParentContainer(manager, offsetY);
    }

    public boolean updateEntitySize(EntityManager manager, double offsetY, @NonNull Entity entity) {


        // 4. Fetch Parent State (Throw if state is corrupt/missing)
        // Using orElseThrow checks data integrity.
        var computedPos = entity.getComponent(ComputedPosition.class)
                .orElseThrow(() -> new IllegalStateException("Parent missing Position"));
        var size = entity.getComponent(ContentSize.class)
                .orElseThrow(() -> new IllegalStateException("Parent missing Size"));

        // 5. Calculate New Dimensions
        // We treat records/components as immutable, creating new instances.
        if (offsetY < 0) {
            // EXPAND UPWARDS: Move Y up, Increase Height
            double newHeight = size.height() + Math.abs(offsetY);

            entity.addComponent(new ContentSize(size.width(), newHeight));
        } else {
            // EXPAND DOWNWARDS: Y stays same, Increase Height
            double newHeight = size.height() + offsetY;

            entity.addComponent(new ContentSize(size.width(), newHeight));
        }

        // 6. Recursion: Bubble the change up the tree
        return entity.updateParentContainerSize(manager, offsetY);
    }

    public boolean updateEntitySize(EntityManager manager, double offsetY) {


        // 4. Fetch Parent State (Throw if state is corrupt/missing)
        // Using orElseThrow checks data integrity.
        var computedPos = this.getComponent(ComputedPosition.class)
                .orElseThrow(() -> new IllegalStateException("Parent missing Position"));
        var size = this.getComponent(ContentSize.class)
                .orElseThrow(() -> new IllegalStateException("Parent missing Size"));

        // 5. Calculate New Dimensions
        // We treat records/components as immutable, creating new instances.
        if (offsetY < 0) {
            // EXPAND UPWARDS: Move Y up, Increase Height
            double newHeight = size.height() + Math.abs(offsetY);

            this.addComponent(new ContentSize(size.width(), newHeight));
        } else {
            // EXPAND DOWNWARDS: Y stays same, Increase Height
            double newHeight = size.height() + offsetY;

            this.addComponent(new ContentSize(size.width(), newHeight));
        }

        // 6. Recursion: Bubble the change up the tree
        return this.updateParentContainerSize(manager, offsetY);
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

