package com.demcha.compose.engine.components.renderable;

import com.demcha.compose.engine.components.content.text.BlockTextData;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.render.Render;
import com.demcha.compose.engine.pagination.Breakable;
import com.demcha.compose.engine.text.TextControlSanitizer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Render marker for breakable multi-line text blocks.
 */
@Slf4j
@Builder
@EqualsAndHashCode
@NoArgsConstructor
public class BlockText implements Render, Breakable {

    public static Optional<ValidatedTextData> validatedTextData(@NonNull Entity entity) {
        var blockTextDataOpt = entity.getComponent(BlockTextData.class);
        var styleOpt = entity.getComponent(TextStyle.class);

        if (blockTextDataOpt.isEmpty()) {
            log.info("TextComponent has no BlockTextData.class; skipping: {}", entity);
            return Optional.empty();
        }

        TextStyle style = styleOpt.orElse(TextStyle.DEFAULT_STYLE);
        if (styleOpt.isEmpty()) {
            log.info("TextComponent has no TextStyle; skipping: {}", entity);
        }

        return Optional.of(new ValidatedTextData(style, blockTextDataOpt.get()));
    }

    public static Optional<Placement> validatedPlacement(Entity entity) {
        if (!entity.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", entity);
            return Optional.empty();
        }

        var placementOpt = entity.getComponent(Placement.class);
        if (placementOpt.isEmpty()) {
            log.warn("TextComponent has no Placement.class Component, skipping: {}", entity);
            return Optional.empty();
        }
        return placementOpt;
    }

    public static String sanitizeText(String rawText) {
        return TextControlSanitizer.remove(rawText);
    }

    public record ValidatedTextData(TextStyle style, BlockTextData textValue) {
    }
}
