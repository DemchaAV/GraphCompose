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
 * Classpath loader for the Charcoal Gold CV marks — the five contact glyphs
 * in the sidebar and the four credential marks in the closing columns.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/charcoal-gold/icons/} as SVG, so they scale with the
 * box they are drawn into rather than being resampled. Parsed icons are
 * cached per token, so a sheet that repeats a mark reads each file once per
 * JVM.</p>
 *
 * <p>The credential marks are the vocabulary a document names through
 * {@code CvEntry.icon()}. It is scoped to this preset — another preset
 * packages its own set — so {@link #ENTRY_TOKENS} is the list a document can
 * choose from here.</p>
 */
final class CharcoalGoldIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String LOCATION = "location";
    static final String WEBSITE = "website";
    static final String LINKEDIN = "linkedin";

    private static final String ICON_ROOT = "/templates/cv/charcoal-gold/icons/";

    /** Contact glyphs are set at the size of the text beside them. */
    static final double CONTACT_SIZE = 9.0;

    /** A credential mark stands on its own, so it is drawn larger. */
    static final double CREDENTIAL_SIZE = 15.0;

    /** The marks a document may name on a credential entry. */
    static final Set<String> ENTRY_TOKENS =
            Set.of("certificate", "trophy", "growth", "star");

    private static final Set<String> TOKENS =
            Set.of(PHONE, EMAIL, LOCATION, WEBSITE, LINKEDIN,
                    "certificate", "trophy", "growth", "star");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private CharcoalGoldIcons() {
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
                    "Unknown charcoal gold CV icon token '" + token
                            + "'. This preset packages " + ENTRY_TOKENS.stream().sorted().toList()
                            + " for a credential mark.");
        }
        return CACHE.computeIfAbsent(token, CharcoalGoldIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = CharcoalGoldIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing charcoal gold CV icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read charcoal gold CV icon: " + resourcePath, e);
        }
    }
}
