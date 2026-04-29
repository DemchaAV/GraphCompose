package com.demcha.compose.document.dsl;

import com.demcha.compose.document.dsl.internal.BuilderSupport;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for horizontal rows of semantic blocks.
 *
 * <p>A row arranges its direct children left-to-right inside a single row band
 * and is treated as an atomic unit by the canonical paginator: the whole row
 * moves to the next page when its measured height does not fit on the current
 * page.</p>
 *
 * <p>Allowed children: atomic primitives (paragraph, image, line, ellipse,
 * shape, spacer, barcode) and vertical containers ({@link SectionNode},
 * {@link ContainerNode}). Vertical containers act as columns inside the row
 * and inherit the row's atomic pagination. Nested rows and tables are not
 * supported as row children: nested rows would conflict with the single-axis
 * layout contract, and tables are splittable and would clash with the row's
 * atomic pagination.</p>
 *
 * @author Artem Demchyshyn
 */
public final class RowBuilder {
    private String name = "";
    private final List<DocumentNode> children = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();
    private boolean weightsDirty;
    private double gap;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();
    private DocumentColor fillColor;
    private DocumentStroke stroke;
    private DocumentCornerRadius cornerRadius = DocumentCornerRadius.ZERO;
    private DocumentBorders borders = DocumentBorders.NONE;

    /**
     * Creates a row builder.
     */
    public RowBuilder() {
    }

    /**
     * Sets the semantic row name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public RowBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets the horizontal spacing between row children. This is the canonical
     * name; vertical flows use the same {@code spacing(...)} verb.
     *
     * @param spacing spacing in points
     * @return this builder
     */
    public RowBuilder spacing(double spacing) {
        this.gap = spacing;
        return this;
    }

    /**
     * Sets the horizontal gap between row children.
     *
     * @param gap gap in points
     * @return this builder
     * @deprecated since 1.5.0, use {@link #spacing(double)} instead — vertical
     *             flows and rows now share the same {@code spacing(...)} verb.
     */
    @Deprecated(since = "1.5.0")
    public RowBuilder gap(double gap) {
        return spacing(gap);
    }

    /**
     * Sets row inner padding.
     *
     * @param padding padding insets
     * @return this builder
     */
    public RowBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets row outer margin.
     *
     * @param margin margin insets
     * @return this builder
     */
    public RowBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Sets a row background fill.
     *
     * @param fillColor row fill color, or {@code null} to clear
     * @return this builder
     */
    public RowBuilder fillColor(DocumentColor fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /**
     * Sets a row outline stroke.
     *
     * @param stroke row stroke, or {@code null} to clear
     * @return this builder
     */
    public RowBuilder stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Sets the row's render-only corner radius.
     *
     * @param radius corner radius in points
     * @return this builder
     */
    public RowBuilder cornerRadius(double radius) {
        this.cornerRadius = DocumentCornerRadius.of(radius);
        return this;
    }

    /**
     * Sets per-side row borders. When set, the per-side strokes override the
     * uniform stroke configured via {@link #stroke(DocumentStroke)} for the row
     * outline.
     *
     * @param borders per-side border strokes
     * @return this builder
     */
    public RowBuilder borders(DocumentBorders borders) {
        this.borders = borders == null ? DocumentBorders.NONE : borders;
        return this;
    }

    /**
     * Replaces the per-child weights used to distribute the row's inner width.
     *
     * <p>The number of weights must match the number of children when calling
     * {@link #build()}. Pass an empty list (or call {@link #evenWeights()}) to
     * fall back to an even split.</p>
     *
     * @param weights weight per row child
     * @return this builder
     */
    public RowBuilder weights(double... weights) {
        this.weights.clear();
        if (weights != null) {
            for (double w : weights) {
                this.weights.add(w);
            }
        }
        this.weightsDirty = true;
        return this;
    }

    /**
     * Forces an even-split width distribution by clearing any configured weights.
     *
     * @return this builder
     */
    public RowBuilder evenWeights() {
        this.weights.clear();
        this.weightsDirty = true;
        return this;
    }

    /**
     * Adds a pre-built atomic node as the next row child.
     *
     * @param node atomic semantic node
     * @return this builder
     */
    public RowBuilder add(DocumentNode node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        this.children.add(node);
        return this;
    }

    /**
     * Adds a paragraph child configured through a nested builder.
     *
     * @param spec paragraph builder callback
     * @return this builder
     */
    public RowBuilder addParagraph(Consumer<ParagraphBuilder> spec) {
        return add(BuilderSupport.configure(new ParagraphBuilder(), spec).build());
    }

    /**
     * Adds a paragraph child with default text style.
     *
     * @param text paragraph text content
     * @return this builder
     */
    public RowBuilder addParagraph(String text) {
        return add(new ParagraphBuilder().text(text).build());
    }

    /**
     * Adds a paragraph child with the supplied text style.
     *
     * @param text paragraph text content
     * @param textStyle resolved text style
     * @return this builder
     */
    public RowBuilder addParagraph(String text, DocumentTextStyle textStyle) {
        return add(new ParagraphBuilder().text(text).textStyle(textStyle).build());
    }

    /**
     * Adds a textual child as a synonym for {@link #addParagraph(Consumer)}.
     *
     * @param spec paragraph builder callback
     * @return this builder
     */
    public RowBuilder addText(Consumer<ParagraphBuilder> spec) {
        return addParagraph(spec);
    }

    /**
     * Adds an image child configured through a nested builder.
     *
     * @param spec image builder callback
     * @return this builder
     */
    public RowBuilder addImage(Consumer<ImageBuilder> spec) {
        return add(BuilderSupport.configure(new ImageBuilder(), spec).build());
    }

    /**
     * Adds an image child loaded from an image source.
     *
     * @param imageData decoded image data
     * @return this builder
     */
    public RowBuilder addImage(DocumentImageData imageData) {
        return add(new ImageBuilder().source(imageData).build());
    }

    /**
     * Adds an image child loaded from a file path.
     *
     * @param path image file path
     * @return this builder
     */
    public RowBuilder addImage(Path path) {
        return add(new ImageBuilder().source(path).build());
    }

    /**
     * Adds a shape child configured through a nested builder.
     *
     * @param spec shape builder callback
     * @return this builder
     */
    public RowBuilder addShape(Consumer<ShapeBuilder> spec) {
        return add(BuilderSupport.configure(new ShapeBuilder(), spec).build());
    }

    /**
     * Adds a horizontal/vertical line child configured through a nested builder.
     *
     * @param spec line builder callback
     * @return this builder
     */
    public RowBuilder addLine(Consumer<LineBuilder> spec) {
        return add(BuilderSupport.configure(new LineBuilder(), spec).build());
    }

    /**
     * Adds an ellipse/circle child configured through a nested builder.
     *
     * @param spec ellipse builder callback
     * @return this builder
     */
    public RowBuilder addEllipse(Consumer<EllipseBuilder> spec) {
        return add(BuilderSupport.configure(new EllipseBuilder(), spec).build());
    }

    /**
     * Adds a barcode child configured through a nested builder.
     *
     * @param spec barcode builder callback
     * @return this builder
     */
    public RowBuilder addBarcode(Consumer<BarcodeBuilder> spec) {
        return add(BuilderSupport.configure(new BarcodeBuilder(), spec).build());
    }

    /**
     * Adds a fixed-size spacer child for explicit row gaps.
     *
     * @param spec spacer builder callback
     * @return this builder
     */
    public RowBuilder addSpacer(Consumer<SpacerBuilder> spec) {
        return add(BuilderSupport.configure(new SpacerBuilder(), spec).build());
    }

    /**
     * Adds a fixed-width spacer with zero height.
     *
     * @param width spacer width in points
     * @return this builder
     */
    public RowBuilder addSpacer(double width) {
        return add(new SpacerBuilder().width(width).build());
    }

    /**
     * Adds a section as a column-shaped row child configured through a nested
     * builder. The section provides an independently-measured vertical stack of
     * blocks that occupies one row slot.
     *
     * @param spec section builder callback
     * @return this builder
     */
    public RowBuilder addSection(Consumer<SectionBuilder> spec) {
        return add(BuilderSupport.configure(new SectionBuilder(), spec).build());
    }

    /**
     * Adds a named section as a column-shaped row child without repeating the
     * name inside the nested builder.
     *
     * @param name section name used in snapshots and layout graph paths
     * @param spec section builder callback
     * @return this builder
     */
    public RowBuilder addSection(String name, Consumer<SectionBuilder> spec) {
        return add(BuilderSupport.configure(new SectionBuilder().name(name), spec).build());
    }

    /**
     * Builds the detached row node.
     *
     * @return the built {@link RowNode}
     */
    public RowNode build() {
        validate();
        return new RowNode(
                name,
                List.copyOf(children),
                List.copyOf(weights),
                gap,
                padding,
                margin,
                fillColor,
                stroke,
                cornerRadius,
                borders);
    }

    private void validate() {
        if (weightsDirty && !weights.isEmpty() && weights.size() != children.size()) {
            throw new IllegalStateException("RowBuilder weights size " + weights.size()
                    + " does not match children size " + children.size()
                    + ". Pass " + children.size() + " weights or call evenWeights().");
        }
        for (DocumentNode child : children) {
            if (child instanceof RowNode) {
                throw new IllegalStateException("Row '" + name
                        + "' cannot contain another row; use a section as a column instead.");
            }
            if (child instanceof TableNode) {
                throw new IllegalStateException("Row '" + name
                        + "' cannot contain a table; tables are splittable and would conflict with the row's atomic pagination.");
            }
            if (!isAllowedRowChild(child)) {
                throw new IllegalStateException("Row '" + name + "' does not support child node type '"
                        + child.getClass().getSimpleName() + "'.");
            }
        }
    }

    private static boolean isAllowedRowChild(DocumentNode child) {
        return child instanceof ParagraphNode
                || child instanceof ImageNode
                || child instanceof ShapeNode
                || child instanceof LineNode
                || child instanceof EllipseNode
                || child instanceof SpacerNode
                || child instanceof BarcodeNode
                || child instanceof SectionNode
                || child instanceof ContainerNode
                // LayerStackNode is an atomic overlay composite: its layers
                // share the same bounding box and do not compete with the
                // parent row's horizontal band, so it is safe to drop into a
                // row slot just like a section column.
                || child instanceof LayerStackNode;
    }
}
