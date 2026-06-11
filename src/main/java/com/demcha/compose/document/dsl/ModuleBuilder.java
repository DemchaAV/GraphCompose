package com.demcha.compose.document.dsl;

import com.demcha.compose.document.dsl.internal.SemanticNameNormalizer;
import com.demcha.compose.document.node.*;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for named semantic modules with optional title content.
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public final class ModuleBuilder extends AbstractFlowBuilder<ModuleBuilder, SectionNode> {
    private String title = "";
    private DocumentTextStyle titleStyle = DocumentTextStyle.DEFAULT;
    private TextAlign titleAlign = TextAlign.LEFT;
    private double titleLineSpacing = 0.0;
    private DocumentInsets titlePadding = DocumentInsets.zero();
    private DocumentInsets titleMargin = DocumentInsets.zero();
    private boolean keepTogether = false;

    /**
     * Creates a module builder.
     */
    public ModuleBuilder() {
    }

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
            name(SemanticNameNormalizer.normalize(this.title));
        }
        return this;
    }

    /**
     * Sets the title text style with the public canonical style value.
     *
     * @param titleStyle title text style
     * @return this builder
     */
    public ModuleBuilder titleStyle(DocumentTextStyle titleStyle) {
        this.titleStyle = titleStyle == null ? DocumentTextStyle.DEFAULT : titleStyle;
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
     * Sets title padding with the public canonical spacing value.
     *
     * @param titlePadding title padding
     * @return this builder
     */
    public ModuleBuilder titlePadding(DocumentInsets titlePadding) {
        this.titlePadding = titlePadding == null ? DocumentInsets.zero() : titlePadding;
        return this;
    }

    /**
     * Sets title margin with the public canonical spacing value.
     *
     * @param titleMargin title margin
     * @return this builder
     */
    public ModuleBuilder titleMargin(DocumentInsets titleMargin) {
        this.titleMargin = titleMargin == null ? DocumentInsets.zero() : titleMargin;
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
     * @param spec  list configuration
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
     * @param rows    data rows
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

    /**
     * Keeps the whole module (title plus body) on one page: when it does not fit
     * in the remaining page space but fits on a fresh page, it relocates whole to
     * the next page instead of orphaning its title from the body. Modules taller
     * than a page still flow.
     *
     * @return this builder
     * @since 1.8.0
     */
    public ModuleBuilder keepTogether() {
        this.keepTogether = true;
        return this;
    }

    /**
     * Sets whether the module keeps together on one page.
     *
     * @param value true to keep the module whole
     * @return this builder
     * @since 1.8.0
     */
    public ModuleBuilder keepTogether(boolean value) {
        this.keepTogether = value;
        return this;
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
        return new SectionNode(name(), moduleChildren, spacing(), padding(), margin(), fillColor(),
                stroke(), cornerRadius(), borders(), keepTogether);
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
