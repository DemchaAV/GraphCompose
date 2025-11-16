package com.demcha.components.geometry;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import com.demcha.exeptions.ContentSizeNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

/**
 * This is size of Box Component which including a Component size + margins
 *
 */
@Slf4j
@Accessors(fluent = true)
@Data
public final class OuterBoxSize {
    final private double width;
    final private double height;

    public static Optional<OuterBoxSize> from(@NonNull Entity entity) {
        log.debug("Starting calculation a OuterBoxSize for entity: {}", entity);
        Optional<ContentSize> sizeOpt = entity.getComponent(ContentSize.class);
        Margin margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        var size = sizeOpt
                .orElseThrow(
                        () -> {
                            log.debug("{} has no outer BoxSize.", entity);
                            return new ContentSizeNotFoundException(entity);
                        }
                );
        Optional<OuterBoxSize> outerBoxSizeOpt = from(size, margin);
        OuterBoxSize outerBoxSize = outerBoxSizeOpt.orElseThrow();
        log.debug("Calculated outer {} for entity: {}", outerBoxSize, entity);
        return outerBoxSizeOpt;
    }

    public static Optional<OuterBoxSize> from(@NonNull ContentSize contentSize, @NonNull Margin margin) {
        Objects.requireNonNull(contentSize, () -> {
            log.error("All objects must have a ContentSize.");
            throw new ContentSizeNotFoundException();
        });


        var w = contentSize.width() + margin.horizontal();
        var h = contentSize.height() + margin.vertical();

        var box = new OuterBoxSize(w, h);
        log.debug("{}", box);
        return Optional.of(box);
    }

    private static Placement getPlacement(Entity entity) {
        return entity.getComponent(Placement.class).orElseThrow(() -> {
            log.error("Placement.class has not been found. {}", entity);
            return new NullPointerException("Placement.class has not been found.");
        });
    }

    public static double getX(Placement placement, @NonNull Margin margin) {
        //TODO  has to be placement.y() - margin.left()
        return placement.x();
    }

    public static double getY(Placement placement, @NonNull Margin margin) {
        return placement.y() - margin.bottom();
    }

    public static double getOuterX(Entity entity) {
        Placement placement = getPlacement(entity);
        Margin margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        return getX(placement, margin);
    }

    public static double getOuterY(Entity entity) {
        Placement placement = getPlacement(entity);
        Margin margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        return getY(placement, margin);
    }


}

