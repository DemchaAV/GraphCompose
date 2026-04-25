package com.demcha.compose.document.dsl.internal;

import java.util.Objects;
import java.util.function.Consumer;

/** Internal helper for applying builder customizers. */
public final class BuilderSupport {
    private BuilderSupport() {
    }

    /**
     * Applies a non-null customizer to the supplied builder and returns it.
     *
     * @param builder builder being configured
     * @param spec callback that mutates the builder
     * @param <B> builder type
     * @return the same builder instance
     */
    public static <B> B configure(B builder, Consumer<B> spec) {
        Objects.requireNonNull(spec, "spec").accept(builder);
        return builder;
    }
}
