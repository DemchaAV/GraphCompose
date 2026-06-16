package com.demcha.examples.flagships;

import com.demcha.compose.document.style.DocumentColor;

/**
 * Palette layer for {@link EngineDeckExample} — the v2 "theme" layer. The violet
 * brand identity (from {@code logo.svg}), the neutrals, and the dark-banner /
 * code / chart-series colours in one place, so the composition and the chart
 * builders ({@link EngineDeckCharts}) read the same tokens. Both import it
 * statically.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class EngineDeckTheme {

    private EngineDeckTheme() {
    }

    // ── Violet brand identity (from logo.svg) ─────────────────────────────────
    static final DocumentColor HERO_BG = DocumentColor.rgb(18, 18, 33);
    static final DocumentColor VIOLET = DocumentColor.rgb(124, 92, 252);
    static final DocumentColor VIOLET_DEEP = DocumentColor.rgb(97, 40, 217);
    static final DocumentColor VIOLET_LIGHT = DocumentColor.rgb(167, 139, 250);

    // ── Neutrals ──────────────────────────────────────────────────────────────
    static final DocumentColor INK = DocumentColor.rgb(28, 30, 46);
    static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 132);
    static final DocumentColor BODY = DocumentColor.rgb(64, 68, 84);
    static final DocumentColor SURFACE = DocumentColor.rgb(247, 248, 252);
    static final DocumentColor SURFACE_LINE = DocumentColor.rgb(226, 229, 239);
    static final DocumentColor HERO_TEXT = DocumentColor.rgb(208, 211, 226);

    // ── Banner (page 1) ───────────────────────────────────────────────────────
    static final DocumentColor CODE_BG = DocumentColor.rgb(12, 12, 22);
    static final DocumentColor CARD_DARK = DocumentColor.rgb(36, 35, 60);
    static final DocumentColor RULE_DARK = DocumentColor.rgb(58, 56, 92);
    static final DocumentColor GREEN = DocumentColor.rgb(46, 196, 138);
    static final DocumentColor CODE_TXT = DocumentColor.rgb(178, 182, 202);
    static final DocumentColor CODE_STR = DocumentColor.rgb(120, 204, 170);
    static final DocumentColor CODE_FN = DocumentColor.rgb(150, 190, 255);
    static final DocumentColor ON_DARK = DocumentColor.rgb(230, 232, 242);
    static final DocumentColor ON_DARK_MUTED = DocumentColor.rgb(152, 156, 178);

    // ── Comparison series (GraphCompose = violet, iText = slate, Jasper = amber) ─
    static final DocumentColor SLATE = DocumentColor.rgb(118, 126, 148);
    static final DocumentColor AMBER = DocumentColor.rgb(224, 158, 72);
}
