package com.demcha.compose.document.table;

import com.demcha.compose.document.node.DocumentNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Public table cell payload for the canonical DSL.
 *
 * <p>A cell carries either plain text {@code lines} (the v1.4 / v1.5
 * shape) or a composed {@link DocumentNode} {@code content} (v1.6 +).
 * When {@code content} is non-null the layout pipeline prepares the
 * child node with the cell's resolved inner width as a constraint,
 * uses the prepared height to size the row, and the renderer
 * dispatches the child's fragments through the standard
 * {@code NodeDefinition} pipeline. The cell's {@code lines} field
 * is ignored in that case (use {@link #node(DocumentNode)} for the
 * composed shape and {@link #text(String)} / {@link #lines(String...)}
 * for the plain-text shape).</p>
 *
 * <p>Use plain string rows for the common case and this type only
 * when a cell needs multiple lines, a per-cell style override, a
 * {@code colSpan}, a {@code rowSpan}, or a composed child node.</p>
 *
 * <p>{@code rowSpan} merges the cell vertically across the next
 * {@code rowSpan - 1} rows. The layout layer skips occupied grid
 * positions when interpreting subsequent rows, so authors only
 * specify the cells that are not yet covered by a prior row's
 * spanning cell.</p>
 *
 * @param lines   text lines rendered inside the cell when {@code content} is {@code null}
 * @param style   optional style override
 * @param colSpan number of columns the cell occupies (must be {@code >= 1})
 * @param rowSpan number of rows the cell occupies (must be {@code >= 1})
 * @param content optional composed child node; when non-null the cell
 *                renders the child instead of the plain {@code lines}
 * @author Artem Demchyshyn
 */
public record DocumentTableCell(
        List<String> lines,
        DocumentTableStyle style,
        int colSpan,
        int rowSpan,
        DocumentNode content) {

    /**
     * Creates a normalized table cell payload.
     */
    public DocumentTableCell {
        if (colSpan < 1) {
            throw new IllegalArgumentException("colSpan must be >= 1: " + colSpan);
        }
        if (rowSpan < 1) {
            throw new IllegalArgumentException("rowSpan must be >= 1: " + rowSpan);
        }
        lines = normalizeLines(lines);
    }

    /**
     * Backward-compatible 4-arg constructor (v1.4 / v1.5 shape) that
     * defaults {@code content} to {@code null} (plain-text cell).
     *
     * @param lines   text lines rendered inside the cell
     * @param style   optional style override
     * @param colSpan number of columns the cell occupies (must be {@code >= 1})
     * @param rowSpan number of rows the cell occupies (must be {@code >= 1})
     */
    public DocumentTableCell(List<String> lines, DocumentTableStyle style, int colSpan, int rowSpan) {
        this(lines, style, colSpan, rowSpan, null);
    }

    /**
     * Backward-compatible 3-arg constructor for callers that do not need
     * {@code rowSpan}. Defaults {@code rowSpan} to {@code 1} and
     * {@code content} to {@code null}.
     *
     * @param lines   text lines rendered inside the cell
     * @param style   optional style override
     * @param colSpan number of columns the cell occupies (must be {@code >= 1})
     */
    public DocumentTableCell(List<String> lines, DocumentTableStyle style, int colSpan) {
        this(lines, style, colSpan, 1, null);
    }

    /**
     * Backward-compatible 2-arg constructor for callers that do not need column
     * or row spans. Defaults both spans to {@code 1} and {@code content} to
     * {@code null}.
     *
     * @param lines text lines rendered inside the cell
     * @param style optional style override
     */
    public DocumentTableCell(List<String> lines, DocumentTableStyle style) {
        this(lines, style, 1, 1, null);
    }

    /**
     * Creates a one-line text cell.
     *
     * @param text cell text
     * @return table cell
     */
    public static DocumentTableCell text(String text) {
        return new DocumentTableCell(List.of(text == null ? "" : text), null, 1, 1, null);
    }

    /**
     * Creates a multi-line text cell.
     *
     * @param lines text lines
     * @return table cell
     */
    public static DocumentTableCell lines(String... lines) {
        if (lines == null || lines.length == 0) {
            return text("");
        }
        return new DocumentTableCell(Arrays.asList(lines), null, 1, 1, null);
    }

    /**
     * Creates a composed cell whose content is the given
     * {@link DocumentNode}. The cell renders the child node (with
     * its own style, alignment, padding, and inline runs) inside
     * the cell's bounds; the cell's own {@code lines} are unused
     * when {@code content} is non-null.
     *
     * @param child composed child node, must not be {@code null}
     * @return table cell carrying the composed child
     * @throws NullPointerException when {@code child} is {@code null}
     */
    public static DocumentTableCell node(DocumentNode child) {
        Objects.requireNonNull(child, "child");
        return new DocumentTableCell(List.of(), null, 1, 1, child);
    }

    /**
     * Creates a copy with a style override.
     *
     * @param style style override
     * @return styled table cell
     */
    public DocumentTableCell withStyle(DocumentTableStyle style) {
        return new DocumentTableCell(lines, style, colSpan, rowSpan, content);
    }

    /**
     * Creates a copy that spans the given number of adjacent columns.
     *
     * <p>The sum of {@code colSpan} values across a row must equal the table
     * column count, accounting for rows occupied by prior spanning cells.
     * Use {@code 1} for a regular single-column cell.</p>
     *
     * @param span number of columns the cell occupies (must be {@code >= 1})
     * @return spanned table cell
     */
    public DocumentTableCell colSpan(int span) {
        return new DocumentTableCell(lines, style, span, rowSpan, content);
    }

    /**
     * Creates a copy that spans the given number of adjacent rows.
     *
     * <p>{@code rowSpan} merges the cell vertically across the next
     * {@code rowSpan - 1} rows. Authors omit cells from rows that are
     * fully covered by a prior spanning cell — the layout layer skips
     * those grid positions.</p>
     *
     * @param span number of rows the cell occupies (must be {@code >= 1})
     * @return row-spanned table cell
     */
    public DocumentTableCell rowSpan(int span) {
        return new DocumentTableCell(lines, style, colSpan, span, content);
    }

    /**
     * Returns whether this cell carries a composed child node rather
     * than plain text lines.
     *
     * @return {@code true} when {@link #content()} is non-null
     */
    public boolean hasComposedContent() {
        return content != null;
    }

    private static List<String> normalizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of("");
        }

        List<String> normalized = new ArrayList<>(lines.size());
        for (String line : lines) {
            normalized.add(Objects.requireNonNullElse(line, ""));
        }
        return List.copyOf(normalized);
    }
}
