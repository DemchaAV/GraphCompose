package com.demcha.components.layout;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.style.Margin;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Represents an object that can be anchored to a specific position.
 *
 * <p>Implementations of this interface provide access to an {@link Anchor}
 * that defines how the object is positioned or aligned within its container
 * or relative to another element.</p>
 *
 * <p>The meaning of the anchor depends on the context — for example, in a
 * UI layout system it might define alignment (e.g., top-left, center, bottom-right),
 * while in a graphics system it might define a fixed reference point.</p>
 */
@Slf4j
public record Anchor(HAnchor h, VAnchor v) implements Component {
    public static Anchor topLeft() {
        return new Anchor(HAnchor.LEFT, VAnchor.TOP);
    }
    public static Anchor left() {
        return new Anchor(HAnchor.LEFT, VAnchor.DEFAULT);
    }

    public static Anchor center() {
        return new Anchor(HAnchor.CENTER, VAnchor.MIDDLE);
    }
    public static Anchor centerLeft() {
        return new Anchor(HAnchor.LEFT, VAnchor.MIDDLE);
    }
    public static Anchor centerRight() {
        return new Anchor(HAnchor.RIGHT, VAnchor.MIDDLE);
    }

    public static Anchor topRight() {
        return new Anchor(HAnchor.RIGHT, VAnchor.TOP);
    }

    public static Anchor bottomLeft() {
        return new Anchor(HAnchor.LEFT, VAnchor.BOTTOM);
    }

    public static Anchor bottomRight() {
        return new Anchor(HAnchor.RIGHT, VAnchor.BOTTOM);
    }

    public static Anchor topCenter() {
        return new Anchor(HAnchor.CENTER, VAnchor.TOP);
    }

    public static Anchor bottomCenter() {
        return new Anchor(HAnchor.CENTER, VAnchor.BOTTOM);
    }
    public static Anchor defaultAnchor() {
        return new Anchor(HAnchor.DEFAULT, VAnchor.DEFAULT);
    }


    public ComputedPosition getComputedPosition(Entity child, InnerBoxSize perrentInnerBoxSize) {
        log.debug("Starting calculation of computed position for {} ", child);
        var position = child.getComponent(Position.class).orElse(Position.zero());

        var outerBoxSize = OuterBoxSize.from(child).get();

        return getComputedPosition(position, outerBoxSize, perrentInnerBoxSize);
    }

    private ComputedPosition getComputedPosition(Position position, OuterBoxSize outerBoxSize, InnerBoxSize parentInnerBoxSize) {
        Objects.requireNonNull(position, () -> {
            log.error("Position cannot be null.");
            throw new NullPointerException("Position cannot be null.");
        });
        Objects.requireNonNull(outerBoxSize, () -> {
            log.error("OuterBoxSize cannot be null.");
            throw new NullPointerException("OuterBoxSize cannot be null.");

        });
        Objects.requireNonNull(parentInnerBoxSize, () -> {
            log.error("ParentInnerBoxSize cannot be null.");
            throw new NullPointerException("ParentInnerBoxSize cannot be null.");

        });
        double outerW = outerBoxSize.width();
        double outerH = outerBoxSize.height();

        double areaW = parentInnerBoxSize.innerW();
        double areaH = parentInnerBoxSize.innerH();

        return getComputedPosition(position, areaW, outerW, areaH, outerH);

    }

    private ComputedPosition getComputedPosition(Position position, double areaW, double outerW, double areaH, double outerH) {
        double x = switch (this.h()) {
            case LEFT -> position.x();      // x=0 at left
            case CENTER -> (areaW - outerW) / 2.0 + position.x();
            case RIGHT -> areaW - outerW - position.x();
            case DEFAULT ->  position.x();
        };

        double y = switch (this.v()) {
            case BOTTOM -> position.y();          // y=0 at bottom
            case MIDDLE -> (areaH - outerH) / 2.0 + position.y();
            case TOP -> areaH - outerH - position.y();
            case DEFAULT ->  position.y();
        };

        var computed = new ComputedPosition(x, y);
        log.debug("Computed position with Anchor has been created: {}", computed);
        return computed;
    }

    public static RenderingPosition renderingPosition(ComputedPosition computedPosition, Margin margin) {
        Objects.requireNonNull(computedPosition, "computedPosition cannot be null");
        Objects.requireNonNull(margin, "margin cannot be null");

        // computedPosition — это левый-нижний угол OUTER-box (учитывает anchor/align).
        // Чтобы рисовать контент/рамку внутри margin, просто смещаемся внутрь.
        double rx = computedPosition.x() + margin.left();
        double ry = computedPosition.y() + margin.bottom();

        var renderingPosition = new RenderingPosition(rx, ry);
        log.debug("Rendering position computed: {}", renderingPosition);
        return renderingPosition;
    }
    public RenderingPosition renderingPosition(@NonNull Entity entity) {
        ComputedPosition computedPosition = entity.getComponent(ComputedPosition.class).get();
        Margin margin = entity.getComponent(Margin.class).get();
        return renderingPosition(computedPosition, margin);
    }

}

