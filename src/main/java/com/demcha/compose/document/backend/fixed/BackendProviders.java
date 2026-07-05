package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.exceptions.MissingBackendException;

import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Locates the fixed-layout backend service providers registered on the
 * classpath.
 *
 * <p>Since GraphCompose 2.0 the render/measurement backend ships as a separate
 * artifact discovered through {@link ServiceLoader}. This locator resolves each
 * provider once, lazily, and caches it. When no provider is registered it throws
 * {@link MissingBackendException} naming the artifact to add. The explicit
 * {@code render(FixedLayoutBackend)} / {@code export(SemanticBackend)} paths do
 * not go through here — they use a caller-supplied backend.</p>
 *
 * @since 2.0.0
 */
public final class BackendProviders {

    private static final String MISSING_BACKEND_MESSAGE =
            "No fixed-layout render backend on the classpath: add the "
            + "io.github.demchaav:graph-compose-render-pdf artifact (or the "
            + "io.github.demchaav:graph-compose-bundle aggregate, or another "
            + "provider implementation) to render, rasterize, or measure a document.";

    private static volatile FontMetricsProvider fontMetrics;
    private static volatile FixedLayoutBackendProvider fixedLayout;

    private BackendProviders() {
    }

    /**
     * Returns the registered fixed-layout backend provider, resolving and caching
     * it on first use.
     *
     * @return the fixed-layout backend provider
     * @throws MissingBackendException if no provider is registered on the classpath
     */
    public static FixedLayoutBackendProvider fixedLayout() {
        FixedLayoutBackendProvider provider = fixedLayout;
        if (provider == null) {
            provider = load(FixedLayoutBackendProvider.class);
            fixedLayout = provider;
        }
        return provider;
    }

    /**
     * Returns the registered font-metrics provider, resolving and caching it on
     * first use.
     *
     * @return the font-metrics provider
     * @throws MissingBackendException if no provider is registered on the classpath
     */
    public static FontMetricsProvider fontMetrics() {
        FontMetricsProvider provider = fontMetrics;
        if (provider == null) {
            provider = load(FontMetricsProvider.class);
            fontMetrics = provider;
        }
        return provider;
    }

    private static <T> T load(Class<T> service) {
        return ServiceLoader.load(service, service.getClassLoader())
                .findFirst()
                .orElseThrow(missingBackend());
    }

    private static Supplier<MissingBackendException> missingBackend() {
        return () -> new MissingBackendException(MISSING_BACKEND_MESSAGE);
    }
}
