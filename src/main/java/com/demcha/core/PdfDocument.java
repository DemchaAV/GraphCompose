package com.demcha.core;

import com.demcha.components.content.components_builders.ComponentBuilder;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.system.System;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * # PdfDocument
 * <p>
 * A minimal ECS-style (Entity–Component–System) registry for your PDF domain.
 * <p>
 * - **Entities** are identified by {@link UUID}.<br>
 * - **Components** are plain data objects keyed by their concrete {@link Class}.<br>
 * - **Systems** implement domain logic via {@link System#process(PdfDocument)} and
 * operate on entities/components stored here.
 * </p>
 *
 * <h2>Key characteristics</h2>
 * <ul>
 *   <li>Not thread-safe — confine to a single thread or add external synchronization.</li>
 *   <li>Component lookup is O(1) by class per entity.</li>
 *   <li>{@link #getEntitiesWithComponent(Class)} is O(N) over all entities.</li>
 *   <li>Logging via SLF4J for traceability.</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * PdfDocument doc = new PdfDocument();
 * UUID e = doc.createEntity();
 * doc.addComponent(e, new Position(50, 100));
 * doc.addComponent(e, new Text("Hello"));
 *
 * doc.addSystem(new LayoutSystem());
 * doc.addSystem(new RenderSystem());
 *
 * doc.processSystems(); // Systems read/write components via the document
 * }</pre>
 *
 * <h2>Gotchas</h2>
 * <ul>
 *   <li>{@link #addComponent(UUID, Component)} assumes the entity exists and will throw
 *       a {@link NullPointerException} if it does not.</li>
 *   <li>{@link #getComponent(UUID, Class)} returns {@code null} when the entity or component
 *       is missing — check for {@code null}.</li>
 *   <li>{@link #getEntitiesWithComponent(Class)} may return {@code null} when none are found
 *       (current implementation). Treat accordingly.</li>
 *   <li>The type cast in {@link #getComponent(UUID, Class)} is unchecked; ensure you request
 *       the correct component class.</li>
 *   <li>The interface name {@code com.demcha.system.System} shadows {@link java.lang.System};
 *       import carefully or fully qualify when needed.</li>
 * </ul>
 *
 * @author Your Name
 * @since 0.1
 */
@Slf4j
@Getter
@Setter
public class PdfDocument {
    private final Map<UUID, Entity> entities = new HashMap<>();
    private final List<System> systems = new ArrayList<>();

    public Entity createEntity() {
        return createEntity(null);
    }

    /**
     * Method create
     * @param name
     * @return
     */
    public Entity createEntity(String name) {
        var entity = new Entity();

        putEntity(entity);

        if (name != null && !name.isBlank()) {
            entity.addComponent(new EntityName(name));
            log.info("Created {}", entity);
        } else {
            log.info("Created entity with no EntityName {}", entity.getId());
        }
        return entity;
    }

    public Optional<Entity> getEntity(UUID id) {
        log.info("Getting  Entity id  {}", id);
        return Optional.ofNullable(entities.get(id));
    }

    public  Entity putEntity(Entity entity) {
        log.info("Putting  Entity id  {}", entity.getId());
        return entities.put(entity.getId(), entity);
    }

    public UUID createAndPopulateEntity(ComponentBuilder componentBuilder) {
        log.info("Creating and populating entity");
        var entity = createEntity(componentBuilder.entityName());
        var id = entity.getId();
        Set<Component> components = componentBuilder.buildComponents();
        log.info("Populating entity UUID [{}] with\nComponents: {}", entity, components);
        for (Component component : components) {
            log.debug("Add component {}", component.getClass().getSimpleName());
            addComponent(id, component);
        }
        log.info("Created and populated entity {}", entity);
        return id;
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
        log.debug("Getting component type {} from entity id [{}]", componentType.getName(), entityId);
        if (!entities.containsKey(entityId)) {
            log.error("No component type {} in entity id [{}]", componentType.getName(), entityId);
            return null;
        }
        log.debug("Found component type {} from entity id [{}]", componentType.getName(), entityId);
        return entities.get(entityId).getComponent(componentType);
    }

    public List<UUID> getEntitiesWithComponent(Class<? extends Component> componentType) {
        log.debug("Getting component with id {}", componentType.getName());
        List<UUID> entityIds = null;
        for (Map.Entry<UUID, Entity> entry : entities.entrySet()) {
            var entityId = entry.getKey();
            log.debug("Checking entity with id {}", entry.getKey());
            var components = entry.getValue();
            if (components.has(componentType)) {
                log.debug("Found entity id [{}]  with component {}", entityId, componentType.getName());
                if (entityIds == null) {
                    entityIds = new ArrayList<>();
                }
                log.debug("Adding entity [{}], to list  with component {}", entityId, componentType.getName());
                entityIds.add(entityId);
            }
        }
        log.debug("Found [{}] entities  with component {}", entityIds == null ? 0 : entityIds.size(), componentType.getName());
        return entityIds;
    }

    public void addSystem(System system) {
        log.info("Adding System {}", system.getClass().getName());
        systems.add(system);
    }

    /**
     * System
     */
    public void processSystems() {
        log.info("Processing Systems");
        for (System system : systems) {
            log.info("Processing System {}", system);
            system.process(this); // Передаём себя, чтобы система могла получить доступ к компонентам
        }
    }


}
