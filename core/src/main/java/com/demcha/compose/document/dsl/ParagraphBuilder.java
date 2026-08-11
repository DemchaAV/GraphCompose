package com.demcha.compose.document.dsl;

import com.demcha.compose.document.emoji.EmojiLibrary;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.InternalLinkTarget;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.InlineSvgRun;
import com.demcha.compose.document.node.InlineHighlightRun;
import com.demcha.compose.document.node.InlineImageRun;
import com.demcha.compose.document.node.ShapeLayer;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.layout.ParagraphDirection;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextAutoSize;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.svg.SvgIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for semantic paragraph nodes and inline text runs.
 * @since 1.0.0
 */
public final class ParagraphBuilder {
    private String name = "";
    private String text = "";
    private final List<InlineRun> inlineRuns = new ArrayList<>();
    private DocumentTextStyle textStyle = DocumentTextStyle.DEFAULT;
    private TextAlign align = TextAlign.LEFT;
    private boolean alignChosenByCaller;
    private TextDirection direction = TextDirection.LTR;
    private double lineSpacing = 0.0;
    private String bulletOffset = "";
    private DocumentTextIndent indentStrategy = DocumentTextIndent.NONE;
    private DocumentLinkTarget linkTarget;
    private String anchor;
    private DocumentBookmarkOptions bookmarkOptions;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();
    private DocumentTextAutoSize autoSize;
    private TextVerticalAlign verticalAlign = TextVerticalAlign.DEFAULT;

    /**
     * Creates a paragraph builder.
     */
    public ParagraphBuilder() {
    }

    /**
     * Sets the paragraph node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public ParagraphBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets plain paragraph text and clears inline runs.
     *
     * @param text paragraph text
     * @return this builder
     */
    public ParagraphBuilder text(String text) {
        this.text = text == null ? "" : text;
        this.inlineRuns.clear();
        return this;
    }

    /**
     * Sets paragraph text style with the public canonical style value.
     *
     * @param textStyle paragraph text style
     * @return this builder
     */
    public ParagraphBuilder textStyle(DocumentTextStyle textStyle) {
        this.textStyle = textStyle == null ? DocumentTextStyle.DEFAULT : textStyle;
        return this;
    }

    /**
     * Sets horizontal text alignment.
     *
     * @param align text alignment
     * @return this builder
     */
    public ParagraphBuilder align(TextAlign align) {
        this.align = align == null ? TextAlign.LEFT : align;
        this.alignChosenByCaller = true;
        return this;
    }

    /**
     * Sets the writing direction of the paragraph.
     *
     * <p>A {@link TextDirection#RTL} or first-strong-resolved {@link TextDirection#AUTO}
     * paragraph aligns to the right, because that is the edge a right-to-left line starts
     * from. Calling {@link #align(TextAlign)} overrides that — the caller's alignment is
     * never second-guessed.</p>
     *
     * @param direction writing direction; {@code null} restores {@link TextDirection#LTR}
     * @return this builder
     * @since 2.2.0
     */
    public ParagraphBuilder direction(TextDirection direction) {
        this.direction = direction == null ? TextDirection.LTR : direction;
        return this;
    }

    /**
     * Sets the vertical seating of the text within its line box.
     *
     * <p>{@link TextVerticalAlign#TOP}, {@code CENTER} and {@code BOTTOM} seat the
     * line by its cap band (cap top to the box top, cap band centred, or baseline
     * to the box bottom); pair them with a vertically-centred layer placement
     * ({@code .center(...)} / {@code .centerLeft(...)}) to seat a single line
     * inside a taller {@code ShapeContainer} / {@code LayerStack} layer.</p>
     *
     * <p>Tuned for a single line of text. A multi-line paragraph seats each line
     * independently, and a line that mixes inline images with text is seated by
     * the text cap height.</p>
     *
     * @param verticalAlign vertical text alignment, or {@code null} for
     *                      {@link TextVerticalAlign#DEFAULT}
     * @return this builder
     * @since 1.7.0
     */
    public ParagraphBuilder verticalAlign(TextVerticalAlign verticalAlign) {
        this.verticalAlign = verticalAlign == null ? TextVerticalAlign.DEFAULT : verticalAlign;
        return this;
    }

    /**
     * Sets spacing between wrapped lines.
     *
     * @param lineSpacing line spacing in points
     * @return this builder
     */
    public ParagraphBuilder lineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    /**
     * Sets the first-line prefix used by list-like paragraphs.
     *
     * @param bulletOffset first-line prefix
     * @return this builder
     */
    public ParagraphBuilder bulletOffset(String bulletOffset) {
        this.bulletOffset = bulletOffset == null ? "" : bulletOffset;
        return this;
    }

    /**
     * Sets paragraph indentation behavior with the public canonical value.
     *
     * @param indentStrategy indent strategy
     * @return this builder
     */
    public ParagraphBuilder indentStrategy(DocumentTextIndent indentStrategy) {
        this.indentStrategy = indentStrategy == null ? DocumentTextIndent.NONE : indentStrategy;
        return this;
    }

    /**
     * Attaches paragraph-level external link metadata.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public ParagraphBuilder link(DocumentLinkOptions linkOptions) {
        this.linkTarget = linkOptions == null ? null : new ExternalLinkTarget(linkOptions);
        return this;
    }

    /**
     * Attaches a paragraph-level link target (external URI or internal anchor).
     *
     * @param linkTarget link target, or {@code null} to clear
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder linkTarget(DocumentLinkTarget linkTarget) {
        this.linkTarget = linkTarget;
        return this;
    }

    /**
     * Attaches a paragraph-level internal link to a named {@code anchor(...)}
     * elsewhere in the document.
     *
     * @param anchor target anchor name
     * @return this builder
     * @throws IllegalArgumentException if {@code anchor} is blank
     * @since 1.9.0
     */
    public ParagraphBuilder linkTo(String anchor) {
        this.linkTarget = new InternalLinkTarget(anchor);
        return this;
    }

    /**
     * Declares a named in-document navigation anchor at this paragraph's
     * top-left.
     *
     * @param anchor anchor name, or {@code null}/blank to clear
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder anchor(String anchor) {
        this.anchor = anchor == null || anchor.isBlank() ? null : anchor.trim();
        return this;
    }

    /**
     * Adds a plain inline text run.
     *
     * @param text inline text
     * @return this builder
     */
    public ParagraphBuilder inlineText(String text) {
        return inlineText(text, (DocumentTextStyle) null, null);
    }

    /**
     * Adds an inline text run with a public canonical style value.
     *
     * @param text inline text
     * @param textStyle inline text style
     * @return this builder
     */
    public ParagraphBuilder inlineText(String text, DocumentTextStyle textStyle) {
        return inlineText(text, textStyle, null);
    }

    /**
     * Adds an inline link run.
     *
     * @param text visible link text
     * @param linkOptions link metadata
     * @return this builder
     */
    public ParagraphBuilder inlineLink(String text, DocumentLinkOptions linkOptions) {
        return inlineText(text, (DocumentTextStyle) null, linkOptions);
    }

    /**
     * Adds an inline internal-link run that jumps to a named {@code anchor(...)}
     * elsewhere in the document.
     *
     * @param text   visible link text
     * @param anchor target anchor name
     * @return this builder
     * @throws IllegalArgumentException if {@code anchor} is blank
     * @since 1.9.0
     */
    public ParagraphBuilder inlineLinkTo(String text, String anchor) {
        this.inlineRuns.add(new InlineTextRun(text, null, new InternalLinkTarget(anchor)));
        this.text = "";
        return this;
    }

    /**
     * Adds a styled inline text run with optional link metadata.
     *
     * @param text inline text
     * @param textStyle inline text style
     * @param linkOptions optional link metadata
     * @return this builder
     */
    public ParagraphBuilder inlineText(String text, DocumentTextStyle textStyle, DocumentLinkOptions linkOptions) {
        this.inlineRuns.add(new InlineTextRun(text, textStyle, linkOptions));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline run drawn on a rounded background "chip" — styled text on a
     * padded fill, on the text baseline (e.g. a GitHub-style inline {@code code}
     * span). The chip wraps with the line; its background is a PDF decoration
     * (text-only backends keep the text and drop the fill).
     *
     * @param text         visible text
     * @param textStyle    glyph style; falls back to the paragraph style when {@code null}
     * @param background   chip fill colour; must not be {@code null}
     * @param cornerRadius corner radius in points, clamped to half the chip height
     * @param padding      inset between the glyphs and the chip edges
     * @return this builder
     * @throws IllegalArgumentException if {@code cornerRadius} is negative or non-finite
     * @since 1.9.0
     */
    public ParagraphBuilder inlineHighlight(String text, DocumentTextStyle textStyle,
                                            DocumentColor background, double cornerRadius, DocumentInsets padding) {
        return inlineHighlight(text, textStyle, background, cornerRadius, padding, null);
    }

    /**
     * Adds a clickable highlight chip: as {@link #inlineHighlight(String,
     * DocumentTextStyle, DocumentColor, double, DocumentInsets)}, but the whole
     * chip becomes an external link.
     *
     * @param text         visible text
     * @param textStyle    glyph style; falls back to the paragraph style when {@code null}
     * @param background   chip fill colour; must not be {@code null}
     * @param cornerRadius corner radius in points, clamped to half the chip height
     * @param padding      inset between the glyphs and the chip edges
     * @param link         external link metadata, or {@code null} for no link
     * @return this builder
     * @throws IllegalArgumentException if {@code cornerRadius} is negative or non-finite
     * @since 1.9.0
     */
    public ParagraphBuilder inlineHighlight(String text, DocumentTextStyle textStyle, DocumentColor background,
                                            double cornerRadius, DocumentInsets padding, DocumentLinkOptions link) {
        Objects.requireNonNull(background, "background");
        this.inlineRuns.add(new InlineHighlightRun(text == null ? "" : text, textStyle,
                new InlineBackground(background, cornerRadius, padding), link));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline code chip with engine defaults — a monospace font, a muted
     * code ink and a light rounded background.
     *
     * @param text the code text
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineCode(String text) {
        this.inlineRuns.add(new InlineHighlightRun(text == null ? "" : text, CodeChip.STYLE, CodeChip.BACKGROUND));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline code chip with an explicit glyph style (e.g. to match the
     * paragraph size), keeping the default chip fill and padding.
     *
     * @param text      the code text
     * @param textStyle the glyph style (typically a monospace font)
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineCode(String text, DocumentTextStyle textStyle) {
        this.inlineRuns.add(new InlineHighlightRun(text == null ? "" : text, textStyle, CodeChip.BACKGROUND));
        this.text = "";
        return this;
    }

    /**
     * Adds a coloured chip: {@code text} in {@code fg} on a {@code bg} fill, with
     * the default code radius and padding.
     *
     * @param text the text
     * @param fg   the text colour
     * @param bg   the chip fill colour; must not be {@code null}
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineChip(String text, DocumentColor fg, DocumentColor bg) {
        Objects.requireNonNull(bg, "bg");
        this.inlineRuns.add(new InlineHighlightRun(text == null ? "" : text,
                DocumentTextStyle.builder().color(fg).build(),
                new InlineBackground(bg, CodeChip.BACKGROUND.cornerRadius(), CodeChip.BACKGROUND.padding())));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline image run measured on the same baseline as the
     * surrounding text. Width and height are explicit and uniform across
     * backends; vertical alignment defaults to {@link InlineImageAlignment#CENTER}.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     * @return this builder
     */
    public ParagraphBuilder inlineImage(DocumentImageData imageData, double width, double height) {
        return inlineImage(imageData, width, height, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline image run with explicit vertical alignment.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     * @param alignment vertical alignment relative to surrounding text
     * @return this builder
     */
    public ParagraphBuilder inlineImage(DocumentImageData imageData,
                                        double width,
                                        double height,
                                        InlineImageAlignment alignment) {
        return inlineImage(imageData, width, height, alignment, 0.0, null);
    }

    /**
     * Adds an inline image run with optional link metadata. The link
     * annotation covers only the image rectangle — wrap an adjacent text
     * run in a matching {@code inlineLink} call to extend the hit area.
     *
     * @param imageData image payload
     * @param width target width in points
     * @param height target height in points
     * @param alignment vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive values move
     *                       the image up
     * @param linkOptions optional inline link metadata
     * @return this builder
     */
    public ParagraphBuilder inlineImage(DocumentImageData imageData,
                                        double width,
                                        double height,
                                        InlineImageAlignment alignment,
                                        double baselineOffset,
                                        DocumentLinkOptions linkOptions) {
        this.inlineRuns.add(new InlineImageRun(
                imageData,
                width,
                height,
                alignment == null ? InlineImageAlignment.CENTER : alignment,
                baselineOffset,
                linkOptions));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline image run that jumps to a named {@code anchor(...)} elsewhere
     * in the document, with default {@link InlineImageAlignment#CENTER} alignment
     * and zero offset.
     *
     * @param imageData image payload
     * @param width     target width in points
     * @param height    target height in points
     * @param anchor    target anchor name
     * @return this builder
     * @throws IllegalArgumentException if {@code anchor} is blank
     * @since 1.9.0
     */
    public ParagraphBuilder inlineImageLinkTo(DocumentImageData imageData, double width, double height, String anchor) {
        return inlineImageLinkTo(imageData, width, height, InlineImageAlignment.CENTER, 0.0, anchor);
    }

    /**
     * Adds a fully-specified inline image run that jumps to a named
     * {@code anchor(...)} elsewhere in the document.
     *
     * @param imageData      image payload
     * @param width          target width in points
     * @param height         target height in points
     * @param alignment      vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param anchor         target anchor name
     * @return this builder
     * @throws IllegalArgumentException if {@code anchor} is blank
     * @since 1.9.0
     */
    public ParagraphBuilder inlineImageLinkTo(DocumentImageData imageData,
                                              double width,
                                              double height,
                                              InlineImageAlignment alignment,
                                              double baselineOffset,
                                              String anchor) {
        this.inlineRuns.add(new InlineImageRun(
                imageData,
                width,
                height,
                alignment == null ? InlineImageAlignment.CENTER : alignment,
                baselineOffset,
                new InternalLinkTarget(anchor)));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline filled circle ("dot") measured on the same baseline as the
     * surrounding text — the building block for skill rating dots, custom
     * bullets and status indicators that should not depend on font glyph
     * coverage.
     *
     * @param diameter circle diameter in points
     * @param fill fill color
     * @return this builder
     */
    public ParagraphBuilder dot(double diameter, DocumentColor fill) {
        return shape(ShapeOutline.circle(diameter), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline circle with an explicit fill and/or outline stroke — for
     * example a filled dot ({@code ●}) or an outlined one ({@code ○}).
     *
     * @param diameter circle diameter in points
     * @param fill optional fill color
     * @param stroke optional outline stroke
     * @return this builder
     */
    public ParagraphBuilder dot(double diameter, DocumentColor fill, DocumentStroke stroke) {
        return shape(ShapeOutline.circle(diameter), fill, stroke, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline ellipse measured on the surrounding text baseline.
     *
     * @param width target width in points
     * @param height target height in points ({@code width == height} renders a circle)
     * @param fill optional fill color
     * @param stroke optional outline stroke
     * @return this builder
     */
    public ParagraphBuilder ellipse(double width, double height, DocumentColor fill, DocumentStroke stroke) {
        return shape(new ShapeOutline.Ellipse(width, height), fill, stroke, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline diamond (rhombus) sized {@code size × size}.
     *
     * @param size figure width and height in points
     * @param fill fill color
     * @return this builder
     */
    public ParagraphBuilder diamond(double size, DocumentColor fill) {
        return shape(ShapeOutline.diamond(size, size), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline upward-pointing triangle sized {@code size × size}.
     *
     * @param size figure width and height in points
     * @param fill fill color
     * @return this builder
     */
    public ParagraphBuilder triangle(double size, DocumentColor fill) {
        return shape(ShapeOutline.triangle(size, size), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline five-pointed star sized {@code size × size}.
     *
     * @param size figure width and height in points
     * @param fill fill color
     * @return this builder
     */
    public ParagraphBuilder star(double size, DocumentColor fill) {
        return shape(ShapeOutline.star(size, size), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline block arrow sized {@code size × size} pointing in
     * {@code direction} — a directional marker between text or a list bullet.
     *
     * @param size figure width and height in points
     * @param direction the way the arrow points
     * @param fill fill color
     * @return this builder
     */
    public ParagraphBuilder arrow(double size, ShapeOutline.Direction direction, DocumentColor fill) {
        return shape(ShapeOutline.arrow(size, size, direction), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline arrow of the given {@link ShapeOutline.ArrowStyle} — the
     * swappable-design overload (block arrow, triangular arrowhead, …).
     *
     * @param size figure width and height in points
     * @param direction the way the arrow points
     * @param style the arrow design
     * @param fill fill color
     * @return this builder
     * @since 1.7.0
     */
    public ParagraphBuilder arrow(double size,
                                  ShapeOutline.Direction direction,
                                  ShapeOutline.ArrowStyle style,
                                  DocumentColor fill) {
        return shape(ShapeOutline.arrow(size, size, direction, style), fill, null,
                InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline chevron sized {@code size × size} pointing in
     * {@code direction} — a lighter directional separator for step lists.
     *
     * @param size figure width and height in points
     * @param direction the way the chevron points
     * @param fill fill color
     * @return this builder
     */
    public ParagraphBuilder chevron(double size, ShapeOutline.Direction direction, DocumentColor fill) {
        return shape(ShapeOutline.chevron(size, size, direction), fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline shape of any {@link ShapeOutline} kind with a filled
     * interior, default {@link InlineImageAlignment#CENTER} alignment and zero
     * offset.
     *
     * @param outline figure geometry; supplies the run's size
     * @param fill fill color
     * @return this builder
     */
    public ParagraphBuilder shape(ShapeOutline outline, DocumentColor fill) {
        return shape(outline, fill, null, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline shape of any {@link ShapeOutline} kind, measured on the
     * surrounding text baseline. At least one of {@code fill} or {@code stroke}
     * must be present; vertical alignment defaults to
     * {@link InlineImageAlignment#CENTER} when {@code null}. The figure is drawn
     * from geometry, so it never depends on font glyph coverage.
     *
     * @param outline figure geometry; supplies the run's size
     * @param fill optional fill color
     * @param stroke optional outline stroke
     * @param alignment vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param linkOptions optional inline link metadata
     * @return this builder
     */
    public ParagraphBuilder shape(ShapeOutline outline,
                                  DocumentColor fill,
                                  DocumentStroke stroke,
                                  InlineImageAlignment alignment,
                                  double baselineOffset,
                                  DocumentLinkOptions linkOptions) {
        this.inlineRuns.add(new InlineShapeRun(
                outline,
                fill,
                stroke,
                alignment == null ? InlineImageAlignment.CENTER : alignment,
                baselineOffset,
                linkOptions));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline SVG-icon run sized to {@code size} points tall, with
     * default {@link InlineImageAlignment#CENTER} alignment and zero offset.
     *
     * <p>The icon is drawn as crisp vector layers on the text baseline, carrying
     * its own colours — so it renders independently of the active font's glyph
     * coverage. This is the inline path for vector colour emoji (e.g. Twemoji
     * SVG) and small vector marks. The icon keeps its aspect ratio: the width is
     * {@code size * icon.aspectRatio()}.</p>
     *
     * @param icon parsed vector icon; must not be {@code null}
     * @param size target height in points (the icon's vertical extent on the line)
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineSvgIcon(SvgIcon icon, double size) {
        return inlineSvgIcon(icon, size, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds an inline SVG-icon run with explicit vertical alignment.
     *
     * @param icon      parsed vector icon; must not be {@code null}
     * @param size      target height in points
     * @param alignment vertical alignment relative to the surrounding text
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineSvgIcon(SvgIcon icon, double size, InlineImageAlignment alignment) {
        return inlineSvgIcon(icon, size, alignment, 0.0, null);
    }

    /**
     * Adds a fully-specified, optionally clickable inline SVG-icon run, measured
     * on the surrounding text baseline. The figure is drawn from vector geometry,
     * so it never depends on font glyph coverage.
     *
     * @param icon           parsed vector icon; must not be {@code null}
     * @param size           target height in points
     * @param alignment      vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param linkOptions    optional inline link metadata
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineSvgIcon(SvgIcon icon,
                                    double size,
                                    InlineImageAlignment alignment,
                                    double baselineOffset,
                                    DocumentLinkOptions linkOptions) {
        Objects.requireNonNull(icon, "icon");
        this.inlineRuns.add(new InlineSvgRun(
                icon,
                size * icon.aspectRatio(),
                size,
                alignment == null ? InlineImageAlignment.CENTER : alignment,
                baselineOffset,
                linkOptions));
        this.text = "";
        return this;
    }

    /**
     * Adds a colour emoji resolved from its GitHub-style shortcode (e.g.
     * {@code ":star:"}) as an inline vector glyph sized to {@code size} points
     * tall, with default {@link InlineImageAlignment#CENTER} alignment.
     *
     * <p>Resolution is lenient: when the shortcode is unknown, or no emoji set is
     * on the classpath (the {@code graph-compose-emoji} artifact), the literal
     * shortcode is added as inline text — the way GitHub renders an unrecognised
     * {@code :code:}. Resolution uses {@link EmojiLibrary#getDefault()}.</p>
     *
     * @param shortcode emoji shortcode, with or without surrounding colons
     * @param size      target height in points
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineEmoji(String shortcode, double size) {
        return inlineEmoji(shortcode, size, InlineImageAlignment.CENTER, 0.0, null);
    }

    /**
     * Adds a colour emoji (see {@link #inlineEmoji(String, double)}) with explicit
     * vertical alignment, baseline offset and optional link metadata.
     *
     * @param shortcode      emoji shortcode, with or without surrounding colons
     * @param size           target height in points
     * @param alignment      vertical alignment relative to the surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param linkOptions    optional link metadata (ignored on the text fallback)
     * @return this builder
     * @since 1.9.0
     */
    public ParagraphBuilder inlineEmoji(String shortcode,
                                  double size,
                                  InlineImageAlignment alignment,
                                  double baselineOffset,
                                  DocumentLinkOptions linkOptions) {
        SvgIcon icon = EmojiLibrary.getDefault().find(shortcode).orElse(null);
        if (icon != null) {
            return inlineSvgIcon(icon, size, alignment, baselineOffset, linkOptions);
        }
        return inlineText(shortcode);
    }

    /**
     * Adds an inline filled shape that jumps to a named {@code anchor(...)}
     * elsewhere in the document, with default {@link InlineImageAlignment#CENTER}
     * alignment and zero offset.
     *
     * @param outline figure geometry; supplies the run's size
     * @param fill    fill color
     * @param anchor  target anchor name
     * @return this builder
     * @throws IllegalArgumentException if {@code anchor} is blank
     * @since 1.9.0
     */
    public ParagraphBuilder shapeLinkTo(ShapeOutline outline, DocumentColor fill, String anchor) {
        return shapeLinkTo(outline, fill, null, InlineImageAlignment.CENTER, 0.0, anchor);
    }

    /**
     * Adds a fully-specified inline shape that jumps to a named {@code anchor(...)}
     * elsewhere in the document. At least one of {@code fill} or {@code stroke}
     * must be present.
     *
     * @param outline        figure geometry; supplies the run's size
     * @param fill           optional fill color
     * @param stroke         optional outline stroke
     * @param alignment      vertical alignment relative to surrounding text
     * @param baselineOffset extra vertical shift in points; positive moves up
     * @param anchor         target anchor name
     * @return this builder
     * @throws IllegalArgumentException if {@code anchor} is blank
     * @since 1.9.0
     */
    public ParagraphBuilder shapeLinkTo(ShapeOutline outline,
                                        DocumentColor fill,
                                        DocumentStroke stroke,
                                        InlineImageAlignment alignment,
                                        double baselineOffset,
                                        String anchor) {
        this.inlineRuns.add(new InlineShapeRun(
                List.of(new ShapeLayer(outline, fill, stroke)),
                alignment == null ? InlineImageAlignment.CENTER : alignment,
                baselineOffset,
                new InternalLinkTarget(anchor)));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline checkbox — a rounded square frame with an optional centred
     * checkmark inside (the checked state), each in its own colour — for todo /
     * checklist markers between text.
     *
     * @param size box width and height in points
     * @param checked whether the checkmark is shown
     * @param boxColor frame stroke color
     * @param checkColor checkmark fill color
     * @return this builder
     */
    public ParagraphBuilder checkbox(double size, boolean checked, DocumentColor boxColor, DocumentColor checkColor) {
        this.inlineRuns.add(InlineShapeRun.checkbox(size, checked, boxColor, checkColor));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline checkbox using one colour for both the frame and the
     * checkmark.
     *
     * @param size box width and height in points
     * @param checked whether the checkmark is shown
     * @param color frame and checkmark color
     * @return this builder
     */
    public ParagraphBuilder checkbox(double size, boolean checked, DocumentColor color) {
        return checkbox(size, checked, color, color);
    }

    /**
     * Adds an inline checkbox whose checked-state tick uses the given
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
    public ParagraphBuilder checkbox(double size,
                                     boolean checked,
                                     ShapeOutline.CheckmarkStyle markStyle,
                                     DocumentColor boxColor,
                                     DocumentColor checkColor) {
        this.inlineRuns.add(InlineShapeRun.checkbox(size, checked, markStyle, boxColor, checkColor));
        this.text = "";
        return this;
    }

    /**
     * Adds an inline checkbox whose checked-state mark is an arbitrary
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
    public ParagraphBuilder checkbox(double size,
                                     boolean checked,
                                     ShapeOutline mark,
                                     DocumentColor boxColor,
                                     DocumentColor checkColor) {
        this.inlineRuns.add(InlineShapeRun.checkbox(size, checked, mark, boxColor, checkColor));
        this.text = "";
        return this;
    }

    /**
     * Replaces inline runs with the contents of a {@link RichText} builder.
     *
     * @param rich rich-text builder; must not be {@code null}
     * @return this builder
     */
    public ParagraphBuilder rich(RichText rich) {
        Objects.requireNonNull(rich, "rich");
        return inlineRunsMixed(rich.runs());
    }

    /**
     * Replaces inline runs by configuring a fresh {@link RichText} builder.
     *
     * <p>Convenient for inline declaration:
     * {@code paragraph.rich(t -> t.plain("Status: ").bold("Pending"))}.</p>
     *
     * <p>Seed the supplied builder with {@link RichText#plain(String)} — not
     * {@code t.text(...)}: {@link RichText#text(String)} is a static factory,
     * so that call compiles but builds a separate, discarded {@code RichText}
     * and leaves this paragraph empty.</p>
     *
     * @param spec callback that configures the rich-text builder
     * @return this builder
     */
    public ParagraphBuilder rich(Consumer<RichText> spec) {
        Objects.requireNonNull(spec, "spec");
        RichText builder = RichText.empty();
        spec.accept(builder);
        return inlineRunsMixed(builder.runs());
    }

    /**
     * Replaces inline runs with text-only runs. Equivalent to
     * {@link #inlineRunsMixed(List)} when the supplied list is text-only.
     *
     * @param inlineTextRuns inline text runs in source order
     * @return this builder
     */
    public ParagraphBuilder inlineRuns(List<InlineTextRun> inlineTextRuns) {
        this.inlineRuns.clear();
        if (inlineTextRuns != null) {
            for (InlineTextRun run : inlineTextRuns) {
                if (run != null) {
                    this.inlineRuns.add(run);
                }
            }
        }
        if (!this.inlineRuns.isEmpty()) {
            this.text = "";
        }
        return this;
    }

    /**
     * Replaces inline runs with a mixed list of text and image runs.
     *
     * @param runs inline runs in source order; may mix text and image
     * @return this builder
     */
    public ParagraphBuilder inlineRunsMixed(List<? extends InlineRun> runs) {
        this.inlineRuns.clear();
        if (runs != null) {
            for (InlineRun run : runs) {
                if (run != null) {
                    this.inlineRuns.add(run);
                }
            }
        }
        if (!this.inlineRuns.isEmpty()) {
            this.text = "";
        }
        return this;
    }

    /**
     * Attaches paragraph-level bookmark metadata.
     *
     * @param bookmarkOptions bookmark metadata
     * @return this builder
     */
    public ParagraphBuilder bookmark(DocumentBookmarkOptions bookmarkOptions) {
        this.bookmarkOptions = bookmarkOptions;
        return this;
    }

    /**
     * Sets paragraph padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public ParagraphBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets paragraph padding from explicit side values.
     *
     * @param top top padding
     * @param right right padding
     * @param bottom bottom padding
     * @param left left padding
     * @return this builder
     */
    public ParagraphBuilder padding(float top, float right, float bottom, float left) {
        return padding(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Sets paragraph margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public ParagraphBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Sets paragraph margin from explicit side values.
     *
     * @param top top margin
     * @param right right margin
     * @param bottom bottom margin
     * @param left left margin
     * @return this builder
     */
    public ParagraphBuilder margin(float top, float right, float bottom, float left) {
        return margin(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Enables auto-size paragraph rendering. The layout pipeline searches the
     * inclusive font-size range {@code [minSize, maxSize]} (in points) and
     * picks the largest size that still renders the paragraph on a single line
     * inside the resolved inner width. Pass {@code null} to clear.
     *
     * @param autoSize auto-size hint, or {@code null} to disable
     * @return this builder
     */
    public ParagraphBuilder autoSize(DocumentTextAutoSize autoSize) {
        this.autoSize = autoSize;
        return this;
    }

    /**
     * Convenience overload of {@link #autoSize(DocumentTextAutoSize)} taking
     * explicit max and min font sizes with the default search step.
     *
     * @param maxSize upper bound for the resolved font size in points
     * @param minSize lower bound for the resolved font size in points
     * @return this builder
     */
    public ParagraphBuilder autoSize(double maxSize, double minSize) {
        return autoSize(DocumentTextAutoSize.between(maxSize, minSize));
    }

    /**
     * Convenience overload of {@link #autoSize(DocumentTextAutoSize)} that uses
     * {@link DocumentTextAutoSize#DEFAULT_MIN_SIZE} as the lower bound.
     *
     * @param maxSize upper bound for the resolved font size in points
     * @return this builder
     */
    public ParagraphBuilder autoSize(double maxSize) {
        return autoSize(DocumentTextAutoSize.upTo(maxSize));
    }

    /**
     * Builds the semantic paragraph node.
     *
     * @return paragraph node
     */
    public ParagraphNode build() {
        return new ParagraphNode(
                name,
                text,
                List.copyOf(inlineRuns),
                textStyle,
                resolveAlign(),
                lineSpacing,
                bulletOffset,
                indentStrategy,
                linkTarget,
                bookmarkOptions,
                padding,
                margin,
                autoSize,
                verticalAlign,
                anchor,
                direction);
    }

    /**
     * A right-to-left paragraph starts at the right edge, so that is where its lines sit
     * unless the caller said otherwise.
     *
     * <p>{@link TextDirection#AUTO} is decided the same way the bidirectional algorithm
     * decides it — by the first strong character, read through the same resolver the
     * layout reads. Resolving it here rather than during layout is what keeps
     * "the caller did not choose an alignment" distinguishable from "the caller chose
     * LEFT": the node carries a concrete alignment, and that distinction lives only in
     * this builder.</p>
     */
    private TextAlign resolveAlign() {
        if (alignChosenByCaller || direction == TextDirection.LTR) {
            return align;
        }
        // Asked of the same resolver the layout asks, rather than answered again here.
        // The paragraph has to sit at the edge it is laid out from, and two readings of
        // "which way does this run" are two chances to pick different edges.
        String probe = text.isBlank() ? inlineRunText() : text;
        return ParagraphDirection.resolve(probe, direction) == TextDirection.RTL
                ? TextAlign.RIGHT
                : align;
    }

    private String inlineRunText() {
        StringBuilder concatenated = new StringBuilder();
        for (InlineRun run : inlineRuns) {
            if (run instanceof InlineTextRun textRun) {
                concatenated.append(textRun.text());
            } else if (run instanceof InlineHighlightRun highlight) {
                concatenated.append(highlight.text());
            }
        }
        return concatenated.toString();
    }
}

