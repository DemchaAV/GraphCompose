package com.demcha.compose.document.output;

/**
 * What a page zone knows about the page it is being drawn on.
 *
 * <p>Handed to a {@link DocumentPageZone}'s content function once per page, after
 * pagination has settled — so {@link #total()} is the real page count, not an
 * estimate. A zone builds its content from these values directly, which is why
 * the node zone has no placeholder tokens: {@code "Page " + page.number() + " of
 * " + page.total()} says what {@code {page} of {pages}} said, and anything the
 * tokens could not express — roman numerals, an offset, a different string on
 * the last page — is ordinary Java in the same lambda.</p>
 *
 * <p>In a combined document the numbers are section-local: each section's zone
 * counts from that section's first page, matching how the text chrome resolves
 * its tokens.</p>
 *
 * @param number 1-based page number within the document or section
 * @param total  page count of the document or section
 * @author Artem Demchyshyn
 * @since 2.2.3
 */
public record PageContext(int number, int total) {

    /**
     * Validates the page coordinates.
     *
     * @throws IllegalArgumentException if the numbers cannot describe a page
     */
    public PageContext {
        if (number < 1) {
            throw new IllegalArgumentException("Page number must be 1-based, got " + number + ".");
        }
        if (total < number) {
            throw new IllegalArgumentException(
                    "Page total (" + total + ") cannot be smaller than the page number (" + number + ").");
        }
    }

    /**
     * Whether this is the first page.
     *
     * @return {@code true} on page 1
     */
    public boolean isFirst() {
        return number == 1;
    }

    /**
     * Whether this is the last page.
     *
     * @return {@code true} on the final page
     */
    public boolean isLast() {
        return number == total;
    }
}
