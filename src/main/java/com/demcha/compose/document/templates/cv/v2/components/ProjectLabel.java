package com.demcha.compose.document.templates.cv.v2.components;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits legacy project labels like {@code "GraphCompose (Java, PDFBox)"}
 * into a display title and a parenthesised technology stack.
 *
 * <p>Since v1.6.8 the {@code title} field <strong>preserves inline
 * Markdown syntax</strong> (notably {@code [text](url)} hyperlinks)
 * so callers can route it through
 * {@link MarkdownInline#append(com.demcha.compose.document.dsl.RichText,
 * String, com.demcha.compose.document.style.DocumentTextStyle)} and
 * render the title as a clickable link. Callers that only need the
 * visible text (e.g. for plain-text exports) should pass {@link #title()}
 * back through {@link MarkdownInline#plainText(String)}.</p>
 *
 * <p>The {@code stack} segment (the trailing {@code "(Java, PDFBox)"}
 * fragment) is always plain text — link syntax inside the stack is not
 * supported because the regex requires the stack content to contain
 * no parentheses.</p>
 */
public record ProjectLabel(String title, String stack) {

    /**
     * Trailing {@code " (stack)"} pattern at the end of the label.
     * The stack body forbids inner parentheses so we never mistake
     * a link URL's {@code (...)} segment for a stack delimiter when
     * the label opens with a Markdown link like {@code [name](url) (Java)}.
     */
    private static final Pattern TRAILING_STACK =
            Pattern.compile("\\s+\\(([^()]*)\\)\\s*$");

    public ProjectLabel {
        title = title == null ? "" : title;
        stack = stack == null ? "" : stack;
    }

    /**
     * Parses a project label, separating the title from an optional
     * trailing stack fragment.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "GraphCompose (Java, PDFBox)"} &rarr;
     *       title={@code "GraphCompose"}, stack={@code "Java, PDFBox"}</li>
     *   <li>{@code "[GraphCompose](https://gc) (Java, PDFBox)"} &rarr;
     *       title={@code "[GraphCompose](https://gc)"} (with Markdown
     *       intact), stack={@code "Java, PDFBox"}</li>
     *   <li>{@code "GraphCompose"} &rarr;
     *       title={@code "GraphCompose"}, stack={@code ""}</li>
     *   <li>{@code "[GraphCompose](https://gc)"} &rarr;
     *       title={@code "[GraphCompose](https://gc)"}, stack={@code ""}
     *       (the URL's parens do not match the stack pattern because
     *       there is no whitespace before the opening paren)</li>
     * </ul>
     *
     * @param value raw label string, possibly with inline Markdown
     *              link syntax; null treated as empty
     * @return parsed label
     */
    public static ProjectLabel parse(String value) {
        if (value == null) {
            return new ProjectLabel("", "");
        }
        String trimmed = value.trim();
        Matcher trailing = TRAILING_STACK.matcher(trimmed);
        if (trailing.find()) {
            String title = trimmed.substring(0, trailing.start()).trim();
            String stack = trailing.group(1).trim();
            return new ProjectLabel(title, stack);
        }
        return new ProjectLabel(trimmed, "");
    }
}
