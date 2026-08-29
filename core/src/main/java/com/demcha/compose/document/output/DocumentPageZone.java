package com.demcha.compose.document.output;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A band at the top or bottom of every page whose content is a node subtree.
 *
 * <p>Where {@link DocumentHeaderFooter} offers three text slots and three
 * placeholder tokens, a zone offers the document tree: the content function
 * returns any {@link DocumentNode}, and the engine lays it out and paints it
 * through the same handlers the body goes through. A badge, a link, a logo, a
 * shape, a table, right-to-left text and any font the document has all work in a
 * zone for the same reason they work in the body — it is the same machinery,
 * not a second implementation of it.</p>
 *
 * <pre>{@code
 * document.chrome().zone(DocumentPageZone.footer(32, page -> new RowBuilder()
 *         .addParagraph(p -> p.text("Confidential"))
 *         .flexSpacer()
 *         .addParagraph(p -> p.text(page.number() + " / " + page.total()))
 *         .build()));
 * }</pre>
 *
 * <p>The content function is called once per page with that page's
 * {@link PageContext}, after pagination has settled. It must return a node; a
 * zone that should be absent on some pages says so through
 * {@code appliesTo} instead of returning {@code null}.</p>
 *
 * <p><strong>A zone is atomic.</strong> It is laid out into its declared
 * {@code height} and does not paginate. Content that needs more
 * than the band raises
 * {@link com.demcha.compose.document.exceptions.AtomicNodeTooLargeException}
 * naming the zone and its height — the same contract the body has for a block
 * too big for a page. A zone that quietly dropped what it could not fit would
 * be the harder bug to find, so declare a height that fits what you build.</p>
 *
 * <p>Unlike {@link DocumentHeaderFooter}, a zone reserves its height from the
 * page's content area by default — a zone exists to hold content, and content
 * the body can overlap is not what anyone means by it.</p>
 *
 * <p>The two types coexist deliberately rather than one replacing the other;
 * {@code docs/adr/0017-page-chrome-two-paths.md} records why, and what evidence
 * would justify revisiting it.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.2.3
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentPageZone {

    /** Which band of the page this zone occupies. */
    @Builder.Default
    private final DocumentHeaderFooterZone zone = DocumentHeaderFooterZone.FOOTER;

    /** Depth of the band in points. */
    @Builder.Default
    private final double height = 30.0;

    /**
     * Whether the band is taken out of the page's content area. On by default,
     * the opposite of {@code DocumentHeaderFooter.reserveSpace}, which has to
     * default to off to keep documents that predate the flag rendering as they did.
     */
    @Builder.Default
    private final boolean reserveSpace = true;

    /** Inset between the band's edges and its content. */
    @Builder.Default
    private final DocumentInsets padding = DocumentInsets.zero();

    /**
     * Which pages carry the zone; {@code null} means every page. Replaces the
     * numbering-driven suppression of {@link DocumentPageNumbering}, which
     * hides the whole band as a side effect of not counting a page — here the
     * two are separate questions, so "no number on the cover, but keep the
     * logo" is one zone with a predicate rather than two zones.
     */
    private final Predicate<PageContext> appliesTo;

    /** Builds the band's content for a page. Required. */
    private final Function<PageContext, DocumentNode> content;

    private DocumentPageZone() {
        this.zone = DocumentHeaderFooterZone.FOOTER;
        this.height = 30.0;
        this.reserveSpace = true;
        this.padding = DocumentInsets.zero();
        this.appliesTo = null;
        this.content = null;
    }

    /**
     * A footer zone of the given depth.
     *
     * @param height  band depth in points
     * @param content builds the band's content for a page
     * @return a footer zone
     */
    public static DocumentPageZone footer(double height, Function<PageContext, DocumentNode> content) {
        return builder()
                .zone(DocumentHeaderFooterZone.FOOTER)
                .height(height)
                .content(content)
                .build();
    }

    /**
     * A header zone of the given depth.
     *
     * @param height  band depth in points
     * @param content builds the band's content for a page
     * @return a header zone
     */
    public static DocumentPageZone header(double height, Function<PageContext, DocumentNode> content) {
        return builder()
                .zone(DocumentHeaderFooterZone.HEADER)
                .height(height)
                .content(content)
                .build();
    }

    /**
     * Whether this zone is drawn on the given page.
     *
     * @param page the page being drawn
     * @return {@code true} when the zone applies
     */
    public boolean appliesTo(PageContext page) {
        return appliesTo == null || appliesTo.test(page);
    }
}
