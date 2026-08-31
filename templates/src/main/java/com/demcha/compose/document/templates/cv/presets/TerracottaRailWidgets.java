package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;

import java.util.function.Consumer;

import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ACCENT_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.BULLET_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ENTRY_INDENT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.HEADING_TO_BODY_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.HEADING_TO_DASH_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.NO_BORDER;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.PAPER;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SECTION_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.text;

/**
 * The idioms this sheet repeats: the tracked heading, the rule between
 * blocks, the marker riding a rail, the title-and-date line, and the row a
 * column has to wrap to hold.
 */
final class TerracottaRailWidgets {

    private TerracottaRailWidgets() {
    }

    // -- headings ----------------------------------------------------------

    /**
     * A letter-spaced heading.
     *
     * <p>A text style carries no tracking, so the gap between letters is a
     * space character — which one is the whole choice, and the caller makes
     * it. The word gap stays clearly wider than the letter gap, or a tracked
     * heading reads as one long word.</p>
     *
     * @param text   the heading
     * @param spacer the space to set between letters
     * @return the tracked string
     */
    static String tracked(String text, char spacer) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() * 2);
        for (int index = 0; index < text.length(); index++) {
            char letter = text.charAt(index);
            if (letter == ' ') {
                out.append(spacer).append(' ').append(spacer);
                continue;
            }
            if (index > 0 && text.charAt(index - 1) != ' ') {
                out.append(spacer);
            }
            out.append(letter);
        }
        return out.toString();
    }

    /** The masthead's own tracking: a full space between letters. */
    static String trackedWide(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() * 2);
        boolean startOfWord = true;
        for (int index = 0; index < text.length(); index++) {
            char letter = text.charAt(index);
            if (letter == ' ') {
                out.append("   ");
                startOfWord = true;
                continue;
            }
            if (!startOfWord) {
                out.append(' ');
            }
            out.append(letter);
            startOfWord = false;
        }
        return out.toString();
    }

    /** A heading with nothing under it. */
    static void heading(SectionBuilder block, String title, char spacer) {
        block.addParagraph(p -> p
                .name("Heading_" + compact(title))
                .text(tracked(title, spacer))
                .textStyle(text(SECTION_HEADING_SIZE, INK, true))
                .margin(0f, 0f, (float) HEADING_TO_BODY_GAP, 0f));
    }

    /** A heading over a short terracotta dash. */
    static void headingWithDash(SectionBuilder block, String title, char spacer, double dashWidth) {
        block.addParagraph(p -> p
                .name("Heading_" + compact(title))
                .text(tracked(title, spacer))
                .textStyle(text(SECTION_HEADING_SIZE, INK, true))
                .margin(0f, 0f, (float) HEADING_TO_DASH_GAP, 0f));
        block.addLine(line -> line
                .name("AccentLine_" + compact(title))
                .horizontal(dashWidth)
                .thickness(ACCENT_RULE_THICKNESS)
                .color(ACCENT)
                .margin(new DocumentInsets(
                        0, 0, HEADING_TO_BODY_GAP - HEADING_TO_DASH_GAP, 0)));
    }

    /** A node name built from a heading, with the spacing taken out of it. */
    static String compact(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char letter = value.charAt(index);
            if (Character.isLetterOrDigit(letter)) {
                out.append(letter);
            }
        }
        return out.toString();
    }

    // -- rules -------------------------------------------------------------

    /** The hairline that separates two blocks in a column. */
    static void divider(SectionBuilder column, String name, double top, double bottom) {
        column.addLine(line -> line
                .name("Divider_" + name)
                .fill()
                .thickness(RULE_THICKNESS)
                .color(RULE)
                .margin(new DocumentInsets(top, 0, bottom, 0)));
    }

    // -- marks -------------------------------------------------------------

    /** An inline mark, drawn at the size the preset gives its kind. */
    static void inlineIcon(ParagraphBuilder paragraph, String token, double size) {
        paragraph.inlineSvgIcon(TerracottaRailIcons.icon(token), size,
                InlineImageAlignment.CENTER);
    }

    // -- the rail ----------------------------------------------------------

    /** The marker an entry rides: a terracotta ring with the paper inside. */
    private static DocumentNode marker(String prefix, int index) {
        return new EllipseBuilder()
                .name(prefix + "Marker_" + index)
                .circle(MARKER_DIAMETER)
                .fillColor(PAPER)
                .stroke(DocumentStroke.of(ACCENT, 1.0))
                .build();
    }

    /**
     * An entry's first line, with its marker sitting ON the rail.
     *
     * <p>The marker is nudged left by the entry's own indent plus half the
     * ring, so the ring's centre lands on the rail rather than beside it.
     * Both terms are geometry the sheet already holds, not offsets tuned by
     * eye.</p>
     */
    static void railedLine(SectionBuilder body, String prefix, int index, DocumentNode line) {
        body.addLayerStack(stack -> stack
                .name(prefix + "Line_" + index)
                .layer(line, LayerAlign.TOP_LEFT, 0)
                .position(marker(prefix, index),
                        -(ENTRY_INDENT + MARKER_DIAMETER / 2.0), 0.0,
                        LayerAlign.TOP_LEFT, 1));
    }

    /**
     * A title with its date flush right on the same line.
     *
     * <p>A table, not a row: a row cannot nest in a row cell and a table can
     * — it spans the cell and its columns lay out. Both cells hold a
     * paragraph, because a table inside a row cell draws only leaf
     * content.</p>
     */
    static DocumentNode titleAndDate(String name, String title, String date,
                                     double width, double dateShare) {
        double dateColumn = width * dateShare;
        return new TableBuilder()
                .name(name)
                .width(width)
                .columns(DocumentTableColumn.fixed(width - dateColumn),
                        DocumentTableColumn.fixed(dateColumn))
                .defaultCellStyle(DocumentTableStyle.builder()
                        .padding(DocumentInsets.zero())
                        .textAnchor(DocumentTableTextAnchor.BOTTOM_LEFT)
                        .stroke(NO_BORDER)
                        .build())
                .rowCells(
                        DocumentTableCell.node(new ParagraphBuilder()
                                .name(name + "_Title")
                                .text(title)
                                .lineSpacing(0)
                                .textStyle(text(ITEM_TITLE_SIZE, INK, true))
                                .build()),
                        DocumentTableCell.node(new ParagraphBuilder()
                                .name(name + "_Period")
                                .text(date)
                                .align(TextAlign.RIGHT)
                                .lineSpacing(0)
                                .textStyle(text(ITEM_TITLE_SIZE, ACCENT, false))
                                .build()))
                .build();
    }

    // -- rows --------------------------------------------------------------

    /**
     * A row inside a column.
     *
     * <p>A row cannot nest in a row cell on this engine, and both of this
     * sheet's columns are cells of the body row — so every horizontal pair
     * inside one is wrapped in a layer stack, which can.</p>
     */
    static void layeredRow(SectionBuilder parent, String name, double marginTop,
                           double marginBottom, Consumer<RowBuilder> spec) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name + "Layer");
        layer.spacing(0);
        layer.addRow(name, spec);
        parent.addLayerStack(stack -> stack
                .name(name + "Stack")
                .margin(new DocumentInsets(marginTop, 0, marginBottom, 0))
                .layer(layer.build(), LayerAlign.TOP_LEFT, 0));
    }

    /** A bullet line: a mark, a gap, and the item beside it. */
    static void bulletLine(SectionBuilder block, String name, Consumer<ParagraphBuilder> mark,
                           String item, DocumentColor color, double size) {
        block.addParagraph(p -> {
            p.name(name);
            mark.accept(p);
            p.inlineText("  ");
            p.inlineText(item, text(size, color, false));
            p.margin(0f, 0f, (float) BULLET_ROW_GAP, 0f);
        });
    }
}
