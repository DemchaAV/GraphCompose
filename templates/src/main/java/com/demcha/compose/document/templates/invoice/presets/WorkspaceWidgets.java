package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;

import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DISC_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_TILE_D;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ITEM_TILE_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TILE_COLOR;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.style;

/**
 * The pieces more than one part of the Workspace sheet draws: the wrapper every
 * horizontal pair inside a cell goes through, the disc and the tile that own
 * their glyphs, and the full-width rules that separate the closing blocks.
 */
final class WorkspaceWidgets {

    private WorkspaceWidgets() {
    }

    /**
     * A row that survives inside a row cell.
     *
     * <p>A row nested directly in a row cell is refused by the layout compiler.
     * Every horizontal pair that lives inside a cell in this preset goes through
     * here, wrapped in a single layer of a stack.</p>
     *
     * @param parent the section to add the row to
     * @param name   the node name, for review and rollback
     * @param spec   describes the row
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
     * A filled disc that owns its glyph: the glyph is knocked out of the accent
     * fill, so it is a child of the shape rather than a coloured circle with an
     * icon dropped near it.
     *
     * @param token the packaged mark
     * @return the disc node
     */
    static DocumentNode disc(String token) {
        return new ShapeContainerBuilder()
                .name("Disc" + token)
                .circle(DISC_D)
                .fillColor(ACCENT)
                .center(glyph(token, DISC_GLYPH))
                .build();
    }

    /**
     * A line's mark on its coloured tile, or bare when the design gives that
     * token no tile.
     *
     * @param token the packaged mark
     * @return the tile node
     */
    static DocumentNode tile(String token) {
        DocumentColor fill = TILE_COLOR.get(token);
        if (fill == null) {
            return glyph(token, ITEM_TILE_D);
        }
        return new ShapeContainerBuilder()
                .name("ItemTile" + token)
                .roundedRect(ITEM_TILE_D, ITEM_TILE_D, ITEM_TILE_RADIUS)
                .fillColor(fill)
                .center(glyph(token, ITEM_GLYPH))
                .build();
    }

    /** A glyph centred in a box of its own, so a shape can own it. */
    static DocumentNode glyph(String token, double size) {
        return glyph(token, size, HorizontalAlign.CENTER);
    }

    /**
     * The same, aligned. A glyph that leads a text row is left-aligned: its
     * column's width is the distance to the text, not a box for the glyph to sit
     * in the middle of.
     *
     * @param token the packaged mark
     * @param size  the glyph's box
     * @param align where the glyph sits in it
     * @return the glyph node
     */
    static DocumentNode glyph(String token, double size, HorizontalAlign align) {
        return new SectionBuilder()
                .name("Glyph" + token)
                .spacing(0)
                .addSvgIcon(WorkspaceIcons.icon(token), size, align)
                .build();
    }

    /** One method for all three full-width hairlines; the page uses two weights. */
    static void fullWidthRule(PageFlowBuilder page, String name, DocumentColor color,
                              double thickness, double gapAbove, double gapBelow) {
        page.addLine(line -> line
                .name(name)
                .horizontal(CONTENT_W)
                .thickness(thickness)
                .color(color)
                .margin(new DocumentInsets(gapAbove, 0, gapBelow, 0)));
    }

    /**
     * A block of plain lines, one paragraph each.
     *
     * @param block  the section to fill
     * @param prefix the node-name prefix, numbered from one
     * @param lines  the lines
     * @param size   their type size
     * @param color  their colour
     */
    static void textLines(SectionBuilder block, String prefix, List<String> lines,
                          double size, DocumentColor color) {
        int index = 0;
        for (String line : lines) {
            String name = prefix + (++index);
            block.addParagraph(p -> p
                    .name(name)
                    .text(line)
                    .textStyle(style(size, color, DocumentTextDecoration.DEFAULT)));
        }
    }

    /** Even weights for a row of {@code count} cells. */
    static double[] evenWeights(int count) {
        double[] weights = new double[count];
        for (int i = 0; i < count; i++) {
            weights[i] = 1.0 / count;
        }
        return weights;
    }
}
