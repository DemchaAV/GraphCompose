package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.Objects;

/**
 * Inline icon-plus-text row widget — a small glyph image followed by a
 * label on the same baseline, optionally wrapped as a single click target.
 *
 * <h2>What it renders</h2>
 *
 * <p>One left-aligned paragraph holding two inline runs: a centred inline
 * image of {@code iconSize}×{@code iconSize}, then three spaces and the
 * label text. When a {@link DocumentLinkOptions} is supplied it is applied
 * to <strong>both</strong> the image run and the text run, so the whole row
 * (icon + label) is one clickable rectangle in the PDF — mirroring the flat
 * Mint Editorial blueprint's {@code iconLine()} contact / social rows.</p>
 *
 * <h2>When to use</h2>
 *
 * <p>Reach for {@code IconTextRow} when a sidebar stacks contact details or
 * social links as PNG-glyph rows (phone / email / location / website /
 * LinkedIn …) where each entire row should be clickable. It differs from the
 * shared {@code ContactLine} variants — those assume pipe-separated text or a
 * stacked link list with no per-row glyph — so this is the icon-driven row
 * primitive those widgets do not cover.</p>
 *
 * <h2>Reuse</h2>
 *
 * <p>Lives in {@code cv/v2/widgets} as a CV-sidebar primitive. Mint Editorial
 * is the first consumer; any future CV preset that wants icon-led contact or
 * social rows with a full-row click target can reuse it instead of inlining
 * the inline-image + link paragraph again.</p>
 */
public final class IconTextRow {

    /**
     * Spacer between the icon glyph and the label text.
     */
    private static final String GAP = "   ";

    private IconTextRow() {
    }

    /**
     * Renders an icon + text row.
     *
     * @param host     host section the row paragraph is appended to
     * @param icon     glyph image payload (already decoded / cached by the
     *                 caller); when {@code null} the row renders text only
     * @param iconSize icon edge length in points (width == height)
     * @param text     label text rendered after the icon; a blank label
     *                 still renders the icon, so callers should skip empty
     *                 rows upstream if that is unwanted
     * @param style    text style for the label
     * @param link     optional link wrapping the whole row (icon + label);
     *                 {@code null} renders a non-clickable row
     * @param margin   paragraph margin (vertical rhythm between rows)
     */
    public static void render(SectionBuilder host, DocumentImageData icon,
                              double iconSize, String text,
                              DocumentTextStyle style, DocumentLinkOptions link,
                              DocumentInsets margin) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(style, "style");
        String label = text == null ? "" : text;
        DocumentInsets rowMargin = margin == null ? DocumentInsets.zero() : margin;

        host.addParagraph(paragraph -> {
            paragraph.textStyle(style)
                    .align(TextAlign.LEFT)
                    .link(link)
                    .margin(rowMargin)
                    .rich(rich -> {
                        if (icon != null) {
                            rich.image(icon, iconSize, iconSize,
                                    InlineImageAlignment.CENTER, 0.0, link);
                        }
                        if (link != null) {
                            rich.link(GAP + label, link);
                        } else {
                            rich.style(GAP + label, style);
                        }
                    });
        });
    }
}
