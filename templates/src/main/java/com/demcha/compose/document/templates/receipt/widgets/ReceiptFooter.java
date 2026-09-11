package com.demcha.compose.document.templates.receipt.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptData;
import com.demcha.compose.document.templates.receipt.components.ReceiptStyles;

import java.util.Objects;

/**
 * The foot of the receipt: a verification QR code, how to reach the issuer,
 * and the small print, under a hairline.
 *
 * <p>The QR encodes a URL where the payment can be checked against the
 * issuer's own records. That is the part of a receipt that survives being
 * printed: a scanned code turns a piece of paper back into a live
 * lookup.</p>
 *
 * <p>Every part is optional. A receipt with no verification URL renders the
 * text alone at full width rather than leaving a hole where the code would
 * have been.</p>
 */
public final class ReceiptFooter {

    /** Edge length of the QR code, in points. */
    private static final double QR_SIZE = 52.0;

    /** Weight of the QR column against the text beside it. */
    private static final double QR_COLUMN_WEIGHT = 0.16;

    /** Gap above the hairline that separates the footer from the body. */
    private static final double RULE_GAP = 6.0;

    private ReceiptFooter() {
    }

    /**
     * Reports whether a receipt carries anything the footer would draw.
     *
     * <p>A preset asks before adding the section that hosts it: an empty
     * section still takes its turn in the page flow's spacing, so a receipt
     * with no footer would otherwise end on a gap the size of one.</p>
     *
     * @param data receipt content
     * @return {@code true} when a QR code, support line, or small print exists
     */
    public static boolean hasContent(ReceiptData data) {
        return data != null
                && (!data.verificationUrl().isBlank()
                || !data.verificationText().isBlank()
                || !data.supportLines().isEmpty()
                || !data.legalNote().isBlank());
    }

    /**
     * Renders the footer block; renders nothing when the receipt carries no
     * QR code, support lines, or small print.
     *
     * @param host  section the footer is appended to
     * @param data  receipt content
     * @param theme active theme
     */
    public static void render(SectionBuilder host, ReceiptData data, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(theme, "theme");

        if (!hasContent(data)) {
            return;
        }
        boolean hasQr = !data.verificationUrl().isBlank();

        host.addLine(line -> line
                .fill()
                .thickness(theme.spacing().accentRuleWidth())
                .color(theme.palette().rule())
                .margin(new DocumentInsets(RULE_GAP, 0, RULE_GAP, 0)));

        if (hasQr) {
            host.addRow("ReceiptFooterRow", row -> row
                    .weights(QR_COLUMN_WEIGHT, 1)
                    .spacing(14)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .addSection("ReceiptVerificationCode", column -> column
                            .addBarcode(code -> code
                                    .qrCode()
                                    .data(data.verificationUrl())
                                    .size(QR_SIZE, QR_SIZE)
                                    .quietZone(1)
                                    .foreground(theme.palette().ink())
                                    .background(theme.palette().mainFill())))
                    .addSection("ReceiptFooterText", column -> footerText(column, data, theme)));
        } else {
            host.addSection("ReceiptFooterText", column -> footerText(column, data, theme));
        }
    }

    private static void footerText(SectionBuilder host, ReceiptData data, BrandTheme theme) {
        host.spacing(theme.spacing().sectionBodySpacing());
        if (!data.verificationText().isBlank()) {
            host.addParagraph(p -> p
                    .text(data.verificationText())
                    .textStyle(ReceiptStyles.caption(theme))
                    .margin(DocumentInsets.zero()));
        }
        if (!data.supportLines().isEmpty()) {
            host.addParagraph(p -> p
                    .text(String.join("\n", data.supportLines()))
                    .textStyle(ReceiptStyles.smallPrint(theme))
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .margin(DocumentInsets.zero()));
        }
        if (!data.legalNote().isBlank()) {
            host.addParagraph(p -> p
                    .text(data.legalNote())
                    .textStyle(ReceiptStyles.smallPrint(theme))
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .margin(DocumentInsets.zero()));
        }
    }
}
