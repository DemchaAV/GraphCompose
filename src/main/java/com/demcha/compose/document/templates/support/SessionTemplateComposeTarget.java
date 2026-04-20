package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.Objects;

/**
 * {@link TemplateComposeTarget} backed by the canonical {@link DocumentSession}
 * DSL.
 *
 * @author Artem Demchyshyn
 */
public final class SessionTemplateComposeTarget implements TemplateComposeTarget {
    private final DocumentSession session;
    private DocumentDsl.PageFlowBuilder root;

    /**
     * Creates a target backed by one live document session.
     *
     * @param session session receiving composed template nodes
     */
    public SessionTemplateComposeTarget(DocumentSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    public double pageWidth() {
        return session.canvas().innerWidth();
    }

    @Override
    public void startDocument(String rootName, double spacing) {
        if (root != null) {
            throw new IllegalStateException("Template document flow has already been started.");
        }
        root = session.dsl()
                .pageFlow()
                .name(rootName)
                .spacing(spacing);
    }

    @Override
    public void addParagraph(TemplateParagraphSpec paragraph) {
        requireRoot().addParagraph(builder -> builder
                .name(paragraph.name())
                .text(paragraph.text())
                .textStyle(paragraph.style())
                .align(paragraph.align())
                .lineSpacing(paragraph.lineSpacing())
                .bulletOffset(paragraph.bulletOffset())
                .indentStrategy(paragraph.indentStrategy())
                .link(paragraph.linkOptions())
                .padding(paragraph.padding())
                .margin(paragraph.margin()));
    }

    @Override
    public void addList(TemplateListSpec list) {
        requireRoot().addList(builder -> builder
                .name(list.name())
                .items(list.items())
                .marker(list.marker())
                .textStyle(list.style())
                .align(list.align())
                .lineSpacing(list.lineSpacing())
                .itemSpacing(list.itemSpacing())
                .continuationIndent(list.continuationIndent())
                .normalizeMarkers(list.normalizeMarkers())
                .padding(list.padding())
                .margin(list.margin()));
    }

    @Override
    public void addDivider(TemplateDividerSpec divider) {
        requireRoot().addDivider(builder -> builder
                .name(divider.name())
                .width(divider.width())
                .thickness(divider.thickness())
                .color(divider.color())
                .padding(new Padding(8, 0, 8, 0))
                .margin(divider.margin()));
    }

    @Override
    public void addTable(TemplateTableSpec table) {
        DocumentDsl.TableBuilder builder = session.dsl()
                .table()
                .name(table.name())
                .columns(table.columns().toArray(TableColumnSpec[]::new))
                .defaultCellStyle(table.defaultCellStyle())
                .width(table.width())
                .padding(table.padding())
                .margin(table.margin());

        table.columnStyles().forEach(builder::columnStyle);
        table.rowStyles().forEach(builder::rowStyle);
        table.rows().forEach(builder::row);

        requireRoot().add(builder.build());
    }

    @Override
    public void addPageBreak(String name) {
        requireRoot().addPageBreak(builder -> builder.name(name));
    }

    @Override
    public void finishDocument() {
        requireRoot().build();
        root = null;
    }

    private DocumentDsl.PageFlowBuilder requireRoot() {
        if (root == null) {
            throw new IllegalStateException("Template document flow has not been started.");
        }
        return root;
    }
}
