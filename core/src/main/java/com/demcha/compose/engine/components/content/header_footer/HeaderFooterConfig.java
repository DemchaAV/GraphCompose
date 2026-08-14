package com.demcha.compose.engine.components.content.header_footer;

import com.demcha.compose.engine.components.content.text.TextStyle;
import lombok.Builder;
import lombok.Getter;

import java.awt.Color;

/**
 * Configuration for a repeating page header or footer.
 *
 * <p>Headers and footers are document-level constructs rather than nodes in the
 * document tree. They are attached to the canonical PDF render configuration and rendered
 * once per page after the main layout/pagination pass.</p>
 *
 * <p>Text content may include placeholders:</p>
 * <ul>
 *   <li>{@code {page}} — current page number</li>
 *   <li>{@code {pages}} — total number of pages</li>
 *   <li>{@code {date}} — current date (ISO format)</li>
 * </ul>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder
public final class HeaderFooterConfig {

    /** Whether this is a HEADER or FOOTER zone. */
    private final HeaderFooterZone zone;

    /** Height reserved for this zone in points. */
    @Builder.Default
    private final float height = 30f;

    /** Left-aligned text (may contain placeholders). */
    private final String leftText;

    /** Center-aligned text (may contain placeholders). */
    private final String centerText;

    /** Right-aligned text (may contain placeholders). */
    private final String rightText;

    /** Font size for the header/footer text. */
    @Builder.Default
    private final float fontSize = 9f;

    /** Text color. */
    @Builder.Default
    private final Color textColor = new Color(128, 128, 128);

    /** Whether to draw a separator line between the zone and the content. */
    @Builder.Default
    private final boolean showSeparator = false;

    /** Separator line color. */
    @Builder.Default
    private final Color separatorColor = new Color(200, 200, 200);

    /** Separator line thickness. */
    @Builder.Default
    private final float separatorThickness = 0.5f;

    /** Printed value on the first counted page. */
    @Builder.Default
    private final int numberStartAt = 1;

    /** Physical 1-based page where counting begins; earlier pages are uncounted. */
    @Builder.Default
    private final int numberCountFrom = 1;

    /** Whether the number is shown on the first physical page. */
    @Builder.Default
    private final boolean numberShowOnFirstPage = true;

    /** Numeral style for the rendered page number. */
    @Builder.Default
    private final PageNumberStyle numberStyle = PageNumberStyle.DECIMAL;

    /**
     * Resolves placeholder tokens in the given text for a specific page, honouring
     * this zone's page-numbering offset/restart/style. {@code {page}} and
     * {@code {pages}} use the counted value ({@code startAt + (physicalPage -
     * countFrom)}); {@code {pages}} reports the counted total, not the physical
     * page count. With the default numbering this yields exactly the same output
     * as the static {@link #resolvePlaceholders(String, int, int)}.
     *
     * @param text         raw text with placeholders
     * @param physicalPage 1-based physical page number
     * @param totalPages   total number of physical pages
     * @return resolved text
     */
    public String resolveTokens(String text, int physicalPage, int totalPages) {
        if (text == null || text.isEmpty()) return text;
        int counted = numberStartAt + (physicalPage - numberCountFrom);
        int countedTotal = numberStartAt + (totalPages - numberCountFrom);
        return text
                .replace("{page}", numberStyle.format(counted))
                .replace("{pages}", numberStyle.format(countedTotal))
                .replace("{date}", renderDate().toString());
    }

    /**
     * The date the {@code {date}} token resolves to.
     *
     * <p>The clock by default. A build that has to produce the same bytes twice can pin it with
     * {@code -Dgraphcompose.renderDate=YYYY-MM-DD} — the same need {@code SOURCE_DATE_EPOCH}
     * answers for archives, and the reason this repository can hold its committed example
     * previews to a byte comparison: a document that prints today re-renders differently every
     * morning, and a guard that reports that is a guard people learn to ignore.</p>
     *
     * <p>An unparseable value is the clock again rather than a failed render: a mistyped property
     * should not stop a document being produced, and the drift it causes surfaces where drift is
     * checked.</p>
     *
     * @return the pinned date, or today
     */
    static java.time.LocalDate renderDate() {
        String pinned = System.getProperty("graphcompose.renderDate");
        if (pinned == null || pinned.isBlank()) {
            return java.time.LocalDate.now();
        }
        try {
            return java.time.LocalDate.parse(pinned.trim());
        } catch (java.time.format.DateTimeParseException notADate) {
            return java.time.LocalDate.now();
        }
    }

    /**
     * Whether this zone's number should be rendered on the given physical page —
     * false before {@code countFrom}, and false on page 1 when
     * {@code showOnFirstPage} is off.
     *
     * @param physicalPage 1-based physical page number
     * @return {@code true} if the zone renders on this page
     */
    public boolean appliesTo(int physicalPage) {
        return physicalPage >= numberCountFrom && (numberShowOnFirstPage || physicalPage != 1);
    }

    /**
     * Resolves placeholder tokens with decimal, no-offset numbering. Retained for
     * binary compatibility (public since v1.7.0); production rendering uses the
     * numbering-aware instance method {@link #resolveTokens(String, int, int)},
     * which reproduces this output for the default numbering.
     *
     * @param text          raw text with placeholders
     * @param currentPage   1-based page number
     * @param totalPages    total number of pages
     * @return resolved text
     */
    public static String resolvePlaceholders(String text, int currentPage, int totalPages) {
        if (text == null || text.isEmpty()) return text;
        return text
                .replace("{page}", String.valueOf(currentPage))
                .replace("{pages}", String.valueOf(totalPages))
                .replace("{date}", renderDate().toString());
    }
}
