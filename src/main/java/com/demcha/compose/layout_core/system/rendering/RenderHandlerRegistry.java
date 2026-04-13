package com.demcha.compose.layout_core.system.rendering;

import com.demcha.compose.layout_core.system.interfaces.Render;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry for renderer-owned handlers keyed by render marker type.
 */
public final class RenderHandlerRegistry {
    private final Map<Class<? extends Render>, RenderHandler<?, ?>> handlers = new LinkedHashMap<>();

    public <R extends Render, RS extends RenderingSystemECS<?>> void register(RenderHandler<R, RS> handler) {
        Objects.requireNonNull(handler, "handler");
        handlers.put(handler.renderType(), handler);
    }

    public Optional<RenderHandler<?, ?>> find(Render render) {
        if (render == null) {
            return Optional.empty();
        }

        RenderHandler<?, ?> direct = handlers.get(render.getClass());
        if (direct != null) {
            return Optional.of(direct);
        }

        return handlers.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(render))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
