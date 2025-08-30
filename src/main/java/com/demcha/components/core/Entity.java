package com.demcha.components.core;

import com.demcha.core.EntityManager;
import com.demcha.system.PdfRender;
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
    private final UUID id;
    private final Map<Class<? extends Component>, Component> comps = new LinkedHashMap<>();
    private EntityName name;
    @Getter
    private PdfRender pdfRender;
    @Setter
    @Getter
    private boolean guideLines;

    public Entity() {
        UUID uuid = UUID.randomUUID();
        log.info("Creating entity {}", uuid);
        this.id = uuid;
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
        if (c instanceof PdfRender) {
            if (pdfRender == null) {
                this.pdfRender = (PdfRender) c;
            } else {
                log.warn("PdfRender Component Already signed in {}", this);
                throw new IllegalStateException("%PdfRender Component Already signed in %s".formatted(c, this.pdfRender));
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
        if (c instanceof PdfRender) {
            if (pdfRender == null) {
                this.pdfRender = (PdfRender) c;
            } else {
                log.warn("Rendering Component Already signed in {}", this);
                this.pdfRender = (PdfRender) c;
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
            log.error("No component found for type {} for entity [{}]", type.getName(), id);
            return new NoSuchElementException("Missing " + type.getName() + " for " + id);
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

    public boolean hasRender(Class<? extends Component> type) {
        return comps.values().stream()
                .anyMatch(comp -> comp instanceof PdfRender);
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
        log.debug("Viewing component {} {}", id, this);
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

    @Override
    public String toString() {
        return "Entity: \"" + (name == null ? "null" : name.value()) + "\" UUID: [" + id + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Entity entity = (Entity) o;
        return id.equals(entity.id) && comps.equals(entity.comps) && Objects.equals(name, entity.name);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + comps.hashCode();
        result = 31 * result + Objects.hashCode(name);
        return result;
    }


    public Entity buildInto(EntityManager entityManager) {
        log.info("Put  {} in to the EntityManager", this);

        entityManager.putEntity(this);
        return this;
    }
}

