package com.demcha.components.geometry;

import com.demcha.components.core.Entity;
import com.demcha.components.style.Padding;
import com.demcha.system.ContentSizeNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public record InnerBoxSize(double innerW, double innerH) {

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
}
