package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.SpacerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.Face;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACCENT_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.DISPLAY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.HEADING_SUFFIX_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.LATO;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MAIN_HEADING_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.NO_STROKE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.cellStyle;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.style;

/**
 * The pieces more than one part of the Orange Ops sheet draws: the two kinds of
 * heading, an inline mark, and the badge disc.
 *
 * <h2>The two kinds of heading rule</h2>
 *
 * <p>In the aside the accent rule under a heading is exactly as wide as the
 * heading's own text, and no API measures a string — so the pair is a two-row
 * table on a single {@link DocumentTableColumn#auto()} column: the column sizes
 * itself to the heading, and the second row is a hairline-high spacer in an
 * accent-filled cell, which paints across whatever width the column resolved
 * to. In the main column the rule is a fixed short length and is a plain line.
 * Both come from the design; neither is a guess about the other.</p>
 */
final class OrangeOpsWidgets {

    private OrangeOpsWidgets() {
    }

    /** Places a packaged mark inline, on the line of the paragraph it opens. */
    static void mark(ParagraphBuilder paragraph, String token) {
        paragraph.inlineSvgIcon(OrangeOpsIcons.icon(token), OrangeOpsIcons.size(token),
                InlineImageAlignment.CENTER);
    }

    /**
     * The type size a paragraph needs so an inline mark is not clipped.
     *
     * <p>An inline run is measured into the line box the paragraph's style
     * gives it, so a mark taller than its label is drawn clipped to the label
     * unless the box is opened up first.</p>
     */
    static double markLineSize(String token, double labelSize) {
        return Math.max(labelSize, OrangeOpsIcons.size(token) / LATO.lineFactor());
    }

    /**
     * An aside heading and its text-width accent rule.
     *
     * @param block the block the heading opens
     * @param name  the node name, for review and rollback
     * @param text  the heading
     */
    static void asideHeading(SectionBuilder block, String name, String text) {
        block.addTable(table -> {
            table.name(name + "Heading");
            table.columns(DocumentTableColumn.auto());
            table.defaultCellStyle(cellStyle(DocumentInsets.zero(),
                    DocumentTableTextAnchor.TOP_LEFT));
            table.rowCells(DocumentTableCell.node(new ParagraphBuilder()
                    .name(name + "HeadingText")
                    .text(text)
                    .lineSpacing(0)
                    .textStyle(style(DISPLAY_FONT, HEADING_SIZE, INK, true))
                    .build()));
            table.rowStyle(0, cellStyle(new DocumentInsets(0, 0, HEADING_TO_RULE, 0),
                    DocumentTableTextAnchor.TOP_LEFT));
            table.rowCells(DocumentTableCell.node(new SpacerBuilder()
                            .name(name + "HeadingRule")
                            .height(ACCENT_RULE_THICKNESS)
                            .build())
                    .withStyle(DocumentTableStyle.builder()
                            .padding(DocumentInsets.zero())
                            .fillColor(ACCENT)
                            .textAnchor(DocumentTableTextAnchor.TOP_LEFT)
                            .stroke(NO_STROKE)
                            .build()));
        });
    }

    /**
     * A main-column heading and its fixed-length accent rule.
     *
     * @param block  the block the heading opens
     * @param name   the node name, for review and rollback
     * @param text   the heading
     * @param suffix the smaller parenthetical set on the same line, or
     *               {@code null} when the heading stands alone
     */
    static void mainHeading(SectionBuilder block, String name, String text, String suffix) {
        block.addParagraph(p -> {
            p.name(name + "Heading");
            p.lineSpacing(0);
            p.textStyle(style(DISPLAY_FONT, HEADING_SIZE, INK, true));
            p.inlineText(text, style(DISPLAY_FONT, HEADING_SIZE, INK, true));
            if (suffix != null && !suffix.isBlank()) {
                p.inlineText("  ", style(BODY_FONT, HEADING_SUFFIX_SIZE, INK, false));
                p.inlineText(suffix, style(BODY_FONT, HEADING_SUFFIX_SIZE, INK, false));
            }
            p.margin(0f, 0f, (float) HEADING_TO_RULE, 0f);
        });
        block.addLine(line -> line
                .name(name + "HeadingRule")
                .horizontal(MAIN_HEADING_RULE_WIDTH)
                .thickness(ACCENT_RULE_THICKNESS)
                .color(ACCENT));
    }

    /**
     * One block of a column, and what its last line is set in.
     *
     * <p>The join rule below a block is spaced from the ink above it, and what
     * that ink is differs per block — so a block carries the face of its own
     * last line rather than the column guessing.</p>
     *
     * @param name             the node name of the rule that follows, for review
     * @param render           draws the block into the column
     * @param trailingFace     the face of the block's last line
     * @param trailingSize     that line's type size
     * @param trailingDescends whether the design's own last line there descends
     */
    record Block(String name, Consumer<SectionBuilder> render, Face trailingFace,
                 double trailingSize, boolean trailingDescends) {
    }

    /**
     * Draws a column's blocks with a hairline between each neighbouring pair.
     *
     * <p>A join takes the spacing the design gives the join in that position,
     * and a column carrying more blocks than the design's four reuses the last
     * row of {@code joinInk}, which is the tightest. A berth nobody filled
     * simply is not in {@code blocks}, so it takes its rule with it rather than
     * leaving a hairline over a gap.</p>
     *
     * @param column  the column to draw into
     * @param blocks  the blocks that have content, in the order the design has them
     * @param joinInk each join's {above, below} spacing in design pixels
     */
    static void stack(SectionBuilder column, List<Block> blocks, double[][] joinInk) {
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            block.render().accept(column);
            if (i == blocks.size() - 1) {
                continue;
            }
            double[] ink = joinInk[Math.min(i, joinInk.length - 1)];
            double above = OrangeOpsStyles.boxGap(ink[0], block.trailingFace(),
                    block.trailingSize(), block.trailingDescends(), null, 0);
            double below = OrangeOpsStyles.boxGap(ink[1], null, 0, DISPLAY, HEADING_SIZE);
            column.addLine(line -> line
                    .name("After" + block.name() + "Rule")
                    .fill()
                    .thickness(RULE_THICKNESS)
                    .color(RULE)
                    .margin(new DocumentInsets(above, 0, below, 0)));
        }
    }

    /** A body split into the lines it was written as, blanks dropped. */
    static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split(String.valueOf((char) 10))) {
            if (!line.isBlank()) {
                out.add(line.strip());
            }
        }
        return out;
    }

    /**
     * A coloured disc with the mark centred inside it, as one inline run.
     *
     * @param name     the node name, for review and rollback
     * @param token    the mark to draw inside the disc
     * @param diameter the disc's diameter in points
     * @param fill     the disc colour
     * @return a paragraph carrying the badge as its only run
     */
    static DocumentNode badge(String name, String token, double diameter, DocumentColor fill) {
        return new ParagraphBuilder()
                .name(name)
                .lineSpacing(0)
                .textStyle(style(BODY_FONT, diameter / LATO.lineFactor(), BODY, false))
                .inlineSvgIcon(OrangeOpsIcons.badge(token, fill), diameter,
                        InlineImageAlignment.CENTER)
                .build();
    }
}
