package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.function.Consumer;

import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.ICON_TILE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.ICON_TILE_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.LINE_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PANEL_BORDER;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.WHITE;

/**
 * The shapes the Metered invoice repeats: a horizontal pair inside a column, a
 * label/value pair over a fixed label lane, a mark beside a caption, and the
 * bordered tile a service mark sits in.
 */
final class MeteredWidgets {

    private MeteredWidgets() {
    }

    /**
     * A row wrapped in a layer, so it can sit inside a row cell.
     *
     * <p>A row nested in a row cell is refused by the layout compiler, and
     * through a section too — it is the ancestry that matters, not the immediate
     * parent. A row wrapped in a LayerStack layer lays out horizontally there.
     * Every horizontal pair inside one of this page's three two-column rows goes
     * through here; the ones at the top of the flow do not need it.</p>
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

    /**
     * One label/value pair over a fixed label column, so every value in a list
     * starts on the same axis. Four lists on this page use it at four widths.
     *
     * @param parent     the section the pair belongs to
     * @param name       the pair's name
     * @param label      the label text
     * @param value      the value text
     * @param labelWidth the label lane's width
     * @param labelStyle the label's type
     * @param valueStyle the value's type
     * @param link       a target for the value, or {@code null} when it is not
     *                   something a reader can act on
     */
    static void labelledRow(SectionBuilder parent, String name, String label, String value,
                            double labelWidth,
                            DocumentTextStyle labelStyle, DocumentTextStyle valueStyle,
                            DocumentLinkOptions link) {
        layeredRow(parent, name, row -> row
                .columns(DocumentRowColumn.fixed(labelWidth), DocumentRowColumn.auto())
                .addParagraph(p -> p.text(label).textStyle(labelStyle))
                .addParagraph(p -> {
                    p.text(value).textStyle(valueStyle);
                    if (link != null) {
                        p.link(link);
                    }
                }));
    }

    /** A mark and a caption on one centre line, with the gap owned by the column. */
    static void iconHeading(SectionBuilder parent, String name, String iconToken,
                            double iconSize, double gap, String text,
                            DocumentTextStyle textStyle) {
        layeredRow(parent, name, row -> row
                .verticalAlign(RowVerticalAlign.CENTER)
                .columns(DocumentRowColumn.fixed(iconSize + gap), DocumentRowColumn.auto())
                .addSection(name + "Icon", cell -> cell.addSvgIcon(MeteredIcons.icon(iconToken), iconSize))
                .addParagraph(p -> p.name(name + "Label").text(text).textStyle(textStyle)));
    }

    /**
     * The bordered square a service mark lives in — the tile is laid out, the
     * mark is its content.
     *
     * @param token the mark's token
     * @param index the line's index, which names the tile
     * @return the tile
     */
    static DocumentNode tile(String token, int index) {
        ShapeContainerBuilder tile = new ShapeContainerBuilder();
        tile.name("LineIconTile_" + index)
                .roundedRect(ICON_TILE, ICON_TILE, ICON_TILE_RADIUS)
                .fillColor(WHITE)
                .stroke(DocumentStroke.of(PANEL_BORDER, HAIRLINE))
                .center(MeteredIcons.icon(token).node(LINE_ICON));
        return tile.build();
    }

    /**
     * The tile's space without the tile, for a line that names no mark.
     *
     * <p>Drawing an empty bordered box would be a mark of its own — the design
     * has none — so the lane keeps its width and its height and stays blank,
     * which leaves every text lane down the column on the same axis.</p>
     *
     * @param index the line's index, which names the space
     * @return a node the size of a tile
     */
    static DocumentNode tileSpace(int index) {
        return new SpacerNode("LineIconSpace_" + index, ICON_TILE, ICON_TILE,
                DocumentInsets.zero(), DocumentInsets.zero());
    }

    /** A section padded to hold a block's body under a hanging mark. */
    static DocumentInsets hangingBody(double iconSize, double gap) {
        return new DocumentInsets(0, 0, 0, iconSize + gap);
    }
}
