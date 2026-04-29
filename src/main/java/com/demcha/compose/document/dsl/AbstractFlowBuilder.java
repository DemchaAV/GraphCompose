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
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
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
 * Base class for vertical flow builders used by root flows and sections.
 *
 * @param <T> concrete builder type
 * @param <N> concrete node type
 */
public abstract class AbstractFlowBuilder<T extends AbstractFlowBuilder<T, N>, N extends DocumentNode> {
    private String name = "";
    private final List<DocumentNode> children = new ArrayList<>();
    private double spacing = 0.0;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();
    private DocumentColor fillColor;
    private DocumentStroke stroke;
    private DocumentCornerRadius cornerRadius = DocumentCornerRadius.ZERO;
    private DocumentBorders borders = DocumentBorders.NONE;

    /**
     * Creates a base flow builder.
     */
    protected AbstractFlowBuilder() {
    }

    protected abstract T self();

    protected abstract N buildNode();

    /**
     * Sets the semantic flow name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public T name(String name) {
        this.name = name == null ? "" : name;
        return self();
    }

    /**
     * Sets vertical spacing between child nodes.
     *
     * @param spacing spacing in points
     * @return this builder
     */
    public T spacing(double spacing) {
        this.spacing = spacing;
        return self();
    }

    /**
     * Sets flow padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    public T padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return self();
    }

    /**
     * Sets flow padding from explicit side values.
     *
     * @param top top padding
     * @param right right padding
     * @param bottom bottom padding
     * @param left left padding
     * @return this builder
     */
    public T padding(float top, float right, float bottom, float left) {
        return padding(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Sets flow margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    public T margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return self();
    }

    /**
     * Sets flow margin from explicit side values.
     *
     * @param top top margin
     * @param right right margin
     * @param bottom bottom margin
     * @param left left margin
     * @return this builder
     */
    public T margin(float top, float right, float bottom, float left) {
        return margin(new DocumentInsets(top, right, bottom, left));
    }

    /**
     * Sets the flow background fill.
     *
     * @param fillColor fill color
     * @return this builder
     */
    public T fillColor(Color fillColor) {
        this.fillColor = fillColor == null ? null : DocumentColor.of(fillColor);
        return self();
    }

    /**
     * Sets the flow background fill with a public canonical color.
     *
     * @param fillColor fill color
     * @return this builder
     */
    public T fillColor(DocumentColor fillColor) {
        this.fillColor = fillColor;
        return self();
    }

    /**
     * Sets the flow border using the public canonical stroke value.
     *
     * @param stroke border stroke, or {@code null} for no border
     * @return this builder
     */
    public T stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return self();
    }

    /**
     * Sets per-side flow borders. When set, the per-side borders override the
     * uniform stroke configured via {@link #stroke(DocumentStroke)} for the
     * rectangle outline. Pass {@code null} or {@link DocumentBorders#NONE} to
     * fall back to the uniform stroke.
     *
     * @param borders per-side border strokes
     * @return this builder
     */
    public T borders(DocumentBorders borders) {
        this.borders = borders == null ? DocumentBorders.NONE : borders;
        return self();
    }

    /**
     * Sets the flow background corner radius in points.
     *
     * <p>The radius affects only rectangle-like background rendering; it does not
     * change layout measurement, padding, or child placement.</p>
     *
     * @param radius corner radius in points
     * @return this builder
     */
    public T cornerRadius(double radius) {
        return cornerRadius(DocumentCornerRadius.of(radius));
    }

    /**
     * Sets the flow background corner radius with the public canonical value.
     *
     * @param cornerRadius corner radius, or {@code null} for square corners
     * @return this builder
     */
    public T cornerRadius(DocumentCornerRadius cornerRadius) {
        this.cornerRadius = cornerRadius == null ? DocumentCornerRadius.ZERO : cornerRadius;
        return self();
    }

    /**
     * Convenience preset that paints a colored "band" as the flow background.
     *
     * <p>Equivalent to calling {@link #fillColor(DocumentColor)}.</p>
     *
     * @param color band fill color, or {@code null} to clear
     * @return this builder
     */
    public T band(DocumentColor color) {
        return fillColor(color);
    }

    /**
     * Convenience preset for a "soft panel" background — a filled, padded,
     * rounded rectangle commonly used for hero blocks, callouts and cards.
     *
     * <p>The preset only configures background fill, corner radius, and inner
     * padding. Pass {@code 0.0} for {@code radius} or {@code padding} to skip
     * either step.</p>
     *
     * @param color panel fill color
     * @param radius corner radius in points
     * @param padding inner padding in points (applied uniformly on all sides)
     * @return this builder
     */
    public T softPanel(DocumentColor color, double radius, double padding) {
        fillColor(color);
        if (radius > 0) {
            cornerRadius(radius);
        }
        if (padding > 0) {
            padding(DocumentInsets.of(padding));
        }
        return self();
    }

    /**
     * Convenience preset for a soft panel with a default 8pt corner radius and
     * 12pt uniform padding.
     *
     * @param color panel fill color
     * @return this builder
     */
    public T softPanel(DocumentColor color) {
        return softPanel(color, 8.0, 12.0);
    }

    /**
     * Convenience preset for a left accent strip (decorative bar on the left
     * edge, common in callouts, asides, and quote blocks).
     *
     * @param color accent color
     * @param width accent stripe width in points
     * @return this builder
     */
    public T accentLeft(DocumentColor color, double width) {
        return borders(DocumentBorders.left(new DocumentStroke(color, width)));
    }

    /**
     * Convenience preset for a right accent strip.
     *
     * @param color accent color
     * @param width accent stripe width in points
     * @return this builder
     */
    public T accentRight(DocumentColor color, double width) {
        return borders(DocumentBorders.right(new DocumentStroke(color, width)));
    }

    /**
     * Convenience preset for a top accent strip.
     *
     * @param color accent color
     * @param width accent stripe width in points
     * @return this builder
     */
    public T accentTop(DocumentColor color, double width) {
        return borders(DocumentBorders.top(new DocumentStroke(color, width)));
    }

    /**
     * Convenience preset for a bottom accent strip (e.g., a colored rule under
     * a header section).
     *
     * @param color accent color
     * @param width accent stripe width in points
     * @return this builder
     */
    public T accentBottom(DocumentColor color, double width) {
        return borders(DocumentBorders.bottom(new DocumentStroke(color, width)));
    }

    /**
     * Adds an already-created child node.
     *
     * @param node child semantic node
     * @return this builder
     */
    public T add(DocumentNode node) {
        children.add(Objects.requireNonNull(node, "node"));
        return self();
    }

    /**
     * Adds a paragraph configured through a nested builder.
     *
     * @param spec paragraph builder callback
     * @return this builder
     */
    public T addParagraph(Consumer<ParagraphBuilder> spec) {
        return add(BuilderSupport.configure(new ParagraphBuilder(), spec).build());
    }

    /**
     * Adds a plain paragraph with the default text style.
     *
     * @param text paragraph text
     * @return this builder
     */
    public T addParagraph(String text) {
        return add(new ParagraphBuilder().text(text).build());
    }

    /**
     * Adds a plain paragraph with a public canonical text style.
     *
     * @param text paragraph text
     * @param textStyle paragraph text style
     * @return this builder
     */
    public T addParagraph(String text, DocumentTextStyle textStyle) {
        return add(new ParagraphBuilder()
                .text(text)
                .textStyle(textStyle)
                .build());
    }

    /**
     * Alias for {@link #addParagraph(Consumer)}.
     *
     * @param spec paragraph builder callback
     * @return this builder
     */
    public T addText(Consumer<ParagraphBuilder> spec) {
        return addParagraph(spec);
    }

    /**
     * Alias for {@link #addParagraph(String)}.
     *
     * @param text paragraph text
     * @return this builder
     */
    public T addText(String text) {
        return addParagraph(text);
    }

    /**
     * Alias for {@link #addParagraph(String, DocumentTextStyle)}.
     *
     * @param text paragraph text
     * @param textStyle paragraph text style
     * @return this builder
     */
    public T addText(String text, DocumentTextStyle textStyle) {
        return addParagraph(text, textStyle);
    }

    /**
     * Adds a list configured through a nested builder.
     *
     * @param spec list builder callback
     * @return this builder
     */
    public T addList(Consumer<ListBuilder> spec) {
        return add(BuilderSupport.configure(new ListBuilder(), spec).build());
    }

    /**
     * Adds a bullet list without requiring a nested builder.
     *
     * @param items list item texts
     * @return this builder
     */
    public T addList(String... items) {
        return add(new ListBuilder().items(items).build());
    }

    /**
     * Adds a bullet list from an existing item collection.
     *
     * @param items list item texts
     * @return this builder
     */
    public T addList(List<String> items) {
        return add(new ListBuilder().items(items).build());
    }

    /**
     * Adds an image configured through a nested builder.
     *
     * @param spec image builder callback
     * @return this builder
     */
    public T addImage(Consumer<ImageBuilder> spec) {
        return add(BuilderSupport.configure(new ImageBuilder(), spec).build());
    }

    /**
     * Adds an image with explicit dimensions — shortcut for the common
     * {@code addImage(i -> i.source(data).size(w, h))} call.
     *
     * @param data image source bytes/path/url
     * @param width image width in points
     * @param height image height in points
     * @return this builder
     */
    public T addImage(DocumentImageData data, double width, double height) {
        return add(new ImageBuilder().source(data).size(width, height).build());
    }

    /**
     * Adds a shape configured through a nested builder.
     *
     * @param spec shape builder callback
     * @return this builder
     */
    public T addShape(Consumer<ShapeBuilder> spec) {
        return add(BuilderSupport.configure(new ShapeBuilder(), spec).build());
    }

    /**
     * Adds a filled shape with explicit dimensions — shortcut for the common
     * {@code addShape(s -> s.size(w, h).fillColor(c))} call.
     *
     * @param width shape width in points
     * @param height shape height in points
     * @param fillColor canonical fill color
     * @return this builder
     */
    public T addShape(double width, double height, DocumentColor fillColor) {
        return add(new ShapeBuilder().size(width, height).fillColor(fillColor).build());
    }

    /**
     * Adds an invisible fixed-size spacer.
     *
     * @param spec spacer builder callback
     * @return this builder
     */
    public T addSpacer(Consumer<SpacerBuilder> spec) {
        return add(BuilderSupport.configure(new SpacerBuilder(), spec).build());
    }

    /**
     * Adds an invisible fixed-size spacer.
     *
     * @param width spacer width in points
     * @param height spacer height in points
     * @return this builder
     */
    public T spacer(double width, double height) {
        return add(new SpacerBuilder().size(width, height).build());
    }

    /**
     * Adds a line configured through a nested builder.
     *
     * @param spec line builder callback
     * @return this builder
     */
    public T addLine(Consumer<LineBuilder> spec) {
        return add(BuilderSupport.configure(new LineBuilder(), spec).build());
    }

    /**
     * Adds an ellipse configured through a nested builder.
     *
     * @param spec ellipse builder callback
     * @return this builder
     */
    public T addEllipse(Consumer<EllipseBuilder> spec) {
        return add(BuilderSupport.configure(new EllipseBuilder(), spec).build());
    }

    /**
     * Adds a filled circle ellipse — shortcut for
     * {@code addEllipse(e -> e.circle(diameter).fillColor(fillColor))}.
     *
     * @param diameter circle diameter in points
     * @param fillColor canonical fill color
     * @return this builder
     */
    public T addEllipse(double diameter, DocumentColor fillColor) {
        return add(new EllipseBuilder().circle(diameter).fillColor(fillColor).build());
    }

    /**
     * Adds a filled ellipse with explicit width and height — shortcut for
     * {@code addEllipse(e -> e.size(w, h).fillColor(fillColor))}.
     *
     * @param width ellipse width in points
     * @param height ellipse height in points
     * @param fillColor canonical fill color
     * @return this builder
     */
    public T addEllipse(double width, double height, DocumentColor fillColor) {
        return add(new EllipseBuilder().size(width, height).fillColor(fillColor).build());
    }

    /**
     * Adds a circle with equal width and height.
     *
     * @param diameter circle diameter in points
     * @return this builder
     */
    public T addCircle(double diameter) {
        return add(new EllipseBuilder().circle(diameter).build());
    }

    /**
     * Adds a filled circle — shortcut for
     * {@code addCircle(diameter, e -> e.fillColor(fillColor))}.
     *
     * @param diameter circle diameter in points
     * @param fillColor canonical fill color
     * @return this builder
     */
    public T addCircle(double diameter, DocumentColor fillColor) {
        return add(new EllipseBuilder().circle(diameter).fillColor(fillColor).build());
    }

    /**
     * Adds a circle with equal width and height.
     *
     * @param diameter circle diameter in points
     * @param spec optional additional ellipse builder callback
     * @return this builder
     */
    public T addCircle(double diameter, Consumer<EllipseBuilder> spec) {
        return add(BuilderSupport.configure(new EllipseBuilder().circle(diameter), spec).build());
    }

    /**
     * Adds a barcode or QR code configured through a nested builder.
     *
     * @param spec barcode builder callback
     * @return this builder
     */
    public T addBarcode(Consumer<BarcodeBuilder> spec) {
        return add(BuilderSupport.configure(new BarcodeBuilder(), spec).build());
    }

    /**
     * Adds a divider configured through a nested builder.
     *
     * @param spec divider builder callback
     * @return this builder
     */
    public T addDivider(Consumer<DividerBuilder> spec) {
        return add(BuilderSupport.configure(new DividerBuilder(), spec).build());
    }

    /**
     * Adds a table configured through a nested builder.
     *
     * @param spec table builder callback
     * @return this builder
     */
    public T addTable(Consumer<TableBuilder> spec) {
        return add(BuilderSupport.configure(new TableBuilder(), spec).build());
    }

    /**
     * Adds a layer stack composed through a nested builder.
     *
     * <p>{@link com.demcha.compose.document.node.LayerStackNode} is an atomic
     * overlay composite: its child layers share the same bounding box and are
     * painted in source order (first behind, last on top). Use it for
     * monogram badges, watermark stamps, image-with-caption tiles, framed
     * hero blocks, and any other case where two or more nodes need to sit on
     * the same coordinates with explicit alignment.</p>
     *
     * <p>Stacks are atomic for pagination — they always move whole to the
     * next page when they do not fit — and are allowed inside row column
     * slots since they do not compete with the parent row's horizontal
     * band.</p>
     *
     * @param spec layer stack builder callback
     * @return this builder
     */
    public T addLayerStack(Consumer<LayerStackBuilder> spec) {
        return add(BuilderSupport.configure(new LayerStackBuilder(), spec).build());
    }

    /**
     * Adds a paragraph composed of a {@link RichText} run sequence.
     *
     * @param rich rich-text builder
     * @return this builder
     */
    public T addRich(RichText rich) {
        return addParagraph(paragraph -> paragraph.rich(rich));
    }

    /**
     * Adds a paragraph composed by configuring a fresh {@link RichText} builder.
     *
     * <p>Useful for label/value lines: {@code section.addRich(t -> t.text("Status: ").bold("Pending"))}.</p>
     *
     * @param spec rich-text configuration callback
     * @return this builder
     */
    public T addRich(Consumer<RichText> spec) {
        return addParagraph(paragraph -> paragraph.rich(spec));
    }

    /**
     * Adds a paragraph that contains a single clickable link span.
     *
     * <p>Convenience over {@code addParagraph(p -> p.inlineLink(text, options))} for
     * the common single-link case. The visible text is the same as the supplied
     * text; the link target comes from the supplied URI.</p>
     *
     * @param text visible link text
     * @param uri target URI
     * @return this builder
     */
    public T addLink(String text, String uri) {
        return addParagraph(paragraph -> paragraph
                .inlineLink(text == null ? "" : text, new DocumentLinkOptions(uri == null ? "" : uri)));
    }

    /**
     * Adds a paragraph that contains a single clickable link span configured
     * with explicit link options.
     *
     * @param text visible link text
     * @param options link target metadata
     * @return this builder
     */
    public T addLink(String text, DocumentLinkOptions options) {
        return addParagraph(paragraph -> paragraph.inlineLink(text == null ? "" : text, options));
    }

    /**
     * Adds a horizontal row configured through a nested builder.
     *
     * <p>Rows arrange their direct children left-to-right inside a single row band
     * and are treated atomically by the canonical paginator.</p>
     *
     * @param spec row builder callback
     * @return this builder
     */
    public T addRow(Consumer<RowBuilder> spec) {
        return add(BuilderSupport.configure(new RowBuilder(), spec).build());
    }

    /**
     * Adds a named horizontal row without repeating the name inside the nested builder.
     *
     * @param name row name used in snapshots and layout graph paths
     * @param spec row builder callback
     * @return this builder
     */
    public T addRow(String name, Consumer<RowBuilder> spec) {
        return add(BuilderSupport.configure(new RowBuilder().name(name), spec).build());
    }

    /**
     * Adds a section configured through a nested builder.
     *
     * @param spec section builder callback
     * @return this builder
     */
    public T addSection(Consumer<SectionBuilder> spec) {
        return add(BuilderSupport.configure(new SectionBuilder(), spec).build());
    }

    /**
     * Adds a named section without repeating the name inside the nested builder.
     *
     * @param name section name used in snapshots and layout graph paths
     * @param spec callback that configures the section body
     * @return this builder
     */
    public T addSection(String name, Consumer<SectionBuilder> spec) {
        return add(BuilderSupport.configure(new SectionBuilder().name(name), spec).build());
    }

    /**
     * Adds a titled semantic module.
     *
     * @param title visible module title
     * @param spec callback that configures module body blocks
     * @return this builder
     */
    public T module(String title, Consumer<ModuleBuilder> spec) {
        return add(BuilderSupport.configure(new ModuleBuilder().title(title), spec).build());
    }

    /**
     * Adds a semantic module.
     *
     * @param spec callback that configures module title and body blocks
     * @return this builder
     */
    public T module(Consumer<ModuleBuilder> spec) {
        return add(BuilderSupport.configure(new ModuleBuilder(), spec).build());
    }

    /**
     * Alias for {@link #module(String, Consumer)}.
     *
     * @param title visible module title
     * @param spec callback that configures module body blocks
     * @return this builder
     */
    public T addModule(String title, Consumer<ModuleBuilder> spec) {
        return module(title, spec);
    }

    /**
     * Alias for {@link #module(Consumer)}.
     *
     * @param spec callback that configures module title and body blocks
     * @return this builder
     */
    public T addModule(Consumer<ModuleBuilder> spec) {
        return module(spec);
    }

    /**
     * Adds a page-break control node configured through a nested builder.
     *
     * @param spec page-break builder callback
     * @return this builder
     */
    public T addPageBreak(Consumer<PageBreakBuilder> spec) {
        return add(BuilderSupport.configure(new PageBreakBuilder(), spec).build());
    }

    protected String name() {
        return name;
    }

    protected List<DocumentNode> children() {
        return List.copyOf(children);
    }

    protected double spacing() {
        return spacing;
    }

    protected DocumentInsets padding() {
        return padding;
    }

    protected DocumentInsets margin() {
        return margin;
    }

    protected DocumentColor fillColor() {
        return fillColor;
    }

    protected DocumentStroke stroke() {
        return stroke;
    }

    protected DocumentCornerRadius cornerRadius() {
        return cornerRadius;
    }

    protected DocumentBorders borders() {
        return borders;
    }
}

/**
 * Root page-flow builder that writes its built node into the session.
 */
