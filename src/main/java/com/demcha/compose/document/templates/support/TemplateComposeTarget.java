package com.demcha.compose.document.templates.support;

import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.Objects;

/**
 * Shared target abstraction used by canonical template scene composers.
 *
 * <p><b>Pipeline role:</b> this is the seam between backend-neutral template
 * scene definitions and the concrete authoring target used during one
 * composition pass. The canonical session-backed implementation writes into
 * {@link com.demcha.compose.document.api.DocumentSession}, while the deprecated
 * compatibility implementation writes into the legacy composer stack.</p>
 *
 * <p>This interface is public for advanced extensions, but it is not the
 * recommended starting point for normal template consumers.</p>
 *
 * @author Artem Demchyshyn
 */
public interface TemplateComposeTarget {

    /**
     * Returns the active inner page width.
     *
     * @return page width available to the template root flow
     */
    double pageWidth();

    /**
     * Starts one root document flow.
     *
     * @param rootName semantic root name
     * @param spacing vertical spacing between top-level blocks
     */
    void startDocument(String rootName, double spacing);

    /**
     * Appends one paragraph block.
     *
     * @param paragraph paragraph instruction
     */
    void addParagraph(TemplateParagraphSpec paragraph);

    /**
     * Appends one titled semantic module.
     *
     * <p>The default implementation keeps the target surface small: module
     * composition is still lowered into existing paragraph, list, table,
     * divider, and page-break operations.</p>
     *
     * @param module module instruction
     */
    default void addModule(TemplateModuleSpec module) {
        TemplateModuleSpec safeModule = Objects.requireNonNull(module, "module");
        if (safeModule.title() != null) {
            addParagraph(safeModule.title());
        }
        for (TemplateModuleBlock block : safeModule.blocks()) {
            block.render(this);
        }
    }

    /**
     * Appends one list block.
     *
     * <p>The default implementation keeps the interface source-compatible for
     * custom targets. Canonical targets should override this method and emit a
     * native list node.</p>
     *
     * @param list list instruction
     */
    default void addList(TemplateListSpec list) {
        for (int index = 0; index < list.items().size(); index++) {
            Padding padding = new Padding(
                    index == 0 ? list.padding().top() : 0.0,
                    list.padding().right(),
                    index == list.items().size() - 1 ? list.padding().bottom() : 0.0,
                    list.padding().left());
            Margin margin = new Margin(
                    index == 0 ? list.margin().top() : 0.0,
                    list.margin().right(),
                    index == list.items().size() - 1 ? list.margin().bottom() : list.itemSpacing(),
                    list.margin().left());
            addParagraph(new TemplateParagraphSpec(
                    list.name() + "_" + index,
                    list.items().get(index),
                    list.style(),
                    list.align(),
                    list.lineSpacing(),
                    listPrefix(list),
                    listIndentStrategy(list),
                    null,
                    padding,
                    margin));
        }
    }

    private static String listPrefix(TemplateListSpec list) {
        return list.marker().isVisible()
                ? list.marker().prefix()
                : list.continuationIndent();
    }

    private static BlockIndentStrategy listIndentStrategy(TemplateListSpec list) {
        if (list.marker().isVisible()) {
            return BlockIndentStrategy.ALL_LINES;
        }
        return list.continuationIndent().isEmpty()
                ? BlockIndentStrategy.NONE
                : BlockIndentStrategy.FROM_SECOND_LINE;
    }

    /**
     * Appends one divider block.
     *
     * @param divider divider instruction
     */
    void addDivider(TemplateDividerSpec divider);

    /**
     * Appends one semantic table block.
     *
     * @param table table instruction
     */
    void addTable(TemplateTableSpec table);

    /**
     * Appends one explicit page-break block.
     *
     * @param name semantic break name
     */
    void addPageBreak(String name);

    /**
     * Finalizes the root flow.
     */
    void finishDocument();
}
