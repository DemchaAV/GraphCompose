package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.image.DocumentImageData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Navy Sidebar CV marks — the four contact glyphs
 * in the sidebar and the three section badges in the main column.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/navy-sidebar/icons/} as PNG, each at the point size the
 * design draws it at: a pin needs more height than an envelope to read as the
 * same weight, and the badge glyphs are set larger than the contact ones
 * because they sit inside a filled disc.</p>
 */
final class NavySidebarIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String LOCATION = "location";
    static final String LINKEDIN = "linkedin";
    static final String BRIEFCASE = "briefcase";
    static final String TROPHY = "trophy";
    static final String CERTIFICATE = "certificate";

    private static final String ICON_ROOT = "/templates/cv/navy-sidebar/icons/";

    private static final Map<String, Double> SIZES = Map.of(
            PHONE, 10.0,
            EMAIL, 10.0,
            LOCATION, 11.0,
            LINKEDIN, 10.5,
            BRIEFCASE, 11.3,
            TROPHY, 11.3,
            CERTIFICATE, 11.3);

    private static final Map<String, DocumentImageData> CACHE = new ConcurrentHashMap<>();

    private NavySidebarIcons() {
    }

    /**
     * The drawn size of one mark.
     *
     * @param token one of the constants on this class
     * @return the size in points
     * @throws IllegalArgumentException when the token names no packaged mark
     */
    static double size(String token) {
        Double size = SIZES.get(token);
        if (size == null) {
            throw new IllegalArgumentException("Unknown navy sidebar CV icon token: " + token);
        }
        return size;
    }

    /**
     * Reads one mark, caching the decoded image per token so a CV that
     * repeats a channel reads the file once per JVM.
     *
     * @param token one of the constants on this class
     * @return the image data
     */
    static DocumentImageData image(String token) {
        return CACHE.computeIfAbsent(token, NavySidebarIcons::read);
    }

    private static DocumentImageData read(String token) {
        String resourcePath = ICON_ROOT + token + ".png";
        try (InputStream input = NavySidebarIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing navy sidebar CV icon: " + resourcePath);
            }
            return DocumentImageData.fromBytes(input.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read navy sidebar CV icon: " + resourcePath, e);
        }
    }
}
