package com.demcha.components.core;

import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import com.demcha.exceptions.ContentSizeNotFoundException;
import com.demcha.system.interfaces.Render;
import com.demcha.system.utils.page_breaker.Offset;
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
        log.info("Creating entity {}", uuid);
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

    public Entity verticalOffsetAndCorrectionSize(Offset offset) {
        if (offset == null || offset.y() == 0.0) {
            log.trace("Update ComputateioPosition.class and ContentSize.class not requer becouse {}", offset);
            return this;
        }
        log.debug("Checking component ContentSize.class {}", offset);
        var size = getComponent(ContentSize.class).orElseThrow(ContentSizeNotFoundException::new);
        log.debug("Updating Component Size to {}", size);
        addComponent(new ContentSize(size.width(), size.height() + Math.abs(offset.y())));

        return updateVerticalComputedPosition(offset);
    }

    public Entity updateVerticalComputedPosition(@NonNull Offset offset) {
        return updateComputedPosition(offset.y(), 0.0);
    }

    public Entity updateHorizontalComputedPosition(@NonNull Offset offset) {
        return updateComputedPosition(0.0, offset.y());
    }

    public Entity updateComputedPosition(double yPosition, double xPosition) {
        if (yPosition == 0.0 && xPosition == 0.0) {
            return this;
        }
        log.debug("Checking component ComputedPosition.class {}", this);
        var computedPosition = getComponent(ComputedPosition.class)
                .orElseThrow(() -> new NoSuchElementException("Missing component ComputedPosition.class"));
        log.debug("Updating Component ComputedPosition to {}", computedPosition);
        addComponent(new ComputedPosition(computedPosition.x() + xPosition, computedPosition.y() + yPosition));
        return this;
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

    public void updateSize(EntityManager manager) {
        var padding = getComponent(Padding.class).orElse(Padding.zero());
        var thisPlacement = getComponent(Placement.class).orElseThrow(() -> new NoSuchElementException("Missing component Placement.class"));
        double boundingTopLine = 0.0;
        double boundingBottomLine = 0.0;
        double boundingRightLine = 0.0;
        double boundingLeftLine = 0.0;
        int pageStarts = thisPlacement.startPage();
        int pageEnds = thisPlacement.endPage();

        if (getChildren().isEmpty()) {
            log.error("No children found for entity [{}] updateSize() aborted", this);
        } else {
            var children = getChildren();
            Set<Entity> entities = manager.getSetEntitiesFromUuids(new HashSet<>(children));
            for (Entity entity : entities) {
                Placement placement = entity.getComponent(Placement.class).orElseThrow(() -> new NoSuchElementException("Missing component Placement.class"));
                if (boundingTopLine < entity.boundingTopLine()) {
                    boundingTopLine = entity.boundingTopLine();
                    pageStarts = Math.min(pageStarts, placement.startPage());
                }
                if ((boundingBottomLine < entity.boundingBottomLine() && thisPlacement.endPage() <= placement.endPage())
                    || thisPlacement.endPage() < placement.endPage()
                ) {
                    boundingBottomLine = entity.boundingBottomLine();
                    pageEnds = Math.max(pageEnds, placement.endPage());
                }
                if (boundingRightLine < entity.boundingRightLine()) boundingRightLine = entity.boundingRightLine();
                if (boundingLeftLine < entity.boundingLeftLine()) boundingLeftLine = entity.boundingLeftLine();
            }

            log.trace("Updating component size");
        }
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

    public boolean updateParent(EntityManager manager, Offset offset) {
        if (offset == null) {
            log.error("Offset cannot be null");
            return false;
        }
        return updateParent(manager, offset.y());
    }

    public boolean updateParent(EntityManager manager, double offsetY) {

        if (offsetY == 0) {
            log.info("offset is zero");
            return false;
        }
        ParentComponent parentComponent = this.getComponent(ParentComponent.class).orElse(null);
        if (parentComponent == null) {
            log.error("Parent component cannot be found for entity [{}] updateSize() aborted", this);
            return false;
        }
        var parent = manager.getEntity(parentComponent.uuid()).orElse(null);
        var computedPos = parent.getComponent(ComputedPosition.class).orElseThrow();
        var size = parent.getComponent(ContentSize.class).orElseThrow();
        if (parent == null) {
            return false;
        } else {
            if (offsetY < 0) {
                double y = computedPos.y() + offsetY;
                double height = size.height() + Math.floor(offsetY);
                parent.addComponent(new ComputedPosition(computedPos.x(), y));
                parent.addComponent(new ContentSize(size.width(), height));
            } else {
                double height = size.height() + Math.floor(offsetY);
                parent.addComponent(new ContentSize(size.width(), height));
            }
            parent.updateParent(manager, offsetY);

        }
        return true;
    }

    public String printInfo() {
        System.out.println(this);
        StringBuilder info = new StringBuilder(this + "\n");

        comps.forEach((k, e) -> {
            info.append(e.toString()).append("\n");
        });
        return info.toString();
    }

    public String printInfoWithChildren(EntityManager entityManager) {
        StringBuilder info = new StringBuilder(printInfo());
        info.append(printChildren(entityManager));
        return info.toString();
    }

    @Override
    public String toString() {
        return "Entity[" + name +
               " id: " + uuid +
               " renderType: " + (render != null ? render.getClass().getSimpleName() : "null") +
               ']';
    }


}

