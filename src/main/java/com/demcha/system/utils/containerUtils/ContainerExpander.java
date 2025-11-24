package com.demcha.system.utils.containerUtils;

import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.core.EntityManager;
import com.demcha.exceptions.ContentSizeNotFoundException;
import com.demcha.components.geometry.Expendable;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public final class ContainerExpander {

    private static final double EPS = 1e-6;

    private ContainerExpander() {
    }

    /**
     * Ensures the parent's ContentSize is at least large enough to contain all children,
     * based on the parent's InnerBoxSize constraints and children OuterBoxSize.
     *
     * @param parent   container entity (must have InnerBoxSize and ContentSize)
     * @param children direct children of the container
     * @return true if parent's ContentSize was increased, false otherwise
     */
    public static boolean expandContentSizeByChildren(Entity parent, Set<Entity> children) {
        Objects.requireNonNull(parent, "parent cannot be null");
        Objects.requireNonNull(children, "children cannot be null");

        if (children.isEmpty()) {
            log.debug("No children; nothing to expand for {}", parent);
            return false;
        }

        final InnerBoxSize inner = InnerBoxSize.from(parent).orElseThrow(() ->
                new IllegalStateException("Parent is missing InnerBoxSize: " + parent));

        // Start from current inner box constraints
        double requiredContentWidth = inner.width();
        double requiredContentHeight = inner.height();

        if (log.isTraceEnabled()) {
            log.trace("Initial required size from inner box for {}: {}x{}",
                    parent, requiredContentWidth, requiredContentHeight);
        }

        for (Entity child : children) {
            Optional<OuterBoxSize> childOuterOpt = OuterBoxSize.from(child);
            if (childOuterOpt.isEmpty()) continue;

            OuterBoxSize childOuter = childOuterOpt.get();
            if (log.isTraceEnabled())
                log.trace("Child {} outer size: {}x{}", child, childOuter.width(), childOuter.height());

            // NOTE: if children can be offset, replace with (childRight, childBottom) calculation
            requiredContentWidth = Math.max(requiredContentWidth, childOuter.width());
            requiredContentHeight = Math.max(requiredContentHeight, childOuter.height());
        }

        return applyContentSizeIfLarger(parent, requiredContentWidth, requiredContentHeight);
    }

    /**
     * Increases ContentSize if (required > current inner) considering EPS.
     */
    private static boolean applyContentSizeIfLarger(Entity parent, double requiredInnerWidth, double requiredInnerHeight) {
        final InnerBoxSize inner = InnerBoxSize.from(parent).orElseThrow(() ->
                new IllegalStateException("Parent is missing InnerBoxSize: " + parent));

        final boolean wider = requiredInnerWidth > inner.width() + EPS;
        final boolean taller = requiredInnerHeight > inner.height() + EPS;

        if (!wider && !taller) {
            log.debug("Parent {} content not expanded (required <= inner).", parent);
            return false;
        }

        final ContentSize current = parent.getComponent(ContentSize.class).orElseThrow(() -> {
            log.error("All objects must have a ContentSize. Object {} doesn't have one.", parent);
            return new ContentSizeNotFoundException(parent);
        });

        final double newWidth = wider ? current.width() + (requiredInnerWidth - inner.width()) : current.width();
        final double newHeight = taller ? current.height() + (requiredInnerHeight - inner.height()) : current.height();

        // Avoid churn
        if (Math.abs(newWidth - current.width()) < EPS && Math.abs(newHeight - current.height()) < EPS) {
            log.debug("Computed size equals current ContentSize (within EPS); skipping update for {}", parent);
            return false;
        }

        ContentSize updated = new ContentSize(newWidth, newHeight);
        parent.addComponent(updated); // ensure addComponent replaces old instance

        log.info("Expanded ContentSize for {}: {}x{} -> {}x{}",
                parent, current.width(), current.height(), newWidth, newHeight);
        return true;
    }

    /**
     * The method normalizeBoxSize adjusts the size of parent entities. For each parent, it inspects the dimensions (OuterBoxSize) of all its direct children.
     * It then ensures the parent's OuterBoxSize is large enough to encompass its own original size and the size of its largest child by updating it to the maximum width and maximum height found.
     * In simple terms: It makes a parent container at least as big as its biggest child.
     *
     * @param childrenByParents map already sorted by parent
     */
    public static void process(Map<UUID, Set<UUID>> childrenByParents, EntityManager entityManager) {
        log.info("Box size normalizer");


        for (Map.Entry<UUID, Set<UUID>> parentUuid : childrenByParents.entrySet()) {

            var entityParentOpt = entityManager.getEntity(parentUuid.getKey());
            if (entityParentOpt.isEmpty()) {
                log.warn("Hasn't find a Parent Entity by id {}", parentUuid.getKey());
                continue;
            }
            var parentEntity = entityParentOpt.get();
            if (!isExpandable(parentEntity)) {
                continue;
            }
            // Retriven Entiti
            var childrenEntities = entityManager.getSetEntitiesFromUuids(parentUuid.getValue());


            if (parentEntity.has(Align.class)) {

                log.debug("It is a container with component Align");


            }
            ContainerExpander.expandContentSizeByChildren(parentEntity, childrenEntities);

        }
    }



    private static boolean isExpandable(Entity entity) {
        if (entity.hasAssignable(Expendable.class)) {
            return true;
        }
        log.info("Entity {} has no Expendable", entity);
        return false;
    }
}
