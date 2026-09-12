package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;

import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DISC_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.style;

/**
 * The pieces more than one part of the Payments sheet draws: the wrapper every
 * horizontal pair inside a cell goes through, the disc that owns its glyph, and
 * the two text helpers.
 */
final class PaymentsWidgets {

    private PaymentsWidgets() {
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
     * A filled disc that owns its glyph: the glyph sits inside the fill, so it
     * is a child of the shape rather than a coloured circle with an icon
     * dropped near it.
     *
     * @param token the packaged mark
     * @param fill  the disc colour
     * @return the disc node
     */
    static DocumentNode disc(String token, DocumentColor fill) {
        return disc(token, fill, DISC_D, DISC_GLYPH);
    }

    /**
     * The same, at a stated diameter — the payment card's and the notes' discs
     * are each their own size, measured off the block they belong to.
     *
     * @param token    the packaged mark
     * @param fill     the disc colour
     * @param diameter the disc's diameter
     * @param glyph    the glyph's box inside it
     * @return the disc node
     */
    static DocumentNode disc(String token, DocumentColor fill, double diameter, double glyph) {
        return new ShapeContainerBuilder()
                .name("Disc" + token)
                .circle(diameter)
                .fillColor(fill)
                .center(glyphNode(token, glyph, HorizontalAlign.CENTER))
                .build();
    }

    /**
     * A glyph in a box of its own, so a shape can own it. A glyph that leads a
     * text row is aligned left: its column's width is the distance to the text,
     * not a box for the glyph to sit in the middle of.
     *
     * @param token the packaged mark
     * @param size  the glyph's box
     * @param align where the glyph sits in it
     * @return the glyph node
     */
    static DocumentNode glyphNode(String token, double size, HorizontalAlign align) {
        return new SectionBuilder()
                .name("Glyph" + token)
                .spacing(0)
                .addSvgIcon(PaymentsIcons.icon(token), size, align)
                .build();
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

    /** The paragraph, made reachable when there is somewhere for it to point. */
    static ParagraphBuilder linked(ParagraphBuilder paragraph, String href) {
        return href == null || href.isBlank()
                ? paragraph
                : paragraph.link(new DocumentLinkOptions(href));
    }
}
