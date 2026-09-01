package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.svg.SvgIcon;

import java.util.Locale;
import java.util.function.Consumer;

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.DISC_GLYPH;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PARTY_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.bold;

/**
 * The shapes the Obsidian invoice repeats: a horizontal pair inside a column, a
 * filled disc, a hairline, and the initials a party's disc falls back to.
 */
final class ObsidianWidgets {

    private ObsidianWidgets() {
    }

    /**
     * A row wrapped in a layer, so it can sit inside a row cell.
     *
     * <p>A row nested in a row cell is refused by the layout compiler, and
     * through a section too. A row wrapped in a LayerStack layer lays out
     * horizontally there.</p>
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

    /** A hairline of the sheet's own weight. */
    static LineBuilder rule(LineBuilder line, String name, double width, DocumentColor color) {
        return line.name(name).horizontal(width).thickness(RULE_THIN).color(color);
    }

    /** A packaged mark, boxed so it can sit in a row cell or inside a disc. */
    static DocumentNode glyph(SvgIcon icon, String name, double size, HorizontalAlign align) {
        return new SectionBuilder()
                .name("Glyph_" + name)
                .spacing(0)
                .addSvgIcon(icon, size, align)
                .build();
    }

    /** A party's disc, carrying a mark. */
    static DocumentNode disc(String name, DocumentColor fill, DocumentNode content, double diameter) {
        return new ShapeContainerBuilder()
                .name(name)
                .circle(diameter)
                .fillColor(fill)
                .center(content)
                .build();
    }

    /** A party's disc, carrying initials. */
    static DocumentNode initialsDisc(String name, DocumentColor fill, String initials) {
        return disc(name, fill, new ParagraphBuilder()
                .name(name + "Initials")
                .text(initials)
                .textStyle(bold(PARTY_NAME_SIZE, INK))
                .align(TextAlign.CENTER)
                .build(), DISC_D);
    }

    /** A mark's disc at the party size. */
    static DocumentNode glyphDisc(String name, DocumentColor fill, SvgIcon icon) {
        return disc(name, fill, glyph(icon, name, DISC_GLYPH, HorizontalAlign.CENTER), DISC_D);
    }

    /**
     * The initials a disc shows when there is no mark to show instead.
     *
     * <p>The first letter of each of the first two words, which is what the
     * design's own client disc is. A name of one word gives one letter, and a
     * name of none gives nothing — the disc is still drawn, because it is the
     * design's shape and not a container for the letters.</p>
     *
     * @param name the party's name
     * @return up to two upper-case letters
     */
    static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        StringBuilder initials = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty() && Character.isLetter(word.charAt(0))) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.toString().toUpperCase(Locale.ENGLISH);
    }

    /** Gives a paragraph a target when there is one. */
    static ParagraphBuilder linked(ParagraphBuilder paragraph, DocumentLinkOptions link) {
        return link == null ? paragraph : paragraph.link(link);
    }
}
