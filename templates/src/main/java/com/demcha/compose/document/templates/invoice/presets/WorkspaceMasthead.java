package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;
import com.demcha.compose.document.templates.data.invoice.InvoiceRecipient;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT_RULE_T;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ACCENT_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DISC_D;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.DISC_GAP;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.HALF;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.ISSUER_TAX_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.LOCKUP_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.META_CELL_INSET;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.META_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.META_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.PARTY_TAX_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.PARTY_TEXT_INDENT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.RULE_THIN;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.SECTION_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TITLE_RIGHT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.TITLE_TOP_LIFT;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.WORDMARK_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.blockGap;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.capOffset;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.WorkspaceStyles.style;

/**
 * The header and the two addressed parties: the lockup beside the title, the
 * accent bar under it, the issuer against the metadata grid, and the bill-to /
 * ship-to split.
 *
 * <p>Both splits divide the content box in half: measured, their two vertical
 * dividers land on the same x. Neither divider is a node — each is the right
 * cell's <em>left border</em>, which is why it begins and ends with that cell's
 * content rather than running the row's full height.</p>
 */
final class WorkspaceMasthead {

    private WorkspaceMasthead() {
    }

    /**
     * Draws the header, the accent bar, the issuer/metadata row and the rule
     * that closes it.
     *
     * @param page     the page flow
     * @param brand    the lockup
     * @param supplier who is invoicing
     * @param masthead the title and the metadata rows
     */
    static void renderHeader(PageFlowBuilder page, InvoiceBrand brand,
                             InvoiceContactBlock supplier, InvoiceMasthead masthead) {
        renderLockupRow(page, brand, masthead);
        renderAccentBar(page);
        page.addRow("IssuerMetaRow", row -> {
            row.spacing(0).weights(HALF, HALF);
            row.addSection("IssuerAddress", cell -> renderIssuer(cell, supplier));
            row.addSection("InvoiceMeta", cell -> {
                cell.spacing(0)
                        .borders(DocumentBorders.left(DocumentStroke.of(HAIRLINE, RULE_THIN)))
                        .padding(new DocumentInsets(0, 0, 0, META_CELL_INSET));
                renderMeta(cell, masthead.entries());
            });
        });
        WorkspaceWidgets.fullWidthRule(page, "PartiesRule", HAIRLINE, RULE_THIN,
                blockGap(28.5, BODY_SIZE, 0), px(24));
    }

    /**
     * TOP, not CENTER: the title's line box is taller than the lockup, so
     * centring pushes the lockup down the row it is supposed to set the top edge
     * of.
     *
     * <p>What fills the lockup column comes from the brand — a logo drawn to the
     * design's measured width, or the brand's name set as a wordmark. The
     * templates artifact carries no mark of its own.</p>
     */
    private static void renderLockupRow(PageFlowBuilder page, InvoiceBrand brand,
                                        InvoiceMasthead masthead) {
        page.addRow("Masthead", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.fixed(LOCKUP_W), DocumentRowColumn.weight(1));
            row.addSection("BrandLockup", cell -> {
                cell.spacing(0);
                if (brand.logo() != null) {
                    cell.add(new ImageBuilder()
                            .name("BrandLogo")
                            .source(brand.logo())
                            .width(LOCKUP_W)
                            .build());
                } else if (!brand.name().isBlank()) {
                    cell.addParagraph(p -> p
                            .name("BrandWordmark")
                            .text(brand.name())
                            .textStyle(style(WORDMARK_SIZE, INK, DocumentTextDecoration.BOLD)));
                }
            });
            row.addSection("InvoiceTitle", cell -> cell
                    .spacing(0)
                    .padding(new DocumentInsets(-TITLE_TOP_LIFT, TITLE_RIGHT_INSET, 0, 0))
                    .addParagraph(p -> p
                            .name("InvoiceTitleText")
                            .text(masthead.title())
                            .textStyle(style(TITLE_SIZE, INK, DocumentTextDecoration.BOLD))
                            .align(TextAlign.RIGHT)));
        });
    }

    /**
     * The bar is anchored to the content box's left edge, not to the mark above
     * it, so its width derives from the page rather than from the asset. Stating
     * its own position rather than a gap keeps the two facts separable when
     * either changes.
     */
    private static void renderAccentBar(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("BrandAccentRule")
                .horizontal(ACCENT_RULE_W)
                .thickness(ACCENT_RULE_T)
                .color(ACCENT)
                .margin(new DocumentInsets(
                        px(89) - (LINE_BOX * TITLE_SIZE - TITLE_TOP_LIFT),
                        0,
                        blockGap(19, 0, META_LABEL_SIZE),
                        0)));
    }

    private static void renderIssuer(SectionBuilder cell, InvoiceContactBlock supplier) {
        // The two cells of the split do not share a top edge: the metadata block
        // starts above the issuer name, and the divider starts with it.
        cell.spacing(0).padding(new DocumentInsets(px(12), 0, 0, px(2)));
        cell.addParagraph(p -> p
                .name("IssuerName")
                .text(supplier.legalName())
                .textStyle(style(NAME_SIZE, INK, DocumentTextDecoration.BOLD)));
        cell.addSection("IssuerAddressLines", lines -> {
            lines.spacing(gap(27.5, BODY_SIZE))
                    .margin(new DocumentInsets(gap(38, NAME_SIZE), 0, 0, 0));
            WorkspaceWidgets.textLines(lines, "IssuerAddressLine", supplier.addressLines(),
                    BODY_SIZE, INK);
        });
        renderLabelledRow(cell, "Issuer", supplier.taxRegistrationLabel(),
                supplier.taxRegistrationNumber(), ISSUER_TAX_LABEL_W, gap(38, BODY_SIZE));
    }

    private static void renderMeta(SectionBuilder cell, List<InvoiceMasthead.Entry> entries) {
        cell.addSection("InvoiceMetaRows", rows -> {
            rows.spacing(gap(36.2, BODY_SIZE));
            int index = 0;
            for (InvoiceMasthead.Entry entry : entries) {
                String name = "MetaRow" + (++index);
                WorkspaceWidgets.layeredRow(rows, name, row -> {
                    row.spacing(0)
                            .columns(DocumentRowColumn.fixed(META_LABEL_W),
                                    DocumentRowColumn.weight(1));
                    row.addParagraph(p -> p
                            .name(name + "Label")
                            .text(entry.label())
                            .textStyle(style(META_LABEL_SIZE, INK,
                                    DocumentTextDecoration.DEFAULT)));
                    row.addParagraph(p -> p
                            .name(name + "Value")
                            .text(entry.value())
                            .textStyle(style(BODY_SIZE,
                                    entry.emphasized() ? ACCENT : INK,
                                    entry.emphasized()
                                            ? DocumentTextDecoration.BOLD
                                            : DocumentTextDecoration.DEFAULT)));
                });
            }
        });
    }

    /**
     * Draws the two addressed parties.
     *
     * @param page   the page flow
     * @param billTo who is billed
     * @param shipTo where the work went, or a party with no name when absent
     */
    static void renderParties(PageFlowBuilder page, InvoiceRecipient billTo,
                              InvoiceRecipient shipTo) {
        List<InvoiceRecipient> parties = new ArrayList<>();
        parties.add(billTo);
        if (!shipTo.name().isBlank()) {
            parties.add(shipTo);
        }
        List<String> tokens = List.of(WorkspaceIcons.BILL_TO, WorkspaceIcons.SHIP_TO);

        page.addRow("PartiesRow", row -> {
            row.spacing(0).weights(WorkspaceWidgets.evenWeights(parties.size()));
            for (int i = 0; i < parties.size(); i++) {
                InvoiceRecipient party = parties.get(i);
                String token = tokens.get(Math.min(i, tokens.size() - 1));
                boolean first = i == 0;
                row.addSection("Party" + (i + 1), cell -> {
                    cell.spacing(0);
                    if (!first) {
                        // The divider belongs to the split, so it is the cell's
                        // own left edge rather than a rule drawn between them.
                        cell.borders(DocumentBorders.left(DocumentStroke.of(HAIRLINE, RULE_THIN)))
                                .padding(new DocumentInsets(0, 0, 0, px(28)));
                    }
                    renderParty(cell, party, token);
                });
            }
        });
    }

    /**
     * One method for both parties: they are the same component with different
     * data, down to the registration only one of them usually prints.
     */
    private static void renderParty(SectionBuilder cell, InvoiceRecipient party, String token) {
        WorkspaceWidgets.layeredRow(cell, "PartyHead", row -> {
            row.spacing(DISC_GAP)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.fixed(DISC_D), DocumentRowColumn.weight(1));
            row.addSection("PartyDisc", disc -> disc
                    .spacing(0)
                    .add(WorkspaceWidgets.disc(token)));
            row.addParagraph(p -> p
                    .name("PartyLabel")
                    .text(party.heading())
                    .textStyle(style(SECTION_LABEL_SIZE, ACCENT, DocumentTextDecoration.BOLD))
                    .margin(new DocumentInsets(capOffset(8, SECTION_LABEL_SIZE), 0, 0, 0)));
        });
        cell.addSection("PartyBody", block -> {
            block.spacing(0).padding(new DocumentInsets(0, 0, 0, PARTY_TEXT_INDENT));
            block.addParagraph(p -> p
                    .name("PartyName")
                    .text(party.name())
                    .textStyle(style(NAME_SIZE, INK, DocumentTextDecoration.BOLD))
                    .margin(new DocumentInsets(px(2), 0, 0, 0)));
            if (!party.subline().isBlank()) {
                block.addParagraph(p -> p
                        .name("PartySubline")
                        .text(party.subline())
                        .textStyle(style(BODY_SIZE, INK, DocumentTextDecoration.DEFAULT))
                        .margin(new DocumentInsets(gap(30, NAME_SIZE), 0, 0, 0)));
            }
            block.addSection("PartyAddressLines", lines -> {
                lines.spacing(gap(28, BODY_SIZE))
                        .margin(new DocumentInsets(
                                gap(35, party.subline().isBlank() ? NAME_SIZE : BODY_SIZE),
                                0, 0, 0));
                WorkspaceWidgets.textLines(lines, "PartyAddressLine", party.addressLines(),
                        BODY_SIZE, INK);
            });
            renderLabelledRow(block, "Party", party.registrationLabel(),
                    party.registrationNumber(), PARTY_TAX_LABEL_W, gap(47, BODY_SIZE));
        });
    }

    /**
     * A labelled fact under an address — a registration, printed as a label
     * column beside its value. Drawn only when there is a value: a label on its
     * own is a heading over an absence.
     */
    private static void renderLabelledRow(SectionBuilder parent, String prefix, String label,
                                          String value, double labelWidth, double gapAbove) {
        if (value == null || value.isBlank()) {
            return;
        }
        WorkspaceWidgets.layeredRow(parent, prefix + "TaxLine", row -> {
            row.spacing(0)
                    .margin(new DocumentInsets(gapAbove, 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(labelWidth), DocumentRowColumn.weight(1));
            row.addParagraph(p -> p
                    .name(prefix + "TaxLabel")
                    .text(label)
                    .textStyle(style(BODY_SIZE, INK, DocumentTextDecoration.DEFAULT)));
            row.addParagraph(p -> p
                    .name(prefix + "TaxValue")
                    .text(value)
                    .textStyle(style(BODY_SIZE, INK, DocumentTextDecoration.DEFAULT)));
        });
    }
}
