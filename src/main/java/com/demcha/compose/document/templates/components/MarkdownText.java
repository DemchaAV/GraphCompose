package com.demcha.compose.document.templates.components;

import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight inline-markdown parser for Templates v2.
 *
 * <p>Translates a string containing the two most common inline markdown
 * markers ({@code **bold**} and {@code *italic*} / {@code _italic_})
 * into a list of {@link InlineRun} values with appropriate
 * {@link DocumentTextDecoration} flags. The base
 * {@link DocumentTextStyle} provides the font, size, and colour for
 * plain runs; bold and italic markers produce styled runs whose
 * decoration is {@code BOLD} or {@code ITALIC} respectively, with
 * the same font / size / colour as the base.</p>
 *
 * <p>Strings without markdown markers produce a single
 * {@code InlineTextRun} with the base style — i.e. the parser is a
 * no-op safe identity for plain text. This makes it cheap to invoke
 * unconditionally on every body string.</p>
 *
 * <p>Markers must not nest (e.g. {@code ***bold-italic***} is treated
 * as plain text); use explicit {@code **bold _italic_ end**}
 * concatenation if you need both decorations on adjacent runs. The
 * parser handles unbalanced markers gracefully — an unclosed
 * {@code **} is rendered as the literal characters.</p>
 *
 * <p>The intent is "neural-network-friendly" rich text: an LLM emits a
 * resume bullet like {@code "**Java 21**, SQL, Kotlin"} and the
 * preset renders it with Java 21 in bold, no separate API call
 * required.</p>
 */
public final class MarkdownText {

    private MarkdownText() {
        // utility class — not instantiable
    }

    /**
     * Parses {@code text} into a list of styled inline runs.
     *
     * <p>If {@code text} contains no markdown markers, returns a
     * single-element list with the original text and the base style.
     * Multiple runs are emitted only when bold or italic markers are
     * present.</p>
     *
     * @param text      input string; may be null (treated as empty)
     * @param baseStyle base text style applied to plain runs and
     *                  preserved (other than font name) for bold /
     *                  italic runs; may be null (no styling applied)
     * @return non-empty immutable list of inline runs
     */
    public static List<InlineRun> parse(String text, DocumentTextStyle baseStyle) {
        String source = text == null ? "" : text;
        List<InlineRun> runs = new ArrayList<>();
        int i = 0;
        StringBuilder plain = new StringBuilder();

        while (i < source.length()) {
            // Bold: **...**
            if (i + 1 < source.length()
                    && source.charAt(i) == '*' && source.charAt(i + 1) == '*') {
                int end = source.indexOf("**", i + 2);
                if (end > i + 2) {
                    flushPlain(plain, baseStyle, runs);
                    runs.add(new InlineTextRun(
                            source.substring(i + 2, end),
                            withDecoration(baseStyle, Decoration.BOLD)));
                    i = end + 2;
                    continue;
                }
            }
            // Italic: *...*  or  _..._
            char ch = source.charAt(i);
            if ((ch == '*' || ch == '_') && i + 1 < source.length()) {
                // Avoid eating a literal "**" — we already handled bold
                if (ch == '*' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                    plain.append(ch);
                    i++;
                    continue;
                }
                int end = source.indexOf(ch, i + 1);
                if (end > i + 1) {
                    flushPlain(plain, baseStyle, runs);
                    runs.add(new InlineTextRun(
                            source.substring(i + 1, end),
                            withDecoration(baseStyle, Decoration.ITALIC)));
                    i = end + 1;
                    continue;
                }
            }
            plain.append(ch);
            i++;
        }
        flushPlain(plain, baseStyle, runs);

        if (runs.isEmpty()) {
            // Always return at least one run so the caller never has to
            // null-check the empty case.
            runs.add(new InlineTextRun(source, baseStyle));
        }
        return List.copyOf(runs);
    }

    private static void flushPlain(StringBuilder buf, DocumentTextStyle style,
                                    List<InlineRun> runs) {
        if (buf.length() == 0) {
            return;
        }
        runs.add(new InlineTextRun(buf.toString(), style));
        buf.setLength(0);
    }

    private enum Decoration {
        BOLD,
        ITALIC
    }

    private static DocumentTextStyle withDecoration(DocumentTextStyle base, Decoration deco) {
        if (base == null) {
            return null;
        }
        DocumentTextDecoration target = deco == Decoration.BOLD
                ? DocumentTextDecoration.BOLD
                : DocumentTextDecoration.ITALIC;
        return DocumentTextStyle.builder()
                .fontName(base.fontName())
                .size(base.size())
                .decoration(target)
                .color(base.color())
                .build();
    }
}
