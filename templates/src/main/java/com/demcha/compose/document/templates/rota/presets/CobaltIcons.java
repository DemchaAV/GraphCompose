package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The marks the Cobalt rota packages, one per band of staff.
 *
 * <p>A band may name one or name none: the band is the navy strip and its
 * label, and the mark before it is the document's. A token this preset does not
 * package is a data error reported by name, because an unnamed mark and a
 * misspelt one are different mistakes and only one of them is silent.</p>
 */
final class CobaltIcons {

    /** The marks a document may name on a band. */
    static final List<String> TOKENS = List.of("management", "bartenders", "barbacks");

    private static final String ICON_ROOT = "/templates/rota/cobalt/icons/";

    /**
     * Keyed on the token and not on a file path: the marks are classpath
     * resources of this artifact, so the cache is as long-lived as the class
     * and the same across every document. Concurrent because two render threads
     * must not meet inside a plain map.
     */
    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private CobaltIcons() {
    }

    /**
     * The packaged mark a token names.
     *
     * @param token one of this preset's tokens
     * @return the parsed icon
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error and not a missing resource
     */
    static SvgIcon icon(String token) {
        if (!TOKENS.contains(token)) {
            throw new IllegalArgumentException(
                    "Unknown cobalt rota icon token '" + token
                            + "'. This preset packages " + TOKENS + ".");
        }
        return CACHE.computeIfAbsent(token, CobaltIcons::read);
    }

    /**
     * Whether the document named a mark at all, for a band that may carry none.
     *
     * @param token the token a document states
     * @return {@code true} when the document named one
     */
    static boolean has(String token) {
        return token != null && !token.isBlank();
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = CobaltIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing cobalt rota icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read cobalt rota icon: " + resourcePath, e);
        }
    }
}
