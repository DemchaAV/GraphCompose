package com.demcha.compose.document.templates.receipt.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptData;
import com.demcha.compose.document.templates.data.receipt.ReceiptParty;
import com.demcha.compose.document.templates.receipt.components.FieldRowRenderer;
import com.demcha.compose.document.templates.receipt.components.ReceiptStyles;

import java.util.Objects;

/**
 * The two sides of the transfer, side by side inside one panel, with an
 * arrow between them.
 *
 * <p>The source document stacked payer and beneficiary as two unrelated grey
 * tables and left the reader to infer the direction. Two columns and one
 * arrow say it without a word.</p>
 *
 * <p>Both sides share a single panel rather than sitting in a card each: a
 * section is measured against its content, so two cards of unequal text come
 * out unequal in width, and a receipt in which "paid from" is visibly wider
 * than "paid to" reads as a rendering accident. One panel is also the truer
 * statement — this is one payment, not two facts that happen to be
 * adjacent.</p>
 */
public final class PartyPair {

    /** Weight of the narrow middle column holding the direction arrow. */
    private static final double ARROW_COLUMN_WEIGHT = 0.12;

    /** Size of the direction arrow, in points. */
    private static final double ARROW_SIZE = 11.0;

    private PartyPair() {
    }

    /**
     * Renders the panel; renders nothing when neither side is present.
     *
     * @param host   section the panel is appended to
     * @param data   receipt content
     * @param accent the issuer's brand accent, used for the arrow
     * @param theme  active theme
     */
    public static void render(SectionBuilder host, ReceiptData data,
                              DocumentColor accent, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(theme, "theme");

        if (!data.hasPayer() && !data.hasBeneficiary()) {
            return;
        }
        DocumentColor arrowColor = accent == null ? theme.palette().muted() : accent;
        boolean bothSides = data.hasPayer() && data.hasBeneficiary();

        host.addSection("ReceiptPartyPanel", panel -> {
            panel.softPanel(theme.palette().banner(),
                            DocumentCornerRadius.of(theme.spacing().bannerCornerRadius()),
                            theme.spacing().bannerInnerPadding())
                    .keepTogether();
            if (!bothSides) {
                // One side only: no direction to draw, so the column runs full width.
                if (data.hasPayer()) {
                    side(panel, data.payerLabel(), data.payer(), theme);
                } else {
                    side(panel, data.beneficiaryLabel(), data.beneficiary(), theme);
                }
                return;
            }
            panel.addRow("ReceiptPartyRow", row -> row
                    .weights(1, ARROW_COLUMN_WEIGHT, 1)
                    .spacing(10)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .addSection("ReceiptPayer", column ->
                            side(column, data.payerLabel(), data.payer(), theme))
                    .addSection("ReceiptDirection", column -> column
                            .addParagraph(p -> p
                                    .arrow(ARROW_SIZE, ShapeOutline.Direction.RIGHT, arrowColor)
                                    .align(TextAlign.CENTER)
                                    .margin(DocumentInsets.zero())))
                    .addSection("ReceiptBeneficiary", column ->
                            side(column, data.beneficiaryLabel(), data.beneficiary(), theme)));
        });
    }

    private static void side(SectionBuilder host, String label, ReceiptParty party, BrandTheme theme) {
        host.spacing(theme.spacing().sectionBodySpacing())
                .addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(label))
                        .textStyle(ReceiptStyles.eyebrow(theme))
                        .margin(DocumentInsets.zero()));

        if (!party.name().isBlank()) {
            host.addParagraph(p -> p
                    .text(party.name())
                    .textStyle(ReceiptStyles.partyName(theme))
                    .margin(DocumentInsets.zero()));
        }
        if (!party.addressLines().isEmpty()) {
            host.addParagraph(p -> p
                    .text(String.join("\n", party.addressLines()))
                    .textStyle(ReceiptStyles.caption(theme))
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .margin(DocumentInsets.zero()));
        }
        // Inline form: a side is a column of the pair row, and a row cannot hold
        // another row.
        FieldRowRenderer.renderInline(host, party.fields(), TextAlign.LEFT, theme);
    }
}
