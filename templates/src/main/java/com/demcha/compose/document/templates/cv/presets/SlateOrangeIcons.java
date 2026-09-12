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
 * Classpath loader for the Slate Orange CV marks: the four contact glyphs,
 * the ten competency marks, the trophy the achievements share, and the three
 * marks the closing block draws.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/slate-orange/icons/} as SVG, so they scale with the box
 * they are drawn into rather than being resampled. Parsed icons are cached
 * per token, so a sheet that repeats a mark reads each file once per JVM.</p>
 *
 * <p>{@link #ENTRY_TOKENS} is the vocabulary a document names through
 * {@code CvEntry.icon()}. It is scoped to this preset — another preset
 * packages its own set — and an entry with no token is drawn without a mark.
 * Each mark carries the width it is drawn at: an icon's box is not its glyph,
 * and the sizes are per-glyph rather than one shared number.</p>
 */
final class SlateOrangeIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String LINKEDIN = "linkedin";
    static final String LOCATION = "location";
    static final String ACHIEVEMENT = "achievement";

    private static final String ICON_ROOT = "/templates/cv/slate-orange/icons/";

    /** The width each mark is drawn at, as the asset resolver chose it. */
    private static final Map<String, Double> SIZES = Map.ofEntries(
            Map.entry(PHONE, 10.2),
            Map.entry(EMAIL, 10.2),
            Map.entry(LINKEDIN, 10.2),
            Map.entry(LOCATION, 10.2),
            Map.entry("customer-service", 12.4),
            Map.entry("operations", 12.4),
            Map.entry("calendar", 12.4),
            Map.entry("reporting", 12.4),
            Map.entry("communication", 12.4),
            Map.entry("problem-solving", 12.4),
            Map.entry("sales", 12.4),
            Map.entry("office-suite", 12.4),
            Map.entry("crm", 12.4),
            Map.entry("time-management", 12.4),
            Map.entry(ACHIEVEMENT, 13.0),
            Map.entry("availability", 11.8),
            Map.entry("relocation", 11.8),
            Map.entry("remote-work", 11.8));

    /** The marks a document may name on a competency or a closing fact. */
    static final Set<String> ENTRY_TOKENS = Set.of(
            "customer-service", "operations", "calendar", "reporting", "communication",
            "problem-solving", "sales", "office-suite", "crm", "time-management",
            ACHIEVEMENT, "availability", "relocation", "remote-work");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private SlateOrangeIcons() {
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
                    "Unknown slate orange CV icon token '" + token
                            + "'. This preset packages " + ENTRY_TOKENS.stream().sorted().toList()
                            + " for an entry mark.");
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
        return CACHE.computeIfAbsent(token, SlateOrangeIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = SlateOrangeIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing slate orange CV icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read slate orange CV icon: " + resourcePath, e);
        }
    }
}
