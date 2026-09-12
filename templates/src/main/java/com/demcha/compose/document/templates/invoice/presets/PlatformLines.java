package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BAND;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.RULE_THICK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.SMALL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_HEADER_H;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_HEAD_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_PAD_CENTRED;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_PAD_FIRST;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_PAD_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_PAD_RIGHT;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_PAD_X;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_ROW_H;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.WHITE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.capPitch;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.style;

/**
 * The usage table: six columns, a filled caps header that repeats on every page
 * it reaches, and a horizontal separator under every row.
 *
 * <h2>Why the separators are rows</h2>
 *
 * <p>A table's rules come from each cell's own style and there is no per-edge
 * control, so any stroke that buys a horizontal separator also buys five
 * verticals — and the design has no vertical rule anywhere inside the table, nor
 * a side border below the header. Every cell is therefore stroked at zero width
 * and the separators are rows of their own: one cell spanning every column, zero
 * padding, and a full-width line as its content.</p>
 */
final class PlatformLines {

    /** The six columns, as shares of the content width, solved from the design. */
    private static final double[] WEIGHTS = {0.2834, 0.1658, 0.1348, 0.123, 0.1487, 0.1443};

    /** The two numeric columns are centred; the four leading ones are not. */
    private static final boolean[] CENTRED = {false, false, false, false, true, true};

    private PlatformLines() {
    }

    /**
     * The table.
     *
     * @param page         the page flow
     * @param serviceLines the lines and their column captions
     * @param currencyCode the code the two money captions carry
     */
    static void render(PageFlowBuilder page, InvoiceServiceLines serviceLines,
                       String currencyCode) {
        List<String> captions = captions(serviceLines.columns(), currencyCode);
        page.addTable(table -> {
            table.name("LineItems").width(CONTENT_W);
            table.margin(new DocumentInsets(py(654 - 622), 0, 0, 0));
            List<DocumentTableColumn> columns = new ArrayList<>();
            for (double weight : WEIGHTS) {
                columns.add(DocumentTableColumn.fixed(CONTENT_W * weight));
            }
            table.columns(columns.toArray(new DocumentTableColumn[0]));

            renderHeader(table, captions);
            // The header's bottom edge, then one separator under every row —
            // including the last, which the design draws.
            table.rowCells(separatorCells());
            List<InvoiceServiceLines.Line> lines = serviceLines.lines();
            for (int i = 0; i < lines.size(); i++) {
                table.rowCells(bodyCells(lines.get(i), i));
                table.rowCells(separatorCells());
            }
        });
    }

    /**
     * The captions, with the currency code appended to the two money columns.
     *
     * <p>The design states the currency once per money column and writes the
     * figures under it bare, which is why the code belongs in the caption rather
     * than in front of every amount.</p>
     */
    private static List<String> captions(InvoiceServiceLines.Columns columns,
                                         String currencyCode) {
        String suffix = currencyCode == null || currencyCode.isBlank()
                ? ""
                : " (" + currencyCode.trim() + ")";
        return List.of(
                columns.description(),
                columns.servicePeriod(),
                columns.region(),
                columns.quantity(),
                columns.unitPrice() + suffix,
                columns.amount() + suffix);
    }

    /**
     * The header band: one cell per column, each carrying the fill so the band
     * reads as continuous, and a repeat instruction so it comes back at the top
     * of every page the table reaches. Without it a continuation page carries
     * unlabelled columns — a defect no single-page render can show.
     *
     * <p>The first heading is inset further than the other five. Measured; the
     * design really is asymmetric there.</p>
     */
    private static void renderHeader(TableBuilder table, List<String> captions) {
        double pad = (TABLE_HEADER_H - LINE_BOX * TABLE_HEAD_SIZE) / 2.0;
        List<DocumentTableCell> cells = new ArrayList<>();
        for (int i = 0; i < captions.size(); i++) {
            DocumentInsets insets = CENTRED[i]
                    ? new DocumentInsets(pad, TABLE_PAD_CENTRED, pad, TABLE_PAD_CENTRED)
                    : new DocumentInsets(pad, TABLE_PAD_RIGHT, pad,
                            i == 0 ? TABLE_PAD_FIRST : TABLE_PAD_X);
            DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                    .padding(insets)
                    .fillColor(BAND)
                    .stroke(DocumentStroke.of(BAND, 0))
                    .textStyle(bold(TABLE_HEAD_SIZE, ACCENT))
                    .textAnchor(CENTRED[i]
                            ? DocumentTableTextAnchor.CENTER
                            : DocumentTableTextAnchor.CENTER_LEFT)
                    .build();
            cells.add(new DocumentTableCell(List.of(captions.get(i)), cellStyle));
        }
        table.headerCells(cells);
        table.repeatHeader();
    }

    /** One usage row: a composed description cell and five plain ones. */
    private static List<DocumentTableCell> bodyCells(InvoiceServiceLines.Line line, int index) {
        double pad = (TABLE_ROW_H - descriptionHeight()) / 2.0;
        List<String> values = List.of(
                "",
                line.servicePeriod(),
                line.region(),
                "",
                PlatformText.rate(line.unitPrice()),
                PlatformText.money(line.amount()));
        List<DocumentTableCell> cells = new ArrayList<>();
        for (int i = 0; i < WEIGHTS.length; i++) {
            DocumentInsets insets = CENTRED[i]
                    ? new DocumentInsets(pad, TABLE_PAD_CENTRED, pad, TABLE_PAD_CENTRED)
                    : new DocumentInsets(pad, TABLE_PAD_RIGHT, pad,
                            i == 0 ? TABLE_PAD_ICON : TABLE_PAD_X);
            DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                    .padding(insets)
                    .fillColor(WHITE)
                    .stroke(DocumentStroke.of(WHITE, 0))
                    .textStyle(style(SMALL_SIZE, MUTED))
                    .lineSpacing(capPitch(23, SMALL_SIZE))
                    .textAnchor(CENTRED[i]
                            ? DocumentTableTextAnchor.CENTER
                            : DocumentTableTextAnchor.CENTER_LEFT)
                    .build();
            if (i == 0) {
                cells.add(new DocumentTableCell(List.of(), cellStyle, 1, 1,
                        PlatformWidgets.descriptionCell(line, index)));
            } else if (i == 3) {
                // The count and what it counts are two lines of one cell, so a
                // long unit falls under the figure rather than widening the
                // column the design measured.
                String usage = PlatformText.usage(line.quantity(), line.unit());
                cells.add(line.unit().isBlank()
                        ? new DocumentTableCell(List.of(usage), cellStyle)
                        : new DocumentTableCell(List.of(usage, line.unit()), cellStyle));
            } else {
                cells.add(new DocumentTableCell(List.of(values.get(i)), cellStyle));
            }
        }
        return cells;
    }

    /** The tallest cell in a row, and therefore what its padding is solved against. */
    private static double descriptionHeight() {
        return Math.max(TABLE_ICON,
                LINE_BOX * BODY_SIZE + capPitch(23, BODY_SIZE, SMALL_SIZE)
                        + LINE_BOX * SMALL_SIZE);
    }

    /** A separator row: one cell across every column, carrying a full-width line. */
    private static List<DocumentTableCell> separatorCells() {
        DocumentTableStyle cellStyle = DocumentTableStyle.builder()
                .padding(DocumentInsets.zero())
                .fillColor(WHITE)
                .stroke(DocumentStroke.of(WHITE, 0))
                .build();
        return List.of(new DocumentTableCell(List.of(), cellStyle, WEIGHTS.length, 1,
                new LineBuilder()
                        .name("LineItemsSeparator")
                        .horizontal(CONTENT_W)
                        .thickness(RULE_THICK)
                        .color(HAIRLINE)
                        .build()));
    }
}
