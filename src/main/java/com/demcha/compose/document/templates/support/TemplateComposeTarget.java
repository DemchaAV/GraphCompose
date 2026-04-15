package com.demcha.compose.document.templates.support;

/**
 * Shared target abstraction used by canonical template scene composers.
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
