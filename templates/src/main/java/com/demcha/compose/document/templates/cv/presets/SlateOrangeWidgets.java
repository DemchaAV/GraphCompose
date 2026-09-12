package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.cv.data.CvEntry;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.CREDENTIAL_RULE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.LINE_FACTOR;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.RULE_TO_SIDEBAR_BODY;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.SIDEBAR_TABLE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.cellStyle;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.SlateOrangeStyles.style;

/**
 * The idioms this sheet repeats: the heading over its rule, a mark as a
 * cell's node, a line of text as a cell's node, the empty cell, and the
 * mark-and-label table both sidebar blocks are built from.
 */
final class SlateOrangeWidgets {

    private SlateOrangeWidgets() {
    }

    /**
     * A caps heading over its orange rule.
     *
     * <p>The rule is filled rather than given a width, so it spans whichever
     * column the heading is in — one method serves the sidebar and the main
     * column alike. The credential columns cannot use it, because they live
     * inside table cells where a line node is not drawn; their rules are
     * filled cells instead.</p>
     */
    static void heading(SectionBuilder block, String name, String text) {
        block.addParagraph(p -> p
                .name(name + "Heading")
                .text(text)
                .lineSpacing(0)
                .textStyle(style(DISPLAY_FONT, HEADING_SIZE, INK, true))
                .margin(0f, 0f, (float) HEADING_TO_RULE, 0f));
        block.addLine(line -> line
                .name(name + "HeadingRule")
                .fill()
                .thickness(RULE_THICKNESS)
                .color(ACCENT));
    }

    /** One line of text as a cell's single node. */
    static DocumentNode text(String name, String value, DocumentTextStyle textStyle) {
        return new ParagraphBuilder()
                .name(name)
                .text(value)
                .lineSpacing(0)
                .textStyle(textStyle)
                .build();
    }

    /** An empty cell that still has to be a paragraph, because a cell holds a node. */
    static DocumentNode spacer(String name) {
        return new ParagraphBuilder()
                .name(name)
                .text(" ")
                .lineSpacing(0)
                .textStyle(style(BODY_FONT, CREDENTIAL_RULE_SIZE, INK, false))
                .build();
    }

    /** Places a mark inline, on the baseline of the paragraph it opens. */
    static void inlineIcon(ParagraphBuilder paragraph, String token) {
        paragraph.inlineSvgIcon(SlateOrangeIcons.icon(token), SlateOrangeIcons.size(token),
                InlineImageAlignment.CENTER);
    }

    /**
     * A mark on its own, as a cell's single node.
     *
     * <p>Styled at the size of the label beside it although it carries no
     * text: the default line box would set the row's pitch instead of the mark
     * doing so. An inline run is measured into that box, so a mark taller than
     * the label would be drawn clipped to it — whichever is larger wins.</p>
     */
    static DocumentNode iconCell(String name, String token, double labelSize) {
        double boxSize = Math.max(labelSize, SlateOrangeIcons.size(token) / LINE_FACTOR);
        ParagraphBuilder paragraph = new ParagraphBuilder();
        paragraph.name(name);
        paragraph.lineSpacing(0);
        paragraph.textStyle(style(BODY_FONT, boxSize, INK, false));
        inlineIcon(paragraph, token);
        return paragraph.build();
    }

    /**
     * The shape both mark-and-label blocks share: a fixed mark column beside a
     * label column, one table row per entry.
     *
     * <p>The pitch is the table's own row padding, so adding an eleventh entry
     * changes nothing about the spacing — the alternative, a bottom margin
     * repeated on every label, is the same fact stated ten times. It is a gap
     * BETWEEN rows, so the last row does not carry it: there it would become a
     * gap between this block and the next, which the next block already
     * owns.</p>
     */
    static void iconTable(SectionBuilder block, String tableName, String rowName,
                          List<CvEntry> entries, double pitch, double iconColumn) {
        block.addTable(table -> {
            table.name(tableName);
            table.margin(new DocumentInsets(RULE_TO_SIDEBAR_BODY, 0, 0, 0));
            table.width(SIDEBAR_TABLE_WIDTH);
            table.columns(
                    DocumentTableColumn.fixed(iconColumn),
                    DocumentTableColumn.fixed(SIDEBAR_TABLE_WIDTH - iconColumn));
            table.defaultCellStyle(cellStyle(
                    new DocumentInsets(0, 0, gap(pitch, rowHeight(entries)), 0),
                    DocumentTableTextAnchor.CENTER_LEFT));
            for (int index = 0; index < entries.size(); index++) {
                CvEntry entry = entries.get(index);
                table.rowCells(
                        DocumentTableCell.node(entry.icon().isBlank()
                                ? spacer(rowName + "Icon_" + index)
                                : iconCell(rowName + "Icon_" + index, entry.icon(), LABEL_SIZE)),
                        DocumentTableCell.node(text(rowName + "Label_" + index, entry.title(),
                                style(BODY_FONT, LABEL_SIZE, INK, false))));
            }
            table.rowStyle(entries.size() - 1,
                    cellStyle(DocumentInsets.zero(), DocumentTableTextAnchor.CENTER_LEFT));
        });
    }

    /** A row is as tall as the tallest mark in the block or its label's line box. */
    private static double rowHeight(List<CvEntry> entries) {
        double tallest = LABEL_SIZE * LINE_FACTOR;
        for (CvEntry entry : entries) {
            if (!entry.icon().isBlank()) {
                tallest = Math.max(tallest, SlateOrangeIcons.size(entry.icon()));
            }
        }
        return tallest;
    }

    /** A node name built from a heading, with the spacing taken out of it. */
    static String nameOf(String value) {
        return compact(value);
    }
}
