package com.demcha.compose.document.dsl;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeType;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.ContainerNode;
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
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.engine.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.engine.components.components_builders.TableCellSpec;
import com.demcha.compose.engine.components.components_builders.TableCellStyle;
import com.demcha.compose.engine.components.components_builders.TableColumnSpec;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

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
 *     document.pageFlow(page -> page
 *             .module("Summary", module -> module.paragraph("Hello GraphCompose"))
 *             .module("Skills", module -> module.bullets("Java", "SQL")));
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
     * Starts a semantic module builder.
     *
     * @return a detached module builder
     */
    public ModuleBuilder module() {
        return new ModuleBuilder();
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
     * Starts a semantic list builder.
     *
     * @return a detached list builder
     */
    public ListBuilder list() {
        return new ListBuilder();
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

    private static String normalizeSemanticName(String raw) {
        String safe = raw == null ? "" : raw.strip();
        if (safe.isEmpty()) {
            return "Module";
        }
        StringBuilder normalized = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < safe.length(); index++) {
            char current = safe.charAt(index);
            if (!Character.isLetterOrDigit(current)) {
                capitalize = true;
                continue;
            }
            normalized.append(capitalize ? Character.toUpperCase(current) : current);
            capitalize = false;
        }
        if (normalized.isEmpty()) {
            return "Module";
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized.insert(0, "Module");
        }
        return normalized.toString();
    }

    private static Padding toPadding(DocumentInsets insets) {
        if (insets == null) {
            return Padding.zero();
        }
        return new Padding(insets.top(), insets.right(), insets.bottom(), insets.left());
    }

    private static Margin toMargin(DocumentInsets insets) {
        if (insets == null) {
            return Margin.zero();
        }
        return new Margin(insets.top(), insets.right(), insets.bottom(), insets.left());
    }

    private static TextStyle toTextStyle(DocumentTextStyle textStyle) {
        if (textStyle == null) {
            return TextStyle.DEFAULT_STYLE;
        }
        return new TextStyle(
                textStyle.fontName(),
                textStyle.size(),
                toDecoration(textStyle.decoration()),
                textStyle.color().color());
    }

    private static TextDecoration toDecoration(DocumentTextDecoration decoration) {
        if (decoration == null) {
            return TextDecoration.DEFAULT;
        }
        return switch (decoration) {
            case BOLD -> TextDecoration.BOLD;
            case ITALIC -> TextDecoration.ITALIC;
            case BOLD_ITALIC -> TextDecoration.BOLD_ITALIC;
            case UNDERLINE -> TextDecoration.UNDERLINE;
            case STRIKETHROUGH -> TextDecoration.STRIKETHROUGH;
            case DEFAULT -> TextDecoration.DEFAULT;
        };
    }

    private static TableColumnSpec toTableColumn(DocumentTableColumn column) {
        Objects.requireNonNull(column, "column");
        return column.type() == DocumentTableColumn.Type.FIXED
                ? TableColumnSpec.fixed(column.fixedWidth())
                : TableColumnSpec.auto();
    }

    private static TableCellStyle toTableStyle(DocumentTableStyle style) {
        if (style == null) {
            return TableCellStyle.empty();
        }
        return TableCellStyle.builder()
                .padding(style.padding() == null ? null : toPadding(style.padding()))
                .fillColor(style.fillColor() == null ? null : style.fillColor().color())
                .textStyle(style.textStyle() == null ? null : toTextStyle(style.textStyle()))
                .lineSpacing(style.lineSpacing())
                .build();
    }

    private static TableCellSpec toTableCell(DocumentTableCell cell) {
        Objects.requireNonNull(cell, "cell");
        TableCellSpec spec = TableCellSpec.of(cell.lines());
        return cell.style() == null ? spec : spec.withStyle(toTableStyle(cell.style()));
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

        /**
         * Sets flow padding with the public canonical spacing value.
         *
         * @param padding padding in points
         * @return this builder
         */
        public T padding(DocumentInsets padding) {
            return padding(toPadding(padding));
        }

        public T padding(float top, float right, float bottom, float left) {
            return padding(new Padding(top, right, bottom, left));
        }

        public T margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return self();
        }

        /**
         * Sets flow margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public T margin(DocumentInsets margin) {
            return margin(toMargin(margin));
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

        /**
         * Sets the flow background fill with a public canonical color.
         *
         * @param fillColor fill color
         * @return this builder
         */
        public T fillColor(DocumentColor fillColor) {
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

        public T addList(Consumer<ListBuilder> spec) {
            return add(configure(new ListBuilder(), spec).build());
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

        /**
         * Adds a titled semantic module.
         *
         * @param title visible module title
         * @param spec callback that configures module body blocks
         * @return this builder
         */
        public T module(String title, Consumer<ModuleBuilder> spec) {
            return add(configure(new ModuleBuilder().title(title), spec).build());
        }

        /**
         * Adds a semantic module.
         *
         * @param spec callback that configures module title and body blocks
         * @return this builder
         */
        public T module(Consumer<ModuleBuilder> spec) {
            return add(configure(new ModuleBuilder(), spec).build());
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
     * Developer-friendly semantic module builder.
     *
     * <p>A module is a titled full-width section that lowers to canonical
     * paragraph, list, table, image, divider, and page-break nodes. It keeps the
     * authoring API close to document language without introducing a separate
     * low-level builder layer.</p>
     */
    public static final class ModuleBuilder extends AbstractFlowBuilder<ModuleBuilder, SectionNode> {
        private String title = "";
        private TextStyle titleStyle = TextStyle.DEFAULT_STYLE;
        private TextAlign titleAlign = TextAlign.LEFT;
        private double titleLineSpacing = 0.0;
        private Padding titlePadding = Padding.zero();
        private Margin titleMargin = Margin.zero();

        @Override
        protected ModuleBuilder self() {
            return this;
        }

        /**
         * Sets the visible module title and default semantic name.
         *
         * @param title visible title
         * @return this builder
         */
        public ModuleBuilder title(String title) {
            this.title = title == null ? "" : title;
            if (name().isBlank() || "Module".equals(name())) {
                name(normalizeSemanticName(this.title));
            }
            return this;
        }

        /**
         * Sets the title text style.
         *
         * @param titleStyle title text style
         * @return this builder
         */
        public ModuleBuilder titleStyle(TextStyle titleStyle) {
            this.titleStyle = titleStyle == null ? TextStyle.DEFAULT_STYLE : titleStyle;
            return this;
        }

        /**
         * Sets the title text style with the public canonical style value.
         *
         * @param titleStyle title text style
         * @return this builder
         */
        public ModuleBuilder titleStyle(DocumentTextStyle titleStyle) {
            this.titleStyle = toTextStyle(titleStyle);
            return this;
        }

        /**
         * Sets the title alignment.
         *
         * @param titleAlign title alignment
         * @return this builder
         */
        public ModuleBuilder titleAlign(TextAlign titleAlign) {
            this.titleAlign = titleAlign == null ? TextAlign.LEFT : titleAlign;
            return this;
        }

        /**
         * Sets extra spacing between wrapped title lines.
         *
         * @param titleLineSpacing title line spacing
         * @return this builder
         */
        public ModuleBuilder titleLineSpacing(double titleLineSpacing) {
            this.titleLineSpacing = titleLineSpacing;
            return this;
        }

        /**
         * Sets title padding.
         *
         * @param titlePadding title padding
         * @return this builder
         */
        public ModuleBuilder titlePadding(Padding titlePadding) {
            this.titlePadding = titlePadding == null ? Padding.zero() : titlePadding;
            return this;
        }

        /**
         * Sets title padding with the public canonical spacing value.
         *
         * @param titlePadding title padding
         * @return this builder
         */
        public ModuleBuilder titlePadding(DocumentInsets titlePadding) {
            this.titlePadding = toPadding(titlePadding);
            return this;
        }

        /**
         * Sets title margin.
         *
         * @param titleMargin title margin
         * @return this builder
         */
        public ModuleBuilder titleMargin(Margin titleMargin) {
            this.titleMargin = titleMargin == null ? Margin.zero() : titleMargin;
            return this;
        }

        /**
         * Sets title margin with the public canonical spacing value.
         *
         * @param titleMargin title margin
         * @return this builder
         */
        public ModuleBuilder titleMargin(DocumentInsets titleMargin) {
            this.titleMargin = toMargin(titleMargin);
            return this;
        }

        /**
         * Appends a paragraph body block.
         *
         * @param text paragraph text
         * @return this builder
         */
        public ModuleBuilder paragraph(String text) {
            return addParagraph(text);
        }

        /**
         * Appends a paragraph body block.
         *
         * @param spec paragraph configuration
         * @return this builder
         */
        public ModuleBuilder paragraph(Consumer<ParagraphBuilder> spec) {
            return addParagraph(spec);
        }

        /**
         * Appends a bullet list.
         *
         * @param items list item texts
         * @return this builder
         */
        public ModuleBuilder bullets(List<String> items) {
            return addList(items);
        }

        /**
         * Appends a bullet list.
         *
         * @param items list item texts
         * @return this builder
         */
        public ModuleBuilder bullets(String... items) {
            return addList(items);
        }

        /**
         * Appends a dash-marker list.
         *
         * @param items list item texts
         * @return this builder
         */
        public ModuleBuilder dashList(List<String> items) {
            return list(items, ListBuilder::dash);
        }

        /**
         * Appends a dash-marker list.
         *
         * @param items list item texts
         * @return this builder
         */
        public ModuleBuilder dashList(String... items) {
            return list(items == null ? List.of() : List.of(items), ListBuilder::dash);
        }

        /**
         * Appends markerless aligned rows.
         *
         * @param rows row texts
         * @return this builder
         */
        public ModuleBuilder rows(List<String> rows) {
            return list(rows, list -> list.noMarker().continuationIndent("  "));
        }

        /**
         * Appends markerless aligned rows.
         *
         * @param rows row texts
         * @return this builder
         */
        public ModuleBuilder rows(String... rows) {
            return rows(rows == null ? List.of() : List.of(rows));
        }

        /**
         * Appends a configurable list.
         *
         * @param items list item texts
         * @param spec list configuration
         * @return this builder
         */
        public ModuleBuilder list(List<String> items, Consumer<ListBuilder> spec) {
            return addList(list -> {
                list.items(items);
                if (spec != null) {
                    spec.accept(list);
                }
            });
        }

        /**
         * Appends a prebuilt table node.
         *
         * @param table table node
         * @return this builder
         */
        public ModuleBuilder table(TableNode table) {
            return add(table);
        }

        /**
         * Appends a configurable table.
         *
         * @param spec table configuration
         * @return this builder
         */
        public ModuleBuilder table(Consumer<TableBuilder> spec) {
            return addTable(spec);
        }

        /**
         * Appends a simple table with auto-width columns.
         *
         * @param headers header row values
         * @param rows data rows
         * @return this builder
         */
        public ModuleBuilder table(List<String> headers, List<List<String>> rows) {
            return addTable(table -> {
                table.autoColumns(headers == null ? 0 : headers.size());
                if (headers != null) {
                    table.header(headers.toArray(String[]::new));
                }
                if (rows != null) {
                    for (List<String> row : rows) {
                        table.row(row == null ? new String[0] : row.toArray(String[]::new));
                    }
                }
            });
        }

        /**
         * Appends a prebuilt image node.
         *
         * @param image image node
         * @return this builder
         */
        public ModuleBuilder image(ImageNode image) {
            return add(image);
        }

        /**
         * Appends a configurable image.
         *
         * @param spec image configuration
         * @return this builder
         */
        public ModuleBuilder image(Consumer<ImageBuilder> spec) {
            return addImage(spec);
        }

        /**
         * Appends a divider.
         *
         * @param spec divider configuration
         * @return this builder
         */
        public ModuleBuilder divider(Consumer<DividerBuilder> spec) {
            return addDivider(spec);
        }

        /**
         * Appends an explicit page break.
         *
         * @param name semantic page-break name
         * @return this builder
         */
        public ModuleBuilder pageBreak(String name) {
            return addPageBreak(pageBreak -> pageBreak.name(name));
        }

        /**
         * Appends a custom canonical node.
         *
         * @param node node to append
         * @return this builder
         */
        public ModuleBuilder custom(DocumentNode node) {
            return add(node);
        }

        @Override
        protected SectionNode buildNode() {
            List<DocumentNode> moduleChildren = new ArrayList<>();
            if (!title.isBlank()) {
                moduleChildren.add(new ParagraphBuilder()
                        .name(name() + "Title")
                        .text(title)
                        .textStyle(titleStyle)
                        .align(titleAlign)
                        .lineSpacing(titleLineSpacing)
                        .padding(titlePadding)
                        .margin(titleMargin)
                        .build());
            }
            moduleChildren.addAll(children());
            return new SectionNode(name(), moduleChildren, spacing(), padding(), margin(), fillColor(), stroke());
        }

        /**
         * Builds the detached module node.
         *
         * @return the built module node
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
        private final List<InlineTextRun> inlineTextRuns = new ArrayList<>();
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
            this.inlineTextRuns.clear();
            return this;
        }

        public ParagraphBuilder textStyle(TextStyle textStyle) {
            this.textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
            return this;
        }

        /**
         * Sets paragraph text style with the public canonical style value.
         *
         * @param textStyle paragraph text style
         * @return this builder
         */
        public ParagraphBuilder textStyle(DocumentTextStyle textStyle) {
            this.textStyle = toTextStyle(textStyle);
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

        public ParagraphBuilder inlineText(String text) {
            return inlineText(text, null, null);
        }

        public ParagraphBuilder inlineText(String text, TextStyle textStyle) {
            return inlineText(text, textStyle, null);
        }

        /**
         * Adds an inline text run with a public canonical style value.
         *
         * @param text inline text
         * @param textStyle inline text style
         * @return this builder
         */
        public ParagraphBuilder inlineText(String text, DocumentTextStyle textStyle) {
            return inlineText(text, toTextStyle(textStyle), null);
        }

        public ParagraphBuilder inlineLink(String text, PdfLinkOptions linkOptions) {
            return inlineText(text, null, linkOptions);
        }

        public ParagraphBuilder inlineText(String text, TextStyle textStyle, PdfLinkOptions linkOptions) {
            this.inlineTextRuns.add(new InlineTextRun(text, textStyle, linkOptions));
            this.text = "";
            return this;
        }

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

        public ParagraphBuilder bookmark(PdfBookmarkOptions bookmarkOptions) {
            this.bookmarkOptions = bookmarkOptions;
            return this;
        }

        public ParagraphBuilder padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return this;
        }

        /**
         * Sets paragraph padding with the public canonical spacing value.
         *
         * @param padding padding in points
         * @return this builder
         */
        public ParagraphBuilder padding(DocumentInsets padding) {
            return padding(toPadding(padding));
        }

        public ParagraphBuilder padding(float top, float right, float bottom, float left) {
            return padding(new Padding(top, right, bottom, left));
        }

        public ParagraphBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        /**
         * Sets paragraph margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public ParagraphBuilder margin(DocumentInsets margin) {
            return margin(toMargin(margin));
        }

        public ParagraphBuilder margin(float top, float right, float bottom, float left) {
            return margin(new Margin(top, right, bottom, left));
        }

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
                    margin);
        }
    }

    /**
     * Builder for simple semantic lists.
     */
    public static final class ListBuilder {
        private String name = "";
        private final List<String> items = new ArrayList<>();
        private ListMarker marker = ListMarker.bullet();
        private TextStyle textStyle = TextStyle.DEFAULT_STYLE;
        private TextAlign align = TextAlign.LEFT;
        private double lineSpacing = 0.0;
        private double itemSpacing = 0.0;
        private String continuationIndent = "";
        private boolean normalizeMarkers = true;
        private Padding padding = Padding.zero();
        private Margin margin = Margin.zero();

        public ListBuilder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public ListBuilder items(String... items) {
            this.items.clear();
            if (items != null) {
                this.items.addAll(List.of(items));
            }
            return this;
        }

        public ListBuilder items(List<String> items) {
            this.items.clear();
            if (items != null) {
                this.items.addAll(items);
            }
            return this;
        }

        public ListBuilder addItem(String item) {
            this.items.add(item);
            return this;
        }

        public ListBuilder marker(ListMarker marker) {
            this.marker = marker == null ? ListMarker.bullet() : marker;
            return this;
        }

        public ListBuilder marker(String marker) {
            return marker(ListMarker.custom(marker));
        }

        public ListBuilder bullet() {
            return marker(ListMarker.bullet());
        }

        public ListBuilder dash() {
            return marker(ListMarker.dash());
        }

        public ListBuilder noMarker() {
            return marker(ListMarker.none());
        }

        public ListBuilder textStyle(TextStyle textStyle) {
            this.textStyle = textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
            return this;
        }

        /**
         * Sets list text style with the public canonical style value.
         *
         * @param textStyle list text style
         * @return this builder
         */
        public ListBuilder textStyle(DocumentTextStyle textStyle) {
            this.textStyle = toTextStyle(textStyle);
            return this;
        }

        public ListBuilder align(TextAlign align) {
            this.align = align == null ? TextAlign.LEFT : align;
            return this;
        }

        public ListBuilder lineSpacing(double lineSpacing) {
            this.lineSpacing = lineSpacing;
            return this;
        }

        public ListBuilder itemSpacing(double itemSpacing) {
            this.itemSpacing = itemSpacing;
            return this;
        }

        /**
         * Sets the prefix used only for wrapped continuation lines when the list
         * has no visible marker.
         *
         * @param continuationIndent continuation-line prefix, often a few spaces
         * @return this builder
         */
        public ListBuilder continuationIndent(String continuationIndent) {
            this.continuationIndent = continuationIndent == null ? "" : continuationIndent;
            return this;
        }

        public ListBuilder normalizeMarkers(boolean normalizeMarkers) {
            this.normalizeMarkers = normalizeMarkers;
            return this;
        }

        public ListBuilder padding(Padding padding) {
            this.padding = padding == null ? Padding.zero() : padding;
            return this;
        }

        /**
         * Sets list padding with the public canonical spacing value.
         *
         * @param padding padding in points
         * @return this builder
         */
        public ListBuilder padding(DocumentInsets padding) {
            return padding(toPadding(padding));
        }

        public ListBuilder padding(float top, float right, float bottom, float left) {
            return padding(new Padding(top, right, bottom, left));
        }

        public ListBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        /**
         * Sets list margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public ListBuilder margin(DocumentInsets margin) {
            return margin(toMargin(margin));
        }

        public ListBuilder margin(float top, float right, float bottom, float left) {
            return margin(new Margin(top, right, bottom, left));
        }

        public ListNode build() {
            return new ListNode(
                    name,
                    List.copyOf(items),
                    marker,
                    textStyle,
                    align,
                    lineSpacing,
                    itemSpacing,
                    continuationIndent,
                    normalizeMarkers,
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

        /**
         * Sets image padding with the public canonical spacing value.
         *
         * @param padding padding in points
         * @return this builder
         */
        public ImageBuilder padding(DocumentInsets padding) {
            return padding(toPadding(padding));
        }

        public ImageBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        /**
         * Sets image margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public ImageBuilder margin(DocumentInsets margin) {
            return margin(toMargin(margin));
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

        /**
         * Sets shape fill with a public canonical color.
         *
         * @param fillColor fill color
         * @return this builder
         */
        public ShapeBuilder fillColor(DocumentColor fillColor) {
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

        /**
         * Sets shape padding with the public canonical spacing value.
         *
         * @param padding padding in points
         * @return this builder
         */
        public ShapeBuilder padding(DocumentInsets padding) {
            return padding(toPadding(padding));
        }

        public ShapeBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        /**
         * Sets shape margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public ShapeBuilder margin(DocumentInsets margin) {
            return margin(toMargin(margin));
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

        /**
         * Sets barcode foreground with a public canonical color.
         *
         * @param foreground foreground color
         * @return this builder
         */
        public BarcodeBuilder foreground(DocumentColor foreground) {
            return foreground(foreground == null ? null : foreground.color());
        }

        public BarcodeBuilder background(Color background) {
            this.background = background == null ? Color.WHITE : background;
            return this;
        }

        public BarcodeBuilder background(ComponentColor background) {
            return background(background == null ? null : background.color());
        }

        /**
         * Sets barcode background with a public canonical color.
         *
         * @param background background color
         * @return this builder
         */
        public BarcodeBuilder background(DocumentColor background) {
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

        /**
         * Sets barcode padding with the public canonical spacing value.
         *
         * @param padding padding in points
         * @return this builder
         */
        public BarcodeBuilder padding(DocumentInsets padding) {
            return padding(toPadding(padding));
        }

        public BarcodeBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        /**
         * Sets barcode margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public BarcodeBuilder margin(DocumentInsets margin) {
            return margin(toMargin(margin));
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

        /**
         * Sets divider color with a public canonical color.
         *
         * @param color divider color
         * @return this builder
         */
        public DividerBuilder color(DocumentColor color) {
            return color(color == null ? null : color.color());
        }

        @Override
        public DividerBuilder name(String name) {
            super.name(name);
            return this;
        }

        @Override
        public DividerBuilder padding(Padding padding) {
            super.padding(padding);
            return this;
        }

        @Override
        public DividerBuilder padding(DocumentInsets padding) {
            super.padding(padding);
            return this;
        }

        @Override
        public DividerBuilder margin(Margin margin) {
            super.margin(margin);
            return this;
        }

        @Override
        public DividerBuilder margin(DocumentInsets margin) {
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

        /**
         * Sets columns with public canonical table column values.
         *
         * @param columns column specifications
         * @return this builder
         */
        public TableBuilder columns(DocumentTableColumn... columns) {
            this.columns.clear();
            if (columns != null) {
                for (DocumentTableColumn column : columns) {
                    this.columns.add(toTableColumn(column));
                }
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

        /**
         * Adds a public canonical table column value.
         *
         * @param column column specification
         * @return this builder
         */
        public TableBuilder addColumn(DocumentTableColumn column) {
            this.columns.add(toTableColumn(column));
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
         * Adds a row made of public canonical table cells.
         *
         * @param row table cells
         * @return this builder
         */
        public TableBuilder rowCells(DocumentTableCell... row) {
            if (row == null) {
                return row(List.of());
            }
            List<TableCellSpec> cells = new ArrayList<>(row.length);
            for (DocumentTableCell cell : row) {
                cells.add(toTableCell(cell));
            }
            return row(cells);
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
         * Adds a semantic header row from public canonical table cells.
         *
         * @param row header cells
         * @return this builder
         */
        public TableBuilder headerCells(DocumentTableCell... row) {
            return rowCells(row);
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
         * Sets the default cell style with a public canonical table style.
         *
         * @param defaultCellStyle default cell style
         * @return this builder
         */
        public TableBuilder defaultCellStyle(DocumentTableStyle defaultCellStyle) {
            this.defaultCellStyle = defaultCellStyle == null ? TableCellStyle.DEFAULT : toTableStyle(defaultCellStyle);
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

        /**
         * Applies a public canonical style override to the first row.
         *
         * @param style header row style override
         * @return this builder
         */
        public TableBuilder headerStyle(DocumentTableStyle style) {
            return rowStyle(0, toTableStyle(style));
        }

        public TableBuilder rowStyle(int rowIndex, TableCellStyle style) {
            if (rowIndex < 0) {
                throw new IllegalArgumentException("rowIndex cannot be negative: " + rowIndex);
            }
            rowStyles.put(rowIndex, Objects.requireNonNull(style, "style"));
            return this;
        }

        /**
         * Applies a public canonical style override to a row.
         *
         * @param rowIndex zero-based row index
         * @param style row style override
         * @return this builder
         */
        public TableBuilder rowStyle(int rowIndex, DocumentTableStyle style) {
            return rowStyle(rowIndex, toTableStyle(style));
        }

        public TableBuilder columnStyle(int columnIndex, TableCellStyle style) {
            if (columnIndex < 0) {
                throw new IllegalArgumentException("columnIndex cannot be negative: " + columnIndex);
            }
            columnStyles.put(columnIndex, Objects.requireNonNull(style, "style"));
            return this;
        }

        /**
         * Applies a public canonical style override to a column.
         *
         * @param columnIndex zero-based column index
         * @param style column style override
         * @return this builder
         */
        public TableBuilder columnStyle(int columnIndex, DocumentTableStyle style) {
            return columnStyle(columnIndex, toTableStyle(style));
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

        /**
         * Sets table padding with the public canonical spacing value.
         *
         * @param padding padding in points
         * @return this builder
         */
        public TableBuilder padding(DocumentInsets padding) {
            return padding(toPadding(padding));
        }

        public TableBuilder margin(Margin margin) {
            this.margin = margin == null ? Margin.zero() : margin;
            return this;
        }

        /**
         * Sets table margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public TableBuilder margin(DocumentInsets margin) {
            return margin(toMargin(margin));
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

        /**
         * Sets page-break margin with the public canonical spacing value.
         *
         * @param margin margin in points
         * @return this builder
         */
        public PageBreakBuilder margin(DocumentInsets margin) {
            this.margin = toMargin(margin);
            return this;
        }

        public PageBreakNode build() {
            return new PageBreakNode(name, margin);
        }
    }
}
