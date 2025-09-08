package com.demcha.system;

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
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
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

    /**
     * A static map that holds all available layout strategies, keyed by their corresponding {@link StackAxis}.
     * This map is the core of the Strategy pattern implementation, allowing for a fast, clean lookup
     * of the correct alignment algorithm without using conditional statements.
     */
    private static final Map<StackAxis, LayoutStrategy> STRATEGIES = Map.of(
            StackAxis.HORIZONTAL, new HorizontalStrategy(),
            StackAxis.VERTICAL, new VerticalStrategy(),
            StackAxis.REVERSE_HORIZONTAL, new ReverseHorizontalStrategy(),
            StackAxis.REVERSE_VERTICAL, new ReverseVerticalStrategy()
    );

    /**
     * The primary public method to initiate the alignment process.
     * <p>
     * It performs the following steps:
     * <ol>
     * <li>Validates that the provided entity is a container.</li>
     * <li>Retrieves the container's {@link StackAxis} to determine the layout direction.</li>
     * <li>Looks up the appropriate {@link LayoutStrategy} from the strategies map.</li>
     * <li>Delegates the entire alignment process to the selected strategy.</li>
     * </ol>
     *
     * @param parent        The container {@link Entity} whose children need to be aligned.
     * @param entityManager The {@link EntityManager} used to fetch entity objects from their IDs.
     */
    public static void align(Entity parent, EntityManager entityManager) {
        if (!parent.hasAssignable(Container.class)) {
            log.debug("{} is not a Container and will be skipped.", parent);
            return;
        }

        StackAxis stackAxis = parent.getComponent(StackAxis.class)
                .orElseThrow(() -> new IllegalStateException("Container Entity must have a StackAxis component."));

        // Select the appropriate strategy from the map based on the container's StackAxis.
        LayoutStrategy strategy = STRATEGIES.get(stackAxis);

        if (strategy != null) {
            // Delegate the alignment task to the chosen strategy.
            log.debug("Aligning children of {} using {} strategy.", parent, stackAxis);
            strategy.alignChildren(parent, entityManager);
        } else {
            // Handle cases where a StackAxis is defined but has no corresponding strategy.
            log.warn("No layout strategy found for StackAxis: {}. No alignment will be performed.", stackAxis);
        }
    }

    /**
     * <h2>The Strategy Interface</h2>
     * <p>
     * This interface defines the contract for all concrete layout strategy classes.
     * It ensures that every strategy has a single, well-defined entry point for executing
     * its alignment algorithm.
     * </p>
     */
    private interface LayoutStrategy {
        /**
         * Executes the specific layout algorithm for aligning children of a given parent container.
         *
         * @param parent        The container entity.
         * @param entityManager The manager to fetch child entities.
         */
        void alignChildren(Entity parent, EntityManager entityManager);
    }

    /**
     * <h2>Abstract Base Strategy</h2>
     * <p>
     * An abstract class that implements the {@link LayoutStrategy} interface. It contains the
     * common boilerplate logic shared by all concrete strategies, such as the main iteration
     * loop over child entities and the recursive alignment call for nested containers.
     * This helps to keep the concrete strategies clean and focused on their specific layout logic.
     * </p>
     */
    private abstract static class BaseLayoutStrategy implements LayoutStrategy {

        /**
         * A mutable inner class used as an accumulator to track the container's dimensions
         * as child elements are processed.
         */
        protected static final class Axes {
            /** The primary axis of layout direction (e.g., width for horizontal, height for vertical). */
            double main = 0;
            /** The secondary axis, perpendicular to the main axis (e.g., height for horizontal). */
            double cross = 0;
        }

        /**
         * The template method that orchestrates the alignment process. It defines the skeleton
         * of the algorithm and lets subclasses override specific steps.
         */
        @Override
        public void alignChildren(Entity parent, EntityManager entityManager) {
            Align align = parent.getComponent(Align.class).orElseThrow();
            List<Entity> children = getOrderedChildren(parent, entityManager);
            Axes axes = new Axes();

            for (int i = 0; i < children.size(); i++) {
                Entity child = children.get(i);
                boolean isLastChild = (i == children.size() - 1);

                // IMPORTANT: Recursively align children of any nested containers first.
                // This ensures that we calculate layouts from the innermost container outwards.
                ContainerAligner.align(child, entityManager);

                // --- Defer to concrete strategy for specific calculations ---
                updateChildPosition(child, axes);
                updateContainerDimensions(child, axes);

                // Apply spacing between elements, but not after the last one.
                if (!isLastChild) {
                    axes.main += align.spacing();
                }
            }

            // After processing all children, update the parent's final size.
            parent.addComponent(getFinalContentSize(axes));
        }

        /**
         * Hook method to get the list of children in the correct iteration order.
         * Standard strategies process children in their natural order. Reverse strategies will override this.
         *
         * @return A list of child entities.
         */
        protected List<Entity> getOrderedChildren(Entity parent, EntityManager entityManager) {
            return parent.getChildren().stream()
                    .map(id -> entityManager.getEntity(id).orElseThrow())
                    .collect(Collectors.toList());
        }

        // --- Abstract "Hook" Methods ---
        // These methods must be implemented by concrete subclasses to define the specific layout logic.

        /**
         * Calculates and applies the new position for a child entity based on the current main axis progress.
         * @param child The child entity to position.
         * @param axes  The current state of the container's dimensions.
         */
        protected abstract void updateChildPosition(Entity child, Axes axes);

        /**
         * Updates the container's dimensions (main and cross axes) based on the size of the child being added.
         * @param child The child entity being processed.
         * @param axes  The accumulator for the container's dimensions.
         */
        protected abstract void updateContainerDimensions(Entity child, Axes axes);

        /**
         * Constructs the final {@link ContentSize} component for the parent container from the accumulated axes.
         * @param axes The final calculated dimensions.
         * @return The {@link ContentSize} component to be added to the parent.
         */
        protected abstract ContentSize getFinalContentSize(Axes axes);
    }

    // --------------------------------------------------------------------------------
    // --- Concrete Strategy Implementations ---
    // --------------------------------------------------------------------------------

    /**
     * Strategy for aligning children along the horizontal (X) axis.
     * - Main axis: Width
     * - Cross axis: Max Height
     */
    private static class HorizontalStrategy extends BaseLayoutStrategy {
        @Override
        protected void updateChildPosition(Entity child, Axes axes) {
            Position currentPos = child.getComponent(Position.class).orElse(Position.zero());
            Anchor anchor = child.getComponent(Anchor.class).orElse(Anchor.defaultAnchor());
            // Position the child at the end of the current main axis progress.
            child.addComponent(new Position(currentPos.x() + axes.main, currentPos.y()));
            child.addComponent(new Anchor(HAnchor.DEFAULT, anchor.v()));
        }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) {
            var outbox = OuterBoxSize.from(child).orElseThrow();
            axes.main += outbox.width(); // The main axis grows with the width of each child.
            axes.cross = Math.max(axes.cross, outbox.height()); // The cross axis is the height of the tallest child.
        }

        @Override
        protected ContentSize getFinalContentSize(Axes axes) {
            // For horizontal, main axis is width, cross axis is height.
            return new ContentSize(axes.main, axes.cross);
        }
    }

    /**
     * Strategy for aligning children along the vertical (Y) axis.
     * - Main axis: Height
     * - Cross axis: Max Width
     */
    private static class VerticalStrategy extends BaseLayoutStrategy {
        @Override
        protected void updateChildPosition(Entity child, Axes axes) {
            Position currentPos = child.getComponent(Position.class).orElse(Position.zero());
            Anchor anchor = child.getComponent(Anchor.class).orElse(Anchor.defaultAnchor());
            // Position the child at the end of the current main axis progress.
            child.addComponent(new Position(currentPos.x(), currentPos.y() + axes.main));
            child.addComponent(new Anchor(anchor.h(), VAnchor.DEFAULT));
        }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) {
            var outbox = OuterBoxSize.from(child).orElseThrow();
            axes.main += outbox.height(); // The main axis grows with the height of each child.
            axes.cross = Math.max(axes.cross, outbox.width()); // The cross axis is the width of the widest child.
        }

        @Override
        protected ContentSize getFinalContentSize(Axes axes) {
            // For vertical, main axis is height, cross axis is width.
            return new ContentSize(axes.cross, axes.main);
        }
    }

    /**
     * An abstract strategy that simply reverses the iteration order of children.
     * It inherits all other logic from {@link BaseLayoutStrategy}, demonstrating the power of composition.
     */
    private abstract static class ReverseLayoutStrategy extends BaseLayoutStrategy {
        @Override
        protected List<Entity> getOrderedChildren(Entity parent, EntityManager entityManager) {
            List<Entity> children = super.getOrderedChildren(parent, entityManager);
            Collections.reverse(children);
            return children;
        }
    }

    /**
     * A reverse horizontal strategy. It uses composition to delegate all layout calculations
     * to the standard {@link HorizontalStrategy}, after it has reversed the child order.
     */
    private static class ReverseHorizontalStrategy extends ReverseLayoutStrategy {
        private final HorizontalStrategy delegate = new HorizontalStrategy();

        @Override
        protected void updateChildPosition(Entity child, Axes axes) { delegate.updateChildPosition(child, axes); }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) { delegate.updateContainerDimensions(child, axes); }

        @Override
        protected ContentSize getFinalContentSize(Axes axes) { return delegate.getFinalContentSize(axes); }
    }

    /**
     * A reverse vertical strategy. It uses composition to delegate all layout calculations
     * to the standard {@link VerticalStrategy}, after it has reversed the child order.
     */
    private static class ReverseVerticalStrategy extends ReverseLayoutStrategy {
        private final VerticalStrategy delegate = new VerticalStrategy();

        @Override
        protected void updateChildPosition(Entity child, Axes axes) { delegate.updateChildPosition(child, axes); }

        @Override
        protected void updateContainerDimensions(Entity child, Axes axes) { delegate.updateContainerDimensions(child, axes); }

        @Override
        protected ContentSize getFinalContentSize(Axes axes) { return delegate.getFinalContentSize(axes); }
    }
}

