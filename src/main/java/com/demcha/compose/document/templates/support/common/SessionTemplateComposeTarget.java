package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.AbstractFlowBuilder;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.dsl.ModuleBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;

import java.util.Objects;

/**
 * {@link TemplateComposeTarget} backed by the canonical {@link DocumentSession}
 * DSL.
 *
 * @author Artem Demchyshyn
 */
public final class SessionTemplateComposeTarget implements TemplateComposeTarget {
    private final DocumentSession session;
    private PageFlowBuilder root;

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
    public void addRow(TemplateRowSpec row) {
        addRow(requireRoot(), row);
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

    private void addParagraph(AbstractFlowBuilder<?, ?> flow, TemplateParagraphSpec paragraph) {
        flow.addParagraph(builder -> builder
                .name(paragraph.name())
                .text(paragraph.text())
                .textStyle(TemplateDocumentAdapters.textStyle(paragraph.style()))
                .align(paragraph.align())
                .lineSpacing(paragraph.lineSpacing())
                .bulletOffset(paragraph.bulletOffset())
                .indentStrategy(TemplateDocumentAdapters.indent(paragraph.indentStrategy()))
                .link(paragraph.linkOptions())
                .padding(TemplateDocumentAdapters.insets(paragraph.padding()))
                .margin(TemplateDocumentAdapters.insets(paragraph.margin()))
                .inlineRuns(paragraph.inlineTextRuns()));
    }

    private void addModule(AbstractFlowBuilder<?, ?> flow, TemplateModuleSpec module) {
        TemplateModuleSpec safeModule = Objects.requireNonNull(module, "module");
        long startNanos = TemplateLifecycleLog.moduleStart(safeModule);
        try {
            flow.addModule(builder -> {
                builder.name(safeModule.name());
                applyTitle(builder, safeModule.title());
                FlowTemplateComposeTarget nestedTarget = new FlowTemplateComposeTarget(builder);
                for (TemplateModuleBlock block : safeModule.blocks()) {
                    block.render(nestedTarget);
                }
            });
            TemplateLifecycleLog.moduleSuccess(safeModule, startNanos);
        } catch (RuntimeException | Error failure) {
            TemplateLifecycleLog.moduleFailure(safeModule, startNanos, failure);
            throw failure;
        }
    }

    private static void applyTitle(ModuleBuilder builder, TemplateParagraphSpec title) {
        if (title == null) {
            return;
        }
        builder.title(title.text())
                .titleStyle(TemplateDocumentAdapters.textStyle(title.style()))
                .titleAlign(title.align())
                .titleLineSpacing(title.lineSpacing())
                .titlePadding(TemplateDocumentAdapters.insets(title.padding()))
                .titleMargin(TemplateDocumentAdapters.insets(title.margin()));
    }

    private void addList(AbstractFlowBuilder<?, ?> flow, TemplateListSpec list) {
        flow.addList(builder -> builder
                .name(list.name())
                .items(list.items())
                .marker(list.marker())
                .textStyle(TemplateDocumentAdapters.textStyle(list.style()))
                .align(list.align())
                .lineSpacing(list.lineSpacing())
                .itemSpacing(list.itemSpacing())
                .continuationIndent(list.continuationIndent())
                .normalizeMarkers(list.normalizeMarkers())
                .padding(TemplateDocumentAdapters.insets(list.padding()))
                .margin(TemplateDocumentAdapters.insets(list.margin())));
    }

    private void addDivider(AbstractFlowBuilder<?, ?> flow, TemplateDividerSpec divider) {
        flow.addDivider(builder -> builder
                .name(divider.name())
                .width(divider.width())
                .thickness(divider.thickness())
                .color(divider.color())
                .padding(new DocumentInsets(8, 0, 8, 0))
                .margin(TemplateDocumentAdapters.insets(divider.margin())));
    }

    private void addRow(AbstractFlowBuilder<?, ?> flow, TemplateRowSpec row) {
        TemplateRowSpec safeRow = Objects.requireNonNull(row, "row");
        RowBuilder builder = new RowBuilder()
                .name(safeRow.name())
                .spacing(safeRow.gap())
                .padding(TemplateDocumentAdapters.insets(safeRow.padding()))
                .margin(TemplateDocumentAdapters.insets(safeRow.margin()));
        if (!safeRow.weights().isEmpty()) {
            double[] weights = safeRow.weights().stream().mapToDouble(Double::doubleValue).toArray();
            builder.weights(weights);
        }
        for (TemplateColumnSpec column : safeRow.columns()) {
            builder.addSection(column.name(), section -> {
                section.spacing(column.spacing())
                        .padding(TemplateDocumentAdapters.insets(column.padding()))
                        .margin(TemplateDocumentAdapters.insets(column.margin()));
                FlowTemplateComposeTarget nestedTarget = new FlowTemplateComposeTarget(section);
                for (TemplateModuleBlock block : column.blocks()) {
                    block.render(nestedTarget);
                }
            });
        }
        flow.add(builder.build());
    }

    private void addTable(AbstractFlowBuilder<?, ?> flow, TemplateTableSpec table) {
        TableBuilder builder = session.dsl()
                .table()
                .name(table.name())
                .columns(table.columns().stream()
                        .map(TemplateDocumentAdapters::tableColumn)
                        .toArray(DocumentTableColumn[]::new))
                .defaultCellStyle(TemplateDocumentAdapters.tableStyle(table.defaultCellStyle()))
                .width(table.width())
                .padding(TemplateDocumentAdapters.insets(table.padding()))
                .margin(TemplateDocumentAdapters.insets(table.margin()));

        table.columnStyles().forEach((column, style) ->
                builder.columnStyle(column, TemplateDocumentAdapters.tableStyle(style)));
        table.rowStyles().forEach((row, style) ->
                builder.rowStyle(row, TemplateDocumentAdapters.tableStyle(style)));
        table.rows().forEach(row -> builder.rowCells(TemplateDocumentAdapters.tableRow(row)));

        flow.add(builder.build());
    }

    private PageFlowBuilder requireRoot() {
        if (root == null) {
            throw new IllegalStateException("Template document flow has not been started.");
        }
        return root;
    }

    private final class FlowTemplateComposeTarget implements TemplateComposeTarget {
        private final AbstractFlowBuilder<?, ?> flow;

        private FlowTemplateComposeTarget(AbstractFlowBuilder<?, ?> flow) {
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
        public void addRow(TemplateRowSpec row) {
            SessionTemplateComposeTarget.this.addRow(flow, row);
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
