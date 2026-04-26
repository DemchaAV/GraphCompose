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
     * Adds a shape configured through a nested builder.
     *
     * @param spec shape builder callback
     * @return this builder
     */
    public T addShape(Consumer<ShapeBuilder> spec) {
        return add(BuilderSupport.configure(new ShapeBuilder(), spec).build());
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
     * Adds a circle with equal width and height.
     *
     * @param diameter circle diameter in points
     * @return this builder
     */
    public T addCircle(double diameter) {
        return add(new EllipseBuilder().circle(diameter).build());
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
}

/**
 * Root page-flow builder that writes its built node into the session.
 */
