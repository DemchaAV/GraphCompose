package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Teal Pulse CV marks: the two halves of the brand
 * mark, the four contact glyphs, the five badge glyphs and the heart that
 * closes the sheet.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/teal-pulse/icons/} as SVG, so they scale with the box
 * they are drawn into rather than being resampled. Parsed icons are cached
 * per token, so a sheet that repeats a mark reads each file once per JVM.</p>
 *
 * <p>Every mark here is chrome — this sheet draws the same ones whatever the
 * CV says — so the tokens are constants and no document names one. Each
 * carries the width it is drawn at: an icon's box is not its glyph, and the
 * sizes are measurements off the drawing rather than a shared constant.</p>
 */
final class TealPulseIcons {

    static final String BRAND_HEART = "brand-heart";
    static final String BRAND_PULSE = "brand-pulse";
    static final String LOCATION = "location";
    static final String EMAIL = "email";
    static final String PHONE = "phone";
    static final String LINKEDIN = "linkedin";
    static final String SUMMARY = "summary";
    static final String EXPERIENCE = "experience";
    static final String EDUCATION = "education";
    static final String CERTIFICATIONS = "certifications";
    static final String ADDITIONAL = "additional";
    static final String HEART = "heart";

    private static final String ICON_ROOT = "/templates/cv/teal-pulse/icons/";

    /**
     * The width each mark is drawn at — its whole box, not the glyph inside,
     * which sits a few points within it. These are points rather than
     * measurements off the drawing: the asset resolver chose them per glyph so
     * that each one's visible ink matches what the design shows.
     */
    private static final Map<String, Double> SIZES = Map.ofEntries(
            Map.entry(BRAND_HEART, 77.1),
            Map.entry(BRAND_PULSE, 92.8),
            Map.entry(LOCATION, 15.6),
            Map.entry(EMAIL, 16.2),
            Map.entry(PHONE, 14.3),
            Map.entry(LINKEDIN, 13.6),
            Map.entry(SUMMARY, 20.0),
            Map.entry(EXPERIENCE, 20.0),
            Map.entry(EDUCATION, 17.5),
            Map.entry(CERTIFICATIONS, 17.5),
            Map.entry(ADDITIONAL, 17.5),
            Map.entry(HEART, 14.9));

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private TealPulseIcons() {
    }

    /**
     * The width one mark is drawn at.
     *
     * @param token one of the packaged tokens
     * @return the drawn width in points
     */
    static double size(String token) {
        Double size = SIZES.get(token);
        if (size == null) {
            throw new IllegalStateException("No size for teal pulse CV icon '" + token + "'");
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
        return CACHE.computeIfAbsent(token, TealPulseIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = TealPulseIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing teal pulse CV icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read teal pulse CV icon: " + resourcePath, e);
        }
    }
}
