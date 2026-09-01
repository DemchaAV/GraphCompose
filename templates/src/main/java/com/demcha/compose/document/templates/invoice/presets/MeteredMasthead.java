package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.ACCENT_BRIGHT;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.CONTENT_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.IDENTITY_SPLIT;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.LOGO_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.META_ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.META_BLOCK_INSET;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.META_COLUMN_INSET;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.META_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.META_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.META_PAIR_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.META_VALUE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PARTIES_SPLIT;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PARTY_EXTRA_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PARTY_HEAD_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PARTY_INSET;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PARTY_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PARTY_LINE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.PARTY_NAME;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.RULE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SEAM_CLEARANCE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SEAM_IDENTITY_TO_RULE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SEAM_RULE_TO_PARTIES;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SECTION_ICON;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SECTION_ICON_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SECTION_LABEL;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SHIP_COLUMN_INSET;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUPPLIER_BLOCK_INSET;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUPPLIER_GROUP_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUPPLIER_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUPPLIER_LINE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUPPLIER_NAME;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUPPLIER_NAME_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.SUPPLIER_PAIR_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TITLE;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TITLE_RULE_GAP;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TITLE_RULE_T;
import static com.demcha.compose.document.templates.invoice.presets.MeteredStyles.TITLE_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.MeteredWidgets.hangingBody;
import static com.demcha.compose.document.templates.invoice.presets.MeteredWidgets.iconHeading;
import static com.demcha.compose.document.templates.invoice.presets.MeteredWidgets.labelledRow;
import static com.demcha.compose.document.templates.invoice.presets.MeteredWidgets.layeredRow;

/**
 * The three bands above the line items: the lockup and the title, the supplier
 * against the invoice's own metadata, and the two addressed parties.
 */
final class MeteredMasthead {

    private MeteredMasthead() {
    }

    /**
     * The lockup on the left and the document's title on the right, over the
     * accent rule.
     *
     * <p>The mark is the caller's: the lockup draws the brand's logo at the
     * design's measured width, or the brand's name when there is none. The
     * templates artifact carries no mark of its own.</p>
     */
    static void renderBrandHeader(SectionBuilder body, InvoiceBrand brand, InvoiceMasthead masthead) {
        body.addRow("BrandHeader", row -> row
                .verticalAlign(RowVerticalAlign.CENTER)
                .weights(1, 1)
                .addSection("BrandMark", mark -> {
                    if (brand.logo() != null) {
                        mark.add(new ImageBuilder()
                                .name("BrandLogo")
                                .source(brand.logo())
                                .width(LOGO_W)
                                .build());
                    } else {
                        mark.addParagraph(p -> p
                                .name("BrandName")
                                .text(brand.name())
                                .textStyle(TITLE));
                    }
                })
                .addSection("InvoiceTitle", title -> title
                        .spacing(TITLE_RULE_GAP)
                        .addParagraph(p -> p
                                .name("InvoiceTitleText")
                                .text(masthead.title())
                                .textStyle(TITLE)
                                .align(TextAlign.RIGHT))
                        .addAligned(HorizontalAlign.RIGHT, new LineBuilder()
                                .name("InvoiceTitleRule")
                                .horizontal(TITLE_RULE_W)
                                .thickness(TITLE_RULE_T)
                                .color(ACCENT_BRIGHT)
                                .build())));
    }

    /** The supplier's own details against the invoice's metadata, split by a hairline. */
    static void renderIdentityRow(SectionBuilder body, InvoiceContactBlock supplier,
                                  InvoiceMasthead masthead) {
        body.addRow("IdentityRow", row -> row
                .weights(IDENTITY_SPLIT, 1 - IDENTITY_SPLIT)
                .addSection("SupplierColumn", left -> {
                    left.borders(DocumentBorders.right(DocumentStroke.of(RULE, HAIRLINE)));
                    left.padding(new DocumentInsets(0, SEAM_CLEARANCE, 0, 0));
                    renderSupplierBlock(left, supplier);
                })
                .addSection("MetaColumn", right -> {
                    right.padding(new DocumentInsets(0, 0, 0, META_COLUMN_INSET));
                    renderInvoiceMeta(right, masthead);
                }));
        body.addDivider(d -> d
                .name("IdentityRule")
                .width(CONTENT_W)
                .thickness(HAIRLINE)
                .color(RULE)
                .margin(SEAM_IDENTITY_TO_RULE));
    }

    private static void renderSupplierBlock(SectionBuilder column, InvoiceContactBlock supplier) {
        column.addSection("SupplierBlock", block -> {
            block.margin(SUPPLIER_BLOCK_INSET);
            block.spacing(SUPPLIER_GROUP_GAP);
            block.addSection("SupplierIdentity", identity -> {
                identity.spacing(SUPPLIER_LINE_GAP);
                identity.addParagraph(p -> p
                        .name("SupplierName")
                        .text(supplier.legalName())
                        .textStyle(SUPPLIER_NAME)
                        .margin(new DocumentInsets(0, 0, SUPPLIER_NAME_GAP, 0)));
                List<String> lines = supplier.addressLines();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    int index = i;
                    identity.addParagraph(p -> p
                            .name("SupplierAddress_" + index)
                            .text(line)
                            .textStyle(BODY));
                }
            });
            block.addSection("SupplierContacts", contacts -> {
                contacts.spacing(SUPPLIER_PAIR_GAP);
                int index = 0;
                if (!supplier.taxRegistrationNumber().isBlank()) {
                    labelledRow(contacts, "SupplierContact_" + index++,
                            supplier.taxRegistrationLabel(), supplier.taxRegistrationNumber(),
                            SUPPLIER_LABEL_W, BODY, BODY, null);
                }
                if (!supplier.phone().isBlank()) {
                    labelledRow(contacts, "SupplierContact_" + index++,
                            "Phone:", supplier.phone(),
                            SUPPLIER_LABEL_W, BODY, BODY, InvoiceUri.telLink(supplier.phone()));
                }
                if (!supplier.email().isBlank()) {
                    labelledRow(contacts, "SupplierContact_" + index++,
                            "Email:", supplier.email(), SUPPLIER_LABEL_W, BODY, BODY,
                            InvoiceUri.mailLink(supplier.email()));
                }
                if (!supplier.website().isBlank()) {
                    labelledRow(contacts, "SupplierContact_" + index,
                            "Website:", supplier.website(), SUPPLIER_LABEL_W, BODY, BODY,
                            InvoiceUri.webLink(supplier.website()));
                }
            });
        });
    }

    private static void renderInvoiceMeta(SectionBuilder column, InvoiceMasthead masthead) {
        column.addSection("InvoiceMeta", meta -> {
            meta.margin(META_BLOCK_INSET);
            meta.spacing(META_PAIR_GAP);
            List<InvoiceMasthead.Entry> entries = masthead.entries();
            for (int i = 0; i < entries.size(); i++) {
                InvoiceMasthead.Entry entry = entries.get(i);
                labelledRow(meta, "InvoiceMeta_" + i, entry.label(), entry.value(),
                        META_LABEL_W, META_LABEL,
                        entry.emphasized() ? META_ACCENT : META_VALUE, null);
            }
        });
    }

    /** The billed party against the shipped-to party, split by the same hairline. */
    static void renderParties(SectionBuilder body, InvoiceRecipient billTo, InvoiceRecipient shipTo) {
        body.addRow("PartiesRow", row -> row
                .margin(SEAM_RULE_TO_PARTIES)
                .weights(PARTIES_SPLIT, 1 - PARTIES_SPLIT)
                .addSection("BillToColumn", left -> {
                    left.borders(DocumentBorders.right(DocumentStroke.of(RULE, HAIRLINE)));
                    left.padding(new DocumentInsets(0, SEAM_CLEARANCE, 0, PARTY_INSET));
                    renderParty(left, "BillTo", billTo, MeteredIcons.BILL_TO);
                })
                .addSection("ShipToColumn", right -> {
                    right.padding(new DocumentInsets(0, 0, 0, SHIP_COLUMN_INSET));
                    if (shipTo != null) {
                        renderParty(right, "ShipTo", shipTo, MeteredIcons.SHIP_TO);
                    }
                }));
    }

    /**
     * The shared body of the two party blocks. The heading's mark hangs in the
     * block's own margin: everything under it is left-aligned with the label,
     * which is one padding on the body group rather than a margin repeated on
     * every line.
     */
    private static void renderParty(SectionBuilder column, String id, InvoiceRecipient party,
                                    String iconToken) {
        column.addSection(id, block -> {
            block.keepTogether();
            block.spacing(PARTY_HEAD_GAP);
            iconHeading(block, id + "Heading", iconToken, SECTION_ICON, SECTION_ICON_GAP,
                    party.heading(), SECTION_LABEL);
            block.addSection(id + "Body", lines -> {
                lines.padding(hangingBody(SECTION_ICON, SECTION_ICON_GAP));
                lines.spacing(PARTY_LINE_GAP);
                lines.addParagraph(p -> p
                        .name(id + "Name")
                        .text(party.name())
                        .textStyle(PARTY_NAME));
                List<String> address = party.addressLines();
                for (int i = 0; i < address.size(); i++) {
                    String line = address.get(i);
                    int index = i;
                    lines.addParagraph(p -> p
                            .name(id + "Address_" + index)
                            .text(line)
                            .textStyle(BODY));
                }
                if (!party.registrationNumber().isBlank()) {
                    layeredRow(lines, id + "Registration", row -> row
                            .margin(new DocumentInsets(PARTY_EXTRA_GAP, 0, 0, 0))
                            .columns(DocumentRowColumn.fixed(PARTY_LABEL_W), DocumentRowColumn.auto())
                            .addParagraph(p -> p.text(party.registrationLabel()).textStyle(BODY))
                            .addParagraph(p -> p.text(party.registrationNumber()).textStyle(BODY)));
                }
            });
        });
    }
}
