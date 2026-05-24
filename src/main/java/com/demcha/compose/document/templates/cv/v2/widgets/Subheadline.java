package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;

/**
 * Secondary headline widget — the small spaced-caps tagline rendered
 * directly beneath the main {@link Headline} (e.g.
 * {@code P R O F E S S I O N A L   T I T L E}).
 *
 * <h2>When to use</h2>
 *
 * <p>Reach for {@code Subheadline} when a preset stacks a quieter
 * descriptor under the subject's name — typical of classic /
 * editorial layouts where the headline block reads as two centred
 * lines: a loud name and a soft caption. Visual signature of
 * {@code CenteredHeadline}; will likely fit
 * {@code EditorialBlue} when ported.</p>
 *
 * <h2>Variants</h2>
 *
 * <ul>
 *   <li>{@link #centeredSpacedCaps} — centred, transformed to
 *       letter-spaced uppercase. The canonical use today.</li>
 * </ul>
 *
 * <p>If a future preset wants a right-aligned or verbatim (non-spaced)
 * subheadline, add a sibling factory here — keep the
 * {@code (host, text, style)} signature shape so call sites stay
 * uniform. Padding is supplied by the caller via {@link SectionBuilder}
 * because subheadline insets are part of the headline block's vertical
 * rhythm, not a per-widget concern.</p>
 */
public final class Subheadline {

    private Subheadline() {
    }

    /**
     * Centred letter-spaced uppercase subheadline. Text is transformed
     * through {@link TextOrnaments#spacedUpper(String)} — pass the raw
     * caption ({@code "Professional Title"}) and the widget handles the
     * spacing.
     *
     * @param host  host section (typically the same section that
     *              hosts the main {@link Headline})
     * @param text  caption text to render, before the spaced-caps
     *              transform
     * @param style explicit text style — the subheadline has no
     *              dedicated theme slot, so the caller composes
     *              {@code font + size + decoration + colour} and hands
     *              it in. Centralise the style in the preset, not at
     *              each call site.
     */
    public static void centeredSpacedCaps(SectionBuilder host, String text,
                                          DocumentTextStyle style) {
        host.addParagraph(p -> p
                .text(TextOrnaments.spacedUpper(text))
                .textStyle(style)
                .align(TextAlign.CENTER)
                .margin(DocumentInsets.top(1)));
    }
}
