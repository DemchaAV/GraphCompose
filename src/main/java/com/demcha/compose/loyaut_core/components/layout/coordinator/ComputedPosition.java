package com.demcha.compose.loyaut_core.components.layout.coordinator;

import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.core.Component;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.InnerBoxSize;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.components.style.Padding;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the computed absolute position (x, y) of an {@link Entity}.
 * <p>
 * A {@code ComputedPosition} is usually calculated based on:
 * <ul>
 *   <li>The parent entity's {@link InnerBoxSize}</li>
 *   <li>The {@link Anchor} of the child</li>
 *   <li>Page size (for root-level entities)</li>
 * </ul>
 */
@Slf4j
public record ComputedPosition(double x, double y) implements Component {
    /**
     * Returns a zero position (0, 0).
     */
    public static ComputedPosition zero() {
        return new ComputedPosition(0, 0);
    }

    /**
     * Computes the position of a child entity relative to its parent's inner box size.
     * If the child has no {@link Anchor}, a default {@link Anchor#topLeft()} is added.
     *
     * @param child              the child entity
     * @param parentInnerBoxSize the inner box size of the parent
     * @return computed position for the child
     */
    public static ComputedPosition from(@NonNull Entity child, @NonNull InnerBoxSize parentInnerBoxSize, PaddingCoordinate paddingParentCoordinate) {
        var anchor = child.getComponent(Anchor.class).orElseGet(() -> {
            log.warn("No Anchor found for {}. Using default Anchor (top-left).", child);
            Anchor defaultAnchor = Anchor.defaultAnchor();
            child.addComponent(defaultAnchor);
            return defaultAnchor;
        });
        ComputedPosition computedPosition = anchor.getComputedPosition(child, parentInnerBoxSize);
        double x;
        double y;

        if (anchor.equals(Anchor.defaultAnchor())) {
            var position = child.getComponent(Position.class).orElse(Position.zero());
            x = computedPosition.x + paddingParentCoordinate.x() + position.x();
            y = computedPosition.y + paddingParentCoordinate.y() + position.y();
        }
        x = computedPosition.x + paddingParentCoordinate.x();
        y = computedPosition.y + paddingParentCoordinate.y();
        computedPosition = new ComputedPosition(x, y);

        return computedPosition;
    }

    /**
     * Computes the position of a child relative to its parent entity.
     *
     * @param child  the child entity
     * @param parent the parent entity
     * @return computed position
     * @throws java.util.NoSuchElementException if the parent has no {@link InnerBoxSize}
     */
    public static ComputedPosition from(@NonNull Entity child, @NonNull Entity parent) {
        var parentInnerBox = InnerBoxSize.from(parent).orElseThrow();
        var paddingParentCoordinate = PaddingCoordinate.from(parent);
        return from(child, parentInnerBox, paddingParentCoordinate);
    }

    /**
     * Computes the position of a child entity relative to a {@link Canvas}.
     * This is typically used for root-level entities (no parent).
     *
     * @param childEntity the child entity
     * @param canvas    the page size
     * @return computed position
     */
    public static ComputedPosition from(@NonNull Entity childEntity, @NonNull Canvas canvas) {
        log.debug("Computing position using default Canvas.");
        var margin = canvas.margin() != null ? canvas.margin() : Margin.zero();
        InnerBoxSize innerBoxSize = new InnerBoxSize(canvas.width()- margin.horizontal(), canvas.height() - margin.vertical());
        var paddingParentCoordinate = new PaddingCoordinate(margin.left(), margin.bottom());
        return from(childEntity, innerBoxSize, paddingParentCoordinate);
    }

    /**
     * Computes a {@link PaddingCoordinate} by applying the given padding to this position.
     *
     * @param padding the padding to apply
     * @return padding-adjusted coordinate
     */
    public PaddingCoordinate paddingCoordinate(@NonNull Padding padding) {

        var x = this.x + padding.left();
        var y = this.y + padding.bottom();

        log.debug("ComputedPosition is {}", this.toString());
        log.debug("Padding coordinate is {}", padding);

        return new PaddingCoordinate(x, y);
    }
}
