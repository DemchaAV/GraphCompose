package com.demcha.core;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.layout.ParentComponent;
import com.demcha.system.PdfRender;
import com.demcha.system.System;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.*;

/**
 * # EntityManager
 * <p>
 * A minimal ECS-style (Entity–Component–System) registry for your PDF domain.
 * <p>
 * - **Entities** are identified by {@link UUID}.<br>
 * - **Components** are plain data objects keyed by their concrete {@link Class}.<br>
 * - **Systems** implement domain logic via {@link System#process(EntityManager)} and
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
 * EntityManager doc = new EntityManager();
 * UUID e = doc.createEntity();
 * doc.addComponent(e, new Position(50, 100));
 * doc.addComponent(e, new TextComponent("Hello"));
 *
 * doc.addSystem(new LayoutSystem());
 * doc.addSystem(new RenderSystem());
 *
 * doc.processSystems(); // Systems read/write components via the entityManager
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
public class EntityManager {
    private final Map<UUID, Entity> entities;
    private final List<System> systems;
    private Map<Integer, List<UUID>> layers;     // NEW: layer → ordered ids
    private Map<UUID, Integer> depthById;
    private boolean guideLines;


    public EntityManager() {
        log.info("Creating new EntityManager");
        this.layers = new HashMap<>();
        this.depthById = new HashMap<>();
        this.systems = new ArrayList<>();
        this.entities = new HashMap<>();
    }


    public Entity createEntity() {
        return createEntity(null);
    }

    /**
     * Method create
     *
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

    public void setGuideLines(boolean guideLines) {
        this.guideLines = guideLines;
        entities.forEach((id, entity) -> {
            entity.setGuideLines(guideLines);
        });
    }

    public Entity putEntity(Entity entity) {
        UUID uuid = entity.getId();
        log.info("Putting Entity id {}", uuid);

        Optional<Entity> existing = getEntity(uuid);

        if (existing.isPresent()) {
            if (entity.equals(existing)) {
                log.info("Entity already exists and is identical");
                return existing.orElse(null);
            } else {
                log.warn("Entity conflict detected for id {}. Replacing old entity.", uuid);
                return entities.put(uuid, entity);
            }
        }
        entity.setGuideLines(guideLines);
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
                    entityIds = new HashSet<>();
                }
                log.debug("Adding entity [{}], to list  with component {}", entityId, componentType.getName());
                entityIds.add(entityId);
            }
        }
        if (entityIds == null || entityIds.isEmpty()) {
            log.warn("No component with id {} found", componentType.getName());
        }
        log.debug("Found [{}] entities  with component {}", entityIds == null ? 0 : entityIds.size(), componentType.getName());
        return Optional.ofNullable(entityIds);
    }

    public Set<UUID> getEntitiesWithAnyPdfRender() {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Entity> e : entities.entrySet()) {
            boolean hasRenderable = e.getValue().view().values().stream()
                    .anyMatch(comp -> comp instanceof PdfRender);
            if (hasRenderable) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public boolean remove(UUID entityId) {
        return entities.remove(entityId) != null;
    }
    public boolean remove(Entity entity) {
        return entities.remove(entity.getId()) != null;
    }

    public Set<UUID> getEntitiesWithPdfRender(Class<? extends PdfRender> componentType) {
        log.debug("Searching for entities with component type {}", componentType.getName());

        Set<UUID> result = new HashSet<>();
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

    public Optional<Map<UUID, Set<UUID>>> childrenByParent() {
        Optional<Set<UUID>> entitiesWithComponent = getEntitiesWithComponent(ParentComponent.class);

        if (entitiesWithComponent.isEmpty()) {
            return Optional.empty();
        }

        return childrenByParent(entitiesWithComponent.get());
    }

    public Optional<Map<UUID, Set<UUID>>> childrenByParent(Set<UUID> childrenWithParent) {
        Map<UUID, Set<UUID>> childrenByParent = new HashMap<>();

        for (UUID childId : childrenWithParent) {
            getComponent(childId, ParentComponent.class).ifPresent(parentComponent -> {
                UUID parentId = parentComponent.uuid(); // assuming ParentComponent has uuid()
                childrenByParent
                        .computeIfAbsent(parentId, k -> new HashSet<>())
                        .add(childId);
            });
        }

        return Optional.of(childrenByParent);
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


    public void addSystem(System system) {
        log.info("Adding System {}", system.getClass().getName());
        systems.add(system);
    }



    public void printEntities() {
        this.entities.values().forEach(entity -> {
            java.lang.System.out.println(entity.toString());
            entity.view().forEach((k, v) ->
                    java.lang.System.out.println(v)
            );
        });
    }


}
