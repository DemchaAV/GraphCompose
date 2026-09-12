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
 * Classpath loader for the Violet Grid CV marks: the five contact glyphs, the
 * six skill marks, the two project glyphs, the graduation cap and the
 * quotation mark that opens the closing band.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/violet-grid/icons/} as SVG, so they scale with the box
 * they are drawn into rather than being resampled. Parsed icons are cached
 * per token, so a sheet that repeats a mark reads each file once per JVM.</p>
 *
 * <p>{@link #ENTRY_TOKENS} is the vocabulary a document names through
 * {@code CvEntry.icon()}. It is scoped to this preset — another preset
 * packages its own set — and each mark carries the width it is drawn at,
 * because an icon's box is not its glyph and the sizes are per-kind.</p>
 */
final class VioletGridIcons {

    static final String EMAIL = "email";
    static final String PHONE = "phone";
    static final String LOCATION = "location";
    static final String WEBSITE = "website";
    static final String LINKEDIN = "linkedin";
    static final String GRADUATION = "graduation";
    static final String QUOTE = "quote";

    private static final String ICON_ROOT = "/templates/cv/violet-grid/icons/";

    /** The width each mark is drawn at, as the asset resolver chose it. */
    private static final Map<String, Double> SIZES = Map.ofEntries(
            Map.entry(EMAIL, 11.5),
            Map.entry(PHONE, 11.5),
            Map.entry(LOCATION, 11.5),
            Map.entry(WEBSITE, 11.5),
            Map.entry(LINKEDIN, 11.5),
            Map.entry("ux-research", 20.5),
            Map.entry("information-architecture", 20.5),
            Map.entry("wireframing", 20.5),
            Map.entry("prototyping", 20.5),
            Map.entry("usability-testing", 20.5),
            Map.entry("design-systems", 20.5),
            Map.entry("project-wallet", 28.0),
            Map.entry("project-health", 28.0),
            Map.entry(GRADUATION, 26.0),
            Map.entry(QUOTE, 14.1));

    /** The marks a document may name on a skill, a project or a degree. */
    static final Set<String> ENTRY_TOKENS = Set.of(
            "ux-research", "information-architecture", "wireframing", "prototyping",
            "usability-testing", "design-systems", "project-wallet", "project-health",
            GRADUATION);

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private VioletGridIcons() {
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
                    "Unknown violet grid CV icon token '" + token
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
        return CACHE.computeIfAbsent(token, VioletGridIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = VioletGridIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing violet grid CV icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read violet grid CV icon: " + resourcePath, e);
        }
    }
}
