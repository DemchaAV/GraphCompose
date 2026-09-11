package com.demcha.compose.document.templates.receipt.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptStatus;
import com.demcha.compose.document.templates.data.receipt.ReceiptStatusTone;

import java.util.Objects;

/**
 * The rounded status chip — {@code Completed}, {@code Processing},
 * {@code Returned} — rendered as an inline highlight so the pill hugs its
 * label instead of stretching across the column.
 *
 * <h2>Why the tone colours live here and not in the theme</h2>
 *
 * <p>These four pairs are semantic, not cosmetic: a settled payment reads
 * green and a failed one reads red in every brand's statement, and a reader
 * checking whether money arrived should not have to learn a new colour per
 * issuer. Putting them in {@code Palette} would invite a theme to make
 * "failed" green, which is the one thing a receipt must never do.</p>
 *
 * <p>The issuer's own accent still reaches the pill: an in-progress payment
 * has no universal colour, so it takes the brand accent the caller
 * supplies.</p>
 */
public final class StatusPill {

    /** Settled: money arrived. */
    private static final DocumentColor SETTLED_INK = DocumentColor.rgb(13, 110, 74);
    private static final DocumentColor SETTLED_FILL = DocumentColor.rgb(226, 244, 235);

    /** Attention: held, returned, disputed — someone has to look at it. */
    private static final DocumentColor ATTENTION_INK = DocumentColor.rgb(146, 94, 4);
    private static final DocumentColor ATTENTION_FILL = DocumentColor.rgb(253, 243, 222);

    /** Failed: the payment did not happen. */
    private static final DocumentColor FAILED_INK = DocumentColor.rgb(176, 42, 34);
    private static final DocumentColor FAILED_FILL = DocumentColor.rgb(251, 234, 233);

    /** Corner radius of the chip, in points — a full round on an 8pt label. */
    private static final double PILL_RADIUS = 9.0;

    private static final DocumentInsets PILL_PADDING = new DocumentInsets(3.5, 9, 3.5, 9);

    private StatusPill() {
    }

    /**
     * Renders the chip as its own paragraph.
     *
     * @param host   section the chip is appended to
     * @param status status label and tone; a blank label renders nothing
     * @param accent the issuer's brand accent, used for in-progress payments
     * @param align  where the chip sits in its column
     * @param theme  active theme
     */
    public static void render(SectionBuilder host, ReceiptStatus status,
                              DocumentColor accent, TextAlign align, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(theme, "theme");
        if (status == null || !status.hasLabel()) {
            return;
        }
        DocumentColor ink = inkFor(status.tone(), accent, theme);
        DocumentColor fill = fillFor(status.tone(), theme);
        host.addParagraph(p -> p
                .inlineHighlight(status.label(), ReceiptStyles.pill(theme, ink),
                        fill, PILL_RADIUS, PILL_PADDING)
                .align(align == null ? TextAlign.LEFT : align)
                .margin(DocumentInsets.zero()));
    }

    private static DocumentColor inkFor(ReceiptStatusTone tone, DocumentColor accent, BrandTheme theme) {
        return switch (tone) {
            case SETTLED -> SETTLED_INK;
            case ATTENTION -> ATTENTION_INK;
            case FAILED -> FAILED_INK;
            case IN_PROGRESS -> accent == null ? theme.palette().ink() : accent;
        };
    }

    private static DocumentColor fillFor(ReceiptStatusTone tone, BrandTheme theme) {
        return switch (tone) {
            case SETTLED -> SETTLED_FILL;
            case ATTENTION -> ATTENTION_FILL;
            case FAILED -> FAILED_FILL;
            // No universal colour for "on its way": the brand accent supplies the
            // ink, and the theme's own panel fill keeps the chip legible under it.
            case IN_PROGRESS -> theme.palette().banner();
        };
    }
}
