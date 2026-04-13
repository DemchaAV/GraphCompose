package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.renderable.PageBreakComponent;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Builder for a forced page-break entity.
 *
 * <p>The page-break entity is a zero-width, zero-height entity with a marker
 * component. The layout system should recognize this marker and advance to
 * the next page. If the page breaker does not recognize the marker, a small
 * height is used as a fallback.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * cb.pageBreak().build();
 * </pre>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
public class PageBreakBuilder extends EmptyBox<PageBreakBuilder> {

    PageBreakBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void initialize() {
        entity.addComponent(new PageBreakComponent());
        // Use a small symbolic size — the page break is not rendered visually.
        // The actual page advance happens via the rendering pipeline which
        // recognizes the PageBreakComponent marker.
        entity.addComponent(new ContentSize(0, 1));
    }
}
