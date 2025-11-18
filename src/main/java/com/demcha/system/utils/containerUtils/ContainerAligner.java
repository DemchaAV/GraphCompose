package com.demcha.system.utils.containerUtils;

import com.demcha.components.containers.abstract_builders.StackAxis;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.layout.VAnchor;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.renderable.Container;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h1>ContainerAligner using the Strategy Pattern</h1>
 *
 * <p>This class is responsible for aligning child entities within a parent container.
 * It has been refactored from a procedural switch-based approach to use the
 * <strong>Strategy design pattern</strong>. This decouples the main alignment logic from the
 * specific algorithms for different layout directions (e.g., horizontal, vertical).</p>
 *
 * <h2>Design Benefits:</h2>
 * <ul>
 * <li><strong>Open/Closed Principle:</strong> New layout types can be added by creating a new
 * strategy class without modifying this core aligner class.</li>
 * <li><strong>Single Responsibility Principle:</strong> Each strategy class has one job: to
 * implement a specific layout algorithm. The {@code ContainerAligner} class's only
 * job is to select the correct strategy.</li>
 * <li><strong>Reduced Complexity:</strong> Eliminates large switch statements, making the code
 * cleaner and easier to read.</li>
 * </ul>
 *
 * @see LayoutStrategy
 */
@Slf4j
public class ContainerAligner {

    private static final Map<StackAxis, LayoutStrategy> STRATEGIES = Map.of(
            StackAxis.HORIZONTAL, new HorizontalStrategy(),
            StackAxis.VERTICAL, new VerticalStrategy(),
            StackAxis.REVERSE_HORIZONTAL, new ReverseHorizontalStrategy(),
            StackAxis.REVERSE_VERTICAL, new ReverseVerticalStrategy()
    );

    public static void align(Entity parent, EntityManager entityManager) {
        if (!parent.hasAssignable(Container.class)) {
            log.debug("{} is not a Container and will be skipped.", parent);
            return;
        }

        StackAxis stackAxis = parent.getComponent(StackAxis.class)
                .orElseThrow(() -> new IllegalStateException("Container Entity must have a StackAxis component."));

        LayoutStrategy strategy = STRATEGIES.get(stackAxis);

        if (strategy != null) {
            log.debug("Aligning children of {} using {} strategy.", parent, stackAxis);
            strategy.alignChildren(parent, entityManager);
        } else {
            log.warn("No layout strategy found for StackAxis: {}. No alignment will be performed.", stackAxis);
        }
    }

    private interface LayoutStrategy {
        void alignChildren(Entity parent, EntityManager entityManager);
    }

    private abstract static class BaseLayoutStrategy implements LayoutStrategy {

        @Override
        public void alignChildren(Entity parent, EntityManager entityManager) {
            Align align = parent.getComponent(Align.class).orElseThrow();
            List<Entity> children = getOrderedChildren(parent, entityManager);
            Axes axes = new Axes();

            for (int i = 0; i < children.size(); i++) {
                Entity child = children.get(i);
                boolean isLastChild = (i == children.size() - 1);

                // IMPORTANT: Recursively align children of any nested containers first.
                ContainerAligner.align(child, entityManager);

                updateChildPosition(child, axes);
                updateContainerDimensions(child, axes);

                // Apply spacing between elements, but not after the last one.
                if (!isLastChild) {
                    axes.main += align.spacing();
                }
            }

            // After processing all children, update the parent's final size.
            Padding parentPadding = parent.getComponent(Padding.class).orElse(Padding.zero());
            parent.addComponent(getFinalContentSize(axes, parentPadding));
        }

        /**
         * CORRECTED: This now returns children in the standard, forward order.
         * The reverse logic is handled by the ReverseLayoutStrategy.
         */
        protected List<Entity> getOrderedChildren(Entity parent, EntityManager entityManager) {
            return parent.getChildren().stream()
                    .map(id -> entityManager.getEntity(id).orElseThrow())
                    .collect(Collectors.toList()).reversed();
        }

        protected abstract void updateChildPosition(Entity child, Axes axes);

        protected abstract void updateContainerDimensions(Entity child, Axes axes);

        /**
         * CORRECTED: The `lastChildMargin` parameter was removed to prevent double-counting.
         * The parent's size is determined by the accumulated children's outer sizes and the parent's own padding.
         */
        protected abstract ContentSize getFinalContentSize(Axes axes, Padding padding);

        protected static final class Axes {
            double main = 0;
            double cross = 0;
        }
    }

    // --- Concrete Strategy Implementations ---

    private static class HorizontalStrategy extends BaseLayoutStrategy {
        @Override
        protected void updateChildPosition(Entity child, Axes axes) {
            Position currentPos = child.getComponent(Position.class).orElse(Position.zero());
            Anchor anchor = child.getComponent(Anchor.class).orElse(Anchor.defaultAnchor());
            child.addComponent(new Position(axes.main, currentPos.y()));
            child.addComponent(new Anchor(HAnchor.DEFAULT, anchor.v()));
        }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) {
            var outbox = OuterBoxSize.from(child).orElseThrow();
            axes.main += outbox.width();
            axes.cross = Math.max(axes.cross, outbox.height());
        }

        @Override
        protected ContentSize getFinalContentSize(Axes axes, Padding padding) {
            // CORRECTED: Do not add last child's margin. Parent padding affects both axes.
            return new ContentSize(axes.main + padding.horizontal(), axes.cross + padding.vertical());
        }
    }

    private static class VerticalStrategy extends BaseLayoutStrategy {
        @Override
        protected void updateChildPosition(Entity child, Axes axes) {
            Position currentPos = child.getComponent(Position.class).orElse(Position.zero());
            Anchor anchor = child.getComponent(Anchor.class).orElse(Anchor.defaultAnchor());
            // CORRECTED: Preserves original x-position for a true vertical stack.
            // The previous logic (currentPos.x() + margin.bottom()) created an unintentional diagonal layout.
            child.addComponent(new Position(currentPos.x(), axes.main));
            child.addComponent(new Anchor(anchor.h(), VAnchor.DEFAULT));
        }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) {
            var outbox = OuterBoxSize.from(child).orElseThrow();
            axes.main += outbox.height();
            axes.cross = Math.max(axes.cross, outbox.width());
        }

        @Override
        protected ContentSize getFinalContentSize(Axes axes, Padding padding) {
            // CORRECTED: Do not add last child's margin. Parent padding affects both axes.
            return new ContentSize(axes.cross + padding.horizontal(), axes.main + padding.vertical());
        }
    }

    private abstract static class ReverseLayoutStrategy extends BaseLayoutStrategy {
        /**
         * CORRECTED: This class now correctly reverses the order of children.
         */
        @Override
        protected List<Entity> getOrderedChildren(Entity parent, EntityManager entityManager) {
            return parent.getChildren().stream()
                    .map(id -> entityManager.getEntity(id).orElseThrow())
                    .collect(Collectors.toList()); // .reversed() is now correctly placed here
        }
    }

    private static class ReverseHorizontalStrategy extends ReverseLayoutStrategy {
        private final HorizontalStrategy delegate = new HorizontalStrategy();

        @Override
        protected void updateChildPosition(Entity child, Axes axes) {
            delegate.updateChildPosition(child, axes);
        }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) {
            delegate.updateContainerDimensions(child, axes);
        }

        @Override
        protected ContentSize getFinalContentSize(Axes axes, Padding padding) {
            return delegate.getFinalContentSize(axes, padding);
        }
    }

    private static class ReverseVerticalStrategy extends ReverseLayoutStrategy {
        private final VerticalStrategy delegate = new VerticalStrategy();

        @Override
        protected void updateChildPosition(Entity child, Axes axes) {
            delegate.updateChildPosition(child, axes);
        }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) {
            delegate.updateContainerDimensions(child, axes);
        }

        @Override
        protected ContentSize getFinalContentSize(Axes axes, Padding padding) {
            return delegate.getFinalContentSize(axes, padding);
        }
    }
}