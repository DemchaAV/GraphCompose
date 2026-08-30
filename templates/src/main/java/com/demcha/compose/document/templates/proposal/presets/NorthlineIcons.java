package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.image.DocumentImageData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Northline icon set.
 *
 * <p>The icons ship inside the templates artifact under
 * {@code templates/proposal/northline/icons/}, keyed by the icon tokens the
 * structured proposal data carries (e.g. {@code badge-summary},
 * {@code fact-start}, {@code goal-brand}). Loaded bytes are cached per
 * token, so a two-page proposal reads each icon file once per JVM.</p>
 */
final class NorthlineIcons {

    private static final String ICON_ROOT = "/templates/proposal/northline/icons/";
    private static final Map<String, DocumentImageData> CACHE = new ConcurrentHashMap<>();

    private NorthlineIcons() {
    }

    /**
     * Loads (and caches) the icon for a data-supplied token.
     *
     * @param token the icon token, without extension
     * @return the icon image data
     * @throws IllegalArgumentException when the token names no packaged icon
     *         — an icon token is data, so a wrong one is a data error, the
     *         same class the phase-grid header contract throws
     */
    static DocumentImageData image(String token) {
        return CACHE.computeIfAbsent(token, NorthlineIcons::read);
    }

    private static DocumentImageData read(String token) {
        String resourcePath = ICON_ROOT + token + ".png";
        try (InputStream input = NorthlineIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException(
                        "Unknown northline proposal icon token '" + token
                                + "' — no packaged icon at " + resourcePath + ".");
            }
            return DocumentImageData.fromBytes(input.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read northline proposal icon: " + resourcePath, e);
        }
    }
}
