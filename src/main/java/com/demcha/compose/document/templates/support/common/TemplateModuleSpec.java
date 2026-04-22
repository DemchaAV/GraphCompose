package com.demcha.compose.document.templates.support.common;

import java.util.List;

/**
 * Immutable titled module instruction used by canonical template composers.
 *
 * <p><b>Pipeline role:</b> groups one optional heading paragraph plus ordered
 * body blocks so compose-first templates can express section-level structure
 * without introducing a new layout primitive or changing pagination
 * semantics.</p>
 *
 * @param name semantic module name used in diagnostics and graph-oriented
 *             tooling
 * @param title optional module title paragraph, or {@code null} when omitted
 * @param blocks ordered body blocks rendered after the title
 * @author Artem Demchyshyn
 */
public record TemplateModuleSpec(
        String name,
        TemplateParagraphSpec title,
        List<TemplateModuleBlock> blocks
) {
    /**
     * Creates a normalized module instruction.
     */
    public TemplateModuleSpec {
        name = name == null ? "" : name;
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }
}
