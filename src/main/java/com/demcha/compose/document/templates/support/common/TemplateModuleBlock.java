package com.demcha.compose.document.templates.support.common;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * One renderable body block inside a reusable template module.
 *
 * <p><b>Pipeline role:</b> keeps higher-level template modules semantic while
 * still rendering through the existing paragraph, list, table, divider, and
 * page-break target methods.</p>
 *
 * @author Artem Demchyshyn
 */
public sealed interface TemplateModuleBlock permits
        TemplateModuleBlock.Paragraph,
        TemplateModuleBlock.ListBlock,
        TemplateModuleBlock.Table,
        TemplateModuleBlock.Divider,
        TemplateModuleBlock.PageBreak,
        TemplateModuleBlock.Custom {

    /**
     * Renders this block into the supplied target.
     *
     * @param target active template compose target
     */
    void render(TemplateComposeTarget target);

    /**
     * Creates a paragraph module block.
     *
     * @param paragraph paragraph instruction
     * @return module block
     */
    static TemplateModuleBlock paragraph(TemplateParagraphSpec paragraph) {
        return new Paragraph(paragraph);
    }

    /**
     * Creates a list module block.
     *
     * @param list list instruction
     * @return module block
     */
    static TemplateModuleBlock list(TemplateListSpec list) {
        return new ListBlock(list);
    }

    /**
     * Creates a table module block.
     *
     * @param table table instruction
     * @return module block
     */
    static TemplateModuleBlock table(TemplateTableSpec table) {
        return new Table(table);
    }

    /**
     * Creates a divider module block.
     *
     * @param divider divider instruction
     * @return module block
     */
    static TemplateModuleBlock divider(TemplateDividerSpec divider) {
        return new Divider(divider);
    }

    /**
     * Creates a page-break module block.
     *
     * @param name semantic page-break name
     * @return module block
     */
    static TemplateModuleBlock pageBreak(String name) {
        return new PageBreak(name);
    }

    /**
     * Creates a custom module block for advanced template integrations.
     *
     * @param renderer callback that emits into the active target
     * @return module block
     */
    static TemplateModuleBlock custom(Consumer<TemplateComposeTarget> renderer) {
        return new Custom(renderer);
    }

    /**
     * Paragraph-backed module block.
     *
     * @param paragraph paragraph instruction
     */
    record Paragraph(TemplateParagraphSpec paragraph) implements TemplateModuleBlock {
        /**
         * Validates the paragraph instruction.
         */
        public Paragraph {
            Objects.requireNonNull(paragraph, "paragraph");
        }

        @Override
        public void render(TemplateComposeTarget target) {
            target.addParagraph(paragraph);
        }
    }

    /**
     * List-backed module block.
     *
     * @param list list instruction
     */
    record ListBlock(TemplateListSpec list) implements TemplateModuleBlock {
        /**
         * Validates the list instruction.
         */
        public ListBlock {
            Objects.requireNonNull(list, "list");
        }

        @Override
        public void render(TemplateComposeTarget target) {
            target.addList(list);
        }
    }

    /**
     * Table-backed module block.
     *
     * @param table table instruction
     */
    record Table(TemplateTableSpec table) implements TemplateModuleBlock {
        /**
         * Validates the table instruction.
         */
        public Table {
            Objects.requireNonNull(table, "table");
        }

        @Override
        public void render(TemplateComposeTarget target) {
            target.addTable(table);
        }
    }

    /**
     * Divider-backed module block.
     *
     * @param divider divider instruction
     */
    record Divider(TemplateDividerSpec divider) implements TemplateModuleBlock {
        /**
         * Validates the divider instruction.
         */
        public Divider {
            Objects.requireNonNull(divider, "divider");
        }

        @Override
        public void render(TemplateComposeTarget target) {
            target.addDivider(divider);
        }
    }

    /**
     * Page-break-backed module block.
     *
     * @param name semantic page-break name
     */
    record PageBreak(String name) implements TemplateModuleBlock {
        /**
         * Normalizes the semantic page-break name.
         */
        public PageBreak {
            name = name == null ? "" : name;
        }

        @Override
        public void render(TemplateComposeTarget target) {
            target.addPageBreak(name);
        }
    }

    /**
     * Advanced callback-backed module block.
     *
     * @param renderer callback that emits into the active target
     */
    record Custom(Consumer<TemplateComposeTarget> renderer) implements TemplateModuleBlock {
        /**
         * Validates the custom render callback.
         */
        public Custom {
            Objects.requireNonNull(renderer, "renderer");
        }

        @Override
        public void render(TemplateComposeTarget target) {
            renderer.accept(target);
        }
    }
}
