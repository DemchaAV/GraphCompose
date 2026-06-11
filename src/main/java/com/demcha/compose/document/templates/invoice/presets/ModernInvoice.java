package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.invoice.builder.InvoiceBuilder;
import com.demcha.compose.document.templates.invoice.spec.InvoiceSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;

/**
 * Templates v2 "Modern Invoice" preset — minimal v2 invoice surface.
 *
 * <p>Provides a clean, single-column invoice rendering through the new
 * {@link InvoiceBuilder} pipeline. The legacy {@code InvoiceTemplateV2}
 * continues to ship the cinematic invoice experience; this v2 preset
 * is the canonical seam for builder-driven custom invoices.</p>
 */
public final class ModernInvoice {

    /**
     * Stable template identifier.
     */
    public static final String ID = "modern-invoice";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Modern Invoice";

    private ModernInvoice() {
    }

    /**
     * Builds a fresh {@code Modern Invoice} template configured for
     * the given business theme.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<InvoiceSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.comfortable();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(20.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(41, 128, 185))
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.0)
                .color(DocumentColor.rgb(40, 50, 70))
                .build();

        return InvoiceBuilder.builder()
                .id(ID)
                .displayName(DISPLAY_NAME)
                .headingStyle(headingStyle)
                .bodyStyle(bodyStyle)
                .spacing(spacing)
                .build();
    }
}
