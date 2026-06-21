package com.demcha.compose.emoji;

/**
 * Marker and metadata for the {@code graph-compose-emoji} companion artifact.
 *
 * <p>This artifact carries colour-emoji SVG glyphs (under {@code emoji/svg/}) and
 * a shortcode index ({@code emoji/emoji-index.properties}) that the engine's
 * {@code com.demcha.compose.document.emoji.EmojiLibrary} resolves from the
 * classpath. It is split out of the core {@code graph-compose} jar — modelled on
 * {@code graph-compose-fonts} — so the engine stays lean and the emoji set
 * releases on its own cadence.</p>
 *
 * <p>There is no API to call here: simply having this jar on the classpath makes
 * the bundled glyphs resolvable. The resolver, the {@code RichText.emoji(...)}
 * DSL and all logic live in the engine, so this class intentionally depends on
 * nothing from the engine.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public final class GraphComposeEmoji {

    /**
     * Classpath resource prefix under which the emoji set is packaged: glyphs at
     * {@code emoji/svg/<codepoint>.svg} and the shortcode index at
     * {@code emoji/emoji-index.properties}.
     */
    public static final String RESOURCE_ROOT = "emoji/";

    /**
     * Classpath location of the shortcode &rarr; codepoint index that
     * {@code EmojiLibrary} loads to resolve names like {@code ":star:"}.
     */
    public static final String INDEX_RESOURCE = RESOURCE_ROOT + "emoji-index.properties";

    private GraphComposeEmoji() {
    }

    /**
     * Reports whether the emoji set is reachable on the current classpath. A
     * {@code true} result means this artifact (or an equivalent resource set) is
     * present and {@code EmojiLibrary} will resolve shortcodes; {@code false}
     * means emoji shortcodes cannot be resolved (callers fall back to text).
     *
     * @return {@code true} if the emoji index is on the classpath
     */
    public static boolean isAvailable() {
        return GraphComposeEmoji.class.getClassLoader().getResource(INDEX_RESOURCE) != null;
    }
}
