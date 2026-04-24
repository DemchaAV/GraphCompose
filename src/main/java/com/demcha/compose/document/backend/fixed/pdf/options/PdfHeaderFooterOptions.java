package com.demcha.compose.document.backend.fixed.pdf.options;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.awt.Color;

/**
 * Canonical repeating header/footer configuration for the semantic PDF API.
 *
 * <p>Text supports the standard PDF chrome placeholder tokens:
 * {@code {page}}, {@code {pages}}, and {@code {date}}.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PdfHeaderFooterOptions {
    @Builder.Default
    private final PdfHeaderFooterZone zone = PdfHeaderFooterZone.HEADER;

    @Builder.Default
    private final float height = 30f;

    private final String leftText;
    private final String centerText;
    private final String rightText;

    @Builder.Default
    private final float fontSize = 9f;

    @Builder.Default
    private final Color textColor = new Color(128, 128, 128);

    @Builder.Default
    private final boolean showSeparator = false;

    @Builder.Default
    private final Color separatorColor = new Color(200, 200, 200);

    @Builder.Default
    private final float separatorThickness = 0.5f;

    private PdfHeaderFooterOptions() {
        this.zone = PdfHeaderFooterZone.HEADER;
        this.height = 30f;
        this.leftText = null;
        this.centerText = null;
        this.rightText = null;
        this.fontSize = 9f;
        this.textColor = new Color(128, 128, 128);
        this.showSeparator = false;
        this.separatorColor = new Color(200, 200, 200);
        this.separatorThickness = 0.5f;
    }

    /**
     * Returns a copy of these options with the supplied zone.
     *
     * @param zone target header/footer zone
     * @return a copy configured for the requested zone
     */
    public PdfHeaderFooterOptions withZone(PdfHeaderFooterZone zone) {
        return toBuilder().zone(zone).build();
    }
}
