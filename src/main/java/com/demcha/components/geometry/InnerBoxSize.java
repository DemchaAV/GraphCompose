package com.demcha.components.geometry;

import com.demcha.components.components_builders.ElementBuilder;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.exeptions.ContentSizeNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public record InnerBoxSize(double width, double hight) {

    public static Optional<InnerBoxSize> from(@NonNull Entity entity) {
        log.debug("Starting calculation a OuterBoxSize for entity: {}", entity);
        Optional<ContentSize> sizeOpt = entity.getComponent(ContentSize.class);
        var padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        var size = sizeOpt
                .orElseThrow(
                        () -> {
                            log.debug("{} has no outer BoxSize.", entity);
                            return new ContentSizeNotFoundException();
                        }
                );

        var innerBoxSizeOpt = from(size, padding);
        return innerBoxSizeOpt;
    }

    public static Optional<InnerBoxSize> from(@NonNull ElementBuilder entity) {
        log.debug("Starting calculation a OuterBoxSize for entity: {}", entity);
        Optional<ContentSize> sizeOpt = entity.getComponent(ContentSize.class);
        var padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        var size = sizeOpt
                .orElseThrow(
                        () -> {
                            log.debug("{} has no outer BoxSize.", entity);
                            return new ContentSizeNotFoundException();
                        }
                );
        var innerBoxSizeOpt = from(size, padding);
        var innerBoxSize = innerBoxSizeOpt.orElseThrow();
        log.debug("Calculated Inner  {} for entity: {}", innerBoxSize, entity);
        return innerBoxSizeOpt;
    }

    public static Optional<InnerBoxSize> from(ContentSize contentSize, Padding padding) {
        Objects.requireNonNull(contentSize, () -> {
            log.error("Can not calculate InnerBoxSize. All objects must have a ContentSize.");
            throw new ContentSizeNotFoundException();
        });
        Objects.requireNonNull(padding, () -> {
            log.error("Padding cannot be null.");
            throw new NullPointerException("Padding cannot be null.");
        });

        var w = contentSize.width() - padding.horizontal();
        var h = contentSize.height() - padding.vertical();

        var box = new InnerBoxSize(w, h);
        log.debug("{}", box);
        return Optional.of(box);
    }



    private static Placement getPlacement(Entity entity) {
        return entity.getComponent(Placement.class).orElseThrow(() -> {
            log.error("Placement.class has not been found. {}", entity);
            return new NullPointerException("Placement.class has not been found.");
        });
    }

    public double getX(Placement placement, Padding padding) {
        return placement.x() + padding.left();
    }

    public double getY(Placement placement, Padding padding) {
        return placement.y() + padding.bottom();
    }

    public double getInnerX(Entity entity) {
        Placement placement = getPlacement(entity);
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        return getX(placement, padding);
    }

    public double getInnerY(Entity entity) {
        Placement placement = getPlacement(entity);
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        return getY(placement, padding);
    }
}
