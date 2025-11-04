package com.demcha.components.core;

import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import com.demcha.system.Render;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
        if (prev == null) {
            log.debug("Added component {} to {}", c, this);
        } else {
            log.debug("Replaced component {} on {}", c, this);
        }
        return this;
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
        if (prev == null) {
            log.debug("Added component {} to {}", c, this);
        } else {
            log.debug("Replaced component {} on {}", c, this);
        }
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

    public void printInfo() {
        System.out.println(this);
        comps.forEach((k, e) -> {
            System.out.println(e);
        });
    }

    @Override
    public String toString() {
        return "Entity[" + name +
               " id: " + uuid +
               ']';
    }


}

