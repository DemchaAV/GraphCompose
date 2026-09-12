package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.image.DocumentImageData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Professional Sidebar CV contact marks.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/professional-sidebar/icons/} as PNG, at the point size
 * the design draws each one at — the marks are not one size, because a round
 * pin and a wide envelope need different heights to read as the same
 * weight.</p>
 */
final class ProfessionalSidebarIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String LOCATION = "location";
    static final String LINKEDIN = "linkedin";
    static final String WEBSITE = "website";

    private static final String ICON_ROOT = "/templates/cv/professional-sidebar/icons/";

    private static final Map<String, Double> SIZES = Map.of(
            PHONE, 8.8,
            EMAIL, 9.2,
            LOCATION, 9.6,
            LINKEDIN, 9.4,
            WEBSITE, 9.6);

    private static final Map<String, DocumentImageData> CACHE = new ConcurrentHashMap<>();

    private ProfessionalSidebarIcons() {
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
            throw new IllegalArgumentException(
                    "Unknown professional sidebar CV icon token: " + token);
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
        return CACHE.computeIfAbsent(token, ProfessionalSidebarIcons::read);
    }

    private static DocumentImageData read(String token) {
        String resourcePath = ICON_ROOT + token + ".png";
        try (InputStream input =
                     ProfessionalSidebarIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing professional sidebar CV icon: " + resourcePath);
            }
            return DocumentImageData.fromBytes(input.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read professional sidebar CV icon: " + resourcePath, e);
        }
    }
}
