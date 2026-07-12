/**
 * Colour-emoji resolution for the canonical document model.
 *
 * <p>The entry point is {@link com.demcha.compose.document.emoji.EmojiLibrary},
 * which maps GitHub-style shortcodes (e.g. {@code ":star:"}) to parsed
 * {@link com.demcha.compose.document.svg.SvgIcon} glyphs and backs the
 * {@code RichText.emoji(...)} / {@code ParagraphBuilder.inlineEmoji(...)} DSL. It is
 * data-driven from the classpath layout {@code emoji/emoji-index.properties}
 * + {@code emoji/svg/<codepoint>.svg} shipped by the independently-versioned
 * {@code graph-compose-emoji} companion artifact; the engine carries no emoji
 * art and has no Maven dependency on it, exactly like
 * {@code com.demcha.compose.font.DefaultFonts} and {@code graph-compose-fonts}.</p>
 *
 * <p>Resolution is lenient by contract: an unknown shortcode or an absent emoji
 * set yields an empty result so callers fall back to literal text, the way
 * GitHub renders an unrecognised {@code :code:}.</p>
 *
 * @since 1.9.0
 */
package com.demcha.compose.document.emoji;
