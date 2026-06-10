package com.demcha.compose.document.dsl;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.InlineImageRun;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;

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
 * @since 1.0.0
 */
public final class RichText {
    private final List<InlineRun> runs = new ArrayList<>();

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
     * Appends an inline image run with default {@link InlineImageAlignment#CENTER}
     * alignment and zero offset.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     * @return this builder
     */
    public RichText image(DocumentImageData imageData, double width, double height) {
        return image(imageData, width, height, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline image run with explicit vertical alignment.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     * @param alignment vertical alignment relative to surrounding text
     * @return this builder
     */
    public RichText image(DocumentImageData imageData,
                          double width,
                          double height,
                          InlineImageAlignment alignment) {
        return image(imageData, width, height, alignment, 0.0, null);
    }

    /**
     * Appends a clickable inline image run; the link annotation covers the
     * image rectangle on supporting backends.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     * @param alignment vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param linkOptions optional link metadata
     * @return this builder
     */
    public RichText image(DocumentImageData imageData,
                          double width,
                          double height,
                          InlineImageAlignment alignment,
                          double baselineOffset,
                          DocumentLinkOptions linkOptions) {
        runs.add(new InlineImageRun(
                imageData,
                width,
                height,
                alignment == null ? InlineImageAlignment.CENTER : alignment,
                baselineOffset,
                linkOptions));
        return this;
    }

    /**
     * Appends an inline filled circle ("dot") run — the building block for
     * skill rating dots, custom bullets and inline status indicators that
     * should not depend on font glyph coverage.
     *
     * @param diameter circle diameter in points
     * @param fill fill color
     * @return this builder
     */
    public RichText dot(double diameter, DocumentColor fill) {
        return shape(ShapeOutline.circle(diameter), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline circle run with an explicit fill and/or outline stroke
     * — for example a filled dot ({@code ●}) or an outlined one ({@code ○}).
     *
     * @param diameter circle diameter in points
     * @param fill optional fill color
     * @param stroke optional outline stroke
     * @return this builder
     */
    public RichText dot(double diameter, DocumentColor fill, DocumentStroke stroke) {
        return shape(ShapeOutline.circle(diameter), fill, stroke, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline ellipse run with default
     * {@link InlineImageAlignment#CENTER} alignment and zero offset.
     *
     * @param width target width in points
     * @param height target height in points
     * @param fill optional fill color
     * @param stroke optional outline stroke
     * @return this builder
     */
    public RichText ellipse(double width, double height, DocumentColor fill, DocumentStroke stroke) {
        return shape(new ShapeOutline.Ellipse(width, height), fill, stroke, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline diamond (rhombus) sized {@code size × size}.
     *
     * @param size figure width and height in points
     * @param fill fill color
     * @return this builder
     */
    public RichText diamond(double size, DocumentColor fill) {
        return shape(ShapeOutline.diamond(size, size), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline upward-pointing triangle sized {@code size × size}.
     *
     * @param size figure width and height in points
     * @param fill fill color
     * @return this builder
     */
    public RichText triangle(double size, DocumentColor fill) {
        return shape(ShapeOutline.triangle(size, size), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline five-pointed star sized {@code size × size}.
     *
     * @param size figure width and height in points
     * @param fill fill color
     * @return this builder
     */
    public RichText star(double size, DocumentColor fill) {
        return shape(ShapeOutline.star(size, size), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline block arrow sized {@code size × size} pointing in
     * {@code direction} — a directional marker between text or a list bullet.
     *
     * @param size figure width and height in points
     * @param direction the way the arrow points
     * @param fill fill color
     * @return this builder
     */
    public RichText arrow(double size, ShapeOutline.Direction direction, DocumentColor fill) {
        return shape(ShapeOutline.arrow(size, size, direction), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline arrow of the given {@link ShapeOutline.ArrowStyle} — the
     * swappable-design overload, so a caller (or a future "pick your arrow" UI)
     * can choose a block arrow, a triangular arrowhead, etc.
     *
     * @param size figure width and height in points
     * @param direction the way the arrow points
     * @param style the arrow design
     * @param fill fill color
     * @return this builder
     * @since 1.7.0
     */
    public RichText arrow(double size,
                          ShapeOutline.Direction direction,
                          ShapeOutline.ArrowStyle style,
                          DocumentColor fill) {
        return shape(ShapeOutline.arrow(size, size, direction, style), fill, null,
                InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline chevron sized {@code size × size} pointing in
     * {@code direction} — a lighter directional separator for step lists.
     *
     * @param size figure width and height in points
     * @param direction the way the chevron points
     * @param fill fill color
     * @return this builder
     */
    public RichText chevron(double size, ShapeOutline.Direction direction, DocumentColor fill) {
        return shape(ShapeOutline.chevron(size, size, direction), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends an inline shape of any {@link ShapeOutline} kind with a filled
     * interior, default {@link InlineImageAlignment#CENTER} alignment and zero
     * offset.
     *
     * @param outline figure geometry; supplies the run's size
     * @param fill fill color
     * @return this builder
     */
    public RichText shape(ShapeOutline outline, DocumentColor fill) {
        return shape(outline, fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Appends a fully-specified inline shape run of any {@link ShapeOutline}
     * kind. At least one of {@code fill} or {@code stroke} must be present.
     *
     * @param outline figure geometry; supplies the run's size
     * @param fill optional fill color
     * @param stroke optional outline stroke
     * @param alignment vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param linkOptions optional inline link metadata
     * @return this builder
     */
    public RichText shape(ShapeOutline outline,
                          DocumentColor fill,
                          DocumentStroke stroke,
                          InlineImageAlignment alignment,
                          double baselineOffset,
                          DocumentLinkOptions linkOptions) {
        runs.add(new InlineShapeRun(
                outline,
                fill,
                stroke,
                alignment == null ? InlineImageAlignment.CENTER : alignment,
                baselineOffset,
                linkOptions));
        return this;
    }

    /**
     * Appends an inline <b>area sparkline</b> — a filled mini-chart silhouette
     * of the value run, rendered on the text baseline like any other inline
     * shape. The run's minimum maps to the bottom of the box, its maximum to
     * the top; pair it with {@link DocumentColor#withOpacity(double)} for a
     * softer fill.
     *
     * <pre>{@code rich.plain("Revenue ").sparkline(36, 9, accent, 65.2, 69.8, 74.1, 81.3, 88.2)}</pre>
     *
     * @param width sparkline width in points
     * @param height sparkline height in points
     * @param fill silhouette fill colour
     * @param values data run, at least two finite values
     * @return this builder
     * @since 1.8.0
     */
    public RichText sparkline(double width, double height, DocumentColor fill, double... values) {
        return shape(ShapeOutline.polygon(width, height,
                SparklineGeometry.areaPoints(values)), fill);
    }

    /**
     * Appends an inline <b>line sparkline</b> — the value run as a
     * constant-thickness stroked-looking band, without the filled area.
     *
     * @param width sparkline width in points
     * @param height sparkline height in points
     * @param thickness line thickness in points (must be smaller than {@code height})
     * @param color line colour
     * @param values data run, at least two finite values
     * @return this builder
     * @since 1.8.0
     */
    public RichText sparklineLine(double width, double height, double thickness,
                                  DocumentColor color, double... values) {
        if (thickness <= 0 || thickness >= height) {
            throw new IllegalArgumentException(
                    "sparkline thickness must be in (0, height): " + thickness);
        }
        return shape(ShapeOutline.polygon(width, height,
                SparklineGeometry.ribbonPoints(values, thickness / height)), color);
    }

    /**
     * Appends an inline checkbox — a rounded square frame with an optional
     * centred checkmark inside (the checked state), each in its own colour —
     * for todo / checklist markers between text.
     *
     * @param size box width and height in points
     * @param checked whether the checkmark is shown
     * @param boxColor frame stroke color
     * @param checkColor checkmark fill color
     * @return this builder
     */
    public RichText checkbox(double size, boolean checked, DocumentColor boxColor, DocumentColor checkColor) {
        runs.add(InlineShapeRun.checkbox(size, checked, boxColor, checkColor));
        return this;
    }

    /**
     * Appends an inline checkbox using one colour for both the frame and the
     * checkmark.
     *
     * @param size box width and height in points
     * @param checked whether the checkmark is shown
     * @param color frame and checkmark color
     * @return this builder
     */
    public RichText checkbox(double size, boolean checked, DocumentColor color) {
        return checkbox(size, checked, color, color);
    }

    /**
     * Appends an inline checkbox whose checked-state tick uses the given
     * {@link ShapeOutline.CheckmarkStyle} — the "pick your tick" overload.
     *
     * @param size box width and height in points
     * @param checked whether the checkmark is shown
     * @param markStyle design of the checked-state tick
     * @param boxColor frame stroke color
     * @param checkColor checkmark fill color
     * @return this builder
     * @since 1.7.0
     */
    public RichText checkbox(double size,
                             boolean checked,
                             ShapeOutline.CheckmarkStyle markStyle,
                             DocumentColor boxColor,
                             DocumentColor checkColor) {
        runs.add(InlineShapeRun.checkbox(size, checked, markStyle, boxColor, checkColor));
        return this;
    }

    /**
     * Appends an inline checkbox whose checked-state mark is an arbitrary
     * {@link ShapeOutline} — the power-user overload. Size the mark to fit the
     * frame (≈ {@code 0.6 × size}); it is drawn centred in the box.
     *
     * @param size box width and height in points
     * @param checked whether the mark is shown
     * @param mark checked-state mark geometry, already sized; must be non-null
     *             when {@code checked} is {@code true}
     * @param boxColor frame stroke color
     * @param checkColor mark fill color
     * @return this builder
     * @since 1.7.0
     */
    public RichText checkbox(double size,
                             boolean checked,
                             ShapeOutline mark,
                             DocumentColor boxColor,
                             DocumentColor checkColor) {
        runs.add(InlineShapeRun.checkbox(size, checked, mark, boxColor, checkColor));
        return this;
    }

    /**
     * Returns the accumulated runs as an immutable list.
     *
     * @return inline runs in source order
     */
    public List<InlineRun> runs() {
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
