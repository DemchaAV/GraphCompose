package com.demcha.compose.document.output;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.font.FontName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Backend-neutral header / footer chrome configuration.
 *
 * <p>Text supports the standard chrome placeholder tokens accepted by the PDF
 * backend: {@code {page}}, {@code {pages}}, and {@code {date}}. Backends that
 * cannot evaluate the tokens may render them verbatim.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentHeaderFooter {
    @Builder.Default
    private final DocumentHeaderFooterZone zone = DocumentHeaderFooterZone.HEADER;

    @Builder.Default
    private final float height = 30f;

    private final String leftText;
    private final String centerText;
    private final String rightText;

    @Builder.Default
    private final float fontSize = 9f;

    /**
     * Font family for the zone's text. Standard-14 Helvetica by default — the face
     * the zone has always drawn — so an existing header or footer is unchanged.
     * Set it to reach a family with the coverage the text needs: the engine has no
     * automatic fallback, here or in the body, so a code point the chosen family
     * cannot encode is substituted with {@code ?}.
     *
     * @since 2.2.3
     */
    @Builder.Default
    private final FontName fontName = FontName.HELVETICA;

    @Builder.Default
    private final DocumentColor textColor = DocumentColor.GRAY;

    @Builder.Default
    private final boolean showSeparator = false;

    @Builder.Default
    private final DocumentColor separatorColor = DocumentColor.LIGHT_GRAY;

    @Builder.Default
    private final float separatorThickness = 0.5f;

    @Builder.Default
    private final DocumentPageNumbering numbering = DocumentPageNumbering.DEFAULT;

    private DocumentHeaderFooter() {
        this.zone = DocumentHeaderFooterZone.HEADER;
        this.height = 30f;
        this.leftText = null;
        this.centerText = null;
        this.rightText = null;
        this.fontSize = 9f;
        this.fontName = FontName.HELVETICA;
        this.textColor = DocumentColor.GRAY;
        this.showSeparator = false;
        this.separatorColor = DocumentColor.LIGHT_GRAY;
        this.separatorThickness = 0.5f;
        this.numbering = DocumentPageNumbering.DEFAULT;
    }

    /**
     * Returns a copy of these options targeted at the supplied zone.
     *
     * @param zone target zone
     * @return updated header/footer
     */
    public DocumentHeaderFooter withZone(DocumentHeaderFooterZone zone) {
        return toBuilder().zone(zone).build();
    }
}
