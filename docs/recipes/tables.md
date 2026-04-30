# Advanced tables

Phase D of the v1.5 release lifts the canonical table from "fixed
header + body rows" to a feature set that covers most rendered-report
patterns:

| Feature | DSL entry point |
| --- | --- |
| Column span | `DocumentTableCell.text(...).colSpan(int)` |
| Row span | `DocumentTableCell.text(...).rowSpan(int)` |
| Header row alias | `TableBuilder.headerRow(String...)` |
| Totals row | `TableBuilder.totalRow(String...)` |
| Zebra rows | `TableBuilder.zebra(odd, even)` |
| Repeated header on page break | `TableBuilder.repeatHeader()` |

All the new pieces compose. A table can have a row-spanning side cell,
zebra striping on the data rows, a bold totals row at the bottom, and
a header that re-emits at the top of every continuation page when the
table paginates.

## Row span — merge a cell vertically

Spanning cells declare how many rows they cover via `rowSpan(int)`.
The layout layer skips occupied grid positions when interpreting
subsequent source rows, so authors only specify the cells that are
not yet covered by a prior spanning cell.

```java
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;

addTable(table -> table
        .columns(
                DocumentTableColumn.auto(),
                DocumentTableColumn.auto(),
                DocumentTableColumn.auto())
        .defaultCellStyle(bordered)
        // Row 0: Tall middle cell spans BOTH rows below it.
        .rowCells(
                DocumentTableCell.text("A0"),
                DocumentTableCell.text("Tall middle\n(spans 3 rows)").rowSpan(3),
                DocumentTableCell.text("C0"))
        // Row 1: only A1 + C1 — middle is occupied by the spanning cell.
        .rowCells(
                DocumentTableCell.text("A1"),
                DocumentTableCell.text("C1"))
        .rowCells(
                DocumentTableCell.text("A2"),
                DocumentTableCell.text("C2")));
```

Row span composes with `colSpan` — a single cell can be both
`colSpan(2).rowSpan(3)` to merge a 2x3 block. The layout layer raises
a precise diagnostic if a span overlaps another cell, exceeds the
remaining rows / columns, or leaves a gap.

## Zebra — alternating row fills

`zebra(odd, even)` paints odd-indexed rows (0, 2, 4 — first, third,
fifth visually) in one fill and even-indexed rows (1, 3, 5) in
another. Either argument may be `null` to skip painting that parity.

```java
import com.demcha.compose.document.style.DocumentColor;

DocumentColor zebraOdd = DocumentColor.rgb(244, 247, 252);
DocumentColor zebraEven = DocumentColor.WHITE;

addTable(table -> table
        .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
        .defaultCellStyle(bordered)
        .headerRow("Item", "Qty", "Amount")
        .row("Apples", "12", "$24.00")
        .row("Pears", "6", "$18.00")
        .row("Strawberries", "20", "$40.00")
        .row("Mangoes", "4", "$16.00")
        .zebra(zebraOdd, zebraEven));
```

A two-arg overload accepts full `DocumentTableStyle` values when the
zebra row needs more than just a fill colour:

```java
.zebra(
        DocumentTableStyle.builder().fillColor(zebraOdd).padding(DocumentInsets.of(8)).build(),
        DocumentTableStyle.builder().fillColor(zebraEven).padding(DocumentInsets.of(8)).build())
```

Zebra is applied lazily at `build()` time and only to rows that don't
already have an explicit `rowStyle(idx, ...)` override, so
`headerStyle(...)` and `totalRow(...)` always win.

## Totals row — bold + subtle fill

`totalRow(values...)` appends the row at the end of the table and
assigns a default totals style (bold text + a subtle gray-blue fill).
A two-arg overload takes a custom `DocumentTableStyle` for branded
totals rows.

```java
DocumentTableStyle goldenTotal = DocumentTableStyle.builder()
        .fillColor(DocumentColor.rgb(232, 220, 180))
        .stroke(DocumentStroke.of(rule, 0.6))
        .padding(DocumentInsets.of(7))
        .textStyle(DocumentTextStyle.builder()
                .decoration(DocumentTextDecoration.BOLD)
                .color(ink)
                .build())
        .build();

addTable(table -> table
        .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
        .defaultCellStyle(bordered)
        .headerRow("Item", "Qty", "Amount")
        .row("Apples",  "12", "$24.00")
        .row("Pears",   "6",  "$18.00")
        .row("Mangoes", "4",  "$16.00")
        .totalRow(goldenTotal, "Total", "22", "$58.00")
        .zebra(zebraOdd, zebraEven));
```

The totals row is registered with an explicit `rowStyle` entry at the
last row index, so it wins over zebra alternation on that row
regardless of parity.

## Repeated header on page break

`repeatHeader()` repeats the first row at the top of every
continuation page when the table is split across pages.
`repeatHeader(int)` repeats N leading rows — useful when you have
both a title row AND a column-header row that should both repeat.

```java
addTable(table -> {
    TableBuilder t = table
            .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
            .defaultCellStyle(bordered)
            .headerRow("Item", "Amount")
            .headerStyle(headerStyle)
            .repeatHeader();              // ← header re-emits on every page
    for (int i = 1; i <= 60; i++) {
        t.row("Row " + i, String.format("$%d.00", i));
    }
});
```

`repeatHeader(0)` is the default and behaves exactly like a v1.4
table — the second page starts directly with whatever data row the
split landed on. Authors opting into the feature only pay for it when
they explicitly call the method.

## Put it all together

The runnable example
[`examples/.../TableAdvancedExample.java`](../../examples/src/main/java/com/demcha/examples/TableAdvancedExample.java)
combines every Phase D feature on one PDF: a 3-column invoice with a
row-spanning side note, zebra body rows, a totals row, and a
repeating "Item / Qty / Amount" header on every continuation page.
The output lands at `examples/target/generated-pdfs/table-advanced.pdf`
on every full example sweep.

## Layout invariants you can rely on

The Phase D feature set pins five test invariants:

1. **Row span placement** — `TableBuilderRowSpanTest` verifies the
   spanning cell's height equals the SUM of its covered row heights
   and that subsequent rows correctly skip occupied columns.
2. **Spanning cell render direction** — `TableResolvedCell.yOffset`
   is negative for spanning cells, so they extend visually downward
   through the rows they merge instead of upward beyond the starting
   row.
3. **Zebra precedence** — `TableBuilderZebraAndTotalsTest` verifies
   that explicit row styles (header, totals, manual `rowStyle(idx)`)
   always win over zebra alternation.
4. **Header repeats on every continuation page** —
   `TableBuilderRepeatHeaderTest` walks all pages of a 60-row table
   and asserts every page's first row is the configured header.
5. **Default repeat-header = 0** — tables without a `repeatHeader()`
   call paginate exactly as they did in v1.4, so existing layouts
   stay byte-stable.

## See also

- [`TableBuilder`](../../src/main/java/com/demcha/compose/document/dsl/TableBuilder.java) — full builder API.
- [`DocumentTableCell`](../../src/main/java/com/demcha/compose/document/table/DocumentTableCell.java) — cell payload with `colSpan` / `rowSpan` mutators.
- [`DocumentTableStyle`](../../src/main/java/com/demcha/compose/document/table/DocumentTableStyle.java) — style overrides for fill, stroke, text style, padding.
- Runnable example: `examples/src/main/java/com/demcha/examples/TableAdvancedExample.java`.
