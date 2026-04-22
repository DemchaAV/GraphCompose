package com.demcha.compose.document.templates.support.common;

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
        addParagraph(requireRoot(), paragraph);
    }

    @Override
    public void addModule(TemplateModuleSpec module) {
        addModule(requireRoot(), module);
    }

    @Override
    public void addList(TemplateListSpec list) {
        addList(requireRoot(), list);
    }

    @Override
    public void addDivider(TemplateDividerSpec divider) {
        addDivider(requireRoot(), divider);
    }

    @Override
    public void addTable(TemplateTableSpec table) {
        addTable(requireRoot(), table);
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

    private void addParagraph(DocumentDsl.AbstractFlowBuilder<?, ?> flow, TemplateParagraphSpec paragraph) {
        flow.addParagraph(builder -> builder
                .name(paragraph.name())
                .text(paragraph.text())
                .textStyle(paragraph.style())
                .align(paragraph.align())
                .lineSpacing(paragraph.lineSpacing())
                .bulletOffset(paragraph.bulletOffset())
                .indentStrategy(paragraph.indentStrategy())
                .link(paragraph.linkOptions())
                .padding(paragraph.padding())
                .margin(paragraph.margin())
                .inlineRuns(paragraph.inlineTextRuns()));
    }

    private void addModule(DocumentDsl.AbstractFlowBuilder<?, ?> flow, TemplateModuleSpec module) {
        TemplateModuleSpec safeModule = Objects.requireNonNull(module, "module");
        flow.addModule(builder -> {
            builder.name(safeModule.name());
            applyTitle(builder, safeModule.title());
            FlowTemplateComposeTarget nestedTarget = new FlowTemplateComposeTarget(builder);
            for (TemplateModuleBlock block : safeModule.blocks()) {
                block.render(nestedTarget);
            }
        });
    }

    private static void applyTitle(DocumentDsl.ModuleBuilder builder, TemplateParagraphSpec title) {
        if (title == null) {
            return;
        }
        builder.title(title.text())
                .titleStyle(title.style())
                .titleAlign(title.align())
                .titleLineSpacing(title.lineSpacing())
                .titlePadding(title.padding())
                .titleMargin(title.margin());
    }

    private void addList(DocumentDsl.AbstractFlowBuilder<?, ?> flow, TemplateListSpec list) {
        flow.addList(builder -> builder
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

    private void addDivider(DocumentDsl.AbstractFlowBuilder<?, ?> flow, TemplateDividerSpec divider) {
        flow.addDivider(builder -> builder
                .name(divider.name())
                .width(divider.width())
                .thickness(divider.thickness())
                .color(divider.color())
                .padding(new Padding(8, 0, 8, 0))
                .margin(divider.margin()));
    }

    private void addTable(DocumentDsl.AbstractFlowBuilder<?, ?> flow, TemplateTableSpec table) {
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

        flow.add(builder.build());
    }

    private DocumentDsl.PageFlowBuilder requireRoot() {
        if (root == null) {
            throw new IllegalStateException("Template document flow has not been started.");
        }
        return root;
    }

    private final class FlowTemplateComposeTarget implements TemplateComposeTarget {
        private final DocumentDsl.AbstractFlowBuilder<?, ?> flow;

        private FlowTemplateComposeTarget(DocumentDsl.AbstractFlowBuilder<?, ?> flow) {
            this.flow = Objects.requireNonNull(flow, "flow");
        }

        @Override
        public double pageWidth() {
            return SessionTemplateComposeTarget.this.pageWidth();
        }

        @Override
        public void startDocument(String rootName, double spacing) {
            throw new IllegalStateException("Nested template flows cannot start a new document.");
        }

        @Override
        public void addParagraph(TemplateParagraphSpec paragraph) {
            SessionTemplateComposeTarget.this.addParagraph(flow, paragraph);
        }

        @Override
        public void addModule(TemplateModuleSpec module) {
            SessionTemplateComposeTarget.this.addModule(flow, module);
        }

        @Override
        public void addList(TemplateListSpec list) {
            SessionTemplateComposeTarget.this.addList(flow, list);
        }

        @Override
        public void addDivider(TemplateDividerSpec divider) {
            SessionTemplateComposeTarget.this.addDivider(flow, divider);
        }

        @Override
        public void addTable(TemplateTableSpec table) {
            SessionTemplateComposeTarget.this.addTable(flow, table);
        }

        @Override
        public void addPageBreak(String name) {
            flow.addPageBreak(builder -> builder.name(name));
        }

        @Override
        public void finishDocument() {
            throw new IllegalStateException("Nested template flows do not finish the root document.");
        }
    }
}
