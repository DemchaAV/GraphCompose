package com.demcha.compose.document.templates.receipt.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptField;

import java.util.List;
import java.util.Objects;

/**
 * Field rows in the two forms a receipt needs.
 *
 * <p>{@link #renderAll} is the full-width form: label on the left, value hard
 * against the right edge, and a dotted leader stretched between them. The
 * leader is the same construction the table-of-contents builder uses, and it
 * is here for the same reason — across a full page width a bare label and a
 * bare value read as two unrelated columns, and the reader has to track which
 * value belongs to which row. Values right-align because that is what makes a
 * column of them scannable: a reader comparing two account numbers compares
 * their ends, not their beginnings.</p>
 *
 * <p>{@link #renderInline} is the narrow form, and it exists for a hard engine
 * rule rather than for taste: a row cannot contain another row, so fields
 * sitting inside a column of the hero panel or of the party panel cannot use
 * the leader form. They render as one paragraph per field, label and value on
 * the same line in their own weights — which is legible at that width without
 * a leader.</p>
 */
public final class FieldRowRenderer {

    /** Gap under each row, in points. */
    private static final double ROW_BOTTOM_PADDING = 4.0;

    /** Horizontal gap between the label, the leader, and the value. */
    private static final double LEADER_GAP = 6.0;

    /**
     * A bottom-aligned line lands on the descender line; lifting it by roughly
     * the font's descent seats it on the text baseline. Same approximation the
     * table-of-contents builder makes, for the same reason: the exact descent
     * is a font metric the DSL layer cannot reach.
     */
    private static final double LEADER_BASELINE_LIFT_RATIO = 0.2;

    private static final double LEADER_THICKNESS = 1.0;

    /** Dash pattern that renders as dots once the caps are rounded. */
    private static final double LEADER_DOT = 0.1;

    private static final double LEADER_DOT_GAP = 4.0;

    private FieldRowRenderer() {
    }

    /**
     * Renders every field of a block as a leader row.
     *
     * @param host   section the rows are appended to
     * @param fields rows to render; an empty list renders nothing
     * @param theme  active theme
     */
    public static void renderAll(SectionBuilder host, List<ReceiptField> fields, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(theme, "theme");
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (ReceiptField field : fields) {
            render(host, field, theme);
        }
    }

    /**
     * Renders every field as a single line each — the form to use inside a
     * column that is already part of a row.
     *
     * @param host   section the lines are appended to
     * @param fields rows to render; an empty list renders nothing
     * @param align  where each line sits in its column
     * @param theme  active theme
     */
    public static void renderInline(SectionBuilder host, List<ReceiptField> fields,
                                    TextAlign align, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(theme, "theme");
        if (fields == null || fields.isEmpty()) {
            return;
        }
        DocumentTextStyle labelStyle = ReceiptStyles.label(theme);
        TextAlign lineAlign = align == null ? TextAlign.LEFT : align;
        for (ReceiptField field : fields) {
            DocumentTextStyle valueStyle = field.emphasized()
                    ? ReceiptStyles.valueStrong(theme)
                    : ReceiptStyles.value(theme);
            host.addParagraph(p -> p
                    .inlineText(field.label() + "  ", labelStyle)
                    .inlineText(field.value(), valueStyle)
                    .align(lineAlign)
                    .margin(DocumentInsets.zero()));
        }
    }

    /**
     * Renders one leader row.
     *
     * @param host  section the row is appended to
     * @param field the label/value pair
     * @param theme active theme
     */
    public static void render(SectionBuilder host, ReceiptField field, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(theme, "theme");

        DocumentTextStyle labelStyle = ReceiptStyles.label(theme);
        DocumentTextStyle valueStyle = field.emphasized()
                ? ReceiptStyles.valueStrong(theme)
                : ReceiptStyles.value(theme);
        double baselineLift = labelStyle.size() * LEADER_BASELINE_LIFT_RATIO;

        host.addRow("ReceiptFieldRow", row -> row
                .gap(LEADER_GAP)
                // Bottom-aligned so the leader lands on the row's baseline rather
                // than riding along the top of the line.
                .verticalAlign(RowVerticalAlign.BOTTOM)
                .columns(DocumentRowColumn.auto(),
                        DocumentRowColumn.weight(1),
                        DocumentRowColumn.auto())
                .padding(new DocumentInsets(0, 0, ROW_BOTTOM_PADDING, 0))
                .addParagraph(p -> p
                        .text(field.label())
                        .textStyle(labelStyle)
                        .margin(DocumentInsets.zero()))
                .addLine(line -> line
                        .fill()
                        .stroke(DocumentStroke.of(theme.palette().rule(), LEADER_THICKNESS))
                        .dashed(LEADER_DOT, LEADER_DOT_GAP)
                        .lineCap(DocumentLineCap.ROUND)
                        .margin(DocumentInsets.bottom(baselineLift)))
                // No explicit right alignment: the column is auto-sized to the
                // value, so the text already ends at the row's right edge — and
                // asking for RIGHT here would make the paragraph claim the full
                // row width, which an auto column cannot grant.
                .addParagraph(p -> p
                        .text(field.value())
                        .textStyle(valueStyle)
                        .margin(DocumentInsets.zero())));
    }
}
