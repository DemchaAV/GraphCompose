package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;

import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CONTACT_GAP_PX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CONTACT_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CONTACT_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.DIVIDER_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.LEFT_COLUMN_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.LOGO_MARGIN_T;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.LOGO_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.META_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.META_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_STRONG;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.SUPPLIER_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.SUPPLIER_TAX_GAP_PX;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TITLE_MARGIN_T;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TITLE_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.TOP_BEARING_REGULAR;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.inkGapToImage;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.spaces;
import static com.demcha.compose.document.templates.invoice.presets.MerchantStyles.style;

/**
 * The head of the sheet: the lockup against the title over its accent rule, then
 * the supplier's details beside the invoice's metadata, split by a hairline.
 */
final class MerchantMasthead {

    private MerchantMasthead() {
    }

    /**
     * The lockup and the title.
     *
     * <p>The two disagree about where the page starts — the lockup's ink and the
     * title's box sit at different heights — so the flow opens at the earlier of
     * them and each carries the difference as its own margin. The mark is the
     * caller's: the lockup draws the brand's logo at the design's measured width,
     * or the brand's name when there is none.</p>
     */
    static void renderBrandHeader(PageFlowBuilder page, InvoiceBrand brand,
                                  InvoiceMasthead masthead) {
        page.addRow("Masthead", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(LOGO_W), DocumentRowColumn.weight(1));
            row.addSection("BrandLockup", cell -> {
                cell.spacing(0);
                if (brand.logo() != null) {
                    // The lockup's ink starts two pixels inside the content
                    // column's own left edge.
                    cell.add(new ImageBuilder()
                            .name("BrandLogo")
                            .source(brand.logo())
                            .width(LOGO_W)
                            .margin(new DocumentInsets(LOGO_MARGIN_T, 0, 0, px(2)))
                            .build());
                } else if (!brand.name().isBlank()) {
                    cell.addParagraph(p -> p
                            .name("BrandWordmark")
                            .text(brand.name())
                            .textStyle(bold(SUPPLIER_NAME_SIZE, INK))
                            .margin(new DocumentInsets(LOGO_MARGIN_T, 0, 0, px(2))));
                }
            });
            row.addParagraph(p -> p
                    .name("InvoiceTitle")
                    .text(masthead.title())
                    .textStyle(bold(TITLE_SIZE, INK))
                    .align(TextAlign.RIGHT)
                    .margin(new DocumentInsets(TITLE_MARGIN_T, 0, 0, 0)));
        });
    }

    /** The short accent rule under the title, at the design's own inset from the right. */
    static void renderTitleRule(PageFlowBuilder page, double mastheadHeight) {
        page.addLine(line -> line
                .name("TitleRule")
                .horizontal(TITLE_RULE_W)
                .thickness(py(2.6))
                .color(ACCENT)
                .margin(new DocumentInsets(
                        Math.max(0, py(122) - MerchantStyles.MARGIN_TOP - mastheadHeight),
                        0, 0, CONTENT_W - TITLE_RULE_W)));
    }

    /** The supplier's details against the invoice's metadata. */
    static void renderIdentityRow(PageFlowBuilder page, InvoiceContactBlock supplier,
                                  InvoiceMasthead masthead) {
        page.addRow("IdentityRow", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(LEFT_COLUMN_W),
                            DocumentRowColumn.fixed(DIVIDER_W),
                            DocumentRowColumn.weight(1))
                    // The row's top is the divider's; the rule above it occupies a
                    // whole point whatever it paints.
                    .margin(new DocumentInsets(
                            py(144 - 122) - MerchantStyles.RULE_BOX, 0, 0, 0));
            row.addSection("SupplierColumn", column -> renderSupplier(column, supplier));
            row.addSection("IdentityColumnDivider", column -> {
                column.spacing(0);
                column.addLine(line -> line
                        .name("IdentityColumnDivider")
                        .vertical(py(391 - 144))
                        .thickness(DIVIDER_W)
                        .color(RULE_SOFT));
            });
            row.addSection("MetaColumn", column -> renderMeta(column, masthead));
        });
    }

    /** The rule under both columns. */
    static void renderIdentityRule(PageFlowBuilder page, double identityHeight) {
        page.addLine(line -> line
                .name("IdentityRule")
                .horizontal(CONTENT_W)
                .thickness(RULE_THIN)
                .color(RULE_STRONG)
                .margin(new DocumentInsets(
                        Math.max(0, py(423 - 144) - identityHeight), 0, 0, 0)));
    }

    private static void renderSupplier(SectionBuilder column, InvoiceContactBlock supplier) {
        column.spacing(0);
        column.addParagraph(p -> p
                .name("SupplierName")
                .text(supplier.legalName())
                .textStyle(bold(SUPPLIER_NAME_SIZE, INK))
                .margin(capTop(154 - 144, SUPPLIER_NAME_SIZE, true)));

        column.addSection("SupplierAddress", block -> {
            block.spacing(capGap(25, BODY_SIZE, false, BODY_SIZE, false))
                    .margin(new DocumentInsets(
                            capGap(186 - 154, SUPPLIER_NAME_SIZE, true, BODY_SIZE, false),
                            0, 0, 0));
            List<String> lines = supplier.addressLines();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int index = i;
                block.addParagraph(p -> p
                        .name("SupplierAddress_" + index)
                        .text(line)
                        .textStyle(style(BODY_SIZE, INK)));
            }
        });

        // The three contact rows are the one block on this sheet whose line box
        // is not the type: an inline mark taller than the text sets the box, so
        // both the pitch and the block's own start are measured against the mark.
        column.addSection("SupplierContact", block -> {
            block.spacing(py(28.5) - CONTACT_ICON)
                    .margin(new DocumentInsets(
                            inkGapToImage(280 - 236, BODY_SIZE, false), 0, 0, 0));
            channel(block, "Website", MerchantIcons.WEBSITE, supplier.website(),
                    ContactUri.webLink(supplier.website()));
            channel(block, "Email", MerchantIcons.EMAIL, supplier.email(),
                    ContactUri.mailLink(supplier.email()));
            channel(block, "Phone", MerchantIcons.PHONE, supplier.phone(),
                    ContactUri.telLink(supplier.phone()));
        });

        if (!supplier.taxRegistrationNumber().isBlank()) {
            column.addParagraph(p -> {
                p.name("SupplierTaxId");
                p.inlineText(supplier.taxRegistrationLabel(), style(BODY_SIZE, INK));
                p.inlineText(spaces(SUPPLIER_TAX_GAP_PX, BODY_SIZE), style(BODY_SIZE, INK));
                p.inlineText(supplier.taxRegistrationNumber(), style(BODY_SIZE, INK));
                // Measured from the last contact mark, for the same reason: the
                // block above ends at its mark's foot, not at a text box's.
                p.margin(new DocumentInsets(
                        py(377 - 337) - CONTACT_ICON - TOP_BEARING_REGULAR * BODY_SIZE,
                        0, 0, 0));
            });
        }
    }

    private static void channel(SectionBuilder block, String name, String token,
                                String value, DocumentLinkOptions link) {
        if (value.isBlank()) {
            return;
        }
        block.addParagraph(p -> {
            p.name("SupplierContact" + name);
            p.inlineSvgIcon(MerchantIcons.icon(token), CONTACT_ICON, InlineImageAlignment.CENTER);
            p.inlineText(spaces(CONTACT_GAP_PX, CONTACT_SIZE), style(CONTACT_SIZE, INK));
            if (link == null) {
                p.inlineText(value, style(CONTACT_SIZE, INK));
            } else {
                p.inlineText(value, style(CONTACT_SIZE, INK), link);
            }
        });
    }

    private static void renderMeta(SectionBuilder column, InvoiceMasthead masthead) {
        column.spacing(capGap(40.8, BODY_SIZE, false, BODY_SIZE, false))
                .padding(new DocumentInsets(0, 0, 0, META_PAD_L))
                .margin(capTop(156 - 144, BODY_SIZE, false));
        List<InvoiceMasthead.Entry> entries = masthead.entries();
        for (int i = 0; i < entries.size(); i++) {
            InvoiceMasthead.Entry entry = entries.get(i);
            int index = i;
            layeredRow(column, "MetaRow_" + index, row -> {
                row.spacing(0)
                        .columns(DocumentRowColumn.fixed(META_LABEL_W),
                                DocumentRowColumn.weight(1));
                row.addParagraph(p -> p.text(entry.label()).textStyle(style(BODY_SIZE, INK)));
                row.addParagraph(p -> p
                        .text(entry.value())
                        .textStyle(style(BODY_SIZE, entry.emphasized() ? ACCENT : INK)));
            });
        }
    }

    /** A row wrapped in a layer, so it can sit inside a row cell. */
    static void layeredRow(SectionBuilder parent, String name, Consumer<RowBuilder> spec) {
        SectionBuilder holder = new SectionBuilder();
        holder.name(name + "Holder");
        holder.addRow(name, spec);
        DocumentNode node = holder.build();
        parent.addLayerStack(stack -> stack
                .name(name + "Layer")
                .layer(node, LayerAlign.TOP_LEFT, 0));
    }
}
