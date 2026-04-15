package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.model.node.ContainerNode;
import com.demcha.compose.document.model.node.DocumentNode;
import com.demcha.compose.document.model.node.ImageNode;
import com.demcha.compose.document.model.node.PageBreakNode;
import com.demcha.compose.document.model.node.ParagraphNode;
import com.demcha.compose.document.model.node.SectionNode;
import com.demcha.compose.document.model.node.ShapeNode;
import com.demcha.compose.document.model.node.TableNode;
import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.content.ImageData;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent semantic authoring facade for {@link DocumentSession}.
 *
 * <p>The DSL keeps the V2 authoring model node-based internally while exposing
 * a more V1-like builder experience for the common document-building path.</p>
 *
 * <pre>{@code
 * try (var document = GraphCompose.document(Path.of("output.pdf")).create()) {
 *     document.dsl()
 *             .pageFlow()
 *             .name("QuickStart")
 *             .spacing(8)
 *             .addParagraph(p -> p
 *                     .name("Greeting")
 *                     .text("Hello GraphCompose")
 *                     .textStyle(TextStyle.DEFAULT_STYLE))
 *             .addDivider(d -> d
 *                     .name("Rule")
 *                     .width(document.canvas().innerWidth())
 *                     .color(ComponentColor.LIGHT_GRAY))
 *             .build();
 *
 *     document.buildPdf();
 * }
 * }</pre>
 */
public final class DocumentDsl {
    private final DocumentSession session;

    /**
     * Creates a DSL facade bound to one live document session.
     *
     * @param session mutable session that receives built root nodes
     */
    public DocumentDsl(DocumentSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * Starts a root page-flow builder that attaches itself to the session when
     * {@link PageFlowBuilder#build()} is called.
     *
     * @return a root flow builder
     */
    public PageFlowBuilder pageFlow() {
        return new PageFlowBuilder(session);
    }

    /**
     * Starts a semantic section builder.
     *
     * @return a detached section builder
     */
    public SectionBuilder section() {
        return new SectionBuilder();
    }

    /**
     * Starts a paragraph builder.
     *
     * @return a detached paragraph builder
     */
    public ParagraphBuilder paragraph() {
        return new ParagraphBuilder();
    }

    /**
     * Alias for {@link #paragraph()} for callers thinking in short text blocks.
     *
     * @return a detached paragraph builder
     */
    public ParagraphBuilder text() {
        return paragraph();
    }

    /**
     * Starts an image builder.
     *
     * @return a detached image builder
     */
    public ImageBuilder image() {
        return new ImageBuilder();
    }

    /**
     * Starts a generic rectangle-like shape builder.
     *
     * @return a detached shape builder
     */
    public ShapeBuilder shape() {
        return new ShapeBuilder();
    }

    /**
     * Starts a divider builder preconfigured as a one-point horizontal rule.
     *
     * @return a detached divider builder
     */
    public DividerBuilder divider() {
        return new DividerBuilder();
    }

    /**
     * Starts a semantic table builder.
     *
     * @return a detached table builder
     */
    public TableBuilder table() {
        return new TableBuilder();
    }

    /**
     * Starts a page-break control builder.
     *
     * @return a detached page-break builder
     */
    public PageBreakBuilder pageBreak() {
        return new PageBreakBuilder();
    }

    private static <B> B configure(B builder, Consumer<B> spec) {
        Objects.requireNonNull(spec, "spec").accept(builder);
        return builder;
    }

    /**
     * Base class for vertical flow builders used by root flows and sections.
     *
     * @param <T> concrete builder type
     * @param <N> concrete node type
     */
    public abstract static class AbstractFlowBuilder<T extends AbstractFlowBuilder<T, N>, N extends DocumentNode> {
        private String name = "";
        private final List<DocumentNode> children = new ArrayList<>();
        private double spacing = 0.0;
        private Padding padding = Padding.zero();
        private Margin margin = Margin.zero();
        private Color fillColor;
        private Stroke stroke;

        protected abstract T self();

        protected abstract N buildNode();

        public T name(String name) {
            this.name = name == null ? "" : name;
            return self();
        }

        public T spacing(double spacing) {
            this.spacing = spacing;
            return self();
        }

        public T padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return self();
        }

        public T padding(float top, float right, float bottom, float left) {
            return padding(new Padding(top, right, bottom, left));
        }

        public T margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return self();
        }

        public T margin(float top, float right, float bottom, float left) {
            return margin(new Margin(top, right, bottom, left));
        }

        public T fillColor(Color fillColor) {
            this.fillColor = fillColor;
            return self();
        }

        public T fillColor(ComponentColor fillColor) {
            return fillColor(fillColor == null ? null : fillColor.color());
        }

        public T stroke(Stroke stroke) {
            this.stroke = stroke;
            return self();
        }

        public T add(DocumentNode node) {
            children.add(Objects.requireNonNull(node, "node"));
            return self();
        }

        public T addParagraph(Consumer<ParagraphBuilder> spec) {
            return add(configure(new ParagraphBuilder(), spec).build());
        }

        public T addText(Consumer<ParagraphBuilder> spec) {
            return addParagraph(spec);
        }

        public T addImage(Consumer<ImageBuilder> spec) {
            return add(configure(new ImageBuilder(), spec).build());
        }

        public T addShape(Consumer<ShapeBuilder> spec) {
            return add(configure(new ShapeBuilder(), spec).build());
        }

        public T addDivider(Consumer<DividerBuilder> spec) {
            return add(configure(new DividerBuilder(), spec).build());
        }

        public T addTable(Consumer<TableBuilder> spec) {
            return add(configure(new TableBuilder(), spec).build());
        }

        public T addSection(Consumer<SectionBuilder> spec) {
            return add(configure(new SectionBuilder(), spec).build());
        }

        public T addPageBreak(Consumer<PageBreakBuilder> spec) {
            return add(configure(new PageBreakBuilder(), spec).build());
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

        protected Padding padding() {
            return padding;
        }

        protected Margin margin() {
            return margin;
        }

        protected Color fillColor() {
            return fillColor;
        }

        protected Stroke stroke() {
            return stroke;
        }
    }

    /**
     * Root page-flow builder that writes its built node into the session.
     */
    public static final class PageFlowBuilder extends AbstractFlowBuilder<PageFlowBuilder, ContainerNode> {
        private final DocumentSession session;

        private PageFlowBuilder(DocumentSession session) {
            this.session = session;
        }

        @Override
        protected PageFlowBuilder self() {
            return this;
        }

        @Override
        protected ContainerNode buildNode() {
            return new ContainerNode(name(), children(), spacing(), padding(), margin(), fillColor(), stroke());
        }

        /**
         * Builds the root flow and attaches it to the bound session.
         *
         * @return the built root node
         */
        public ContainerNode build() {
            ContainerNode root = buildNode();
            session.add(root);
            return root;
        }
    }

    /**
     * Semantic section builder for nested full-width sections.
     */
    public static final class SectionBuilder extends AbstractFlowBuilder<SectionBuilder, SectionNode> {
        @Override
        protected SectionBuilder self() {
            return this;
        }

        @Override
        protected SectionNode buildNode() {
            return new SectionNode(name(), children(), spacing(), padding(), margin(), fillColor(), stroke());
        }

        /**
         * Builds the detached section node.
         *
         * @return the built section node
         */
        public SectionNode build() {
            return buildNode();
        }
    }

    /**
     * Builder for paragraph-like semantic text blocks.
     */
    public static final class ParagraphBuilder {
        private String name = "";
        private String text = "";
        private TextStyle textStyle = TextStyle.DEFAULT_STYLE;
        private TextAlign align = TextAlign.LEFT;
        private double lineSpacing = 0.0;
        private Padding padding = Padding.zero();
        private Margin margin = Margin.zero();

        public ParagraphBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public ParagraphBuilder text(String text) {
            this.text = text == null ? "" : text;
            return this;
        }

        public ParagraphBuilder textStyle(TextStyle textStyle) {
            this.textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
            return this;
        }

        public ParagraphBuilder align(TextAlign align) {
            this.align = align == null ? TextAlign.LEFT : align;
            return this;
        }

        public ParagraphBuilder lineSpacing(double lineSpacing) {
            this.lineSpacing = lineSpacing;
            return this;
        }

        public ParagraphBuilder padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return this;
        }

        public ParagraphBuilder padding(float top, float right, float bottom, float left) {
            return padding(new Padding(top, right, bottom, left));
        }

        public ParagraphBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        public ParagraphBuilder margin(float top, float right, float bottom, float left) {
            return margin(new Margin(top, right, bottom, left));
        }

        public ParagraphNode build() {
            return new ParagraphNode(name, text, textStyle, align, lineSpacing, padding, margin);
        }
    }

    /**
     * Builder for semantic images.
     */
    public static final class ImageBuilder {
        private String name = "";
        private ImageData imageData;
        private Double width;
        private Double height;
        private Padding padding = Padding.zero();
        private Margin margin = Margin.zero();

        public ImageBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public ImageBuilder source(ImageData imageData) {
            this.imageData = Objects.requireNonNull(imageData, "imageData");
            return this;
        }

        public ImageBuilder source(Path path) {
            return source(ImageData.create(path));
        }

        public ImageBuilder source(String path) {
            return source(ImageData.create(path));
        }

        public ImageBuilder width(double width) {
            this.width = width;
            return this;
        }

        public ImageBuilder height(double height) {
            this.height = height;
            return this;
        }

        public ImageBuilder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ImageBuilder padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return this;
        }

        public ImageBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        public ImageNode build() {
            return new ImageNode(name, Objects.requireNonNull(imageData, "imageData"), width, height, padding, margin);
        }
    }

    /**
     * Builder for simple rectangle-like shapes.
     */
    public static class ShapeBuilder {
        protected String name = "";
        protected double width;
        protected double height;
        protected Color fillColor;
        protected Stroke stroke;
        protected Padding padding = Padding.zero();
        protected Margin margin = Margin.zero();

        public ShapeBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public ShapeBuilder width(double width) {
            this.width = width;
            return this;
        }

        public ShapeBuilder height(double height) {
            this.height = height;
            return this;
        }

        public ShapeBuilder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ShapeBuilder fillColor(Color fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        public ShapeBuilder fillColor(ComponentColor fillColor) {
            return fillColor(fillColor == null ? null : fillColor.color());
        }

        public ShapeBuilder stroke(Stroke stroke) {
            this.stroke = stroke;
            return this;
        }

        public ShapeBuilder padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return this;
        }

        public ShapeBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        public ShapeNode build() {
            return new ShapeNode(name, width, height, fillColor, stroke, padding, margin);
        }
    }

    /**
     * Builder for thin horizontal dividers.
     */
    public static final class DividerBuilder extends ShapeBuilder {
        private DividerBuilder() {
            height = 1.0;
            fillColor = ComponentColor.LIGHT_GRAY;
        }

        public DividerBuilder width(double width) {
            super.width(width);
            return this;
        }

        public DividerBuilder height(double height) {
            super.height(height);
            return this;
        }

        public DividerBuilder thickness(double height) {
            return height(height);
        }

        public DividerBuilder color(Color color) {
            super.fillColor(color);
            return this;
        }

        public DividerBuilder color(ComponentColor color) {
            return color(color == null ? null : color.color());
        }

        @Override
        public DividerBuilder name(String name) {
            super.name(name);
            return this;
        }

        @Override
        public DividerBuilder margin(Margin margin) {
            super.margin(margin);
            return this;
        }

        @Override
        public ShapeNode build() {
            return new ShapeNode(name, width, height, fillColor, stroke, padding, margin);
        }
    }

    /**
     * Builder for semantic tables with row-atomic pagination.
     */
    public static final class TableBuilder {
        private String name = "";
        private final List<TableColumnSpec> columns = new ArrayList<>();
        private final List<List<TableCellSpec>> rows = new ArrayList<>();
        private TableCellStyle defaultCellStyle = TableCellStyle.DEFAULT;
        private Double width;
        private Padding padding = Padding.zero();
        private Margin margin = Margin.zero();

        public TableBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public TableBuilder columns(TableColumnSpec... columns) {
            this.columns.clear();
            if (columns != null) {
                this.columns.addAll(List.of(columns));
            }
            return this;
        }

        public TableBuilder autoColumns(int count) {
            this.columns.clear();
            for (int index = 0; index < count; index++) {
                this.columns.add(TableColumnSpec.auto());
            }
            return this;
        }

        public TableBuilder addColumn(TableColumnSpec column) {
            this.columns.add(Objects.requireNonNull(column, "column"));
            return this;
        }

        public TableBuilder row(String... values) {
            List<TableCellSpec> row = new ArrayList<>();
            if (values != null) {
                for (String value : values) {
                    row.add(TableCellSpec.text(value));
                }
            }
            return row(row);
        }

        public TableBuilder row(List<TableCellSpec> row) {
            this.rows.add(List.copyOf(Objects.requireNonNull(row, "row")));
            return this;
        }

        public TableBuilder defaultCellStyle(TableCellStyle defaultCellStyle) {
            this.defaultCellStyle = defaultCellStyle == null ? TableCellStyle.DEFAULT : defaultCellStyle;
            return this;
        }

        public TableBuilder width(double width) {
            this.width = width;
            return this;
        }

        public TableBuilder padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return this;
        }

        public TableBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        public TableNode build() {
            return new TableNode(name, List.copyOf(columns), List.copyOf(rows), defaultCellStyle, width, padding, margin);
        }
    }

    /**
     * Builder for explicit page-break control nodes.
     */
    public static final class PageBreakBuilder {
        private String name = "";
        private Margin margin = Margin.zero();

        public PageBreakBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public PageBreakBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        public PageBreakNode build() {
            return new PageBreakNode(name, margin);
        }
    }
}
