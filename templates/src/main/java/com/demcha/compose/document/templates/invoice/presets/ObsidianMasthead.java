package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HALF;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.META_COL_W;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.META_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.META_NUMBER_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.META_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.RULE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.WORDMARK_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.topBearing;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.layeredRow;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.rule;

/**
 * The head of the sheet: the wordmark over the document's name on the left,
 * against a metadata stack on the right whose rows sit under their own rules.
 */
final class ObsidianMasthead {

    /**
     * The first row's number stands alone above the ruled rows, so the stack has
     * one more measured position than it has entries. Rule tops and row cap tops,
     * both measured: the pitch is even to within a pixel, but stating each keeps
     * them separable if one moves.
     */
    private static final double[] RULE_AT = {117.5, 160.5, 199.5, 239.5};
    private static final double[] ROW_AT = {135, 175, 215, 255};

    private ObsidianMasthead() {
    }

    /**
     * The masthead.
     *
     * <p>The brand's name is the wordmark — set, not drawn, because the design
     * sets it as type. A caller whose mark is a drawing puts it on the party disc
     * below instead, which is where this design carries one.</p>
     */
    static void render(PageFlowBuilder page, InvoiceBrand brand, InvoiceMasthead masthead) {
        page.addRow("Masthead", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.weight(1), DocumentRowColumn.fixed(META_COL_W));
            row.addSection("BrandColumn", cell -> renderBrand(cell, brand, masthead));
            row.addSection("InvoiceMeta", cell -> renderMeta(cell, masthead));
        });
    }

    private static void renderBrand(SectionBuilder cell, InvoiceBrand brand,
                                    InvoiceMasthead masthead) {
        cell.spacing(0);
        cell.addParagraph(p -> p
                .name("BrandWordmark")
                .text(brand.name())
                .textStyle(bold(WORDMARK_SIZE, INK))
                .margin(capTop(1, WORDMARK_SIZE, true)));
        cell.addParagraph(p -> p
                .name("InvoiceTitle")
                .text(masthead.title())
                .textStyle(bold(TITLE_SIZE, INK))
                // Measured from the content box's top, less the wordmark's own
                // line box, which is what the title is being placed under.
                .margin(capTop(195 - 46, TITLE_SIZE, true, LINE_BOX * WORDMARK_SIZE)));
    }

    /**
     * The metadata stack.
     *
     * <p>The first entry is the one the design sets apart: its label sits above
     * its value in the accent and neither carries a rule, so an invoice whose
     * first entry is its number reads as a heading for the stack rather than as
     * its first row. The rest are ruled label/value pairs.</p>
     */
    private static void renderMeta(SectionBuilder cell, InvoiceMasthead masthead) {
        List<InvoiceMasthead.Entry> entries = masthead.entries();
        cell.spacing(0);
        if (entries.isEmpty()) {
            return;
        }
        InvoiceMasthead.Entry lead = entries.get(0);
        cell.addParagraph(p -> p
                .name("MetaNumberLabel")
                .text(lead.label())
                .textStyle(plain(META_LABEL_SIZE, MUTED))
                .margin(capTop(52 - 46, META_LABEL_SIZE, false)));
        cell.addParagraph(p -> p
                .name("MetaNumber")
                .text(lead.value())
                .textStyle(bold(META_NUMBER_SIZE, ACCENT))
                .margin(capTop(82 - 52, META_NUMBER_SIZE, false, LINE_BOX * META_LABEL_SIZE)));

        for (int i = 1; i < entries.size() && i <= RULE_AT.length; i++) {
            InvoiceMasthead.Entry entry = entries.get(i);
            int at = i - 1;
            String name = "MetaRow_" + at;
            double gapAboveRule = at == 0
                    ? capGap(RULE_AT[0] - 82, META_NUMBER_SIZE, true, 0, false)
                    : capGap(RULE_AT[at] - ROW_AT[at - 1], META_VALUE_SIZE, false, 0, false);
            cell.addLine(line -> rule(line, name + "Rule", META_COL_W, HAIRLINE)
                    .margin(new DocumentInsets(Math.max(0, gapAboveRule), 0, 0, 0)));
            double gapBelowRule =
                    px(ROW_AT[at] - RULE_AT[at]) - RULE_BOX - topBearing(META_VALUE_SIZE, false);
            layeredRow(cell, name, row -> {
                row.spacing(0)
                        .margin(new DocumentInsets(Math.max(0, gapBelowRule), 0, 0, 0))
                        .weights(HALF, HALF);
                row.addParagraph(p -> p
                        .name(name + "Label")
                        .text(entry.label())
                        .textStyle(plain(META_VALUE_SIZE, MUTED)));
                row.addParagraph(p -> p
                        .name(name + "Value")
                        .text(entry.value())
                        .textStyle(plain(META_VALUE_SIZE, entry.emphasized() ? ACCENT : INK))
                        .align(TextAlign.RIGHT));
            });
        }
    }
}
