package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Midnight Navy CV marks: the five contact glyphs and
 * the three the achievement discs draw.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/midnight-navy/icons/} as SVG, so they scale with the box
 * they are drawn into rather than being resampled. Parsed icons are cached per
 * token, so a sheet that repeats a mark reads each file once per JVM.</p>
 *
 * <p>{@link #ENTRY_TOKENS} is the vocabulary a document names through
 * {@code CvEntry.icon()}. It is scoped to this preset — another preset packages
 * its own set — and each mark carries the width it is drawn at, because an
 * icon's box is not its glyph and the sizes are per-kind.</p>
 */
final class MidnightNavyIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String LOCATION = "location";
    static final String LINKEDIN = "linkedin";
    static final String WEBSITE = "website";

    private static final String ICON_ROOT = "/templates/cv/midnight-navy/icons/";

    /** The width each mark is drawn at, as the design sizes it. */
    private static final Map<String, Double> SIZES = Map.of(
            PHONE, 8.5,
            EMAIL, 8.5,
            LOCATION, 8.5,
            LINKEDIN, 8.5,
            WEBSITE, 8.5,
            "trophy", 15.0,
            "growth", 15.0,
            "award", 15.0);

    /** The marks a document may name on an achievement. */
    static final Set<String> ENTRY_TOKENS = Set.of("trophy", "growth", "award");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private MidnightNavyIcons() {
    }

    /**
     * The width one mark is drawn at.
     *
     * @param token one of the packaged tokens
     * @return the drawn width in points
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error
     */
    static double size(String token) {
        Double size = SIZES.get(token);
        if (size == null) {
            throw new IllegalArgumentException(
                    "Unknown midnight navy CV icon token '" + token
                            + "'. This preset packages " + ENTRY_TOKENS.stream().sorted().toList()
                            + " for an achievement mark.");
        }
        return size;
    }

    /**
     * Reads one mark.
     *
     * @param token one of the packaged tokens
     * @return the parsed icon
     */
    static SvgIcon icon(String token) {
        size(token);
        return CACHE.computeIfAbsent(token, MidnightNavyIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = MidnightNavyIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing midnight navy CV icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read midnight navy CV icon: " + resourcePath, e);
        }
    }
}
