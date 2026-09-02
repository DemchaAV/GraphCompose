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
 * Classpath loader for the Terracotta Rail CV marks — the four contact
 * glyphs and the bullet in the sidebar, the three marks a fact block may
 * name, and the three a project may.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/terracotta-rail/icons/} as SVG, so they scale with the
 * box they are drawn into rather than being resampled. Parsed icons are
 * cached per token, so a sheet that repeats a mark reads each file once per
 * JVM.</p>
 *
 * <p>{@link #ENTRY_TOKENS} is the vocabulary a document names through
 * {@code CvEntry.icon()}. It is scoped to this preset — another preset
 * packages its own set — and an entry that names none is drawn without a
 * mark.</p>
 */
final class TerracottaRailIcons {

    static final String EMAIL = "email";
    static final String PHONE = "phone";
    static final String LOCATION = "location";
    static final String LINKEDIN = "linkedin";
    static final String GLOBE = "globe";
    static final String SQUARE = "square";

    private static final String ICON_ROOT = "/templates/cv/terracotta-rail/icons/";

    /** A contact glyph is set at the size of the line beside it. */
    static final double CONTACT_SIZE = 10.0;

    /** The competency bullet — a terracotta square, not a disc. */
    static final double BULLET_SIZE = 3.8;

    /** A fact's mark stands beside a two-line block, so it is drawn larger. */
    static final double FACT_SIZE = 16.0;

    /** A project's mark is the sketch that opens its row. */
    static final double PROJECT_SIZE = 22.0;

    /** The marks a document may name on a fact or a project entry. */
    static final Set<String> ENTRY_TOKENS =
            Set.of(GLOBE, "badge", "clock", "building", "hotel", "house");

    private static final Set<String> TOKENS =
            Set.of(EMAIL, PHONE, LOCATION, LINKEDIN, SQUARE,
                    GLOBE, "badge", "clock", "building", "hotel", "house");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private TerracottaRailIcons() {
    }

    /**
     * Reads one mark.
     *
     * @param token one of the packaged tokens
     * @return the parsed icon
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error
     */
    static SvgIcon icon(String token) {
        if (!TOKENS.contains(token)) {
            throw new IllegalArgumentException(
                    "Unknown terracotta rail CV icon token '" + token
                            + "'. This preset packages " + ENTRY_TOKENS.stream().sorted().toList()
                            + " for an entry mark.");
        }
        return CACHE.computeIfAbsent(token, TerracottaRailIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = TerracottaRailIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing terracotta rail CV icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read terracotta rail CV icon: " + resourcePath, e);
        }
    }
}
