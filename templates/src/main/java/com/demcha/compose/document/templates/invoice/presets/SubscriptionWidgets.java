package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;

import java.util.function.Consumer;

import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HEADING_RULE_PAD;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HEADING_RULE_THICK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.px;

/**
 * The two shapes the Subscription invoice repeats: a horizontal pair inside a
 * column, and the heading plaque — a caption underlined in one of the cycle's
 * colours, sized to the caption rather than to the column.
 */
final class SubscriptionWidgets {

    private SubscriptionWidgets() {
    }

    /**
     * A row wrapped in a layer, so it can sit inside a row cell.
     *
     * <p>A row nested in a row cell is refused by the layout compiler, and
     * through a section too — it is the ancestry that matters, not the immediate
     * parent. A row wrapped in a LayerStack layer lays out horizontally there.</p>
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
     * A caption with a coloured rule under it, as wide as the caption.
     *
     * <p>The rule is an accent on a cell that hugs its text, not a line of a
     * stated width: the design's rules end where their headings do, and a stated
     * width would have to be re-measured for every caption a document might set.
     * The spacer beside it is what stops the cell from filling the row.</p>
     *
     * @param host    the section the plaque belongs to
     * @param name    the plaque's name
     * @param heading the caption
     * @param size    the caption's type size
     * @param accent  the rule's colour
     */
    static void headingPlaque(SectionBuilder host, String name, String heading,
                              double size, DocumentColor accent) {
        layeredRow(host, name, row -> {
            row.spacing(0);
            row.columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1));
            row.addSection(name + "Rule", cell -> {
                cell.spacing(0);
                cell.padding(new DocumentInsets(0, 0, HEADING_RULE_PAD, 0));
                cell.accentBottom(accent, HEADING_RULE_THICK);
                cell.addParagraph(p -> p
                        .name(name + "Text")
                        .text(heading)
                        .textStyle(bold(size, INK)));
            });
            row.addSpacer(px(1));
        });
    }
}
