package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_GUTTER;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_PAD_H;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_RADIUS;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.CARD_STROKE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.DISC_CLIENT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.DISC_SUPPLIER;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.HALF;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.MUTED;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PARTY_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PARTY_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.PARTY_TEXT_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.SURFACE;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.bold;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capGap;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.capTop;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.plain;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.initials;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.initialsDisc;
import static com.demcha.compose.document.templates.invoice.presets.ObsidianWidgets.layeredRow;

/**
 * The two party cards: the issuer beside the billed party, each opening with a
 * filled disc.
 */
final class ObsidianParties {

    /** The address lines' measured cap tops; the first is taken off the name. */
    private static final double[] LINE_AT = {420, 446, 471};

    private ObsidianParties() {
    }

    /**
     * The parties row.
     *
     * @param page     the page flow
     * @param brand    the issuer's brand, whose logo the supplier's disc carries
     * @param supplier the issuer
     * @param billTo   the billed party
     */
    static void render(PageFlowBuilder page, InvoiceBrand brand,
                       InvoiceContactBlock supplier, InvoiceRecipient billTo) {
        page.addRow("PartiesRow", row -> {
            row.spacing(CARD_GUTTER).verticalAlign(RowVerticalAlign.TOP).weights(HALF, HALF);
            row.addSection("SupplierCard", cell -> renderCard(cell, "Supplier", "From",
                    supplierDisc(brand, supplier), supplier.legalName(),
                    supplier.addressLines(), supplier.email(),
                    ContactUri.mailLink(supplier.email())));
            row.addSection("BillToCard", cell -> renderCard(cell, "BillTo", billTo.heading(),
                    initialsDisc("BillToDisc", DISC_CLIENT, initials(billTo.name())),
                    billTo.name(), billTo.addressLines(), billTo.email(),
                    ContactUri.mailLink(billTo.email())));
        });
    }

    /**
     * The issuer's disc: the caller's logo when there is one, the brand's own
     * monogram when it states one, and initials taken from the name otherwise.
     *
     * <p>This design carries the issuer's mark here rather than in the masthead,
     * where it sets the name as type instead — so a caller with a drawing to show
     * has exactly one place to put it.</p>
     */
    private static DocumentNode supplierDisc(InvoiceBrand brand, InvoiceContactBlock supplier) {
        if (brand.logo() != null) {
            return ObsidianWidgets.disc("SupplierDisc", DISC_SUPPLIER,
                    new ImageBuilder()
                            .name("SupplierDiscLogo")
                            .source(brand.logo())
                            .width(ObsidianStyles.DISC_GLYPH)
                            .build(),
                    DISC_D);
        }
        String monogram = (brand.monogramTop() + brand.monogramBottom()).trim();
        String letters = monogram.isBlank()
                ? initials(brand.name().isBlank() ? supplier.legalName() : brand.name())
                : monogram;
        return initialsDisc("SupplierDisc", DISC_SUPPLIER, letters);
    }

    private static void renderCard(SectionBuilder cell, String id, String label,
                                   DocumentNode disc, String name, List<String> addressLines,
                                   String contact, DocumentLinkOptions link) {
        cell.spacing(0)
                .fillColor(SURFACE)
                .stroke(CARD_STROKE)
                .cornerRadius(CARD_RADIUS)
                .padding(new DocumentInsets(0, CARD_PAD_H, px(26), CARD_PAD_H))
                .keepTogether();
        cell.addParagraph(p -> p
                .name(id + "Label")
                .text(label)
                .textStyle(bold(PARTY_LABEL_SIZE, ACCENT))
                .margin(capTop(355 - 325, PARTY_LABEL_SIZE, true)));
        layeredRow(cell, id + "Body", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .margin(new DocumentInsets(
                            capGap(388 - 355, PARTY_LABEL_SIZE, true, 0, false), 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(PARTY_TEXT_INDENT),
                            DocumentRowColumn.weight(1));
            row.addSection(id + "Disc", cell2 -> cell2.spacing(0).add(disc));
            row.addSection(id + "Text", text -> {
                text.spacing(0);
                text.addParagraph(p -> p
                        .name(id + "Name")
                        .text(name)
                        .textStyle(bold(PARTY_NAME_SIZE, INK))
                        .margin(capTop(392 - 388, PARTY_NAME_SIZE, true)));
                for (int i = 0; i < addressLines.size() && i < LINE_AT.length; i++) {
                    String line = addressLines.get(i);
                    int index = i;
                    double above = index == 0
                            ? capGap(LINE_AT[0] - 392, PARTY_NAME_SIZE, true, BODY_SIZE, false)
                            : capGap(LINE_AT[index] - LINE_AT[index - 1], BODY_SIZE, false,
                                    BODY_SIZE, false);
                    double gapAbove = Math.max(0, above);
                    text.addParagraph(p -> p
                            .name(id + "AddressLine_" + index)
                            .text(line)
                            .textStyle(plain(BODY_SIZE, MUTED))
                            .margin(new DocumentInsets(gapAbove, 0, 0, 0)));
                }
                if (!contact.isBlank()) {
                    text.addParagraph(p -> {
                        p.name(id + "Contact").margin(new DocumentInsets(Math.max(0,
                                capGap(504 - 471, BODY_SIZE, false, BODY_SIZE, false)), 0, 0, 0));
                        if (link == null) {
                            p.text(contact).textStyle(plain(BODY_SIZE, MUTED));
                        } else {
                            p.inlineText(contact, plain(BODY_SIZE, MUTED), link);
                        }
                    });
                }
            });
        });
    }

    /** The disc colour the issuer's card uses, for the closing band to match. */
    static DocumentColor supplierTone() {
        return DISC_SUPPLIER;
    }
}
