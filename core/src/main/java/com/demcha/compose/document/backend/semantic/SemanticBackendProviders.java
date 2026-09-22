package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.exceptions.MissingBackendException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Locates the semantic export backend registered for one output format.
 *
 * <p>The rules are the fixed-layout locator's, because a classpath behaves the same way
 * whichever kind of backend is on it: the format is matched case-insensitively, a missing
 * provider names the artifact to add rather than failing as a null, and two providers
 * claiming one format is a mistake to report rather than a preference to resolve — leaving
 * {@link ServiceLoader} enumeration order to pick the backend would make the same document
 * export differently on two machines.</p>
 *
 * <p>There is no default-format lookup here, and that is the difference from the
 * fixed-layout side. "Render this document" has an obvious answer worth defaulting to;
 * "export this document semantically" does not, and guessing between two installed formats
 * would be picking a file type on the caller's behalf.</p>
 *
 * <p><b>Experimental</b> ({@code @Beta}) — see {@code docs/api-stability.md}.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.5.0
 */
@Beta
public final class SemanticBackendProviders {

    /** The artifact that provides each format the project itself publishes. */
    private static final Map<String, String> KNOWN_ARTIFACTS =
            Map.of("docx", "io.github.demchaav:graph-compose-render-docx");

    private static final Map<String, SemanticBackendProvider> byFormat = new ConcurrentHashMap<>();

    private SemanticBackendProviders() {
    }

    /**
     * Returns the provider exporting {@code format}, resolving and caching it on first use.
     *
     * @param format output format identifier such as {@code "docx"}
     * @return the provider exporting that format
     * @throws MissingBackendException if no provider for the format is on the classpath
     * @throws IllegalStateException   if more than one provider declares the format
     */
    public static SemanticBackendProvider forFormat(String format) {
        Objects.requireNonNull(format, "format");
        String key = format.toLowerCase(Locale.ROOT);
        SemanticBackendProvider cached = byFormat.get(key);
        if (cached != null) {
            return cached;
        }
        SemanticBackendProvider resolved = select(key, load());
        byFormat.put(key, resolved);
        return resolved;
    }

    /**
     * Chooses the one provider declaring {@code format}, or says why it cannot.
     *
     * <p>Separated from the classpath lookup and left package-private so the choice can be
     * tested against a stated list of providers. Registering a second provider through
     * {@link ServiceLoader} inside a test would mean writing a services file that every
     * other test in the module then shares.</p>
     *
     * @param format     the lower-cased format being looked for
     * @param candidates every provider on the classpath
     * @return the single provider declaring that format
     */
    static SemanticBackendProvider select(String format, List<SemanticBackendProvider> candidates) {
        List<SemanticBackendProvider> matches = candidates.stream()
                .filter(provider -> format.equals(normalized(provider)))
                .toList();
        if (matches.isEmpty()) {
            throw new MissingBackendException(missingMessage(format));
        }
        if (matches.size() > 1) {
            String names = matches.stream()
                    .map(provider -> provider.getClass().getName())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    "Multiple semantic export backends are registered for format \"" + format
                    + "\": " + names + ". Remove the duplicate artifact from the classpath, or "
                    + "pass a backend to export(...) instead of resolving it by format.");
        }
        return matches.get(0);
    }

    private static List<SemanticBackendProvider> load() {
        List<SemanticBackendProvider> providers = new ArrayList<>();
        ServiceLoader.load(SemanticBackendProvider.class).forEach(providers::add);
        if (providers.isEmpty()) {
            // The thread context loader is what a container hands an application, and the
            // service file lives with the backend artifact rather than with the core.
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            if (context != null) {
                ServiceLoader.load(SemanticBackendProvider.class, context).forEach(providers::add);
            }
        }
        return providers;
    }

    private static String normalized(SemanticBackendProvider provider) {
        String format = provider.format();
        return format == null ? "" : format.toLowerCase(Locale.ROOT);
    }

    private static String missingMessage(String format) {
        String artifact = KNOWN_ARTIFACTS.get(format);
        return "No semantic export backend on the classpath for format \"" + format + "\": add "
               + (artifact == null
                        ? "the artifact providing it"
                        : "the " + artifact + " artifact (or the "
                          + "io.github.demchaav:graph-compose-bundle aggregate)")
               + ", or pass a backend to export(...) directly.";
    }
}
