package com.demcha.compose.document.templates.receipt.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptData;
import com.demcha.compose.document.templates.receipt.components.FieldRowRenderer;
import com.demcha.compose.document.templates.receipt.components.ReceiptStyles;
import com.demcha.compose.document.templates.receipt.components.StatusPill;

import java.util.Objects;

/**
 * The block that answers the question the reader opened the file for: how
 * much, to what, and did it go through.
 *
 * <p>The amount is set at the theme's headline size — larger than the
 * document title — because on a receipt it <em>is</em> the headline. The
 * status chip sits at the top of the right column so the two facts that
 * matter share one eye-line, and the value/operation dates fill the rest of
 * that column as a compact table rather than as another block further down
 * the page.</p>
 *
 * <p>A brand accent strip runs down the left edge: it is the one place the
 * issuer's colour appears above the fold, which is what makes the layout
 * read as branded without tinting the numbers.</p>
 */
public final class AmountHero {

    /**
     * How much wider the amount column is than the status column. The amount
     * needs room to stay on one line at 30pt; the dates beside it are short.
     */
    private static final double AMOUNT_COLUMN_WEIGHT = 1.35;

    private static final double STATUS_COLUMN_WEIGHT = 1.0;

    private AmountHero() {
    }

    /**
     * Renders the hero panel.
     *
     * @param host   section the panel is appended to
     * @param data   receipt content
     * @param accent the issuer's brand accent
     * @param theme  active theme
     */
    public static void render(SectionBuilder host, ReceiptData data,
                              DocumentColor accent, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(theme, "theme");

        DocumentColor strip = accent == null ? theme.palette().ink() : accent;

        host.addSection("ReceiptAmountHero", panel -> panel
                .softPanel(theme.palette().banner(),
                        DocumentCornerRadius.of(theme.spacing().bannerCornerRadius()),
                        theme.spacing().bannerInnerPadding())
                .accentLeft(strip, 3.0)
                .keepTogether()
                .addRow("ReceiptAmountRow", row -> row
                        .weights(AMOUNT_COLUMN_WEIGHT, STATUS_COLUMN_WEIGHT)
                        .spacing(20)
                        .verticalAlign(RowVerticalAlign.TOP)
                        .addSection("ReceiptAmountColumn", column -> {
                            column.spacing(3);
                            column.addParagraph(p -> p
                                    .text(TextOrnaments.spacedUpper(data.amountLabel()))
                                    .textStyle(ReceiptStyles.eyebrow(theme))
                                    .margin(DocumentInsets.zero()));
                            column.addParagraph(p -> p
                                    .text(data.amount())
                                    .textStyle(ReceiptStyles.amount(theme))
                                    .margin(DocumentInsets.zero()));
                            if (!data.amountCaption().isBlank()) {
                                column.addParagraph(p -> p
                                        .text(data.amountCaption())
                                        .textStyle(ReceiptStyles.caption(theme))
                                        .lineSpacing(theme.typography().bodyLineSpacing())
                                        .margin(DocumentInsets.zero()));
                            }
                        })
                        .addSection("ReceiptStatusColumn", column -> {
                            column.spacing(theme.spacing().sectionBodySpacing());
                            StatusPill.render(column, data.status(), accent, TextAlign.RIGHT, theme);
                            // Inline form: this column is inside a row, and a row
                            // cannot hold another row.
                            FieldRowRenderer.renderInline(column, data.summaryFields(),
                                    TextAlign.RIGHT, theme);
                        })));
    }
}
