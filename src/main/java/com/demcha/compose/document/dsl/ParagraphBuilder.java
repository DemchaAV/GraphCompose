package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.internal.BuilderSupport;
import com.demcha.compose.document.dsl.internal.SemanticNameNormalizer;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentBarcodeOptions;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextAutoSize;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for semantic paragraph nodes and inline text runs.
 */
public final class ParagraphBuilder {
    private String name = "";
    private String text = "";
    private final List<InlineTextRun> inlineTextRuns = new ArrayList<>();
    private DocumentTextStyle textStyle = DocumentTextStyle.DEFAULT;
    private TextAlign align = TextAlign.LEFT;
    private double lineSpacing = 0.0;
    private String bulletOffset = "";
    private DocumentTextIndent indentStrategy = DocumentTextIndent.NONE;
    private DocumentLinkOptions linkOptions;
    private DocumentBookmarkOptions bookmarkOptions;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();
    private DocumentTextAutoSize autoSize;

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
        this.inlineTextRuns.clear();
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
     * Attaches paragraph-level link metadata.
     *
     * @param linkOptions link metadata
     * @return this builder
     */
    public ParagraphBuilder link(DocumentLinkOptions linkOptions) {
        this.linkOptions = linkOptions;
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
     * Adds a styled inline text run with optional link metadata.
     *
     * @param text inline text
     * @param textStyle inline text style
     * @param linkOptions optional link metadata
     * @return this builder
     */
    public ParagraphBuilder inlineText(String text, DocumentTextStyle textStyle, DocumentLinkOptions linkOptions) {
        this.inlineTextRuns.add(new InlineTextRun(text, textStyle, linkOptions));
        this.text = "";
        return this;
    }

    /**
     * Replaces inline runs.
     *
     * @param inlineTextRuns inline text runs in source order
     * @return this builder
     */
    public ParagraphBuilder inlineRuns(List<InlineTextRun> inlineTextRuns) {
        this.inlineTextRuns.clear();
        if (inlineTextRuns != null) {
            inlineTextRuns.stream()
                    .filter(Objects::nonNull)
                    .forEach(this.inlineTextRuns::add);
        }
        if (!this.inlineTextRuns.isEmpty()) {
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
                List.copyOf(inlineTextRuns),
                textStyle,
                align,
                lineSpacing,
                bulletOffset,
                indentStrategy,
                linkOptions,
                bookmarkOptions,
                padding,
                margin,
                autoSize);
    }
}

/**
 * Builder for simple semantic lists.
 */
