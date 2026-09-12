package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.image.DocumentImageData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Serif Headline CV marks — the five contact
 * glyphs, the two project marks, the certification medal and the three
 * achievement marks.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/serif-headline/icons/} as PNG, each at the point size
 * the design draws it at: a mark set inside a plate is drawn smaller than
 * one standing on its own, and a contact glyph smaller again.</p>
 *
 * <p>The project and achievement marks are the vocabulary a document names
 * through {@code CvEntry.icon()}. It is scoped to this preset — another
 * preset packages its own set — so {@link #ENTRY_TOKENS} is the list a
 * document can choose from here.</p>
 */
final class SerifHeadlineIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String LOCATION = "location";
    static final String LINKEDIN = "linkedin";
    static final String GITHUB = "github";

    /** The mark the certification plates always take; not data-driven. */
    static final String MEDAL = "medal";

    private static final String ICON_ROOT = "/templates/cv/serif-headline/icons/";

    private static final Map<String, Double> SIZES = Map.ofEntries(
            Map.entry(PHONE, 8.7),
            Map.entry(EMAIL, 8.7),
            Map.entry(LOCATION, 9.6),
            Map.entry(LINKEDIN, 8.7),
            Map.entry(GITHUB, 9.0),
            Map.entry("cart", 15.1),
            Map.entry("api", 15.1),
            Map.entry(MEDAL, 16.3),
            Map.entry("trophy", 22.7),
            Map.entry("chart", 22.7),
            Map.entry("rocket", 22.7));

    /**
     * The tokens a document may name on an entry: the two project marks and
     * the three achievement marks.
     */
    static final Set<String> ENTRY_TOKENS = Set.of("cart", "api", "trophy", "chart", "rocket");

    private static final Map<String, DocumentImageData> CACHE = new ConcurrentHashMap<>();

    private SerifHeadlineIcons() {
    }

    /**
     * The drawn size of one mark.
     *
     * @param token one of the packaged tokens
     * @return the size in points
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error
     */
    static double size(String token) {
        Double size = SIZES.get(token);
        if (size == null) {
            throw new IllegalArgumentException(
                    "Unknown serif headline CV icon token '" + token
                            + "'. This preset packages " + ENTRY_TOKENS.stream().sorted().toList()
                            + " for an entry mark.");
        }
        return size;
    }

    /**
     * Reads one mark, caching the decoded image per token so a sheet that
     * repeats a mark reads the file once per JVM.
     *
     * @param token one of the packaged tokens
     * @return the image data
     */
    static DocumentImageData image(String token) {
        // Validated here too, not only in size(): the marks drawn bare are
        // scaled to their box rather than to their own size, so without this
        // an unknown token would surface as a missing resource instead of as
        // the data error it is.
        size(token);
        return CACHE.computeIfAbsent(token, SerifHeadlineIcons::read);
    }

    private static DocumentImageData read(String token) {
        String resourcePath = ICON_ROOT + token + ".png";
        try (InputStream input = SerifHeadlineIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing serif headline CV icon: " + resourcePath);
            }
            return DocumentImageData.fromBytes(input.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read serif headline CV icon: " + resourcePath, e);
        }
    }
}
