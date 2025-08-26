package com.demcha.components.core;

import com.demcha.core.PdfDocument;
import com.demcha.system.PdfEntityRender;
import com.demcha.system.PdfRender;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.*;

@Slf4j
public final class Entity implements PdfEntityRender {
    @Getter
    private final UUID id;
    private final Map<Class<? extends Component>, Component> comps = new LinkedHashMap<>();
    private EntityName name;
    private PdfRender render;
    @Setter @Getter
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
            if (render == null) {
                this.render = (PdfRender) c;
            } else {
                log.warn("Rendering Component Already signed in {}", this);
                throw new IllegalStateException("%sRendering Component Already signed in %s".formatted(c, this.render));
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
            if (render == null) {
                this.render = (PdfRender) c;
            } else {
                log.warn("Rendering Component Already signed in {}", this);
                this.render = (PdfRender) c;
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
            log.warn("Component {} from {} not found!", type, this);
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


    @Override
    public boolean render(PDPageContentStream cs) throws IOException {
        if (!hasRender()) {
            log.debug("Rendering entity {} without render", this);
            throw new NoSuchElementException("No component rendered for " + this);
        }
        return render.render(this, cs,guideLines);
    }

    public Entity buildInto(PdfDocument pdfDocument) {
        log.info("Put  {} in to the PdfDocument", this);

        pdfDocument.putEntity(this);
        return this;
    }
}

