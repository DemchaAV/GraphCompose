package com.demcha.components.geometry;

import com.demcha.components.core.Entity;
import com.demcha.components.style.Margin;
import com.demcha.system.ContentSizeNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

/**
 * This is size of Box Component which including a Component size + margins
 *
 * @param width
 * @param height
 */
@Slf4j
public record OuterBoxSize(double width, double height) {

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
}

