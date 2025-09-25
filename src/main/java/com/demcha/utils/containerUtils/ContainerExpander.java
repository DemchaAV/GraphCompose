package com.demcha.utils.containerUtils;

import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.system.ContentSizeNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
public final class ContainerExpander {

    private static final double EPS = 1e-6;

    private ContainerExpander() {}

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
        double requiredContentWidth = inner.innerW();
        double requiredContentHeight = inner.innerH();

        if (log.isTraceEnabled()) {
            log.trace("Initial required size from inner box for {}: {}x{}",
                    parent, requiredContentWidth, requiredContentHeight);
        }

        for (Entity child : children) {
            Optional<OuterBoxSize> childOuterOpt = OuterBoxSize.from(child);
            if (childOuterOpt.isEmpty()) continue;

            OuterBoxSize childOuter = childOuterOpt.get();
            if (log.isTraceEnabled()) log.trace("Child {} outer size: {}x{}", child, childOuter.width(), childOuter.height());

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

        final boolean wider = requiredInnerWidth > inner.innerW() + EPS;
        final boolean taller = requiredInnerHeight > inner.innerH() + EPS;

        if (!wider && !taller) {
            log.debug("Parent {} content not expanded (required <= inner).", parent);
            return false;
        }

        final ContentSize current = parent.getComponent(ContentSize.class).orElseThrow(() -> {
            log.error("All objects must have a ContentSize. Object {} doesn't have one.", parent);
            return new ContentSizeNotFoundException(parent);
        });

        final double newWidth = wider ? current.width() + (requiredInnerWidth - inner.innerW()) : current.width();
        final double newHeight = taller ? current.height() + (requiredInnerHeight - inner.innerH()) : current.height();

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
}
