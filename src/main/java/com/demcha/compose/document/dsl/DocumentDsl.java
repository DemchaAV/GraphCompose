package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeType;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.model.node.BarcodeNode;
import com.demcha.compose.document.model.node.ContainerNode;
import com.demcha.compose.document.model.node.DocumentNode;
import com.demcha.compose.document.model.node.ImageNode;
import com.demcha.compose.document.model.node.PageBreakNode;
import com.demcha.compose.document.model.node.ParagraphNode;
import com.demcha.compose.document.model.node.SectionNode;
import com.demcha.compose.document.model.node.ShapeNode;
import com.demcha.compose.document.model.node.TableNode;
import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
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
import java.util.LinkedHashMap;
import java.util.Map;
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
 *     document.pageFlow()
 *             .name("QuickStart")
 *             .spacing(8)
 *             .addParagraph("Hello GraphCompose", TextStyle.DEFAULT_STYLE)
 *             .addDivider(d -> d
 *                     .name("Rule")
 *                     .width(document.canvas().innerWidth())
 *                     .color(ComponentColor.LIGHT_GRAY))
 *             .build();
 *
 *     document.buildPdf();
 * }
 * }</pre>
 *
 * @author Artem Demchyshyn
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
     * Configures, builds, and attaches one root page flow in a single call.
     *
     * @param spec callback that configures the root flow
     * @return the built root container node
     */
    public ContainerNode pageFlow(Consumer<PageFlowBuilder> spec) {
        return configure(pageFlow(), spec).build();
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
     * Starts a semantic barcode or QR-code builder.
     *
     * @return a detached barcode builder
     */
    public BarcodeBuilder barcode() {
        return new BarcodeBuilder();
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
         * Adds a plain paragraph without requiring an explicit nested builder.
         *
         * @param text paragraph text
         * @param textStyle paragraph text style, or the default style when {@code null}
         * @return this builder
         */
        public T addParagraph(String text, TextStyle textStyle) {
            return add(new ParagraphBuilder()
                    .text(text)
                    .textStyle(textStyle)
                    .build());
        }

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
         * Alias for {@link #addParagraph(String, TextStyle)}.
         *
         * @param text paragraph text
         * @param textStyle paragraph text style
         * @return this builder
         */
        public T addText(String text, TextStyle textStyle) {
            return addParagraph(text, textStyle);
        }

        public T addImage(Consumer<ImageBuilder> spec) {
            return add(configure(new ImageBuilder(), spec).build());
        }

        public T addShape(Consumer<ShapeBuilder> spec) {
            return add(configure(new ShapeBuilder(), spec).build());
        }

        public T addBarcode(Consumer<BarcodeBuilder> spec) {
            return add(configure(new BarcodeBuilder(), spec).build());
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

        /**
         * Adds a named section without repeating the name inside the nested builder.
         *
         * @param name section name used in snapshots and layout graph paths
         * @param spec callback that configures the section body
         * @return this builder
         */
        public T addSection(String name, Consumer<SectionBuilder> spec) {
            return add(configure(new SectionBuilder().name(name), spec).build());
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
        private String bulletOffset = "";
        private BlockIndentStrategy indentStrategy = BlockIndentStrategy.NONE;
        private PdfLinkOptions linkOptions;
        private PdfBookmarkOptions bookmarkOptions;
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

        public ParagraphBuilder bulletOffset(String bulletOffset) {
            this.bulletOffset = bulletOffset == null ? "" : bulletOffset;
            return this;
        }

        public ParagraphBuilder indentStrategy(BlockIndentStrategy indentStrategy) {
            this.indentStrategy = indentStrategy == null ? BlockIndentStrategy.NONE : indentStrategy;
            return this;
        }

        public ParagraphBuilder link(PdfLinkOptions linkOptions) {
            this.linkOptions = linkOptions;
            return this;
        }

        public ParagraphBuilder bookmark(PdfBookmarkOptions bookmarkOptions) {
            this.bookmarkOptions = bookmarkOptions;
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
            return new ParagraphNode(
                    name,
                    text,
                    textStyle,
                    align,
                    lineSpacing,
                    bulletOffset,
                    indentStrategy,
                    linkOptions,
                    bookmarkOptions,
                    padding,
                    margin);
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
        private PdfLinkOptions linkOptions;
        private PdfBookmarkOptions bookmarkOptions;
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

        public ImageBuilder link(PdfLinkOptions linkOptions) {
            this.linkOptions = linkOptions;
            return this;
        }

        public ImageBuilder bookmark(PdfBookmarkOptions bookmarkOptions) {
            this.bookmarkOptions = bookmarkOptions;
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
            return new ImageNode(
                    name,
                    Objects.requireNonNull(imageData, "imageData"),
                    width,
                    height,
                    linkOptions,
                    bookmarkOptions,
                    padding,
                    margin);
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
        protected PdfLinkOptions linkOptions;
        protected PdfBookmarkOptions bookmarkOptions;
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

        public ShapeBuilder link(PdfLinkOptions linkOptions) {
            this.linkOptions = linkOptions;
            return this;
        }

        public ShapeBuilder bookmark(PdfBookmarkOptions bookmarkOptions) {
            this.bookmarkOptions = bookmarkOptions;
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
            return new ShapeNode(name, width, height, fillColor, stroke, linkOptions, bookmarkOptions, padding, margin);
        }
    }

    /**
     * Builder for semantic barcode and QR-code nodes.
     */
    public static final class BarcodeBuilder {
        private String name = "";
        private String content = "";
        private PdfBarcodeType type = PdfBarcodeType.QR_CODE;
        private Color foreground = Color.BLACK;
        private Color background = Color.WHITE;
        private int quietZoneMargin = 0;
        private double width;
        private double height;
        private PdfLinkOptions linkOptions;
        private PdfBookmarkOptions bookmarkOptions;
        private Padding padding = Padding.zero();
        private Margin margin = Margin.zero();

        public BarcodeBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public BarcodeBuilder options(PdfBarcodeOptions options) {
            PdfBarcodeOptions safe = Objects.requireNonNull(options, "options");
            this.content = safe.getContent();
            this.type = safe.getType();
            this.foreground = safe.getForeground();
            this.background = safe.getBackground();
            this.quietZoneMargin = safe.getQuietZoneMargin();
            return this;
        }

        public BarcodeBuilder data(String content) {
            this.content = content == null ? "" : content;
            return this;
        }

        public BarcodeBuilder type(PdfBarcodeType type) {
            this.type = type == null ? PdfBarcodeType.QR_CODE : type;
            return this;
        }

        public BarcodeBuilder qrCode() {
            return type(PdfBarcodeType.QR_CODE);
        }

        public BarcodeBuilder code128() {
            return type(PdfBarcodeType.CODE_128);
        }

        public BarcodeBuilder code39() {
            return type(PdfBarcodeType.CODE_39);
        }

        public BarcodeBuilder ean13() {
            return type(PdfBarcodeType.EAN_13);
        }

        public BarcodeBuilder ean8() {
            return type(PdfBarcodeType.EAN_8);
        }

        public BarcodeBuilder foreground(Color foreground) {
            this.foreground = foreground == null ? Color.BLACK : foreground;
            return this;
        }

        public BarcodeBuilder foreground(ComponentColor foreground) {
            return foreground(foreground == null ? null : foreground.color());
        }

        public BarcodeBuilder background(Color background) {
            this.background = background == null ? Color.WHITE : background;
            return this;
        }

        public BarcodeBuilder background(ComponentColor background) {
            return background(background == null ? null : background.color());
        }

        public BarcodeBuilder quietZone(int quietZoneMargin) {
            this.quietZoneMargin = Math.max(0, quietZoneMargin);
            return this;
        }

        public BarcodeBuilder width(double width) {
            this.width = width;
            return this;
        }

        public BarcodeBuilder height(double height) {
            this.height = height;
            return this;
        }

        public BarcodeBuilder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public BarcodeBuilder link(PdfLinkOptions linkOptions) {
            this.linkOptions = linkOptions;
            return this;
        }

        public BarcodeBuilder bookmark(PdfBookmarkOptions bookmarkOptions) {
            this.bookmarkOptions = bookmarkOptions;
            return this;
        }

        public BarcodeBuilder padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return this;
        }

        public BarcodeBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        public BarcodeNode build() {
            PdfBarcodeOptions options = PdfBarcodeOptions.builder()
                    .content(content)
                    .type(type)
                    .foreground(foreground)
                    .background(background)
                    .quietZoneMargin(quietZoneMargin)
                    .build();
            return new BarcodeNode(name, options, width, height, linkOptions, bookmarkOptions, padding, margin);
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
        private final Map<Integer, TableCellStyle> rowStyles = new LinkedHashMap<>();
        private final Map<Integer, TableCellStyle> columnStyles = new LinkedHashMap<>();
        private TableCellStyle defaultCellStyle = TableCellStyle.DEFAULT;
        private Double width;
        private PdfLinkOptions linkOptions;
        private PdfBookmarkOptions bookmarkOptions;
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

        /**
         * Adds a semantic header row as the first logical table row.
         *
         * <p>This is a naming convenience over {@link #row(String...)} for the
         * common "header then data rows" authoring pattern.</p>
         *
         * @param values header cell text values
         * @return this builder
         */
        public TableBuilder header(String... values) {
            return row(values);
        }

        /**
         * Adds a semantic header row with explicit cell specifications.
         *
         * @param row header cell specifications
         * @return this builder
         */
        public TableBuilder header(List<TableCellSpec> row) {
            return row(row);
        }

        /**
         * Adds multiple plain-text rows in source order.
         *
         * @param rows row values, one string array per row
         * @return this builder
         */
        public TableBuilder rows(String[]... rows) {
            if (rows != null) {
                for (String[] row : rows) {
                    row(row);
                }
            }
            return this;
        }

        /**
         * Adds multiple explicit rows in source order.
         *
         * @param rows row specifications, one list per row
         * @return this builder
         */
        public TableBuilder rows(List<List<TableCellSpec>> rows) {
            if (rows != null) {
                for (List<TableCellSpec> row : rows) {
                    row(row);
                }
            }
            return this;
        }

        public TableBuilder defaultCellStyle(TableCellStyle defaultCellStyle) {
            this.defaultCellStyle = defaultCellStyle == null ? TableCellStyle.DEFAULT : defaultCellStyle;
            return this;
        }

        /**
         * Applies a style override to the first row, which is commonly used as a header row.
         *
         * @param style header row style override
         * @return this builder
         */
        public TableBuilder headerStyle(TableCellStyle style) {
            return rowStyle(0, style);
        }

        public TableBuilder rowStyle(int rowIndex, TableCellStyle style) {
            if (rowIndex < 0) {
                throw new IllegalArgumentException("rowIndex cannot be negative: " + rowIndex);
            }
            rowStyles.put(rowIndex, Objects.requireNonNull(style, "style"));
            return this;
        }

        public TableBuilder columnStyle(int columnIndex, TableCellStyle style) {
            if (columnIndex < 0) {
                throw new IllegalArgumentException("columnIndex cannot be negative: " + columnIndex);
            }
            columnStyles.put(columnIndex, Objects.requireNonNull(style, "style"));
            return this;
        }

        public TableBuilder width(double width) {
            this.width = width;
            return this;
        }

        public TableBuilder link(PdfLinkOptions linkOptions) {
            this.linkOptions = linkOptions;
            return this;
        }

        public TableBuilder bookmark(PdfBookmarkOptions bookmarkOptions) {
            this.bookmarkOptions = bookmarkOptions;
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
            return new TableNode(
                    name,
                    List.copyOf(columns),
                    List.copyOf(rows),
                    defaultCellStyle,
                    Map.copyOf(rowStyles),
                    Map.copyOf(columnStyles),
                    width,
                    linkOptions,
                    bookmarkOptions,
                    padding,
                    margin);
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
