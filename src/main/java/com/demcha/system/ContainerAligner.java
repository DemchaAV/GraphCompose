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
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContainerAligner {


    public static void align(Entity parent, EntityManager entityManager) {
        Align align = parent.getComponent(Align.class).orElseThrow();
        var stackAxis = parent.getComponent(StackAxis.class).orElseThrow();
        if (!parent.has(ContentSize.class)) {
            log.info("{} is a container with component Container", parent);
            parent.addComponent(new ContentSize(0, 0));
            log.info("{} set in up a default size for container", parent);
        }

        var axes = new Axes();
        boolean hasNext = false;
        boolean isLast = false;
        switch (stackAxis) {
            case HORIZONTAL, REVERSE_VERTICAL -> {
                for (int i = 0; i < parent.getChildren().size(); i++) {
                    var entity = entityManager.getEntity(parent.getChildren().get(i)).orElseThrow();
                    log.info("HORIZONTAL, REVERSE_VERTICAL build(): {}", entity);
                    updateChildPosition(entity, stackAxis, axes);
                    if (i == (parent.getChildren().size() - 2)) {
                        hasNext = true;
                    }
                    if (i == (parent.getChildren().size() - 1)) {
                        isLast = true;
                    }
                    updateContainerDimensions(entity, hasNext, stackAxis, axes);
                    rearrange(entity, isLast, stackAxis, axes, align);
                }
            }
            case VERTICAL, REVERSE_HORIZONTAL -> {
                for (int i = parent.getChildren().size(); i > 0; i--) {
                    var entity = entityManager.getEntity(parent.getChildren().get(i - 1)).orElseThrow();
                    log.info("VERTICAL, REVERSE_HORIZONTAL build(): {}", entity);
                    updateChildPosition(entity, stackAxis, axes);
                    if (i == entity.getChildren().size() - 2) {
                        hasNext = true;
                    }
                    if (i == (parent.getChildren().size() - 1)) {
                        isLast = true;
                    }
                    updateContainerDimensions(entity, hasNext, stackAxis, axes);
                    rearrange(entity, isLast, stackAxis, axes, align);
                }
            }
            case DEFAULT -> defaultRearrange();
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + stackAxis);
            }
        }

        parent.addComponent(new ContentSize(axes.main, axes.cross));
    }

    private static void updateContainerDimensions(Entity entity, boolean isLast, StackAxis stackAxis, Axes axes) {
        switch (stackAxis) {
            case HORIZONTAL, REVERSE_HORIZONTAL -> updateContainerDimensionsHorizontal(entity, isLast, axes);
            case VERTICAL, REVERSE_VERTICAL -> updateContainerDimensionsVertical(entity, isLast, axes);
            case DEFAULT -> defaultRearrange();
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + stackAxis);
            }
        }
    }

    private static void rearrange(Entity child, boolean isLast, StackAxis stackAxis, Axes axes, Align align) {
        var out = OuterBoxSize.from(child).orElseThrow();
        switch (stackAxis) {
            case HORIZONTAL, REVERSE_HORIZONTAL -> {
                log.info("rearrange: current [{}] axisVertical= {}, axisHorizontal+ {} ", stackAxis, axes.cross, axes.main);
                axes.cross = Math.max(axes.cross, out.height());
                axes.main += isLast ? 0 : align.spacing();
                log.info("rearrange: New [{}] axisVertical= {}, axisHorizontal+ {} ", stackAxis, axes.cross, axes.main);
            }
            case VERTICAL, REVERSE_VERTICAL -> {
                log.info("rearrange: current [{}] axisVertical= {}, axes.main+ {} ", stackAxis, axes.cross, axes.main);
                axes.main = Math.max(axes.main, out.width());
                axes.cross += isLast ? 0 : align.spacing();
                log.info("rearrange: New [{}] axisVertical= {}, axes.main+ {} ", stackAxis, axes.cross, axes.main);

            }
            case DEFAULT -> {
                defaultRearrange();
            }
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + stackAxis);
            }
        }
    }

    private static void defaultRearrange() {
        log.info("The rearrange Settings are default");
        log.info("Didn't rearrange anything");
    }

    /**
     * Updates the position of a child entity within the horizontal container.
     * For horizontal containers, the primary axis is X. The child's X position
     * is adjusted based on the {@code primaryAxisPosition} accumulated from previous children.
     * A default horizontal anchor is also added to the child.
     *
     * @param child The {@link Entity} whose position needs to be updated.
     */

    private static void updateChildPosition(Entity child, StackAxis stackAxis, Axes axes) {
        Position cur = child.getComponent(Position.class).orElse(Position.zero());
        var anchor = child.getComponent(Anchor.class).orElse(Anchor.defaultAnchor());
        log.info("Current position {} with Axis {}", child, cur);
        switch (stackAxis) {
            case HORIZONTAL, REVERSE_HORIZONTAL -> {
                child.addComponent(new Anchor(HAnchor.DEFAULT, anchor.v())); // выравнивание по вертикали из Align
                Position c = new Position(cur.x() + axes.main, cur.y());
                log.info("New position {} with Axis {}", child, c);
                child.addComponent(c);
            }
            case VERTICAL, REVERSE_VERTICAL -> {
                child.addComponent(new Anchor(anchor.h(), VAnchor.DEFAULT)); // выравнивание по горизонтали из Align
                Position c = new Position(cur.x(), cur.y() + axes.cross);
                log.info("New position {} with Axis {}", child, c);
                child.addComponent(c);
            }
            case DEFAULT -> defaultRearrange();
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + stackAxis);
            }
        }

    }

    /**
     * Updates the dimensions of the container based on the dimensions of a child entity.
     * The container's width (primary axis) grows with each child's width plus spacing.
     * The container's height (secondary axis) is determined by the maximum height
     * among all its children.
     *
     * @param child The {@link Entity} whose dimensions contribute to the container's overall size.
     */

    private static void updateContainerDimensionsHorizontal(Entity child, boolean isLast, Axes axes) {

        var outbox = OuterBoxSize.from(child).orElseThrow();
        log.debug("{}", outbox);
        // Main axis grows with each child
        log.info("updateContainerDimensionsHorizontal: Current axisHorizontal={}, axisVertical={}", axes.main, axes.cross);
        axes.main += outbox.width();
        // Cross axis is the max of all children
        axes.cross = Math.max(axes.cross, outbox.height());
        log.info("updateContainerDimensionsHorizontal: Calculated axisHorizontal={}, axisVertical={}", axes.main, axes.cross);
    }

    private static void updateContainerDimensionsVertical(Entity child, boolean isLast, Axes axes) {

        var outbox = OuterBoxSize.from(child).orElseThrow();
        log.info("updateContainerDimensionsVertical: Current axisHorizontal={}, axisVertical={}", axes.main, axes.cross);
        // Main axis grows with each child
        axes.cross += outbox.height();
        // Cross axis is the max of all children
        axes.main = Math.max(axes.main, outbox.height());
        log.info("updateContainerDimensionsVertical: Calculated axisHorizontal={}, axisVertical={}", axes.main, axes.cross);
    }

    // Mutable accumulator to avoid pass-by-value bugs
    private static final class Axes {
        double main;   // X for H*, Y for V*
        double cross;  // max(Y) for H*, max(X) for V*
    }
}
