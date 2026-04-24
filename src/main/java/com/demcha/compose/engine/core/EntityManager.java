package com.demcha.compose.engine.core;

import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.components.core.Component;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.core.EntityName;
import com.demcha.compose.engine.components.layout.ParentComponent;
import com.demcha.compose.engine.core.SystemRegistry;
import com.demcha.compose.engine.render.Render;
import com.demcha.compose.engine.core.SystemECS;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Central registry for entities, systems, and document-scoped engine state.
 * <p>
 * {@code EntityManager} is the runtime hub of GraphCompose. Builders register
 * entities here, systems query and mutate those entities during layout and
 * rendering, and composer implementations use the registry to orchestrate the
 * full document pipeline.
 * </p>
 *
 * <p>Besides storing entities, the manager also owns system registration, the
 * active font library, layer/depth metadata used by layout, and document-wide
 * flags such as markdown and guide-line rendering.</p>
 *
 * <h2>Typical role in the pipeline</h2>
 * <ol>
 *   <li>builders create entities and register them here</li>
 *   <li>layout systems compute sizes, positions, and pagination metadata</li>
 *   <li>rendering systems read renderable entities and draw the final output</li>
 * </ol>
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
@Slf4j
@Getter
@Setter
public class EntityManager {
    private final Map<UUID, Entity> entities;
    private final SystemRegistry systems;
    private final FontLibrary fonts;
    private Map<Integer, List<UUID>> layers;
    private Map<UUID, Integer> depthById;
    private long mutationVersion;
    private boolean guideLines;
    private boolean markdown = false;

    public EntityManager() {
        this(false);
    }

    public EntityManager(@NonNull List<SystemECS> systems) {
        this(systems, DefaultFonts.standardLibrary(), true);
    }

    public EntityManager(@NonNull List<SystemECS> systems, FontLibrary fonts, boolean markdown) {
        log.debug("Creating new EntityManager");
        this.fonts = fonts;
        this.markdown = markdown;
        this.systems = new SystemRegistry();
        this.systems.addAllSystems(systems);
        this.entities = new LinkedHashMap<>();
        this.layers = new TreeMap<>();
        this.depthById = new LinkedHashMap<>();
    }

    public EntityManager(boolean markdown) {
        this(new ArrayList<>(), DefaultFonts.standardLibrary(), markdown);
    }

    public EntityManager(FontLibrary fonts, boolean markdown) {
        this(new ArrayList<>(), fonts, markdown);
    }

    public Entity createEntity() {
        return createEntity(null);
    }

    /**
     * Creates and registers a new entity, optionally assigning an {@link EntityName}.
     *
     * @param name optional logical name used for diagnostics and debugging
     * @return the newly created entity
     */
    public Entity createEntity(String name) {
        var entity = new Entity();

        putEntity(entity);

        if (name != null && !name.isBlank()) {
            entity.addComponent(new EntityName(name));
            log.trace("Created {}", entity);
        } else {
            log.trace("Created entity with no EntityName {}", entity.getUuid());
        }
        return entity;
    }

    public Optional<Entity> getEntity(UUID id) {
        log.trace("Getting  Entity id  {}", id);
        return Optional.ofNullable(entities.get(id));
    }

    public void setGuideLines(boolean guideLines) {
        this.guideLines = guideLines;
        touchMutation();
        entities.forEach((id, entity) -> {
            entity.setGuideLines(guideLines);
        });
    }

    public void setMarkdown(boolean markdown) {
        this.markdown = markdown;
        touchMutation();
    }

    public Entity putEntity(Entity entity) {
        UUID uuid = entity.getUuid();
        log.trace("Putting Entity id {}", uuid);

        Optional<Entity> existing = getEntity(uuid);

        if (existing.isPresent()) {
            if (entity.equals(existing)) {
                log.trace("Entity already exists and is identical");
                return existing.orElse(null);
            } else {
                log.warn("Entity conflict detected for id {}. Replacing old entity.", uuid);
                touchMutation();
                return entities.put(uuid, entity);
            }
        }
        entity.setGuideLines(guideLines);
        touchMutation();
        return entities.put(uuid, entity);
    }

    public String displayName(UUID id) {
        var entity = getEntity(id);

        log.info("Displaying entity with id {}", id);
        String name = this.entities.get(id).name();
        return name == null ? "Entity#" + id.toString().substring(0, 8) : name;
    }

    public void addComponent(UUID entityId, Component component) {
        Entity entity = entities.get(entityId);
        entity.addComponent(component);
    }

    public <T extends Component> Optional<T> getComponent(UUID entityId, Class<T> componentType) {
        Optional<Entity> entity = getEntity(entityId);
        return entity.flatMap(value -> value.getComponent(componentType));
    }

    public Optional<Set<UUID>> getEntitiesWithComponent(Class<? extends Component> componentType) {
        log.debug("Getting component with id {}", componentType.getName());
        Set<UUID> entityIds = null;
        for (Map.Entry<UUID, Entity> entry : entities.entrySet()) {
            var entityId = entry.getKey();
            log.debug("Checking entity with id {}", entry.getKey());
            var components = entry.getValue();
            if (components.has(componentType)) {
                log.debug("Found entity id [{}]  with component {}", entityId, componentType.getName());
                if (entityIds == null) {
                    entityIds = new LinkedHashSet<>();
                }
                log.debug("Adding entity [{}], to list  with component {}", entityId, componentType.getName());
                entityIds.add(entityId);
            }
        }
        if (entityIds == null || entityIds.isEmpty()) {
            log.warn("No component with id {} found", componentType.getName());
        }
        log.debug("Found [{}] entities  with component {}", entityIds == null ? 0 : entityIds.size(),
                componentType.getName());
        return Optional.ofNullable(entityIds);
    }

    public Set<UUID> getEntitiesWithAnyRender() {
        Set<UUID> result = new LinkedHashSet<>();
        for (Map.Entry<UUID, Entity> e : entities.entrySet()) {
            boolean hasRenderable = e.getValue().view().values().stream()
                    .anyMatch(comp -> comp instanceof Render);
            if (hasRenderable) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public boolean remove(UUID entityId) {
        boolean removed = entities.remove(entityId) != null;
        if (removed) {
            touchMutation();
        }
        return removed;
    }

    public boolean remove(Entity entity) {
        boolean removed = entities.remove(entity.getUuid()) != null;
        if (removed) {
            touchMutation();
        }
        return removed;
    }

    public Set<UUID> getEntitiesWithRender(Class<? extends Render> componentType) {
        log.debug("Searching for entities with component type {}", componentType.getName());

        Set<UUID> result = new LinkedHashSet<>();
        for (Map.Entry<UUID, Entity> e : entities.entrySet()) {
            UUID entityId = e.getKey();
            Entity entity = e.getValue();

            // перебираем компоненты у сущности
            for (Component comp : entity.view().values()) {
                // подходит ли по типу?
                if (componentType.isInstance(comp)) {
                    result.add(entityId);
                    log.debug("Matched entity {} with {}", entityId, componentType.getSimpleName());
                    break; // уже добавили — дальше компоненты этой сущности можно не проверять
                }
            }
        }

        if (result.isEmpty()) {
            log.warn("No entities found with {}", componentType.getName());
        } else {
            log.debug("Found {} entities with {}", result.size(), componentType.getName());
        }
        return result;
    }

    /**
     * Retrive Entities from UUUIDs Set
     *
     * @param uuids Set will be transformed
     * @return Set Entities
     */
    public Set<Entity> getSetEntitiesFromUuids(Set<UUID> uuids) {
        return uuids
                .stream()
                .map(this::getEntity)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));

    }

    /**
     * SystemECS
     */
    public void processSystems() {
        log.info("Processing Systems");
        for (SystemECS system : systems.systems().values()) {
            log.info("Processing SystemECS {}", system);
            system.process(this); // Передаём себя, чтобы система могла получить доступ к компонентам
        }
        log.info("Processed Systems");
    }

    public void printEntities() {
        this.entities.forEach((uuid, entity) -> {
            java.lang.System.out.println(entity.toString());
            entity.view().forEach((k, v) -> java.lang.System.out.printf("UUID[%s] : %s \n", uuid, entity.toString()));
        });
    }

    public void printEntitiesWithInfo() {
        this.entities.forEach((uuid, entity) -> {
            java.lang.System.out.println(entity.toString());
            entity.view().forEach((k, v) -> java.lang.System.out.printf("UUID[%s] : %s \n", uuid, entity.printInfo()));
        });
    }

    private void touchMutation() {
        mutationVersion++;
    }

}
