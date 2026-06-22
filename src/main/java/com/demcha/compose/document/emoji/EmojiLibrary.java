package com.demcha.compose.document.emoji;

import com.demcha.compose.document.svg.SvgIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves emoji shortcodes (e.g. {@code ":star:"}) to parsed {@link SvgIcon}s,
 * loaded from the classpath layout shipped by the {@code graph-compose-emoji}
 * companion artifact: a {@code emoji/emoji-index.properties}
 * ({@code shortcode=codepoint}) plus one glyph per codepoint at
 * {@code emoji/svg/<codepoint>.svg}.
 *
 * <p>The engine carries no emoji art and has no Maven dependency on the emoji
 * module — exactly like {@code DefaultFonts} and {@code graph-compose-fonts}.
 * This resolver is fully data-driven: any classpath providing that layout works,
 * so the bundled Noto Emoji set (SIL OFL 1.1) can be swapped for another emoji
 * set by changing the classpath alone, with no code change.</p>
 *
 * <p>Resolution is lenient by design — {@link #find(String)} returns an empty
 * {@link Optional} for an unknown shortcode or when no emoji set is on the
 * classpath, so callers (e.g. a {@code :shortcode:} markdown pass) can fall back
 * to literal text the way GitHub does. {@link #require(String)} is the strict
 * variant. Parsed icons are cached per codepoint; the instance is thread-safe.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public final class EmojiLibrary {

    private static final Logger LOG = LoggerFactory.getLogger(EmojiLibrary.class);

    private static final String INDEX_RESOURCE = "emoji/emoji-index.properties";
    private static final String SVG_PREFIX = "emoji/svg/";
    private static final String SVG_SUFFIX = ".svg";

    private static final EmojiLibrary DEFAULT = new EmojiLibrary(EmojiLibrary.class.getClassLoader());

    private final ClassLoader loader;
    private final Map<String, SvgIcon> iconCache = new ConcurrentHashMap<>();
    private volatile Map<String, String> shortcodeToCodepoint;

    /**
     * Creates a library resolving against the given class loader.
     *
     * @param loader class loader whose resources carry the emoji layout; must not be {@code null}
     */
    public EmojiLibrary(ClassLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    /**
     * Returns the process-wide default library, resolving emoji from the
     * application classpath (the {@code graph-compose-emoji} artifact when
     * present).
     *
     * @return the shared default library
     */
    public static EmojiLibrary getDefault() {
        return DEFAULT;
    }

    /**
     * Reports whether an emoji set is reachable on this library's classpath.
     *
     * @return {@code true} if {@code emoji/emoji-index.properties} is present
     */
    public boolean isAvailable() {
        return loader.getResource(INDEX_RESOURCE) != null;
    }

    /**
     * Resolves a shortcode to its glyph, leniently. The shortcode is matched
     * case-insensitively with optional surrounding colons ({@code ":star:"} and
     * {@code "star"} are equivalent).
     *
     * @param shortcode emoji shortcode, with or without surrounding colons
     * @return the parsed glyph, or empty if the shortcode is unknown, the glyph
     * is missing, or no emoji set is on the classpath
     */
    public Optional<SvgIcon> find(String shortcode) {
        String name = normalize(shortcode);
        if (name == null) {
            return Optional.empty();
        }
        String codepoint = index().get(name);
        if (codepoint == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(iconForCodepoint(codepoint));
    }

    /**
     * Resolves a shortcode to its glyph, strictly.
     *
     * @param shortcode emoji shortcode, with or without surrounding colons
     * @return the parsed glyph
     * @throws IllegalArgumentException if the shortcode cannot be resolved; the
     *                                  message names the {@code graph-compose-emoji}
     *                                  artifact when no emoji set is present
     */
    public SvgIcon require(String shortcode) {
        Optional<SvgIcon> icon = find(shortcode);
        if (icon.isPresent()) {
            return icon.get();
        }
        String name = normalize(shortcode);
        String message;
        if (!isAvailable()) {
            message = "No emoji set on the classpath: add the graph-compose-emoji artifact "
                      + "(or equivalent emoji/ resources) to resolve \"" + shortcode + "\"";
        } else if (name != null && index().containsKey(name)) {
            message = "Emoji \"" + shortcode + "\" is indexed but its glyph could not be rendered";
        } else {
            message = "Unknown emoji shortcode \"" + shortcode + "\" (not in emoji-index.properties)";
        }
        throw new IllegalArgumentException(message);
    }

    private SvgIcon iconForCodepoint(String codepoint) {
        // computeIfAbsent records nothing when the mapping function returns null,
        // so a missing or unparseable glyph stays unresolved (and is retried)
        // without NPE — callers then fall back to literal text.
        return iconCache.computeIfAbsent(codepoint, cp -> {
            String xml;
            try (InputStream in = loader.getResourceAsStream(SVG_PREFIX + cp + SVG_SUFFIX)) {
                if (in == null) {
                    return null;
                }
                xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read emoji glyph for codepoint " + cp, e);
            }
            try {
                return SvgIcon.parse(xml);
            } catch (RuntimeException e) {
                // A real-world glyph may use an SVG feature the parser rejects;
                // treat it as unresolved rather than failing the whole render.
                LOG.debug("emoji glyph {} could not be parsed: {}", cp, e.toString());
                return null;
            }
        });
    }

    private Map<String, String> index() {
        Map<String, String> idx = shortcodeToCodepoint;
        if (idx == null) {
            synchronized (this) {
                idx = shortcodeToCodepoint;
                if (idx == null) {
                    idx = loadIndex();
                    shortcodeToCodepoint = idx;
                }
            }
        }
        return idx;
    }

    private Map<String, String> loadIndex() {
        try (InputStream in = loader.getResourceAsStream(INDEX_RESOURCE)) {
            if (in == null) {
                return Map.of();
            }
            Properties props = new Properties();
            props.load(in);
            Map<String, String> resolved = new HashMap<>();
            for (String name : props.stringPropertyNames()) {
                String codepoint = props.getProperty(name);
                if (codepoint != null && !codepoint.isBlank()) {
                    resolved.put(name.trim().toLowerCase(Locale.ROOT),
                            codepoint.trim().toLowerCase(Locale.ROOT));
                }
            }
            return Map.copyOf(resolved);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + INDEX_RESOURCE, e);
        }
    }

    private static String normalize(String shortcode) {
        if (shortcode == null) {
            return null;
        }
        String trimmed = shortcode.trim();
        if (trimmed.startsWith(":") && trimmed.endsWith(":") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        trimmed = trimmed.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
