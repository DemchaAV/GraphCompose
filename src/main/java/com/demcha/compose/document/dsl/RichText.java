package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for mixed-style text inside a single paragraph.
 *
 * <p>Use this when a paragraph needs more than one styled segment — for
 * example label/value pairs, status badges inline with body copy, or accented
 * keywords. Each chained call appends one {@link InlineTextRun} to the
 * builder; {@link #runs()} produces the immutable list that
 * {@link ParagraphBuilder#rich(RichText)} accepts.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * RichText line = RichText.text("Status: ")
 *         .bold("Pending")
 *         .plain(" — last review on ")
 *         .accent("Mar 14", DocumentColor.of(new Color(40, 90, 180)));
 *
 * section.addRich(line);
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class RichText {
    private final List<InlineTextRun> runs = new ArrayList<>();

    private RichText() {
    }

    /**
     * Starts a rich text builder with no runs.
     *
     * @return empty rich text builder
     */
    public static RichText empty() {
        return new RichText();
    }

    /**
     * Starts a rich text builder seeded with one plain text run.
     *
     * @param text first plain text fragment
     * @return rich text builder ready for chained calls
     */
    public static RichText text(String text) {
        return new RichText().plain(text);
    }

    /**
     * Appends a plain (paragraph-style) text run.
     *
     * @param text text fragment
     * @return this builder
     */
    public RichText plain(String text) {
        runs.add(new InlineTextRun(text == null ? "" : text));
        return this;
    }

    /**
     * Appends a bold text run.
     *
     * @param text text fragment
     * @return this builder
     */
    public RichText bold(String text) {
        return decorated(text, DocumentTextDecoration.BOLD);
    }

    /**
     * Appends an italic text run.
     *
     * @param text text fragment
     * @return this builder
     */
    public RichText italic(String text) {
        return decorated(text, DocumentTextDecoration.ITALIC);
    }

    /**
     * Appends a bold-italic text run.
     *
     * @param text text fragment
     * @return this builder
     */
    public RichText boldItalic(String text) {
        return decorated(text, DocumentTextDecoration.BOLD_ITALIC);
    }

    /**
     * Appends an underlined text run.
     *
     * @param text text fragment
     * @return this builder
     */
    public RichText underline(String text) {
        return decorated(text, DocumentTextDecoration.UNDERLINE);
    }

    /**
     * Appends a strikethrough text run.
     *
     * @param text text fragment
     * @return this builder
     */
    public RichText strikethrough(String text) {
        return decorated(text, DocumentTextDecoration.STRIKETHROUGH);
    }

    /**
     * Appends a colored text run with the default decoration.
     *
     * @param text text fragment
     * @param color text color
     * @return this builder
     */
    public RichText color(String text, DocumentColor color) {
        return styled(text, DocumentTextStyle.builder().color(color).build());
    }

    /**
     * Convenience overload accepting a {@link Color}.
     *
     * @param text text fragment
     * @param color text color
     * @return this builder
     */
    public RichText color(String text, Color color) {
        return color(text, color == null ? null : DocumentColor.of(color));
    }

    /**
     * Appends a bold-and-colored "accent" text run — the typical pattern for
     * highlighting status keywords ("Pending", "Paid", "Overdue") inline.
     *
     * @param text text fragment
     * @param color accent color
     * @return this builder
     */
    public RichText accent(String text, DocumentColor color) {
        return styled(text, DocumentTextStyle.builder()
                .decoration(DocumentTextDecoration.BOLD)
                .color(color)
                .build());
    }

    /**
     * Convenience overload of {@link #accent(String, DocumentColor)} accepting a
     * {@link Color}.
     *
     * @param text text fragment
     * @param color accent color
     * @return this builder
     */
    public RichText accent(String text, Color color) {
        return accent(text, color == null ? null : DocumentColor.of(color));
    }

    /**
     * Appends a sized text run (e.g., for inline larger keyword) at the default
     * decoration and color.
     *
     * @param text text fragment
     * @param size font size in points
     * @return this builder
     */
    public RichText size(String text, double size) {
        return styled(text, DocumentTextStyle.builder().size(size).build());
    }

    /**
     * Appends a fully-styled text run.
     *
     * @param text text fragment
     * @param style explicit style for this run; when {@code null}, falls back
     *              to the paragraph default
     * @return this builder
     */
    public RichText style(String text, DocumentTextStyle style) {
        return appendRun(text, style, null);
    }

    /**
     * Appends a clickable link run with default link styling.
     *
     * @param text visible link text
     * @param options link metadata
     * @return this builder
     */
    public RichText link(String text, DocumentLinkOptions options) {
        return appendRun(text, null, options);
    }

    /**
     * Convenience link overload using a raw URI string.
     *
     * @param text visible link text
     * @param uri link target URI
     * @return this builder
     */
    public RichText link(String text, String uri) {
        return link(text, new DocumentLinkOptions(uri == null ? "" : uri));
    }

    /**
     * Appends a fully-customized run with both an explicit style and link
     * metadata.
     *
     * @param text text fragment
     * @param style explicit style or {@code null}
     * @param link link metadata or {@code null}
     * @return this builder
     */
    public RichText with(String text, DocumentTextStyle style, DocumentLinkOptions link) {
        return appendRun(text, style, link);
    }

    /**
     * Appends a single space, useful between styled keywords.
     *
     * @return this builder
     */
    public RichText space() {
        return plain(" ");
    }

    /**
     * Appends another rich-text builder's runs in source order.
     *
     * @param other another rich text builder
     * @return this builder
     */
    public RichText append(RichText other) {
        Objects.requireNonNull(other, "other");
        runs.addAll(other.runs);
        return this;
    }

    /**
     * Returns the accumulated runs as an immutable list.
     *
     * @return inline runs in source order
     */
    public List<InlineTextRun> runs() {
        return List.copyOf(runs);
    }

    /**
     * Returns the number of accumulated runs.
     *
     * @return run count
     */
    public int size() {
        return runs.size();
    }

    /**
     * Returns whether the builder has no runs yet.
     *
     * @return {@code true} when the builder is empty
     */
    public boolean isEmpty() {
        return runs.isEmpty();
    }

    private RichText decorated(String text, DocumentTextDecoration decoration) {
        return styled(text, DocumentTextStyle.builder().decoration(decoration).build());
    }

    private RichText styled(String text, DocumentTextStyle style) {
        return appendRun(text, style, null);
    }

    private RichText appendRun(String text, DocumentTextStyle style, DocumentLinkOptions link) {
        runs.add(new InlineTextRun(text == null ? "" : text, style, link));
        return this;
    }
}
