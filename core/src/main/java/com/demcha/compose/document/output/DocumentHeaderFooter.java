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
 * <p>This is the text band: three slots, three tokens, painted after the page is
 * laid out. When the band needs to hold something — a badge, a link, a logo, a
 * layout — reach for {@link DocumentPageZone} instead, whose content is a node
 * subtree the engine lays out and paints the way it does the body, and which
 * exports to DOCX as a real Word header or footer. The two coexist and neither
 * changes the other: an existing header or footer renders exactly as it did.</p>
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

    /**
     * Whether this zone's {@link #height} is taken out of the page's content area.
     *
     * <p>{@code false} by default, which is how a zone has always behaved: the
     * height positions the text and nothing more, the body is laid out as if no
     * zone existed, and whether the two collide is left to the page margin.
     * Setting it makes the guarantee explicit — the body never runs under the
     * zone. The content area is inset to the larger of the page margin and this
     * height, so a zone that already fits inside the margin reserves nothing and
     * reflows nothing.</p>
     *
     * <p>It follows that turning this on can add pages to a document that was
     * relying on the overlap. That is why it is opt-in.</p>
     *
     * @since 2.2.3
     */
    @Builder.Default
    private final boolean reserveSpace = false;

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
        this.reserveSpace = false;
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
