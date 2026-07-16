package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.exceptions.MissingBackendException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
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
 * <p>Several fixed-layout backends may coexist on one classpath. The
 * {@link #fixedLayout(String)} overload selects one by its
 * {@link FixedLayoutBackendProvider#format() format}. The no-argument
 * {@link #fixedLayout()} default stays deterministic regardless of classpath
 * order: it prefers the {@code "pdf"} provider and falls back to the
 * lexicographically smallest format when no PDF backend is present.</p>
 *
 * @since 2.0.0
 */
public final class BackendProviders {

    private static final String DEFAULT_FIXED_LAYOUT_FORMAT = "pdf";

    private static final String MISSING_BACKEND_MESSAGE =
            "No fixed-layout render backend on the classpath: add the "
            + "io.github.demchaav:graph-compose-render-pdf artifact (or the "
            + "io.github.demchaav:graph-compose-bundle aggregate, or another "
            + "provider implementation) to render, rasterize, or measure a document.";

    private static volatile FontMetricsProvider fontMetrics;
    private static volatile FixedLayoutBackendProvider fixedLayout;
    private static final Map<String, FixedLayoutBackendProvider> fixedLayoutByFormat =
            new ConcurrentHashMap<>();

    private BackendProviders() {
    }

    /**
     * Returns the default fixed-layout backend provider, resolving and caching
     * it on first use.
     *
     * <p>When several providers are registered the choice is deterministic:
     * the {@code "pdf"} provider wins if present, otherwise the provider with
     * the lexicographically smallest {@link FixedLayoutBackendProvider#format()
     * format}.</p>
     *
     * @return the default fixed-layout backend provider
     * @throws MissingBackendException if no provider is registered on the classpath
     */
    public static FixedLayoutBackendProvider fixedLayout() {
        FixedLayoutBackendProvider provider = fixedLayout;
        if (provider == null) {
            provider = defaultFixedLayout();
            fixedLayout = provider;
        }
        return provider;
    }

    /**
     * Returns the fixed-layout backend provider registered for one output
     * format, resolving and caching it on first use.
     *
     * <p>Matching against {@link FixedLayoutBackendProvider#format()} is
     * case-insensitive. When several providers declare the same format the
     * first one enumerated by {@link ServiceLoader} wins.</p>
     *
     * @param format output format identifier such as {@code "pdf"} or {@code "pptx"}
     * @return the provider rendering that format
     * @throws MissingBackendException if no provider for the format is registered
     *                                 on the classpath
     * @since 2.1.0
     */
    public static FixedLayoutBackendProvider fixedLayout(String format) {
        Objects.requireNonNull(format, "format");
        String key = format.toLowerCase(Locale.ROOT);
        FixedLayoutBackendProvider cached = fixedLayoutByFormat.get(key);
        if (cached != null) {
            return cached;
        }
        FixedLayoutBackendProvider resolved = fixedLayoutProviders().stream()
                .filter(provider -> key.equals(normalizedFormat(provider)))
                .findFirst()
                .orElseThrow(() -> new MissingBackendException(missingFormatMessage(key)));
        return cacheByFormat(key, resolved);
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

    private static FixedLayoutBackendProvider defaultFixedLayout() {
        FixedLayoutBackendProvider chosen = fixedLayoutProviders().stream()
                .min(Comparator
                        .comparing((FixedLayoutBackendProvider provider) ->
                                !DEFAULT_FIXED_LAYOUT_FORMAT.equals(normalizedFormat(provider)))
                        .thenComparing(BackendProviders::normalizedFormat))
                .orElseThrow(missingBackend());
        return cacheByFormat(normalizedFormat(chosen), chosen);
    }

    /**
     * Canonicalizes provider instances per format so the default lookup and the
     * format-keyed lookup always hand out the same instance for one format.
     */
    private static FixedLayoutBackendProvider cacheByFormat(
            String key, FixedLayoutBackendProvider resolved) {
        FixedLayoutBackendProvider prior = fixedLayoutByFormat.putIfAbsent(key, resolved);
        return prior != null ? prior : resolved;
    }

    private static List<FixedLayoutBackendProvider> fixedLayoutProviders() {
        List<FixedLayoutBackendProvider> providers = new ArrayList<>();
        ServiceLoader.load(FixedLayoutBackendProvider.class,
                FixedLayoutBackendProvider.class.getClassLoader()).forEach(provider -> {
            // A provider without a format cannot be selected or win the default;
            // registering one is a contract violation, so it is skipped entirely.
            if (!normalizedFormat(provider).isEmpty()) {
                providers.add(provider);
            }
        });
        return providers;
    }

    private static String normalizedFormat(FixedLayoutBackendProvider provider) {
        String format = provider.format();
        return format == null ? "" : format.toLowerCase(Locale.ROOT);
    }

    private static String missingFormatMessage(String format) {
        return switch (format) {
            case "pdf" -> "No fixed-layout render backend for format \"pdf\" on the "
                    + "classpath: add the io.github.demchaav:graph-compose-render-pdf "
                    + "artifact (or the io.github.demchaav:graph-compose-bundle aggregate).";
            case "pptx" -> "No fixed-layout render backend for format \"pptx\" on the "
                    + "classpath: add the io.github.demchaav:graph-compose-render-pptx "
                    + "artifact.";
            default -> "No fixed-layout render backend for format \"" + format
                    + "\" on the classpath: add an artifact that registers a "
                    + "FixedLayoutBackendProvider for that format.";
        };
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
