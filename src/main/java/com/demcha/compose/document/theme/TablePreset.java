package com.demcha.compose.document.theme;

import com.demcha.compose.document.table.DocumentTableStyle;

import java.util.Objects;

/**
 * Reusable table style preset for business documents.
 *
 * <p>The preset bundles the four most common per-cell style overrides used when
 * authoring invoice, proposal, and report tables. Templates apply
 * {@link #defaultCellStyle()} as the table default, then attach {@link #headerStyle()}
 * to the first row, {@link #totalRowStyle()} to the totals row, and use
 * {@link #zebraStyle()} for alternating-row tinting.</p>
 *
 * @param defaultCellStyle base cell style (padding, body text style, borders)
 * @param headerStyle override for header rows
 * @param totalRowStyle override for total/summary rows
 * @param zebraStyle override applied to every other body row for zebra tinting
 *
 * @author Artem Demchyshyn
 */
public record TablePreset(
        DocumentTableStyle defaultCellStyle,
        DocumentTableStyle headerStyle,
        DocumentTableStyle totalRowStyle,
        DocumentTableStyle zebraStyle
) {
    /**
     * Validates required style tokens.
     */
    public TablePreset {
        Objects.requireNonNull(defaultCellStyle, "defaultCellStyle");
        Objects.requireNonNull(headerStyle, "headerStyle");
        Objects.requireNonNull(totalRowStyle, "totalRowStyle");
        Objects.requireNonNull(zebraStyle, "zebraStyle");
    }
}
