package com.demcha.compose.engine.render;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;

import java.io.IOException;
import java.util.Objects;

/**
 * Backend-neutral render-pass session.
 *
 * <p>The engine uses a session to model "one rendering pass over one document".
 * Backends can reuse expensive page-local resources during the pass while the
 * engine keeps its contracts PDF-free. Concrete backends remain responsible for
 * what a page surface actually is and how it is opened or closed.</p>
 *
 * <p>Render handlers must treat the returned surfaces as session-owned. They may
 * change graphics/text state for their own draw operation, but they must not
 * close the surface directly.</p>
 *
 * @param <T> backend-specific surface type
 */
public interface RenderPassSession<T extends AutoCloseable> extends AutoCloseable {

    /**
     * Ensures that the target page exists and is ready to accept draw commands.
     *
     * @param pageIndex zero-based page index
     * @throws IOException if the backend cannot materialize the page
     */
    void ensurePage(int pageIndex) throws IOException;

    /**
     * Returns the session-owned drawing surface for a page.
     *
     * @param pageIndex zero-based page index
     * @return backend-specific page surface
     * @throws IOException if the backend cannot provide the surface
     */
    T pageSurface(int pageIndex) throws IOException;

    /**
     * Convenience helper for single-page entities.
     *
     * <p>Entities spanning multiple pages must acquire surfaces explicitly per
     * page fragment so their handler can manage page-local draw state.</p>
     *
     * @param entity renderable entity with resolved placement
     * @return session-owned page surface for the entity's single page
     * @throws IOException if the backend cannot provide the surface
     */
    default T pageSurface(Entity entity) throws IOException {
        Objects.requireNonNull(entity, "entity must not be null");
        Placement placement = entity.getComponent(Placement.class)
                .orElseThrow(() -> new IllegalStateException("Entity " + entity + " is missing Placement"));
        if (placement.startPage() != placement.endPage()) {
            throw new IllegalStateException(
                    "Entity " + entity + " spans multiple pages and must request page surfaces per fragment");
        }
        return pageSurface(placement.startPage());
    }

    @Override
    void close() throws IOException;
}
