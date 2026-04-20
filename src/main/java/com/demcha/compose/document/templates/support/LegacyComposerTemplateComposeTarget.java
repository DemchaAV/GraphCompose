package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.components_builders.BlockTextBuilder;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.components_builders.VContainerBuilder;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.DocumentComposer;

import java.util.List;
import java.util.Objects;

/**
 * {@link TemplateComposeTarget} backed by the legacy {@link DocumentComposer}
 * and engine-level builders.
 *
 * @author Artem Demchyshyn
 */
public final class LegacyComposerTemplateComposeTarget implements TemplateComposeTarget {
    private final DocumentComposer composer;
    private final ComponentBuilder componentBuilder;
    private VContainerBuilder root;

    /**
     * Creates a target backed by one legacy composer.
     *
     * @param composer composer receiving legacy template entities
     */
    public LegacyComposerTemplateComposeTarget(DocumentComposer composer) {
        this.composer = Objects.requireNonNull(composer, "composer");
        this.componentBuilder = composer.componentBuilder();
    }

    @Override
    public double pageWidth() {
        return composer.canvas().innerWidth();
    }

    @Override
    public void startDocument(String rootName, double spacing) {
        if (root != null) {
            throw new IllegalStateException("Template document flow has already been started.");
        }
        root = componentBuilder.vContainer(Align.left(spacing))
                .entityName(rootName)
                .size(pageWidth(), 0)
                .anchor(Anchor.topLeft());
    }

    @Override
    public void addParagraph(TemplateParagraphSpec paragraph) {
        BlockTextBuilder builder = componentBuilder.blockText(alignment(paragraph.align(), paragraph.lineSpacing()), paragraph.style())
                .entityName(paragraph.name())
                .size(pageWidth(), 2)
                .strategy(paragraph.indentStrategy())
                .bulletOffset(paragraph.bulletOffset())
                .anchor(anchor(paragraph.align()))
                .padding(paragraph.padding())
                .margin(paragraph.margin());
        builder.text(splitText(paragraph.text()), paragraph.style(), Padding.zero(), Margin.zero());
        requireRoot().addChild(builder.build());
    }

    @Override
    public void addList(TemplateListSpec list) {
        BlockTextBuilder builder = componentBuilder.blockText(alignment(list.align(), list.lineSpacing()), list.style())
                .entityName(list.name())
                .size(pageWidth(), 2)
                .strategy(list.marker().isVisible() ? BlockIndentStrategy.ALL_LINES : BlockIndentStrategy.NONE)
                .bulletOffset(list.marker().prefix())
                .anchor(anchor(list.align()))
                .padding(list.padding())
                .margin(list.margin());
        builder.text(list.items(), list.style(), Padding.zero(), Margin.zero());
        requireRoot().addChild(builder.build());
    }

    @Override
    public void addDivider(TemplateDividerSpec divider) {
        requireRoot().addChild(componentBuilder.divider()
                .entityName(divider.name())
                .width(divider.width())
                .thickness(divider.thickness())
                .color(divider.color())
                .anchor(Anchor.topLeft())
                .margin(divider.margin())
                .build());
    }

    @Override
    public void addTable(TemplateTableSpec table) {
        TableBuilder builder = componentBuilder.table()
                .entityName(table.name())
                .columns(table.columns().toArray(TableColumnSpec[]::new))
                .width(table.width())
                .padding(table.padding())
                .margin(table.margin())
                .anchor(Anchor.topLeft())
                .defaultCellStyle(table.defaultCellStyle());
        table.columnStyles().forEach(builder::columnStyle);
        table.rowStyles().forEach(builder::rowStyle);
        table.rows().forEach(row -> builder.row(row.toArray(TableCellSpec[]::new)));
        requireRoot().addChild(builder.build());
    }

    @Override
    public void addPageBreak(String name) {
        requireRoot().addChild(componentBuilder.pageBreak()
                .entityName(name)
                .build());
    }

    @Override
    public void finishDocument() {
        requireRoot().build();
        root = null;
    }

    private VContainerBuilder requireRoot() {
        if (root == null) {
            throw new IllegalStateException("Template document flow has not been started.");
        }
        return root;
    }

    private Align alignment(TextAlign align, double spacing) {
        return switch (align) {
            case CENTER -> Align.middle(spacing);
            case RIGHT -> Align.right(spacing);
            case LEFT -> Align.left(spacing);
        };
    }

    private Anchor anchor(TextAlign align) {
        return switch (align) {
            case CENTER -> Anchor.topCenter();
            case RIGHT -> Anchor.topRight();
            case LEFT -> Anchor.topLeft();
        };
    }

    private List<String> splitText(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n");
        return List.of(normalized.split("\n"));
    }
}
