package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceServiceLines;

import java.util.function.Consumer;

import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.DISC_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.SMALL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_ICON_GAP;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TABLE_ICON_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.capPitch;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.style;

/**
 * The shapes the Platform invoice repeats: a horizontal pair inside a column,
 * the filled disc a party heading opens with, and the mark-and-two-lines block
 * that fills the table's first column.
 */
final class PlatformWidgets {

    private PlatformWidgets() {
    }

    /**
     * A row wrapped in a layer, so it can sit inside a row cell.
     *
     * <p>A row nested in a row cell is refused by the layout compiler, and
     * through a section too — it is the ancestry that matters, not the immediate
     * parent. A row wrapped in a LayerStack layer lays out horizontally there.
     * Every horizontal pair below the top level goes through here.</p>
     *
     * @param parent the section the pair belongs to
     * @param name   the pair's name, which its holder and layer extend
     * @param spec   the row itself
     */
    static void layeredRow(SectionBuilder parent, String name, Consumer<RowBuilder> spec) {
        SectionBuilder holder = new SectionBuilder();
        holder.name(name + "Holder");
        holder.addRow(name, spec);
        DocumentNode node = holder.build();
        parent.addLayerStack(stack -> stack
                .name(name + "Layer")
                .layer(node, LayerAlign.TOP_LEFT, 0));
    }

    /** A mark knocked out of a filled accent disc. */
    static DocumentNode disc(String name, String token, double diameter, double markSize) {
        return new ShapeContainerBuilder()
                .name(name)
                .circle(diameter)
                .fillColor(ACCENT)
                .center(PlatformIcons.icon(token).node(markSize))
                .build();
    }

    /** The party disc, at the size the two headings use. */
    static DocumentNode partyDisc(String name, String token) {
        return disc(name, token, DISC_D, DISC_ICON);
    }

    /**
     * The service mark and the two-line description beside it, as one table cell's
     * content.
     *
     * <p>The mark hangs in a fixed gutter and the text takes the rest, so a longer
     * product name wraps under itself rather than under the mark.</p>
     *
     * @param line  the service line
     * @param index the line's index, which names the block
     * @return the block
     */
    static DocumentNode descriptionCell(InvoiceServiceLines.Line line, int index) {
        String token = line.icon();
        SectionBuilder holder = new SectionBuilder();
        holder.name("Description_" + index + "Holder");
        holder.addRow("Description_" + index, cell -> {
            cell.spacing(0)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(TABLE_ICON_GUTTER + TABLE_ICON_GAP),
                            DocumentRowColumn.weight(1));
            cell.addSection("DescriptionIcon_" + index, gutter -> {
                gutter.spacing(0);
                if (!token.isBlank()) {
                    gutter.addSvgIcon(PlatformIcons.icon(token), TABLE_ICON);
                }
            });
            cell.addSection("DescriptionText_" + index, text -> {
                text.spacing(capPitch(23, BODY_SIZE, SMALL_SIZE));
                text.addParagraph(p -> p
                        .name("LineTitle_" + index)
                        .text(line.title())
                        .textStyle(bold(BODY_SIZE, INK)));
                text.addParagraph(p -> p
                        .name("LineSubtitle_" + index)
                        .text(line.description())
                        .textStyle(style(SMALL_SIZE, MUTED)));
            });
        });
        return holder.build();
    }
}
