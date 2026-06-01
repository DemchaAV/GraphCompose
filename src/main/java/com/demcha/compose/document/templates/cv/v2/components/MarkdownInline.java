package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.components.MarkdownText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny adapter that pushes inline-markdown-parsed runs of {@code text}
 * into a {@link RichText} builder using {@code baseStyle} for plain
 * (non-emphasised) segments.
 *
 * <p>Honours {@code **bold**}, {@code *italic*}, {@code _italic_} via
 * the shared {@link MarkdownText} parser. Lives in the components
 * layer because every body / row / entry renderer calls it.</p>
 *
 * <p><strong>Inline links (since v1.6.8).</strong> Recognises the
 * standard Markdown {@code [label](url)} syntax and emits a clickable
 * hyperlink run via {@link RichText#link(String, String)}. The link
 * pattern has higher precedence than emphasis: emphasis inside the
 * {@code [...]} label is rendered as plain link text in this v1
 * implementation. Emphasis outside the link continues to work as
 * before. Square-bracket fragments without a following {@code (url)}
 * stay as literal text.</p>
 *
 * <p>{@link #plainText(String)} also strips link syntax so callers
 * that only care about the visible label (e.g. {@code ProjectLabel.
 * parse}) keep getting a clean title.</p>
 */
public final class MarkdownInline {

    /**
     * Matches {@code [text](url)}. The text capture allows any
     * non-bracket characters (no nesting). The URL capture forbids
     * parentheses and whitespace so we do not greedily eat across
     * adjacent links.
     */
    private static final Pattern LINK_PATTERN =
            Pattern.compile("\\[([^\\[\\]]*)\\]\\(([^()\\s]+)\\)");

    private MarkdownInline() {
    }

    /**
     * Appends {@code text} to {@code rich}, expanding inline markdown.
     *
     * <p>Order of processing:</p>
     * <ol>
     *   <li>Scan for {@code [label](url)} matches; emit each match as
     *       a {@link RichText#link(String, String) hyperlink run}.</li>
     *   <li>Pass every plain segment between (or surrounding) link
     *       matches through {@link MarkdownText} for {@code **bold**}
     *       / {@code *italic*} / {@code _italic_} expansion.</li>
     * </ol>
     *
     * @param rich      target rich-text builder
     * @param text      source string; null treated as empty
     * @param baseStyle style applied to plain runs
     */
    public static void append(RichText rich, String text,
                              DocumentTextStyle baseStyle) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher matcher = LINK_PATTERN.matcher(text);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                appendEmphasis(rich, text.substring(cursor, matcher.start()), baseStyle);
            }
            rich.link(matcher.group(1), matcher.group(2));
            cursor = matcher.end();
        }
        if (cursor < text.length()) {
            appendEmphasis(rich, text.substring(cursor), baseStyle);
        }
    }

    /**
     * Trims surrounding whitespace before delegating to
     * {@link #append(RichText, String, DocumentTextStyle)}.
     *
     * @param rich      target rich-text builder
     * @param text      source string; null treated as empty
     * @param baseStyle style applied to plain runs
     */
    public static void appendTrimmed(RichText rich, String text,
                                     DocumentTextStyle baseStyle) {
        append(rich, text == null ? "" : text.trim(), baseStyle);
    }

    /**
     * Appends {@code prefix + plainText(value)} only when the
     * plain-text projection is non-blank. Used by renderers that
     * label optional supplementary content like {@code " (since
     * 2024)"} segments.
     *
     * @param rich   target rich-text builder
     * @param prefix prefix to attach before the cleaned value
     * @param value  source string; null treated as empty
     * @param style  style applied to the combined run
     */
    public static void appendPlainIfPresent(RichText rich, String prefix,
                                            String value,
                                            DocumentTextStyle style) {
        String clean = plainText(value);
        if (!clean.isBlank()) {
            rich.style(prefix + clean, style);
        }
    }

    /**
     * Returns a plain-text projection of {@code value} with inline
     * Markdown syntax removed: {@code [label](url)} collapses to
     * just {@code label}; emphasis markers (asterisks, underscores,
     * backticks) are stripped. {@code null} is treated as the empty
     * string.
     *
     * @param value source string
     * @return cleaned plain-text projection
     */
    public static String plainText(String value) {
        if (value == null) {
            return "";
        }
        String stripped = LINK_PATTERN.matcher(value).replaceAll("$1");
        return stripped
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    /**
     * Pipes a non-link segment through the emphasis parser. Split
     * out so that the link path stays a single delegation to
     * {@link RichText#link(String, String)} and the read of
     * {@code append} reflects the two-pass design directly.
     */
    private static void appendEmphasis(RichText rich, String text,
                                       DocumentTextStyle baseStyle) {
        if (text.isEmpty()) {
            return;
        }
        for (InlineRun run : MarkdownText.parse(text, baseStyle)) {
            if (!(run instanceof InlineTextRun textRun)) {
                continue;
            }
            DocumentTextStyle runStyle = textRun.textStyle() == null
                    ? baseStyle
                    : textRun.textStyle();
            rich.style(textRun.text(), runStyle);
        }
    }
}
