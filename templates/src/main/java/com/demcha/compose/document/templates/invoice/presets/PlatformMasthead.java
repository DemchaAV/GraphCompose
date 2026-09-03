package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CAP_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CONTACT_GAP_PX;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.CONTACT_ICON;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.DIVIDER_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LEFT_COLUMN_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.LOCKUP_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.MARGIN_TOP;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.META_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.META_PAD_L;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.RULE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.RULE_THICK;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.SUPPLIER_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.capPitch;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.flowY;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.py;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.spaces;
import static com.demcha.compose.document.templates.invoice.presets.PlatformStyles.style;
import static com.demcha.compose.document.templates.invoice.presets.PlatformWidgets.layeredRow;

/**
 * The head of the sheet: the lockup against the title, then the supplier's own
 * details against the invoice's metadata, split by a full-height hairline.
 */
final class PlatformMasthead {

    private PlatformMasthead() {
    }

    /**
     * The lockup on the left and the document's title on the right.
     *
     * <p>The mark is the caller's: the lockup draws the brand's logo at the
     * design's measured width, or the brand's name when there is none. The
     * templates artifact carries no mark of its own.</p>
     */
    static void renderBrandHeader(PageFlowBuilder page, InvoiceBrand brand,
                                  InvoiceMasthead masthead) {
        page.addRow("Masthead", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(LOCKUP_W), DocumentRowColumn.weight(1));
            row.addSection("BrandLockup", cell -> {
                cell.spacing(0);
                if (brand.logo() != null) {
                    cell.add(new ImageBuilder()
                            .name("BrandLogo")
                            .source(brand.logo())
                            .width(LOCKUP_W)
                            .margin(new DocumentInsets(flowY(42), 0, 0, 0))
                            .build());
                } else if (!brand.name().isBlank()) {
                    cell.addParagraph(p -> p
                            .name("BrandWordmark")
                            .text(brand.name())
                            .textStyle(bold(SUPPLIER_NAME_SIZE, INK))
                            .margin(new DocumentInsets(flowY(42), 0, 0, 0)));
                }
            });
            row.addParagraph(p -> p
                    .name("InvoiceTitle")
                    .text(masthead.title())
                    .textStyle(bold(TITLE_SIZE, INK))
                    .align(TextAlign.RIGHT));
        });
    }

    /**
     * The supplier's details against the invoice's metadata.
     *
     * <p>The gap above is measured from the title's line-box foot to the
     * supplier name's cap, because the masthead row's box ends at the former and
     * the design measures the latter.</p>
     */
    static void renderIdentityRow(PageFlowBuilder page, InvoiceContactBlock supplier,
                                  InvoiceMasthead masthead) {
        page.addRow("IdentityRow", row -> {
            row.spacing(0)
                    .columns(DocumentRowColumn.fixed(LEFT_COLUMN_W),
                            DocumentRowColumn.fixed(DIVIDER_W),
                            DocumentRowColumn.weight(1))
                    .margin(new DocumentInsets(
                            py(135) - CAP_INSET * SUPPLIER_NAME_SIZE
                                    - (flowY(45) - CAP_INSET * TITLE_SIZE + LINE_BOX * TITLE_SIZE)
                                    - MARGIN_TOP,
                            0, 0, 0));
            row.addSection("SupplierColumn", column -> renderSupplier(column, supplier));
            row.addSection("IdentityColumnDivider", column -> {
                column.spacing(0);
                column.addLine(line -> line
                        .name("IdentityColumnDivider")
                        .vertical(py(383 - 128))
                        .thickness(DIVIDER_W)
                        .color(RULE));
            });
            row.addSection("MetaColumn", column -> renderMeta(column, masthead));
        });
    }

    /** The rule under both columns, at the design's own width and inset. */
    static void renderIdentityRule(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("IdentityRule")
                .horizontal(px(924))
                .thickness(RULE_THICK)
                .color(RULE)
                .margin(new DocumentInsets(py(408 - 383), 0, 0, px(5))));
    }

    private static void renderSupplier(SectionBuilder column, InvoiceContactBlock supplier) {
        column.spacing(0).padding(new DocumentInsets(0, 0, 0, px(6)));
        column.addParagraph(p -> p
                .name("SupplierName")
                .text(supplier.legalName())
                .textStyle(bold(SUPPLIER_NAME_SIZE, INK)));

        column.addSection("SupplierAddress", block -> {
            block.spacing(capPitch(24, BODY_SIZE))
                    .margin(new DocumentInsets(
                            capPitch(38, SUPPLIER_NAME_SIZE, BODY_SIZE), 0, 0, 0));
            List<String> lines = supplier.addressLines();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int index = i;
                block.addParagraph(p -> p
                        .name("SupplierAddress_" + index)
                        .text(line)
                        .textStyle(style(BODY_SIZE, BODY)));
            }
        });

        column.addSection("SupplierContact", block -> {
            block.spacing(capPitch(28, BODY_SIZE))
                    .margin(new DocumentInsets(capPitch(46, BODY_SIZE), 0, 0, 0));
            channel(block, "Website", PlatformIcons.WEBSITE, supplier.website(),
                    ContactUri.webLink(supplier.website()));
            channel(block, "Email", PlatformIcons.EMAIL, supplier.email(),
                    ContactUri.mailLink(supplier.email()));
            channel(block, "Phone", PlatformIcons.PHONE, supplier.phone(),
                    ContactUri.telLink(supplier.phone()));
        });

        if (!supplier.taxRegistrationNumber().isBlank()) {
            column.addParagraph(p -> {
                p.name("SupplierTaxId");
                p.inlineText(supplier.taxRegistrationLabel(), style(BODY_SIZE, BODY));
                p.inlineText(spaces(20, BODY_SIZE), style(BODY_SIZE, BODY));
                p.inlineText(supplier.taxRegistrationNumber(), style(BODY_SIZE, BODY));
                p.margin(new DocumentInsets(capPitch(38, BODY_SIZE), 0, 0, 0));
            });
        }
    }

    /**
     * One contact channel: its mark, a measured gap, and the value.
     *
     * <p>The gap is a run of spaces rather than a column, because the design sets
     * mark and value on one baseline inside a single line — a two-column row
     * would put the mark on the line box's own top edge instead.</p>
     */
    private static void channel(SectionBuilder block, String name, String token,
                                String value, DocumentLinkOptions link) {
        if (value.isBlank()) {
            return;
        }
        block.addParagraph(p -> {
            p.name("SupplierContact" + name);
            p.inlineSvgIcon(PlatformIcons.icon(token), CONTACT_ICON, InlineImageAlignment.CENTER);
            p.inlineText(spaces(CONTACT_GAP_PX, BODY_SIZE), style(BODY_SIZE, BODY));
            if (link == null) {
                p.inlineText(value, style(BODY_SIZE, BODY));
            } else {
                p.inlineText(value, style(BODY_SIZE, BODY), link);
            }
        });
    }

    private static void renderMeta(SectionBuilder column, InvoiceMasthead masthead) {
        column.spacing(capPitch(36.17, BODY_SIZE))
                .padding(new DocumentInsets(0, 0, 0, META_PAD_L))
                .margin(new DocumentInsets(
                        py(138) - CAP_INSET * BODY_SIZE
                                - (py(135) - CAP_INSET * SUPPLIER_NAME_SIZE),
                        0, 0, 0));
        List<InvoiceMasthead.Entry> entries = masthead.entries();
        for (int i = 0; i < entries.size(); i++) {
            InvoiceMasthead.Entry entry = entries.get(i);
            layeredRow(column, "MetaRow_" + i, row -> {
                row.spacing(0)
                        .columns(DocumentRowColumn.fixed(META_LABEL_W),
                                DocumentRowColumn.weight(1));
                row.addParagraph(p -> p.text(entry.label()).textStyle(style(BODY_SIZE, BODY)));
                row.addParagraph(p -> p
                        .text(entry.value())
                        .textStyle(style(BODY_SIZE, entry.emphasized() ? ACCENT : INK)));
            });
        }
    }
}
