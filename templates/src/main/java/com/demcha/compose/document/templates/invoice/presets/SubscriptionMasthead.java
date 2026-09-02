package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTACT_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.CONTENT_PAD;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.GAP_MASTHEAD_TO_IDENTITY;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.IDENTITY_LEFT_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.MARK_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.MARK_TOP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.MARK_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_BAR_H;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_BAR_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_LABEL_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_RIGHT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_ROW_GAP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_RULE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_RULE_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_RULE_THICK;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.META_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.PARTY_LINE_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.SUPPLIER_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.SUPPLIER_TOP_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TITLE_GREY;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.WORDMARK_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.WORDMARK_TOP;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.baselineGap;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.cycle;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.SubscriptionWidgets.layeredRow;

/**
 * The head of the sheet: the lockup against the title, then the supplier's own
 * details against a metadata panel whose rows each open with a coloured bar.
 */
final class SubscriptionMasthead {

    private SubscriptionMasthead() {
    }

    /**
     * Mark and wordmark on the left, the document's name pushed to the right
     * edge.
     *
     * <p>Three columns rather than a brand block beside a title block: a row
     * cannot nest in a row cell, and the lockup needs no grouping node of its own
     * to sit on the same line as the title. The mark is the caller's — the
     * templates artifact carries none — and the wordmark is the brand's name.</p>
     */
    static void renderBrandHeader(PageFlowBuilder page, InvoiceBrand brand,
                                  InvoiceMasthead masthead) {
        page.addRow("Masthead", row -> {
            row.padding(CONTENT_PAD);
            row.spacing(0);
            row.verticalAlign(RowVerticalAlign.TOP);
            row.columns(DocumentRowColumn.fixed(MARK_W + MARK_GUTTER),
                    DocumentRowColumn.auto(),
                    DocumentRowColumn.weight(1));
            row.addSection("BrandMark", cell -> {
                cell.spacing(0);
                if (brand.logo() != null) {
                    cell.add(new ImageBuilder()
                            .name("BrandLogo")
                            .source(brand.logo())
                            .width(MARK_W)
                            .margin(new DocumentInsets(MARK_TOP, 0, 0, 0))
                            .build());
                }
            });
            row.addParagraph(p -> p
                    .name("BrandWordmark")
                    .text(brand.name())
                    .textStyle(plain(WORDMARK_SIZE, TITLE_GREY))
                    .margin(new DocumentInsets(WORDMARK_TOP, 0, 0, 0)));
            row.addParagraph(p -> p
                    .name("InvoiceTitle")
                    .text(masthead.title())
                    .textStyle(bold(TITLE_SIZE, TITLE_GREY))
                    .align(TextAlign.RIGHT));
        });
    }

    /** The supplier's details against the metadata panel. */
    static void renderIdentityBand(PageFlowBuilder page, InvoiceContactBlock supplier,
                                   InvoiceMasthead masthead) {
        page.addRow("IdentityBand", row -> {
            row.padding(CONTENT_PAD);
            row.margin(new DocumentInsets(GAP_MASTHEAD_TO_IDENTITY, 0, 0, 0));
            row.spacing(0);
            row.weights(IDENTITY_LEFT_RATIO, 1 - IDENTITY_LEFT_RATIO);
            row.addSection("SupplierIdentity", cell -> renderSupplier(cell, supplier));
            row.addSection("MetaPanel", cell -> renderMetaPanel(cell, masthead));
        });
    }

    private static void renderSupplier(SectionBuilder cell, InvoiceContactBlock supplier) {
        cell.spacing(0);
        // The panel's first accent bar opens the band; the name's cap sits a
        // little lower than the bar's top, and that offset is the supplier's.
        cell.margin(new DocumentInsets(SUPPLIER_TOP_OFFSET, 0, 0, 0));
        cell.addParagraph(p -> p
                .name("SupplierName")
                .text(supplier.legalName())
                .textStyle(bold(SUPPLIER_NAME_SIZE, INK)));

        cell.addSection("SupplierAddress", block -> {
            block.margin(new DocumentInsets(
                    baselineGap(30, SUPPLIER_NAME_SIZE, BODY_SIZE), 0, 0, 0));
            block.spacing(PARTY_LINE_PITCH - 1.2 * BODY_SIZE);
            List<String> lines = supplier.addressLines();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int index = i;
                block.addParagraph(p -> p
                        .name("SupplierAddress_" + index)
                        .text(line)
                        .textStyle(plain(BODY_SIZE, INK)));
            }
        });

        cell.addSection("SupplierContacts", block -> {
            block.margin(new DocumentInsets(baselineGap(48.9, BODY_SIZE, BODY_SIZE), 0, 0, 0));
            block.spacing(px(30) - 1.2 * BODY_SIZE);
            contact(block, "Email", "E", supplier.email(),
                    InvoiceUri.mailLink(supplier.email()));
            contact(block, "Website", "W", supplier.website(),
                    InvoiceUri.webLink(supplier.website()));
            contact(block, "Registration", supplier.taxRegistrationLabel(),
                    supplier.taxRegistrationNumber(), null);
        });
    }

    /**
     * One contact channel, over a fixed label column so every value starts on
     * the same axis.
     *
     * <p>The design abbreviates its channel labels to single letters; those are
     * the preset's, because which letter opens a channel is a property of the
     * design, while the registration's label is the document's — it differs by
     * jurisdiction.</p>
     */
    private static void contact(SectionBuilder block, String name, String label,
                                String value, DocumentLinkOptions link) {
        if (value.isBlank() || label.isBlank()) {
            return;
        }
        layeredRow(block, "Contact" + name, line -> {
            line.spacing(0);
            line.columns(DocumentRowColumn.fixed(CONTACT_LABEL_W), DocumentRowColumn.weight(1));
            line.addParagraph(p -> p
                    .name("Contact" + name + "Label")
                    .text(label)
                    .textStyle(bold(BODY_SIZE, INK)));
            line.addParagraph(p -> {
                p.name("Contact" + name + "Value");
                if (link == null) {
                    p.inlineText(value, plain(BODY_SIZE, INK));
                } else {
                    p.inlineText(value, plain(BODY_SIZE, INK), link);
                }
            });
        });
    }

    /**
     * The metadata panel: each row opens with a bar from the cycle and closes
     * with a hairline, except the last, which the design leaves open.
     */
    private static void renderMetaPanel(SectionBuilder cell, InvoiceMasthead masthead) {
        cell.spacing(META_ROW_GAP);
        List<InvoiceMasthead.Entry> entries = masthead.entries();
        for (int i = 0; i < entries.size(); i++) {
            InvoiceMasthead.Entry entry = entries.get(i);
            DocumentColor accent = cycle(i);
            boolean last = i == entries.size() - 1;
            int index = i;
            cell.addSection("MetaRow_" + index, block -> {
                block.spacing(0);
                layeredRow(block, "MetaLine_" + index, line -> {
                    line.spacing(0);
                    line.padding(new DocumentInsets(0, META_RIGHT_INSET, 0, 0));
                    line.verticalAlign(RowVerticalAlign.CENTER);
                    line.columns(DocumentRowColumn.fixed(META_BAR_W),
                            DocumentRowColumn.weight(1),
                            DocumentRowColumn.weight(1));
                    line.addShape(bar -> bar
                            .name("MetaAccent_" + index)
                            .size(META_BAR_W, META_BAR_H)
                            .fillColor(accent));
                    line.addParagraph(p -> p
                            .name("MetaLabel_" + index)
                            .text(entry.label())
                            .textStyle(bold(META_LABEL_SIZE, INK))
                            .padding(new DocumentInsets(0, 0, 0, META_LABEL_INDENT)));
                    line.addParagraph(p -> p
                            .name("MetaValue_" + index)
                            .text(entry.value())
                            .textStyle(plain(META_VALUE_SIZE, INK))
                            .align(TextAlign.RIGHT));
                });
                if (!last) {
                    block.addLine(rule -> rule
                            .name("MetaSeparator_" + index)
                            .horizontal(META_RULE_W)
                            .thickness(META_RULE_THICK)
                            .color(HAIRLINE)
                            .margin(new DocumentInsets(META_RULE_GAP, 0, 0, META_RULE_INDENT)));
                }
            });
        }
    }
}
