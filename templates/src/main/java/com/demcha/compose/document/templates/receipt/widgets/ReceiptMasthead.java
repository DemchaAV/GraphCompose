package com.demcha.compose.document.templates.receipt.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.core.identity.SvgGlyph;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptData;
import com.demcha.compose.document.templates.receipt.components.ReceiptStyles;

import java.util.Objects;

/**
 * Top of the page: the issuer on the left, what the document is on the
 * right, and a hairline under both.
 *
 * <p>The issuer's mark renders as a recolourable {@link SvgGlyph} silhouette
 * rather than as an image, so one logo file serves a black wordmark on white
 * and a white one on a dark page without a second asset. When no logo is
 * supplied the issuer's name stands in as a spaced-caps wordmark, which is
 * what keeps the masthead intact for a caller who has no vector mark to
 * hand.</p>
 */
public final class ReceiptMasthead {

    /** Gap between the masthead row and the hairline under it, in points. */
    private static final double RULE_GAP = 10.0;

    private ReceiptMasthead() {
    }

    /**
     * Renders the masthead and its hairline.
     *
     * @param host      section the masthead is appended to
     * @param data      receipt content
     * @param logo      the issuer's mark, or {@code null} to fall back to the
     *                  issuer's name as a spaced-caps wordmark
     * @param logoWidth width of the mark in points; the height follows the
     *                  glyph's own aspect ratio
     * @param logoColor colour the mark is filled with
     * @param theme     active theme
     */
    public static void render(SectionBuilder host, ReceiptData data, SvgGlyph logo,
                              double logoWidth, DocumentColor logoColor, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(theme, "theme");

        DocumentColor markColor = logoColor == null ? theme.palette().ink() : logoColor;

        host.addRow("ReceiptMastheadRow", row -> row
                .weights(1, 1)
                .spacing(18)
                .verticalAlign(RowVerticalAlign.CENTER)
                .addSection("ReceiptIssuer", column -> {
                    boolean hasName = !data.issuerName().isBlank();
                    if (logo == null && !hasName) {
                        return;
                    }
                    column.addParagraph(p -> {
                        if (logo != null) {
                            // Inline, so a symbol mark and the issuer's name sit on one
                            // baseline as a lockup. A caller whose mark is already a
                            // wordmark leaves the name blank and gets the mark alone.
                            p.shape(logo.outline(logoWidth), markColor);
                            if (hasName) {
                                p.inlineText("  ");
                            }
                        }
                        if (hasName) {
                            p.inlineText(logo == null
                                            ? TextOrnaments.spacedUpper(data.issuerName())
                                            : data.issuerName(),
                                    ReceiptStyles.title(theme));
                        }
                        p.margin(DocumentInsets.zero());
                    });
                })
                .addSection("ReceiptDocumentTitle", column -> {
                    column.spacing(2);
                    column.addParagraph(p -> p
                            .text(data.documentTitle())
                            .textStyle(ReceiptStyles.title(theme))
                            .align(TextAlign.RIGHT)
                            .margin(DocumentInsets.zero()));
                    if (!data.generatedOn().isBlank()) {
                        column.addParagraph(p -> p
                                .text("Generated on " + data.generatedOn())
                                .textStyle(ReceiptStyles.caption(theme))
                                .align(TextAlign.RIGHT)
                                .margin(DocumentInsets.zero()));
                    }
                    if (!data.reference().isBlank()) {
                        column.addParagraph(p -> p
                                .text("Reference " + data.reference())
                                .textStyle(ReceiptStyles.caption(theme))
                                .align(TextAlign.RIGHT)
                                .margin(DocumentInsets.zero()));
                    }
                }));

        host.addLine(line -> line
                .fill()
                .thickness(theme.spacing().accentRuleWidth())
                .color(theme.palette().rule())
                .margin(new DocumentInsets(RULE_GAP, 0, 0, 0)));
    }
}
