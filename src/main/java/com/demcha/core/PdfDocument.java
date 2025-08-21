package com.demcha.core;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.layout.ParentComponent;
import com.demcha.system.System;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.nio.file.Path;
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
    private final PDPage page;
    private Path pathOut;
    private PDDocument document;

    public PdfDocument(PDPage page) {
        log.info("Created new pdf document {}", page.getBBox());
        this.page = page;
        document = new PDDocument();
        document.addPage(page);
    }

    public PdfDocument() {
        this(new PDPage(PDRectangle.A4));
        log.info("PdfDocument with default settings PDRectangle.A4");
    }

    public PdfDocument(PDRectangle pdfRectangle) {
        this(new PDPage(pdfRectangle));
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

    public PDPageContentStream openContentStream() throws IOException {
        return new PDPageContentStream(
                document, page,
                PDPageContentStream.AppendMode.APPEND,   // keep existing content if any
                true,                                    // compress
                true                                     // resetContext: isolates graphics state (PDFBox 3)
        );
    }

    public void addSystem(System system) {
        log.info("Adding System {}", system.getClass().getName());
        systems.add(system);
    }

    public PageSize pageSize() {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        return new PageSize(width, height);
    }


}
