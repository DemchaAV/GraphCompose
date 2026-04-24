package com.demcha.compose.engine.components.layout.coordinator;

import com.demcha.compose.engine.core.Canvas;
import com.demcha.compose.engine.components.core.Component;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.InnerBoxSize;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolved position of an entity relative to its layout context.
 * <p>
 * {@code ComputedPosition} is produced by layout from builder-time metadata such
 * as {@link Anchor}, {@code Position}, parent inner size, padding offsets, and
 * canvas geometry. It is the bridge between declarative placement intent and the
 * fully resolved coordinates later used to build {@link Placement}.
 * </p>
 */
@Slf4j
public record ComputedPosition(double x, double y) implements Component {
    /**
     * Returns a zero coordinate.
     */
    public static ComputedPosition zero() {
        return new ComputedPosition(0, 0);
    }

    /**
     * Resolves a child's position inside a parent inner box and applies the
     * parent's padding coordinate offset.
     *
     * @param child the child entity being placed
     * @param parentInnerBoxSize the available inner box of the parent
     * @param paddingParentCoordinate the drawing origin inside the parent's padding box
     * @return the resolved position for the child
     */
    public static ComputedPosition from(@NonNull Entity child, @NonNull InnerBoxSize parentInnerBoxSize, PaddingCoordinate paddingParentCoordinate) {
        var anchorOptional = child.getComponent(Anchor.class);
        var anchor = anchorOptional.orElseGet(() -> {
            log.warn("No Anchor found for {}. Using default Anchor (top-left).", child);
            Anchor defaultAnchor = Anchor.defaultAnchor();
            child.addComponent(defaultAnchor);
            return defaultAnchor;
        });
        ComputedPosition computedPosition = anchor.getComputedPosition(child, parentInnerBoxSize);
        double x = computedPosition.x + paddingParentCoordinate.x();
        double y = computedPosition.y + paddingParentCoordinate.y();
        return new ComputedPosition(x, y);
    }

    /**
     * Resolves a child's position directly from a parent entity.
     *
     * @param child the child entity
     * @param parent the parent entity supplying inner box and padding context
     * @return the resolved position
     * @throws java.util.NoSuchElementException if the parent has no {@link InnerBoxSize}
     */
    public static ComputedPosition from(@NonNull Entity child, @NonNull Entity parent) {
        var parentInnerBox = InnerBoxSize.from(parent).orElseThrow();
        var paddingParentCoordinate = PaddingCoordinate.from(parent);
        return from(child, parentInnerBox, paddingParentCoordinate);
    }

    /**
     * Resolves the position of a root-level entity against a document canvas.
     *
     * @param childEntity the entity being positioned
     * @param canvas the document canvas
     * @return the resolved position
     */
    public static ComputedPosition from(@NonNull Entity childEntity, @NonNull Canvas canvas) {
        log.debug("Computing position using default Canvas.");
        var margin = canvas.margin() != null ? canvas.margin() : Margin.zero();
        InnerBoxSize innerBoxSize = new InnerBoxSize(canvas.width()- margin.horizontal(), canvas.height() - margin.vertical());
        var paddingParentCoordinate = new PaddingCoordinate(margin.left(), margin.bottom());
        return from(childEntity, innerBoxSize, paddingParentCoordinate);
    }

    /**
     * Converts this resolved position into a padding-aware coordinate origin.
     *
     * @param padding the padding to apply
     * @return a coordinate shifted inside the padding box
     */
    public PaddingCoordinate paddingCoordinate(@NonNull Padding padding) {

        var x = this.x + padding.left();
        var y = this.y + padding.bottom();

        log.debug("ComputedPosition is {}", this.toString());
        log.debug("Padding coordinate is {}", padding);

        return new PaddingCoordinate(x, y);
    }
}

