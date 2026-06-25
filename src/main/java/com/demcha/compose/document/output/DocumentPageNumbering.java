package com.demcha.compose.document.output;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Page-numbering policy for the {@code {page}} / {@code {pages}} tokens in a
 * header or footer zone: an offset, a restart point, a style, and whether the
 * number shows on the first page. Attaches to a {@link DocumentHeaderFooter} via
 * {@code builder().numbering(...)}.
 *
 * <p>{@code startAt} is the value printed on the first counted page;
 * {@code countFrom} is the physical (1-based) page where counting begins —
 * pages before it are not counted. So {@code startAt=1, countFrom=3} prints the
 * first two pages without numbering and starts the body at {@code 1}. Under an
 * offset, {@code {pages}} expands to the <em>counted</em> total
 * ({@code startAt + (totalPages - countFrom)}), not the physical page count.</p>
 *
 * <p>Suppression is whole-zone: on a page where the number is not shown
 * ({@code showOnFirstPage=false}, or a physical page before {@code countFrom}),
 * the entire header/footer zone is skipped, not just the number — which is what
 * a numbered cover / uncounted front matter wants. Put branding that must always
 * appear in a separate, always-on zone.</p>
 *
 * <p>The {@link #DEFAULT} (decimal, no offset, shown on every page) reproduces
 * the pre-1.9 behaviour exactly. Instances are immutable and thread-safe.</p>
 *
 * @author Artem Demchyshyn
 * @see DocumentPageNumberStyle
 * @since 1.9.0
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentPageNumbering {

    /** Decimal, no offset, shown on every page — the pre-1.9 default. */
    public static final DocumentPageNumbering DEFAULT = builder().build();

    /** Value printed on the first counted page. */
    @Builder.Default
    private final int startAt = 1;

    /** Physical 1-based page where counting begins; earlier pages are uncounted. */
    @Builder.Default
    private final int countFrom = 1;

    /** Whether the numbered zone is rendered on the first physical page. */
    @Builder.Default
    private final boolean showOnFirstPage = true;

    /** Numeral style for the rendered number. */
    @Builder.Default
    private final DocumentPageNumberStyle style = DocumentPageNumberStyle.DECIMAL;

    private DocumentPageNumbering() {
        this.startAt = 1;
        this.countFrom = 1;
        this.showOnFirstPage = true;
        this.style = DocumentPageNumberStyle.DECIMAL;
    }
}
